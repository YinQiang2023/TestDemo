package com.smartwear.xzfit.https.download

import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.https.BaseRetrofitClient
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.ErrorUtils
import com.smartwear.xzfit.utils.HttpLog
import com.smartwear.xzfit.utils.SpUtils
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Android on 2022/5/27.
 * 下载Retrofit Client
 */
internal object DownloadRetrofitClient {
    //国内
    private val mBaseURL: String by lazy { AppUtils.getMetaDataByKey("com.smartwear.xzfit.serverUrl")!! }

    //国外
    private val mBaseEnURL: String by lazy { AppUtils.getMetaDataByKey("com.smartwear.xzfit.foreignServerUrl")!! }

    val service: DownloadApi
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
                .create(DownloadApi::class.java)
        }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(Interceptor { chain ->
                val request: Request = chain.request()
                val response = chain.proceed(request)
                if (response.code != 200) {
                    //下载异常code记录
                    ErrorUtils.onLogResult("Download url:${request.url} CODE : ${response.code}")
                    LogUtils.i("Download url:${request.url} CODE : ${response.code}")

                    var log = StringBuffer("")
                        .append("url : ${request.url} ")
                        .append("code : ${response.code} \n")
                    HttpLog.log(log.toString())
                }
                return@Interceptor response.newBuilder()
                    .body(ProgressResponseBody(chain.request().url.toUrl().toString(), response.body, DownloadManager.networkDownloadProgressListener))
                    .build()
            })
            .connectTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .callTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(BaseRetrofitClient.TIME_OUT.toLong(), TimeUnit.SECONDS)
            .build()
    }
}