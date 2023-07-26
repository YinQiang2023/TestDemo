package com.jwei.xzfit.ui.healthy.ecg

import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.ActivityEcgCalibrationQuestionBinding

class EcgCalibrationQuestionActivity : BaseActivity<ActivityEcgCalibrationQuestionBinding, BaseViewModel>(
    ActivityEcgCalibrationQuestionBinding::inflate,
    BaseViewModel::class.java
) {

    override fun setTitleId(): Int = binding.title.layoutTitle.id
}