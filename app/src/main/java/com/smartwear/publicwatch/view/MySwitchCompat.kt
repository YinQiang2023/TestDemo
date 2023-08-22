package com.smartwear.publicwatch.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.SwitchCompat


/**
 * Created by Android on 2021/11/17.
 * SwitchCompat 滑动改变状态同时调用click
 */
class MySwitchCompat @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : SwitchCompat(context, attrs) {

    private var mTouchX = 0f
    private var mTouchY = 0f

    private var isCallOnClick = false

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val x = ev.x
                val y = ev.y
                mTouchX = x
                mTouchY = y
                isCallOnClick = false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = ev.x
                val y = ev.y
                if (Math.abs(x - mTouchX) > 50 ||
                    Math.abs(y - mTouchY) > 50
                ) {
                    isCallOnClick = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isCallOnClick && super.onTouchEvent(ev)) {
                    callOnClick()
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}