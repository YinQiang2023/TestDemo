package com.smartwear.publicwatch.ui.device.setting.more

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityDonotdisturbBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.utils.TimeUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.view.wheelview.TimePicker
import com.smartwear.publicwatch.view.wheelview.annotation.TimeMode
import com.smartwear.publicwatch.view.wheelview.entity.TimeEntity
import com.smartwear.publicwatch.view.wheelview.widget.TimeWheelLayout
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DoNotDisturbModeBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import java.lang.StringBuilder

/**
 * Created by Android on 2021/10/28.
 */
class DoNotDisturbActivity : BaseActivity<ActivityDonotdisturbBinding, DeviceModel>(
    ActivityDonotdisturbBinding::inflate, DeviceModel::class.java
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
        setTvTitle(R.string.dev_more_set_not_disturb)

        loadDialog = DialogUtils.showLoad(this)

        binding.tvText02.isEnabled = binding.mSwitch.isChecked
        binding.tvText03.isEnabled = binding.mSwitch.isChecked
        binding.tvStartTime.isEnabled = binding.mSwitch.isChecked
        binding.tvEndTime.isEnabled = binding.mSwitch.isChecked
        binding.llStartTime.isEnabled = binding.mSwitch.isChecked
        binding.llEndTime.isEnabled = binding.mSwitch.isChecked

        setViewsClickListener(
            this,
            tvTitle!!,
            binding.llStartTime,
            binding.llEndTime,
            binding.btnSave
        )

        binding.mSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            isUnCommit = true
            binding.tvText02.isEnabled = isChecked
            binding.tvText03.isEnabled = isChecked
            binding.tvStartTime.isEnabled = isChecked
            binding.tvEndTime.isEnabled = isChecked
            binding.llStartTime.isEnabled = isChecked
            binding.llEndTime.isEnabled = isChecked

        }

        binding.mSmartSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            isUnCommit = true
        }
    }

    override fun initData() {
        super.initData()
        viewModel.deviceSettingLiveData.getDoNotDisturbMode().observe(this, { doNotDisturb ->
            if (doNotDisturb == null) return@observe
            LogUtils.d("勿扰模式 ->${GsonUtils.toJson(doNotDisturb)}")
            binding.mSwitch.isChecked = doNotDisturb.isSwitch

            binding.tvText02.isEnabled = binding.mSwitch.isChecked
            binding.tvText03.isEnabled = binding.mSwitch.isChecked
            binding.tvStartTime.isEnabled = binding.mSwitch.isChecked
            binding.tvEndTime.isEnabled = binding.mSwitch.isChecked
            binding.llStartTime.isEnabled = binding.mSwitch.isChecked
            binding.llEndTime.isEnabled = binding.mSwitch.isChecked

            binding.mSmartSwitch.isChecked = doNotDisturb.isSmartSwitch
            binding.tvStartTime.text = StringBuilder().append(TimeUtils.getSpecialStr(doNotDisturb.startTime.hour))
                .append(":").append(TimeUtils.getSpecialStr(doNotDisturb.startTime.minuter))
            binding.tvEndTime.text = StringBuilder().append(TimeUtils.getSpecialStr(doNotDisturb.endTime.hour))
                .append(":").append(TimeUtils.getSpecialStr(doNotDisturb.endTime.minuter))
            isUnCommit = false
            checkLegalTime()
        })

        loadDialog.show()
        ControlBleTools.getInstance().getDoNotDisturbMode(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
                AppTrackingManager.saveOnlyBehaviorTracking("7", "15")
            }
        })
    }

    //region 检测时间是否合法
    /**
     * 检测时间是否合法
     * */
    private fun checkLegalTime() {
        binding.btnSave.isEnabled = true
        /*if(binding.tvStartTime.text.toString().contains(":") &&
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
        binding.btnSave.isEnabled = false
        */
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
            checkLegalTime()
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
                    sendDisturb()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    //region 设置勿扰
    private fun sendDisturb() {
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
        val bean = DoNotDisturbModeBean()
        bean.isSwitch = binding.mSwitch.isChecked
        bean.isSmartSwitch = binding.mSmartSwitch.isChecked
        bean.startTime = SettingTimeBean(
            startTime.split(":")[0].toInt(),
            startTime.split(":")[1].toInt()
        )
        bean.endTime = SettingTimeBean(
            endTime.split(":")[0].toInt(),
            endTime.split(":")[1].toInt()
        )
        loadDialog.show()
        LogUtils.d("勿扰模式设置 ->${GsonUtils.toJson(bean)}")
        ControlBleTools.getInstance().setDoNotDisturbMode(bean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
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
            binding.btnSave.id -> {
                sendDisturb()
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