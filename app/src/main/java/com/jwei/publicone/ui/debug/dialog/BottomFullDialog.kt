package com.jwei.publicone.ui.debug.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import com.jwei.publicone.R


/**
 *                 _ooOoo_
 *                o8888888o
 *                88" . "88
 *                (| -_- |)
 *                 O\ = /O
 *             ____/`---'\____
 *           .   ' \\| |// `.
 *            / \\||| : |||// \
 *          / _||||| -:- |||||- \
 *            | | \\\ - /// | |
 *          | \_| ''\---/'' |_/ |
 *           \ .-\__ `-` ___/-. /
 *        ___`. .' /--.--\ `. . __
 *     ."" '< `.___\_<|>_/___.' >'"".
 *    | | : `- \`.;`\ _ /`;.`/ - ` : | |
 *      \ \ `-. \_ __\ /__ _/ .-` / /
 *======`-.____`-.___\_____/___.-`____.-*======
 *                 `=---='
 *
 *         Buddha bless, never BUG!
 */
abstract class BottomFullDialog(context: Context?) : Dialog(context!!, R.style.BottomFullDialog) {
    @SuppressLint("ClickableViewAccessibility")
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         requestWindowFeature(Window.FEATURE_NO_TITLE)
         val contentView: View = layoutInflater.inflate(
             setLayout(), null
         )
         super.setContentView(contentView)
         contentView.setOnTouchListener { _, _ ->
             this@BottomFullDialog.dismiss()
             true
         }

        window?.setGravity(Gravity.BOTTOM) //设置显示在底部
         val display: Display = window?.windowManager?.defaultDisplay!!
        val layoutParams: WindowManager.LayoutParams = window?.attributes!!
         layoutParams.width=display.width;
         window?.attributes = layoutParams
    }

    abstract fun setLayout():Int
}