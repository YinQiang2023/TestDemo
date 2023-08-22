package com.smartwear.publicwatch.https

import com.smartwear.publicwatch.BuildConfig
import com.smartwear.publicwatch.https.converterfactory.MyGsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Created by android
 * on 2021/7/14
 */
abstract class BaseRetrofitClient {
    companion object {
        internal const val TIME_OUT = 0/*15*/
    }

    val client: OkHttpClient
        get() {
            val builder = OkHttpClient.Builder()
            val logging = HttpLoggingInterceptor()
            if (BuildConfig.DEBUG) {
                logging.level = HttpLoggingInterceptor.Level.BODY
            } else {
                logging.level = HttpLoggingInterceptor.Level.BASIC
            }

            builder.addInterceptor(logging)
                .connectTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
                .callTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
                .readTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT.toLong(), TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

            handleBuilder(builder)
            return builder.build()
        }

    protected abstract fun handleBuilder(builder: OkHttpClient.Builder)

    open fun <S> getService(clazz: Class<S>, baseUrl: String?): S {
        return Retrofit.Builder()
            .client(client)
            .addConverterFactory(MyGsonConverterFactory.create())
            .baseUrl(baseUrl.toString())
            .build()
            .create(clazz)
    }

}