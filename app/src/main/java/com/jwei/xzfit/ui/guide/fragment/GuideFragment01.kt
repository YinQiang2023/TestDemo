package com.jwei.xzfit.ui.guide.fragment

import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.FragmentGuide01Binding
import com.jwei.xzfit.utils.GlideApp

class GuideFragment01 : BaseFragment<FragmentGuide01Binding, BaseViewModel>(
    FragmentGuide01Binding::inflate,
    BaseViewModel::class.java
) {

    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide01).into(binding.ivGuide)
    }
}