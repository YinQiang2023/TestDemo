package com.jwei.xzfit.https

import android.os.Build
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ThreadUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseApplication.Companion.mContext
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.ui.HomeActivity
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.CommonAttributes
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.net.Proxy
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.*

/**
 * Created by android
 * on 2021/7/14
 */
object ErrorLogRetrofitClient : BaseRetrofitClient() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun handleBuilder(builder: OkHttpClient.Builder) {
        builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val x509 = MyX509()
//            builder.sslSocketFactory(getSSLFactory(x509), x509)
//        } else{
//            builder.sslSocketFactory(HTTPSTrustManager.getSslSocketFactory(), HTTPSTrustManager())
//        }

//        setCer(getAssetsInputStream("zh.cer"))
//        sslSocketFactory?.let { trustManager?.let { it1 -> builder.sslSocketFactory(it, it1) } }
        builder.addInterceptor(object : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request: Request = chain.request().newBuilder()
                    .addHeader("content-type", "application/json")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("User-Agent", AppUtils.getUserAgent().toString())
                    .addHeader("Authorization", SpUtils.getValue(SpUtils.AUTHORIZATION, ""))
                    .addHeader("appId", CommonAttributes.APP_ID)
                    .addHeader("av", AppUtils.getAppVersionName())
                    .addHeader("ut", System.currentTimeMillis().toString())
                    .build()
                //请求前判断网络是否可用
                if (!NetworkUtils.isAvailable()) {
                    ThreadUtils.runOnUiThread {
                        ToastUtils.showToast(mContext.getString(R.string.not_network_tips))
                    }
                }
                if (SpUtils.getValue(SpUtils.AUTHORIZATION, "").isEmpty() &&
                    !request.url.toString().contains("infowear/language/deviceLanguageList") &&
                    !request.url.toString().contains("infowear/product/list")
                ) {
                    ThreadUtils.runOnUiThread {
                        ActivityUtils.getActivityList().forEach {
                            if (it is HomeActivity) {
                                BaseViewModel().userLoginOut(HttpCommonAttributes.LOGIN_OUT)
                            }
                        }
                    }
                }
                val response = chain.proceed(request)
                return response
            }
        })
        builder.proxy(Proxy.NO_PROXY)
    }

    val service by lazy {
        getService(ErrorLogService::class.java, AppUtils.getMetaDataByKey("com.jwei.xzfit.serverUrl"))
    }


    var trustManager: X509TrustManager? = null
    var sslSocketFactory: SSLSocketFactory? = null

    fun setCer(cerIn: InputStream?) {
        //读取自签名证书
        try {
            //通过trustManagerForCertificates方法为证书生成 TrustManager
            trustManager = trustManagerForCertificates(cerIn)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager?>(trustManager), null)
            sslSocketFactory = sslContext.socketFactory
        } catch (e: GeneralSecurityException) {
            throw RuntimeException(e)
        }
    }

    @Throws(GeneralSecurityException::class)
    fun trustManagerForCertificates(`in`: InputStream?): X509TrustManager? {
        //InputStream 可以包含多个证书
        //CertificateFactory 用于生成 Certificate，也就是数字证书
        val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
        //由输入流生成证书
        val certificates: Collection<Certificate?> = certificateFactory.generateCertificates(`in`)
        if (certificates.isEmpty()) {
            throw IllegalArgumentException("expected non-empty set of trusted certificates")
        }

        // 将证书放入 keyStore
        val password = "password".toCharArray() // "password"可以任意设置
        val keyStore: KeyStore = newEmptyKeyStore(password)!!
        var index = 0
        for (certificate: Certificate? in certificates) {
            val certificateAlias = Integer.toString(index++)
            keyStore.setCertificateEntry(certificateAlias, certificate)
        }

        // 用　KeyStore 生成 X509 trust manager.
        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        )
        keyManagerFactory.init(keyStore, password)
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(keyStore)
        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size != 1 || trustManagers.get(0) !is X509TrustManager) {
            throw IllegalStateException(
                "Unexpected default trust managers:" + Arrays.toString(trustManagers)
            )
        }
        return trustManagers[0] as X509TrustManager
    }

    @Throws(GeneralSecurityException::class)
    private fun newEmptyKeyStore(password: CharArray): KeyStore? {
        return try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            val `in`: InputStream? = null
            // 传入 'null' 会生成一个空的 Keytore
            //password 用于检查 KeyStore 完整性和 KeyStore 解锁
            keyStore.load(`in`, password)
            keyStore
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }

    fun getAssetsInputStream(name: String?): InputStream? {
        try {
            return mContext.assets.open(name!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}