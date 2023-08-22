package com.smartwear.publicwatch.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.MonthView

class SimpleMonthView(context: Context) : MonthView(context) {
    private var mRadius = 0

    init {
    }

    override fun onPreviewHook() {
        mRadius = Math.min(mItemWidth, mItemHeight) / 11 * 4
    }

    override fun onDrawSelected(canvas: Canvas, calendar: Calendar?, x: Int, y: Int, hasScheme: Boolean): Boolean {
        val cx = x + mItemWidth / 2
        val cy = y + mItemHeight / 2
        canvas.drawCircle(cx.toFloat(), cy.toFloat(), mRadius.toFloat(), mSelectedPaint)
        return false
    }

    override fun onDrawScheme(canvas: Canvas, calendar: Calendar, x: Int, y: Int) {
    }

    override fun onDrawText(canvas: Canvas, calendar: Calendar, x: Int, y: Int, hasScheme: Boolean, isSelected: Boolean) {
        val baselineY = mTextBaseLine + y
        val cx = x + mItemWidth / 2
        if (isSelected) {
            canvas.drawText(
                calendar.day.toString(),
                cx.toFloat(),
                baselineY,
                mSelectTextPaint
            )
//            if(calendar.scheme == MoreSportActivity.sportString){
//                drawBottomCircle(canvas , cx.toFloat() , baselineY,"#989CA0")
//            }
        } else if (hasScheme) {
            canvas.drawText(
                calendar.day.toString(),
                cx.toFloat(),
                baselineY,
                if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mSchemeTextPaint else mOtherMonthTextPaint
            )
//            if(calendar.scheme == MoreSportActivity.sportString){
//                drawBottomCircle(canvas , cx.toFloat() , baselineY,"#989CA0")
//            }
        } else {
            canvas.drawText(
                calendar.day.toString(), cx.toFloat(), baselineY,
                if (calendar.isCurrentDay) mCurDayTextPaint else if (calendar.isCurrentMonth) mCurMonthTextPaint else mOtherMonthTextPaint
            )
        }
    }

    val paint = Paint()

    private fun drawBottomCircle(canvas: Canvas, x: Float, y: Float, color: String) {
        paint.color = Color.parseColor(color)
        canvas.drawCircle(x.toFloat(), y + 10, 5f, paint)
    }
}