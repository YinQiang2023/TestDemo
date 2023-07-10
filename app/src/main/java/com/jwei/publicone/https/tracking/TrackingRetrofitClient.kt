package com.jwei.publicone.https.tracking

import androidx.viewbinding.BuildConfig
import com.blankj.utilcode.util.LogUtils
import com.jwei.publicone.https.BaseRetrofitClient
import com.jwei.publicone.utils.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Android on 2023/4/10.
 * 异常日志埋点Retrofit Client
 */
object TrackingRetrofitClient {
    //国内
    private val mBaseURL: String by lazy { AppUtils.getMetaDataByKey("com.jwei.publicone.trackingServerUrl")!! }

    //国外
    private val mBaseEnURL: String by lazy { AppUtils.getMetaDataByKey("com.jwei.publicone.trackingForeignServerUrl")!! }

    val service: TrackingApi
        get() {
            return Retrofit.Builder()
                .client(createOkHttpClient())
                .baseUrl(
                    if (SpUtils.getValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_DEFAULT) == SpUtils.SERVICE_ADDRESS_TO_TYPE1
                        || SpUtils.getValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_DEFAULT) == SpUtils.SERVICE_ADDRESS_DEFAULT
                    ) {
                        mBaseURL
                    } else {
                        mBaseEnURL
                    }
                )
                .addConverterFactory(GsonConverterFactory.create())
                .callbackExecutor(Executors.newSingleThreadExecutor())
                .build()
                .create(TrackingApi::class.java)
        }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor(Interceptor { chain ->

                val request: Request = chain.request().newBuilder()
                    .addHeader("appId", CommonAttributes.APP_ID)
                    .addHeader("secretKey", "10d16a6f-d5dc-11ed-aedc-0c42a1f03fc6")
                    .addHeader("content-type", "application/json")
                    .addHeader("av", AppUtils.getAppVersionName())
                    .addHeader("ut", System.currentTimeMillis().toString())
                    .addHeader("userId", SpUtils.getValue(SpUtils.USER_ID, ""))
                    .addHeader("appType", "2")  //1、iOS 2、Android 0、其它
                    .addHeader("phoneModel", AppUtils.getPhoneType())
                    .addHeader("osVersion", AppUtils.getOsVersion())
                    .addHeader("cache-control", "no-cache")
                    .build()
                val response = chain.proceed(request)
                if (response.code != 200) {
                    //下载异常code记录
                    ErrorUtils.onLogResult("Tracking url:${request.url} CODE : ${response.code}")
                    LogUtils.i("Tracking url:${request.url} CODE : ${response.code}")

                    var log = StringBuffer("")
                        .append("url : ${request.url} ")
                        .append("code : ${response.code} \n")
                    HttpLog.log(log.toString())
                }
                return@Interceptor response
            })
            .connectTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .callTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}