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

package com.smartwear.xzfit.view.wheelview;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.smartwear.xzfit.view.wheelview.contract.LinkageProvider;
import com.smartwear.xzfit.view.wheelview.contract.OnCarNumberPickedListener;
import com.smartwear.xzfit.view.wheelview.contract.OnLinkagePickedListener;
import com.smartwear.xzfit.view.wheelview.widget.CarNumberWheelLayout;


/**
 * 中国大陆车牌号滚轮选择
 */
@SuppressWarnings({"unused"})
public class CarNumberPicker extends LinkagePicker {
    private OnCarNumberPickedListener onCarNumberPickedListener;

    public CarNumberPicker(@NonNull Activity activity) {
        super(activity);
    }

    public CarNumberPicker(@NonNull Activity activity, @StyleRes int themeResId) {
        super(activity, themeResId);
    }

    @Deprecated
    @Override
    public void setData(@NonNull LinkageProvider data) {
        throw new UnsupportedOperationException("Data already preset");
    }

    @Deprecated
    @Override
    public void setOnLinkagePickedListener(OnLinkagePickedListener onLinkagePickedListener) {
        throw new UnsupportedOperationException("Use setOnCarNumberPickedListener instead");
    }

    @NonNull
    @Override
    protected View createBodyView(@NonNull Activity activity) {
        wheelLayout = new CarNumberWheelLayout(activity);
        return wheelLayout;
    }

    @Override
    protected void onOk() {
        if (onCarNumberPickedListener != null) {
            String province = wheelLayout.getFirstWheelView().getCurrentItem().toString();
            String letter = wheelLayout.getSecondWheelView().getCurrentItem().toString();
            onCarNumberPickedListener.onCarNumberPicked(province, letter);
        }
    }


    public void setOnCarNumberPickedListener(OnCarNumberPickedListener onCarNumberPickedListener) {
        this.onCarNumberPickedListener = onCarNumberPickedListener;
    }

}
