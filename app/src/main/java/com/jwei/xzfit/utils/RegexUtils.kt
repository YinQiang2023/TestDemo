package com.jwei.xzfit.utils

import android.text.TextUtils
import java.util.regex.Pattern

object RegexUtils {

    /**
     * 验证手机格式
     */
    fun isMobileNO(mobiles: String): Boolean {
        /*
         * 移动：134、135、136、137、138、139、150、151、157(TD)、158、159、187、188
         * 联通：130、131、132、152、155、156、185、186 电信：133、153、180、189、（1349卫通）
         * 总结起来就是第一位必定为1，第二位必定为3或5或8，其他位置的可以为0-9
         */
        val telRegex = "[1][0123456789]\\d{9}" // "[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        return if (TextUtils.isEmpty(mobiles)) false else Pattern.matches(telRegex, mobiles)
    }


    fun passwordLimit(psw: String): Boolean {
        if (TextUtils.isEmpty(psw) || psw.length < 6 || psw.length > 20) return false
        return inputFilterStr(psw)
    }

    fun isEmail(mail: String): Boolean {
        return Pattern.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", mail)
    }

    /**
     * 限制editText输入的密码格式
     */
    fun inputFilterStr(input: String): Boolean {
        if (TextUtils.isEmpty(input)) return false
        val blockCharacterSet: String = "0123456789qwertzuiopasdfghjklyxcvbnm€'–{}\"[]¥·|<>..\\)()\$…%;^&﹉+=/#_!?*:,-~@.￥"
        for (index in input.indices) {
            if (!blockCharacterSet.contains(input[index], true))
                return false
        }
        return true
    }


}