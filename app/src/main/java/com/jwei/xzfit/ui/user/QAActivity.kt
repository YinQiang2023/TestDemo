package com.jwei.xzfit.ui.user

import android.view.KeyEvent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.PrivacyPolicyActivityBinding
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.Constant
import com.jwei.xzfit.utils.LocalWebViewClient
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.viewmodel.UserModel

class QAActivity : BaseActivity<PrivacyPolicyActivityBinding, UserModel>(PrivacyPolicyActivityBinding::inflate, UserModel::class.java) {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.common_question_help)
        binding.tvTittle.visibility = View.GONE

        val settings: WebSettings = binding.wbView.settings
        settings.javaScriptEnabled = true
        settings.textZoom = 100
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN;
        binding.wbView.webViewClient = LocalWebViewClient(this)
        if (AppUtils.isZh(this)) {
            binding.wbView.loadUrl(Constant.FAQ_URL_ZH)
        } else {
            binding.wbView.loadUrl(Constant.FAQ_URL_EN)
        }
        //binding.wbView.loadUrl("http://test.wearheart.cn/faq/index.html")

        binding.wbView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                url?.let { view.loadUrl(it) }
                return true
            }
        }

        tvTitle?.setOnClickListener {
            if (binding.wbView.canGoBack()) {
                // 返回上一页面
                binding.wbView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
                binding.wbView.goBack()
            } else {
                finish()
            }
        }
    }

    override fun initData() {
        super.initData()
        //查看帮助行为
        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("2", "3"))
    }


    /* 改写物理按键返回的逻辑 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.wbView.canGoBack()) {
            // 返回上一页面
            binding.wbView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            binding.wbView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}