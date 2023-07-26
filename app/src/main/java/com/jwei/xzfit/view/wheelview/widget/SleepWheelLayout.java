package com.jwei.xzfit.view.wheelview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.jwei.xzfit.R;
import com.jwei.xzfit.view.wheelview.annotation.ItemTextAlign;
import com.jwei.xzfit.view.wheelview.contract.SleepCallback;

import java.util.Arrays;
import java.util.List;

public class SleepWheelLayout extends BaseWheelLayout {
    private WheelView wheelViewHour;
    private WheelView wheelViewMin;
    private SleepCallback sleepCallback;
    private String selectedHour;
    private String selectedMinute;

    public SleepWheelLayout(Context context) {
        super(context);
    }

    public SleepWheelLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SleepWheelLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SleepWheelLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_sleep_picker;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.OptionWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
        return Arrays.asList(wheelViewHour, wheelViewMin);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        wheelViewHour = findViewById(R.id.wheelHour);
        wheelViewMin = findViewById(R.id.wheelMin);
    }

    @Override
    protected void onAttributeSet(@NonNull Context context, @NonNull TypedArray typedArray) {
        float density = context.getResources().getDisplayMetrics().density;
        setTextSize(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_itemTextSize,
                (int) (15 * context.getResources().getDisplayMetrics().scaledDensity)));
        setVisibleItemCount(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_visibleItemCount, 5));
        setSameWidthEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_sameWidthEnabled, false));
        setMaxWidthText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_maxWidthText));
        setSelectedTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColorSelected, 0xFF000000));
        setTextColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_itemTextColor, 0xFF888888));
        setItemSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_itemSpace, (int) (20 * density)));
        setCyclicEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_cyclicEnabled, false));
        setIndicatorEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_indicatorEnabled, false));
        setIndicatorColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_indicatorColor, 0xFFEE3333));
        setIndicatorSize(typedArray.getDimension(R.styleable.OptionWheelLayout_wheel_indicatorSize, 1 * density));
        setCurvedIndicatorSpace(typedArray.getDimensionPixelSize(R.styleable.OptionWheelLayout_wheel_curvedIndicatorSpace, (int) (1 * density)));
        setCurtainEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curtainEnabled, false));
        setCurtainColor(typedArray.getColor(R.styleable.OptionWheelLayout_wheel_curtainColor, 0x88FFFFFF));
        setAtmosphericEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_atmosphericEnabled, false));
        setCurvedEnabled(typedArray.getBoolean(R.styleable.OptionWheelLayout_wheel_curvedEnabled, false));
        setCurvedMaxAngle(typedArray.getInteger(R.styleable.OptionWheelLayout_wheel_curvedMaxAngle, 90));
        setTextAlign(typedArray.getInt(R.styleable.OptionWheelLayout_wheel_itemTextAlign, ItemTextAlign.CENTER));
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        if (sleepCallback == null) return;
//        switch (view.getId()){
//            case R.id.wheelHour:{
//
//            }return;
//            case R.id.wheelMin:{
//
//            }return;
//        }
//            sleepCallback.onOptionSelected(position, wheelView.getItem(position));
    }


    public void setData(List<String> dataHour, List<String> dataMin) {
        wheelViewHour.setData(dataHour);
        wheelViewMin.setData(dataMin);
    }

    public void setDefaultValue(String dataHour, String dataMin) {
        wheelViewHour.setDefaultValue(dataHour);
        wheelViewMin.setDefaultValue(dataMin);
    }

    public void setDefaultPosition(int positionHour, int positionMin) {
        wheelViewHour.setDefaultValue(positionHour);
        wheelViewMin.setDefaultValue(positionMin);
    }

    public void setOnSleepCallbackListener(SleepCallback sleepCallback) {
        this.sleepCallback = sleepCallback;
    }

    public final WheelView getHourWheelView() {
        return wheelViewHour;
    }

    public final WheelView getMinWheelView() {
        return wheelViewMin;
    }

}
