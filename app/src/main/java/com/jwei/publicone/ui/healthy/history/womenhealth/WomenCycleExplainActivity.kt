package com.jwei.publicone.ui.healthy.history.womenhealth

import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.ActivityWomenCycleExplainBinding

class WomenCycleExplainActivity : BaseActivity<ActivityWomenCycleExplainBinding, BaseViewModel>(
    ActivityWomenCycleExplainBinding::inflate,
    BaseViewModel::class.java
) {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
    }
}