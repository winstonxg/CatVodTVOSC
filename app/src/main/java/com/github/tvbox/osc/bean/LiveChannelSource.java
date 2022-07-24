package com.github.tvbox.osc.bean;

/**
 * @author Kenson
 * @date :2022/7/4
 * @description:
 */
public class LiveChannelSource {

    private int sourceIndex;
    private boolean isSelected = false;
    private boolean isFocused = false;

    public void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean b) {
        isSelected = b;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        isFocused = focused;
    }
}