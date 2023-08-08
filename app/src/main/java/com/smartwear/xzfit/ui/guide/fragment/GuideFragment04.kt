package com.smartwear.xzfit.ui.guide.fragment

import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseFragment
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.FragmentGuide04Binding
import com.smartwear.xzfit.utils.GlideApp

class GuideFragment04 : BaseFragment<FragmentGuide04Binding, BaseViewModel>(
    FragmentGuide04Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide04).into(binding.ivGuide)
    }
}