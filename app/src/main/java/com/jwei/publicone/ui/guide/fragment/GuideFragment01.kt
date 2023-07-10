package com.jwei.publicone.ui.guide.fragment

import com.jwei.publicone.R
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.FragmentGuide01Binding
import com.jwei.publicone.utils.GlideApp

class GuideFragment01 : BaseFragment<FragmentGuide01Binding, BaseViewModel>(
    FragmentGuide01Binding::inflate,
    BaseViewModel::class.java
) {

    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide01).into(binding.ivGuide)
    }
}