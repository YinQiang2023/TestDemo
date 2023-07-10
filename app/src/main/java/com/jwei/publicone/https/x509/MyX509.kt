package com.jwei.publicone.https.x509

import android.os.Build
import androidx.annotation.RequiresApi
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager

/**
 * Created by android
 * on 2021/7/15
 */
@RequiresApi(Build.VERSION_CODES.N)
class MyX509 : X509ExtendedTrustManager() {
    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?, authType: String?, socket: Socket?
    ) {
    }

    override fun checkClientTrusted(
        chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?
    ) {
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?, authType: String?, socket: Socket?
    ) {
    }

    override fun checkServerTrusted(
        chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?
    ) {
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }

    fun checkServerTrusted(chain: Array<out X509Certificate>?, authType1: String?, authTyp2: String?, authType3: String?) {
    }
}