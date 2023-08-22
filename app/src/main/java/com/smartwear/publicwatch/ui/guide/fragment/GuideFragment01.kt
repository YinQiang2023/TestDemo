package com.smartwear.publicwatch.ui.guide.fragment

import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.FragmentGuide01Binding
import com.smartwear.publicwatch.utils.GlideApp

class GuideFragment01 : BaseFragment<FragmentGuide01Binding, BaseViewModel>(
    FragmentGuide01Binding::inflate,
    BaseViewModel::class.java
) {

    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide01).into(binding.ivGuide)
    }
}