package com.github.tvbox.osc.ui.adapter;

import android.graphics.Color;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveChannel;
import com.github.tvbox.osc.bean.LiveChannelSource;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2021/1/12
 * @description:
 */
public class LiveChannelSourceAdapter extends BaseQuickAdapter<LiveChannelSource, BaseViewHolder> {
    public LiveChannelSourceAdapter() {
        super(R.layout.item_live_channel_source, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, LiveChannelSource item) {
        TextView tvChannel = helper.getView(R.id.tvChannel);
        tvChannel.setText("信号源 " + (item.getSourceIndex() + 1));
        if (item.isSelected() && !item.isFocused()) {
            tvChannel.setTextColor(mContext.getResources().getColor(R.color.color_1890FF));
        } else {
            tvChannel.setTextColor(Color.WHITE);
        }
    }
}