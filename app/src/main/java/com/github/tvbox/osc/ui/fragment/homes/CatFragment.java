package com.github.tvbox.osc.ui.fragment.homes;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.HistoryFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class CatFragment extends AbstractHomeFragment {

    private FrameLayout mLoadingFrame;
    private FrameLayout mCategoryFrame;
    private TvRecyclerView mGridView;
    private NoScrollViewPager mViewPager;
    private ScrollView mScrollView;
    private LinearLayout contentLayout;
    private LinearLayout btnExpandHist;
    private ImageView imgExpandHistIcon;
    private FragmentContainerView mFeatureView;
    private FrameLayout mHomeFrame;
    private FrameLayout mHistoryFrame;
    private SortAdapter sortAdapter;
    private HomePageAdapter pageAdapter;
    private HistoryFragment historyFragment = null;
    private GridFragment homeFragment = new GridFragment();
    private GridAdapter homeAdapter = new GridAdapter();
    private List<BaseLazyFragment> fragments = new ArrayList<>();
    private UserFragment userFragment;

    private boolean isRight = false;
    private boolean sortChange = false;
    private int currentSelected = 0;
    private int sortFocused = 0;
    public View sortFocusView = null;

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_home_cat;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        EventBus.getDefault().register(this);
        this.mLoadingFrame = findViewById(R.id.mLoadingFrame);
        this.mCategoryFrame = findViewById(R.id.mCategoryFrame);
        this.mGridView = findViewById(R.id.mGridView);
        this.mViewPager = findViewById(R.id.mViewPager);
        this.mScrollView = findViewById(R.id.mScrollView);
        this.tvDate = findViewById(R.id.tvDate);
        this.contentLayout = findViewById(R.id.contentLayout);
        this.btnExpandHist = findViewById(R.id.lblExpandHist);
        this.imgExpandHistIcon = findViewById(R.id.imgExpandHistIcon);
        this.mFeatureView = findViewById(R.id.mFeatureView);
        this.mHistoryFrame = findViewById(R.id.mHistoryFrameLayout);
        this.mHomeFrame = findViewById(R.id.mHomeFrameLayout);
        FragmentManager fragmentManager = getChildFragmentManager();
        userFragment = (UserFragment)fragmentManager.findFragmentByTag("mUserFragment");
        userFragment.updateShowVod(true);
        userFragment.SetFragmentView(mFeatureView);
        userFragment.vodClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCategoryFrame.setVisibility(View.VISIBLE);
                mScrollView.setVisibility(View.INVISIBLE);
            }
        };
        this.sortAdapter = new SortAdapter();
        this.initCategorySection();
        this.homeAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                displayHomeContents();
            }
        });
        this.btnExpandHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleHistPanel();
            }
        });
        this.mFeatureView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                if(focused)
                    CatFragment.this.mScrollView.smoothScrollTo(0, 0);
            }
        });
        mScrollView.setSmoothScrollingEnabled(true);
        try {
            Field field = ScrollView.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            OverScroller scroller = new OverScroller(mContext, new AccelerateInterpolator());
            field.set(mScrollView, scroller);
        } catch (Exception e) {
        }
        setLoadSir(this.mLoadingFrame);
        //mHandler.postDelayed(mFindFocus, 500);
    }

    private void initCategorySection() {
        this.mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.VERTICAL, false));
        this.mGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 2.0f));
        this.mGridView.setAdapter(this.sortAdapter);
        this.mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null && !isRight) {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(false);
                    textView.setTextColor(CatFragment.this.getResources().getColor(R.color.color_BBFFFFFF));
                    textView.invalidate();
                    view.findViewById(R.id.tvFilter).setVisibility(View.GONE);
                    view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                }
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null) {
                    if(CatFragment.this.sortFocusView != null) {
                        CatFragment.this.sortFocusView.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                    }
                    CatFragment.this.isRight = false;
                    CatFragment.this.sortChange = true;
                    view.animate().scaleX(1.1f).scaleY(1.1f).setInterpolator(new BounceInterpolator()).setDuration(300).start();
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(true);
                    textView.setTextColor(CatFragment.this.getResources().getColor(R.color.color_FFFFFF));
                    textView.invalidate();
                    if (!sortAdapter.getItem(position).filters.isEmpty())
                        view.findViewById(R.id.tvFilter).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                    CatFragment.this.sortFocusView = view;
                    CatFragment.this.sortFocused = position;
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
        this.mGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            public final boolean onInBorderKeyEvent(int direction, View view) {
                if(direction == View.FOCUS_RIGHT || direction == View.FOCUS_UP) {
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
        this.sortAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            public final void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int position) {
                if (view.getId() == R.id.tvTitle) {
                    mGridView.smoothScrollToPosition(position);
                    if (view.getParent() != null) {
                        ViewGroup viewGroup = (ViewGroup) view.getParent();
                        sortFocusView = viewGroup;
                        viewGroup.requestFocus();
                        sortFocused = position;
                        if (position != currentSelected) {
                            currentSelected = position;
                            mViewPager.setCurrentItem(position, false);
                        }
                    }
                }

            }
        });
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(this, new Observer<AbsSortXml>() {
            @Override
            public void onChanged(AbsSortXml absXml) {
                if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
                    sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), absXml.classes.sortList, false));
                } else {
                    sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), new ArrayList<>(), true));
                }
                initViewPager(absXml);
            }
        });
    }

    private void toggleHistPanel() {
        boolean trimSize = historyFragment.getLoadSize() == 5 || historyFragment.getLoadSize() == 6;
        if(trimSize) {
            historyFragment.setLoadSize(100);
            imgExpandHistIcon.animate().setDuration(250).rotation(180);
        } else {
            historyFragment.setLoadSize(isBaseOnWidth() ? 5 : 6);
            imgExpandHistIcon.animate().setDuration(250).rotation(0);
        }
        updateHistoryFrameSize(trimSize);
//            view.setNestedScrollingEnabled(false);
//            view.requestLayout();
    }

    public void updateHistoryFrameSize(boolean isTrimSize) {
        TvRecyclerView view = mHistoryFrame.findViewById(R.id.mGridView);
        if(view == null)
            return;
        if(isTrimSize) {
            historyFragment.setLoadSize(100);
            imgExpandHistIcon.animate().setDuration(250).rotation(180);
            int presentingSize = historyFragment.getPresentingSize();
            view.getLayoutParams().height = AutoSizeUtils.mm2px(
                    this.mContext, (float)Math.ceil(((float)presentingSize)/(isBaseOnWidth() ? 5 : 6)) * 266);
        } else {
            historyFragment.setLoadSize(isBaseOnWidth() ? 5 : 6);
            imgExpandHistIcon.animate().setDuration(250).rotation(0);
            view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    private void displayHomeContents() {
        mHomeFrame.post(new Runnable() {
            @Override
            public void run() {
                showSuccess();
                mLoadingFrame.setVisibility(View.GONE);
                mScrollView.setVisibility(View.VISIBLE);
                mScrollView.scrollTo(0, 0);
                List<Movie.Video> results = homeFragment.GetResultList();
                TvRecyclerView view = mHomeFrame.findViewById(R.id.mGridView);
                view.setNestedScrollingEnabled(false);
                if(results.size() > 0) {
                    view.getLayoutParams().height = AutoSizeUtils.mm2px(
                            CatFragment.this.mContext, (float)Math.ceil(((float)results.size())/(isBaseOnWidth() ? 5 : 6)) * 266);
                } else {
                    view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }
                view.requestLayout();
            }
        });
    }

    private void initViewPager(AbsSortXml absXml) {
        showLoading("正在加载首页数据源...");
        historyFragment = new HistoryFragment();
        historyFragment.setLoadSize(isBaseOnWidth() ? 5 : 6);
        getChildFragmentManager().beginTransaction()
                .add(R.id.mHistoryFrameLayout, historyFragment, "mHistoryFragment").disallowAddToBackStack().commit();
        this.mHistoryFrame.post(new Runnable() {
            @Override
            public void run() {
                updateBtnExpandHist();
                TvRecyclerView historyRView = mHistoryFrame.findViewById(R.id.mGridView);
                historyRView.setOnItemListener(new TvRecyclerView.OnItemListener() {
                    @Override
                    public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

                    }

                    @Override
                    public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                        mScrollView.smoothScrollTo(0, itemView.getBottom() - itemView.getHeight() / 2);
                    }

                    @Override
                    public void onItemClick(TvRecyclerView parent, View itemView, int position) {

                    }
                });
            }
        });
        homeFragment = GridFragment.newInstance(new MovieSort.SortData("_home", "首页"), homeAdapter, SourceViewModel.class);
        homeFragment.SetOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {

            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                mScrollView.smoothScrollTo(0, mHomeFrame.getTop() + itemView.getBottom() - itemView.getHeight() / 2 - 230);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {

            }
        });
        getChildFragmentManager().beginTransaction()
                .add(R.id.mHomeFrameLayout, homeFragment, "mHomeFragment").disallowAddToBackStack().commit();
        if (sortAdapter.getData().size() > 0) {
            for (MovieSort.SortData data : sortAdapter.getData()) {
                fragments.add(GridFragment.newInstance(data, SourceViewModel.class));
            }
            if(sortAdapter.getData().size() == 0) {
                showSuccess();
                userFragment.updateShowVod(false);
            } else {
                pageAdapter = new HomePageAdapter(((BaseActivity)mActivity).getSupportFragmentManager(), fragments);
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
                mViewPager.setCurrentItem(currentSelected, false);
                mCategoryFrame.setVisibility(View.INVISIBLE);
            }
        }
    }

    public boolean pressBack() {
        if(historyFragment.isInDelMode()) {
            historyFragment.toggleDelMode();
            return false;
        } if(mCategoryFrame.getVisibility() == View.VISIBLE) {
            mCategoryFrame.setVisibility(View.INVISIBLE);
            mScrollView.setVisibility(View.VISIBLE);
            return false;
        } else
            return exit();
    }

    private Runnable mDataRunnable = new Runnable() {
        @Override
        public void run() {
            if (sortChange) {
                sortChange = false;
                if (sortFocused != currentSelected) {
                    currentSelected = sortFocused;
                    mViewPager.setCurrentItem(sortFocused, false);
                }
            }
        }
    };

    public boolean dispatchKey(KeyEvent event) {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_CONNECTION) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int colCount = isBaseOnWidth() ? 5 : 6;
                    if(historyFragment.getAllRecordSize() <= colCount)
                        updateHistoryFrameSize(false);
                    else
                        updateHistoryFrameSize(historyFragment.getLoadSize() != colCount);
                    updateBtnExpandHist();
                }
            }, 50);
        }
    }

    private void updateBtnExpandHist() {

        if(historyFragment.getAllRecordSize() <= (isBaseOnWidth() ? 5 : 6)) {
            btnExpandHist.setVisibility(View.GONE);
        } else {
            btnExpandHist.setVisibility(View.VISIBLE);
        }
    }

}