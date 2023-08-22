package com.smartwear.publicwatch.ui.healthy.history.womenhealth

import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.ActivityWomenCycleExplainBinding

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