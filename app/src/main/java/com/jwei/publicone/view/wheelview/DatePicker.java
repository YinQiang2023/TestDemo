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
import com.jwei.publicone.view.wheelview.contract.OnDatePickedListener;
import com.jwei.publicone.view.wheelview.widget.DateWheelLayout;


/**
 * 日期选择器
 */
@SuppressWarnings("unused")
public class DatePicker extends ConfirmPicker {
    protected DateWheelLayout wheelLayout;
    private OnDatePickedListener onDatePickedListener;

    public DatePicker(@NonNull Activity activity) {
        super(activity);
    }

    public DatePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new DateWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void initView(@NonNull View contentView) {
        super.initView(contentView);
    }

    @Deprecated
    protected int provideDateMode() {
        throw new UnsupportedOperationException("Use `picker.getWheelLayout().setDateMode()` instead");
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected void onOk() {
        if (onDatePickedListener != null) {
            int year = wheelLayout.getSelectedYear();
            int month = wheelLayout.getSelectedMonth();
            int day = wheelLayout.getSelectedDay();
            onDatePickedListener.onDatePicked(year, month, day);
        }
    }

    public void setOnDatePickedListener(OnDatePickedListener onDatePickedListener) {
        this.onDatePickedListener = onDatePickedListener;
    }

    public final DateWheelLayout getWheelLayout() {
        return wheelLayout;
    }

}
