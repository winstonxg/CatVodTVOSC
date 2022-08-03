package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.player.controller.VodController;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;

import xyz.doikki.videoplayer.player.VideoView;

public class PlayActivity extends BaseActivity {

    private FrameLayout mPlayFrame;
    private PlayerFragment playerFragment;
    private Handler mHandler = new Handler();

    private static final int DETAIL_PLAYER_FRAME_ID = 9999998;

    public PlayActivity() {
        shouldHideSystemBar = true;
    }

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
        if(playerFragment == null) {
            playerFragment = new PlayerFragment();
        }
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
        if(DetailActivity.getManagedPlayerFragment() != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(playerFragment).commit();
        } else {
            playerFragment.destroy();
        }
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