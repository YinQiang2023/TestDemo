package com.smartwear.publicwatch.ui.adapter;

/**
 * Created by dai on 17/5/26.
 */

public interface MZHolderCreator<VH extends MZViewHolder> {
    /**
     * 创建ViewHolder
     *
     * @return
     */
    public VH createViewHolder();
}
