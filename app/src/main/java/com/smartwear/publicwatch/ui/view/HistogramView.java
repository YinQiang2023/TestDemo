package com.smartwear.publicwatch.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;


import com.smartwear.publicwatch.R;
import com.smartwear.publicwatch.ui.user.utils.UnitConverUtils;
import com.smartwear.publicwatch.utils.DateUtils;

import java.text.DecimalFormat;

@SuppressLint("DrawAllocation")
public class HistogramView extends View {
    private final String TAG = HistogramView.class.getSimpleName();
    private final int textHeight;
    private final int textWidthTime, textWidthDate, textWidthDate2;
    private Paint paint_line, paintValue, paint_step_bg, paint_text, paint_step_touch, paintStandardLine;
    private float P_width, P_height, spacing, goal = 100, histogramWidth;
    private float[] progressValue;
    private String[] progressTime;
    private int /*type = 1 ,*/ textWidthGoal;
    private boolean noData;
    private DecimalFormat aFormat;
    private Paint paint_value;
    private boolean isgrid = true;
    private boolean isDrawX0 = false;
    private float baseline;
    private float startY;
    public final static int TODAY = 1;
    public final static int WEEK = 2;
    public final static int MONTH = 3;
    private int type = TODAY;

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typeArray = context.obtainStyledAttributes(attrs, R.styleable.histogramView, 0, 0);
        int stepHistogramView_histogram_bg = typeArray.getColor(R.styleable.histogramView_histogramView_histogram_bg, getResources().getColor(R.color.transparent));
        int stepHistogramView_histogram = typeArray.getColor(R.styleable.histogramView_histogramView_histogram, getResources().getColor(R.color.color_26A9D0));
        int stepHistogramView_histogram_touch = typeArray.getColor(R.styleable.histogramView_histogramView_histogram_touch, getResources().getColor(R.color.color_97EEFF));
        int stepHistogramView_x_text_color = typeArray.getColor(R.styleable.histogramView_histogramView_x_text_color, getResources().getColor(R.color.color_FFFFFF_50));
        isDrawX0 = typeArray.getBoolean(R.styleable.histogramView_histogramView_isDrawX0, false);

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
        paintValue.setColor(stepHistogramView_histogram);

        paint_step_bg = new Paint();
        paint_step_bg.setStrokeWidth(dp2px(5));
        paint_step_bg.setAntiAlias(true);
        paint_step_bg.setStyle(Paint.Style.FILL);
        paint_step_bg.setColor(stepHistogramView_histogram_bg);

        paint_step_touch = new Paint();
        paint_step_touch.setStrokeWidth(dp2px(5));
        paint_step_touch.setAntiAlias(true);
        paint_step_touch.setStyle(Paint.Style.FILL);
        paint_step_touch.setColor(stepHistogramView_histogram_touch);

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
//        ddd = BaseApplication.mContext.getResources().getStringArray(R.array.weekArrays)
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

        if (isgrid) {

            // Y轴-值
            if (isHandleDistance) {
                Log.i(TAG, "onDraw goal = " + goal);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 0f / 5f))), 0f, startY + (baseline - startY) * 5 / 5, paint_text);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 1f / 5f))), 0f, startY + (baseline - startY) * 4 / 5, paint_text);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 2f / 5f))), 0f, startY + (baseline - startY) * 3 / 5, paint_text);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 3f / 5f))), 0f, startY + (baseline - startY) * 2 / 5, paint_text);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 4f / 5f))), 0f, startY + (baseline - startY) * 1 / 5, paint_text);
                canvas.drawText("" + UnitConverUtils.showDistanceStyleToOneLen(String.valueOf((int) (goal * 5f / 5f))), 0f, startY + (baseline - startY) * 0 / 5, paint_text);
            } else {
                canvas.drawText("" + (int) (goal * 0f / 5f), 0f, startY + (baseline - startY) * 5 / 5, paint_text);
                canvas.drawText("" + (int) (goal * 1f / 5f), 0f, startY + (baseline - startY) * 4 / 5, paint_text);
                canvas.drawText("" + (int) (goal * 2f / 5f), 0f, startY + (baseline - startY) * 3 / 5, paint_text);
                canvas.drawText("" + (int) (goal * 3f / 5f), 0f, startY + (baseline - startY) * 2 / 5, paint_text);
                canvas.drawText("" + (int) (goal * 4f / 5f), 0f, startY + (baseline - startY) * 1 / 5, paint_text);
                canvas.drawText("" + (int) (goal * 5f / 5f), 0f, startY + (baseline - startY) * 0 / 5, paint_text);
            }

            //横线
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 0 / 5, P_width, startY + (baseline - startY) * 0 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 1 / 5, P_width, startY + (baseline - startY) * 1 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 2 / 5, P_width, startY + (baseline - startY) * 2 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 3 / 5, P_width, startY + (baseline - startY) * 3 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 4 / 5, P_width, startY + (baseline - startY) * 4 / 5, paintStandardLine);
            canvas.drawLine(textWidthGoal, startY + (baseline - startY) * 5 / 5, P_width, startY + (baseline - startY) * 5 / 5, paintStandardLine);

            //竖线
//            canvas.drawLine(P_width * 0 / 4f, 0f, P_width * 0 / 4f, baseline, paintStandardLine);
//            canvas.drawLine(P_width * 1 / 4f, 0f, P_width * 1 / 4f, baseline, paintStandardLine);
//            canvas.drawLine(P_width * 2 / 4f, 0f, P_width * 2 / 4f, baseline, paintStandardLine);
//            canvas.drawLine(P_width * 3 / 4f, 0f, P_width * 3 / 4f, baseline, paintStandardLine);
//            canvas.drawLine(P_width * 4 / 4f, 0f, P_width * 4 / 4f, baseline, paintStandardLine);

        }
        if (isDrawX0) {
            canvas.drawLine(textWidthGoal, baseline * 5 / 5, P_width, baseline * 5 / 5, paintStandardLine);
        }

        if (type == TODAY && progressTime.length == 24) {
            // spacing = histogramWidth * 2
            histogramWidth = (P_width - textWidthGoal) / (3 * baseNumber + 2);
            spacing = (P_width - textWidthGoal - histogramWidth * baseNumber) / (baseNumber + 1);

            float startX = textWidthGoal + spacing;
            canvas.drawText("00:00", startX - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("06:00", startX + 6 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("12:00", startX + 12 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("18:00", startX + 18 * (spacing + histogramWidth) - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            canvas.drawText("00:00", P_width - textWidthTime, baseline + textHeight, paint_text);
        } else if (type == WEEK && progressTime.length == 7) {
            // spacing = histogramWidth * 1
            histogramWidth = (P_width - textWidthGoal) / (baseNumber * 2 - 1 + 2);
            spacing = (P_width - textWidthGoal - histogramWidth * (progressTime.length + 2)) / (progressTime.length - 1);

            float startX = textWidthGoal + histogramWidth;
            canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[0])),
                    startX - textWidthTime / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            for (int i = 1; i < 7; i++) {
//                canvas.drawText(progressTime[i].substring(3), startX + i * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                canvas.drawText(DateUtils.getWeek(Long.parseLong(progressTime[i])),
                        startX + i * (spacing + histogramWidth) - textWidthTime / 2f - histogramWidth * 0.3f, baseline + textHeight, paint_text);
            }
        } else {
            // spacing = histogramWidth * 1.5
            if (progressTime.length > 0) {
                histogramWidth = (P_width - textWidthGoal) / (progressTime.length * (1.5f + 1) + 1);
                spacing = histogramWidth * 1.5f;

                float startX = textWidthGoal + histogramWidth;
                canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[0]), "dd"), startX - textWidthGoal / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                int index = progressTime.length / 4;
                for (int i = 1; i < 4; i++) {
//                canvas.drawText(progressTime[i * index].substring(3), startX + i * index * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                    canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[i * index]), "dd"), startX + i * index * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                }
                int k = progressTime.length - 1;
//            canvas.drawText(progressTime[k].substring(3), startX + k * (spacing + histogramWidth)- textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
                canvas.drawText(DateUtils.getStringDate(Long.parseLong(progressTime[k]), "dd"), startX + k * (spacing + histogramWidth) - textWidthDate / 2f + histogramWidth / 2f, baseline + textHeight, paint_text);
            }
        }

        if (progressValue == null || progressValue.length == 0) {
            return;
        }

        for (int i = 0; i < progressValue.length; i++) {
            float value = progressValue[i] / goal;
            if (value >= 1) {
                value = 1.0f;
            } else if (value < 0.05 && value > 0) {
                value = 0.05f;
            }
            RectF rectF = new RectF();
            rectF.top = (1 - value) * (baseline - startY) + startY;
            rectF.bottom = baseline;
            if (progressTime.length == 24) {
                rectF.left = textWidthGoal + spacing + i * (spacing + histogramWidth);
            } else if (progressTime.length == 7) {
//                rectF.left = textWidthGoal + histogramWidth + i * (spacing + histogramWidth);
                if (i > 0) {
//                    rectF.left = textWidthGoal / 2 + histogramWidth + i * (spacing + histogramWidth);
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

        if (touchPos == -1.0f) {
            if (onSlidingListener != null) {
                onSlidingListener.SlidingDisOver(-1, -1, "0", 0);
            }
            return;
        }
        int dataBlockIndex = 0;
        if (progressTime.length == 24) {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - spacing) / (spacing + histogramWidth));
        } else if (progressTime.length == 7) {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        } else {
            dataBlockIndex = (int) ((touchPos - textWidthGoal - histogramWidth) / (spacing + histogramWidth));
        }

        if (progressValue == null) return;
        if (dataBlockIndex > progressValue.length - 1) {
            dataBlockIndex = progressValue.length - 1;
        }
        if (dataBlockIndex < 0) {
            dataBlockIndex = 0;
        }

        float value = progressValue[dataBlockIndex] / goal;
        if (value >= 1) {
            value = 1.0f;
        } else if (value < 0.08 && value > 0) {
            value = 0.05f;
        }

        String time = "";
        float step = 0;
        if (progressTime != null) time = progressTime[dataBlockIndex];
        if (progressValue != null) step = progressValue[dataBlockIndex];

        if (step != 0) {
            RectF rectF = new RectF();
            if (progressTime.length == 24) {
                rectF.left = textWidthGoal + spacing + dataBlockIndex * (spacing + histogramWidth);
            } else if (progressTime.length == 7) {

                if (dataBlockIndex > 0) {
//                    rectF.left = textWidthGoal / 2 + histogramWidth + i * (spacing + histogramWidth);
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
            canvas.drawRoundRect(rectF, spacing, spacing, paint_step_touch);
        }

        if (onSlidingListener != null) {
            onSlidingListener.SlidingDisOver(progressValue[dataBlockIndex], dataBlockIndex, time, step);
        }

    }

    public void setProgress(float[] progressValue, String[] progress_time, int type) {
        if (progressValue == null || progressValue.length == 0 || progress_time == null || progress_time.length == 0) return;
        goal = 0;//目标值
        this.type = type;
        for (float v : progressValue) {
            if (v > goal) {
                goal = v;
            }
        }
        if (goal == 0) {
            goal = 100;
        } else {
            goal = goal * 1.2f;//目标值浮动百分之20
            if (goal < 5) {
                goal = 5;//最小等于5
            }
        }
        this.progressValue = progressValue;
        this.progressTime = progress_time;

        postInvalidate();
    }

    private boolean isHandleDistance = false;

    public void setProgress(float[] progressValue, String[] progress_time, int type, int input_goal, boolean is_handle_distance) {
        if (progressValue == null || progressValue.length == 0 || progress_time == null || progress_time.length == 0) return;
        goal = input_goal;//目标值
        isHandleDistance = is_handle_distance;//是否处理距离相关数据，自动适配
        this.type = type;
        for (float v : progressValue) {
            if (v > goal) {
                goal = v;
            }
        }
        if (goal == 0) {
            goal = 100;
        } else {
            goal = goal * 1.2f;//目标值浮动百分之20
            if (goal < 5) {
                goal = 5;//最小等于5
            }
        }
        this.progressValue = progressValue;
        this.progressTime = progress_time;

        postInvalidate();
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
        void SlidingDisOver(float data, int index, String time, float step);
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

    public void setMaxText(String maxText) {
        textWidthGoal = getTextWidth(paint_text, maxText);
        postInvalidate();
    }

    public void setTouchColor(int color) {
        paint_step_touch.setColor(getResources().getColor(color));
    }

    public void setDefaultColor(int color) {
        paintValue.setColor(getResources().getColor(color));
    }

    public void testData() {
        float[] progressValue = new float[7];
        String[] progressTime = new String[7];
        for (int i = 0; i < 7; i++) {
            progressValue[i] = i * 550f;
            progressValue[0] = 8000f;
//            progressValue[4] = 0f;
        }
        progressTime[0] = "1641797411000";
        progressTime[1] = "1641883811000";
        progressTime[2] = "1641970211000";
        progressTime[3] = "1642056611000";
        progressTime[4] = "1642143011000";
        progressTime[5] = "1642229411000";
        progressTime[6] = "1642315811000";
//        WEEK
        setProgress(progressValue, progressTime, WEEK);
    }
}
