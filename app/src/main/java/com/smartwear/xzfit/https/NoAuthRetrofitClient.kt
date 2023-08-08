package com.smartwear.xzfit.https

import com.smartwear.xzfit.https.converterfactory.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

object NoAuthRetrofitClient : BaseRetrofitClient() {

    override fun handleBuilder(builder: OkHttpClient.Builder) {}

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override fun <S> getService(clazz: Class<S>, baseUrl: String?): S {
        return Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl.toString())
            .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(clazz)
    }
}