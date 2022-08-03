package com.github.tvbox.osc.ui.fragment;

import android.os.Handler;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentContainerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.activity.CollectActivity;
import com.github.tvbox.osc.ui.activity.DriveActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.PushActivity;
import com.github.tvbox.osc.ui.activity.RecommendActivity;
import com.github.tvbox.osc.ui.activity.SearchActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.LOG;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author pj567
 * @date :2021/3/9
 * @description:
 */
public class UserFragment extends BaseLazyFragment implements View.OnClickListener {
    private LinearLayout tvVod;
    private LinearLayout tvLive;
    private LinearLayout tvSearch;
    private LinearLayout tvSetting;
    private LinearLayout tvPush;
    private LinearLayout tvFavorite;
    private LinearLayout tvDouban;
    private LinearLayout tvDrive;
    private FragmentContainerView selfView;
    private boolean anyItemFocused = false;
    private boolean hasScheduled = false;
    private boolean showVod = false;
    private final Handler userFragmentHandler = new Handler();

    public static UserFragment newInstance() {
        return new UserFragment();
    }

    public View.OnClickListener vodClickListener = null;

    public UserFragment() {
        this.showVod = false;
    }

    public UserFragment(boolean showVod) {
        this.showVod = showVod;
    }

    @Override
    protected int getLayoutResID() { return R.layout.fragment_user_layout; }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        tvVod = findViewById(R.id.tvVod);
        tvLive = findViewById(R.id.tvLive);
        tvSearch = findViewById(R.id.tvSearch);
        tvSetting = findViewById(R.id.tvSetting);
        tvPush = findViewById(R.id.tvPush);
        tvFavorite = findViewById(R.id.tvFavorite);
        tvDouban = findViewById(R.id.tvDouban);
        tvDrive = findViewById(R.id.tvDrive);
        tvVod.setOnClickListener(this);
        tvLive.setOnClickListener(this);
        tvSearch.setOnClickListener(this);
        tvSetting.setOnClickListener(this);
        tvPush.setOnClickListener(this);
        tvFavorite.setOnClickListener(this);
        tvDouban.setOnClickListener(this);
        tvDrive.setOnClickListener(this);
        tvVod.setOnFocusChangeListener(focusChangeListener);
        tvLive.setOnFocusChangeListener(focusChangeListener);
        tvSearch.setOnFocusChangeListener(focusChangeListener);
        tvSetting.setOnFocusChangeListener(focusChangeListener);
        tvPush.setOnFocusChangeListener(focusChangeListener);
        tvFavorite.setOnFocusChangeListener(focusChangeListener);
        tvDouban.setOnFocusChangeListener(focusChangeListener);
        tvDrive.setOnFocusChangeListener(focusChangeListener);;
        updateShowVod(this.showVod);
    }

    private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus)
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            else
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            anyItemFocused = hasFocus;
            if(!hasScheduled) {
                hasScheduled = true;
                userFragmentHandler.postDelayed(mFeatureViewRunnable, 10);
            }
        }
    };

    public void updateShowVod(boolean showVod) {
        this.showVod = showVod;
        if(tvVod != null) {
            tvVod.setVisibility(showVod ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        FastClickCheckUtil.check(v);
        if(v.getId() == R.id.tvVod && vodClickListener != null) {
            vodClickListener.onClick(v);
        } else if (v.getId() == R.id.tvLive) {
            jumpActivity(LivePlayActivity.class);
        } else if (v.getId() == R.id.tvSearch) {
            jumpActivity(SearchActivity.class);
        } else if (v.getId() == R.id.tvSetting) {
            jumpActivity(SettingActivity.class);
        } else if (v.getId() == R.id.tvPush) {
            jumpActivity(PushActivity.class);
        } else if (v.getId() == R.id.tvFavorite) {
            jumpActivity(CollectActivity.class);
        } else if (v.getId() == R.id.tvDouban) {
            jumpActivity(RecommendActivity.class);
        } else if(v.getId() == R.id.tvDrive) {
            jumpActivity(DriveActivity.class);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_CONNECTION) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void SetFragmentView(FragmentContainerView view) {
        this.selfView = view;
    }

    private final Runnable mFeatureViewRunnable = new Runnable() {
        @Override
        public void run() {
            if(selfView != null) {
                try {
                    selfView.getOnFocusChangeListener().onFocusChange(selfView, anyItemFocused);
                }catch (Exception ex) {}
                hasScheduled = false;
            }
        }
    };
}