package com.smartwear.xzfit.ui.guide.fragment

import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseFragment
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.FragmentGuide03Binding
import com.smartwear.xzfit.utils.GlideApp

class GuideFragment03 : BaseFragment<FragmentGuide03Binding, BaseViewModel>(
    FragmentGuide03Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide03).into(binding.ivGuide)
    }
}