package com.github.tvbox.osc.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.tv.widget.AudioWaveView;
import com.github.tvbox.osc.ui.tv.widget.Epginfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MyEpgAdapter extends BaseAdapter {

    private List<Epginfo> data;
    private Context context;
    public static float fontSize = 20;
    private int defaultSelection = 0;
    private int defaultShiyiSelection = 0;
    private boolean ShiyiSelection = false;
    private String shiyiDate = null;
    private String currentEpgDate = null;
    private int focusSelection = -1;
    SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MyEpgAdapter(List<Epginfo> data, Context context, int i, boolean t) {
        currentEpgDate = timeFormat.format(new Date());
        this.data = data;
        this.context = context;
        this.defaultSelection = i;
        this.ShiyiSelection = t;
    }

    public void updateData(Date epgDate, List<Epginfo> data) {
        currentEpgDate = timeFormat.format(epgDate);
        focusSelection = -1;
        defaultSelection = -1;
        this.data = data;
        notifyDataSetChanged();
    }

    public void setSelection(int i) {
        this.defaultSelection = i;
        notifyDataSetChanged();
    }

    public int getFocusSelection() {
        return focusSelection;
    }

    public void setFocusSelection(int focusSelection) {
        notifyDataSetChanged();
        this.focusSelection = focusSelection;
    }

    public void setShiyiSelection(int i, boolean t) {
        this.defaultShiyiSelection = i;
        this.shiyiDate = t ? currentEpgDate : null;
        ShiyiSelection = t;
        notifyDataSetChanged();
    }

    public void setFontSize(float f) {
        fontSize = f;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Epginfo getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.epglist_item, viewGroup, false);
        }
        TextView textview = (TextView) view.findViewById(R.id.tv_epg_name);
        TextView timeview = (TextView) view.findViewById(R.id.tv_epg_time);
        TextView shiyi = (TextView) view.findViewById(R.id.shiyi);
        AudioWaveView wqddg_AudioWaveView = (AudioWaveView) view.findViewById(R.id.wqddg_AudioWaveView);
        wqddg_AudioWaveView.setVisibility(View.GONE);
        if (i < data.size()) {
            Epginfo info = data.get(i);
            if (new Date().compareTo(info.startdateTime) >= 0 && new Date().compareTo(info.enddateTime) <= 0) {
                shiyi.setVisibility(View.VISIBLE);
                shiyi.setBackgroundColor(Color.YELLOW);
                shiyi.setText("直播");
                shiyi.setTextColor(Color.RED);
            } else if (new Date().compareTo(info.enddateTime) > 0) {
                shiyi.setVisibility(View.VISIBLE);
                shiyi.setBackgroundColor(Color.BLUE);
                shiyi.setTextColor(Color.WHITE);
                shiyi.setText("回看");
            } else if (new Date().compareTo(info.startdateTime) < 0) {
                shiyi.setVisibility(View.VISIBLE);
                shiyi.setBackgroundColor(Color.GRAY);
                shiyi.setTextColor(Color.BLACK);
                shiyi.setText("预约");
            } else {
                shiyi.setVisibility(View.GONE);
            }

            textview.setText(info.title);
            timeview.setText(info.start + "--" + info.end);
            int textColor = context.getResources().getColor(R.color.color_B4000000);
            textview.setTextColor(textColor);
            timeview.setTextColor(textColor);
            Log.e("roinlong", "getView: " + i);
            if (ShiyiSelection == false) {
                Date now = new Date();
                if (now.compareTo(info.startdateTime) >= 0 && now.compareTo(info.enddateTime) <= 0) {
                    wqddg_AudioWaveView.setVisibility(View.VISIBLE);
                    if(i != focusSelection) {
                        textview.setTextColor(Color.rgb(0, 153, 255));
                        timeview.setTextColor(Color.rgb(0, 153, 255));
                    }
                    textview.setFreezesText(true);
                    timeview.setFreezesText(true);
                } else {
                    wqddg_AudioWaveView.setVisibility(View.GONE);
                }
            } else {
                if (i == this.defaultShiyiSelection && currentEpgDate.equals(shiyiDate)) {
                    wqddg_AudioWaveView.setVisibility(View.VISIBLE);
                    if(i != focusSelection) {
                        textview.setTextColor(Color.rgb(0, 153, 255));
                        timeview.setTextColor(Color.rgb(0, 153, 255));
                    }
                    textview.setFreezesText(true);
                    timeview.setFreezesText(true);
                    shiyi.setText("回看中");
                    shiyi.setTextColor(Color.RED);
                    shiyi.setBackgroundColor(Color.rgb(12, 255, 0));
                } else {
                    wqddg_AudioWaveView.setVisibility(View.GONE);
                }
            }
        }
        return view;
    }
}

