package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.RecommendGridAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.viewmodel.RecommendViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class RecommendActivity extends BaseActivity {

    private SortAdapter sortAdapter;
    private TvRecyclerView recomGridView;
    private NoScrollViewPager recomViewPager;
    private List<BaseLazyFragment> fragments = new ArrayList<>();
    private RecommendViewModel sourceViewModel;
    private HomePageAdapter pageAdapter;

    private boolean isRight;
    private boolean sortChange = false;

    private int currentSelected = 0;
    private int sortFocused = 0;
    public View sortFocusView = null;

    private Handler mHandler = new Handler();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_recommend;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        this.recomGridView = findViewById(R.id.recomGridView);
        this.recomViewPager = findViewById(R.id.recomViewPager);
        this.sortAdapter = new SortAdapter();
        this.recomGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.VERTICAL, false));
        this.recomGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 2.0f));
        this.recomGridView.setAdapter(this.sortAdapter);
        this.recomGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null && !isRight) {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(false);
                    textView.setTextColor(getResources().getColor(R.color.color_BBFFFFFF));
                    textView.invalidate();
                    view.findViewById(R.id.tvFilter).setVisibility(View.GONE);
                    view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                }
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null) {
                    if(sortFocusView != null) {
                        sortFocusView.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                    }
                    isRight = false;
                    sortChange = true;
                    view.animate().scaleX(1.1f).scaleY(1.1f).setInterpolator(new BounceInterpolator()).setDuration(300).start();
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(true);
                    textView.setTextColor(getResources().getColor(R.color.color_FFFFFF));
                    textView.invalidate();
                    if (!sortAdapter.getItem(position).filters.isEmpty())
                        view.findViewById(R.id.tvFilter).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                    sortFocusView = view;
                    sortFocused = position;
                    mHandler.removeCallbacks(mDataRunnable);
                    mHandler.postDelayed(mDataRunnable, 200);
                }
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                if (itemView != null && currentSelected == position && !sortAdapter.getItem(position).filters.isEmpty()) { // 弹出筛选
                    BaseLazyFragment baseLazyFragment = fragments.get(currentSelected);
                    if ((baseLazyFragment instanceof GridFragment)) {
                        ((GridFragment) baseLazyFragment).showFilter();
                    }
                }
            }
        });
        this.recomGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            public final boolean onInBorderKeyEvent(int direction, View view) {
                if(direction == View.FOCUS_RIGHT) {
                    view.findViewById(R.id.tvFocusedBar).setVisibility(View.VISIBLE);
                    isRight = true;
                }
                if (direction != View.FOCUS_DOWN) {
                    return false;
                }
                BaseLazyFragment baseLazyFragment = fragments.get(sortFocused);
                if (!(baseLazyFragment instanceof GridFragment)) {
                    return false;
                }
                return !((GridFragment) baseLazyFragment).isLoad();
            }
        });
    }

    private void initData() {
        sourceViewModel.getSort();
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(RecommendViewModel.class);
        sourceViewModel.sortResult.observe(this, new Observer<AbsSortXml>() {
            @Override
            public void onChanged(AbsSortXml absXml) {
                showSuccess();
                if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
                    sortAdapter.setNewData(absXml.classes.sortList);
                } else {
                    sortAdapter.setNewData(new ArrayList<>());
                }
                initViewPager(absXml);
            }
        });
    }

    private void initViewPager(AbsSortXml absXml) {
        if (sortAdapter.getData().size() > 0) {
            BaseQuickAdapter.OnItemClickListener gridFragmentItemClickListener = new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    if (ApiConfig.get().getSourceBeanList().isEmpty())
                        return;
                    String title = ((Movie.Video) adapter.getItem(position)).name;
                    Intent newIntent = new Intent(mContext, SearchActivity.class);
                    newIntent.putExtra("title", title);
                    newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(newIntent);
                }
            };
            for (MovieSort.SortData data : sortAdapter.getData()) {
                GridFragment gridFragment = GridFragment.newInstance(data, new RecommendGridAdapter(), RecommendViewModel.class, 1);
                gridFragment.setOnItemClickListener(gridFragmentItemClickListener);
                fragments.add(gridFragment);
            }
            pageAdapter = new HomePageAdapter(getSupportFragmentManager(), fragments);
            try {
                Field field = ViewPager.class.getDeclaredField("mScroller");
                field.setAccessible(true);
                FixedSpeedScroller scroller = new FixedSpeedScroller(mContext, new AccelerateInterpolator());
                field.set(recomViewPager, scroller);
                scroller.setmDuration(300);
            } catch (Exception e) {
            }
            recomViewPager.setPageTransformer(true, new DefaultTransformer());
            recomViewPager.setAdapter(pageAdapter);
            recomViewPager.setCurrentItem(currentSelected, false);
        }
    }

    private Runnable mDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (sortChange) {
                sortChange = false;
                if (sortFocused != currentSelected) {
                    currentSelected = sortFocused;
                    recomViewPager.setCurrentItem(sortFocused, false);
                }
            }
        }
    };
}