package com.smartwear.xzfit.ui.region;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.impl
 * class_name : EmojiFiltuer
 * description : 过滤emoji表情
 * time : 2021-11-01 17:24
 */
public class EmojiFilter implements InputFilter {
    Pattern emoji = Pattern.compile("[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]",
            Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        Matcher emojiMatcher = emoji.matcher(source);
        if (emojiMatcher.find()) {
            return "";
        }
        for (int index = start; index < end; index++) {
            int type = Character.getType(source.charAt(index));
            if (type == Character.SURROGATE) {
                return "";
            }
        }
        return null;
    }
}
