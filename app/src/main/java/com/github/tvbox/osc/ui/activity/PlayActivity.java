package com.github.tvbox.osc.ui.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.CacheManager;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.player.thirdparty.MXPlayer;
import com.github.tvbox.osc.player.thirdparty.ReexPlayer;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.util.AdBlocker;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.XWalkUtils;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.jessyan.autosize.AutoSize;
import xyz.doikki.videoplayer.player.ProgressManager;
import xyz.doikki.videoplayer.player.VideoView;

public class PlayActivity extends BaseActivity {

    private FrameLayout mPlayFrame;
    private PlayerFragment playerFragment;
    private Handler mHandler = new Handler();

    private static final int DETAIL_PLAYER_FRAME_ID = 9999998;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_play;
    }

    @Override
    protected void init() {
        initView();
    }

    private void initView() {
        mPlayFrame = findViewById(R.id.mPlayFrame);
        FrameLayout tempFrame = new FrameLayout(this);
        tempFrame.setId(DETAIL_PLAYER_FRAME_ID);
        mPlayFrame.addView(tempFrame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        playerFragment = DetailActivity.getManagedPlayerFragment();
        getSupportFragmentManager().beginTransaction()
                .add(DETAIL_PLAYER_FRAME_ID, playerFragment, PlayerFragment.FRAGMENT_TAG).disallowAddToBackStack().commit();
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            if(bundle != null && bundle.containsKey("newSource") && bundle.getBoolean("newSource")) {
                VodInfo vodInfo = (VodInfo) bundle.getSerializable("VodInfo");
                String sourceKey = bundle.getString("sourceKey");
                mPlayFrame.post(new Runnable() {
                    @Override
                    public void run() {
                        playerFragment.getVodController().enableController(true);
                        playerFragment.initData(vodInfo, sourceKey);
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        VodController controller = playerFragment.getVodController();
        if (controller != null && controller.onBackPressed()) {
            return;
        }
        super.onBackPressed();
        getSupportFragmentManager().beginTransaction()
                .remove(playerFragment).commit();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        VodController controller = playerFragment.getVodController();
        if (event != null && controller != null) {
            if (controller.onKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VideoView videoView = playerFragment.getVideoView();
        if (videoView != null) {
            videoView.resume();
        }
        VodController controller = playerFragment.getVodController();
        if(controller != null)
            controller.init3rdPlayerButton();
    }


    @Override
    protected void onPause() {
        super.onPause();
        VideoView videoView = playerFragment.getVideoView();
        if (videoView != null) {
            videoView.pause();
        }
    }

}