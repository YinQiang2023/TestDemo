package com.smartwear.publicwatch.ui.login.privacy

import android.webkit.WebSettings
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.UseAgreementActivityBinding
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.Constant
import com.smartwear.publicwatch.utils.LocalWebViewClient
import com.smartwear.publicwatch.viewmodel.UserModel

class UseAgreementActivity : BaseActivity<UseAgreementActivityBinding, UserModel>(UseAgreementActivityBinding::inflate, UserModel::class.java) {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.user_agreement))
        (getString(R.string.main_app_name) + getString(R.string.user_agreement)).also { binding.tvTittle.text = it }

        val settings: WebSettings = binding.wbView.settings
        settings.javaScriptEnabled = true
        settings.textZoom = 100
        binding.wbView.webViewClient = LocalWebViewClient(this)
        if (AppUtils.isZh(this)) {
            binding.wbView.loadUrl(Constant.USER_AGREEMENT_URL_ZH)
        } else {
            binding.wbView.loadUrl(Constant.USER_AGREEMENT_URL_EN)
        }

    }
}