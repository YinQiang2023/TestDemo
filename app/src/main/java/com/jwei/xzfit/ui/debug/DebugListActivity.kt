package com.jwei.xzfit.ui.debug

import android.content.Intent
import android.view.View
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityDebugListBinding
import com.jwei.xzfit.ui.debug.dialog.*
import com.jwei.xzfit.viewmodel.DeviceModel

class DebugListActivity : BaseActivity<ActivityDebugListBinding, DeviceModel>(
    ActivityDebugListBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("Debug")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.tvDeviceInfo.id -> {
               DeviceInfoDialog(this).show()
            }
            binding.tvNotice.id -> {
                startActivity(Intent(this, DebugNoticeActivity::class.java))
            }
            binding.tvWeather.id -> {
                startActivity(Intent(this, DebugWeatherActivity::class.java))
            }
            binding.tvLanguage.id -> {
                LanguageDialog(this,this,lifecycle,viewModel).show()
            }
            binding.tvEnvironment.id -> {
                EnvironmentDialog(this).show()
            }
            binding.tvDialTransmission.id -> {
                DialDialog(this,this,viewModel).show()
            }
        }
    }

}