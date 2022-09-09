package com.github.tvbox.osc.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.animation.BounceInterpolator;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.adapter.GridAdapter;
import com.github.tvbox.osc.ui.dialog.GridFilterDialog;
import com.github.tvbox.osc.ui.tv.widget.LoadMoreView;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class GridFragment extends BaseLazyFragment {

    public interface GridDataCallback {
        void onDataReceived(SourceViewModel viewModel);
    }

    private MovieSort.SortData sortData = null;
    private TvRecyclerView mGridView;
    private Class<SourceViewModel> viewModelClass;
    private SourceViewModel sourceViewModel;
    private GridFilterDialog gridFilterDialog;
    private int page = 1;
    private int maxPage = 1;
    private boolean isLoad = false;
    private boolean isTop = true;
    private Integer spanCount = null;
    private BaseQuickAdapter<Movie.Video, BaseViewHolder> adapter;
    private TvRecyclerView.OnItemListener itemListener;
    private GridDataCallback dataCallback;
    private BaseQuickAdapter.OnItemClickListener itemClickListener = new BaseQuickAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
            FastClickCheckUtil.check(view);
            Movie.Video video = (Movie.Video) adapter.getData().get(position);
            if (video != null) {
                Bundle bundle = new Bundle();
                bundle.putString("id", video.id);
                bundle.putString("sourceKey", video.sourceKey);
                jumpActivity(DetailActivity.class, bundle);
            }
        }
    };
    private BaseQuickAdapter.OnItemLongClickListener itemLongClickListener = null;

    public static GridFragment newInstance(MovieSort.SortData sortData, Class viewModelClass) {
        GridFragment fragment = new GridFragment().setArguments(sortData, new GridAdapter(), viewModelClass, null);
        return fragment;
    }

    public static GridFragment newInstance(MovieSort.SortData sortData, SourceViewModel model) {
        GridFragment fragment = new GridFragment().setArguments(sortData, new GridAdapter(), SourceViewModel.class, null);
        fragment.sourceViewModel = model;
        return fragment;
    }

    public static GridFragment newInstance(MovieSort.SortData sortData, BaseQuickAdapter<Movie.Video, BaseViewHolder> adapter, Class viewModelClass) {
        GridFragment fragment = new GridFragment().setArguments(sortData, adapter, viewModelClass, null);
        return fragment;
    }

    public static GridFragment newInstance(MovieSort.SortData sortData, BaseQuickAdapter<Movie.Video, BaseViewHolder> adapter, Class viewModelClass, Integer spanCount) {
        GridFragment fragment = new GridFragment().setArguments(sortData, adapter, viewModelClass, spanCount);
        return fragment;
    }

    public GridFragment setArguments(MovieSort.SortData sortData, BaseQuickAdapter<Movie.Video, BaseViewHolder> adapter, Class viewModelClass, Integer spanCount) {
        this.sortData = sortData;
        this.adapter = adapter;
        this.viewModelClass = viewModelClass;
        if(spanCount != null)
            this.spanCount = spanCount;
        return this;
    }

    public void OnDataUpdate(GridDataCallback callback) {
        this.dataCallback = callback;
    }

    public void SetOnItemListener(TvRecyclerView.OnItemListener itemListener) {
        this.itemListener = itemListener;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_grid;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private void initView() {
        if(spanCount == null)
            spanCount = !shouldMoreColumns() ? 5 : 6;
        mGridView = findViewById(R.id.mGridView);
        mGridView.setHasFixedSize(true);
        mGridView.setAdapter(adapter);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, this.spanCount));
        adapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                adapter.setEnableLoadMore(true);
                sourceViewModel.getList(sortData, page);
            }
        }, mGridView);
        mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.findViewById(R.id.tvName).setSelected(false);
                itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
                if(itemListener != null)
                    itemListener.onItemPreSelected(parent, itemView, position);
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                itemView.findViewById(R.id.tvName).setSelected(true);
                itemView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
                if(itemListener != null)
                    itemListener.onItemSelected(parent, itemView, position);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                if(itemListener != null)
                    itemListener.onItemClick(parent, itemView, position);
            }
        });
        if(itemLongClickListener != null)
            this.adapter.setOnItemLongClickListener(itemLongClickListener);
        mGridView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            @Override
            public boolean onInBorderKeyEvent(int direction, View focused) {
                if (direction == View.FOCUS_UP) {
                }
                return false;
            }
        });
        setOnItemClickListener(itemClickListener);
        adapter.setLoadMoreView(new LoadMoreView());
        setLoadSir(mGridView);
    }

    public void setOnItemClickListener(BaseQuickAdapter.OnItemClickListener listener) {
        this.itemClickListener = listener;
        this.adapter.setOnItemClickListener(listener);
    }

    public void setOnItemLongClickListener(BaseQuickAdapter.OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
        this.adapter.setOnItemLongClickListener(listener);
    }

    public List<Movie.Video> GetResultList() {
        if(sourceViewModel == null)
            return null;
        AbsXml absXml = sourceViewModel.listResult.getValue();
        if(absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            return absXml.movie.videoList;
        }
        return null;
    }

    private void initViewModel() {
        if(sourceViewModel == null)
            sourceViewModel = new ViewModelProvider(this).get(viewModelClass);
        if(sortData.id.equals("_home")) {
            showSuccess();
            isLoad = true;
            AbsSortXml result = sourceViewModel.sortResult.getValue();
            if(result != null && result.list != null && result.list.videoList != null)
                adapter.setNewData(sourceViewModel.sortResult.getValue().list.videoList);
            else
                showEmpty();
        }else
            sourceViewModel.listResult.observe(this, new Observer<AbsXml>() {
                @Override
                public void onChanged(AbsXml absXml) {
                    if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
                        if (page == 1) {
                            showSuccess();
                            isLoad = true;
                            adapter.setNewData(absXml.movie.videoList);
                        } else {
                            adapter.addData(absXml.movie.videoList);
                        }
                        page++;
                        maxPage = absXml.movie.pagecount;
                    } else {
                        if (page == 1) {
                            showEmpty();
                        }
                    }
                    if (page > maxPage) {
                        adapter.loadMoreEnd();
                    } else {
                        adapter.loadMoreComplete();
                    }
                    if(GridFragment.this.dataCallback != null)
                        GridFragment.this.dataCallback.onDataReceived(sourceViewModel);
                }
            });
    }

    public boolean isLoad() {
        return isLoad;
    }

    private void initData() {
        if(!sortData.id.equals("_home")) {
            showLoading();
            isLoad = false;
            sourceViewModel.getList(sortData, page);
        }
    }

    public boolean isTop() {
        return isTop;
    }

    public void scrollTop() {
        isTop = true;
        mGridView.scrollToPosition(0);
    }

    public void showFilter() {
        if (!sortData.filters.isEmpty() && gridFilterDialog == null) {
            gridFilterDialog = new GridFilterDialog(mContext);
            gridFilterDialog.setData(sortData);
            gridFilterDialog.setOnDismiss(new GridFilterDialog.Callback() {
                @Override
                public void change() {
                    page = 1;
                    initData();
                    mGridView.scrollToPosition(0);
                }
            });
        }
        if (gridFilterDialog != null)
            gridFilterDialog.show();
    }


}