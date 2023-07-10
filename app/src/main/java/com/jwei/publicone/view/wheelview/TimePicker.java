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

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.jwei.publicone.view.wheelview.basepicker.ConfirmPicker;
import com.jwei.publicone.view.wheelview.contract.OnTimeMeridiemPickedListener;
import com.jwei.publicone.view.wheelview.contract.OnTimePickedListener;
import com.jwei.publicone.view.wheelview.widget.TimeWheelLayout;


/**
 * 时间选择器
 */
@SuppressWarnings("unused")
public class TimePicker extends ConfirmPicker {
    protected TimeWheelLayout wheelLayout;
    private OnTimePickedListener onTimePickedListener;
    private OnTimeMeridiemPickedListener onTimeMeridiemPickedListener;

    public TimePicker(@NonNull Activity activity) {
        super(activity);
    }

    public TimePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new TimeWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void initView(@NonNull View contentView) {
        super.initView(contentView);
    }

    @Deprecated
    protected int provideTimeMode() {
        throw new UnsupportedOperationException("Use `picker.getWheelLayout().setTimeMode()` instead");
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        int hour = wheelLayout.getSelectedHour();
        int minute = wheelLayout.getSelectedMinute();
        int second = wheelLayout.getSelectedSecond();
        if (onTimePickedListener != null) {
            onTimePickedListener.onTimePicked(hour, minute, second);
        }
        if (onTimeMeridiemPickedListener != null) {
            onTimeMeridiemPickedListener.onTimePicked(hour, minute, second, wheelLayout.isAnteMeridiem());
        }
    }

    public void setOnTimePickedListener(OnTimePickedListener onTimePickedListener) {
        this.onTimePickedListener = onTimePickedListener;
    }

    public void setOnTimeMeridiemPickedListener(OnTimeMeridiemPickedListener onTimeMeridiemPickedListener) {
        this.onTimeMeridiemPickedListener = onTimeMeridiemPickedListener;
    }

    public final TimeWheelLayout getWheelLayout() {
        return wheelLayout;
    }
}
