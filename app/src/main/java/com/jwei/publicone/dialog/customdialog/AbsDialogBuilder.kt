package com.jwei.publicone.dialog.customdialog

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes

/**
 * <pre>
 * 定义dialog构造者抽象接口
 * <pre>
 *
 */
public abstract class AbsDialogBuilder {
    //构造dialog实例
    //abstract fun builder(): AbsDialogBuilder
    //修改dialog
    abstract fun setContentView(view: View): AbsDialogBuilder
    abstract fun setContentView(@LayoutRes viewId: Int): AbsDialogBuilder
    abstract fun setCancelable(flag: Boolean): AbsDialogBuilder
    abstract fun setGravity(gravity: Int): AbsDialogBuilder
    abstract fun setBackgroundDrawable(drawable: Drawable?): AbsDialogBuilder
    abstract fun setdim(isDim: Boolean, amount: Float): AbsDialogBuilder
    abstract fun setAnimations(@StyleRes resId: Int): AbsDialogBuilder
    abstract fun setContentPadding(left: Int, top: Int, right: Int, bottom: Int): AbsDialogBuilder
    abstract fun setHeight(height: Int): AbsDialogBuilder
    abstract fun setWidth(width: Int): AbsDialogBuilder
    abstract fun setOffsetX(x: Int): AbsDialogBuilder
    abstract fun setOffsetY(y: Int): AbsDialogBuilder
    abstract fun addFlags(flags: Int): AbsDialogBuilder
    abstract fun setOnShowDismissListener(onShowDismissListener: MyDialog.OnShowDismissListener): AbsDialogBuilder

    //获取dialog
    abstract fun build(): MyDialog
}