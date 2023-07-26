package com.jwei.xzfit.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import com.blankj.utilcode.util.LogUtils
import java.nio.charset.StandardCharsets

/**
 * Created by Android on 2021/12/23.
 */
object EditTextUtils {
    private var isArtificialCorrect = false

    /**
     * 限制输入框输入特殊字符、表情
     * */
    fun evitTextLimit(ed: AppCompatEditText) {
        ed.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                //isUnCommit = true
                //binding.btnSave.isEnabled = isDone()
                if (s != null) {
                    if (!isArtificialCorrect) {
                        isArtificialCorrect = true
                        var str = StringBuilder()
                        //限制特殊字符、表情
                        for (r in s) {
                            val type = Character.getType(r)
                            LogUtils.d("type -- $type")
                            //1 2 5 9 24
                            if (type < Character.SURROGATE.toInt()) {
                                str.append(r)
                            }
                        }
                        ed.setText(str)
                        ed.setSelection(str.length)
                    }
                    isArtificialCorrect = false
                }
            }
        })
    }

    private fun getMaxByteSizeStr(str: StringBuilder, maxByteSize: Int): StringBuilder {
        var repair = StringBuilder(str)
        val byteSize = repair.toString().toByteArray(StandardCharsets.UTF_8).size
        LogUtils.d("已输入字节：$byteSize")
        if (byteSize > maxByteSize) {
            repair = StringBuilder(repair.substring(0, str.length - 1))
            return getMaxByteSizeStr(repair, maxByteSize)
        }
        return repair
    }
}