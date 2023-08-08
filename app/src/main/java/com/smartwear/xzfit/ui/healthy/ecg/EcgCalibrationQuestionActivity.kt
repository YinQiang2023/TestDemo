package com.smartwear.xzfit.ui.healthy.ecg

import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.ActivityEcgCalibrationQuestionBinding

class EcgCalibrationQuestionActivity : BaseActivity<ActivityEcgCalibrationQuestionBinding, BaseViewModel>(
    ActivityEcgCalibrationQuestionBinding::inflate,
    BaseViewModel::class.java
) {

    override fun setTitleId(): Int = binding.title.layoutTitle.id
}