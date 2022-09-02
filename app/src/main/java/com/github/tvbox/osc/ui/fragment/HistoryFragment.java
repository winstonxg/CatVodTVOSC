package com.github.tvbox.osc.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.HistoryAdapter;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends BaseLazyFragment {

    private LinearLayout tvDelTipLayout;
    private TextView tvDelTip;
    private TvRecyclerView mGridView;
    private HistoryAdapter historyAdapter;
    private List<VodInfo> allVodRecord;
    private boolean delMode = false;
    private boolean shouldShowDelHint = false;
    private int presentingSize = -1;
    private int loadSize = 100;

    private static final String defaultDelMsg = "长按任意影视项激活删除模式";
    private static final String enabledDelMsg = "点击影视项删除该纪录，返回键退出删除模式";

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    public HistoryFragment() {}

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_history;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    public boolean isInDelMode() {
        return delMode;
    }

    public void shouldShowDelHint(boolean shouldShow) {
        this.shouldShowDelHint = shouldShow;
        if(this.tvDelTipLayout != null) {
            this.tvDelTipLayout.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    public void toggleDelMode() {
        delMode = !delMode;
        historyAdapter.toggleDelMode(delMode);
        tvDelTip.setText(delMode ? enabledDelMsg : defaultDelMsg);
        tvDelTip.setTextColor(getResources().getColor(delMode ? R.color.color_FF0057 : R.color.color_CCFFFFFF));
    }

    public void setLoadSize(int loadSize) {
        this.loadSize = loadSize;
        if(rootView != null)
            initData();
    }

    public int getLoadSize() {
        return loadSize;
    }

    public int getAllRecordSize() {
        if(this.allVodRecord == null)
            return 0;
        return this.allVodRecord.size();
    }

    public int getPresentingSize() {
        return presentingSize;
    }

    private void initView() {
        EventBus.getDefault().register(this);
        tvDelTip = findViewById(R.id.tvDelTip);
        tvDelTipLayout = findViewById(R.id.tvDelTipLayout);
        tvDelTip.setText(defaultDelMsg);
        tvDelTipLayout.setVisibility(shouldShowDelHint ? View.VISIBLE : View.GONE);
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);
        V7GridLayoutManager layoutManager = new V7GridLayoutManager(this.mContext, !shouldMoreColumns() ? 5 : 6);
        mGridView.setLayoutManager(layoutManager);
        historyAdapter = new HistoryAdapter();
        mGridView.setAdapter(historyAdapter);
        historyAdapter.bindToRecyclerView(mGridView);
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.findViewById(R.id.mItemFrame).setBackgroundColor(Color.TRANSPARENT);
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.findViewById(R.id.mItemFrame).setBackground(getResources().getDrawable(R.drawable.shape_user_focus));
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        historyAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                if(!delMode) {
                    toggleDelMode();
                    return true;
                }
                return false;
            }
        });
        historyAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                VodInfo vodInfo = historyAdapter.getData().get(position);

                if (vodInfo != null) {
                    if (delMode) {
                        historyAdapter.remove(position);
                        RoomDataManger.deleteVodRecord(vodInfo.sourceKey, vodInfo);
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", vodInfo.id);
                        bundle.putString("sourceKey", vodInfo.sourceKey);
                        jumpActivity(DetailActivity.class, bundle);
                    }
                }
            }
        });
    }

    private void initData() {
        allVodRecord = RoomDataManger.getAllVodRecord(100);
        List<VodInfo> vodInfoList = new ArrayList<>();
        int recordCount = 0;
        for (VodInfo vodInfo : allVodRecord) {
            vodInfoList.add(vodInfo);
            if(++recordCount >= loadSize)
                break;
        }
        presentingSize = vodInfoList.size();
        if(tvDelTipLayout != null) {
            if (presentingSize == 0)
                tvDelTipLayout.setVisibility(View.GONE);
            else
                tvDelTipLayout.setVisibility(shouldShowDelHint ? View.VISIBLE : View.GONE);
        }
        historyAdapter.setNewData(vodInfoList);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            initData();
            if(delMode && allVodRecord.size() > 0) {
                mGridView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGridView.setSelection(0);
                    }
                }, 10);
            }
        }
    }
}
