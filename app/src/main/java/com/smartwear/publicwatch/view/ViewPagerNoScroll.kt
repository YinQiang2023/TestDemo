package com.smartwear.publicwatch.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class ViewPagerNoScroll : ViewPager {//是否可以滑动切换界面
    /**
     * @param no scroll
     * true 不可以左右滑动
     * false 可以左右滑动
     */
    var isNoScroll = false

    constructor(context: Context, attrs: AttributeSet) : super(
        context,
        attrs
    ) {        // TODO Auto-generated constructor stub
    }

    constructor(context: Context) : super(context) {        // TODO Auto-generated constructor stub
    }

    override fun onInterceptTouchEvent(arg0: MotionEvent): Boolean {
        // TODO Auto-generated method stub
        return if (isNoScroll) {
            false
        } else {
            super.onInterceptTouchEvent(arg0)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(arg0: MotionEvent): Boolean {
        // TODO Auto-generated method stub
        return if (isNoScroll) {
            false
        } else {
            super.onTouchEvent(arg0)
        }
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        // TODO Auto-generated method stub
//		super.setCurrentItem(item, smoothScroll);
        super.setCurrentItem(item, false) //禁止页面切换时会有多界面的闪烁
    }

    override fun setCurrentItem(item: Int) {
        // TODO Auto-generated method stub
//		super.setCurrentItem(item);
        super.setCurrentItem(item, false) //禁止页面切换时会有多界面的闪烁
    }

    override fun scrollTo(x: Int, y: Int) {
        // TODO Auto-generated method stub
        super.scrollTo(x, y)
    }
}