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
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.HistoryFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.ui.tv.widget.ViewObj;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class AssembledFragment extends AbstractHomeFragment {

        private LinearLayout topLayout;
        private LinearLayout contentLayout;
        private FragmentContainerView mFeatureView;
        private TvRecyclerView mGridView;
        private NoScrollViewPager mViewPager;
        private SortAdapter sortAdapter;
        private HomePageAdapter pageAdapter;
        private List<BaseLazyFragment> fragments = new ArrayList<>();
        private boolean isUpOrRight = false;
        private boolean sortChange = false;
        private int currentSelected = 0;
        private int sortFocused = 0;
        public View sortFocusView = null;


        @Override
        protected int getLayoutResID() {
            return R.layout.fragment_home_assembled;
        }

        @Override
        protected void init() {
            initView();
            initViewModel();
            initData();
        }

        protected void initView() {
            this.topLayout = findViewById(R.id.topLayout);
            this.tvName = findViewById(R.id.tvName);
            this.tvDate = findViewById(R.id.tvDate);
            this.contentLayout = findViewById(R.id.contentLayout);
            this.mGridView = findViewById(R.id.mGridView);
            this.mFeatureView = findViewById(R.id.mFeatureView);
            //this.ivQRCode = findViewById(R.id.ivQRCode);
//            if(Hawk.get(HawkConfig.REMOTE_CONTROL, true)) {
//                refreshQRCode();
//            } else {
//                findViewById(R.id.remoteRoot).setVisibility(View.GONE);
//            }
            FragmentManager fragmentManager = getChildFragmentManager();
            UserFragment userFragment = (UserFragment)fragmentManager.findFragmentByTag("mUserFragment");
            userFragment.SetFragmentView(mFeatureView);
            this.mViewPager = findViewById(R.id.mViewPager);
            this.sortAdapter = new SortAdapter();
            this.mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.VERTICAL, false));
            this.mGridView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 2.0f));
            this.mGridView.setAdapter(this.sortAdapter);
            this.mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
                public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                    if (view != null && !AssembledFragment.this.isUpOrRight) {
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                        TextView textView = view.findViewById(R.id.tvTitle);
                        textView.getPaint().setFakeBoldText(false);
                        textView.setTextColor(AssembledFragment.this.getResources().getColor(R.color.color_BBFFFFFF));
                        textView.invalidate();
                        view.findViewById(R.id.tvFilter).setVisibility(View.GONE);
                        view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                    }
                }

                public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                    if (view != null) {
                        if(AssembledFragment.this.sortFocusView != null) {
                            AssembledFragment.this.sortFocusView.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                        }
                        AssembledFragment.this.isUpOrRight = false;
                        AssembledFragment.this.sortChange = true;
                        view.animate().scaleX(1.1f).scaleY(1.1f).setInterpolator(new BounceInterpolator()).setDuration(300).start();
                        TextView textView = view.findViewById(R.id.tvTitle);
                        textView.getPaint().setFakeBoldText(true);
                        textView.setTextColor(AssembledFragment.this.getResources().getColor(R.color.color_FFFFFF));
                        textView.invalidate();
                        if (!sortAdapter.getItem(position).filters.isEmpty())
                            view.findViewById(R.id.tvFilter).setVisibility(View.VISIBLE);
                        view.findViewById(R.id.tvFocusedBar).setVisibility(View.INVISIBLE);
                        AssembledFragment.this.sortFocusView = view;
                        AssembledFragment.this.sortFocused = position;
                        if(position != 0) {
                            HistoryFragment historyFragment = (HistoryFragment)fragments.get(0);
                            if(historyFragment.isInDelMode()) {
                                historyFragment.toggleDelMode();
                                return;
                            }
                        }
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
                        isUpOrRight = true;
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
                                changeTop(position > 0);
                            }
                        }
                    }

                }
            });
            this.mFeatureView.setOnFocusChangeListener(new View.OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(topHide == 1 && hasFocus) {
                        changeTop(false);
                    } else if(topHide == 0 && !hasFocus && sortFocused > 0) {
                        changeTop(true);
                    }
                }
            });
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
            if (sortAdapter.getData().size() > 0) {
                for (MovieSort.SortData data : sortAdapter.getData()) {
                    if(data.id.equals("_home")) {
                        fragments.add(GridFragment.newInstance(data, sourceViewModel));
                    } else {
                        fragments.add(GridFragment.newInstance(data, SourceViewModel.class));
                    }
                }
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
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mFeatureView.requestFocus();
                    }
                }, 500);
            }
        }

        @Override
        public boolean pressBack() {
            if(!super.pressBack())
                return false;
            int i;
            if (this.fragments.size() <= 0 || this.sortFocused >= this.fragments.size() || (i = this.sortFocused) < 0) {
                return exit();
            }
            BaseLazyFragment baseLazyFragment = this.fragments.get(i);
            if(baseLazyFragment instanceof HistoryFragment) {
                HistoryFragment historyFragment = (HistoryFragment)baseLazyFragment;
                if(historyFragment.isInDelMode()) {
                    historyFragment.toggleDelMode();
                    return false;
                }
            }
            if (baseLazyFragment instanceof GridFragment) {
                if(((GridFragment)baseLazyFragment).popFolder())
                    return false;
                View view = this.sortFocusView;
                if (view != null && !view.isFocused()) {
//                ((GridFragment) baseLazyFragment).scrollTop();
                    changeTop(false);
                    this.sortFocusView.requestFocus();
                } else if (this.sortFocused != 0) {
                    this.mGridView.setSelection(0);
                } else {
                    return exit();
                }
            } else {
                return exit();
            }
            return exit();
        }

    @Override
    public void doAfterApiInit() {
        fragments.add(HistoryFragment.newInstance());
        showSuccess();
    }

    private Runnable mDataRunnable = new Runnable() {
            @Override
            public void run() {
                if (sortChange) {
                    sortChange = false;
                    if (sortFocused != currentSelected) {
                        currentSelected = sortFocused;
                        mViewPager.setCurrentItem(sortFocused, false);
                        changeTop(sortFocused != 0);
                    }
                }
            }
        };

        @Override
        public boolean dispatchKey(KeyEvent event) {
            if(super.dispatchKey(event))
                return true;
            if (topHide < 0)
                return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                mHandler.removeCallbacks(mDataRunnable);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                mHandler.postDelayed(mDataRunnable, 200);
            }
            return false;
        }

        byte topHide = 0;

        private void changeTop(boolean hide) {
            ViewObj viewObj = new ViewObj(mFeatureView, (ViewGroup.MarginLayoutParams) mFeatureView.getLayoutParams());
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    topHide = (byte) (hide ? 1 : 0);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            if (hide && topHide == 0) {
                animatorSet.playTogether(ObjectAnimator.ofObject(viewObj, "marginTop", new IntEvaluator(),
                        AutoSizeUtils.mm2px(this.mContext, 50.0f),
                        AutoSizeUtils.mm2px(this.mContext, -150.0f)),
                        ObjectAnimator.ofFloat(this.mFeatureView, "alpha", 1.0f, 0.0f),
                        ObjectAnimator.ofFloat(this.topLayout, "alpha", 1.0f, 0.0f));
                animatorSet.setDuration(200);
                animatorSet.start();
                return;
            }
            if (!hide && topHide == 1) {
                animatorSet.playTogether(ObjectAnimator.ofObject(viewObj, "marginTop", new IntEvaluator(),
                        AutoSizeUtils.mm2px(this.mContext, -150.0f),
                        AutoSizeUtils.mm2px(this.mContext, 50.0f)),
                        ObjectAnimator.ofFloat(this.topLayout, "alpha", 0.0f, 1.0f),
                        ObjectAnimator.ofFloat(this.mFeatureView, "alpha", 0.0f, 1.0f));
                animatorSet.setDuration(200);
                animatorSet.start();
                return;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            EventBus.getDefault().unregister(this);
        }

    }