package com.github.tvbox.osc.ui.fragment.homes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.QRCodeDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractHomeFragment extends BaseLazyFragment {

    protected SourceViewModel sourceViewModel;
    protected TextView tvDate;
    protected Handler mHandler = new Handler();
    protected TextView tvName;
    //protected ImageView ivQRCode;

    public boolean useCacheConfig = false;

    private static ManagedHomeViewStyle[] managedHomeFragments = new ManagedHomeViewStyle[] {
            new ManagedHomeViewStyle("AssembledFragment", "集合式"),
            new ManagedHomeViewStyle("CatFragment", "老猫式")
    };

    public static class ManagedHomeViewStyle {
        private String className;
        private String name;
        public ManagedHomeViewStyle(String className, String name) {
            this.className = className;
            this.name = name;
        }
        public String getClassName() { return className; }
        public String getName() { return name; }
    }

    public static ArrayList<ManagedHomeViewStyle> getManagedHomeFragments() {
        ArrayList<ManagedHomeViewStyle> fragments = new ArrayList<>();
        for (ManagedHomeViewStyle fragment : managedHomeFragments) {
            fragments.add(fragment);
        }
        return fragments;
    }

    public static ManagedHomeViewStyle lookupHomeViewStyle(String className) {
        for (ManagedHomeViewStyle style : managedHomeFragments) {
            if(style.getClassName().equals(className))
                return style;
        }
        return managedHomeFragments[0];
    }

    private void bindQuickApiChange() {
        if(tvName != null) {
            tvName.setText(getResources().getText(R.string.app_name) + " - " + ApiConfig.get().getHomeSourceBean().getName());
            tvName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    List<SourceBean> sites = ApiConfig.get().getSourceBeanList();
                    if (sites.size() > 0) {
                        SelectDialog<SourceBean> dialog = new SelectDialog<>(mActivity);
                        dialog.setTip("请选择首页数据源");
                        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
                            @Override
                            public void click(SourceBean value, int pos) {
                                ApiConfig.get().setSourceBean(value);
                                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.HOME_BEAN_QUICK_CHANGE, true));
                                AppManager.getInstance().finishAllActivity();
                                Bundle bundle = new Bundle();
                                bundle.putBoolean("useCache", true);
                                jumpActivity(HomeActivity.class, bundle);
                            }

                            @Override
                            public String getDisplay(SourceBean val) {
                                return val.getName();
                            }
                        }, new DiffUtil.ItemCallback<SourceBean>() {
                            @Override
                            public boolean areItemsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                                return oldItem == newItem;
                            }

                            @Override
                            public boolean areContentsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                                return oldItem.getKey().equals(newItem.getKey());
                            }
                        }, sites, sites.indexOf(ApiConfig.get().getHomeSourceBean()));
                        dialog.show();
                    }
                }
            });
        }
    }

    private Runnable mRunnable = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            if(tvDate != null)
                tvDate.setText(timeFormat.format(date));
            mHandler.postDelayed(this, 1000);
        }
    };

    private boolean dataInitOk = false;
    private boolean jarInitOk = false;

    protected void initData() {
        if (dataInitOk && jarInitOk) {
            doAfterApiInit();
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            if (((BaseActivity)mActivity).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                LOG.e("有");
            } else {
                LOG.e("无");
            }
            bindQuickApiChange();
            return;
        }
        if (dataInitOk && !jarInitOk) {
            showLoading("正在加载自定义设置...");
            if (!StringUtils.isEmpty(ApiConfig.get().getSpider())) {
                showLoading("正在加载自定义爬虫代码...");
                ApiConfig.get().loadJar(useCacheConfig,
                        ApiConfig.get().getHomeSourceBean().getSpider(),
                        ApiConfig.get().getSpider(),
                        new ApiConfig.LoadConfigCallback() {
                    @Override
                    public void success() {
                        jarInitOk = true;
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                initData();
                            }
                        }, 50);
                    }

                    @Override
                    public void retry() {

                    }

                    @Override
                    public void error(String msg) {
                        jarInitOk = true;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, "jar加载失败", Toast.LENGTH_SHORT).show();
                                initData();
                            }
                        });
                    }
                });
            }
            return;
        }
        ApiConfig.LoadConfigCallback configCallback = new ApiConfig.LoadConfigCallback() {
            TipDialog dialog = null;

            @Override
            public void retry() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                });
            }

            @Override
            public void success() {
                showLoading("正在加载站点规则...");
                dataInitOk = true;
                if (StringUtils.isEmpty(ApiConfig.get().getSpider())) {
                    jarInitOk = true;
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        initData();
                    }
                }, 50);
            }

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dataInitOk = true;
                            jarInitOk = true;
                            initData();
                        }
                    });
                    return;
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog == null)
                            dialog = new TipDialog(mActivity, msg, "重试", "取消", new TipDialog.OnListener() {
                                @Override
                                public void left() {
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void right() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }

                                @Override
                                public void cancel() {
                                    dataInitOk = true;
                                    jarInitOk = true;
                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            initData();
                                            dialog.hide();
                                        }
                                    });
                                }
                            });
                        if (!dialog.isShowing())
                            dialog.show();
                    }
                });
            }
        };
        showLoading("正在加载索引...", 3000L, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OkGo.getInstance().cancelTag("loadApi");
                String apiUrl = Hawk.get(HawkConfig.API_URL, "");
                if (apiUrl.isEmpty()) {
                    dataInitOk = true;
                    jarInitOk = true;
                    initData();
                    return;
                }
                File cache = new File(App.getInstance().getFilesDir().getAbsolutePath() + "/" + MD5.encode(apiUrl));
                if (!cache.exists()) {
                    dataInitOk = true;
                    jarInitOk = true;
                    initData();
                    return;
                }
                useCacheConfig = true;
                ApiConfig.get().loadConfig(useCacheConfig, configCallback, mActivity);
            }
        });
        ApiConfig.get().loadConfig(useCacheConfig, configCallback, mActivity);
    }

    private long mExitTime = 0;

    protected boolean exit() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            return true;
        } else {
            mExitTime = System.currentTimeMillis();
            Toast.makeText(mActivity, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void updateScreenTime() {
        mHandler.post(mRunnable);
    }

    public void pauseHandler() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public boolean dispatchKey(KeyEvent event) {
        if(tvName != null && event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_MENU)
        {
            tvName.callOnClick();
            return true;
        }
        return false;
    }

    public boolean pressBack() {
        if(!jarInitOk || !dataInitOk) {
            dataInitOk = true;
            jarInitOk = true;
            showSuccess();
            return false;
        }
        return true;
    }
    public abstract void doAfterApiInit();
}
