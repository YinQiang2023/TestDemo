package com.jwei.publicone.ui.login.privacy

import android.webkit.WebSettings
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.UseAgreementActivityBinding
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.Constant
import com.jwei.publicone.utils.LocalWebViewClient
import com.jwei.publicone.viewmodel.UserModel

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