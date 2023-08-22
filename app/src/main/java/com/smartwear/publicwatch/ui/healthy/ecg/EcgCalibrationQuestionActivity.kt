package com.smartwear.publicwatch.ui.healthy.ecg

import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.ActivityEcgCalibrationQuestionBinding

class EcgCalibrationQuestionActivity : BaseActivity<ActivityEcgCalibrationQuestionBinding, BaseViewModel>(
    ActivityEcgCalibrationQuestionBinding::inflate,
    BaseViewModel::class.java
) {

    override fun setTitleId(): Int = binding.title.layoutTitle.id
}