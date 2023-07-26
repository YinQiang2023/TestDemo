package com.jwei.xzfit.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Created by Android on 2022/11/18.
 */
class LocalWebViewClient(var context: Context) : WebViewClient() {

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        val uri = Uri.parse(url)
        return handleUri(view, uri)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        return handleUri(view, uri)
    }

    fun handleUri(view: WebView?, uri: Uri): Boolean {
        val scheme = uri.scheme ?: ""
        return if (scheme.startsWith("http") || scheme.startsWith("https")) {
            //WebView 处理
            view!!.loadUrl(uri.toString())
            false
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = uri
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //其它处理
            true
        }
    }

}