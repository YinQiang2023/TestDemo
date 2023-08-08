package com.smartwear.xzfit.view.wheelview;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.smartwear.xzfit.view.wheelview.basepicker.ConfirmPicker;
import com.smartwear.xzfit.view.wheelview.contract.OnOptionPickedListener;
import com.smartwear.xzfit.view.wheelview.widget.MetricSystemLayout;
import com.smartwear.xzfit.view.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

public class MetricSystemPicker extends ConfirmPicker {
    protected MetricSystemLayout wheelLayout;
    private OnOptionPickedListener onOptionPickedListener;
    private OnOptionPickedListener onOptionPickedListenerIn;
    private boolean initialized = false;
    private List<?> data;
    private List<?> dataIn;
    private Object defaultValue;
    private Object defaultValueIn;
    private int defaultPosition = -1;
    private int defaultPositionIn = -1;

    public MetricSystemPicker(@NonNull Activity activity) {
        super(activity);
    }

    public MetricSystemPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new MetricSystemLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onOptionPickedListener != null) {
            int position = wheelLayout.getWheelView().getCurrentPosition();
            Object item = wheelLayout.getWheelView().getCurrentItem();
            onOptionPickedListener.onOptionPicked(position, item);
        }
        if (onOptionPickedListenerIn != null) {
            int position = wheelLayout.getWheelInView().getCurrentPosition();
            Object item = wheelLayout.getWheelInView().getCurrentItem();
            onOptionPickedListenerIn.onOptionPicked(position, item);
        }
    }

    @Override
    protected void initData() {
        super.initData();
        initialized = true;
        if (data == null || data.size() == 0) {
            data = provideData();
        }
        wheelLayout.setData(data);

        if (dataIn == null || dataIn.size() == 0) {
            dataIn = provideDataIn();
        }

        wheelLayout.setDataIn(dataIn);

        if (defaultValue != null) {
            wheelLayout.setDefaultValue(defaultValue);
        }
        if (defaultPosition != -1) {
            wheelLayout.setDefaultPosition(defaultPosition);
        }

        if (defaultValueIn != null) {
            wheelLayout.setInDefaultValue(defaultValueIn);
        }
        if (defaultPositionIn != -1) {
            wheelLayout.setInDefaultPosition(defaultPositionIn);
        }
    }

    protected List<?> provideData() {
        return null;
    }

    protected List<?> provideDataIn() {
        return null;
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public void setData(Object... data) {
        setData(Arrays.asList(data));
    }

    public void setData(List<?> data) {
        this.data = data;
        if (initialized) {
            wheelLayout.setData(data);
        }
    }

    public void setDefaultValue(Object item) {
        this.defaultValue = item;
        if (initialized) {
            wheelLayout.setDefaultValue(item);
        }
    }

    public void setDefaultPosition(int position) {
        this.defaultPosition = position;
        if (initialized) {
            wheelLayout.setDefaultPosition(position);
        }
    }

    public void setDataIn(Object... data) {
        setDataIn(Arrays.asList(data));
    }

    public void setDataIn(List<?> data) {
        this.dataIn = data;
        if (initialized) {
            wheelLayout.setDataIn(data);
        }
    }

    public void setDefaultValueIn(Object item) {
        this.defaultValueIn = item;
        if (initialized) {
            wheelLayout.setInDefaultValue(item);
        }
    }

    public void setDefaultPositionIn(int position) {
        this.defaultPositionIn = position;
        if (initialized) {
            wheelLayout.setInDefaultPosition(position);
        }
    }

    public void setOnOptionPickedListener(OnOptionPickedListener onOptionPickedListener) {
        this.onOptionPickedListener = onOptionPickedListener;
    }

    public void setOnOptionPickedInListener(OnOptionPickedListener onOptionPickedListener) {
        this.onOptionPickedListenerIn = onOptionPickedListener;
    }

    public final MetricSystemLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getWheelView() {
        return wheelLayout.getWheelView();
    }

    public final TextView getRightLabel() {
        return wheelLayout.getRightLabel();
    }

    public final WheelView getWheelViewIn() {
        return wheelLayout.getWheelInView();
    }

    public final TextView getRightLabelIn() {
        return wheelLayout.getRightLabelIn();
    }

    public void setShowModel(int type) {
        wheelLayout.setShowModel(type);
    }
}
