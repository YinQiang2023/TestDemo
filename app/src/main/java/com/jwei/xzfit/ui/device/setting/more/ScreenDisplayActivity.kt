package com.jwei.xzfit.ui.device.setting.more

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityScreenDisplayBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.utils.TimeUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.view.wheelview.OptionPicker
import com.jwei.xzfit.view.wheelview.TimePicker
import com.jwei.xzfit.view.wheelview.annotation.TimeMode
import com.jwei.xzfit.view.wheelview.entity.TimeEntity
import com.jwei.xzfit.view.wheelview.widget.TimeWheelLayout
import com.jwei.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DialStyleBean
import com.zhapp.ble.bean.ScreenDisplayBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.utils.GlideApp
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import java.lang.StringBuilder

/**
 * Created by Android on 2021/10/28.
 * 屏息显示设置
 */
class ScreenDisplayActivity : BaseActivity<ActivityScreenDisplayBinding, DeviceModel>(
    ActivityScreenDisplayBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    //等待loading
    private lateinit var loadDialog: Dialog

    //是否未提交修改
    private var isUnCommit = false


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.dev_more_set_screen_display)

        loadDialog = DialogUtils.showLoad(this)

        setViewsClickListener(
            this,
            tvTitle!!,
            binding.llScreenSet,
            binding.llStartTime,
            binding.llEndTime,
            binding.btnSave
        )

        binding.rgDial.setOnCheckedChangeListener { group, checkedId ->
            isUnCommit = true
        }
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getScreenDisplay().observe(this, { screenDisplayBean ->
            if (screenDisplayBean == null) return@observe
            LogUtils.d("息屏设置 ->${GsonUtils.toJson(screenDisplayBean)}")
            binding.tvScreenSet.text = when (screenDisplayBean.timingMode) {
                //  0 全天开启 1 时间段开启 2全天关闭
                1 -> {
                    getString(R.string.timing_mode_3)
                }
                2 -> {
                    getString(R.string.close)
                }
                0 -> {
                    getString(R.string.timing_mode_0)
                }
                else -> {
                    getString(R.string.timing_mode_0)
                }
            }
            binding.llStartTime.isEnabled = screenDisplayBean.timingMode == 1
            binding.llEndTime.isEnabled = screenDisplayBean.timingMode == 1
            binding.tvStartTime.isEnabled = screenDisplayBean.timingMode == 1
            binding.tvEndTime.isEnabled = screenDisplayBean.timingMode == 1
            binding.tvEt.isEnabled = screenDisplayBean.timingMode == 1
            binding.tvSt.isEnabled = screenDisplayBean.timingMode == 1
            binding.rbPointer.isEnabled = screenDisplayBean.timingMode != 2
            binding.rbNumber.isEnabled = screenDisplayBean.timingMode != 2

            binding.rgDial.clearCheck()
            when (screenDisplayBean.dialStyle.Style) {
                // 0 指针 1数字
                0 -> binding.rbPointer.isChecked = true
                1 -> binding.rbNumber.isChecked = true
            }
            binding.tvStartTime.text = StringBuilder().append(TimeUtils.getSpecialStr(screenDisplayBean.startTime.hour))
                .append(":").append(TimeUtils.getSpecialStr(screenDisplayBean.startTime.minuter))
            binding.tvEndTime.text = StringBuilder().append(TimeUtils.getSpecialStr(screenDisplayBean.endTime.hour))
                .append(":").append(TimeUtils.getSpecialStr(screenDisplayBean.endTime.minuter))
            isUnCommit = false

            checkLegalTime(screenDisplayBean.timingMode == 1)
        })
        loadDialog.show()
        ControlBleTools.getInstance().getScreenDisplay(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
                AppTrackingManager.saveOnlyBehaviorTracking("7","16")
            }
        })

        viewModel.productInfo.observe(this) {
            if (it == null) return@observe
            if (it.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                LogUtils.e("产品设备信息 -> " + GsonUtils.toJson(it))
                if (!it.data.screenList.isNullOrEmpty()) {
                    for (dis in it.data.screenList!!) {
                        if (dis.key.isNotEmpty() && dis.url.isNotEmpty()) {
                            if (dis.key == "1") {
                                GlideApp.with(this).load(dis.url).placeholder(R.mipmap.def_display1)
                                    .error(R.mipmap.def_display1).into(binding.ivDisplay1)
                            } else if (dis.key == "2") {
                                GlideApp.with(this).load(dis.url).placeholder(R.mipmap.def_display2)
                                    .error(R.mipmap.def_display2).into(binding.ivDisplay2)
                            }
                        }
                    }
                }
            }
        }
        //获取
        viewModel.getProductInfo()
    }

    //region 检测时间是否合法
    /**
     * 检测时间是否合法
     * @param isNeedChecked 是否需要检测
     * */
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
    //endregion

    //region 开启模式选择
    private fun createRangDialog(default: String) {
        val data = mutableListOf<String>()
        data.add(getString(R.string.timing_mode_0))
        data.add(getString(R.string.timing_mode_3))
        data.add(getString(R.string.close))
        var defaultPosition = data.indexOfFirst { it == default }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            binding.tvScreenSet.text = item.toString()
            isUnCommit = true

            binding.llStartTime.isEnabled = position == 1
            binding.llEndTime.isEnabled = position == 1
            binding.tvStartTime.isEnabled = position == 1
            binding.tvEndTime.isEnabled = position == 1
            binding.tvEt.isEnabled = position == 1
            binding.tvSt.isEnabled = position == 1
            binding.rbPointer.isEnabled = position != 2
            binding.rbNumber.isEnabled = position != 2
            checkLegalTime(position == 1)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        if (defaultPosition == -1) defaultPosition = 0
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }
    //endregion

    //region 时间选择
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
            isUnCommit = true
            checkLegalTime(true)
        }
        val wheelLayout: TimeWheelLayout = picker.wheelLayout
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND)
        wheelLayout.setTimeLabel(":", " ", "")
        wheelLayout.setDefaultValue(TimeEntity.target(inHour, inMinute, 0))
        wheelLayout.setCyclicEnabled(true, true)

        picker.show()
    }
    //endregion

    //region 未提交提示
    private fun showUnCommitDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.save_nu_commit_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    sendDisplay()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    //region 设置屏息
    /**
     * @param isShowPowerSaving 是否检测省电模式 默认true
     */
    private fun sendDisplay(isShowPowerSaving: Boolean = true) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        var timingMode = -1
        var dialStyle = -1
        if (binding.rbNumber.isChecked) dialStyle = 1
        if (binding.rbPointer.isChecked) dialStyle = 0
        if (dialStyle == -1) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.screen_dial_plate)).toString()
            )
            return
        }
        timingMode = when (binding.tvScreenSet.text.toString().trim()) {
            getString(R.string.timing_mode_0) -> 0
            getString(R.string.timing_mode_3) -> 1
            getString(R.string.close) -> 2
            else -> -1
        }
        if (timingMode == -1) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.screen_set)).toString()
            )
            return
        }
        var startTime = binding.tvStartTime.text.toString().trim()
        var endTime = binding.tvEndTime.text.toString().trim()
        if (timingMode == 1 && !startTime.contains(":")) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.sleep_start_time_tips)).toString()
            )
            return
        } else {
            if (startTime.isEmpty() || startTime == getString(R.string.no_data_sign)) {
                startTime = "00:00"
            }
        }
        if (timingMode == 1 && !endTime.contains(":")) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.sleep_end_time_tips)).toString()
            )
            return
        } else {
            if (endTime.isEmpty() || endTime == getString(R.string.no_data_sign)) {
                endTime = "00:00"
            }
        }
        if (isShowPowerSaving &&
            SpUtils.getSPUtilsInstance().getBoolean(SpUtils.DEVICE_POWER_SAVING) &&
            (timingMode == 1 || timingMode == 0)
        ) {
            showClosePowerSavingDialog()
            return
        }
        val screeDisplay = ScreenDisplayBean()
        screeDisplay.timingMode = timingMode
        screeDisplay.startTime = SettingTimeBean(
            startTime.split(":")[0].toInt(),
            startTime.split(":")[1].toInt()
        )
        screeDisplay.endTime = SettingTimeBean(
            endTime.split(":")[0].toInt(),
            endTime.split(":")[1].toInt()
        )
        screeDisplay.dialStyle = DialStyleBean(dialStyle)
        loadDialog.show()
        LogUtils.d("息屏设置 设置 ->${GsonUtils.toJson(screeDisplay)}")
        ControlBleTools.getInstance().setScreenDisplay(screeDisplay, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                if (state == SendCmdState.SUCCEED) {
                    isUnCommit = false
                    ToastUtils.showToast(R.string.save_success)
                    finish()
                }
                ToastUtils.showSendCmdStateTips(state)
            }
        })

    }

    /**
     * 关闭息屏显示dialgo
     * */
    private fun showClosePowerSavingDialog() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.close_power_saving),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    ControlBleTools.getInstance().setPowerSaving(false,
                        object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(state: SendCmdState) {
                                DialogUtils.dismissDialog(loadDialog)
                                ToastUtils.showSendCmdStateTips(state)
                                ControlBleTools.getInstance().getPowerSaving(null)
                                sendDisplay(false)
                            }
                        })
                }

                override fun OnCancel() {

                }
            }).show()

    }
    //endregion

    override fun onClick(v: View) {
        when (v.id) {
            tvTitle?.id -> {
                /*if (isUnCommit && binding.btnSave.isEnabled) {
                    showUnCommitDialog()
                    return
                }*/
                finish()
            }
            binding.llScreenSet.id -> {
                createRangDialog(binding.tvScreenSet.text.toString().trim())
            }
            binding.llStartTime.id -> {
                if (binding.tvStartTime.text.toString().trim().contains(":")) {
                    val array = binding.tvStartTime.text.trim().split(":")
                    createTimeDialog(array[0].toInt(), array[1].toInt(), true)
                } else {
                    createTimeDialog(0, 0, true)
                }
            }
            binding.llEndTime.id -> {
                if (binding.tvEndTime.text.toString().trim().contains(":")) {
                    val array = binding.tvEndTime.text.trim().split(":")
                    createTimeDialog(array[0].toInt(), array[1].toInt(), false)
                } else {
                    createTimeDialog(0, 0, false)
                }
            }
            binding.btnSave.id -> {
                sendDisplay()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        /*if (keyCode == KeyEvent.KEYCODE_BACK && isUnCommit && binding.btnSave.isEnabled) {
            showUnCommitDialog()
            return false
        }*/
        return super.onKeyDown(keyCode, event)
    }

}