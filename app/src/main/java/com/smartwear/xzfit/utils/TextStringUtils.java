package com.smartwear.xzfit.utils;


public class TextStringUtils {

    public static boolean isNull(String str) {
        return str == null || str.equals("") || str.length() <= 0 || str.toLowerCase().equals("null");
    }


}
