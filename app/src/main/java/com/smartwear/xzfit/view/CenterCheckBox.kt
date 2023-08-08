package com.smartwear.xzfit.view

import android.content.Context
import android.graphics.Canvas
import androidx.appcompat.widget.AppCompatCheckBox
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

/**
 * Created by Android on 2021/10/11.
 * 通过下面属性实现CheckBox icon 居中
 * android:button="@null"
 * android:gravity="center"
 * android:drawableLeft="@drawable/XXX"
 */
class CenterCheckBox @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatCheckBox(context, attrs, defStyleAttr) {

    init {
        gravity = Gravity.CENTER
        buttonDrawable = null
    }

    override fun onDraw(canvas: Canvas) {
        //先定位居中
        val drawables = compoundDrawables
        val drawable = drawables[0]
        val gravity = gravity
        var left = 0
        if (gravity == Gravity.CENTER) {
            left = (width - drawable.intrinsicWidth - paint.measureText(text.toString())).toInt() / 2
        }
        drawable.setBounds(left, 0, left + drawable.intrinsicWidth, drawable.intrinsicHeight)
        //后绘制
        super.onDraw(canvas)
    }

    fun setLeftDrawable(@DrawableRes resId: Int) {
        setCompoundDrawables(
            ContextCompat.getDrawable(context, resId)?.apply {
                setBounds(0, 0, minimumWidth, minimumHeight)
            }, null, null, null
        )
    }

}