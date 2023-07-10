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

import com.jwei.publicone.R;
import com.jwei.publicone.view.wheelview.OptionPicker;

import java.util.Arrays;
import java.util.List;

/**
 * 性别选择器
 */
@SuppressWarnings("WeakerAccess")
public class MapTypePicker extends OptionPicker {

    public MapTypePicker(Activity activity) {
        super(activity);
    }

    @Override
    protected List<?> provideData() {
        List<String> data = Arrays.asList(mWrActivity.get().getResources().getStringArray(R.array.mapTypeTextArrays));
        return data;
    }

}
