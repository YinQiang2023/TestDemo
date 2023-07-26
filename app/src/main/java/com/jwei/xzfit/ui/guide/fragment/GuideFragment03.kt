package com.jwei.xzfit.ui.guide.fragment

import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.FragmentGuide03Binding
import com.jwei.xzfit.utils.GlideApp

class GuideFragment03 : BaseFragment<FragmentGuide03Binding, BaseViewModel>(
    FragmentGuide03Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide03).into(binding.ivGuide)
    }
}