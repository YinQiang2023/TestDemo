package com.smartwear.publicwatch.https

/**
 * Created by android
 * on 2021/7/14
 */
data class Response<T>(
    val result: String, val msg: String, val code: String, val codeMsg: String, val data: T
)