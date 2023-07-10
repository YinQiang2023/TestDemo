package com.jwei.publicone.view.wheelview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jwei.publicone.R;
import com.jwei.publicone.view.wheelview.annotation.ItemTextAlign;
import com.jwei.publicone.view.wheelview.contract.OnOptionSelectedListener;

import java.util.Arrays;
import java.util.List;

public class MetricSystemLayout extends BaseWheelLayout {
    private WheelView wheelView;
    private WheelView wheelViewIn;
    private TextView rightLabel;
    private TextView rightLabelIn;
    private OnOptionSelectedListener onOptionSelectedListener;
    private OnOptionSelectedListener onOptionSelectedListenerIn;

    public MetricSystemLayout(Context context) {
        super(context);
    }

    public MetricSystemLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MetricSystemLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MetricSystemLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int provideLayoutRes() {
        return R.layout.wheel_metric_system_option;
    }

    @Override
    protected int[] provideStyleableRes() {
        return R.styleable.OptionWheelLayout;
    }

    @Override
    protected List<WheelView> provideWheelViews() {
//        return Collections.singletonList(wheelView);
        return Arrays.asList(wheelView, wheelViewIn);
    }

    @Override
    protected void onInit(@NonNull Context context) {
        wheelView = findViewById(R.id.wheel_picker_option_wheel);
        rightLabel = findViewById(R.id.wheel_picker_option_label);
        wheelViewIn = findViewById(R.id.wheel_picker_option_wheel_in);
        rightLabelIn = findViewById(R.id.wheel_picker_option_label_in);
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
        rightLabel.setText(typedArray.getString(R.styleable.OptionWheelLayout_wheel_label));
    }

    @Override
    public void onWheelSelected(WheelView view, int position) {
        int id = view.getId();
        if (id == R.id.wheel_picker_option_wheel) {
            if (onOptionSelectedListener != null) {
                onOptionSelectedListener.onOptionSelected(position, wheelView.getItem(position));
            }
            return;
        }

        if (id == R.id.wheel_picker_option_wheel_in) {
            if (onOptionSelectedListenerIn != null) {
                onOptionSelectedListenerIn.onOptionSelected(position, wheelViewIn.getItem(position));
            }
            return;
        }
    }

    public void setData(List<?> data) {
        wheelView.setData(data);
    }

    public void setDataIn(List<?> data) {
        wheelViewIn.setData(data);
    }

    public void setDefaultValue(Object value) {
        wheelView.setDefaultValue(value);
    }

    public void setInDefaultValue(Object value) {
        wheelViewIn.setDefaultValue(value);
    }

    public void setDefaultPosition(int position) {
        wheelView.setDefaultPosition(position);
    }

    public void setInDefaultPosition(int position) {
        wheelViewIn.setDefaultPosition(position);
    }

    public void setShowModel(int type) {
        if (type == 0) {
            wheelView.setVisibility(View.VISIBLE);
            rightLabel.setVisibility(View.VISIBLE);
            wheelViewIn.setVisibility(View.GONE);
            rightLabelIn.setVisibility(View.GONE);
        } else {
            wheelView.setVisibility(View.VISIBLE);
            rightLabel.setVisibility(View.VISIBLE);
            wheelViewIn.setVisibility(View.VISIBLE);
            rightLabelIn.setVisibility(View.VISIBLE);
        }
    }

    public void setOnOptionSelectedListener(OnOptionSelectedListener onOptionSelectedListener) {
        this.onOptionSelectedListener = onOptionSelectedListener;
    }

    public void setOnOptionSelectedListenerIn(OnOptionSelectedListener onOptionSelectedListener) {
        this.onOptionSelectedListenerIn = onOptionSelectedListener;
    }

    public final WheelView getWheelView() {
        return wheelView;
    }

    public final WheelView getWheelInView() {
        return wheelViewIn;
    }

    public final TextView getRightLabel() {
        return rightLabel;
    }

    public final TextView getRightLabelIn() {
        return rightLabelIn;
    }
}
