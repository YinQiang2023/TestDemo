package com.smartwear.publicwatch.ui.region;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

/**
 * author : ym
 * package_name : com.transsion.oraimohealth.utils
 * class_name : TextWatcherImpl
 * description : TextWatcher实例
 * time : 2021-10-26 10:24
 */
public class TextWatcherImpl implements TextWatcher {

    private static final int MSG_TEXT_CHANGED = 0x99;

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        mHandler.removeMessages(MSG_TEXT_CHANGED);
        Message message = mHandler.obtainMessage(MSG_TEXT_CHANGED, s.toString().trim());
        mHandler.sendMessageDelayed(message, 100);
    }

    /**
     * 处理文本变化，避免用户快速输入导致ANR
     */
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void dispatchMessage(@NonNull Message msg) {
            if (msg.what != MSG_TEXT_CHANGED) {
                return;
            }
            afterTextChanged(msg.obj.toString());
        }
    };

    /**
     * 在该方法处理文本变化逻辑，避免ANR
     *
     * @param text
     */
    public void afterTextChanged(String text) {

    }
}
