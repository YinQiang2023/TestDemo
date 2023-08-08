package com.smartwear.xzfit.ui.device.setting.remind

import android.annotation.SuppressLint
import android.app.Activity
import android.view.KeyEvent
import android.view.View
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityAddAlarmClockBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.view.wheelview.entity.TimeEntity
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.bean.ClockInfoBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.utils.BleUtils
import com.smartwear.xzfit.utils.ProhibitEmojiUtils

/**
 * Created by Android on 2021/10/25.
 * 添加/编辑 闹钟
 */
@SuppressLint("SetTextI18n")
class AddAlarmClockActivity : BaseActivity<ActivityAddAlarmClockBinding, DeviceModel>(ActivityAddAlarmClockBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    private var clockInfo: ClockInfoBean? = null

    //是否闹钟编辑界面
    private var isAlarm = true

    //默认时间08:00
    private var mHour = 8
    private var mMinute = 0

    private var mWeekDays = 0
    private var isMonday = false
    private var isTuesday = false
    private var isWednesday = false
    private var isThursday = false
    private var isFriday = false
    private var isSaturday = false
    private var isSunday = false

    //是否未提交修改 TODO 取消
    private var isUnCommit = false

    //region initView
    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        clockInfo = intent.getSerializableExtra("data") as ClockInfoBean?
        setTvTitle(if (clockInfo == null) R.string.add_alarm_clock else R.string.edit_alarm_clock)
        /*if(clockInfo == null){
            binding.etName.setText(StringBuilder().append(getString(R.string.alarm_clock)).toString())
            binding.etName.setSelection(binding.etName.text.toString().length)
        }*/

        //binding.tvNameNum.text = "0/10"
        //EditTextUtils.evitTextLimit(binding.etName)
        //val filters = arrayOf<InputFilter>(ProhibitEmojiUtils.inputFilterProhibitEmoji(30))
        binding.etName.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(30)

        binding.timeWheel.setOnTimeMeridiemSelectedListener { hour, minute, second, isAnteMeridiem ->
            LogUtils.d("$hour:$minute")
            //isUnCommit = true
            mHour = hour
            mMinute = minute
        }

        binding.clWeek1.tag = false
        binding.clWeek2.tag = false
        binding.clWeek3.tag = false
        binding.clWeek4.tag = false
        binding.clWeek5.tag = false
        binding.clWeek6.tag = false
        binding.clWeek7.tag = false

        setViewsClickListener(
            this,
            tvTitle!!,
            binding.btnSave,
            binding.clWeekDay,
            binding.clWeek1,
            binding.clWeek2,
            binding.clWeek3,
            binding.clWeek4,
            binding.clWeek5,
            binding.clWeek6,
            binding.clWeek7
        )

        //binding.etName.filters = arrayOf<InputFilter>(ProhibitEmojiUtils.inputFilterSpecialCharacter(10))
    }
    //endregion

    //region initData
    override fun initData() {
        super.initData()

        binding.timeWheel.setDefaultValue(TimeEntity.target(8, 0, 0))
        if (clockInfo != null) {
            var name = clockInfo!!.data.clockName
            /*if(name.length>=10) {
                name = clockInfo!!.data.clockName.substring(0, 10)
            }*/
            binding.etName.setText(name)
            binding.etName.setSelection(binding.etName.text.toString().trim().length)
            binding.tvNameNum.text = "${name.length}/10"

            mHour = clockInfo!!.data.time.hour
            mMinute = clockInfo!!.data.time.minuter
            binding.timeWheel.setDefaultValue(TimeEntity.target(mHour, mMinute, 0))
            mWeekDays = clockInfo!!.data.weekDays
            when (mWeekDays) {
                127 -> {  // 01111111 = 127
                    binding.tvWeekDay.text = getString(R.string.everyday)
                }
                0 -> {   // 00000000
                    binding.tvWeekDay.text = getString(R.string.once_only)
                }
                else -> {
                    val weakDayBuilder = StringBuilder()
                    if (clockInfo!!.data.isMonday) weakDayBuilder.append(getString(R.string.week_easy_1))
                    if (clockInfo!!.data.isTuesday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_2))
                    }
                    if (clockInfo!!.data.isWednesday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_3))
                    }
                    if (clockInfo!!.data.isThursday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_4))
                    }
                    if (clockInfo!!.data.isFriday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_5))
                    }
                    if (clockInfo!!.data.isSaturday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_6))
                    }
                    if (clockInfo!!.data.isSunday) {
                        if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                        weakDayBuilder.append(getString(R.string.week_easy_7))
                    }
                    binding.tvWeekDay.text = weakDayBuilder
                }
            }
            isMonday = clockInfo!!.data.isMonday
            binding.clWeek1.tag = isMonday
            isTuesday = clockInfo!!.data.isTuesday
            binding.clWeek2.tag = isTuesday
            isWednesday = clockInfo!!.data.isWednesday
            binding.clWeek3.tag = isWednesday
            isThursday = clockInfo!!.data.isThursday
            binding.clWeek4.tag = isThursday
            isFriday = clockInfo!!.data.isFriday
            binding.clWeek5.tag = isFriday
            isSaturday = clockInfo!!.data.isSaturday
            binding.clWeek6.tag = isSaturday
            isSunday = clockInfo!!.data.isSunday
            binding.clWeek7.tag = isSunday
            refWeek()

            isUnCommit = false
        }/*TODO 默认08:00
            else{
            val newDataString = TimeUtils.getNowString(com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat("HH:mm"))
            mHour = newDataString.split(":")[0].toInt()
            mMinute = newDataString.split(":")[1].toInt()
        }*/
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
                    returnClockInfo()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    //region 重复周期
    private fun refWeek() {
        binding.ivWeek1.visibility = if (binding.clWeek1.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek2.visibility = if (binding.clWeek2.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek3.visibility = if (binding.clWeek3.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek4.visibility = if (binding.clWeek4.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek5.visibility = if (binding.clWeek5.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek6.visibility = if (binding.clWeek6.tag as Boolean) View.VISIBLE else View.GONE
        binding.ivWeek7.visibility = if (binding.clWeek7.tag as Boolean) View.VISIBLE else View.GONE
    }

    private fun getWeekDay() {
        isMonday = binding.clWeek1.tag as Boolean
        isTuesday = binding.clWeek2.tag as Boolean
        isWednesday = binding.clWeek3.tag as Boolean
        isThursday = binding.clWeek4.tag as Boolean
        isFriday = binding.clWeek5.tag as Boolean
        isSaturday = binding.clWeek6.tag as Boolean
        isSunday = binding.clWeek7.tag as Boolean
        mWeekDays = BleUtils.getBinaryValue(
            false, isSunday, isSaturday, isFriday,
            isThursday, isWednesday, isTuesday, isMonday
        )
        val weakDayBuilder = StringBuilder()
        if (isMonday) weakDayBuilder.append(getString(R.string.week_easy_1))
        if (isTuesday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_2))
        }
        if (isWednesday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_3))
        }
        if (isThursday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_4))
        }
        if (isFriday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_5))
        }
        if (isSaturday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_6))
        }
        if (isSunday) {
            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
            weakDayBuilder.append(getString(R.string.week_easy_7))
        }
        binding.tvWeekDay.text = weakDayBuilder
        when (mWeekDays) {
            127 -> {  // 01111111 = 127
                binding.tvWeekDay.text = getString(R.string.everyday)
            }
            0 -> {   // 00000000
                binding.tvWeekDay.text = getString(R.string.once_only)
            }
        }
        isAlarm = true
        binding.clAlarm.visibility = View.VISIBLE
        binding.llWeek.visibility = View.GONE
        setTvTitle(if (clockInfo == null) R.string.add_alarm_clock else R.string.edit_alarm_clock)
    }
    //endregion

    //region 返回数据
    private fun returnClockInfo() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showShort(R.string.device_no_connection)
            return
        }
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            ToastUtils.showShort(R.string.event_undone_tips)
            return
        }
        if (clockInfo == null) {
            clockInfo = ClockInfoBean()
            clockInfo!!.data = ClockInfoBean.DataBean()
            clockInfo!!.data.isEnable = true //默认开启
        }
        clockInfo!!.data.clockName = binding.etName.text.toString().trim()
        clockInfo!!.data.time = SettingTimeBean(mHour, mMinute)
        clockInfo!!.data.analysisWeekDays(mWeekDays)
        //ToastUtils.showShort("返回数据")
        LogUtils.d("闹钟 ->${GsonUtils.toJson(clockInfo)}")
        setResult(Activity.RESULT_OK, intent.putExtra("data", clockInfo))
        finish()
    }
    //endregion

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isAlarm) {
                /*if (isUnCommit) {
                    showUnCommitDialog()
                }*/
                finish()
            } else {
                isAlarm = true
                binding.clAlarm.visibility = View.VISIBLE
                binding.llWeek.visibility = View.GONE
                setTvTitle(if (clockInfo == null) R.string.add_alarm_clock else R.string.edit_alarm_clock)
            }
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        when (v.id) {
            tvTitle?.id -> {
                if (isAlarm) {
                    /*if (isUnCommit) {
                        showUnCommitDialog()
                        return
                    }*/
                    finish()
                } else {
                    isAlarm = true
                    binding.clAlarm.visibility = View.VISIBLE
                    binding.llWeek.visibility = View.GONE
                    setTvTitle(if (clockInfo == null) R.string.add_alarm_clock else R.string.edit_alarm_clock)
                }
            }
            binding.clWeekDay.id -> {
                isAlarm = false
                KeyboardUtils.hideSoftInput(binding.etName)
                binding.clWeek1.tag = isMonday
                binding.clWeek2.tag = isTuesday
                binding.clWeek3.tag = isWednesday
                binding.clWeek4.tag = isThursday
                binding.clWeek5.tag = isFriday
                binding.clWeek6.tag = isSaturday
                binding.clWeek7.tag = isSunday
                refWeek()
                binding.clAlarm.visibility = View.GONE
                binding.llWeek.visibility = View.VISIBLE
                setTvTitle(R.string.alarm_cycle)

            }
            binding.clWeek1.id, binding.clWeek2.id, binding.clWeek3.id, binding.clWeek4.id,
            binding.clWeek5.id, binding.clWeek6.id, binding.clWeek7.id,
            -> {
                v.tag = !(v.tag as Boolean)
                refWeek()
            }
            binding.btnSave.id -> {
                if (isAlarm) {
                    returnClockInfo()
                } else {
                    getWeekDay()
                }
            }
        }
    }
}