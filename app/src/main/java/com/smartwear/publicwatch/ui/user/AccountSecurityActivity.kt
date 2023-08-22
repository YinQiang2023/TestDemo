package com.smartwear.publicwatch.ui.user

import android.content.Intent
import android.view.View
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.AccountSecurityActivityBinding

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