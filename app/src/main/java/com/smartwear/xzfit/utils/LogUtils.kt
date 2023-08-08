package com.smartwear.xzfit.utils

import android.util.Log

object LogUtils {
    var isWriteLog = true
        set(value) {
            SaveLog.setIsWriteLog(value)
            field = value
        }

    @JvmStatic
    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        SaveLog.writeFile(tag, msg)
    }

    @JvmStatic
    fun i(tag: String, msg: String, isFeedback: Boolean) {
        Log.i(tag, msg)
        SaveLog.writeFile(tag, msg, isFeedback)
    }

    @JvmStatic
    fun v(tag: String, msg: String) {
        Log.v(tag, msg)
        SaveLog.writeFile(tag, msg)
    }

    @JvmStatic
    fun v(tag: String, msg: String, isFeedback: Boolean) {
        Log.v(tag, msg)
        SaveLog.writeFile(tag, msg, isFeedback)
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        Log.e(tag, msg)
        SaveLog.writeFile(tag, msg)
    }

    @JvmStatic
    fun e(tag: String, msg: String, isFeedback: Boolean) {
        Log.e(tag, msg)
        SaveLog.writeFile(tag, msg, isFeedback)
    }

    @JvmStatic
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        SaveLog.writeFile(tag, msg)
    }

    @JvmStatic
    fun d(tag: String, msg: String, isFeedback: Boolean) {
        Log.d(tag, msg)
        SaveLog.writeFile(tag, msg, isFeedback)
    }

    @JvmStatic
    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        SaveLog.writeFile(tag, msg)
    }

    @JvmStatic
    fun w(tag: String, msg: String, isFeedback: Boolean) {
        Log.w(tag, msg)
        SaveLog.writeFile(tag, msg, isFeedback)
    }
}