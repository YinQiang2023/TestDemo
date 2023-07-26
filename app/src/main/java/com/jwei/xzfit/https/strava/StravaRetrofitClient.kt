package com.jwei.xzfit.https.strava

import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ThreadUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.https.BaseRetrofitClient
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.StravaManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.Proxy
import javax.net.ssl.*

/**
 * Created by Android on 2022/5/17.
 */
object StravaRetrofitClient : BaseRetrofitClient() {

    val service by lazy {
        Retrofit.Builder()
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(AppUtils.getMetaDataByKey("com.jwei.publicone.stravaUrl")!!)
            .build()
            .create(StravaApiService::class.java)
    }

    override fun handleBuilder(builder: OkHttpClient.Builder) {
        builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
        builder.addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request: Request = chain.request().newBuilder().apply {
                    addHeader("content-type", "application/json")
                    addHeader("cache-control", "no-cache")
                    //token
                    if (StravaManager.getTokenResponse() != null) {
                        addHeader("Authorization", StravaManager.getTokenResponse()!!.token_type + " " + StravaManager.getTokenResponse()!!.access_token)
                    }
                }.build()

                //请求前判断网络是否可用
                if (!NetworkUtils.isAvailable()) {
                    ThreadUtils.runOnUiThread {
                        ToastUtils.showToast(BaseApplication.mContext.getString(R.string.not_network_tips))
                    }
                }
                val response = chain.proceed(request)
                return response
            }
        })
        builder.proxy(Proxy.NO_PROXY)
    }
}