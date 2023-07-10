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

package com.jwei.publicone.view.wheelview;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;


import com.jwei.publicone.view.wheelview.basepicker.ConfirmPicker;
import com.jwei.publicone.view.wheelview.contract.OnOptionPickedListener;
import com.jwei.publicone.view.wheelview.widget.OptionWheelLayout;
import com.jwei.publicone.view.wheelview.widget.WheelView;

import java.util.Arrays;
import java.util.List;

/**
 * 单项选择器
 */
@SuppressWarnings({"unused"})
public class OptionPicker extends ConfirmPicker {
    protected OptionWheelLayout wheelLayout;
    private OnOptionPickedListener onOptionPickedListener;
    private boolean initialized = false;
    private List<?> data;
    private Object defaultValue;
    private int defaultPosition = -1;

    public OptionPicker(@NonNull Activity activity) {
        super(activity);
    }

    public OptionPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new OptionWheelLayout(activity);
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
    }

    @Override
    protected void initData() {
        super.initData();
        initialized = true;
        if (data == null || data.size() == 0) {
            data = provideData();
        }
        wheelLayout.setData(data);
        if (defaultValue != null) {
            wheelLayout.setDefaultValue(defaultValue);
        }
        if (defaultPosition != -1) {
            wheelLayout.setDefaultPosition(defaultPosition);
        }
    }

    protected List<?> provideData() {
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

    public void setOnOptionPickedListener(OnOptionPickedListener onOptionPickedListener) {
        this.onOptionPickedListener = onOptionPickedListener;
    }

    public final OptionWheelLayout getWheelLayout() {
        return wheelLayout;
    }

    public final WheelView getWheelView() {
        return wheelLayout.getWheelView();
    }

    public final TextView getRightLabel() {
        return wheelLayout.getRightLabel();
    }

}
