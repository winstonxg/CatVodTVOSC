package com.github.tvbox.osc.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import androidx.viewpager.widget.ViewPager;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.fragment.homes.AbstractHomeFragment;
import com.github.tvbox.osc.ui.fragment.homes.AssembledFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.AppUpdate;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {

    private NoScrollViewPager mViewPager;
    private HomePageAdapter pageAdapter;
    private AbstractHomeFragment currentHomeFragment;
    private static final int HOME_FRAME_ID = 9999997;

    private boolean isChaningApi = false;
    private Handler mHandler = new Handler();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        ControlManager.get().startServer();
        new AppUpdate().CheckLatestVersion(this, false, null);
        //mFrame = findViewById(R.id.mFrame);
        mViewPager = findViewById(R.id.mHomeViewPager);
        String homeStyleClassName = Hawk.get(HawkConfig.HOME_VIEW_STYLE, AbstractHomeFragment.getManagedHomeFragments().get(0).getClassName());
        try {
            currentHomeFragment = (AbstractHomeFragment) Class.forName("com.github.tvbox.osc.ui.fragment.homes." + homeStyleClassName).newInstance();
        } catch (Exception e) {
            currentHomeFragment = new AssembledFragment();
        }
        //FrameLayout tempFrame = new FrameLayout(this);
        //tempFrame.setId(HOME_FRAME_ID);
        //mFrame.addView(tempFrame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
//        getSupportFragmentManager().beginTransaction()
//                .add(R.id.mFrame, currentHomeFragment, PlayerFragment.FRAGMENT_TAG).disallowAddToBackStack().commit();
        currentHomeFragment.useCacheConfig = false;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            currentHomeFragment.useCacheConfig = bundle.getBoolean("useCache", false);
        }
        List<BaseLazyFragment> fragments = new ArrayList<>();
        fragments.add(currentHomeFragment);
        pageAdapter = new HomePageAdapter(this.getSupportFragmentManager(), fragments);
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            FixedSpeedScroller scroller = new FixedSpeedScroller(mContext, new AccelerateInterpolator());
            field.set(mViewPager, scroller);
            scroller.setmDuration(300);
        } catch (Exception e) {
        }
        mViewPager.setPageTransformer(true, new DefaultTransformer());
        mViewPager.setAdapter(pageAdapter);
        mViewPager.setCurrentItem(0, false);
    }

    @Override
    public void onBackPressed() {
        if(currentHomeFragment.pressBack())
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentHomeFragment.updateScreenTime();
    }


    @Override
    protected void onPause() {
        super.onPause();
        currentHomeFragment.pauseHandler();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_PUSH_URL) {
            if (ApiConfig.get().getSource("push_agent") != null) {
                Intent newIntent = new Intent(mContext, DetailActivity.class);
                newIntent.putExtra("id", (String) event.obj);
                newIntent.putExtra("sourceKey", "push_agent");
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                HomeActivity.this.startActivity(newIntent);
            }
        } else if(event.type == RefreshEvent.HOME_BEAN_QUICK_CHANGE) {
            isChaningApi = true;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean superDKE = super.dispatchKeyEvent(event);
        boolean fragDKE = currentHomeFragment.dispatchKey(event);
        return superDKE || fragDKE;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(currentHomeFragment != null)
            currentHomeFragment.onDestroy();
        EventBus.getDefault().unregister(this);
        if(!isChaningApi) {
            AppManager.getInstance().appExit(0);
            ControlManager.get().stopServer();
        }
    }

}