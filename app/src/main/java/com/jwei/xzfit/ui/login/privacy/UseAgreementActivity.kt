package com.jwei.xzfit.ui.login.privacy

import android.webkit.WebSettings
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.UseAgreementActivityBinding
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.Constant
import com.jwei.xzfit.utils.LocalWebViewClient
import com.jwei.xzfit.viewmodel.UserModel

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