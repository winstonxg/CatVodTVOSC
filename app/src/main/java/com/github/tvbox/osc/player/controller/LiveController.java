package com.github.tvbox.osc.player.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.orhanobut.hawk.Hawk;

import org.jetbrains.annotations.NotNull;

import xyz.doikki.videoplayer.player.VideoView;

/**
 * 直播控制器
 */

public class LiveController extends BaseController {
    protected ProgressBar mLoading;
    private TextView loadingSpeed;
    private int minFlingDistance = 100;             //最小识别距离
    private int minFlingVelocity = 10;              //最小识别速度
    private boolean shouldShowLoadingSpeed = Hawk.get(HawkConfig.DISPLAY_LOADING_SPEED, true);
    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            loadingSpeed.setText(PlayerHelper.getDisplaySpeed(mControlWrapper.getTcpSpeed()));
            mHandler.postDelayed(this, 1000);
        }
    };

    public LiveController(@NotNull Context context) {
        super(context);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_live_control_view;
    }

    @Override
    protected void initView() {
        super.initView();
        mLoading = findViewById(R.id.loading);
        loadingSpeed = findViewById(R.id.loadingSpeed);
    }

    public interface LiveControlListener {
        boolean singleTap();

        void longPress();

        void playStateChanged(int playState);

        void changeSource(int direction);
    }

    private LiveController.LiveControlListener listener = null;

    public void setListener(LiveController.LiveControlListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (listener.singleTap())
            return true;
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        listener.longPress();
        super.onLongPress(e);
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        switch (playState) {
            case VideoView.STATE_PREPARED:
            case VideoView.STATE_BUFFERED:
            case VideoView.STATE_PLAYING:
                this.loadingSpeed.setVisibility(GONE);
                mHandler.removeCallbacks(mRunnable);
                break;
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_BUFFERING:
                if(!Hawk.get(HawkConfig.LIVE_SHOW_NET_SPEED, false) && shouldShowLoadingSpeed) {
                    this.loadingSpeed.setVisibility(VISIBLE);
                    mHandler.post(mRunnable);
                }
                break;
        }
        listener.playStateChanged(playState);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
            listener.changeSource(-1);          //左滑
        } else if (e2.getX() - e1.getX() > minFlingDistance && Math.abs(velocityX) > minFlingVelocity) {
            listener.changeSource(1);           //右滑
        } else if (e1.getY() - e2.getY() > minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
        } else if (e2.getY() - e1.getY() > minFlingDistance && Math.abs(velocityY) > minFlingVelocity) {
        }
        return false;
    }
}
