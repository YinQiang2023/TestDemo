package com.jwei.xzfit.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jwei.xzfit.BuildConfig
import com.jwei.xzfit.https.RequestBody
import org.json.JSONObject


/**
 * Created by android
 * on 2021/8/6
 */
object JsonUtils {
    const val serviceKey = "wo.szzhkjyxgs.20"
    fun <T> getRequestJson(anyBody: Any, clazz: (Class<T>)): RequestBody {
        val gson = GsonBuilder().create()
        val postObject = JSONObject()
        val data = gson.toJson(anyBody, clazz)
        LogUtils.e(clazz.simpleName, "request data = $data")
        if (BuildConfig.DEBUG) com.blankj.utilcode.util.LogUtils.d("HTTP", data)
        postObject.put("data", AESUtils.encrypt(data, serviceKey).replace("\n", ""))
        return gson.fromJson(postObject.toString(), RequestBody::class.java)
    }

    fun <T> getRequestJson(TAG: String, anyBody: Any, clazz: (Class<T>)): RequestBody {
        val gson = GsonBuilder().create()
        val postObject = JSONObject()
        val data = gson.toJson(anyBody, clazz)
//        Log.i(TAG, "request data = $data")
        LogUtils.e(clazz.simpleName, "$TAG:request data = $data")
        if (BuildConfig.DEBUG) com.blankj.utilcode.util.LogUtils.d("HTTP", data)
        postObject.put("data", AESUtils.encrypt(data, serviceKey).replace("\n", ""))
        return gson.fromJson(postObject.toString(), RequestBody::class.java)
    }

    fun <T> getRequestJson(gson : Gson, TAG: String, anyBody: Any, clazz: (Class<T>)): RequestBody {
        val postObject = JSONObject()
        val data = gson.toJson(anyBody, clazz)
        LogUtils.e(clazz.simpleName, "$TAG:request data = $data")
        if (BuildConfig.DEBUG) com.blankj.utilcode.util.LogUtils.d("HTTP", data)
        postObject.put("data", AESUtils.encrypt(data, serviceKey).replace("\n", ""))
        return gson.fromJson(postObject.toString(), RequestBody::class.java)
    }
}