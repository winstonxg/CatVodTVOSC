package com.github.tvbox.osc.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.ui.fragment.homes.AbstractHomeFragment;
import com.github.tvbox.osc.ui.fragment.homes.AssembledFragment;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.AppUpdate;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeActivity extends BaseActivity {

    private FrameLayout mFrame;
    private AbstractHomeFragment currentHomeFragment;
    private static final int HOME_FRAME_ID = 9999997;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_home;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        ControlManager.get().startServer();
        new AppUpdate().CheckLatestVersion(this, false, null);
        mFrame = findViewById(R.id.mFrame);
        currentHomeFragment = new AssembledFragment();
        FrameLayout tempFrame = new FrameLayout(this);
        tempFrame.setId(HOME_FRAME_ID);
        mFrame.addView(tempFrame, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        getSupportFragmentManager().beginTransaction()
                .add(HOME_FRAME_ID, currentHomeFragment, PlayerFragment.FRAGMENT_TAG).disallowAddToBackStack().commit();
        currentHomeFragment.useCacheConfig = false;
        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            currentHomeFragment.useCacheConfig = bundle.getBoolean("useCache", false);
        }
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
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return currentHomeFragment.dispatchKey(event) && super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        AppManager.getInstance().appExit(0);
        ControlManager.get().stopServer();
    }

}