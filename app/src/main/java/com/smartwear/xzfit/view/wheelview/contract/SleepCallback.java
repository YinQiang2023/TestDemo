package com.smartwear.xzfit.view.wheelview.contract;

public interface SleepCallback {
    /**
     * 单项条目选择回调
     *
     * @param position 选中项的索引
     * @param item     选中项的内容
     */
    void onOptionSelected(int position, String item);
}
