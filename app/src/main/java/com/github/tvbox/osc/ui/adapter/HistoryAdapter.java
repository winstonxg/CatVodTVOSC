package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.MD5;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class HistoryAdapter extends BaseQuickAdapter<VodInfo, BaseViewHolder> {

    private boolean isDelMode = false;

    public HistoryAdapter() {
        super(R.layout.item_grid, new ArrayList<>());
    }

    public void toggleDelMode(boolean isDelMode) {
        this.isDelMode = isDelMode;
        for (int pos = 0; pos < this.getItemCount(); pos++) {
            View delView = this.getViewByPosition(pos, R.id.delFrameLayout);
            if(delView != null)
                delView.setVisibility(isDelMode ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void convert(BaseViewHolder helper, VodInfo item) {
        TextView tvYear = helper.getView(R.id.tvYear);
        /*if (item.year <= 0) {
            tvYear.setVisibility(View.GONE);
        } else {
            tvYear.setText(String.valueOf(item.year));
            tvYear.setVisibility(View.VISIBLE);
        }*/
        tvYear.setText(ApiConfig.get().getSource(item.sourceKey).getName());
        /*TextView tvLang = helper.getView(R.id.tvLang);
        if (TextUtils.isEmpty(item.lang)) {
            tvLang.setVisibility(View.GONE);
        } else {
            tvLang.setText(item.lang);
            tvLang.setVisibility(View.VISIBLE);
        }
        TextView tvArea = helper.getView(R.id.tvArea);
        if (TextUtils.isEmpty(item.area)) {
            tvArea.setVisibility(View.GONE);
        } else {
            tvArea.setText(item.area);
            tvArea.setVisibility(View.VISIBLE);
        }

        TextView tvNote = helper.getView(R.id.tvNote);
        if (TextUtils.isEmpty(item.note)) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.note);
            tvNote.setVisibility(View.VISIBLE);
        }*/
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
}