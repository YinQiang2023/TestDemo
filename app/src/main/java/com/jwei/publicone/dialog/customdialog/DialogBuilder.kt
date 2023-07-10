package com.jwei.publicone.dialog.customdialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.annotation.StyleRes

/**
 * <pre>
 * dialog构造者
 * </pre>
 */
public class DialogBuilder(var mContext: Context) : AbsDialogBuilder() {

    //具体dialog
    private lateinit var mMyDialog: MyDialog

    //dialog附属属性
    private var mWindow: Window? = null
    private var mLp: WindowManager.LayoutParams? = null

    fun builder(): AbsDialogBuilder {
        mMyDialog = MyDialog(mContext)
        return this
    }

    /**
     * 设置Dialog自定义布局
     * @param view Dialog自定义布局view
     * @return this@DialogBuilder
     * */
    override fun setContentView(view: View): AbsDialogBuilder {
        mMyDialog.setContentView(view)
        mWindow = mMyDialog.window!!
        mLp = mWindow?.attributes
        //设置透明背景
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return this
    }

    /**
     * 设置Dialog自定义布局
     * @param viewId Dialog自定义布局Id
     * @return this@DialogBuilder
     * */
    override fun setContentView(@LayoutRes viewId: Int): AbsDialogBuilder {
        mMyDialog.setContentView(viewId)
        mWindow = mMyDialog.window
        mLp = mWindow?.attributes
        //设置透明背景
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return this
    }

    /**
     * dialog弹出后会点击屏幕或物理返回键dialog是否消失
     * @param flag
     * @return this@DialogBuilder
     * */
    override fun setCancelable(flag: Boolean): AbsDialogBuilder {
        mMyDialog.setCancelable(flag)
        return this
    }

    /**
     * 设置dialog Gravity
     * @param gravity  eg : Gravity.BOTTOM 底部显现dialog
     * @return this@DialogBuilder
     * */
    override fun setGravity(gravity: Int): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        mWindow!!.setGravity(gravity)
        return this
    }

    /**
     * 将此dialog的背景更改为自定义Drawable
     * @param drawable
     * @return this@DialogBuilder
     * */
    override fun setBackgroundDrawable(drawable: Drawable?): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        mWindow!!.setBackgroundDrawable(drawable)
        return this
    }

    /**
     * 设置背景是否模糊  已经模糊度
     * @param isDim 是否模糊
     * @param amount 模糊度 0.0f完全不暗，1.0f全暗
     * */
    override fun setdim(isDim: Boolean, amount: Float): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        val lp = mWindow!!.attributes
        if (isDim) {
            lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
            lp.dimAmount = amount
            mWindow!!.attributes = lp
        }
        return this
    }

    /**
     * 设置此 dialog 进出动画
     * @param resId
     * @return this@DialogBuilder
     * */
    override fun setAnimations(@StyleRes resId: Int): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        mWindow!!.setWindowAnimations(resId)
        return this
    }

    /**
     * 设置此dialog的内容的Padding
     * unit : px
     * @param left 左 padding
     * @param top
     * @param right
     * @param bottom
     * @return this@DialogBuilder
     * */
    override fun setContentPadding(left: Int, top: Int, right: Int, bottom: Int): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        mWindow!!.decorView.setPadding(left, top, right, bottom)
        return this
    }

    /**
     * 设置此窗口的内容的height
     * unit : px
     * @param height
     * @return this@DialogBuilder
     * */
    override fun setHeight(height: Int): AbsDialogBuilder {
        if (mLp == null) showThrow()
        mLp!!.height = height
        return this
    }

    /**
     * 设置此窗口的内容的width
     * unit : px
     * @param width
     * @return this@DialogBuilder
     * */
    override fun setWidth(width: Int): AbsDialogBuilder {
        if (mLp == null) showThrow()
        mLp!!.width = width
        return this
    }

    /**
     * 设置此窗口 x轴正位移
     * unit : px
     * @param x
     * @return this@DialogBuilder
     * */
    override fun setOffsetX(x: Int): AbsDialogBuilder {
        if (mLp == null) showThrow()
        mLp!!.x = x
        return this
    }

    /**
     * 设置此窗口 y轴正位移
     * unit : px
     * @param y
     * @return this@DialogBuilder
     * */
    override fun setOffsetY(y: Int): AbsDialogBuilder {
        if (mLp == null) showThrow()
        mLp!!.y = y
        return this
    }

    override fun addFlags(flags: Int): AbsDialogBuilder {
        if (mWindow == null) showThrow()
        mWindow!!.addFlags(flags)
        return this
    }

    /**
     * 设置dialog show dismiss 监听
     * */
    override fun setOnShowDismissListener(onShowDismissListener: MyDialog.OnShowDismissListener): DialogBuilder {
        mMyDialog.setOnShowDismissListener(onShowDismissListener)
        return this
    }


    /**
     * buildDialog
     * @return dialog
     * */
    override fun build(): MyDialog {
        if (mWindow == null) showThrow()
        mWindow!!.attributes = mLp
        return mMyDialog
    }

    /**
     * 提示异常
     * */
    fun showThrow() {
        throw IllegalStateException("You must call with().setContentView()  first")
    }


}