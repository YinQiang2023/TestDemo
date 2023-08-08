package com.smartwear.xzfit.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.smartwear.xzfit.R
import com.smartwear.xzfit.utils.AppUtils

class BlockProgressView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var paintLine1: Paint = Paint()
    private var paintLine2: Paint = Paint()
    private var paintLine3: Paint = Paint()
    private var paintLine4: Paint = Paint()
    private var paintLine5: Paint = Paint()
    private val roundX = 100f
    private val roundY = 100f

    init {
        paintLine1.isAntiAlias = true
        paintLine1.strokeCap = Paint.Cap.SQUARE
        paintLine1.style = Paint.Style.FILL

        paintLine2.isAntiAlias = true
        paintLine2.strokeCap = Paint.Cap.SQUARE
        paintLine2.style = Paint.Style.FILL

        paintLine3.isAntiAlias = true
        paintLine3.strokeCap = Paint.Cap.SQUARE
        paintLine3.style = Paint.Style.FILL

        paintLine4.isAntiAlias = true
        paintLine4.strokeCap = Paint.Cap.SQUARE
        paintLine4.style = Paint.Style.FILL

        paintLine5.isAntiAlias = true
        paintLine5.strokeCap = Paint.Cap.SQUARE
        paintLine5.style = Paint.Style.FILL
    }

    private fun initPaint() {
        paintLine1.color = ContextCompat.getColor(this.context, R.color.device_sport_heart1)
        paintLine2.color = ContextCompat.getColor(this.context, R.color.device_sport_heart2)
        paintLine3.color = ContextCompat.getColor(this.context, R.color.device_sport_heart3)
        paintLine4.color = ContextCompat.getColor(this.context, R.color.device_sport_heart4)
        paintLine5.color = ContextCompat.getColor(this.context, R.color.device_sport_heart5)
    }

    private fun initPaint(color1: Int = 0, color2: Int = 0, color3: Int = 0, color4: Int = 0, color5: Int = 0) {
        AppUtils.tryBlock { paintLine1.color = ContextCompat.getColor(this.context, color1) }
        AppUtils.tryBlock { paintLine2.color = ContextCompat.getColor(this.context, color2) }
        AppUtils.tryBlock { paintLine3.color = ContextCompat.getColor(this.context, color3) }
        AppUtils.tryBlock { paintLine4.color = ContextCompat.getColor(this.context, color4) }
        AppUtils.tryBlock { paintLine5.color = ContextCompat.getColor(this.context, color5) }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect1 = RectF(0f, 0f, (progress1 * width), height.toFloat())

        canvas.save()
        if (progress2 == 0f && progress3 == 0f && progress4 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect1, roundX, roundY, paintLine1)
        } else {
            canvas.drawRoundRect(rect1, roundX, roundY, paintLine1)
            canvas.drawRect((rect1.left + rect1.right) / 2, rect1.top, rect1.right, rect1.bottom, paintLine1)
        }
        canvas.save()

        var startX = this.progress1 * width
        val rect2 = RectF(startX, 0f, (startX + progress2 * width), height.toFloat())

        canvas.save()
        if (progress1 == 0f && progress3 == 0f && progress4 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect2, roundX, roundY, paintLine2)
        } else if (progress1 == 0f) {
            canvas.drawRoundRect(rect2, roundX, roundY, paintLine2)
            canvas.drawRect((rect2.left + rect2.right) / 2, rect2.top, rect2.right, rect2.bottom, paintLine2)
        } else if (progress3 == 0f && progress4 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect2, roundX, roundY, paintLine2)
            canvas.drawRect(rect2.left, rect2.top, (rect2.right + rect2.left) / 2, rect2.bottom, paintLine2)
        } else {
            canvas.drawRect(rect2, paintLine2)
        }
        canvas.save()

        startX += this.progress2 * width
        val rect3 = RectF(startX, 0f, (startX + progress3 * width), height.toFloat())

        canvas.save()
        if (progress1 == 0f && progress2 == 0f && progress4 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect3, roundX, roundY, paintLine3)
        } else if (progress1 == 0f && progress2 == 0f) {
            canvas.drawRoundRect(rect3, roundX, roundY, paintLine3)
            canvas.drawRect((rect3.left + rect3.right) / 2, rect3.top, rect3.right, rect3.bottom, paintLine3)
        } else if (progress4 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect3, roundX, roundY, paintLine3)
            canvas.drawRect(rect3.left, rect3.top, (rect3.right + rect3.left) / 2, rect3.bottom, paintLine3)
        } else {
            canvas.drawRect(rect3, paintLine3)
        }
        canvas.save()


        startX += this.progress3 * width
        val rect4 = RectF(startX, 0f, (startX + progress4 * width), height.toFloat())

        canvas.save()
        if (progress1 == 0f && progress2 == 0f && progress3 == 0f && progress5 == 0f) {
            canvas.drawRoundRect(rect4, roundX, roundY, paintLine4)
        } else if (progress1 == 0f && progress2 == 0f && progress3 == 0f) {
            canvas.drawRoundRect(rect4, roundX, roundY, paintLine4)
            canvas.drawRect((rect4.left + rect4.right) / 2, rect4.top, rect4.right, rect4.bottom, paintLine4)
        } else if (progress5 == 0f) {
            canvas.drawRoundRect(rect4, roundX, roundY, paintLine4)
            canvas.drawRect(rect4.left, rect4.top, (rect4.right + rect4.left) / 2, rect4.bottom, paintLine4)
        } else {
            canvas.drawRect(rect4, paintLine4)
        }
        canvas.save()

        startX += this.progress4 * width
        val rect5 = RectF(startX, 0f, (startX + progress5 * width), height.toFloat())

        canvas.save()
        if (progress1 == 0f && progress2 == 0f && progress3 == 0f && progress4 == 0f) {
            canvas.drawRoundRect(rect5, roundX, roundY, paintLine5)
        } else {
            canvas.drawRoundRect(rect5, roundX, roundY, paintLine5)
            canvas.drawRect(rect5.left, rect5.top, (rect5.right + rect5.left) / 2, rect5.bottom, paintLine5)
        }
        canvas.save()
    }

    private var progress1 = 0f
    private var progress2 = 0f
    private var progress3 = 0f
    private var progress4 = 0f
    private var progress5 = 0f
    private var total = 0

    fun start(progress1: Int = 0, progress2: Int = 0, progress3: Int = 0, progress4: Int = 0, progress5: Int = 0) {
        val total = progress1 + progress2 + progress3 + progress4 + progress5
        if (total != 0) {
            this.progress1 = (progress1 * 1f / total)
            this.progress2 = (progress2 * 1f / total)
            this.progress3 = (progress3 * 1f / total)
            this.progress4 = (progress4 * 1f / total)
            this.progress5 = (progress5 * 1f / total)
            initPaint()
        }
        invalidate()
    }

    fun start(
        progress1: Int = 0,
        progress2: Int = 0,
        progress3: Int = 0,
        progress4: Int = 0,
        progress5: Int = 0,
        color1: Int = 0,
        color2: Int = 0,
        color3: Int = 0,
        color4: Int = 0,
        color5: Int = 0
    ) {
        val total = progress1 + progress2 + progress3 + progress4 + progress5
        if (total != 0) {
            this.progress1 = (progress1 * 1f / total)
            this.progress2 = (progress2 * 1f / total)
            this.progress3 = (progress3 * 1f / total)
            this.progress4 = (progress4 * 1f / total)
            this.progress5 = (progress5 * 1f / total)
            initPaint(color1, color2, color3, color4, color5)
        }
        invalidate()
    }


    private var isClean = false
    fun clean() {
        isClean = true
        paintLine1.reset()
        paintLine2.reset()
        paintLine3.reset()
        paintLine4.reset()
        paintLine5.reset()
        paintLine1.color = ContextCompat.getColor(this.context, R.color.transparent)
        paintLine2.color = ContextCompat.getColor(this.context, R.color.transparent)
        paintLine3.color = ContextCompat.getColor(this.context, R.color.transparent)
        paintLine4.color = ContextCompat.getColor(this.context, R.color.transparent)
        paintLine5.color = ContextCompat.getColor(this.context, R.color.transparent)
        postInvalidate()
    }


}