package com.github.tvbox.osc.player.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.ParseAdapter;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.PlayerHelper;
import com.google.gson.JsonObject;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.util.PlayerUtils;

import static xyz.doikki.videoplayer.util.PlayerUtils.stringForTime;

public class VodController extends BaseController {

    private PlayerFragment playerFragment;
    private ParseAdapter parseAdapter;
    private JsonObject progressData = new JsonObject();
    private boolean isControllerLock = false;
    private boolean isShowingLocker = false;
    private int deviceType = 0;
    private boolean enabledWebRemote = true;

    public VodController(@NonNull @NotNull Context context, PlayerFragment playerFragment) {
        super(context);
        this.playerFragment = playerFragment;
        deviceType = Hawk.get(HawkConfig.TV_TYPE, 0);
        enabledWebRemote = Hawk.get(HawkConfig.REMOTE_CONTROL, true);
        mHandlerCallback = new HandlerCallback() {
            @Override
            public void callback(Message msg) {
                switch (msg.what) {
                    case 1000: { // seek 刷新
                        mProgressRoot.setVisibility(VISIBLE);
                        break;
                    }
                    case 1001: { // seek 关闭
                        mProgressRoot.setVisibility(GONE);
                        break;
                    }
                    case 1002: { // 显示底部菜单
                        showControllerAnimation();
                        break;
                    }
                    case 1003: { // 隐藏底部菜单
                        hideControllerAnimation();
                        break;
                    }
                    case 1004: { // 设置速度
                        if (isInPlaybackState()) {
                            try {
                                float speed = (float) mPlayerConfig.getDouble("sp");
                                mControlWrapper.setSpeed(speed);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else
                            mHandler.sendEmptyMessageDelayed(1004, 100);
                        break;
                    }
                }
            }
        };
        progressData.addProperty("type", "ctrl");
    }

    SeekBar mSeekBar;
    TextView mCurrentTime;
    TextView mTotalTime;
    boolean mIsDragging;
    LinearLayout mProgressRoot;
    TextView mProgressText;
    ImageView mProgressIcon;
    LinearLayout mTopRoot;
    LinearLayout mBottomRoot;
    LinearLayout mParseRoot;
    TvRecyclerView mGridView;
    TextView mPlayTitle;
    TextView tvDate;
    ImageView mPlayPause;
    ImageView mNextBtn;
    ImageView mPreBtn;
    TextView mPlayerScaleBtn;
    TextView mPlayerSpeedBtn;
    TextView mPlayerBtn;
    TextView mPlayerIJKBtn;
    TextView m3rdPlayerBtn;
    ImageView mPlayerRetry;
    TextView mPlayerTimeStartBtn;
    TextView mPlayerTimeSkipBtn;
    TextView mPlayerTimeStepBtn;
    TextView loadingSpeed;
    TextView tvVideoInfo;
    TextView finishAt;
    TextView btnHint;
    ImageView lockerLeft;
    ImageView lockerRight;
    ImageView tvBack;
    ImageView playAudio;

    private boolean shouldShowBottom = true;
    private boolean shouldShowLoadingSpeed = Hawk.get(HawkConfig.DISPLAY_LOADING_SPEED, true);
    private Runnable mRunnable = new Runnable() {
        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void run() {
            Date date = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            tvDate.setText(timeFormat.format(date));
            if(mControlWrapper.getDuration() > 0) {
                SimpleDateFormat onlyTimeFormat = new SimpleDateFormat("HH:mm");
                long remainTime = mControlWrapper.getDuration() - mControlWrapper.getCurrentPosition();
                Date endTime = new Date(date.getTime() + remainTime);
                finishAt.setText("本集完结于 " + onlyTimeFormat.format(endTime));
            } else {
                finishAt.setText("");
            }
            if(loadingSpeed.getVisibility() == VISIBLE)
                loadingSpeed.setText(PlayerHelper.getDisplaySpeed(mControlWrapper.getTcpSpeed()));
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void initView() {
        super.initView();
        mCurrentTime = findViewById(R.id.curr_time);
        mTotalTime = findViewById(R.id.total_time);
        mPlayTitle = findViewById(R.id.tv_info_name);
        tvDate = findViewById(R.id.tv_info_time);
        mSeekBar = findViewById(R.id.seekBar);
        mProgressRoot = findViewById(R.id.tv_progress_container);
        mProgressIcon = findViewById(R.id.tv_progress_icon);
        mProgressText = findViewById(R.id.tv_progress_text);
        mTopRoot = findViewById(R.id.top_container);
        mBottomRoot = findViewById(R.id.bottom_container);
        mParseRoot = findViewById(R.id.parse_root);
        mGridView = findViewById(R.id.mGridView);
        mPlayerRetry = findViewById(R.id.play_retry);
        mPlayPause = findViewById(R.id.play_pause);
        mNextBtn = findViewById(R.id.play_next);
        mPreBtn = findViewById(R.id.play_pre);
        mPlayerScaleBtn = findViewById(R.id.play_scale);
        mPlayerSpeedBtn = findViewById(R.id.play_speed);
        mPlayerBtn = findViewById(R.id.play_player);
        mPlayerIJKBtn = findViewById(R.id.play_ijk);
        m3rdPlayerBtn = findViewById(R.id.play_3rdplayer);
        mPlayerTimeStartBtn = findViewById(R.id.play_time_start);
        mPlayerTimeSkipBtn = findViewById(R.id.play_time_end);
        mPlayerTimeStepBtn = findViewById(R.id.play_time_step);
        loadingSpeed = findViewById(R.id.loadingSpeed);
        tvVideoInfo = findViewById(R.id.tv_video_info);
        finishAt = findViewById(R.id.tv_finish_at);
        btnHint = findViewById(R.id.play_btn_hint);
        lockerLeft = findViewById(R.id.play_screen_lock_left);
        lockerRight = findViewById(R.id.play_screen_lock_right);
        tvBack = findViewById(R.id.tv_back);
        playAudio = findViewById(R.id.play_audio);

        mGridView.setLayoutManager(new V7LinearLayoutManager(getContext(), 0, false));

        parseAdapter = new ParseAdapter();
        parseAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                ParseBean parseBean = parseAdapter.getItem(position);
                // 当前默认解析需要刷新
                int currentDefault = parseAdapter.getData().indexOf(ApiConfig.get().getDefaultParse());
                parseAdapter.notifyItemChanged(currentDefault);
                ApiConfig.get().setDefaultParse(parseBean);
                parseAdapter.notifyItemChanged(position);
                listener.changeParse(parseBean);
                hideBottom();
            }
        });
        mGridView.setAdapter(parseAdapter);
        parseAdapter.setNewData(ApiConfig.get().getParseBeanList());

        mParseRoot.setVisibility(VISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * progress) / seekBar.getMax();
                if (mCurrentTime != null)
                    mCurrentTime.setText(stringForTime((int) newPosition));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragging = true;
                mControlWrapper.stopProgress();
                mControlWrapper.stopFadeOut();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                long duration = mControlWrapper.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / seekBar.getMax();
                mControlWrapper.seekTo((int) newPosition);
                mIsDragging = false;
                mControlWrapper.startProgress();
                mControlWrapper.startFadeOut();
            }
        });
        mPlayerRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.replay();
                hideBottom();
            }
        });
        mPlayPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePlay();
            }
        });
        mNextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.playNext(false);
                hideBottom();
            }
        });
        mPreBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.playPre();
                hideBottom();
            }
        });
        mPlayerScaleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int scaleType = mPlayerConfig.getInt("sc");
                    scaleType++;
                    if (scaleType > 5)
                        scaleType = 0;
                    mPlayerConfig.put("sc", scaleType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setScreenScaleType(scaleType);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerSpeedBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    float speed = (float) mPlayerConfig.getDouble("sp");
                    speed += 0.25f;
                    if (speed > 3)
                        speed = 0.5f;
                    mPlayerConfig.put("sp", speed);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    mControlWrapper.setSpeed(speed);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int playerType = mPlayerConfig.getInt("pl");
                    Integer[] playerTypes = PlayerHelper.getAvailableDefaultPlayerTypes();
                    for (int i = 0; i <playerTypes.length; i++) {
                        if(playerTypes[i] != playerType)
                            continue;
                        else if(i + 1 < playerTypes.length) {
                            playerType = playerTypes[i+1];
                            break;
                        } else {
                            playerType = playerTypes[0];
                        }
                    }
                    mPlayerConfig.put("pl", playerType);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay();
                    // hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerIJKBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String ijk = mPlayerConfig.getString("ijk");
                    List<IJKCode> codecs = ApiConfig.get().getIjkCodes();
                    for (int i = 0; i < codecs.size(); i++) {
                        if (ijk.equals(codecs.get(i).getName())) {
                            if (i >= codecs.size() - 1)
                                ijk = codecs.get(0).getName();
                            else {
                                ijk = codecs.get(i + 1).getName();
                            }
                            break;
                        }
                    }
                    mPlayerConfig.put("ijk", ijk);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                    listener.replay();
                    hideBottom();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeStartBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                    int st = mPlayerConfig.getInt("st");
                    st += step;
                    if (st > 60 * 10)
                        st = 0;
                    mPlayerConfig.put("st", st);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeStartBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("st", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        mPlayerTimeSkipBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                    int et = mPlayerConfig.getInt("et");
                    et += step;
                    if (et > 60 * 10)
                        et = 0;
                    mPlayerConfig.put("et", et);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        mPlayerTimeSkipBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    mPlayerConfig.put("et", 0);
                    updatePlayerCfgView();
                    listener.updatePlayerCfg();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });

        mPlayerTimeStepBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int step = Hawk.get(HawkConfig.PLAY_TIME_STEP, 5);
                step += 5;
                if (step > 30) {
                    step = 5;
                }
                Hawk.put(HawkConfig.PLAY_TIME_STEP, step);
                updatePlayerCfgView();
            }
        });
        mPlayerTimeStepBtn.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Hawk.put(HawkConfig.PLAY_TIME_STEP, 5);
                updatePlayerCfgView();
                return true;
            }
        });

        m3rdPlayerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Integer[] types = PlayerHelper.getAvailable3rdPlayerTypes();
                if(types.length > 0) {
                    Integer selectedType = Hawk.get(HawkConfig.THIRD_PARTY_PLAYER, types[0]);
                    VodController.this.playerFragment.playInOtherPlayer(selectedType);
                }
            }
        });
        tvDate.post(new Runnable() {
            @Override
            public void run() {
                mHandler.post(mRunnable);
            }
        });
        lockerLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLockController();
            }
        });
        lockerRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLockController();
            }
        });
        if(Hawk.get(HawkConfig.TV_TYPE, 0) == 0) {
            tvBack.setVisibility(GONE);
        } else {
            tvBack.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    enableController(false);
                    stopFullScreen();
                }
            });
        }
        init3rdPlayerButton();
        playAudio.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.setAudioTrack();
            }
        });
    }

    public void init3rdPlayerButton() {
        PlayerHelper.reload3rdPlayers();
        Integer[] types = PlayerHelper.getAvailable3rdPlayerTypes();
        if(types.length <= 0) {
            m3rdPlayerBtn.setVisibility(View.GONE);
        } else {
            m3rdPlayerBtn.setVisibility(View.VISIBLE);
            Integer selectedType = Hawk.get(HawkConfig.THIRD_PARTY_PLAYER, types[0]);
            if(Arrays.binarySearch(types, selectedType) < 0)
                Hawk.put(HawkConfig.THIRD_PARTY_PLAYER, types[0]);
            m3rdPlayerBtn.setText(PlayerHelper.get3rdPlayerName(selectedType));
        }
    }

    private void sendScreenChange(boolean isFullscreen) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "detail");
        jsonObject.addProperty("fullscreen", isFullscreen);
        ControlManager.get().getSocketServer().sendToAll(jsonObject);
    }

    public void enableController(boolean enable) {
        this.shouldShowBottom = enable;
        this.mPlayTitle.setVisibility(enable ? VISIBLE : GONE);
        this.tvDate.setVisibility(enable ? VISIBLE : GONE );
        if(!enable) {
            hideLocker();
            hideBottom();
        }
        setDoubleTapTogglePlayEnabled(enable);
        setGestureEnabled(enable);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.player_vod_control_view;
    }

    public void showParse(boolean userJxList) {
        mParseRoot.setVisibility(userJxList ? VISIBLE : GONE);
    }

    private JSONObject mPlayerConfig = null;


    public void setPlayerConfig(JSONObject playerCfg) {
        this.mPlayerConfig = playerCfg;
        listener.updatePlayerCfg();
        updatePlayerCfgView();
    }

    public JSONObject getPlayerConfig() {
        return this.mPlayerConfig;
    }

    public void notifyPlayerConfigUpdate(String changedOption) {
        if(listener != null)
            listener.updatePlayerCfg();
        if(changedOption.equals("pl") || changedOption.equals("ijk"))
            listener.replay();
        if(changedOption.equals("sc")) {
            try {
                mControlWrapper.setScreenScaleType(this.mPlayerConfig.getInt("sc"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(changedOption.equals("sp")) {
            try {
                float speed = (float) mPlayerConfig.getDouble("sp");
                mControlWrapper.setSpeed(speed);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    void updatePlayerCfgView() {
        try {
            int playerType = mPlayerConfig.getInt("pl");
            mPlayerBtn.setText(PlayerHelper.getPlayerName(playerType));
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerIJKBtn.setText(mPlayerConfig.getString("ijk"));
            mPlayerIJKBtn.setVisibility(playerType == 1 ? VISIBLE : GONE);
            mPlayerScaleBtn.setText(PlayerHelper.getScaleName(mPlayerConfig.getInt("sc")));
            mPlayerSpeedBtn.setText("x" + mPlayerConfig.getDouble("sp"));
            mPlayerTimeStartBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("st") * 1000));
            mPlayerTimeSkipBtn.setText(PlayerUtils.stringForTime(mPlayerConfig.getInt("et") * 1000));
            mPlayerTimeStepBtn.setText(Hawk.get(HawkConfig.PLAY_TIME_STEP, 5) + "s");
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "vod-update-info");
            obj.addProperty("playerCfg", mPlayerConfig.toString());
            if(enabledWebRemote)
                ControlManager.get().getSocketServer().sendToAll(obj);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateParser(int index) {
        ParseBean parseBean = parseAdapter.getItem(index);
        // 当前默认解析需要刷新
        int currentDefault = parseAdapter.getData().indexOf(ApiConfig.get().getDefaultParse());
        parseAdapter.notifyItemChanged(currentDefault);
        ApiConfig.get().setDefaultParse(parseBean);
        parseAdapter.notifyItemChanged(index);
        listener.changeParse(parseBean);
    }

    public void setTitle(String playTitleInfo) {
        mPlayTitle.setText(playTitleInfo);
    }

    public void resetSpeed() {
        skipEnd = true;
        mHandler.removeMessages(1004);
        mHandler.sendEmptyMessageDelayed(1004, 100);
    }

    public interface VodControlListener {
        void playNext(boolean rmProgress);

        void playPre();

        void changeParse(ParseBean pb);

        void updatePlayerCfg();

        void replay();

        void errReplay();

        void setAudioTrack();

        void setSubtitleTrack();
    }

    public void setListener(VodControlListener listener) {
        this.listener = listener;
    }

    private VodControlListener listener;

    private boolean skipEnd = true;

    public void setProgress(double percent) {
        mControlWrapper.seekTo((int)(mControlWrapper.getDuration() * (percent / 100)));
        mControlWrapper.startProgress();
    }

    @Override
    protected void setProgress(int duration, int position) {
        if (mIsDragging) {
            return;
        }
        if(enabledWebRemote) {
            progressData.addProperty("dur", duration);
            progressData.addProperty("pos", position);
            ControlManager.get().getSocketServer().sendToAll(progressData);
        }
        super.setProgress(duration, position);
        if (skipEnd && position != 0 && duration != 0) {
            int et = 0;
            try {
                et = mPlayerConfig.getInt("et");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (et > 0 && position + (et * 1000) >= duration) {
                skipEnd = false;
                listener.playNext(true);
            }
        }
        mCurrentTime.setText(PlayerUtils.stringForTime(position));
        mTotalTime.setText(PlayerUtils.stringForTime(duration));
        if (duration > 0) {
            mSeekBar.setEnabled(true);
            int pos = (int) (position * 1.0 / duration * mSeekBar.getMax());
            mSeekBar.setProgress(pos);
        } else {
            mSeekBar.setEnabled(false);
        }
        int percent = mControlWrapper.getBufferedPercentage();
        if (percent >= 95) {
            mSeekBar.setSecondaryProgress(mSeekBar.getMax());
        } else {
            mSeekBar.setSecondaryProgress(percent * 10);
        }
    }

    private boolean simSlideStart = false;
    private int simSeekPosition = 0;
    private long simSlideOffset = 0;

    public void tvSlideStop() {
        if (!simSlideStart)
            return;
        mControlWrapper.seekTo(simSeekPosition);
        if (!mControlWrapper.isPlaying())
            mControlWrapper.start();
        simSlideStart = false;
        simSeekPosition = 0;
        simSlideOffset = 0;
    }

    public void tvSlideStart(int dir) {
        int duration = (int) mControlWrapper.getDuration();
        if (duration <= 0)
            return;
        if (!simSlideStart) {
            simSlideStart = true;
        }
        // 每次10秒
        simSlideOffset += (10000.0f * dir);
        int currentPosition = (int) mControlWrapper.getCurrentPosition();
        int position = (int) (simSlideOffset + currentPosition);
        if (position > duration) position = duration;
        if (position < 0) position = 0;
        updateSeekUI(currentPosition, position, duration);
        simSeekPosition = position;
    }

    @Override
    protected void updateSeekUI(int curr, int seekTo, int duration) {
        super.updateSeekUI(curr, seekTo, duration);
        if (seekTo > curr) {
            mProgressIcon.setImageResource(R.drawable.icon_pre);
        } else {
            mProgressIcon.setImageResource(R.drawable.icon_back);
        }
        mProgressText.setText(PlayerUtils.stringForTime(seekTo) + " / " + PlayerUtils.stringForTime(duration));
        mHandler.sendEmptyMessage(1000);
        mHandler.removeMessages(1001);
        mHandler.sendEmptyMessageDelayed(1001, 1000);
    }

    @Override
    protected void onPlayStateChanged(int playState) {
        super.onPlayStateChanged(playState);
        if(enabledWebRemote && playState <= 4) {
            progressData.addProperty("state", playState);
            ControlManager.get().getSocketServer().sendToAll(progressData);
        }
        switch (playState) {
            case VideoView.STATE_IDLE:
                break;
            case VideoView.STATE_PLAYING:
                this.mPlayPause.setImageResource(R.drawable.icon_vodcontroller_pause);
                startProgress();
                break;
            case VideoView.STATE_PAUSED:
                this.mPlayPause.setImageResource(R.drawable.icon_vodcontroller_play);
                break;
            case VideoView.STATE_ERROR:
                this.loadingSpeed.setVisibility(GONE);
                listener.errReplay();
                break;
            case VideoView.STATE_PREPARED:
                if(mControlWrapper.getVideoSize().length >= 2) {
                    String resolution = mControlWrapper.getVideoSize()[0] + " x " + mControlWrapper.getVideoSize()[1];
                    tvVideoInfo.setText(resolution);
                }
            case VideoView.STATE_BUFFERED:
                this.loadingSpeed.setVisibility(GONE);
                break;
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_BUFFERING:
                if(shouldShowLoadingSpeed)
                    this.loadingSpeed.setVisibility(VISIBLE);
                break;
            case VideoView.STATE_PLAYBACK_COMPLETED:
                listener.playNext(true);
                break;
        }
    }

    boolean isBottomVisible() {
        return mBottomRoot.getVisibility() == VISIBLE;
    }

    void showBottom() {
        if(this.shouldShowBottom) {
            mHandler.removeMessages(1003);
            mHandler.sendEmptyMessage(1002);
            mHandler.postDelayed(mHideBottomRunnable, 10000);
        }
    }

    void showControllerAnimation() {
        mTopRoot.setVisibility(VISIBLE);
        mTopRoot.setAlpha(0);
        mTopRoot.setTranslationY(-mTopRoot.getHeight()/2);
        mTopRoot.animate()
                .translationY(0)
                .alpha(1.0f)
                .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();

        mBottomRoot.setVisibility(VISIBLE);
        mBottomRoot.setAlpha(0);
        mBottomRoot.setTranslationY(mBottomRoot.getHeight()/2);
        mBottomRoot.animate()
                .translationY(0)
                .alpha(1.0f).setDuration(300)
                .setInterpolator(new DecelerateInterpolator()).start();

        showLocker();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNextBtn.requestFocus();
            }
        }, 300);

    }

    void hideControllerAnimation() {
        mTopRoot.animate()
                .translationYBy(-mTopRoot.getHeight()/2)
                .alpha(0)
                .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();

        mBottomRoot.animate()
                .translationYBy(mBottomRoot.getHeight()/2)
                .alpha(0)
                .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();

        //mBottomRoot.requestFocus();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mTopRoot.setTranslationY(0);
                mTopRoot.setVisibility(GONE);
                mBottomRoot.setTranslationY(0);
                mBottomRoot.setVisibility(GONE);
            }
        }, 300);
    }

    Runnable mHideBottomRunnable = new Runnable() {
        @Override
        public void run() {
            hideBottom();
        }
    };

    void hideBottom() {
        mHandler.removeMessages(1002);
        mHandler.sendEmptyMessage(1003);
        mHandler.removeCallbacks(mHideBottomRunnable);
        mHandler.removeCallbacks(hideBtnHintRunnable);
        mHandler.post(hideBtnHintRunnable);
    }

    void showBtnHint(View focusedView) {
        long postDelay = 300;
        if(btnHint.getVisibility() == VISIBLE) {
            btnHint.clearAnimation();
            btnHint.animate().alpha(0).setDuration(300).start();
        }
        if(focusedView == mPlayPause) {
            doShowHint(mPlayPause, "播放/暂停", postDelay);
        } else if(focusedView == mPreBtn) {
            doShowHint(mPreBtn, "上一集", postDelay);
        } else if(focusedView == mNextBtn) {
            doShowHint(mNextBtn, "下一集", postDelay);
        } else if(focusedView == mPlayerRetry) {
            doShowHint(mPlayerRetry, "重新加载本集", postDelay);
        } else if(focusedView == mPlayerSpeedBtn) {
            doShowHint(mPlayerSpeedBtn, "播放速度", postDelay);
        } else if(focusedView == mPlayerScaleBtn) {
            doShowHint(mPlayerScaleBtn, "画面比例", postDelay);
        } else if(focusedView == mPlayerBtn) {
            doShowHint(mPlayerBtn, "播放器", postDelay);
        } else if(focusedView == mPlayerIJKBtn) {
            doShowHint(mPlayerIJKBtn, "IJK解码器", postDelay);
        } else if(focusedView == m3rdPlayerBtn) {
            doShowHint(m3rdPlayerBtn, "第三方播放器", postDelay);
        } else if(focusedView == mPlayerTimeStartBtn) {
            doShowHint(mPlayerTimeStartBtn, "跳过片头", postDelay);
        } else if(focusedView == mPlayerTimeSkipBtn) {
            doShowHint(mPlayerTimeSkipBtn, "跳过片尾", postDelay);
        } else if(focusedView == mPlayerTimeStepBtn) {
            doShowHint(mPlayerTimeSkipBtn, "用于设置跳过片头/片尾的步速", postDelay);
        } else if(focusedView == playAudio) {
            doShowHint(playAudio, "选择音轨", postDelay);
        } else {
            mHandler.post(hideBtnHintRunnable);
        }
    }

    private void doShowHint(View focusedView, String hintText, long postDelay) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bringChildToFront(btnHint);
                btnHint.setText(hintText);
                btnHint.setVisibility(VISIBLE);
                btnHint.post(new Runnable() {
                    @Override
                    public void run() {
                        updateHintPosition(focusedView);
                        btnHint.setAlpha(0f);
                        btnHint.clearAnimation();
                        btnHint.animate().alpha(1f).setDuration(1000).start();
                        mHandler.removeCallbacks(hideBtnHintRunnable);
                        mHandler.postDelayed(hideBtnHintRunnable, 7000);
                    }
                });
            }
        }, postDelay);
    }

    private void updateHintPosition(View focusedView) {
        int[] location = new int[2];
        focusedView.getLocationOnScreen(location);
        btnHint.setTranslationX(location[0]);
        btnHint.setTranslationY(location[1] - btnHint.getMeasuredHeight() - 20);
    }

    private Runnable hideBtnHintRunnable = new Runnable() {
        @Override
        public void run() {
            btnHint.clearAnimation();
            btnHint.animate().alpha(0).setDuration(1000).start();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btnHint.setVisibility(GONE);
                }
            }, 1000);
        }
    };

    private void toggleLockController() {
        if(deviceType == 0)
            return;
        if(isControllerLock) {
            isControllerLock = false;
            lockerLeft.setImageResource(R.drawable.icon_vodcontroller_unlock);
            lockerRight.setImageResource(R.drawable.icon_vodcontroller_unlock);
            showBottom();
            mControlWrapper.setLocked(false);
        } else {
            isControllerLock = true;
            lockerLeft.setImageResource(R.drawable.icon_vodcontroller_lock);
            lockerRight.setImageResource(R.drawable.icon_vodcontroller_lock);
            hideBottom();
            mControlWrapper.setLocked(true);
        }
    }

    public void showLocker() {
        if(deviceType == 0)
            return;
        isShowingLocker = true;
        lockerLeft.setVisibility(VISIBLE);
        lockerRight.setVisibility(VISIBLE);
        mHandler.removeCallbacks(hideLockerRunnable);
        mHandler.postDelayed(hideLockerRunnable, 10000);
    }

    public void hideLocker() {
        isShowingLocker = false;
        lockerLeft.setVisibility(GONE);
        lockerRight.setVisibility(GONE);
    }

    private Runnable hideLockerRunnable = new Runnable() {
        @Override
        public void run() {
            hideLocker();
        }
    };

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (super.onKeyEvent(event)) {
            return true;
        }
        if (isBottomVisible()) {
            if(event.getKeyCode() != KeyEvent.KEYCODE_BACK)
                showBtnHint(this.findFocus());
            else if(Hawk.get(HawkConfig.TV_TYPE, 0) == 0) {
                hideBottom();
                return true;
            }
            mHandler.removeCallbacks(hideLockerRunnable);
            mHandler.removeCallbacks(mHideBottomRunnable);
            mHandler.postDelayed(mHideBottomRunnable, 10000);
            mHandler.postDelayed(hideLockerRunnable, 10000);
            return super.dispatchKeyEvent(event);
        }
        boolean isInPlayback = isInPlaybackState();
        int keyCode = event.getKeyCode();
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStart(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (isInPlayback) {
                    togglePlay();
                    return true;
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!isBottomVisible()) {
                    showBottom();
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (isInPlayback) {
                    tvSlideStop();
                    return true;
                }
            }
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(isControllerLock) {
                    toggleLockController();
                    return true;
                }
                if(isFullScreen()) {
                    stopFullScreen();
                    return true;
                }
            }
        }
        if(playerFragment.getVodController().isFullScreen()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == KeyEvent.KEYCODE_ENTER) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if(isControllerLock) {
            if(!isShowingLocker)
                showLocker();
            else
                hideLocker();
            return true;
        }
        if(!shouldShowBottom) {
            togglePlay();
            return true;
        }
        if (!isBottomVisible()) {
            showBottom();
        } else {
            hideBottom();
        }
        return true;
    }

    @Override
    public boolean startFullScreen() {
        enableController(true);
        sendScreenChange(true);
        return super.startFullScreen();
    }

    @Override
    public boolean stopFullScreen() {
        hideBottom();
        hideLocker();
        enableController(false);
        sendScreenChange(false);
        return super.stopFullScreen();
    }

    public boolean isFullScreen() {
        return mControlWrapper.isFullScreen();
    }

    @Override
    public boolean onBackPressed() {
        if(isControllerLock) {
            toggleLockController();
            return true;
        }
        if (super.onBackPressed()) {
            return true;
        }
        if (isBottomVisible()) {
            hideBottom();
            return true;
        }
        return false;
    }
}
