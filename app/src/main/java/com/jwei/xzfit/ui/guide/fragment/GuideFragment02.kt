package com.jwei.xzfit.ui.guide.fragment

import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.FragmentGuide02Binding
import com.jwei.xzfit.utils.GlideApp

class GuideFragment02 : BaseFragment<FragmentGuide02Binding, BaseViewModel>(
    FragmentGuide02Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide02).into(binding.ivGuide)
    }
}