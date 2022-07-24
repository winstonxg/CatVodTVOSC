package com.github.tvbox.osc.ui.fragment.homes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractHomeFragment extends BaseLazyFragment {

    protected SourceViewModel sourceViewModel;
    protected TextView tvDate;
    protected Handler mHandler = new Handler();
    protected TextView tvQuickApi;
    private boolean newChangeApi = false;

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

    protected void initView() {
        tvQuickApi = findViewById(R.id.tvQuickApi);
        if(tvQuickApi != null) {
            tvQuickApi.setOnClickListener(new View.OnClickListener() {
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
                                newChangeApi = true;
                                AppManager.getInstance().finishAllActivity();
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
            sourceViewModel.getSort(ApiConfig.get().getHomeSourceBean().getKey());
            if (((BaseActivity)mActivity).hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                LOG.e("有");
            } else {
                LOG.e("无");
            }
            if(tvQuickApi != null) {
                tvQuickApi.setText(ApiConfig.get().getHomeSourceBean().getName());
                tvQuickApi.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (dataInitOk && !jarInitOk) {
            showLoading("正在加载自定义设置...");
            if (!ApiConfig.get().getSpider().isEmpty()) {
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
        showLoading("正在加载索引...");
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {
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
                if (ApiConfig.get().getSpider().isEmpty()) {
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
        }, mActivity);
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

    public abstract boolean pressBack();
    public abstract boolean dispatchKey(KeyEvent event);

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.newChangeApi)
            jumpActivity(HomeActivity.class);
    }
}
