package com.jwei.publicone.ui.device.devicecontrol

import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityNotEnabledBinding
import com.jwei.publicone.https.response.BindListResponse
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.utils.GlideApp
import com.jwei.publicone.viewmodel.DeviceModel

class NotEnabledActivity : BaseActivity<ActivityNotEnabledBinding, DeviceModel>(
    ActivityNotEnabledBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private lateinit var deviceInfo: BindListResponse.DeviceItem
    private var dialog: Dialog? = null
    private var oldDevice = ""
    private val IS_ENABLE: Int = 0x01
    private var isEnable = false

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.lyStartDevice.id -> {
                startActivityForResult(
                    Intent(this@NotEnabledActivity, EnableDeviceActivity::class.java)
                        .putExtra("newDevice", deviceInfo)
                        .putExtra("oldDevice", oldDevice), IS_ENABLE
                )
            }
            binding.btUnbind.id -> {
                startActivity(
                    Intent(this@NotEnabledActivity, UnBindDeviceActivity::class.java)
                        .putExtra("type", deviceInfo.deviceType.toString())
                        .putExtra("mac", deviceInfo.deviceMac)
                        .putExtra("isEnable", false)
                        .putExtra("clazz", this@NotEnabledActivity::class.java)
                )
            }
            binding.title.tvTitle.id -> {
                if (isEnable) {

                }
                this.finish()
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
//        binding.title.tvTitle.text = getString(R.string.device_info_title)
        setTvTitle(R.string.device_info_title)
        val info = intent.getSerializableExtra("list") as BindListResponse.DeviceItem?
        if (info == null) {
            finish()
            return
        }
        deviceInfo = info
        oldDevice = if (intent.getStringExtra("oldDevice") != null) {
            intent.getStringExtra("oldDevice").toString()
        } else {
            ""
        }

        setViewsClickListener(this, binding.lyStartDevice, binding.btUnbind, binding.title.tvTitle)

        binding.tvDeviceName.text = deviceInfo.deviceName
        refreshDeviceStatus()
        for (i in Global.productList.indices) {
            if (TextUtils.equals(deviceInfo.deviceType.toString(), Global.productList[i].deviceType)) {
                if (TextUtils.isEmpty(Global.productList[i].homeLogo)) {
                    GlideApp.with(this).load(R.mipmap.device_no_bind_right_img).into(binding.ivIcon)
                } else {
                    GlideApp.with(this).load(Global.productList[i].homeLogo).into(binding.ivIcon)
                }
            }
        }

        binding.tvDeviceVersion.text = "V${deviceInfo.deviceVersion}"
        binding.tvAddDevice.text = "${deviceInfo.deviceMac}"
    }

    private fun refreshDeviceStatus() {
        if (deviceInfo.deviceStatus == 1) {
            binding.tvState.text = getString(R.string.device_fragment_using)
        } else {
            binding.tvState.text = getString(R.string.device_fragment_using_no)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IS_ENABLE -> {
                isEnable = resultCode == EnableDeviceActivity.IS_ENABLE_SUCCESS
            }
        }
    }
}