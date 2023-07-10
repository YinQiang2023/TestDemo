package com.jwei.publicone.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.jwei.publicone.R;
import com.jwei.publicone.utils.DateUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Android on 2022/10/12.
 */
@SuppressLint("DrawAllocation")
public class PressureView extends View {
    private final int textHeight;
    private final int textWidthTime, textWidthDate, textWidthDate2, textWidthGoal;
    private Paint paint_line, paintValue, paint_step_bg, paint_text, paint_value_touch, paintStandardLine;
    private float P_width, P_height, spacing, goal = 160, histogramWidth;
    private float[] progressValue;
    private String[] progressTime;
    //    private int type = 1;
    private boolean noData;
    private DecimalFormat aFormat;
    private Paint paint_value;
    private boolean isgrid = true;
    private boolean isDrawX0 = false;
    private float baseline;
    private float startY;

    private int pointSize = 288;
    private float radius = 8;
    public final static int TODAY = 1;
    public final static int WEEK = 2;
    public final static int MONTH = 3;
    private int type = TODAY;
    private int[] textWidthGoalWeek = new int[7];

    public PressureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.pressureview, 0, 0);
        int steppressureView_histogram_bg = typeArray.getColor(R.styleable.pressureview_pressureview_histogram_bg, getResources().getColor(R.color.transparent));
        int steppressureView_histogram = typeArray.getColor(R.styleable.pressureview_pressureview_histogram, getResources().getColor(R.color.color_26A9D0));
        int steppressureView_histogram_touch = typeArray.getColor(R.styleable.pressureview_pressureview_histogram_touch, getResources().getColor(R.color.color_97EEFF));
        int steppressureView_x_text_color = typeArray.getColor(R.styleable.pressureview_pressureview_x_text_color, getResources().getColor(R.color.color_FFFFFF_50));
        isDrawX0 = typeArray.getBoolean(R.styleable.pressureview_pressureview_isDrawX0, false);

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
        paintValue.setStrokeWidth(dp2px(2));
        paintValue.setAntiAlias(true);
        paintValue.setStyle(Paint.Style.FILL);
        paintValue.setColor(steppressureView_histogram);

        paint_step_bg = new Paint();
        paint_step_bg.setStrokeWidth(dp2px(5));
        paint_step_bg.setAntiAlias(true);
        paint_step_bg.setStyle(Paint.Style.FILL);
        paint_step_bg.setColor(steppressureView_histogram_bg);

        paint_value_touch = new Paint();
        paint_value_touch.setStrokeWidth(dp2px(5));
        paint_value_touch.setAntiAlias(true);
        paint_value_touch.setStyle(Paint.Style.FILL);
        paint_value_touch.setColor(steppressureView_histogram_touch);

        paint_text = new Paint();
        paint_text.setStrokeWidth(dp2px(2));
        paint_text.setAntiAlias(true);
        paint_text.setStyle(Paint.Style.FILL);
        paint_text.setColor(steppressureView_x_text_color);
        paint_text.setTextSize(sp2px(9));


        aFormat = new DecimalFormat(",##0");
        aFormat.applyPattern(",##0.00");

        Paint.FontMetricsInt fontMetrics = paint_text.getFontMetricsInt();
        textHeight = fontMetrics.descent - fontMetrics.ascent;
        textWidthTime = getTextWidth(paint_text, "00:00");
        textWidthDate = getTextWidth(paint_text, "00");
        textWidthDate2 = getTextWidth(paint_text, "00-00");
        textWidthGoal = getTextWidth(paint_text, "160");

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

        if (isgrid) {
            // x 轴
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 0 / 5, P_width, startY + (baseline - startY) * 0 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 1 / 5, P_width, startY + (baseline - startY) * 1 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 2 / 5, P_width, startY + (baseline - startY) * 2 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 3 / 5, P_width, startY + (baseline - startY) * 3 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 4 / 5, P_width, startY + (baseline - startY) * 4 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 5 / 5, P_width, startY + (baseline - startY) * 5 / 5, paintStandardLine);

//            canvas.drawText("0", 0f, startY + (baseline - startY) * 4 / 4, paint_text);
//            canvas.drawText("40", 0f, startY + (baseline - startY) * 3 / 4, paint_text);
//            canvas.drawText("80", 0f, startY + (baseline - startY) * 2 / 4, paint_text);
//            canvas.drawText("120", 0f, startY + (baseline - startY) * 1 / 4, paint_text);
//            canvas.drawText("160", 0f, startY + (baseline - startY) * 0 / 4, paint_text);
            canvas.drawText("" + (int) (goal * 0f / 5f), 0f, startY + (baseline - startY) * 5 / 5, paint_text);
            canvas.drawText("" + (int) (goal * 1f / 5f), 0f, startY + (baseline - startY) * 4 / 5, paint_text);
            canvas.drawText("" + (int) (goal * 2f / 5f), 0f, startY + (baseline - startY) * 3 / 5, paint_text);
            canvas.drawText("" + (int) (goal * 3f / 5f), 0f, startY + (baseline - startY) * 2 / 5, paint_text);
            canvas.drawText("" + (int) (goal * 4f / 5f), 0f, startY + (baseline - startY) * 1 / 5, paint_text);
            canvas.drawText("" + (int) (goal * 5f / 5f), 0f, startY + (baseline - startY) * 0 / 5, paint_text);
        }
        if (isDrawX0) {
            canvas.drawLine(textWidthGoal, baseline * 5 / 5, P_width, baseline * 5 / 5, paintStandardLine);
        }

        if (progressTime.length == 0) return;

        if (type == TODAY/* && progressTime.length == pointSize*/) {
            // spacing = histogramWidth * 2
//            histogramWidth = 0;
//            histogramWidth = (P_width - textWidthGoal) / (3 * pointSize + 2);
//            spacing = (P_width - textWidthGoal - histogramWidth * pointSize) / (pointSize + 1);
            histogramWidth = 0;
            spacing = (P_width - textWidthGoal) / (pointSize - 1);

            float startX = textWidthGoal;
            canvas.drawText("00:00", startX - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
//            canvas.drawText("06:00", startX + 72 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
//            canvas.drawText("12:00", startX + 144 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
//            canvas.drawText("18:00", startX + 216 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("06:00", startX + (0.25f * pointSize) * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("12:00", startX + (0.5f * pointSize) * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("18:00", startX + (0.75f * pointSize) * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("00:00", P_width - textWidthTime, baseline + textHeight, paint_text);
        } else if (type == WEEK/*progressTime.length == 7*/) {
            // spacing = histogramWidth * 1
            histogramWidth = (P_width - textWidthGoal) / (baseNumber * 2 - 1 + 2);
            spacing = (P_width - textWidthGoal - histogramWidth * (progressTime.length + 2)) / (progressTime.length - 1);

            float startX = textWidthGoal + histogramWidth;
            canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[0])),
                    startX - textWidthGoal / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            textWidthGoalWeek[0] = getTextWidth(paint_text, DateUtils.getWeek(Long.parseLong(progressTime[0])));
            for (int i = 1; i < 7; i++) {
                canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[i])),
                        startX + i * (spacing + histogramWidth) - textWidthDate / 2f - histogramWidth * 0.3f, baseline + textHeight, paint_text);
                textWidthGoalWeek[i] = getTextWidth(paint_text, DateUtils.getWeek(Long.parseLong(progressTime[i])));
            }
        } else {
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

        if (type == TODAY) {
            if (progressValue == null || progressValue.length == 0) {
                return;
            }
        } else {
            if (pressureInfo == null || pressureInfo.size() == 0) return;
        }

        if (type == TODAY/*progressTime.length == pointSize*/) {
            paintValue.setStyle(Paint.Style.STROKE);
            Path mPath = new Path();
            mPath.lineTo(0, 0);
            boolean isFirstNo0 = false;
            for (int i = 0; i < progressValue.length; i++) {
                float value = progressValue[i] / goal;
                float x = textWidthTime / 2f + i * spacing;
                float y = (1 - value) * (baseline - startY) + startY;

                if (isFirstNo0) {
                    if (progressValue[i] == 0) {
                        canvas.drawPath(mPath, paintValue);
                        mPath.reset();
                        isFirstNo0 = false;
                    } else {
                        //if (progressValue[i] != 0) {
                        mPath.lineTo(x, y);
                        if (i == progressValue.length - 1) {
                            canvas.drawPath(mPath, paintValue);
                        }
                    }
                } else {
                    if (progressValue[i] > 0) {
                        isFirstNo0 = true;
                        mPath.moveTo(x, y);
                        //断开的间隔点不画
                        /*paintValue.setStyle(Paint.Style.FILL);
                        canvas.drawCircle(x, y, paintValue.getStrokeWidth() / 2, paintValue);
                        paintValue.setStyle(Paint.Style.STROKE);*/
                        if (i == progressValue.length - 1) {
                            mPath.lineTo(x, y);
                            canvas.drawPath(mPath, paintValue);
                        }
                    }
                }
            }
        } else {
            paintValue.setStyle(Paint.Style.FILL);
            for (int i = 0; i < pressureInfo.size(); i++) {
                float value = pressureInfo.get(i).progressValue / goal;
                if (value >= 1) {
                    value = 1.0f;
                } else if (value < 0.05 && value > 0) {
                    value = 0.05f;
                }
                RectF rectF = new RectF();
                rectF.top = (1 - value) * (baseline - startY) + startY;
                rectF.bottom = baseline;
                if (progressTime.length == 7) {
                    if (i > 0) {
                        rectF.left = textWidthGoal / 2 + histogramWidth + i * (spacing + histogramWidth);
                    } else {
                        rectF.left = textWidthGoal + histogramWidth + i * (spacing + histogramWidth);
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
                onSlidingListener.SlidingDisOver(-1, -1, "0", 0, -1, -1);
            }
            return;
        }
        int dataBlockIndex = 0;
        if (type == TODAY/*progressTime.length == pointSize*/) {
            dataBlockIndex = (int) ((touchPos - textWidthGoal) / (spacing + histogramWidth));
        } else if (type == WEEK/*progressTime.length == 7*/) {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        } else {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        }

        if (type == TODAY) {
            if (progressValue == null) return;
            if (dataBlockIndex > progressValue.length - 1) {
                dataBlockIndex = progressValue.length - 1;
            }
        } else {
            if (pressureInfo == null || pressureInfo.size() == 0) return;
            if (dataBlockIndex > pressureInfo.size() - 1) {
                dataBlockIndex = pressureInfo.size() - 1;
            }
        }

        if (dataBlockIndex < 0) {
            dataBlockIndex = 0;
        }

        String time = "";
        float step = 0;
        float data = 0f;
        float max = -1f;
        float min = -1f;
        if (progressTime != null) time = progressTime[dataBlockIndex];

        if (type == TODAY) {
            float value = progressValue[dataBlockIndex] / goal;
            if (value >= 1) {
                value = 1.0f;
            } else if (value < 0.08 && value > 0) {
                value = 0.05f;
            }
            if (progressValue != null) step = progressValue[dataBlockIndex];
            if (step != 0) {
                if (type == TODAY/*progressTime.length == pointSize*/) {
//                    float x = textWidthGoal + dataBlockIndex * spacing;
//                    float y = (1 - value) * (baseline - startY) + startY;

                    float x = textWidthTime / 2f + dataBlockIndex * spacing;
                    float y = (1 - value) * (baseline - startY) + startY;

                    canvas.drawCircle(x, y, radius, paint_value_touch);
                    if (x <= P_width) {
                        canvas.drawCircle(x, y, radius, paint_value_touch);
                    } else {
                        canvas.drawCircle(P_width, y, radius, paint_value_touch);
                    }
//                } else {
//                    RectF rectF = new RectF();
//                    if (progressTime.length == 7) {
//                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
//                    } else {
//                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
//                    }
//                    rectF.top = (1 - value) * (baseline - startY) + startY;
//                    rectF.right = rectF.left + histogramWidth;
//                    rectF.bottom = baseline;
//                    canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
                }
            }
            data = progressValue[dataBlockIndex];
        } else {
            float value = pressureInfo.get(dataBlockIndex).progressValue / goal;
            if (value >= 1) {
                value = 1.0f;
            } else if (value < 0.08 && value > 0) {
                value = 0.05f;
            }
            if (pressureInfo != null && pressureInfo.size() > 0) step = pressureInfo.get(dataBlockIndex).progressValue;
            if (step != 0) {
                RectF rectF = new RectF();
                if (progressTime.length == 7) {
                    if (dataBlockIndex > 0) {
                        rectF.left = textWidthGoal / 2 + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                    } else {
                        rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                    }
                } else {
                    rectF.left = textWidthGoal + histogramWidth + dataBlockIndex * (spacing + histogramWidth);
                }
                rectF.top = (1 - value) * (baseline - startY) + startY;
                rectF.right = rectF.left + histogramWidth;
                rectF.bottom = baseline;
                canvas.drawRoundRect(rectF, spacing, spacing, paint_value_touch);
            }
            data = pressureInfo.get(dataBlockIndex).progressValue;
            max = pressureInfo.get(dataBlockIndex).maxValue;
            min = pressureInfo.get(dataBlockIndex).minValue;
        }


        if (onSlidingListener != null) {
            onSlidingListener.SlidingDisOver(data, dataBlockIndex, time, step, max, min);
        }

    }

    public void setProgress(float[] progressValue, String[] progress_time, int type) {
        this.progressValue = progressValue;
        this.progressTime = progress_time;
        pointSize = progress_time.length;
        this.type = type;
        goal = 0;
        for (float v : progressValue) {
            if (v > goal) {
                goal = v;
            }
        }
        if (goal == 0) {
            goal = 160;
        } else {
//            goal = goal * 1.2f;
            goal = goal + goal * 0.2f;
            if (goal < 5) {
                goal = 5;
            }
        }
        postInvalidate();
    }

    public void setProgress(List<PressureView.PressureInfo> pressureInfo, String[] progress_time, int type) {
        this.pressureInfo = pressureInfo;
        this.progressTime = progress_time;
//        pointSize = progress_time.length;
        this.type = type;
        goal = 0;
        for (int i = 0; i < pressureInfo.size(); i++) {
            float v = pressureInfo.get(i).progressValue;
            if (v > goal) {
                goal = v;
            }
        }
        if (goal == 0) {
            goal = 160;
        } else {
//            goal = goal * 1.2f;
            goal = goal + goal * 0.2f;
            if (goal < 5) {
                goal = 5;
            }
        }
        postInvalidate();
    }

    private List<PressureView.PressureInfo> pressureInfo;

    public static class PressureInfo {
        public float progressValue;
        public float maxValue;
        public float minValue;

        public PressureInfo() {
        }
    }

    private float touchPos = -1.0f;

    public void setTouchPos(float eventX) {
        touchPos = eventX;
    }

    private PressureView.OnSlidingListener onSlidingListener;

    public void setOnSlidingListener(PressureView.OnSlidingListener onSlidingListener) {
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
        List<PressureView.PressureInfo> pressureInfo = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Random rand = new Random();
            PressureView.PressureInfo info = new PressureView.PressureInfo();
            info.progressValue = (rand.nextInt(30) + 70) * 1f;
            info.maxValue = (rand.nextInt(30) + 70) * 1f;
            info.minValue = (rand.nextInt(30) + 70) * 1f;
            pressureInfo.add(info);
        }


        progressTime[0] = "1641797411000";
        progressTime[1] = "1641883811000";
        progressTime[2] = "1641970211000";
        progressTime[3] = "1642056611000";
        progressTime[4] = "1642143011000";
        progressTime[5] = "1642229411000";
        progressTime[6] = "1642315811000";
//        WEEK
        setProgress(pressureInfo, progressTime, WEEK);
    }

    public void clearData() {
        if (progressValue != null) progressValue = new float[0];
        if (pressureInfo != null) pressureInfo.clear();
        if (progressTime != null) progressTime = new String[0];
        touchPos = -1;
        postInvalidate();
    }

}
