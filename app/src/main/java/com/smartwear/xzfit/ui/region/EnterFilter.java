package com.smartwear.xzfit.ui.region;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.impl
 * class_name : EnterFilter
 * description : 过滤换行键
 * time : 2022-06-24 11:07
 */
public class EnterFilter implements InputFilter {

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (source.toString().contains("\n")) {
            return source.toString().replaceAll("\n","");
        }
        return source;
    }
}
