package com.github.tvbox.osc.ui.fragment;

import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.github.catvod.crawler.JarLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.dialog.AboutDialog;
import com.github.tvbox.osc.ui.dialog.ApiDialog;
import com.github.tvbox.osc.ui.dialog.ApiHistoryDialog;
import com.github.tvbox.osc.ui.dialog.BackupDialog;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.XWalkInitDialog;
import com.github.tvbox.osc.ui.fragment.homes.AbstractHomeFragment;
import com.github.tvbox.osc.util.AppUpdate;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import okhttp3.HttpUrl;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class ModelSettingFragment extends BaseLazyFragment {
    private TextView tvDebugOpen;
    private TextView tvMediaCodec;
    private TextView tvParseWebView;
    private TextView tvPlay;
    private TextView tvRender;
    private TextView tvScale;
    private TextView tvApi;
    private TextView tvHomeApi;
    private TextView homeView;
    private TextView tvDns;
    private TextView tvSearchView;
    private TextView thirdPartyPlayer;
    private LinearLayout thirdPartyPlayLayout;
    private TextView tvVersion;
    private TextView tv2kAdapter;
    private TextView tvType;
    private TextView tvRemoteControl;

    public static ModelSettingFragment newInstance() {
        return new ModelSettingFragment().setArguments();
    }

    public ModelSettingFragment setArguments() {
        return this;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_model;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        tvDebugOpen = findViewById(R.id.tvDebugOpen);
        tvParseWebView = findViewById(R.id.tvParseWebView);
        tvMediaCodec = findViewById(R.id.tvMediaCodec);
        tvPlay = findViewById(R.id.tvPlay);
        tvRender = findViewById(R.id.tvRenderType);
        tvScale = findViewById(R.id.tvScaleType);
        tvApi = findViewById(R.id.tvApi);
        tvHomeApi = findViewById(R.id.tvHomeApi);
        homeView = findViewById(R.id.homeView);
        tvDns = findViewById(R.id.tvDns);
        tvSearchView = findViewById(R.id.tvSearchView);
        thirdPartyPlayer = findViewById(R.id.tv3rdPlay);
        thirdPartyPlayLayout = findViewById(R.id.thirdPartyPlay);
        tvVersion = findViewById(R.id.tvVersion);
        tv2kAdapter = findViewById(R.id.tv2kAdapter);
        tvType = findViewById(R.id.tvType);
        tvRemoteControl = findViewById(R.id.tvRemoteControl);

        tvMediaCodec.setText(Hawk.get(HawkConfig.IJK_CODEC, ""));
        tvDebugOpen.setText(Hawk.get(HawkConfig.DEBUG_OPEN, false) ? "已打开" : "已关闭");
        tv2kAdapter.setText(Hawk.get(HawkConfig.ENABLE_2K_ADAPTER, false) ? "已打开" : "已关闭");
        tvParseWebView.setText(Hawk.get(HawkConfig.PARSE_WEBVIEW, true) ? "系统自带" : "XWalkView");
        tvApi.setText(Hawk.get(HawkConfig.API_URL, ""));
        tvDns.setText(OkGoHelper.dnsHttpsList.get(Hawk.get(HawkConfig.DOH_URL, 0)));
        homeView.setText(AbstractHomeFragment.lookupHomeViewStyle(Hawk.get(HawkConfig.HOME_VIEW_STYLE, "")).getName());
        //tvHomeRec.setText(getHomeRecName(Hawk.get(HawkConfig.HOME_REC, 0)));
        tvSearchView.setText(getSearchView(Hawk.get(HawkConfig.SEARCH_VIEW, 0)));
        tvHomeApi.setText(ApiConfig.get().getHomeSourceBean().getName());
        tvScale.setText(PlayerHelper.getScaleName(Hawk.get(HawkConfig.PLAY_SCALE, 0)));
        tvPlay.setText(PlayerHelper.getPlayerName(Hawk.get(HawkConfig.PLAY_TYPE, 0)));
        tvRender.setText(PlayerHelper.getRenderName(Hawk.get(HawkConfig.PLAY_RENDER, 0)));
        tvVersion.setText(AppUpdate.getCurrentVersionNo());
        tvType.setText(PlayerHelper.getDeviceTypeName(Hawk.get(HawkConfig.TV_TYPE, 0)));
        tvRemoteControl.setText(Hawk.get(HawkConfig.REMOTE_CONTROL, true) ? "已打开" : "已关闭");

        findViewById(R.id.llDebug).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                Hawk.put(HawkConfig.DEBUG_OPEN, !Hawk.get(HawkConfig.DEBUG_OPEN, false));
                tvDebugOpen.setText(Hawk.get(HawkConfig.DEBUG_OPEN, false) ? "已打开" : "已关闭");
            }
        });
        findViewById(R.id.llParseWebVew).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                boolean useSystem = !Hawk.get(HawkConfig.PARSE_WEBVIEW, true);
                Hawk.put(HawkConfig.PARSE_WEBVIEW, useSystem);
                tvParseWebView.setText(Hawk.get(HawkConfig.PARSE_WEBVIEW, true) ? "系统自带" : "XWalkView");
                if (!useSystem) {
                    Toast.makeText(mContext, "注意: XWalkView只适用于部分低Android版本，Android5.0以上推荐使用系统自带", Toast.LENGTH_LONG).show();
                    XWalkInitDialog dialog = new XWalkInitDialog(mContext);
                    dialog.setOnListener(new XWalkInitDialog.OnListener() {
                        @Override
                        public void onchange() {
                        }
                    });
                    dialog.show();
                }
            }
        });
        findViewById(R.id.llBackup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                BackupDialog dialog = new BackupDialog(mActivity);
                dialog.show();
            }
        });
        findViewById(R.id.llAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                AboutDialog dialog = new AboutDialog(mActivity);
                dialog.show();
            }
        });
        findViewById(R.id.llVersion).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                new AppUpdate().CheckLatestVersion(ModelSettingFragment.this.mActivity, true, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Toast.makeText(mContext, "已经是最新版本", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                });
            }
        });
        findViewById(R.id.llHomeApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                List<SourceBean> sites = ApiConfig.get().getSourceBeanList();
                if (sites.size() > 0) {
                    SelectDialog<SourceBean> dialog = new SelectDialog<>(mActivity);
                    dialog.setTip("请选择首页数据源");
                    dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
                        @Override
                        public void click(SourceBean value, int pos) {
                            ApiConfig.get().setSourceBean(value);
                            tvHomeApi.setText(ApiConfig.get().getHomeSourceBean().getName());
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
        findViewById(R.id.llHomeView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                ArrayList<AbstractHomeFragment.ManagedHomeViewStyle> homeViewStyles = AbstractHomeFragment.getManagedHomeFragments();
                String currentVal = Hawk.get(HawkConfig.HOME_VIEW_STYLE, homeViewStyles.get(0).getClassName());
                int defaultPos = 0;
                for (AbstractHomeFragment.ManagedHomeViewStyle style: homeViewStyles) {
                    if(style.getClassName().equals(currentVal))
                        break;
                    else
                        defaultPos ++;
                }
                if(defaultPos < 0 || defaultPos >= homeViewStyles.size())
                    defaultPos = 0;
                SelectDialog<AbstractHomeFragment.ManagedHomeViewStyle> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择主界面样式");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<AbstractHomeFragment.ManagedHomeViewStyle>() {
                    @Override
                    public void click(AbstractHomeFragment.ManagedHomeViewStyle value, int pos) {
                        Hawk.put(HawkConfig.HOME_VIEW_STYLE, value.getClassName());
                        homeView.setText(value.getName());
                    }

                    @Override
                    public String getDisplay(AbstractHomeFragment.ManagedHomeViewStyle val) {
                        return val.getName();
                    }
                }, new DiffUtil.ItemCallback<AbstractHomeFragment.ManagedHomeViewStyle>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull AbstractHomeFragment.ManagedHomeViewStyle oldItem, @NonNull @NotNull AbstractHomeFragment.ManagedHomeViewStyle newItem) {
                        return oldItem.getClassName().equals(newItem.getClassName());
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull AbstractHomeFragment.ManagedHomeViewStyle oldItem, @NonNull @NotNull AbstractHomeFragment.ManagedHomeViewStyle newItem) {
                        return oldItem.getClassName().equals(newItem.getClassName());
                    }
                }, homeViewStyles, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.ll2kAdapter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                List<String> options = Arrays.asList(new String[] { "关闭", "开启" });
                boolean enable2KAdapter = Hawk.get(HawkConfig.ENABLE_2K_ADAPTER, false);
                SelectDialog<String> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("是否开启大屏幕适应");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
                    @Override
                    public void click(String value, int pos) {
                        Hawk.put(HawkConfig.ENABLE_2K_ADAPTER, pos == 0 ? false : true);
                        Toast.makeText(mContext, "重启后全局生效。", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public String getDisplay(String val) {
                        return val;
                    }
                }, null, options, enable2KAdapter? 1 : 0);
                dialog.show();
            }
        });
        findViewById(R.id.llBackground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String requestBackgroundUrl = ApiConfig.get().getRequestBackgroundUrl();
                if(requestBackgroundUrl == null) {
                    Toast.makeText(mContext, "没有设置壁纸源，请添加参数wallpaper。", Toast.LENGTH_LONG).show();
                    return;
                }
                File cache = new File(App.getInstance().getCacheDir().getAbsolutePath() + "/temp_wallpaper.jpg");
                Toast.makeText(mContext, "正在获取壁纸...", Toast.LENGTH_LONG).show();
                OkGo.<File>get(requestBackgroundUrl).execute(new AbsCallback<File>() {

                    @Override
                    public File convertResponse(okhttp3.Response response) throws Throwable {
                        File cacheDir = cache.getParentFile();
                        if (!cacheDir.exists())
                            cacheDir.mkdirs();
                        if (cache.exists())
                            cache.delete();
                        FileOutputStream fos = new FileOutputStream(cache);
                        fos.write(response.body().bytes());
                        fos.flush();
                        fos.close();
                        return cache;
                    }

                    @Override
                    public void onSuccess(Response<File> response) {
                        if (response.body().exists()) {
                            Hawk.put(HawkConfig.CUSTOM_WALLPAPER, true);
                            ((BaseActivity)mActivity).updateBackground();
                        } else {
                            Toast.makeText(mContext, "下载壁纸错误。", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(Response<File> response) {
                        super.onError(response);
                        Toast.makeText(mContext, "下载壁纸错误。", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        findViewById(R.id.llResetBackground).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Hawk.put(HawkConfig.CUSTOM_WALLPAPER, false);
                ((BaseActivity)mActivity).updateBackground();
            }
        });
        findViewById(R.id.llDns).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int dohUrl = Hawk.get(HawkConfig.DOH_URL, 0);

                SelectDialog<String> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择安全DNS");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
                    @Override
                    public void click(String value, int pos) {
                        tvDns.setText(OkGoHelper.dnsHttpsList.get(pos));
                        Hawk.put(HawkConfig.DOH_URL, pos);
                        String url = OkGoHelper.getDohUrl(pos);
                        OkGoHelper.dnsOverHttps.setUrl(url.isEmpty() ? null : HttpUrl.get(url));
                        IjkMediaPlayer.toggleDotPort(pos > 0);
                    }

                    @Override
                    public String getDisplay(String val) {
                        return val;
                    }
                }, new DiffUtil.ItemCallback<String>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
                        return oldItem.equals(newItem);
                    }
                }, OkGoHelper.dnsHttpsList, dohUrl);
                dialog.show();
            }
        });
        findViewById(R.id.llApi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                ApiDialog dialog = new ApiDialog(mActivity);
                EventBus.getDefault().register(dialog);
                dialog.setOnListener(new ApiDialog.OnListener() {
                    @Override
                    public void onchange(String api) {
                        Hawk.put(HawkConfig.API_URL, api);
                        tvApi.setText(api);
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        ((BaseActivity) mActivity).hideSysBar();
                        EventBus.getDefault().unregister(dialog);
                    }
                });
                dialog.show();
            }
        });
        findViewById(R.id.llMediaCodec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<IJKCode> ijkCodes = ApiConfig.get().getIjkCodes();
                if (ijkCodes == null || ijkCodes.size() == 0)
                    return;
                FastClickCheckUtil.check(v);

                int defaultPos = 0;
                String ijkSel = Hawk.get(HawkConfig.IJK_CODEC, "");
                for (int j = 0; j < ijkCodes.size(); j++) {
                    if (ijkSel.equals(ijkCodes.get(j).getName())) {
                        defaultPos = j;
                        break;
                    }
                }

                SelectDialog<IJKCode> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择IJK解码");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<IJKCode>() {
                    @Override
                    public void click(IJKCode value, int pos) {
                        value.selected(true);
                        tvMediaCodec.setText(value.getName());
                    }

                    @Override
                    public String getDisplay(IJKCode val) {
                        return val.getName();
                    }
                }, new DiffUtil.ItemCallback<IJKCode>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                        return oldItem == newItem;
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull IJKCode oldItem, @NonNull @NotNull IJKCode newItem) {
                        return oldItem.getName().equals(newItem.getName());
                    }
                }, ijkCodes, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llDisplayLoadingSpeed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> speedOptions = Arrays.asList(new String[] { "关闭", "展示" });
                boolean displayLoadingSpeed = Hawk.get(HawkConfig.DISPLAY_LOADING_SPEED, true);
                FastClickCheckUtil.check(v);
                SelectDialog<String> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("是否展示缓冲速度");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
                    @Override
                    public void click(String value, int pos) {
                        Hawk.put(HawkConfig.DISPLAY_LOADING_SPEED, pos == 0 ? false : true);
                    }

                    @Override
                    public String getDisplay(String val) {
                        return val;
                    }
                }, null, speedOptions, displayLoadingSpeed? 1 : 0);
                dialog.show();
            }
        });
        findViewById(R.id.llScale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.PLAY_SCALE, 0);
                ArrayList<Integer> players = new ArrayList<>();
                players.add(0);
                players.add(1);
                players.add(2);
                players.add(3);
                players.add(4);
                players.add(5);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择默认画面缩放");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.PLAY_SCALE, value);
                        tvScale.setText(PlayerHelper.getScaleName(value));
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return PlayerHelper.getScaleName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, players, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.PLAY_TYPE, 0);
                ArrayList<Integer> players = new ArrayList<>();
                players.add(0);
                players.add(1);
                players.add(2);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择默认播放器");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.PLAY_TYPE, value);
                        tvPlay.setText(PlayerHelper.getPlayerName(value));
                        PlayerHelper.init();
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return PlayerHelper.getPlayerName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, players, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llRender).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.PLAY_RENDER, 0);
                ArrayList<Integer> renders = new ArrayList<>();
                renders.add(0);
                renders.add(1);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择默认渲染方式");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.PLAY_RENDER, value);
                        tvRender.setText(PlayerHelper.getRenderName(value));
                        PlayerHelper.init();
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return PlayerHelper.getRenderName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, renders, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llSearchView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                int defaultPos = Hawk.get(HawkConfig.SEARCH_VIEW, 0);
                ArrayList<Integer> types = new ArrayList<>();
                types.add(0);
                types.add(1);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择搜索视图");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.SEARCH_VIEW, value);
                        tvSearchView.setText(getSearchView(value));
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return getSearchView(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, types, defaultPos);
                dialog.show();
            }
        });
        thirdPartyPlayLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!thirdPartyPlayLayout.isFocusable())
                    return;
                FastClickCheckUtil.check(view);
                Integer[] types = PlayerHelper.getAvailable3rdPlayerTypes();
                Integer currentVal = Hawk.get(HawkConfig.THIRD_PARTY_PLAYER, types[0]);
                int defaultPos = Arrays.binarySearch(types, currentVal);
                if(defaultPos < 0)
                    defaultPos = 0;
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择外部播放器");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.THIRD_PARTY_PLAYER, value);
                        thirdPartyPlayer.setText(get3rdPlayerName(value));
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return get3rdPlayerName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, Arrays.asList(types), defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llTvType).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                int defaultPos = Hawk.get(HawkConfig.TV_TYPE, 0);
                ArrayList<Integer> types = new ArrayList<>();
                types.add(0);
                types.add(1);
                SelectDialog<Integer> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("请选择设备类型");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<Integer>() {
                    @Override
                    public void click(Integer value, int pos) {
                        Hawk.put(HawkConfig.TV_TYPE, value);
                        tvType.setText(PlayerHelper.getDeviceTypeName(value));
                        PlayerHelper.init();
                    }

                    @Override
                    public String getDisplay(Integer val) {
                        return PlayerHelper.getDeviceTypeName(val);
                    }
                }, new DiffUtil.ItemCallback<Integer>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull Integer oldItem, @NonNull @NotNull Integer newItem) {
                        return oldItem.intValue() == newItem.intValue();
                    }
                }, types, defaultPos);
                dialog.show();
            }
        });
        findViewById(R.id.llRemoteControl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> options = Arrays.asList(new String[] { "开启", "关闭" });
                boolean selectedVal = Hawk.get(HawkConfig.REMOTE_CONTROL, true);
                FastClickCheckUtil.check(view);
                SelectDialog<String> dialog = new SelectDialog<>(mActivity);
                dialog.setTip("是否开启网页遥控");
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
                    @Override
                    public void click(String value, int pos) {
                        Hawk.put(HawkConfig.REMOTE_CONTROL, pos == 0 ? true : false);
                        tvRemoteControl.setText(pos == 0 ? "已打开" : "已关闭");
                    }

                    @Override
                    public String getDisplay(String val) {
                        return val;
                    }
                }, null, options, selectedVal ? 0 : 1);
                dialog.show();
            }
        });
        findViewById(R.id.llApiHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<String>());
                if (history.isEmpty())
                    return;
                String current = Hawk.get(HawkConfig.API_URL, "");
                int idx = 0;
                if (history.contains(current))
                    idx = history.indexOf(current);
                ApiHistoryDialog dialog = new ApiHistoryDialog(getContext());
                dialog.setTip("历史配置列表");
                dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
                    @Override
                    public void click(String value) {
                        Hawk.put(HawkConfig.API_URL, value);
                        tvApi.setText(value);
                        dialog.dismiss();
                    }

                    @Override
                    public void del(String value, ArrayList<String> data) {
                        Hawk.put(HawkConfig.API_HISTORY, data);
                    }
                }, history, idx);
                dialog.show();
            }
        });
        findViewById(R.id.llHomeApi).requestFocus();
        SettingActivity.callback = new SettingActivity.DevModeCallback() {
            @Override
            public void onChange() {
                findViewById(R.id.llDebug).setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayerHelper.reload3rdPlayers();
        if(!thirdPartyPlayLayout.isFocusable()) {
            String thirdPartyPlayerName = PlayerHelper.get3rdPlayerName(Hawk.get(HawkConfig.THIRD_PARTY_PLAYER, 0));
            if(thirdPartyPlayerName != null) {
                thirdPartyPlayLayout.setFocusable(true);
                thirdPartyPlayer.setTextColor(getResources().getColor(R.color.color_FFFFFF));
                thirdPartyPlayer.setText(thirdPartyPlayerName);
            }
        } else {
            Integer[] thirdPartyPlayerTypes = PlayerHelper.getAvailable3rdPlayerTypes();
            String thirdPartyPlayerName = PlayerHelper.get3rdPlayerName(Hawk.get(HawkConfig.THIRD_PARTY_PLAYER, 0));
            if (thirdPartyPlayerTypes.length <= 0) {
                thirdPartyPlayer.setTextColor(getResources().getColor(R.color.color_6CFFFFFF));
                thirdPartyPlayer.setText("没有找到可用播放器");
                thirdPartyPlayLayout.setFocusable(false);
            } else {
                thirdPartyPlayer.setText(thirdPartyPlayerName);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_API_URL_CHANGE) {
            String newApi = (String) event.obj;
            Hawk.put(HawkConfig.API_URL, newApi);
            tvApi.setText(newApi);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        SettingActivity.callback = null;
    }

    String get3rdPlayerName(int type) {
        return PlayerHelper.get3rdPlayerName(type);
    }

    String getSearchView(int type) {
        if (type == 0) {
            return "文字列表";
        } else {
            return "缩略图";
        }
    }
}