package com.jwei.publicone.utils;

import android.view.View;

/**
 * FileName: CustomClickListener
 * Copyright (C), 2019-2020, Shenzhen Hongbao Technology Co., Ltd. All Rights Reserved.
 * Description: 自定义防止重复点击
 * Author：daitao
 * Date: 2020/5/28 13:57
 * History:
 * <author> <time> <version> <desc>
 */
public abstract class CustomClickListener implements View.OnClickListener {
    private long mLastClickTime;
    private long timeInterval = 500L;

    public CustomClickListener() {

    }

    public CustomClickListener(long interval) {
        this.timeInterval = interval;
    }

    @Override
    public void onClick(View v) {
        long nowTime = System.currentTimeMillis();
        if (Math.abs(nowTime - mLastClickTime) > timeInterval) {
            // 单次点击事件
            onSingleClick();
            mLastClickTime = nowTime;
        } else {
            // 快速点击事件
            onFastClick();
        }
    }

    public abstract void onSingleClick();

    public void onFastClick() {
    }
}