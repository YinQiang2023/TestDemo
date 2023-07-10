package com.jwei.publicone.ui.guide.fragment

import com.jwei.publicone.R
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.FragmentGuide02Binding
import com.jwei.publicone.utils.GlideApp

class GuideFragment02 : BaseFragment<FragmentGuide02Binding, BaseViewModel>(
    FragmentGuide02Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide02).into(binding.ivGuide)
    }
}