package com.jwei.xzfit.ui.user

import android.app.Activity
import android.content.Intent
import android.text.TextUtils
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.ActivityStravaLoginBinding
import com.jwei.xzfit.utils.manager.StravaManager

/**
 * Created by Android on 2022/5/16.
 */
class StravaWebLoginActivity : BaseActivity<ActivityStravaLoginBinding, BaseViewModel>(
    ActivityStravaLoginBinding::inflate, BaseViewModel::class.java
) {

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.strava))

        //scheme 协议进入
        //infowear://wearheart?code=f1497199e858bcdbaf6b3da6e5c0c1552e7f2a06&scope=activity%3Awrite%2Cread%2Cread_all
        //infowear://wearheart?error=access_denied
        LogUtils.d("scheme: $intent")
        if (intent != null) {
            val schemeUri = intent.data
            if (schemeUri != null) {
                if (schemeUri.toString().startsWith(StravaManager.INFOWEAR_SCHEME_URL)) {
                    val code = schemeUri.getQueryParameter("code")
                    val resultIntent = Intent()
                    if (!TextUtils.isEmpty(code)) {
                        //正常授权 code==f1497199e858bcdbaf6b3da6e5c0c1552e7f2a06 无网络/取消 code==null
                        resultIntent.putExtra("code", code)
                        StravaManager.resultRequestPermissions(StravaManager.ACTIVITY_WEB_RESULE_REQUEST_CODE, Activity.RESULT_OK, resultIntent)
                    } else {
                        StravaManager.resultRequestPermissions(StravaManager.ACTIVITY_WEB_RESULE_REQUEST_CODE, Activity.RESULT_CANCELED, resultIntent)
                    }
                    finish()
                    return
                }
            }
        }

        //startActivityForResult 进入
        binding.web.loadUrl(StravaManager.getWebLoginUrl())
    }

    override fun initData() {
        super.initData()
        binding.web.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request != null) {
                    LogUtils.d("Strava login code = " + request.url.getQueryParameter("code"))
                    val code = request.url.getQueryParameter("code")
                    val intent = Intent()
                    if (!TextUtils.isEmpty(code)) {
                        //正常授权 code==43e9eba9035ece446cabe99d6794eccb51db6341 无网络/取消 code==null
                        intent.putExtra("code", code)
                        setResult(Activity.RESULT_OK, intent)
                    } else {
                        setResult(Activity.RESULT_CANCELED)
                    }
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.web.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.web.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.web.destroy()
    }
}