package com.smartwear.xzfit.ui.healthy.history.womenhealth

import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.ActivityWomenCycleExplainBinding

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