package com.smartwear.xzfit.ui.guide.fragment

import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseFragment
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.FragmentGuide01Binding
import com.smartwear.xzfit.utils.GlideApp

class GuideFragment01 : BaseFragment<FragmentGuide01Binding, BaseViewModel>(
    FragmentGuide01Binding::inflate,
    BaseViewModel::class.java
) {

    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide01).into(binding.ivGuide)
    }
}