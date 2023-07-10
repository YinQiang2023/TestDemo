package com.jwei.publicone.utils

import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils

object ProhibitEmojiUtils {

    /**
     * 过滤表情
     */
    /*fun inputFilterProhibitEmoji(maxLength:Int = Int.MAX_VALUE): InputFilter {
        return InputFilter { source, start, end, dest, dstart, dend ->
            val buffer = StringBuffer()
            //if (maxLength - (dest.length - (dend - dstart)) > 0) {
                var i = start
                while (i < end) {
                    val codePoint = source[i]
                    val type = Character.getType(source[i])
                    LogUtils.d("type ---->$type")
                    if (type != Character.SURROGATE.toInt() && type != Character.OTHER_SYMBOL.toInt()) {
                        buffer.append(codePoint)
                    } else {
                        i++
                        i++
                        continue
                    }
                    i++
                }
            //}

            if (source is Spanned) {
                val sp = SpannableString(buffer)
                TextUtils.copySpansFrom(
                    source, start, end, null,
                    sp, 0
                )
                sp
            } else {
                buffer
            }
        }
    }*/

    fun inputFilterProhibitEmoji(maxLength: Int = Int.MAX_VALUE): Array<InputFilter> {
        return arrayOf(InputFilter.LengthFilter(maxLength),
            InputFilter { source, start, end, dest, dstart, dend ->
                val buffer = StringBuffer()
                //if (maxLength - (dest.length - (dend - dstart)) > 0) {
                var i = start
                while (i < end) {
                    val codePoint = source[i]
                    val type = Character.getType(source[i])
                    LogUtils.d("type ---->$type")
                    if (type != Character.SURROGATE.toInt() && type != Character.OTHER_SYMBOL.toInt()) {
                        buffer.append(codePoint)
                    } else {
                        i++
                        i++
                        continue
                    }
                    i++
                }
                //}
                if (source is Spanned) {
                    val sp = SpannableString(buffer)
                    try {
                        TextUtils.copySpansFrom(
                            source, start, end, null,
                            sp, 0
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    sp
                } else {
                    buffer
                }
            }
        )
    }

    fun getLength(s: String): Int {
        var length = 0
        for (i in s.indices) {
            val ascii = Character.codePointAt(s, i)
            if (ascii in 0..255) {
                length++
            } else {
                length += 2
            }
        }
        return length
    }

    private fun getIsEmoji(codePoint: Char): Boolean {
        return !(codePoint.toInt() == 0x0 || codePoint.toInt() == 0x9 || codePoint.toInt() == 0xA
                || codePoint.toInt() == 0xD
                || codePoint.toInt() in 0x20..0xD7FF
                || codePoint.toInt() in 0xE000..0xFFFD
                || codePoint.toInt() in 0x10000..0x10FFFF)
    }

    fun inputFilterProhibitChinese(maxLength: Int = Int.MAX_VALUE): Array<InputFilter> {
        return arrayOf(InputFilter.LengthFilter(maxLength),
            InputFilter { source, start, end, dest, dstart, dend ->
                val buffer = StringBuffer()
                try {
                    var i = start
                    while (i < end) {
                        val codePoint = source[i]
                        val type = Character.getType(source[i])
                        LogUtils.d("type ---->$type")
                        if (type != Character.SURROGATE.toInt() && type != Character.OTHER_SYMBOL.toInt()) {
                            if (codePoint.toInt() < 0X4e00 || codePoint.toInt() > 0x9fff) {
                                buffer.append(codePoint)
                            }
                        } else {
                            i++
                            i++
                            continue
                        }
                        i++
                    }
                    if (source is Spanned) {
                        val sp = SpannableString(buffer)
                        TextUtils.copySpansFrom(source, start, end, null, sp, 0)
                        return@InputFilter sp
                    } else {
                        return@InputFilter buffer
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@InputFilter buffer
                }
            }
        )
    }
}