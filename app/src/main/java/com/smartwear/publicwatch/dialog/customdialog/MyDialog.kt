package com.smartwear.publicwatch.dialog.customdialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.view.inputmethod.InputMethodManager

class MyDialog : Dialog {

    private lateinit var mContext: Context

    constructor(context: Context) : super(context) {
        inits(context)
    }

    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        inits(context)
    }

    constructor(context: Context, cancelable: Boolean, cancelListener: DialogInterface.OnCancelListener?) : super(context, cancelable, cancelListener) {
        inits(context)
    }

    private fun inits(context: Context) {
        mContext = context
    }

    override fun show() {
        //Unable to add window -- token android.os.BinderProxy is not valid; is your activity running?
        if (mContext is Activity && !(this.mContext as Activity).isDestroyed) {
            //LogUtils.d("show dialog $this")
            super.show()
            mOnShowDismissListener?.onShow()
        }
    }

    override fun dismiss() {
        //dialog关闭时关闭软键盘
        val view: View? = currentFocus
        val mInputMethodManager: InputMethodManager =
            mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mInputMethodManager.hideSoftInputFromWindow(
            view?.windowToken, InputMethodManager.RESULT_UNCHANGED_SHOWN
        )

        super.dismiss()
        mOnShowDismissListener?.onDismiss()
    }

    private var mOnShowDismissListener: OnShowDismissListener? = null

    fun setOnShowDismissListener(onShowListener: OnShowDismissListener) {
        this.mOnShowDismissListener = onShowListener
    }

    interface OnShowDismissListener {
        fun onShow()
        fun onDismiss()
    }

}