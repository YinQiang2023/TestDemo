package com.smartwear.xzfit.ui.guide.fragment

import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseFragment
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.FragmentGuide02Binding
import com.smartwear.xzfit.utils.GlideApp

class GuideFragment02 : BaseFragment<FragmentGuide02Binding, BaseViewModel>(
    FragmentGuide02Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide02).into(binding.ivGuide)
    }
}