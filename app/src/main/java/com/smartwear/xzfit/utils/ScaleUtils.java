package com.smartwear.xzfit.utils;

import android.content.res.Resources;

import com.smartwear.xzfit.base.BaseApplication;

/**
 * FileName: ScaleUtils
 * Copyright (C), 2019-2020, Shenzhen Hongbao Technology Co., Ltd. All Rights Reserved.
 * Description: 屏幕尺寸工具
 * Author：dai
 * Date: 2020/3/26 10:10
 * History:
 * <author> <time> <version> <desc>
 */
public class ScaleUtils {

    public static int dip2px(float dipValue) {
        final float scale = BaseApplication.mContext.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static int px2dip(float pxValue) {
        final float scale = BaseApplication.mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static int px2sp(float f) {
        return Math.round((f / Resources.getSystem().getDisplayMetrics().scaledDensity) + 0.5f);
    }

    public static int sp2px(float f) {
        return Math.round((Resources.getSystem().getDisplayMetrics().scaledDensity * f) + 0.5f);
    }

    public static int getPercentWidth1px() {
        return px2dip(1.0f);
    }


}
