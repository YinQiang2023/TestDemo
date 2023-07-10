/*
 * Copyright (c) 2016-present 贵州纳雍穿青人李裕江<1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.jwei.publicone.view.wheelview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;


import com.jwei.publicone.R;
import com.jwei.publicone.view.wheelview.annotation.ItemTextAlign;
import com.jwei.publicone.view.wheelview.annotation.TimeMode;
import com.jwei.publicone.view.wheelview.contract.OnTimeMeridiemSelectedListener;
import com.jwei.publicone.view.wheelview.contract.OnTimeSelectedListener;
import com.jwei.publicone.view.wheelview.contract.TimeFormatter;
import com.jwei.publicone.view.wheelview.contract.WheelFormatter;
import com.jwei.publicone.view.wheelview.entity.TimeEntity;
import com.jwei.publicone.view.wheelview.impl.SimpleTimeFormatter;

import java.util.Arrays;
import java.util.List;

/**
 * 时间滚轮控件
 *
 * @since 2021/6/5 16:20
 */
@SuppressWarnings("unused")
public class TimeWheelLayout extends BaseWheelLayout {
    private NumberWheelView hourWheelView;
    private NumberWheelView minuteWheelView;
    private NumberWheelView secondWheelView;
    private TextView hourLabelView;
    private TextView minuteLabelView;
    private TextView secondLabelView;
    private WheelView meridiemWheelView;
    private TimeEntity startValue;
    private TimeEntity endValue;
    private Integer selectedHour;
    private Integer selectedMinute;
    private Integer selectedSecond;
    private boolean isAnteMeridiem;
    private int timeMode;
    private OnTimeSelectedListener onTimeSelectedListener;
    private OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener;

    public TimeWheelLayout(Context context) {
        super(context);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimeWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_picker_time;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.TimeWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(hourWheelView, minuteWheelView, secondWheelView, meridiemWheelView);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        hourWheelView = findViewById(R.id.wheel_picker_time_hour_wheel);
        minuteWheelView = findViewById(R.id.wheel_picker_time_minute_wheel);
        secondWheelView = findViewById(R.id.wheel_picker_time_second_wheel);
        hourLabelView = findViewById(R.id.wheel_picker_time_hour_label);
        minuteLabelView = findViewById(R.id.wheel_picker_time_minute_label);
        secondLabelView = findViewById(R.id.wheel_picker_time_second_label);
        meridiemWheelView = findViewById(R.id.wheel_picker_time_meridiem_wheel);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        setTextSize(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_itemTextSize,
                (int) (15 * context.getResources().getDisplayMetrics().scaledDensity)));
        setVisibleItemCount(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.TimeWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_itemTextColor, 0xFF888888));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_itemSpace,
                (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_indicatorColor, 0xFFEE3333));
        setIndicatorSize(typedArray.getDimension(R.styleable.TimeWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.TimeWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.TimeWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.TimeWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.TimeWheelLayout_wheel_curvedMaxAngle, 90));
        setTextAlign(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
        setTimeMode(typedArray.getInt(R.styleable.TimeWheelLayout_wheel_timeMode, TimeMode.HOUR_24_NO_SECOND));
        String hourLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_hourLabel);
        String minuteLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_minuteLabel);
        String secondLabel = typedArray.getString(R.styleable.TimeWheelLayout_wheel_secondLabel);
        setTimeLabel(hourLabel, minuteLabel, secondLabel);
        setTimeFormatter(new SimpleTimeFormatter(this));
        setRange(TimeEntity.target(0, 0, 0),
                TimeEntity.target(23, 59, 59), TimeEntity.now());
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            selectedHour = (Integer) hourWheelView.getItem(position);
            selectedMinute = null;
            selectedSecond = null;
            changeMinute(selectedHour);
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            selectedMinute = (Integer) minuteWheelView.getItem(position);
            selectedSecond = null;
            changeSecond();
            timeSelectedCallback();
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            selectedSecond = (Integer) secondWheelView.getItem(position);
            timeSelectedCallback();
        }
    }

    @Override
    public void onWheelScrollStateChanged(WheelView view, int state) {
        int id = view.getId();
        if (id == R.id.wheel_picker_time_hour_wheel) {
            minuteWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
            secondWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_minute_wheel) {
            hourWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
            secondWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
            return;
        }
        if (id == R.id.wheel_picker_time_second_wheel) {
            hourWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
            minuteWheelView.setEnabled(state == WheelView.SCROLL_STATE_IDLE);
        }
    }

    private void timeSelectedCallback() {
        if (onTimeSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond);
                }
            });
        }
        if (onTimeMeridiemSelectedListener != null) {
            secondWheelView.post(new Runnable() {
                @Override
                public void run() {
                    onTimeMeridiemSelectedListener.onTimeSelected(selectedHour, selectedMinute, selectedSecond, isAnteMeridiem());
                }
            });
        }
    }

    public void setTimeMode(@TimeMode int timeMode) {
        this.timeMode = timeMode;
        hourWheelView.setVisibility(View.VISIBLE);
        hourLabelView.setVisibility(View.VISIBLE);
        minuteWheelView.setVisibility(View.VISIBLE);
        minuteLabelView.setVisibility(View.VISIBLE);
        secondWheelView.setVisibility(View.VISIBLE);
        secondLabelView.setVisibility(View.VISIBLE);
        meridiemWheelView.setVisibility(View.GONE);
        if (timeMode == TimeMode.NONE) {
            hourWheelView.setVisibility(View.GONE);
            hourLabelView.setVisibility(View.GONE);
            minuteWheelView.setVisibility(View.GONE);
            minuteLabelView.setVisibility(View.GONE);
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
            this.timeMode = timeMode;
            return;
        }
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            secondWheelView.setVisibility(View.GONE);
            secondLabelView.setVisibility(View.GONE);
        }
        if (isHour12Mode()) {
            meridiemWheelView.setVisibility(View.VISIBLE);
            meridiemWheelView.setData(Arrays.asList("AM", "PM"));
        }
    }

    public boolean isHour12Mode() {
        return timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_12_HAS_SECOND;
    }

    /**
     * 设置日期时间范围
     */
    public void setRange(TimeEntity startValue, TimeEntity endValue) {
        setRange(startValue, endValue, null);
    }

    /**
     * 设置日期时间范围
     */
    public void setRange(TimeEntity startValue, TimeEntity endValue, TimeEntity defaultValue) {
        if (startValue == null) {
            startValue = TimeEntity.target(isHour12Mode() ? 1 : 0, 0, 0);
        }
        if (endValue == null) {
            endValue = TimeEntity.target(isHour12Mode() ? 12 : 23, 59, 59);
        }
        if (endValue.toTimeInMillis() < startValue.toTimeInMillis()) {
            throw new IllegalArgumentException("Ensure the start time is less than the time date");
        }
        this.startValue = startValue;
        this.endValue = endValue;
        if (defaultValue != null) {
            isAnteMeridiem = defaultValue.getHour() <= 12;
            defaultValue.setHour(wrapHour(defaultValue.getHour()));
            selectedHour = defaultValue.getHour();
            selectedMinute = defaultValue.getMinute();
            selectedSecond = defaultValue.getSecond();
        }
        changeHour();
        changeAnteMeridiem();
    }

    public void setDefaultValue(@NonNull final TimeEntity defaultValue) {
        setRange(startValue, endValue, defaultValue);
    }

    public void setTimeFormatter(final TimeFormatter timeFormatter) {
        if (timeFormatter == null) {
            return;
        }
        hourWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatHour((Integer) value);
            }
        });
        minuteWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatMinute((Integer) value);
            }
        });
        secondWheelView.setFormatter(new WheelFormatter() {
            @Override
            public String formatItem(@NonNull Object value) {
                return timeFormatter.formatSecond((Integer) value);
            }
        });
    }

    public void setTimeLabel(CharSequence hour, CharSequence minute, CharSequence second) {
        hourLabelView.setText(hour);
        minuteLabelView.setText(minute);
        secondLabelView.setText(second);
    }

    public void setOnTimeSelectedListener(OnTimeSelectedListener onTimeSelectedListener) {
        this.onTimeSelectedListener = onTimeSelectedListener;
    }

    public void setOnTimeMeridiemSelectedListener(OnTimeMeridiemSelectedListener onTimeMeridiemSelectedListener) {
        this.onTimeMeridiemSelectedListener = onTimeMeridiemSelectedListener;
    }

    public final TimeEntity getStartValue() {
        return startValue;
    }

    public final TimeEntity getEndValue() {
        return endValue;
    }

    public final NumberWheelView getHourWheelView() {
        return hourWheelView;
    }

    public final NumberWheelView getMinuteWheelView() {
        return minuteWheelView;
    }

    public final NumberWheelView getSecondWheelView() {
        return secondWheelView;
    }

    public final TextView getHourLabelView() {
        return hourLabelView;
    }

    public final TextView getMinuteLabelView() {
        return minuteLabelView;
    }

    public final TextView getSecondLabelView() {
        return secondLabelView;
    }

    public final WheelView getMeridiemWheelView() {
        return meridiemWheelView;
    }

    @Deprecated
    public final TextView getMeridiemLabelView() {
        throw new UnsupportedOperationException("Use getMeridiemWheelView instead");
    }

    public final int getSelectedHour() {
        int hour = (int) hourWheelView.getCurrentItem();
        return wrapHour(hour);
    }

    private int wrapHour(int hour) {
        if (isHour12Mode() && hour > 12) {
            hour = hour - 12;
        }
        return hour;
    }

    public final int getSelectedMinute() {
        return (int) minuteWheelView.getCurrentItem();
    }

    public final int getSelectedSecond() {
        if (timeMode == TimeMode.HOUR_12_NO_SECOND
                || timeMode == TimeMode.HOUR_24_NO_SECOND) {
            return 0;
        }
        return (int) secondWheelView.getCurrentItem();
    }

//    public final boolean isAnteMeridiem() {
//        if (meridiemWheelView.getCurrentItem() != null){
//            return meridiemWheelView.getCurrentItem().toString().equalsIgnoreCase("AM");
//        }
//        return false;
//    }

    public final String isAnteMeridiem() {
        if (meridiemWheelView.getCurrentItem() != null) {
            return meridiemWheelView.getCurrentItem().toString().equalsIgnoreCase("AM") + "";
        }
        return "null";
    }

    private void changeHour() {
        int min = Math.min(startValue.getHour(), endValue.getHour());
        int max = Math.max(startValue.getHour(), endValue.getHour());
        int minHour = isHour12Mode() ? 1 : 0;
        int maxHour = isHour12Mode() ? 12 : 23;
        min = Math.max(minHour, min);
        max = Math.min(maxHour, max);
        if (selectedHour == null) {
            selectedHour = min;
        }
        hourWheelView.setRange(min, max, 1);
        hourWheelView.setDefaultValue(selectedHour);
        changeMinute(selectedHour);
    }

    private void changeMinute(int hour) {
        final int min, max;
        //开始时及结束时相同情况
        if (hour == startValue.getHour() && hour == endValue.getHour()) {
            min = startValue.getMinute();
            max = endValue.getMinute();
        }
        //开始时相同情况
        else if (hour == startValue.getHour()) {
            min = startValue.getMinute();
            max = 59;
        }
        //结束时相同情况
        else if (hour == endValue.getHour()) {
            min = 0;
            max = endValue.getMinute();
        } else {
            min = 0;
            max = 59;
        }
        if (selectedMinute == null) {
            selectedMinute = min;
        }
        minuteWheelView.setRange(min, max, 1);
        minuteWheelView.setDefaultValue(selectedMinute);
        changeSecond();
    }

    private void changeSecond() {
        if (selectedSecond == null) {
            selectedSecond = 0;
        }
        secondWheelView.setRange(0, 59, 1);
        secondWheelView.setDefaultValue(selectedSecond);
    }

    private void changeAnteMeridiem() {
        meridiemWheelView.setDefaultValue(isAnteMeridiem ? "AM" : "PM");
    }

}
