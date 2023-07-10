package com.jwei.publicone.ui.healthy.ecg

import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.ActivityEcgCalibrationBinding
import com.jwei.publicone.view.wheelview.OptionPicker

class EcgCalibrationActivity : BaseActivity<ActivityEcgCalibrationBinding, BaseViewModel>(ActivityEcgCalibrationBinding::inflate, BaseViewModel::class.java),
    View.OnClickListener {

    override fun setTitleId(): Int = binding.title.layoutTitle.id

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnConfirm.id -> {

            }
            binding.tvBloodPressureLevel.id -> {
                createRangDialog(binding.tvBloodPressureLevel.text.toString().trim())
            }
            binding.title.ivRightIcon.id -> {
                startActivity(Intent(this, EcgCalibrationQuestionActivity::class.java))
            }
        }
    }

    override fun initView() {
        super.initView()

        binding.title.tvTitle.text = getString(R.string.healthy_ecg_title_right_tx)
        binding.title.layoutRight.visibility = View.VISIBLE
        binding.title.ivRightIcon.visibility = View.VISIBLE
        binding.title.ivRightIcon.setImageResource(R.mipmap.ic_common_problem)

        setViewsClickListener(this, binding.btnConfirm, binding.tvBloodPressureLevel, binding.title.ivRightIcon)
    }

    private fun createRangDialog(default: String) {
        val data = listOf<String>(*resources.getStringArray(R.array.bloodPressureLevel).clone())
        var defaultPosition = data.indexOfFirst { it == default }
        if (defaultPosition == -1) {
            defaultPosition = 2
        }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            binding.tvBloodPressureLevel.text = item.toString()
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }
}