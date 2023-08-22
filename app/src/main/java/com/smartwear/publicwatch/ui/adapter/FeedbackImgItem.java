package com.smartwear.publicwatch.ui.adapter;

/**
 * FileName: MultiItemEntity
 */
public class FeedbackImgItem {
    public static final int IMG_ADD = 1;
    public static final int IMG = 2;
    private int itemType;
    public Object extra;

    public FeedbackImgItem(int itemType) {
        this.itemType = itemType;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }
}
