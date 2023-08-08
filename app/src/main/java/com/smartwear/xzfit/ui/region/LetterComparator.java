package com.smartwear.xzfit.ui.region;

import android.text.TextUtils;

import java.util.Comparator;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.impl
 * class_name : PinyinComparator
 * description : 首字母比较器
 * time : 2021-12-09 18:28
 */
public class LetterComparator implements Comparator<SortModel> {

    @Override
    public int compare(SortModel model1, SortModel model2) {
        if (model1 == null || model2 == null) {
            return 0;
        }
        if (TextUtils.equals(model1.letters, "@") || TextUtils.equals(model2.letters, "#")) {
            return -1;
        } else if (TextUtils.equals(model1.letters,"#") || TextUtils.equals(model1.letters,"@")) {
            return 1;
        } else {
            return model1.letters.compareTo(model2.letters);
        }
    }
}
