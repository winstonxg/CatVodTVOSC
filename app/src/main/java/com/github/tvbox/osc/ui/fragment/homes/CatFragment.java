package com.github.tvbox.osc.ui.fragment.homes;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.HistoryFragment;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.ui.tv.widget.ViewObj;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class CatFragment extends AbstractHomeFragment {

        private LinearLayout topLayout;
        private LinearLayout contentLayout;
        private FragmentContainerView mFeatureView;
        private FrameLayout mHomeFrame;
        private FrameLayout mHistoryFrame;
        private SortAdapter sortAdapter;
        private HomePageAdapter pageAdapter;
        private HistoryFragment historyFragment = null;
        private GridFragment homeFragment = new GridFragment();
        private GridAdapter homeAdapter = new GridAdapter();
        private List<BaseLazyFragment> fragments = new ArrayList<>();
        private boolean isUpOrRight = false;
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
            this.topLayout = findViewById(R.id.topLayout);
            this.tvDate = findViewById(R.id.tvDate);
            this.contentLayout = findViewById(R.id.contentLayout);
            this.mFeatureView = findViewById(R.id.mFeatureView);
            this.mHistoryFrame = findViewById(R.id.mHistoryFrameLayout);
            this.mHomeFrame = findViewById(R.id.mHomeFrameLayout);
            FragmentManager fragmentManager = getChildFragmentManager();
            UserFragment userFragment = (UserFragment)fragmentManager.findFragmentByTag("mUserFragment");
            userFragment.SetFragmentView(mFeatureView);
            this.sortAdapter = new SortAdapter();
            this.sortAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
                public final void onItemChildClick(BaseQuickAdapter baseQuickAdapter, View view, int position) {
//                    if (view.getId() == R.id.tvTitle) {
//                        mGridView.smoothScrollToPosition(position);
//                        if (view.getParent() != null) {
//                            ViewGroup viewGroup = (ViewGroup) view.getParent();
//                            sortFocusView = viewGroup;
//                            viewGroup.requestFocus();
//                            sortFocused = position;
//                            if (position != currentSelected) {
//                                currentSelected = position;
//                                mViewPager.setCurrentItem(position, false);
//                                changeTop(position > 0);
//                            }
//                        }
//                    }

                }
            });
            //this.homeAdapter.set
            setLoadSir(this.contentLayout);
            //mHandler.postDelayed(mFindFocus, 500);
        }

        private void initViewModel() {
            sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
            sourceViewModel.sortResult.observe(this, new Observer<AbsSortXml>() {
                @Override
                public void onChanged(AbsSortXml absXml) {
                    showSuccess();
                    if (absXml != null && absXml.classes != null && absXml.classes.sortList != null) {
                        sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), absXml.classes.sortList, true));
                    } else {
                        sortAdapter.setNewData(DefaultConfig.adjustSort(ApiConfig.get().getHomeSourceBean().getKey(), new ArrayList<>(), true));
                    }
                    initViewPager(absXml);
                }
            });
        }

        private void initViewPager(AbsSortXml absXml) {
            historyFragment = new HistoryFragment();
            getChildFragmentManager().beginTransaction()
                    .add(R.id.mHistoryFrameLayout, historyFragment, "mHistoryFragment").disallowAddToBackStack().commit();
            mHistoryFrame.post(new Runnable() {
                @Override
                public void run() {
                    TvRecyclerView view = mHistoryFrame.findViewById(R.id.mGridView);
                    view.getLayoutParams().height = (int)getResources().getDimension(R.dimen.vs_280);
                    view.setNestedScrollingEnabled(false);
                    view.requestLayout();
                }
            });
            if (sortAdapter.getData().size() > 0) {
                for (MovieSort.SortData data : sortAdapter.getData()) {
                    if (data.id.equals("_home")) {
                        homeFragment = GridFragment.newInstance(data, SourceViewModel.class);
                        getChildFragmentManager().beginTransaction()
                                .add(R.id.mHomeFrameLayout, homeFragment, "mHomeFragment").disallowAddToBackStack().commit();
                        //homeFragment.
                        mHomeFrame.post(new Runnable() {
                            @Override
                            public void run() {
                                TvRecyclerView view = mHomeFrame.findViewById(R.id.mGridView);
                                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                view.setNestedScrollingEnabled(false);
                                view.requestLayout();
                                LinearLayout linearLayout = (LinearLayout) mHomeFrame.findViewById(R.id.mGridViewLayout);
                                linearLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                                linearLayout.requestLayout();
                            }
                        });
                    } else {
                        fragments.add(GridFragment.newInstance(data, SourceViewModel.class));
                    }
                }
            }
        }

        public boolean pressBack() {
            if(historyFragment.isInDelMode()) {
                historyFragment.toggleDelMode();
                return false;
            }else
                return exit();
        }

        public boolean dispatchKey(KeyEvent event) {
            return true;
        }

    }