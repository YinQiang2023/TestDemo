package com.jwei.publicone.ui.healthy.history.womenhealth

import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.haibin.calendarview.CalendarView
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityWomenHealthBinding
import com.jwei.publicone.utils.SpUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.view.wheelview.NumberPicker
import com.jwei.publicone.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.db.model.track.BehaviorTrackingLog
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.utils.DateUtils
import com.jwei.publicone.utils.manager.AppTrackingManager

class WomenHealthActivity : BaseActivity<ActivityWomenHealthBinding, DeviceModel>(ActivityWomenHealthBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    var TODAY_COLOCR = -0x10000

    //行为埋点
    private var behaviorTrackingLog: BehaviorTrackingLog? = null

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.title.layoutRight.id -> {
                startActivity(
                    Intent(this, WomenPeriodSettingActivity::class.java)
                        .putExtra("type", 0)
                )
            }
            binding.lyCycleShow.id -> {
                startActivity(Intent(this, WomenCycleExplainActivity::class.java))
            }
            binding.tvBackToday.id -> {
                binding.calendarView.scrollToCurrent()
            }
            binding.tvOpenHealthCheck.id -> {
                SpUtils.getSPUtilsInstance().put(SpUtils.WOMEN_HEALTH_OPEN_HEALTH_CHECK, true)
                isShowMain()
            }
            binding.layoutCycleShowDate.id -> {
                showCycleDateDialog()
            }
            binding.chbCycleButton.id -> {
                val isSelect = Global.physiologicalCycleBean?.remindSwitch!!
                Global.physiologicalCycleBean?.remindSwitch = !isSelect
                onSelectCycleButton()
                sendCmd()
                isShowCycleDate()
            }
        }
    }

    private fun onSelectCycleButton() {
        if (Global.physiologicalCycleBean?.remindSwitch!!) {
            binding.chbCycleButton.setImageResource(R.mipmap.women_health_switch_on)
        } else {
            binding.chbCycleButton.setImageResource(R.mipmap.women_health_switch_off)
        }
    }

    override fun initView() {
        super.initView()
        binding.title.tvCenterTitle.text = getString(R.string.women_health_title)
        behaviorTrackingLog = AppTrackingManager.getNewBehaviorTracking("10","31")
        onSelectCycleButton()
        binding.tvCycleShowDate.text = if (Global.physiologicalCycleBean?.advanceDay == 0) {
            "1"
        } else {
            "${Global.physiologicalCycleBean?.advanceDay}"
        }
        isShowCycleDate()

        binding.tvWomenTopDate.text = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIMEYYYYMM_SLASH)
        setViewsClickListener(
            this, binding.title.layoutRight, binding.tvOpenHealthCheck, binding.lyCycleShow,
            binding.tvBackToday, binding.layoutCycleShowDate, binding.chbCycleButton
        )
        CalcCycleDataUtils.loadCycleData()
    }

    override fun initData() {
        super.initData()
        startVisibleTimeTimer()
        CalcCycleDataUtils.loadCycleData()
        isShowMain()
        binding.calendarView.setSchemeDate(
            CalcCycleDataUtils.getCycData(
                binding.calendarView.curYear,
                binding.calendarView.curMonth,
                binding.calendarView.curDay
            )
        )

        binding.calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: com.haibin.calendarview.Calendar) {
            }

            override fun onCalendarSelect(calendar: com.haibin.calendarview.Calendar, isClick: Boolean) {
                binding.tvWomenTopDate.text = DateUtils.FormatDateYYYYMMDD(calendar, DateUtils.TIMEYYYYMM_SLASH)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        CalcCycleDataUtils.loadCycleData()
        binding.calendarView.setSchemeDate(
            CalcCycleDataUtils.getCycData(
                binding.calendarView.curYear,
                binding.calendarView.curMonth,
                binding.calendarView.curDay
            )
        )

        val womenHealthPosition = Global.healthyItemList.indexOfFirst {
            it.topTitleText == BaseApplication.mContext.getString(
                R.string.healthy_sports_list_women_health
            )
        }
        if (womenHealthPosition != -1 && Global.physiologicalCycleBean != null) {
            binding.tvCycle.text = Global.healthyItemList[womenHealthPosition].context
        }
    }

    private fun isShowMain() {
//        if (!SpUtils.getSPUtilsInstance()
//                .getBoolean(SpUtils.WOMEN_HEALTH_OPEN_HEALTH_CHECK, false)
//        ) {
//            binding.title.layoutRight.visibility = View.GONE
//            binding.lyFirstIn.visibility = View.VISIBLE
//            binding.lyMain.visibility = View.GONE
//        } else {
        binding.title.tvRIght.text = getString(R.string.women_health_title_right_text)
        binding.title.tvRIght.visibility = View.VISIBLE
        binding.title.layoutRight.visibility = View.VISIBLE
        binding.lyFirstIn.visibility = View.GONE
        binding.lyMain.visibility = View.VISIBLE
//        }
    }

    private fun isShowCycleDate() {
        if (Global.physiologicalCycleBean?.remindSwitch!!) {
            binding.lyCycleShowDate.visibility = View.VISIBLE
        } else {
            binding.lyCycleShowDate.visibility = View.GONE
        }
    }

    private fun showCycleDateDialog() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            binding.tvCycleShowDate.text = item.toString()
            Global.physiologicalCycleBean?.advanceDay = binding.tvCycleShowDate.text.toString().trim().toInt()
            sendCmd()
        }
        picker.setRange(1, 5, 1)
        if (!TextUtils.isEmpty(binding.tvCycleShowDate.text.toString().trim())) {
            picker.setDefaultValue(binding.tvCycleShowDate.text.toString().trim().toInt())
        } else {
            picker.setDefaultValue(1)
        }

        picker.wheelLayout.setCyclicEnabled(true)
//        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
//        picker.okView.setTextColor(ContextCompat.getColor(this, R.color.color_E96192))
        picker.show()
    }

    private fun sendCmd() {
        val dialog = DialogUtils.showLoad(this)
        dialog.show()
        LogUtils.d("set PhysiologicalCycleBean -->" + Global.physiologicalCycleBean)
        ControlBleTools.getInstance().setPhysiologicalCycle(Global.physiologicalCycleBean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                dialog.dismiss()
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        com.blankj.utilcode.util.LogUtils.d("visibleTime:$visibleTime")
        if(visibleTime>=9999){
            visibleTime = 9999
        }
        behaviorTrackingLog?.let {
            it.functionStatus = "1"
            it.durationSec = visibleTime.toString()
            AppTrackingManager.saveBehaviorTracking(it)
        }
    }

}