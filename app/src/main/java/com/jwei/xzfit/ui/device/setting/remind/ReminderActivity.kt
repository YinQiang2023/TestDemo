package com.jwei.xzfit.ui.device.setting.remind

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.constant.TimeConstants
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityRemindBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.utils.TimeUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.view.wheelview.NumberPicker
import com.jwei.xzfit.view.wheelview.TimePicker
import com.jwei.xzfit.view.wheelview.annotation.TimeMode
import com.jwei.xzfit.view.wheelview.entity.TimeEntity
import com.jwei.xzfit.view.wheelview.widget.TimeWheelLayout
import com.jwei.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.CommonReminderBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.utils.manager.AppTrackingManager
import java.lang.StringBuilder
import java.util.*
import kotlin.math.abs

/**
 * Created by Android on 2021/10/23.
 * 久坐喝水吃药提醒设置页面
 */
class ReminderActivity : BaseActivity<ActivityRemindBinding, DeviceModel>(
    ActivityRemindBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    //提醒类型
    private var type = 0

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
        type = intent.getIntExtra("type", Global.REMINDER_TYPE_SEDENTARY)
        loadDialog = DialogUtils.showLoad(this)
        setTvTitle(
            when (type) {
                Global.REMINDER_TYPE_DRINK -> R.string.device_remind_drink_water
                Global.REMINDER_TYPE_PILLS -> R.string.device_remind_take_pills
                Global.REMINDER_TYPE_SEDENTARY -> R.string.device_remind_sedentary
                Global.REMINDER_TYPE_HAND_WASH -> R.string.device_remind_hand_washing
                else -> R.string.device_remind_sedentary
            }
        )

        //吃药没有午休免打扰
        binding.clNoDisturb.visibility =
            if (type == Global.REMINDER_TYPE_PILLS) View.GONE else View.VISIBLE

//        binding.tvTitle2.text = when (type) {
//            Global.REMINDER_TYPE_DRINK -> getString(R.string.device_remind_drink_water)
//            Global.REMINDER_TYPE_PILLS -> getString(R.string.device_remind_take_pills)
//            Global.REMINDER_TYPE_SEDENTARY -> getString(R.string.device_remind_sedentary)
//            Global.REMINDER_TYPE_HAND_WASH -> getString(R.string.device_remind_hand_washing)
//            else -> getString(R.string.device_remind_sedentary)
//        }

        binding.tvHint.text = when (type) {
            Global.REMINDER_TYPE_DRINK -> getString(R.string.open_remind_drink_tip)
            Global.REMINDER_TYPE_PILLS -> getString(R.string.open_remind_pills_tip)
            Global.REMINDER_TYPE_SEDENTARY -> getString(R.string.open_remind_sedentary_tip)
            Global.REMINDER_TYPE_HAND_WASH -> getString(R.string.open_remind_hand_washing_tip)
            else -> getString(R.string.open_remind_sedentary_tip)
        }

        setViewsClickListener(
            this,
            tvTitle!!,
            binding.llStartTime,
            binding.llEndTime,
            binding.llInterval,
            binding.btnSave
        )

        binding.mSwitch.setOnClickListener {
            isUnCommit = true
            //LogUtils.e("checked --> ${binding.mSwitch.isChecked}")
            //禁用 解禁
            binding.llStartTime.isEnabled = binding.mSwitch.isChecked
            binding.tvStartTime.isEnabled = binding.mSwitch.isChecked
            binding.llEndTime.isEnabled = binding.mSwitch.isChecked
            binding.tvEndTime.isEnabled = binding.mSwitch.isChecked
            binding.llInterval.isEnabled = binding.mSwitch.isChecked
            binding.tvInterval.isEnabled = binding.mSwitch.isChecked
            binding.btnSave.isEnabled = true
        }
        binding.mSwitchDisturb.setOnClickListener {
            isUnCommit = true
            binding.btnSave.isEnabled = true
        }
    }

    override fun initData() {
        super.initData()
        when (type) {
            Global.REMINDER_TYPE_DRINK -> {
                viewModel.deviceSettingLiveData.getDrinkWaterReminder().observe(this, {
                    setReminderData(it)
                })
            }
            Global.REMINDER_TYPE_PILLS -> {
                viewModel.deviceSettingLiveData.getMedicationReminder().observe(this, {
                    setReminderData(it)
                })
            }
            Global.REMINDER_TYPE_SEDENTARY -> {
                viewModel.deviceSettingLiveData.getSedentaryReminder().observe(this, {
                    setReminderData(it)
                })
            }
            Global.REMINDER_TYPE_HAND_WASH -> {
                viewModel.deviceSettingLiveData.getHandWashingRemider().observe(this, {
                    setReminderData(it)
                })
            }
        }

        when (type) {
            Global.REMINDER_TYPE_DRINK -> {
                loadDialog.show()
                ControlBleTools.getInstance().getDrinkWaterReminder(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state) {
                            //获取超时直接关闭页面
                            finish()
                        }
                    }
                })
            }
            Global.REMINDER_TYPE_PILLS -> {
                loadDialog.show()
                ControlBleTools.getInstance().getMedicationReminder(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state) {
                            //获取超时直接关闭页面
                            finish()
                        }
                    }
                })
            }
            Global.REMINDER_TYPE_SEDENTARY -> {
                loadDialog.show()
                ControlBleTools.getInstance().getSedentaryReminder(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state) {
                            //获取超时直接关闭页面
                            finish()
                        }
                    }
                })
            }
            Global.REMINDER_TYPE_HAND_WASH -> {
                loadDialog.show()
                ControlBleTools.getInstance().getWashHandReminder(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state) {
                            //获取超时直接关闭页面
                            finish()
                        }
                    }
                })
            }
        }
    }

    //region 设置提醒数据
    private fun setReminderData(reminderBean: CommonReminderBean?) {
        LogUtils.d("提醒 ->${GsonUtils.toJson(reminderBean)}")
        if (reminderBean == null) return
        binding.mSwitch.isChecked = reminderBean.isOn

        //禁用 解禁
        binding.llStartTime.isEnabled = reminderBean.isOn
        binding.tvStartTime.isEnabled = reminderBean.isOn
        binding.llEndTime.isEnabled = reminderBean.isOn
        binding.tvEndTime.isEnabled = reminderBean.isOn
        binding.llInterval.isEnabled = reminderBean.isOn
        binding.tvInterval.isEnabled = reminderBean.isOn

        binding.mSwitchDisturb.isChecked = reminderBean.noDisturbInLaunch
        frequency = reminderBean.frequency
        if (type == Global.REMINDER_TYPE_PILLS) { //设备默认返回60，错误！人修改为4小时
            if (frequency == 60) frequency *= 4
        }
        binding.tvInterval.text = StringBuilder()
            .append(frequency / 60)
            .append(getString(R.string.hours_text))

        binding.tvStartTime.text = StringBuilder().append(TimeUtils.getSpecialStr(reminderBean.startTime.hour))
            .append(":").append(TimeUtils.getSpecialStr(reminderBean.startTime.minuter))

        binding.tvEndTime.text = StringBuilder().append(TimeUtils.getSpecialStr(reminderBean.endTime.hour))
            .append(":").append(TimeUtils.getSpecialStr(reminderBean.endTime.minuter))
//        getSEdifference()
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
            binding.btnSave.isEnabled = true

        }
        val wheelLayout: TimeWheelLayout = picker.wheelLayout
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND)
        wheelLayout.setTimeLabel(":", " ", "")
        wheelLayout.setDefaultValue(TimeEntity.target(inHour, inMinute, 0))
        wheelLayout.setCyclicEnabled(true, true)

        picker.show()
    }

    /**
     * 计算区间差
     */
    private fun getSEdifference(): Boolean {
        if (binding.tvStartTime.text.toString().trim().contains(":") &&
            binding.tvStartTime.text.toString().trim().contains(":")
        ) {
            val s = binding.tvStartTime.text.toString().trim().split(":")
            val e = binding.tvEndTime.text.toString().trim().split(":")
            try {
                if (s[0].toInt() == e[0].toInt() && s[1].toInt() == e[1].toInt()) { //时间相同 为一整天
                    return true
                }
                /*if (s[0].toInt() == 0 && s[1].toInt() == 0 && e[0].toInt() == 0 && e[1].toInt() == 0) { //00:00 - 00:00 为一整天
                    return true
                }*/
                val span = if (s[0] > e[0]) {
                    abs(
                        com.blankj.utilcode.util.TimeUtils.getTimeSpan(
                            com.blankj.utilcode.util.TimeUtils.string2Date(
                                "2000-01-02 ${binding.tvEndTime.text.toString().trim()}",
                                TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)
                            ),
                            com.blankj.utilcode.util.TimeUtils.string2Date(
                                "2000-01-01 ${binding.tvStartTime.text.toString().trim()}",
                                TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)
                            ),
                            TimeConstants.MIN
                        )
                    )
                } else {
                    com.blankj.utilcode.util.TimeUtils.getTimeSpan(
                        com.blankj.utilcode.util.TimeUtils.string2Date(
                            "2000-01-01 ${binding.tvEndTime.text.toString().trim()}",
                            TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)
                        ),
                        com.blankj.utilcode.util.TimeUtils.string2Date(
                            "2000-01-01 ${binding.tvStartTime.text.toString().trim()}",
                            TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)
                        ),
                        TimeConstants.MIN
                    )
                }
                LogUtils.d("时间区间--->${span}")
                if (span >= frequency) {
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }


    /**
     * 展示非法时间弹窗
     */
    private fun showIllegalTimeDialog() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.illegal_remind_time_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    sendReminder(false)
                }

                override fun OnCancel() {}
            }
        ).show()
    }

    //endregion

    //region 间隔选择
    private fun createIntervalDialog() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            //item.toInt()
            frequency = item.toInt() * 60
            binding.tvInterval.text =
                StringBuilder().append(item.toString()).append(getString(R.string.hours_text))
            isUnCommit = true
            binding.btnSave.isEnabled = true
        }
        if (type == Global.REMINDER_TYPE_PILLS) {
            picker.setRangeStep(2, 6, 2)
            picker.setDefaultValue(4)
        } else {
            picker.setRangeStep(1, 6, 1)
            picker.setDefaultValue(1)
        }

        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }
    //endregion

    //region 设置提醒
    private var frequency = 60
    private fun sendReminder(ischeckTime: Boolean = true) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        val startTime = binding.tvStartTime.text.toString().trim()
        val endTime = binding.tvEndTime.text.toString().trim()
        if (!startTime.contains(":")) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.sleep_start_time_tips)).toString()
            )
            return
        }
        if (!endTime.contains(":")) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.sleep_end_time_tips)).toString()
            )
            return
        }
        if (frequency == 0) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.remind_interval)).toString()
            )
            return
        }

        if (ischeckTime && !getSEdifference()) {
            showIllegalTimeDialog()
            return
        }

        val reminderBean = CommonReminderBean()
        reminderBean.isOn = binding.mSwitch.isChecked
        reminderBean.startTime = SettingTimeBean(
            startTime.split(":")[0].toInt(),
            startTime.split(":")[1].toInt()
        )
        reminderBean.endTime = SettingTimeBean(
            endTime.split(":")[0].toInt(),
            endTime.split(":")[1].toInt()
        )
        reminderBean.frequency = this.frequency
        reminderBean.noDisturbInLaunch = binding.mSwitchDisturb.isChecked
        val callBack = MySendStateCallBack()
        when (type) {
            Global.REMINDER_TYPE_DRINK -> ControlBleTools.getInstance()
                .setDrinkWaterReminder(reminderBean, callBack)
            Global.REMINDER_TYPE_PILLS -> ControlBleTools.getInstance()
                .setMedicationReminder(reminderBean, callBack)
            Global.REMINDER_TYPE_SEDENTARY -> ControlBleTools.getInstance()
                .setSedentaryReminder(reminderBean, callBack)
            Global.REMINDER_TYPE_HAND_WASH -> ControlBleTools.getInstance()
                .setWashHandReminder(reminderBean, callBack)
        }
        LogUtils.d("提醒设置 ->${GsonUtils.toJson(reminderBean)}")
        loadDialog.show()

    }

    inner class MySendStateCallBack : ParsingStateManager.SendCmdStateListener(lifecycle) {
        override fun onState(state: SendCmdState) {
            DialogUtils.dismissDialog(loadDialog)
            when (state) {
                SendCmdState.SUCCEED -> {
                    isUnCommit = false
                    ToastUtils.showToast(R.string.save_success)
                    finish()
                    when (type) {
                        Global.REMINDER_TYPE_DRINK -> AppTrackingManager.saveOnlyBehaviorTracking("6", "5")
                        Global.REMINDER_TYPE_PILLS -> AppTrackingManager.saveOnlyBehaviorTracking("6", "4")
                        Global.REMINDER_TYPE_SEDENTARY -> AppTrackingManager.saveOnlyBehaviorTracking("6", "3")
                        //Global.REMINDER_TYPE_HAND_WASH -> AppTrackingManager.saveOnlyBehaviorTracking("6","5")
                    }
                }
                else -> ToastUtils.showSendCmdStateTips(state)
            }
        }
    }
    //endregion

    //region 未提交提示
    private fun showUnCommitDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.dialog_title_tips),
            getString(R.string.save_nu_commit_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    binding.btnSave.callOnClick()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
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
            binding.llInterval.id -> {
                createIntervalDialog()
            }
            binding.btnSave.id -> {
                sendReminder()
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