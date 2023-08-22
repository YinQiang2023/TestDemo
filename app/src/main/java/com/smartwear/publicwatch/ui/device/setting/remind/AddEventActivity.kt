package com.smartwear.publicwatch.ui.device.setting.remind

import android.annotation.SuppressLint
import android.app.Activity
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.constant.TimeConstants
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.zhapp.ble.ControlBleTools
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityAddEventBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.view.wheelview.DatePicker
import com.smartwear.publicwatch.view.wheelview.TimePicker
import com.smartwear.publicwatch.view.wheelview.annotation.TimeMode
import com.smartwear.publicwatch.view.wheelview.entity.DateEntity
import com.smartwear.publicwatch.view.wheelview.entity.TimeEntity
import com.smartwear.publicwatch.view.wheelview.widget.DateWheelLayout
import com.smartwear.publicwatch.view.wheelview.widget.TimeWheelLayout
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.zhapp.ble.bean.EventInfoBean
import com.zhapp.ble.bean.TimeBean
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.ProhibitEmojiUtils
import com.smartwear.publicwatch.utils.ToastUtils
import java.util.*

/**
 * Created by Android on 2021/10/26.
 * 添加/编辑 事件
 */
class AddEventActivity : BaseActivity<ActivityAddEventBinding, DeviceModel>(
    ActivityAddEventBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private var eventInfo: EventInfoBean? = null

    //是否未提交修改 TODO 取消
    private var isUnCommit = false


    //region initView
    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.device_remind_event)/*if(eventInfo == null) R.string.add_event else R.string.edit_event*/)

        eventInfo = intent.getSerializableExtra("data") as EventInfoBean?

        binding.tvDate.text = TimeUtils.getNowString(com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd"))
        //EditTextUtils.evitTextLimit(binding.etName)
        //val filters = arrayOf<InputFilter>(ProhibitEmojiUtils.inputFilterProhibitEmoji(30))
        binding.etName.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(30)

        binding.tvTime.text = TimeUtils.date2String(Date(), com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat(com.smartwear.publicwatch.utils.TimeUtils.DATEFORMAT_HOUR_MIN))

        setViewsClickListener(this, tvTitle!!, binding.clDate, binding.clTime, binding.btnSave)

        //binding.etName.filters = arrayOf<InputFilter>(ProhibitEmojiUtils.inputFilterSpecialCharacter(20))

    }
    //endregion

    override fun initData() {
        super.initData()
        if (eventInfo != null) {
            binding.etName.setText(eventInfo!!.description)

            binding.tvDate.text = StringBuilder().append(eventInfo!!.time.year).append("-")
                .append(eventInfo!!.time.month).append("-").append(eventInfo!!.time.day)

            binding.tvTime.text = StringBuilder()
                .append(if (eventInfo!!.time.hour < 10) "0${eventInfo!!.time.hour}" else eventInfo!!.time.hour)
                .append(":")
                .append(if (eventInfo!!.time.minute < 10) "0${eventInfo!!.time.minute}" else eventInfo!!.time.minute)
//            AppUtils.tryBlock {
//                //yyyy-MM-dd HH:mm:ss
//                binding.btnSave.isEnabled = TimeUtils.getTimeSpan(
//                    "${eventInfo!!.time.year}-${eventInfo!!.time.month}-${eventInfo!!.time.day} " +
//                            "${eventInfo!!.time.hour}:${eventInfo!!.time.minute}:${eventInfo!!.time.second}",
//                    TimeUtils.getNowString(),
//                    TimeConstants.MIN) > 0
//            }
        }
    }

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
                    returnEventInfo()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    //region 时间选择
    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    private fun createTimeDialog(inHour: Int, inMinute: Int) {
        var isTody = false
        val dateString = binding.tvDate.text.trim().toString()
        AppUtils.tryBlock {
            isTody = TimeUtils.isToday(dateString, com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd"))
        }
        val calendar = Calendar.getInstance()
        val nowH = calendar.get(Calendar.HOUR_OF_DAY)
        val nowM = calendar.get(Calendar.MINUTE)

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
            binding.tvTime.text = "$resultHour:$resultMinute"
            //isUnCommit = true
            //binding.btnSave.isEnabled = isDone()
            /*if(isTody){

            }*/
        }
        val wheelLayout: TimeWheelLayout = picker.wheelLayout
        wheelLayout.setTimeMode(TimeMode.HOUR_24_NO_SECOND)
        wheelLayout.setTimeLabel(":", " ", "")
        wheelLayout.setDefaultValue(TimeEntity.target(inHour, inMinute, 0))
        wheelLayout.setCyclicEnabled(true, true)
        //如果日期是今天，不能选比现在早的时间
        /*if(isTody) {
            wheelLayout.setRange(TimeEntity.target(nowH, nowM, 0), TimeEntity.target(24, 60, 0))
            wheelLayout.setCyclicEnabled(false, false)
        }*/
        picker.show()
    }
    //endregion

    //region 日期选择
    private fun createDateDialog(y: Int, m: Int, d: Int) {
        val calendar = Calendar.getInstance()
        val nowY = calendar.get(Calendar.YEAR)
        /*val nowM = calendar.get(Calendar.MONTH) + 1
        val nowD = calendar.get(Calendar.DAY_OF_MONTH)*/

        val picker = DatePicker(this)
        val wheelLayout: DateWheelLayout = picker.wheelLayout
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setOnDatePickedListener { year, month, day ->
            var resM = if (month < 10) "0$month" else "$month"
            var resD = if (day < 10) "0$day" else "$day"
            binding.tvDate.text = "$year-$resM-$resD"
            //isUnCommit = true
        }

        wheelLayout.setDefaultValue(DateEntity.target(y, m, d))
        wheelLayout.setRange(DateEntity.target(nowY, 1, 1), DateEntity.target(nowY + 10, 12, 31))
        wheelLayout.setCyclicEnabled(false, false)
        wheelLayout.yearWheelView.isCyclicEnabled = true
        picker.show()
    }
    //endregion

    //region 返回结果
    /**
     * 保存并返回结果，是否忽略时间限制
     */
    private fun returnEventInfo(isIgnoreTime: Boolean = false) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        val desc = binding.etName.text.toString().trim()
        if (desc.isEmpty()) {
            ToastUtils.showToast(R.string.event_undone_tips)
            return
        }
        val date = binding.tvDate.text.toString().trim()
        val time = binding.tvTime.text.toString().trim()
        if (!time.contains(":")) {
            ToastUtils.showToast(R.string.event_undone_tips)
            return
        }
        //如果选择的时间，小于等于当前时间，则弹框提示用户
        val span = TimeUtils.getTimeSpan(
            TimeUtils.string2Date("$date $time", com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat(com.smartwear.publicwatch.utils.TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)),
            Date(), TimeConstants.SEC
        )
        LogUtils.d("时间差 $span ")
        if (!isIgnoreTime && span <= 0) {
            showIllegalTimeDialog()
            return
        }
        if (eventInfo == null) {
            eventInfo = EventInfoBean()
        }
        eventInfo!!.description = desc
        eventInfo!!.time = TimeBean(
            date.split("-")[0].toInt(),
            date.split("-")[1].toInt(),
            date.split("-")[2].toInt(),
            time.split(":")[0].toInt(),
            time.split(":")[1].toInt(),
            0
        )
        intent.putExtra("data", eventInfo)
        intent.putExtra("index", intent.getIntExtra("index", -1))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    /**
     * 展示非法时间弹窗
     */
    private fun showIllegalTimeDialog() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*BaseApplication.mContext.getString(R.string.apply_permission)*/null,
            getString(R.string.illegal_time_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    returnEventInfo(true)
                }

                override fun OnCancel() {}
            }
        ).show()
    }

    private fun isDone(): Boolean {
        val desc = binding.etName.text.toString().trim()
        if (desc.isEmpty()) {
            return false
        }
        val date = binding.tvDate.text.toString().trim()
        val time = binding.tvTime.text.toString().trim()
        if (!time.contains(":")) {
            return false
        }
        return true
    }
    //endregion


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        /*if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isUnCommit) {
                showUnCommitDialog()
            }
            return false
        }*/
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        when (v.id) {
            tvTitle?.id -> {
                /*if (isUnCommit) {
                    showUnCommitDialog()
                    return
                }*/
                finish()
            }
            binding.clDate.id -> {
                val array = binding.tvDate.text.trim().split("-")
                createDateDialog(array[0].toInt(), array[1].toInt(), array[2].toInt())
            }
            binding.clTime.id -> {
                if (binding.tvTime.text.toString().trim().contains(":")) {
                    val array = binding.tvTime.text.trim().split(":")
                    createTimeDialog(array[0].toInt(), array[1].toInt())
                } else {
                    createTimeDialog(0, 0)
                }
            }
            binding.btnSave.id -> {
                returnEventInfo()
            }
        }

    }
}