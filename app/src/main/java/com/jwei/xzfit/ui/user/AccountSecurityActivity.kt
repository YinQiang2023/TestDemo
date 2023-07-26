package com.jwei.xzfit.ui.user

import android.content.Intent
import android.view.View
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.AccountSecurityActivityBinding

/**
 * Created by Android on 2022/2/14.
 */
class AccountSecurityActivity : BaseActivity<AccountSecurityActivityBinding, BaseViewModel>(
    AccountSecurityActivityBinding::inflate, BaseViewModel::class.java
), View.OnClickListener {

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.accounts_ecurity)

        setViewsClickListener(this, binding.llWriteOff)
    }

    override fun onClick(v: View?) {
        v?.let { v ->
            when (v.id) {
                binding.llWriteOff.id -> {
                    startActivity(Intent(this, WriteOffActivity::class.java))
                }
            }
        }
    }
}