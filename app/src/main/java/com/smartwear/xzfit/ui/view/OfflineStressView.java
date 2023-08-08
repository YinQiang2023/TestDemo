package com.smartwear.xzfit.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.smartwear.xzfit.R;
import com.smartwear.xzfit.utils.DateUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Android on 2023/2/7.
 */
@SuppressLint("DrawAllocation")
public class OfflineStressView extends View {
    private final int textHeight;
    private final int textWidthTime, textWidthDate, textWidthDate2, textWidthGoal;
    private Paint paint_line, paintValue, paint_value_bg, paint_text, paint_value_touch, paintStandardLine;
    private float P_width, P_height, spacing, goal = 100, histogramWidth;
    //    private float[] progressValue;
    private String[] progressTime;
    //    private int type = 1;
    private boolean noData;
    private DecimalFormat aFormat;
    private Paint paint_value;
    private boolean isgrid = true;
    private boolean isDrawX0 = false;
    private float baseline;
    private float startY;
    private float radius = 10;

    private int pointSize = 24;
    public final static int TODAY = 1;
    public final static int WEEK = 2;
    public final static int MONTH = 3;
    private int type = TODAY;

    public OfflineStressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.offlinestressview, 0, 0);
        int stepOfflineStressView_histogram_bg = typeArray.getColor(R.styleable.offlinestressview_offlinestressview_histogram_bg, getResources().getColor(R.color.transparent));
        int stepOfflineStressView_histogram = typeArray.getColor(R.styleable.offlinestressview_offlinestressview_histogram, getResources().getColor(R.color.color_26A9D0));
        int stepOfflineStressView_histogram_touch = typeArray.getColor(R.styleable.offlinestressview_offlinestressview_histogram_touch, getResources().getColor(R.color.color_97EEFF));
        int stepOfflineStressView_x_text_color = typeArray.getColor(R.styleable.offlinestressview_offlinestressview_x_text_color, getResources().getColor(R.color.color_FFFFFF_50));
        isDrawX0 = typeArray.getBoolean(R.styleable.offlinestressview_offlinestressview_isDrawX0, false);

        paintStandardLine = new Paint();
        paintStandardLine.setStrokeWidth(dp2px(1));
        paintStandardLine.setAntiAlias(true);
        paintStandardLine.setStyle(Paint.Style.FILL);
        paintStandardLine.setColor(getResources().getColor(R.color.color_FFFFFF_12));

        paint_line = new Paint();
        paint_line.setStrokeWidth(dp2px(1));
        paint_line.setAntiAlias(true);
        paint_line.setStrokeCap(Paint.Cap.ROUND);
        paint_line.setStyle(Paint.Style.FILL);

        paintValue = new Paint();
        paintValue.setStrokeWidth(dp2px(5));
        paintValue.setAntiAlias(true);
        paintValue.setStyle(Paint.Style.FILL);
        paintValue.setColor(stepOfflineStressView_histogram);

        paint_value_bg = new Paint();
        paint_value_bg.setStrokeWidth(dp2px(5));
        paint_value_bg.setAntiAlias(true);
        paint_value_bg.setStyle(Paint.Style.FILL);
        paint_value_bg.setColor(stepOfflineStressView_histogram_bg);

        paint_value_touch = new Paint();
        paint_value_touch.setStrokeWidth(dp2px(5));
        paint_value_touch.setAntiAlias(true);
        paint_value_touch.setStyle(Paint.Style.FILL);
        paint_value_touch.setColor(stepOfflineStressView_histogram_touch);

        paint_text = new Paint();
        paint_text.setStrokeWidth(dp2px(2));
        paint_text.setAntiAlias(true);
        paint_text.setStyle(Paint.Style.FILL);
        paint_text.setColor(stepOfflineStressView_x_text_color);
        paint_text.setTextSize(sp2px(10));


        aFormat = new DecimalFormat(",##0");
        aFormat.applyPattern(",##0.00");

        Paint.FontMetricsInt fontMetrics = paint_text.getFontMetricsInt();
        textHeight = fontMetrics.descent - fontMetrics.ascent;
        textWidthTime = getTextWidth(paint_text, "00:00");
        textWidthDate = getTextWidth(paint_text, "00");
        textWidthDate2 = getTextWidth(paint_text, "00-00");
        textWidthGoal = getTextWidth(paint_text, "100");

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

        canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 0 / 4, P_width, startY + (baseline - startY) * 0 / 4, paintStandardLine);
        canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 1 / 4, P_width, startY + (baseline - startY) * 1 / 4, paintStandardLine);
        canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 2 / 4, P_width, startY + (baseline - startY) * 2 / 4, paintStandardLine);
        canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 3 / 4, P_width, startY + (baseline - startY) * 3 / 4, paintStandardLine);
        canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 4 / 4, P_width, startY + (baseline - startY) * 4 / 4, paintStandardLine);

        canvas.drawText("0", 0f, startY + (baseline - startY) * 4 / 4, paint_text);
        canvas.drawText("25", 0f, startY + (baseline - startY) * 3 / 4, paint_text);
        canvas.drawText("50", 0f, startY + (baseline - startY) * 2 / 4, paint_text);
        canvas.drawText("75", 0f, startY + (baseline - startY) * 1 / 4, paint_text);
        canvas.drawText("100", 0f, startY + (baseline - startY) * 0 / 4, paint_text);

        if (isDrawX0) {
            canvas.drawLine(textWidthGoal, baseline * 4 / 4, P_width, baseline * 4 / 4, paintStandardLine);
        }

        if (type == TODAY /*progressTime.length == pointSize*/) {
            // spacing = histogramWidth * 2
            histogramWidth = (P_width - textWidthGoal) / (3 * pointSize + 2);
            spacing = (P_width - textWidthGoal - histogramWidth * pointSize) / (pointSize + 1);

            float startX = textWidthGoal + spacing;
            canvas.drawText("00:00", startX - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("06:00", startX + 6 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("12:00", startX + 12 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("18:00", startX + 18 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("00:00", P_width - textWidthTime, baseline + textHeight, paint_text);
        } else if (type == WEEK /*progressTime.length == 7*/) {
            if (progressTime != null && progressTime.length > 0) {
                // spacing = histogramWidth * 1
                histogramWidth = (P_width - textWidthGoal) / (baseNumber * 2 - 1 + 2);
                spacing = (P_width - textWidthGoal - histogramWidth * (progressTime.length + 2)) / (progressTime.length - 1);

                float startX = textWidthGoal + histogramWidth;
                canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[0])), startX - textWidthGoal / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                for (int i = 1; i < 7; i++) {
                    canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[i])),
                            startX + i * (spacing + histogramWidth) - textWidthGoal / 2f + histogramWidth * 0.3f, baseline + textHeight, paint_text);
                }
            }
        } else {
            if (progressTime != null && progressTime.length > 0) {
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


        if (type == TODAY) {
            if (dayInfo == null || dayInfo.size() == 0) {
                return;
            }
            for (int i = 0; i < dayInfo.size(); i++) {
                float value = 0f;
                value = (dayInfo.get(i).progressValue) / 100f;
//                float x = textWidthGoal + spacing + i * (spacing + histogramWidth) + radius / 2f;

                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(dayInfo.get(i).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
                        "HH:mm").split(":");
                float spacingTemp = spacing * (Float.parseFloat(timeArray[1].trim()) / 60);
                float x = textWidthGoal + spacingTemp + Float.parseFloat(timeArray[0].trim()) * (spacing + histogramWidth) + radius / 2f;
                float y = (1 - value) * (baseline - startY) + startY;
                canvas.drawCircle(x, y, radius, paintValue);
            }
        } else {
            if (dataInfo == null || dataInfo.size() == 0) return;
            for (int i = 0; i < dataInfo.size(); i++) {
                float value = 0f;
                value = (dataInfo.get(i).progressValue) / goal;
                float valueMin = (dataInfo.get(i).progressMinValue) / 100f;

                RectF rectF = new RectF();
                rectF.top = (1 - value) * (baseline - startY) + startY;
                rectF.bottom = baseline;
                if (progressTime.length == 7) {
//                    rectF.left = textWidthGoal + histogramWidth + i * (spacing + histogramWidth);
                    if (i > 0) {
                        rectF.left = textWidthGoal - textWidthGoal / 4 + histogramWidth + i * (spacing + histogramWidth);
                    } else {
                        rectF.left = textWidthGoal - textWidthGoal / 4 + histogramWidth + i * (spacing + histogramWidth);
                    }
                } else {
                    rectF.left = textWidthGoal + histogramWidth + i * (spacing + histogramWidth);
                }
                rectF.right = rectF.left + histogramWidth;
                canvas.drawRoundRect(rectF, histogramWidth / 2, histogramWidth / 2, paintValue);
            }
        }

        if (touchPos == -1.0f) {
            if (onSlidingListener != null) {
                onSlidingListener.SlidingDisOver(-1, -1, "0", 0, -1f, -1f);
            }
            return;
        }
        int dataBlockIndex = 0;
        String time = "";
        float step = 0;
        float max = -1f;
        float min = -1f;
        float data = 0f;

        if (type == TODAY /*progressTime.length == 24*/) {

            for (int i = 0; i < dayInfo.size(); i++) {
                float value = 0f;
                value = (dayInfo.get(i).progressValue) / 100f;
//                float x = textWidthGoal + spacing + i * (spacing + histogramWidth) + radius / 2f;

                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(dayInfo.get(i).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
                        "HH:mm").split(":");
                float spacingTemp = spacing * (Float.parseFloat(timeArray[1].trim()) / 60);
                float x = textWidthGoal + spacingTemp + Float.parseFloat(timeArray[0].trim()) * (spacing + histogramWidth) + radius / 2f;
                if ((touchPos) >= (x - radius) && (touchPos) <= (x + radius)) {
                    dataBlockIndex = i;
                    max = i; //日视图使用max记录真实选中下标
                    break;
                }

            }

//            dataBlockIndex = (int) ((touchPos - textWidthGoal - spacing) / (spacing + histogramWidth));
        } else if (type == WEEK /*progressTime.length == 7*/) {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        } else {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        }

        if (type == TODAY) {
//            if (progressValue == null) return;
//            if (dataBlockIndex > progressValue.length - 1) {
//                dataBlockIndex = progressValue.length - 1;
//            }
            if (dayInfo == null || dayInfo.size() == 0) return;
            if (dataBlockIndex > dayInfo.size() - 1) {
                dataBlockIndex = dayInfo.size() - 1;
            }
        } else {
            if (dataInfo == null || dataInfo.size() == 0) return;
            if (dataBlockIndex > dataInfo.size() - 1) {
                dataBlockIndex = dataInfo.size() - 1;
            }
        }


        if (dataBlockIndex < 0) {
            dataBlockIndex = 0;
        }


        if (progressTime != null) time = progressTime[dataBlockIndex];

        if (type == TODAY) {
            if (dayInfo != null && dayInfo.size() > 0) step = dayInfo.get(dataBlockIndex).progressValue;
            time = dayInfo.get(dataBlockIndex).time;
            if (step != 0) {
//                if (progressTime.length == pointSize) {
                float value = (dayInfo.get(dataBlockIndex).progressValue) / 100f;
                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(dayInfo.get(dataBlockIndex).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
                        "HH:mm").split(":");
                float spacingTemp = spacing * (Float.parseFloat(timeArray[1].trim()) / 60);
                float x = textWidthGoal + spacingTemp + Float.parseFloat(timeArray[0].trim()) * (spacing + histogramWidth) + radius / 2f;

//                    float x = textWidthGoal + spacing + dataBlockIndex * (spacing + histogramWidth) + radius / 2f;
                float y = (1 - value) * (baseline - startY) + startY;
                if (max != -1) {
                    canvas.drawCircle(x, y, radius, paint_value_touch);
                }

                data = dayInfo.get(dataBlockIndex).progressValue;
            }
        } else {
            if (dataInfo != null && dataInfo.size() > 0) step = dataInfo.get(dataBlockIndex).progressValue;
            if (step != 0) {
                float value = dataInfo.get(dataBlockIndex).progressValue / goal;
                RectF rectF = new RectF();
                if (progressTime.length == 7) {
//                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                    if (dataBlockIndex > 0) {
                        rectF.left = textWidthGoal - textWidthGoal / 4 + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                    } else {
                        rectF.left = textWidthGoal - textWidthGoal / 4 + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                    }
                } else {
                    rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                }
                rectF.right = rectF.left + histogramWidth;
                float valueMin = (dataInfo.get(dataBlockIndex).progressMinValue) / 100f;
                rectF.top = (1 - value) * (baseline - startY) + startY;
                rectF.bottom = baseline;

                canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
//                }
            }
            data = dataInfo.get(dataBlockIndex).progressValue;
            max = dataInfo.get(dataBlockIndex).progressMaxValue;
            min = dataInfo.get(dataBlockIndex).progressMinValue;
        }


        if (onSlidingListener != null) {
            onSlidingListener.SlidingDisOver(data, dataBlockIndex, time, step, max, min);
        }

    }

    private List<DayInfo> dayInfo;

    public static class DayInfo {
        public float progressValue;
        public String time;

        public DayInfo() {
        }
    }

    public void setDayProgress(List<DayInfo> dayInfo, String[] progress_time, int type) {
        this.dayInfo = dayInfo;
        this.progressTime = progress_time;
        pointSize = progress_time.length;
        this.type = type;
        postInvalidate();
    }

//    public void setProgress(float[] progressValue, String[] progress_time , int type) {
//        this.progressValue = progressValue;
//        this.progressTime = progress_time;
//        pointSize = progress_time.length;
//        this.type = type;
//        postInvalidate();
//    }

//    private float[] progressMinValue;
//
//    public void setProgress(float[] progressValue, float[] progressMinValue, String[] progress_time) {
//        this.progressValue = progressValue;
//        this.progressMinValue = progressMinValue;
//        this.progressTime = progress_time;
//
//        goal = 0;
//        for (float v : progressValue) {
//            if (v > goal) {
//                goal = v;
//            }
//        }
//        if (goal == 0) {
//            goal = 100;
//        } else {
//            goal = goal * 1.2f;
//            if (goal < 4) {
//                goal = 4;
//            }
//        }
//
//        postInvalidate();
//    }

    public void setProgress(List<DataInfo> bloodOxygenInfo, String[] progress_time, int type) {
        this.dataInfo = bloodOxygenInfo;
        this.progressTime = progress_time;
//        pointSize = progress_time.length;
        this.type = type;
        goal = 100;
        /*goal = 0;
        for (int i = 0; i < bloodOxygenInfo.size(); i++) {
            float v = bloodOxygenInfo.get(i).progressValue;
            if (v > goal) {
                goal = v;
            }
        }
        if (goal == 0) {
            goal = 100;
        } else {
            goal = goal * 1.2f;
            if (goal < 4) {
                goal = 4;
            }
        }*/
        postInvalidate();
    }

    private List<DataInfo> dataInfo;

    public static class DataInfo {
        public float progressValue;
        public float progressMinValue;
        public float progressMaxValue;

        public DataInfo() {
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
        void SlidingDisOver(float data, int index, String time, float step, float max, float min);
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


    public void testData() {
        String[] progressTime = new String[7];
        List<DataInfo> bloodOxygenInfo = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            DataInfo info = new DataInfo();
            Random rand = new Random();
            info.progressValue = (rand.nextInt(30) + 70) * 1f;
            info.progressMaxValue = (rand.nextInt(30) + 70) * 1f;
            info.progressMinValue = (rand.nextInt(30) + 70) * 1f;
            bloodOxygenInfo.add(info);
        }

        progressTime[0] = "1641797411000";
        progressTime[1] = "1641883811000";
        progressTime[2] = "1641970211000";
        progressTime[3] = "1642056611000";
        progressTime[4] = "1642143011000";
        progressTime[5] = "1642229411000";
        progressTime[6] = "1642315811000";
//        WEEK
        setProgress(bloodOxygenInfo, progressTime, WEEK);
    }

    public void clearData() {
        if (progressTime != null) progressTime = new String[0];
        if (dataInfo != null) dataInfo.clear();
        if (dayInfo != null) dayInfo.clear();
        touchPos = -1;
        postInvalidate();
    }
}
