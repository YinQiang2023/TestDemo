package com.jwei.xzfit.ui.healthy.history.womenhealth

import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.ActivityWomenCycleExplainBinding

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