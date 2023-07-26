package com.jwei.xzfit.view.wheelview;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;


import com.jwei.xzfit.view.wheelview.basepicker.ConfirmPicker;
import com.jwei.xzfit.view.wheelview.contract.OnSleepPickedListener;
import com.jwei.xzfit.view.wheelview.widget.SleepWheelLayout;
import com.jwei.xzfit.view.wheelview.widget.WheelView;

import java.util.List;

public class SleepPicker extends ConfirmPicker {
    protected SleepWheelLayout wheelLayout;
    private OnSleepPickedListener onSleepPickedListener;
    private boolean initialized = false;
    private List<String> dataHour, dataMin;
    private String defaultHourValue, defaultMinValue;
    private int defaultHourPosition = -1;
    private int defaultMinPosition = -1;

    public SleepPicker(@NonNull Activity activity) {
        super(activity);
    }

    public SleepPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new SleepWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onSleepPickedListener != null) {
            int position = wheelLayout.getHourWheelView().getCurrentPosition();
            String item = wheelLayout.getHourWheelView().getCurrentItem() + ":" + wheelLayout.getMinWheelView().getCurrentItem();
            onSleepPickedListener.onOptionPicked(item);
        }
    }

    @Override
    protected void initData() {
        super.initData();
        initialized = true;
        if (dataHour == null || dataHour.size() == 0) {
            dataHour = provideData();
        }
        if (dataMin == null || dataMin.size() == 0) {
            dataMin = provideData();
        }
        wheelLayout.setData(dataHour, dataMin);
        if (defaultHourValue != null && defaultMinValue != null) {
            wheelLayout.setDefaultValue(defaultHourValue, defaultMinValue);
        }
        if (defaultHourPosition != -1 && defaultMinPosition != -1) {
            wheelLayout.setDefaultPosition(defaultHourPosition, defaultMinPosition);
        }
    }

    protected List<String> provideData() {
        return null;
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public void setData(List<String> dataHour, List<String> dataMin) {
        this.dataHour = dataHour;
        this.dataMin = dataMin;
//        if (initialized) {
        wheelLayout.setData(dataHour, dataMin);
//        }
    }

    public void setDefaultValue(String itemHour, String itemMin) {
        this.defaultHourValue = itemHour;
        this.defaultMinValue = itemMin;
//        if (initialized) {
        wheelLayout.setDefaultValue(itemHour, itemMin);
//        }
    }

    public void setDefaultPosition(int positionHour, int positionMin) {
        this.defaultHourPosition = positionHour;
        this.defaultMinPosition = positionMin;
//        if (initialized) {
        wheelLayout.setDefaultPosition(positionHour, positionMin);
//        }
    }

    public void setOnOptionPickedListener(OnSleepPickedListener onSleepPickedListener) {
        this.onSleepPickedListener = onSleepPickedListener;
    }

    public final SleepWheelLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getHourWheelView() {
        return wheelLayout.getHourWheelView();
    }

    public final WheelView getMinWheelView() {
        return wheelLayout.getMinWheelView();
    }

}
