package com.jwei.publicone.ui.healthy.ecg

import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.ActivityEcgCalibrationQuestionBinding

class EcgCalibrationQuestionActivity : BaseActivity<ActivityEcgCalibrationQuestionBinding, BaseViewModel>(
    ActivityEcgCalibrationQuestionBinding::inflate,
    BaseViewModel::class.java
) {

    override fun setTitleId(): Int = binding.title.layoutTitle.id
}