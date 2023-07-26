package com.jwei.xzfit.view

import android.content.Context
import android.util.AttributeSet
import com.amap.api.maps.TextureMapView
import android.view.MotionEvent

/**
 * Created by Android on 2021/10/12.
 */
class AMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : TextureMapView(context, attrs) {

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }
}