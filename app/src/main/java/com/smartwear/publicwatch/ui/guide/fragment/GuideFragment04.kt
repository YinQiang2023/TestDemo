package com.smartwear.publicwatch.ui.guide.fragment

import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.FragmentGuide04Binding
import com.smartwear.publicwatch.utils.GlideApp

class GuideFragment04 : BaseFragment<FragmentGuide04Binding, BaseViewModel>(
    FragmentGuide04Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide04).into(binding.ivGuide)
    }
}