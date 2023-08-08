package com.smartwear.xzfit.ui.device.setting.heartrate

import android.app.Dialog
import android.view.View
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityHeartRateSettingBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.view.wheelview.OptionPicker
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.HeartRateMonitorBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.ui.device.bean.DeviceSettingBean
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.manager.AppTrackingManager

class HeartRateSettingActivity : BaseActivity<ActivityHeartRateSettingBinding, DeviceModel>(ActivityHeartRateSettingBinding::inflate, DeviceModel::class.java),
    View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private val TAG: String = HeartRateSettingActivity::class.java.simpleName

    //等待loading
    private lateinit var loadDialog: Dialog

    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSave.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }
                val bean = HeartRateMonitorBean()
                bean.frequency = 0 //设备代码写死5
                bean.mode = if (binding.chbContinuousHeartHare.isChecked) {
                    0
                } else {
                    1
                }
                bean.isWarning = binding.chbHeartHareWarning.isChecked
                bean.warningValue = binding.tvHighestAlarm.text.toString().trim().toInt()
                loadDialog.show()
                LogUtils.d("心率检测设置 ->${GsonUtils.toJson(bean)}")
                ControlBleTools.getInstance()
                    .setHeartRateMonitor(bean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            if (state == SendCmdState.SUCCEED) {
                                ToastUtils.showToast(R.string.save_success)
                                finish()
                                if (binding.chbContinuousHeartHare.isChecked) {
                                    AppTrackingManager.saveOnlyBehaviorTracking("7", "11")
                                }
                                return
                            }
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    })
            }
            binding.layoutHeightsAlarm.id -> {
                createRangDialog(binding.tvHighestAlarm.text.toString().trim())
            }
            binding.chbHeartHareWarning.id -> {
                binding.layoutHeightsAlarm.isEnabled = binding.chbHeartHareWarning.isChecked
                binding.tvHighestAlarm.isEnabled = binding.chbHeartHareWarning.isChecked
                binding.tvAlarmUnit.isEnabled = binding.chbHeartHareWarning.isChecked
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            binding.chbContinuousHeartHare.id -> {
            }
            binding.chbHeartHareWarning.id -> {
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(/*R.string.heart_hare_set_title*/R.string.device_set_heart)

        loadDialog = DialogUtils.showLoad(this)

        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )

        if (deviceSettingBean != null) {
            if (!deviceSettingBean!!.settingsRelated.continuous_heart_rate_switch) {
                binding.lyContinuousHeart.visibility = View.GONE
            }
            if (!deviceSettingBean!!.reminderRelated.heart_rate_warning) {
                binding.lyHeartWarning.visibility = View.GONE
            }
        }

        setViewsClickListener(
            this, binding.btnSave, binding.layoutHeightsAlarm,
            binding.chbContinuousHeartHare/*,binding.chbHeartHareWarning*/
        )
        binding.chbHeartHareWarning.setOnClickListener(this)
        observe()
    }

    private fun observe() {
        viewModel.deviceSettingLiveData.getHeartRateMonitorBean().observe(this, Observer {
            if (it == null) return@Observer
            LogUtils.d("心率检测 ->${GsonUtils.toJson(it)}")
            binding.chbContinuousHeartHare.isChecked = it.mode == 0
            binding.chbHeartHareWarning.isChecked = it.isWarning == true
            binding.layoutHeightsAlarm.isEnabled = binding.chbHeartHareWarning.isChecked
            binding.tvHighestAlarm.isEnabled = binding.chbHeartHareWarning.isChecked
            binding.tvAlarmUnit.isEnabled = binding.chbHeartHareWarning.isChecked
            binding.tvHighestAlarm.text = it.warningValue.toString()
        })
        loadDialog.show()
        ControlBleTools.getInstance().getHeartRateMonitor(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
            }
        })
    }

    private fun createRangDialog(default: String) {
        val data = mutableListOf<SettingHeartRateBean>()
        data.add(SettingHeartRateBean("100"))
        data.add(SettingHeartRateBean("105"))
        data.add(SettingHeartRateBean("110"))
        data.add(SettingHeartRateBean("115"))
        data.add(SettingHeartRateBean("120"))
        data.add(SettingHeartRateBean("125"))
        data.add(SettingHeartRateBean("130"))
        data.add(SettingHeartRateBean("135"))
        data.add(SettingHeartRateBean("140"))
        data.add(SettingHeartRateBean("145"))
        data.add(SettingHeartRateBean("150"))
        val defaultPosition = data.indexOfFirst { it.name == default }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            binding.tvHighestAlarm.text = item.toString()
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }

}