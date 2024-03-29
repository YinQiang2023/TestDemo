package com.smartwear.publicwatch.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.utils.LogUtils

class RoundProgressView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var angleProgress: Float = 0.0f
    private var paint: Paint = Paint()
    private var centerY: Float = 0.0f
    private var centerX: Float = 0.0f

    init {
        paint.strokeWidth = dp2px(10).toFloat()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = ContextCompat.getColor(context!!, R.color.transparent);
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        centerX = width / 2f
        centerY = height / 2f

        val rectF = RectF()
        rectF.left = 0f + paint.strokeWidth / 2f
        rectF.top = 0f + paint.strokeWidth / 2f
        rectF.right = width.toFloat() - paint.strokeWidth / 2f
        rectF.bottom = height.toFloat() - paint.strokeWidth / 2f

        paint.color = ContextCompat.getColor(context!!, R.color.color_D93F6A_15);
        canvas.drawArc(rectF, -45f, 270f, false, paint)

        paint.color = ContextCompat.getColor(context!!, R.color.color_D93F6A)
        canvas.drawArc(rectF, -45f, angleProgress * 270f, false, paint)
    }

    fun setProgress(angleProgress: Float) {
        LogUtils.i("RoundProgressView", "setProgress()")
        this.angleProgress = angleProgress
    }

    private fun dp2px(value: Int): Int {
        val v = context.resources.displayMetrics.density
        return (v * value + 0.5f).toInt()
    }


}
