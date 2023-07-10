package com.jwei.publicone.ui.guide.fragment

import com.jwei.publicone.R
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.FragmentGuide04Binding
import com.jwei.publicone.utils.GlideApp

class GuideFragment04 : BaseFragment<FragmentGuide04Binding, BaseViewModel>(
    FragmentGuide04Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide04).into(binding.ivGuide)
    }
}