package com.jwei.xzfit.ui.guide.fragment

import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.FragmentGuide04Binding
import com.jwei.xzfit.utils.GlideApp

class GuideFragment04 : BaseFragment<FragmentGuide04Binding, BaseViewModel>(
    FragmentGuide04Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide04).into(binding.ivGuide)
    }
}