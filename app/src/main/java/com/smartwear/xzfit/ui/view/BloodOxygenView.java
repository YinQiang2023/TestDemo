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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressLint("DrawAllocation")
public class BloodOxygenView extends View {
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

    public BloodOxygenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.bloodoxygenview, 0, 0);
        int stepbloodoxygenview_histogram_bg = typeArray.getColor(R.styleable.bloodoxygenview_bloodoxygenview_histogram_bg, getResources().getColor(R.color.transparent));
        int stepbloodoxygenview_histogram = typeArray.getColor(R.styleable.bloodoxygenview_bloodoxygenview_histogram, getResources().getColor(R.color.color_26A9D0));
        int stepbloodoxygenview_histogram_touch = typeArray.getColor(R.styleable.bloodoxygenview_bloodoxygenview_histogram_touch, getResources().getColor(R.color.color_97EEFF));
        int stepbloodoxygenview_x_text_color = typeArray.getColor(R.styleable.bloodoxygenview_bloodoxygenview_x_text_color, getResources().getColor(R.color.color_FFFFFF_50));
        isDrawX0 = typeArray.getBoolean(R.styleable.bloodoxygenview_bloodoxygenview_isDrawX0, false);

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
        paintValue.setColor(stepbloodoxygenview_histogram);

        paint_value_bg = new Paint();
        paint_value_bg.setStrokeWidth(dp2px(5));
        paint_value_bg.setAntiAlias(true);
        paint_value_bg.setStyle(Paint.Style.FILL);
        paint_value_bg.setColor(stepbloodoxygenview_histogram_bg);

        paint_value_touch = new Paint();
        paint_value_touch.setStrokeWidth(dp2px(5));
        paint_value_touch.setAntiAlias(true);
        paint_value_touch.setStyle(Paint.Style.FILL);
        paint_value_touch.setColor(stepbloodoxygenview_histogram_touch);

        paint_text = new Paint();
        paint_text.setStrokeWidth(dp2px(2));
        paint_text.setAntiAlias(true);
        paint_text.setStyle(Paint.Style.FILL);
        paint_text.setColor(stepbloodoxygenview_x_text_color);
        paint_text.setTextSize(sp2px(10));


        aFormat = new DecimalFormat(",##0");
        aFormat.applyPattern(",##0.00");

        FontMetricsInt fontMetrics = paint_text.getFontMetricsInt();
        textHeight = fontMetrics.descent - fontMetrics.ascent;
        textWidthTime = getTextWidth(paint_text, "00:00");
        textWidthDate = getTextWidth(paint_text, "00");
        textWidthDate2 = getTextWidth(paint_text, "00-00");
        textWidthGoal = getTextWidth(paint_text, "100%");

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
        canvas.drawText("70%", 0f, startY + (baseline - startY) * 3 / 4, paint_text);
        canvas.drawText("80%", 0f, startY + (baseline - startY) * 2 / 4, paint_text);
        canvas.drawText("90%", 0f, startY + (baseline - startY) * 1 / 4, paint_text);
        canvas.drawText("100%", 0f, startY + (baseline - startY) * 0 / 4, paint_text);

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
//            if (progressValue == null || progressValue.length == 0) {
//                return;
//            }
//            for (int i = 0; i < progressValue.length; i++) {
//                float value = 0f;
//                value = (progressValue[i] - 60) / 40f;
//                float x = textWidthGoal + spacing + i * (spacing + histogramWidth) + radius / 2f;
//                float y = (1 - value) * (baseline - startY) + startY;
//                canvas.drawCircle(x, y, radius, paintValue);
//            }
            if (bloodOxygenDayInfo == null || bloodOxygenDayInfo.size() == 0) {
                return;
            }
            for (int i = 0; i < bloodOxygenDayInfo.size(); i++) {
                float value = 0f;
                value = (bloodOxygenDayInfo.get(i).progressValue - 60) / 40f;
//                float x = textWidthGoal + spacing + i * (spacing + histogramWidth) + radius / 2f;

                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(bloodOxygenDayInfo.get(i).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
                        "HH:mm").split(":");
                float spacingTemp = spacing * (Float.parseFloat(timeArray[1].trim()) / 60);
                float x = textWidthGoal + spacingTemp + Float.parseFloat(timeArray[0].trim()) * (spacing + histogramWidth) + radius / 2f;
                float y = (1 - value) * (baseline - startY) + startY;
                canvas.drawCircle(x, y, radius, paintValue);
            }
        } else {
            if (bloodOxygenInfo == null || bloodOxygenInfo.size() == 0) return;
            for (int i = 0; i < bloodOxygenInfo.size(); i++) {
                float value = 0f;
                value = (bloodOxygenInfo.get(i).progressValue) / goal;
                float valueMin = (bloodOxygenInfo.get(i).progressMinValue - 60) / 40f;

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

            for (int i = 0; i < bloodOxygenDayInfo.size(); i++) {
                float value = 0f;
                value = (bloodOxygenDayInfo.get(i).progressValue - 60) / 40f;
//                float x = textWidthGoal + spacing + i * (spacing + histogramWidth) + radius / 2f;

                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(bloodOxygenDayInfo.get(i).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
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
            if (bloodOxygenDayInfo == null || bloodOxygenDayInfo.size() == 0) return;
            if (dataBlockIndex > bloodOxygenDayInfo.size() - 1) {
                dataBlockIndex = bloodOxygenDayInfo.size() - 1;
            }
        } else {
            if (bloodOxygenInfo == null || bloodOxygenInfo.size() == 0) return;
            if (dataBlockIndex > bloodOxygenInfo.size() - 1) {
                dataBlockIndex = bloodOxygenInfo.size() - 1;
            }
        }


        if (dataBlockIndex < 0) {
            dataBlockIndex = 0;
        }


        if (progressTime != null) time = progressTime[dataBlockIndex];

        if (type == TODAY) {
//            if (progressValue != null) step = progressValue[dataBlockIndex];
//            if (step != 0) {
////                if (progressTime.length == pointSize) {
//                    float value = (progressValue[dataBlockIndex] - 60) / 40f;
//                    float x = textWidthGoal + spacing + dataBlockIndex * (spacing + histogramWidth) + radius / 2f;
//                    float y = (1 - value) * (baseline - startY) + startY;
//                    canvas.drawCircle(x, y, radius, paint_value_touch);
////                } else {
////                    float value = (progressValue[dataBlockIndex]) / goal;
////                    RectF rectF = new RectF();
////                    if (progressTime.length == 7) {
////                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
////                    } else {
////                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
////                    }
////                    rectF.right = rectF.left + histogramWidth;
////                    float valueMin = (progressMinValue[dataBlockIndex] - 60) / 40f;
////                    rectF.top = (1 - value) * (baseline - startY) + startY;
////                    rectF.bottom = baseline;
////
////                    canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
////                }
//                data = progressValue[dataBlockIndex];
//            }
            if (bloodOxygenDayInfo != null && bloodOxygenDayInfo.size() > 0) step = bloodOxygenDayInfo.get(dataBlockIndex).progressValue;
            time = bloodOxygenDayInfo.get(dataBlockIndex).time;
            if (step != 0) {
//                if (progressTime.length == pointSize) {
                float value = (bloodOxygenDayInfo.get(dataBlockIndex).progressValue - 60) / 40f;
                String[] timeArray = DateUtils.getStringDate(DateUtils.getLongTime(bloodOxygenDayInfo.get(dataBlockIndex).time, DateUtils.TIME_YYYY_MM_DD_HHMMSS),
                        "HH:mm").split(":");
                float spacingTemp = spacing * (Float.parseFloat(timeArray[1].trim()) / 60);
                float x = textWidthGoal + spacingTemp + Float.parseFloat(timeArray[0].trim()) * (spacing + histogramWidth) + radius / 2f;

//                    float x = textWidthGoal + spacing + dataBlockIndex * (spacing + histogramWidth) + radius / 2f;
                float y = (1 - value) * (baseline - startY) + startY;
                if (max != -1) {
                    canvas.drawCircle(x, y, radius, paint_value_touch);
                }
//                } else {
//                    float value = (progressValue[dataBlockIndex]) / goal;
//                    RectF rectF = new RectF();
//                    if (progressTime.length == 7) {
//                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
//                    } else {
//                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
//                    }
//                    rectF.right = rectF.left + histogramWidth;
//                    float valueMin = (progressMinValue[dataBlockIndex] - 60) / 40f;
//                    rectF.top = (1 - value) * (baseline - startY) + startY;
//                    rectF.bottom = baseline;
//
//                    canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
//                }
                data = bloodOxygenDayInfo.get(dataBlockIndex).progressValue;
            }
        } else {
            if (bloodOxygenInfo != null && bloodOxygenInfo.size() > 0) step = bloodOxygenInfo.get(dataBlockIndex).progressValue;
            if (step != 0) {
//                if (progressTime.length == pointSize) {
//                    float value = (progressValue[dataBlockIndex] - 60) / 40f;
//                    float x = textWidthGoal + spacing + dataBlockIndex * (spacing + histogramWidth) + radius / 2f;
//                    float y = (1 - value) * (baseline - startY) + startY;
//                    canvas.drawCircle(x, y, radius, paint_value_touch);
//                } else {
                float value = bloodOxygenInfo.get(dataBlockIndex).progressValue / goal;
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
                float valueMin = (bloodOxygenInfo.get(dataBlockIndex).progressMinValue - 60) / 40f;
                rectF.top = (1 - value) * (baseline - startY) + startY;
                rectF.bottom = baseline;

                canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
//                }
            }
            data = bloodOxygenInfo.get(dataBlockIndex).progressValue;
            max = bloodOxygenInfo.get(dataBlockIndex).progressMaxValue;
            min = bloodOxygenInfo.get(dataBlockIndex).progressMinValue;
        }


        if (onSlidingListener != null) {
            onSlidingListener.SlidingDisOver(data, dataBlockIndex, time, step, max, min);
        }

    }

    private List<BloodOxygenView.BloodOxygenDayInfo> bloodOxygenDayInfo;

    public static class BloodOxygenDayInfo {
        public float progressValue;
        public String time;

        public BloodOxygenDayInfo() {
        }
    }

    public void setDayProgress(List<BloodOxygenView.BloodOxygenDayInfo> bloodOxygenDayInfo, String[] progress_time, int type) {
        this.bloodOxygenDayInfo = bloodOxygenDayInfo;
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

    public void setProgress(List<BloodOxygenView.BloodOxygenInfo> bloodOxygenInfo, String[] progress_time, int type) {
        this.bloodOxygenInfo = bloodOxygenInfo;
        this.progressTime = progress_time;
//        pointSize = progress_time.length;
        this.type = type;
        goal = 0;
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
        }
        postInvalidate();
    }

    private List<BloodOxygenView.BloodOxygenInfo> bloodOxygenInfo;

    public static class BloodOxygenInfo {
        public float progressValue;
        public float progressMinValue;
        public float progressMaxValue;

        public BloodOxygenInfo() {
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
        List<BloodOxygenInfo> bloodOxygenInfo = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            BloodOxygenInfo info = new BloodOxygenInfo();
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
        if (bloodOxygenInfo != null) bloodOxygenInfo.clear();
        if (bloodOxygenDayInfo != null) bloodOxygenDayInfo.clear();
        touchPos = -1;
        postInvalidate();
    }
}
