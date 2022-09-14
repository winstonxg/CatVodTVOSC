package com.github.tvbox.osc.ui.fragment.homes;

import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.HomeCatBean;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.adapter.HomeCatAdapter;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.HistoryFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class CatFragment extends AbstractHomeFragment {

    private LinearLayout mHomeFrame;
    private FrameLayout mCategoryFrame;
    private TvRecyclerView mCategoryGridView;
    private NoScrollViewPager mViewPager;
    private TvRecyclerView mHomeGridView;
    private HomeCatAdapter homeCatAdapter;
    private List<HomeCatBean> catBeans = new ArrayList<>();
    private LinearLayout btnExpandHist;
    private ImageView imgExpandHistIcon;
    private SortAdapter sortAdapter;
    private HomePageAdapter pageAdapter;
    private List<BaseLazyFragment> fragments = new ArrayList<>();

    private boolean isRight = false;
    private boolean sortChange = false;
    private int currentSelected = 0;
    private int spanCount = 5;
    private boolean isHistExpanded = false;
    private int sortFocused = 0;
    private int screenHeight = 0;
    private int vodHeaderPos = 0;

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

    protected void initView() {
        EventBus.getDefault().register(this);
        this.mHomeFrame = findViewById(R.id.mHomeFrame);
        this.mCategoryFrame = findViewById(R.id.mCategoryFrame);
        this.mCategoryGridView = findViewById(R.id.mCategoryGridView);
        this.mViewPager = findViewById(R.id.mViewPager);
        this.mHomeGridView = findViewById(R.id.mHomeGridView);
        this.tvDate = findViewById(R.id.tvDate);

        mHomeGridView.setHasFixedSize(true);
        mHomeGridView.getRecycledViewPool().setMaxRecycledViews(0, 10);
        this.homeCatAdapter = new HomeCatAdapter((BaseActivity) this.mActivity);
        mHomeGridView.setAdapter(this.homeCatAdapter);
        this.homeCatAdapter.bindToRecyclerView(mHomeGridView);
        View header = getLayoutInflater().inflate(R.layout.item_home_cat_header, null);
        this.btnExpandHist = header.findViewById(R.id.lblExpandHist);
        this.imgExpandHistIcon = header.findViewById(R.id.imgExpandHistIcon);
        this.tvDate = header.findViewById(R.id.tvDate);
        this.tvName = header.findViewById(R.id.headerName);
        this.ivQRCode = header.findViewById(R.id.ivQRCode);
        if(Hawk.get(HawkConfig.REMOTE_CONTROL, true)) {
            refreshQRCode();
        } else {
            header.findViewById(R.id.remoteRoot).setVisibility(View.GONE);
        }
        this.homeCatAdapter.addHeaderView(header);
        UserFragment userFragment = (UserFragment) getChildFragmentManager().findFragmentByTag("mUserFragment");
        userFragment.updateShowVod(true);
        userFragment.vodClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCategoryFrame.setVisibility(View.VISIBLE);
                mHomeFrame.setVisibility(View.GONE);
                if(mCategoryGridView.getSelectedPosition() == RecyclerView.NO_POSITION)
                    mCategoryGridView.setSelection(0);
            }
        };

        spanCount = !shouldMoreColumns() ? 5 : 6;
        V7GridLayoutManager gridLayoutManager = new V7GridLayoutManager(this.mContext, spanCount);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if(position == 0 || position == vodHeaderPos) {
                    return spanCount;
                } else
                    return 1;
            }
        });
        mHomeGridView.setHasFixedSize(true);
        mHomeGridView.setLayoutManager(gridLayoutManager);

        this.homeCatAdapter.setNewData(catBeans);
        mHomeGridView.setSelection(0);

        this.sortAdapter = new SortAdapter(R.layout.item_home_sort_cat);
        this.initCategorySection();
        this.btnExpandHist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isHistExpanded = !isHistExpanded;
                if(isHistExpanded) {
                    imgExpandHistIcon.animate().setDuration(250).rotation(180);
                    initHistory(false);
                } else {
                    imgExpandHistIcon.animate().setDuration(250).rotation(0);
                    int headerCount = homeCatAdapter.getHeaderLayoutCount();
                    int index = headerCount + spanCount;
                    while (vodHeaderPos > index) {
                        homeCatAdapter.remove(vodHeaderPos - 1 - headerCount);
                        vodHeaderPos--;
                    }
                }
            }
        });
        setLoadSir(this.mHomeFrame);
    }

    private void initCategorySection() {
        this.mCategoryGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.VERTICAL, false));
        this.mCategoryGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 2.0f));
        this.mCategoryGridView.setAdapter(this.sortAdapter);
        this.mCategoryGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null && !isRight) {
                    view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(false);
                    textView.setTextColor(getResources().getColor(R.color.color_CCFFFFFF));
                    textView.invalidate();
                    view.findViewById(R.id.mSortLayout).setBackgroundColor(Color.TRANSPARENT);
                    view.findViewById(R.id.tvFilter).setVisibility(View.GONE);
                }
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (view != null) {
                    CatFragment.this.isRight = false;
                    CatFragment.this.sortChange = true;
                    view.findViewById(R.id.mSortLayout).setBackgroundColor(Color.argb(68, 0, 0, 0));
                    TextView textView = view.findViewById(R.id.tvTitle);
                    textView.getPaint().setFakeBoldText(true);
                    textView.setTextColor(getResources().getColor(R.color.color_FFFFFF));
                    textView.invalidate();
                    if (!sortAdapter.getItem(position).filters.isEmpty())
                        view.findViewById(R.id.tvFilter).setVisibility(View.VISIBLE);
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
        this.mCategoryGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            public final boolean onInBorderKeyEvent(int direction, View view) {
                if(direction == View.FOCUS_RIGHT || direction == View.FOCUS_UP) {
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
                    mCategoryGridView.smoothScrollToPosition(position);
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
                    sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), new ArrayList<>(), false));
                }
                initViewPager(absXml);
            }
        });
    }

    private void initHistory(boolean fullRefresh) {
        List<VodInfo> allVodRecord = RoomDataManger.getAllVodRecord(100);
        int headerCount = homeCatAdapter.getHeaderLayoutCount();
        int recordCount = 0;
        if(fullRefresh) {
            for (int i = 0; i < vodHeaderPos - headerCount; i++)
                homeCatAdapter.remove(0);
            vodHeaderPos = 0;
        }
        for (VodInfo vodInfo : allVodRecord) {
            if(recordCount < vodHeaderPos - 1) {
                recordCount++;
                continue;
            }
            HomeCatBean historyBean = new HomeCatBean();
            historyBean.historyRecord = vodInfo;
            homeCatAdapter.addData(recordCount, historyBean);
            recordCount++;
            if(!isHistExpanded && recordCount >= spanCount)
                break;
        }
        vodHeaderPos = headerCount + recordCount;
        updateBtnExpandHist(allVodRecord.size() > spanCount);
    }

    private void initViewPager(AbsSortXml absXml) {
        initHistory(false);
        HomeCatBean homeTitleBean = new HomeCatBean();
        homeTitleBean.isHead = true;
        homeCatAdapter.addData(homeTitleBean);

        if(ApiConfig.get().getHomeSourceBean().getApi() == null) {
            showSuccess();
            return;
        }

        try {
            if (absXml.list != null && absXml.list.videoList.size() > 0) {
                for (Movie.Video video : absXml.list.videoList) {
                    HomeCatBean videoBean = new HomeCatBean();
                    videoBean.homeItem = video;
                    homeCatAdapter.addData(videoBean);
                }
            }

            if (sortAdapter.getData().size() > 0) {
                for (MovieSort.SortData data : sortAdapter.getData()) {
                    fragments.add(GridFragment.newInstance(data, SourceViewModel.class));
                }
                pageAdapter = new HomePageAdapter(((BaseActivity) mActivity).getSupportFragmentManager(), fragments);
                try {
                    Field field = ViewPager.class.getDeclaredField("mScroller");
                    field.setAccessible(true);
                    FixedSpeedScroller scroller = new FixedSpeedScroller(mContext, new AccelerateInterpolator());
                    field.set(mViewPager, scroller);
                    scroller.setmDuration(300);
                    mViewPager.setPageTransformer(true, new DefaultTransformer());
                    mViewPager.setAdapter(pageAdapter);
                    mViewPager.setCurrentItem(currentSelected, false);
                } catch (Exception e) {
                }
                mCategoryFrame.setVisibility(View.GONE);
            }
        }catch (Exception ex) {
            LOG.e(ex.getMessage());
        }
        showSuccess();
    }

    public boolean pressBack() {
        if(this.homeCatAdapter.getIsDelMode()) {
            this.homeCatAdapter.toggleDelMode(false);
            return false;
        } if(mCategoryFrame.getVisibility() == View.VISIBLE) {
            if(currentSelected >= 0 && currentSelected < fragments.size()) {
                GridFragment currentGridFragment = ((GridFragment)fragments.get(currentSelected));
                if(currentGridFragment.popFolder())
                    return false;
            }
            mCategoryFrame.setVisibility(View.GONE);
            mHomeFrame.setVisibility(View.VISIBLE);
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

    @Override
    public boolean dispatchKey(KeyEvent event) {
        if(super.dispatchKey(event))
            return true;
        View focusedView = mHomeGridView.getFocusedChild();
        if(focusedView != null) {
            View headerName = focusedView.findViewById(R.id.headerName);
            if (headerName != null) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (headerName.isFocused()) {
                            mHomeGridView.getFocusedChild().findViewById(R.id.tvVod).requestFocus();
                        } else {
                            View itemFrame = null;
                            for (int pos = 1; pos < homeCatAdapter.getItemCount(); pos++) {
                                itemFrame = homeCatAdapter.getViewByPosition(pos, R.id.mItemFrame);
                                if (itemFrame != null && itemFrame.getVisibility() == View.VISIBLE) {
                                    itemFrame.requestFocus();
                                    break;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
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
                    initHistory(true);
                }
            }, 50);
        }
    }

    private void updateBtnExpandHist(boolean shouldShow) {
        if(shouldShow) {
            btnExpandHist.setVisibility(View.VISIBLE);
        } else {
            btnExpandHist.setVisibility(View.GONE);
        }
    }

}