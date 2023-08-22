package com.smartwear.publicwatch.ui.login.privacy

import android.annotation.SuppressLint
import android.webkit.WebSettings
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.PrivacyPolicyActivityBinding
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.Constant
import com.smartwear.publicwatch.utils.LocalWebViewClient
import com.smartwear.publicwatch.viewmodel.UserModel

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