package com.smartwear.xzfit.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.smartwear.xzfit.R;
import com.smartwear.xzfit.utils.DateUtils;

import java.text.DecimalFormat;
import java.util.List;

@SuppressLint("DrawAllocation")
public class SleepView extends View {
    private final int textHeight;
    private final int textWidthTime, textWidthDate, textWidthDate2, textWidthGoal;
    private Paint paint_line, paintValue, paint_step_bg, paint_text, paint_touch, paintStandardLine;
    private float P_width, P_height, spacing, goal = 100, histogramWidth;
    private float[] progressValue;
    private String[] progressTime;
    private int type = 1;
    private boolean noData;
    private DecimalFormat aFormat;
    private Paint paint_value;
    private boolean isDrawX0 = false;
    private float baseline;
    private float startY;

    int sleepView_bg_1;
    int sleepView_bg_2;
    int sleepView_bg_3;
    int sleepView_bg_4;
    int sleepView_bg_touch;

    public SleepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.sleepView, 0, 0);
        sleepView_bg_1 = typeArray.getColor(R.styleable.sleepView_sleepView_bg_1, getResources().getColor(R.color.transparent));
        sleepView_bg_2 = typeArray.getColor(R.styleable.sleepView_sleepView_bg_2, getResources().getColor(R.color.transparent));
        sleepView_bg_3 = typeArray.getColor(R.styleable.sleepView_sleepView_bg_3, getResources().getColor(R.color.transparent));
        sleepView_bg_4 = typeArray.getColor(R.styleable.sleepView_sleepView_bg_4, getResources().getColor(R.color.transparent));
        sleepView_bg_touch = typeArray.getColor(R.styleable.sleepView_sleepView_bg_touch, getResources().getColor(R.color.transparent));
        int stepHistogramView_x_text_color = typeArray.getColor(R.styleable.sleepView_sleepView_x_text_color, getResources().getColor(R.color.color_FFFFFF_50));

        paintStandardLine = new Paint();
        paintStandardLine.setStrokeWidth(dp2px(1));
        paintStandardLine.setAntiAlias(true);
        paintStandardLine.setStyle(Paint.Style.FILL);
        paintStandardLine.setColor(getResources().getColor(R.color.color_FFFFFF_12));

        paint_line = new Paint();
        paint_line.setStrokeWidth(dp2px(1));
        paint_line.setAntiAlias(true);
        paint_line.setStrokeCap(Cap.ROUND);
        paint_line.setStyle(Paint.Style.FILL);

        paintValue = new Paint();
        paintValue.setStrokeWidth(dp2px(5));
        paintValue.setAntiAlias(true);
        paintValue.setStyle(Paint.Style.FILL);
        paintValue.setColor(sleepView_bg_1);

        paint_touch = new Paint();
        paint_touch.setStrokeWidth(dp2px(5));
        paint_touch.setAntiAlias(true);
        paint_touch.setStyle(Paint.Style.FILL);
        paint_touch.setColor(sleepView_bg_touch);

        paint_text = new Paint();
        paint_text.setStrokeWidth(dp2px(2));
        paint_text.setAntiAlias(true);
        paint_text.setStyle(Paint.Style.FILL);
        paint_text.setColor(stepHistogramView_x_text_color);
        paint_text.setTextSize(sp2px(10));


        aFormat = new DecimalFormat(",##0");
        aFormat.applyPattern(",##0.00");

        FontMetricsInt fontMetrics = paint_text.getFontMetricsInt();
        textHeight = fontMetrics.descent - fontMetrics.ascent;
        textWidthTime = getTextWidth(paint_text, "00:00");
        textWidthDate = getTextWidth(paint_text, "00");
        textWidthDate2 = getTextWidth(paint_text, "00-00");
        textWidthGoal = getTextWidth(paint_text, "00");

        progressTime = new String[24];
        for (int i = 0; i < 24; i++) {
            progressTime[i] = "00";
        }
    }

    private int baseNumber = 24;


    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        P_width = getWidth();
        P_height = getHeight();
        startY = textHeight * 1.2f;
        baseline = P_height - textHeight * 1.2f;

        if (type == 1) {
            canvas.drawLine(0, startY + (baseline - startY) * 0 / 4, P_width, startY + (baseline - startY) * 0 / 4, paintStandardLine);
            canvas.drawLine(0, startY + (baseline - startY) * 4 / 4, P_width, startY + (baseline - startY) * 4 / 4, paintStandardLine);
            if (!startTime.equals("--:--") && !startTime.equals("--")) {
                canvas.drawText(DateUtils.getStringDate(Long.parseLong(startTime.trim()), "HH:mm"), 0, baseline + textHeight, paint_text);
            } else {
                canvas.drawText(startTime, 0, baseline + textHeight, paint_text);
            }

            if (!endTime.equals("--:--") && !endTime.equals("--")) {
                canvas.drawText(DateUtils.getStringDate(Long.parseLong(endTime.trim()), "HH:mm"), P_width - textWidthTime, baseline + textHeight, paint_text);
            } else {
                canvas.drawText(endTime, P_width - textWidthTime, baseline + textHeight, paint_text);
            }

            if (progressSleepTime == null || progressSleepTime.length == 0) {
                return;
            }
            int index = 0;
            RectF rectF = new RectF();
            for (int i = 0; i < progressSleepTime.length; i++) {
                int sleepTime = progressSleepTime[i];
                rectF.left = (index * 1f / totalTime) * P_width;
                //  类型高度占比1 : 2 : 3 : 4
                switch (progressSleepType[i]) {
                    case 1:
                        rectF.top = startY + 0.6f * (baseline - startY);
                        rectF.bottom = baseline;
                        paintValue.setColor(sleepView_bg_1);
                        break;
                    case 2:
                        rectF.top = startY + 0.3f * (baseline - startY);
                        rectF.bottom = baseline - 0.4f * (baseline - startY);
                        paintValue.setColor(sleepView_bg_2);
                        break;
                    case 3:
                        rectF.top = startY + 0.1f * (baseline - startY);
                        rectF.bottom = baseline - 0.7f * (baseline - startY);
                        paintValue.setColor(sleepView_bg_3);
                        break;
                    case 4:
                        rectF.top = startY + 0.0f * (baseline - startY);
                        rectF.bottom = baseline - 0.9f * (baseline - startY);
                        paintValue.setColor(sleepView_bg_4);
                        break;
                }
                index = index + sleepTime;
                rectF.right = (index * 1f / totalTime) * P_width;
                canvas.drawRoundRect(rectF, 5, 5, paintValue);
            }

        } else {
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 0 / 4, P_width, startY + (baseline - startY) * 0 / 4, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 4 / 4, P_width, startY + (baseline - startY) * 4 / 4, paintStandardLine);
//            canvas.drawText("0", 0f, startY + (baseline - startY) * 4 / 4, paint_text);
//            canvas.drawText("3", 0f, startY + (baseline - startY) * 3 / 4, paint_text);
//            canvas.drawText("6", 0f, startY + (baseline - startY) * 2 / 4, paint_text);
//            canvas.drawText("9", 0f, startY + (baseline - startY) * 1 / 4, paint_text);
//            canvas.drawText("12", 0f, startY + (baseline - startY) * 0 / 4, paint_text);
            if (progressTime.length == 7) {
                baseNumber = 12;
                histogramWidth = (P_width - textWidthGoal) / (baseNumber * 2 - 1 + 2);
                spacing = (P_width - textWidthGoal - histogramWidth * (progressTime.length + 2)) / (progressTime.length - 1);

                float startX = textWidthGoal + histogramWidth;
                canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[0])), startX - textWidthDate2 / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                for (int i = 1; i < 7; i++) {
                    canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[i])), startX + i * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                }

            } else {
                if (progressTime.length > 0) {
                    // spacing = histogramWidth * 1.5
                    histogramWidth = (P_width - textWidthGoal) / (progressTime.length * (1.5f + 1) + 1);
                    spacing = histogramWidth * 1.5f;

                    float startX = textWidthGoal + histogramWidth;
                    canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[0]), "dd"), startX - textWidthDate2 / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                    int index = progressTime.length / 4;
                    for (int i = 1; i < 4; i++) {
                        canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[i * index]), "dd"), startX + i * index * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                    }
                    int k = progressTime.length - 1;
                    canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[k]), "dd"), startX + k * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                }
            }


            if (sleepInfo == null || sleepInfo.size() == 0) {
                return;
            }
            for (int i = 0; i < sleepInfo.size(); i++) {
                int sleepTime1 = sleepInfo.get(i).sleepTime1;
                int sleepTime2 = sleepInfo.get(i).sleepTime2;
                int sleepTime3 = sleepInfo.get(i).sleepTime3;
                int sleepTime4 = sleepInfo.get(i).sleepTime4;
                RectF rectF = new RectF();
                rectF.left = textWidthGoal + histogramWidth + i * (spacing + histogramWidth);
                rectF.right = rectF.left + histogramWidth;
                rectF.bottom = baseline;

                rectF.top = startY + (1 - (sleepTime1 + sleepTime2 + sleepTime3 + sleepTime4) / totalGoal) * (baseline - startY);
                paintValue.setColor(sleepView_bg_4);
                canvas.drawRect(rectF, paintValue);

                rectF.top = startY + (1 - (sleepTime3 + sleepTime2 + sleepTime1) / totalGoal) * (baseline - startY);
                paintValue.setColor(sleepView_bg_3);
                canvas.drawRect(rectF, paintValue);


                rectF.top = startY + (1 - (sleepTime2 + sleepTime1) / totalGoal) * (baseline - startY);
                paintValue.setColor(sleepView_bg_2);
                canvas.drawRect(rectF, paintValue);

                rectF.top = startY + (1 - sleepTime1 / totalGoal) * (baseline - startY);
                paintValue.setColor(sleepView_bg_1);
                canvas.drawRect(rectF, paintValue);
            }

        }

        if (touchPos == -1.0f) {
            if (onSlidingListener != null) {
                onSlidingListener.SlidingDisOver(-1, "");
            }
            return;
        }
        int dataBlockIndex = 0;
        if (type == 1) {
            int index = 0;
            for (int i = 0; i < progressSleepTime.length; i++) {
                index = index + progressSleepTime[i];
                if (touchPos < (P_width * index * 1f / totalTime)) {
                    dataBlockIndex = i;
                    break;
                }
            }
            RectF rectF = new RectF();
            int sleepTime = progressSleepTime[dataBlockIndex];
            rectF.left = ((index - sleepTime) * 1f / totalTime) * P_width;
            //  类型高度占比1 : 2 : 3 : 4
            switch (progressSleepType[dataBlockIndex]) {
                case 1:
                    rectF.top = startY + 0.6f * (baseline - startY);
                    rectF.bottom = baseline;
                    paint_touch.setColor(sleepView_bg_touch);
                    break;
                case 2:
                    rectF.top = startY + 0.3f * (baseline - startY);
                    rectF.bottom = baseline - 0.4f * (baseline - startY);
                    paint_touch.setColor(sleepView_bg_touch);
                    break;
                case 3:
                    rectF.top = startY + 0.1f * (baseline - startY);
                    rectF.bottom = baseline - 0.7f * (baseline - startY);
                    paint_touch.setColor(sleepView_bg_touch);
                    break;
                case 4:
                    rectF.top = startY + 0.0f * (baseline - startY);
                    rectF.bottom = baseline - 0.9f * (baseline - startY);
                    paint_touch.setColor(sleepView_bg_touch);
                    break;
            }
            rectF.right = (index * 1f / totalTime) * P_width;
            canvas.drawRoundRect(rectF, 5, 5, paint_touch);

        } else {
            if (sleepInfo.size() == 7) {
                dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
            } else {
                dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
            }
            if (dataBlockIndex >= 0 && dataBlockIndex < sleepInfo.size()) {
                int sleepTime1 = sleepInfo.get(dataBlockIndex).sleepTime1;
                int sleepTime2 = sleepInfo.get(dataBlockIndex).sleepTime2;
                int sleepTime3 = sleepInfo.get(dataBlockIndex).sleepTime3;
                int sleepTime4 = sleepInfo.get(dataBlockIndex).sleepTime4;

                RectF rectF = new RectF();
                rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                rectF.right = rectF.left + histogramWidth;
                rectF.bottom = baseline;
                rectF.top = startY + (1 - (sleepTime1 + sleepTime2 + sleepTime3 + sleepTime4) / totalGoal) * (baseline - startY);
                paintValue.setColor(sleepView_bg_touch);
                canvas.drawRect(rectF, paintValue);
            }

        }
        if (onSlidingListener != null) {
            String timeTemp = "";
            if (type == 1) {
                if (dataBlockIndex >= 0 && dataBlockIndex < progressSleepTime.length)
                    timeTemp = progressSleepTime[dataBlockIndex] + "";
            } else {
                if (dataBlockIndex >= 0 && dataBlockIndex < progressTime.length)
                    timeTemp = progressTime[dataBlockIndex];
            }
            onSlidingListener.SlidingDisOver(dataBlockIndex, timeTemp);
        }

    }

    private int[] progressSleepTime;
    private int[] progressSleepType;
    private String startTime = "--:--";
    private String endTime = "--:--";
    private int totalTime;

    public void setProgress(int[] progressSleepTime, int[] progressSleepType, String startTime, String endTime) {
        this.progressSleepTime = progressSleepTime;
        this.progressSleepType = progressSleepType;
        this.startTime = startTime;
        this.endTime = endTime;
        totalTime = 0;
        this.type = 1;
        for (int j : progressSleepTime) {
            totalTime = totalTime + j;
        }
        postInvalidate();
    }

    private List<SleepInfo> sleepInfo;
    float totalGoal = 12 * 60f;

    public void setProgress(List<SleepInfo> sleepInfo, String[] progressTime, int type) {
        clearData();
        if (sleepInfo == null || sleepInfo.size() == 0 || progressTime == null || progressTime.length == 0) return;
        this.goal = 12;
        totalGoal = goal * 60f;
        this.type = type;
        this.sleepInfo = sleepInfo;
        this.progressTime = progressTime;
        postInvalidate();
    }

    public static class SleepInfo {
        public int sleepTime1;
        public int sleepTime2;
        public int sleepTime3;
        public int sleepTime4;

        public SleepInfo() {
        }
    }

    private float touchPos = -1.0f;

    public void setTouchPos(float eventX) {
        touchPos = eventX;
    }

    private OnSlidingListener onSlidingListener;

    public void setOnSlidingListener(OnSlidingListener onSlidingListener) {
        this.onSlidingListener = onSlidingListener;
    }

    public interface OnSlidingListener {
        void SlidingDisOver(int index, String time);
    }

    private int getTextWidth(Paint paint, String str) {
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    private int dp2px(int value) {
        float v = getContext().getResources().getDisplayMetrics().density;
        return (int) (v * value + 0.5f);
    }

    private int sp2px(int value) {
        float v = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (v * value + 0.5f);
    }

    int lastX;
    int lastY;
    private float mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = x - lastX;
                int dy = y - lastY;
                if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= mSlop) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                } else {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        lastX = x;
        lastY = y;
        return super.dispatchTouchEvent(event);
    }

    public void clearData() {
        if (progressSleepTime != null) progressSleepTime = new int[0];
        if (progressSleepType != null) progressSleepType = new int[0];
        if (sleepInfo != null) sleepInfo.clear();
        if (progressTime != null) progressTime = new String[0];
        touchPos = -1;
        postInvalidate();
    }
}
