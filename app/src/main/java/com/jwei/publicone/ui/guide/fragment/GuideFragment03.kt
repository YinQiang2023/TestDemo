package com.jwei.publicone.ui.guide.fragment

import com.jwei.publicone.R
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.FragmentGuide03Binding
import com.jwei.publicone.utils.GlideApp

class GuideFragment03 : BaseFragment<FragmentGuide03Binding, BaseViewModel>(
    FragmentGuide03Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide03).into(binding.ivGuide)
    }
}