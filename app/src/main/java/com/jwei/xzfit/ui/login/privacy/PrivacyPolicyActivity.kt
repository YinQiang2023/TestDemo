package com.jwei.xzfit.ui.login.privacy

import android.annotation.SuppressLint
import android.webkit.WebSettings
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.PrivacyPolicyActivityBinding
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.Constant
import com.jwei.xzfit.utils.LocalWebViewClient
import com.jwei.xzfit.viewmodel.UserModel

class PrivacyPolicyActivity : BaseActivity<PrivacyPolicyActivityBinding, UserModel>(PrivacyPolicyActivityBinding::inflate, UserModel::class.java) {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.privacy_policy))
        /*binding.wbView.settings.javaScriptEnabled = true
        binding.wbView.settings.defaultTextEncodingName = "utf-8"
        binding.wbView.setBackgroundColor(Color.parseColor("#040910")) // 设置背景色
        binding.wbView.visibility = View.VISIBLE // 加载完之后进行设置显示，以免加载时初始化效果不好看*/
        (getString(R.string.main_app_name) + getString(R.string.privacy_policy)).also { binding.tvTittle.text = it }

        val settings: WebSettings = binding.wbView.settings
        settings.javaScriptEnabled = true
        settings.textZoom = 100
        binding.wbView.webViewClient = LocalWebViewClient(this)
        if (AppUtils.isZh(this)) {
            binding.wbView.loadUrl(Constant.PRIVACY_POLICY_URL_ZH)
        } else {
            binding.wbView.loadUrl(Constant.PRIVACY_POLICY_URL_EN)
        }
    }
}