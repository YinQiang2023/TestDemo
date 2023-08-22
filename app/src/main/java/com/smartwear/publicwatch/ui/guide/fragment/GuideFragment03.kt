package com.smartwear.publicwatch.ui.guide.fragment

import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.FragmentGuide03Binding
import com.smartwear.publicwatch.utils.GlideApp

class GuideFragment03 : BaseFragment<FragmentGuide03Binding, BaseViewModel>(
    FragmentGuide03Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide03).into(binding.ivGuide)
    }
}