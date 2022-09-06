package com.github.tvbox.osc.ui.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.HomeCatBean;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.MD5;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.UUID;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeCatAdapter extends BaseQuickAdapter<HomeCatBean, BaseViewHolder> {

    private boolean isDelMode = false;
    private BaseActivity currentActivity;

    public HomeCatAdapter(BaseActivity currentActivity) {
        super(R.layout.item_home_cat_vod, new ArrayList<>());
        this.currentActivity = currentActivity;
    }

    public boolean getIsDelMode() {
        return isDelMode;
    }

    @Override
    protected void convert(BaseViewHolder helper, HomeCatBean item) {
        if(item.isHead) {
            helper.setGone(R.id.mHomeTitle, true);
            helper.setGone(R.id.mItemFrame, false);
        } else {
            helper.setGone(R.id.mHomeTitle, false);
            helper.setGone(R.id.mItemFrame, true);
            FrameLayout mItemFrame = helper.getView(R.id.mItemFrame);
            if(item.historyRecord != null) {
                this.convertHistoryRecord(helper, item.historyRecord);
                mItemFrame.setOnLongClickListener(historyLongClickListener);
            } else {
                this.convertMovie(helper, item.homeItem);
            }
            mItemFrame.setOnFocusChangeListener(vodFocusChangeListener);
            mItemFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(isDelMode && item.historyRecord != null) {
                            RoomDataManger.deleteVodRecord(item.historyRecord.sourceKey, item.historyRecord);
                            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
                    } else if(item.historyRecord != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", item.historyRecord.id);
                        bundle.putString("sourceKey", item.historyRecord.sourceKey);
                        currentActivity.jumpActivity(DetailActivity.class, bundle);
                    } else if(item.homeItem != null) {
                        Bundle bundle = new Bundle();
                        bundle.putString("id", item.homeItem.id);
                        bundle.putString("sourceKey", item.homeItem.sourceKey);
                        currentActivity.jumpActivity(DetailActivity.class, bundle);
                    }
                }
            });
        }
        helper.itemView.invalidate();
    }

    private View.OnLongClickListener historyLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if(!isDelMode) {
                isDelMode = true;
                toggleDelMode(true);
                return true;
            }
            return false;
        }
    };

    private View.OnFocusChangeListener vodFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean b) {
            if(b) {
                view.setBackground(currentActivity.getResources().getDrawable(R.drawable.shape_user_focus));
                ((View)view.getParent()).animate().scaleX(1.05f).scaleY(1.05f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
                ((View)view.getParent()).animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).setInterpolator(new BounceInterpolator()).start();
            }
        }
    };

    public void toggleDelMode(boolean isDelMode) {
        this.isDelMode = isDelMode;
        int headerCount = this.getHeaderLayoutCount();
        for (int pos = 0; pos < this.getData().size(); pos++) {
            if(this.getData().get(pos).historyRecord != null) {
                View delView = this.getViewByPosition(pos + headerCount, R.id.delFrameLayout);
                if (delView != null)
                    delView.setVisibility(isDelMode ? View.VISIBLE : View.GONE);
            }
        }
    }

    protected void convertHistoryRecord(BaseViewHolder helper, VodInfo item) {
        TextView tvYear = helper.getView(R.id.tvYear);
        tvYear.setText(ApiConfig.get().getSource(item.sourceKey).getName());
        helper.setVisible(R.id.tvLang, false);
        helper.setVisible(R.id.tvArea, false);
        helper.setVisible(R.id.tvNote, false);
        helper.setText(R.id.tvName, item.name);
        // helper.setText(R.id.tvActor, item.actor);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        //由于部分电视机使用glide报错
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            FrameLayout innerFrame = helper.getView(R.id.mItemInnerFrame);
            innerFrame.post(new Runnable() {
                @Override
                public void run() {
                    innerFrame.getLayoutParams().height = (int)(innerFrame.getWidth() * 1.44171779);
                    innerFrame.requestLayout();
                    Picasso.get()
                            .load(DefaultConfig.checkReplaceProxy(item.pic))
                            .transform(new RoundTransformation(MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition()))
                                    .centerCorp(true)
                                    .override(AutoSizeUtils.mm2px(mContext,277), AutoSizeUtils.mm2px(mContext,400))
                                    .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                            .placeholder(R.drawable.img_loading_placeholder)
                            .error(R.drawable.img_loading_placeholder)
                            .into(ivThumb);

                }
            });
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
        if(isDelMode)
            helper.setGone(R.id.delFrameLayout, isDelMode);
    }

    private void convertMovie(BaseViewHolder helper, Movie.Video item) {
        helper.setGone(R.id.delFrameLayout, false);
        TextView tvYear = helper.getView(R.id.tvYear);
        if (item.year <= 0) {
            tvYear.setVisibility(View.GONE);
        } else {
            tvYear.setText(String.valueOf(item.year));
            tvYear.setVisibility(View.VISIBLE);
        }
        TextView tvLang = helper.getView(R.id.tvLang);
        tvLang.setVisibility(View.GONE);
        TextView tvArea = helper.getView(R.id.tvArea);
        tvArea.setVisibility(View.GONE);
        if (TextUtils.isEmpty(item.note)) {
            helper.setVisible(R.id.tvNote, false);
        } else {
            helper.setVisible(R.id.tvNote, true);
            helper.setText(R.id.tvNote, item.note);
        }
        helper.setText(R.id.tvName, item.name);
        helper.setText(R.id.tvActor, item.actor);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            FrameLayout innerFrame = helper.getView(R.id.mItemInnerFrame);
            innerFrame.post(new Runnable() {
                @Override
                public void run() {
                    innerFrame.getLayoutParams().height = (int)(innerFrame.getWidth() * 1.44171779);
                    innerFrame.requestLayout();
                    Picasso.get()
                            .load(DefaultConfig.checkReplaceProxy(item.pic))
                            .transform(new RoundTransformation(MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition()))
                                    .centerCorp(true)
                                    .override(AutoSizeUtils.mm2px(mContext,277), AutoSizeUtils.mm2px(mContext,400))
                                    .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                            .placeholder(R.drawable.img_loading_placeholder)
                            .error(R.drawable.img_loading_placeholder)
                            .into(ivThumb);

                }
            });
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}
