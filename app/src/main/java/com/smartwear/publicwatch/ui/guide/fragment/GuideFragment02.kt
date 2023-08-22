package com.smartwear.publicwatch.ui.guide.fragment

import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.FragmentGuide02Binding
import com.smartwear.publicwatch.utils.GlideApp

class GuideFragment02 : BaseFragment<FragmentGuide02Binding, BaseViewModel>(
    FragmentGuide02Binding::inflate,
    BaseViewModel::class.java
) {
    override fun initView() {
        super.initView()
        GlideApp.with(this).load(R.mipmap.guide02).into(binding.ivGuide)
    }
}