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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.jwei.publicone.view.wheelview.DatePicker;
import com.jwei.publicone.view.wheelview.annotation.DateMode;
import com.jwei.publicone.view.wheelview.entity.DateEntity;
import com.jwei.publicone.view.wheelview.impl.BirthdayFormatter;

import java.util.Calendar;

/**
 * 出生日期选择器
 */
@SuppressWarnings("unused")
public class FeedbackDatePicker extends DatePicker {

    public FeedbackDatePicker(@NonNull Activity activity) {
        super(activity);
    }

    public FeedbackDatePicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Override
    protected void initData() {
        super.initData();
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        DateEntity startValue = DateEntity.target(currentYear - 1, 1, 1);
        DateEntity endValue = DateEntity.target(currentYear, currentMonth, currentDay);
        wheelLayout.setRange(startValue, endValue);
        wheelLayout.setDateMode(DateMode.YEAR_MONTH_DAY);
        wheelLayout.setDateFormatter(new BirthdayFormatter());
        wheelLayout.setDefaultValue(DateEntity.target(currentYear, currentMonth, currentDay));
    }

    public void setDefaultValue(int year, int month, int day) {
        wheelLayout.setDefaultValue(DateEntity.target(year, month, day));
    }

}
