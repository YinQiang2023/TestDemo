package com.smartwear.xzfit.ui.device.wristbright

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityWristBrightBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.view.wheelview.OptionPicker
import com.smartwear.xzfit.view.wheelview.TimePicker
import com.smartwear.xzfit.view.wheelview.annotation.TimeMode
import com.smartwear.xzfit.view.wheelview.contract.TextProvider
import com.smartwear.xzfit.view.wheelview.entity.TimeEntity
import com.smartwear.xzfit.view.wheelview.widget.TimeWheelLayout
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.bean.WristScreenBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import java.io.Serializable

class WristBrightActivity : BaseActivity<ActivityWristBrightBinding, DeviceModel>(
    ActivityWristBrightBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    //等待loading
    private lateinit var loadDialog: Dialog
    private var timeModel = 0
    private var sensitivityMode = 0
    val timeModelData = mutableListOf<Bean>()
    val sensitivityModeData = mutableListOf<SensitivityModeDataBean>()

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.tvWristScreen.id -> {
                createTimingModeDialog(binding.tvWristScreen.text.toString().trim())
            }
            binding.tvStartTime.id -> {
                val array = binding.tvStartTime.text.trim().split(":")
                createTimeDialog(array[0].toInt(), array[1].toInt(), true)
            }
            binding.tvEndTime.id -> {
                val array = binding.tvEndTime.text.trim().split(":")
                createTimeDialog(array[0].toInt(), array[1].toInt(), false)
            }
            binding.tvSensitiveSetting.id -> {
                createSensitivityModeDialog(binding.tvSensitiveSetting.text.toString().trim())
            }
            binding.btnSave.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }
                val bean = WristScreenBean()
                bean.timingMode = timeModel
                bean.sensitivityMode = sensitivityMode
                val arrayStart = binding.tvStartTime.text.trim().split(":")
                bean.startTime = SettingTimeBean(arrayStart[0].toInt(), arrayStart[1].toInt())
                val arrayEnd = binding.tvEndTime.text.trim().split(":")
                bean.endTime = SettingTimeBean(arrayEnd[0].toInt(), arrayEnd[1].toInt())
                LogUtils.d("抬腕亮屏设置 ->${GsonUtils.toJson(bean)}")
                ControlBleTools.getInstance().setWristScreen(bean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        if (state.name == SendCmdState.SUCCEED.name) {
                            this@WristBrightActivity.finish()
                            AppTrackingManager.saveOnlyBehaviorTracking("7","13")
                        } else {
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    }
                })
            }
        }
    }

    /**
     * 检测时间是否合法
     * @param isNeedChecked 是否需要检测
     */
    private fun checkLegalTime(isNeedChecked: Boolean = false) {
        binding.btnSave.isEnabled = true
        /*if(!isNeedChecked){
            binding.btnSave.isEnabled = true
            return
        }
        if(binding.tvStartTime.text.toString().contains(":") &&
            binding.tvStartTime.text.toString().contains(":")) {
            val s = binding.tvStartTime.text.toString().trim().split(":")
            val e = binding.tvEndTime.text.toString().trim().split(":")
            var hE = e[0].toInt() - s[0].toInt() //时差
            var mE = e[1].toInt() - s[1].toInt() //分差
            LogUtils.d("时间区间--->$hE , $mE")
            if (hE == 1 && mE > 0) { //差值 > 一小时
                binding.btnSave.isEnabled = true
                return
            }
            if (hE > 1) { //差值 > 一小时
                binding.btnSave.isEnabled = true
                return
            }
        }
        binding.btnSave.isEnabled = false*/
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }


    override fun initView() {
        super.initView()
        setTvTitle(R.string.wrist_bright_screen_title)

        loadDialog = DialogUtils.showLoad(this)


        setViewsClickListener(
            this,
            binding.tvWristScreen,
            binding.tvStartTime,
            binding.tvEndTime,
            binding.tvSensitiveSetting,
            binding.btnSave
        )

        fillTimeModeData()
        fillSensitivityMode()
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        super.initData()
        viewModel.deviceSettingLiveData.getWristScreen().observe(this, Observer {
            if (it == null) return@Observer
            LogUtils.d("抬腕亮屏 ->${GsonUtils.toJson(it)}")
            it.endTime
            timeModel = it.timingMode
            sensitivityMode = it.sensitivityMode
            binding.tvWristScreen.text = timeModelData[timeModelData.indexOfFirst { data -> data.value == timeModel }].text
            binding.tvSensitiveSetting.text = sensitivityModeData[sensitivityModeData.indexOfFirst { data -> data.value == sensitivityMode }].text
            var startHour = if (it.startTime.hour < 10) {
                "0${it.startTime.hour}"
            } else {
                "${it.startTime.hour}"
            }
            var startMin = if (it.startTime.minuter < 10) {
                "0${it.startTime.minuter}"
            } else {
                "${it.startTime.minuter}"
            }
            binding.tvStartTime.text = "$startHour:$startMin"

            var endHour = if (it.endTime.hour < 10) {
                "0${it.endTime.hour}"
            } else {
                "${it.endTime.hour}"
            }
            var endMin = if (it.endTime.minuter < 10) {
                "0${it.endTime.minuter}"
            } else {
                "${it.endTime.minuter}"
            }
            binding.tvEndTime.text = "$endHour:$endMin"
            binding.tvStartTime.isEnabled = it.timingMode == 1
            binding.tvEndTime.isEnabled = it.timingMode == 1
            binding.tvEt.isEnabled = it.timingMode == 1
            binding.tvSt.isEnabled = it.timingMode == 1
            checkLegalTime(it.timingMode == 1)
        })
        loadDialog.show()
        ControlBleTools.getInstance().getWristScreen(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
            }
        })
    }

    private fun fillTimeModeData() {
        timeModelData.add(Bean(0, getString(R.string.wrist_bright_screen_open_all_day)))
        timeModelData.add(Bean(1, getString(R.string.timing_mode_3)))
        timeModelData.add(Bean(2, getString(R.string.close)))
        timeModelData.add(Bean(3, getString(R.string.wrist_bright_screen_intelligent_open)))
    }

    private fun fillSensitivityMode() {
        sensitivityModeData.add(SensitivityModeDataBean(0, getString(R.string.wrist_bright_screen_low)))
        sensitivityModeData.add(SensitivityModeDataBean(1, getString(R.string.wrist_bright_screen_middle)))
        sensitivityModeData.add(SensitivityModeDataBean(2, getString(R.string.wrist_bright_screen_high)))
    }

    private fun createTimingModeDialog(default: String) {
        val defaultPosition = timeModelData.indexOfFirst { it.text == default }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            binding.tvWristScreen.text = timeModelData[position].text
            timeModel = timeModelData[position].value
            binding.tvStartTime.isEnabled = timeModel == 1
            binding.tvEndTime.isEnabled = timeModel == 1
            binding.tvEt.isEnabled = timeModel == 1
            binding.tvSt.isEnabled = timeModel == 1
            checkLegalTime(timeModel == 1)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(timeModelData)
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }

    class Bean(var value: Int, var text: String) : Serializable, TextProvider {
        override fun provideText(): String {
            return text
        }
    }

    @SuppressLint("SetTextI18n")
    private fun createTimeDialog(inHour: Int, inMinute: Int, isStart: Boolean) {
        val picker = TimePicker(this)
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setOnTimeMeridiemPickedListener { hour, minute, second, isAnteMeridiem ->
            var resultHour = if (hour < 10) {
                "0$hour"
            } else {
                "$hour"
            }
            var resultMinute = if (minute < 10) {
                "0$minute"
            } else {
                "$minute"
            }
            if (isStart) {
                binding.tvStartTime.text = "$resultHour:$resultMinute"
            } else {
                binding.tvEndTime.text = "$resultHour:$resultMinute"
            }
            checkLegalTime(true)
        }
        val wheelLayout: TimeWheelLayout = picker.wheelLayout
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND)
        wheelLayout.setTimeLabel(":", " ", "")
        wheelLayout.setDefaultValue(TimeEntity.target(inHour, inMinute, 0))
        wheelLayout.setCyclicEnabled(true, true)

        picker.show()
    }

    private fun createSensitivityModeDialog(default: String) {
        val defaultPosition = sensitivityModeData.indexOfFirst { it.text == default }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            binding.tvSensitiveSetting.text = sensitivityModeData[position].text
            sensitivityMode = sensitivityModeData[position].value
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(sensitivityModeData)
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }

    class SensitivityModeDataBean(var value: Int, var text: String) : Serializable, TextProvider {
        override fun provideText(): String {
            return text
        }
    }

}