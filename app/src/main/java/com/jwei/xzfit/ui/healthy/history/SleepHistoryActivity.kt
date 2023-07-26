package com.jwei.xzfit.ui.healthy.history

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson.JSON
import com.google.android.material.tabs.TabLayout
import com.haibin.calendarview.CalendarView
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.SleepHistoryActivityBinding
import com.jwei.xzfit.db.model.track.BehaviorTrackingLog
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.https.response.SleepDayResponse
import com.jwei.xzfit.https.response.SleepListResponse
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean
import com.jwei.xzfit.ui.view.SleepView
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.viewmodel.DailyModel
import kotlinx.coroutines.launch
import java.util.*

class SleepHistoryActivity : BaseActivity<SleepHistoryActivityBinding, DailyModel>(SleepHistoryActivityBinding::inflate, DailyModel::class.java), View.OnClickListener {
    private val TAG: String = SleepHistoryActivity::class.java.simpleName

    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var dayList: SleepDayResponse? = null
    private var weekList: SleepListResponse? = null
    private var monthList: SleepListResponse? = null
    private var showDateType = Global.DateType.TODAY
    private var dialog: Dialog? = null
    private var currentDate = ""

    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    //行为埋点
    private var behaviorTrackingLog: BehaviorTrackingLog? = null

    enum class SleepType(val day: Int) {
        AWAKE(0), //清醒
        LIGHT_SLEEP(1), //浅睡
        DEEP_SLEEP(2), //深睡
        RAPID_EYE_MOVEMENT(3) //快速眼动
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            binding.dailySelect.lyDate.id -> {
                binding.dailySelect.tvDatePreview.text = currentDate
                rotateArrow()
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        behaviorTrackingLog = AppTrackingManager.getNewBehaviorTracking("10", "27")
        setViewsClickListener(this, binding.dailySelect.lyDate)
        binding.dailySelect.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                //选中
                when (tab.position) {
                    0 -> {
                        //日
                        showDateType = Global.DateType.TODAY
                        showView(true)
//                        showDayData()
                        getDayData()
                    }
                    1 -> {
                        showDateType = Global.DateType.WEEK
//                        if (weekList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthView(weekList)
//                        }
                        showView(false)
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    2 -> {
                        showDateType = Global.DateType.MONTH
//                        if (monthList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthView(monthList)
//                        }
                        showView(false)
                        binding.dailySelect.tvDate.text = monthDate
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                //未选中
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                //再次选中
            }
        })

        binding.calendarView.setOnCalendarSelectListener(object : CalendarView.OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: com.haibin.calendarview.Calendar) {}
            override fun onCalendarSelect(calendar: com.haibin.calendarview.Calendar, isClick: Boolean) {
                currentDate = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIME_YYYY_MM)
                binding.dailySelect.tvDatePreview.text = currentDate
                if (isClick) {
                    val date = DateUtils.FormatDateYYYYMMDD(calendar)
                    val calendarData = Calendar.getInstance()
                    calendarData.timeInMillis = DateUtils.getLongTime(date, DateUtils.TIME_YYYY_MM_DD)
                    var registerTime = DateUtils.getLongTime(SpUtils.getValue(SpUtils.REGISTER_TIME, "0"), DateUtils.TIME_YYYY_MM_DD)
                    if (calendar.timeInMillis < registerTime) {
                        ToastUtils.showToast(R.string.history_over_time_tips2)
                        return
                    }
                    if (calendarData.timeInMillis > System.currentTimeMillis()) {
                        calendarData.timeInMillis = System.currentTimeMillis()
                        ToastUtils.showToast(R.string.history_over_time_tips)
                        return
                    }
                    curDay = date
                    curWeek = curDay
                    curMonth = DateUtils.getStringDate(DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIME_YYYY_MM)
                    when (showDateType) {
                        Global.DateType.TODAY -> {
                            viewModel.getSleepDayData(curDay)
                            dismissDialog()
                            dialog?.show()
                        }
                        Global.DateType.WEEK -> {
                            getWeekOrMonthData()
                        }
                        Global.DateType.MONTH -> {
                            getWeekOrMonthData()
                        }
                    }
                    rotateArrow()
                }
            }
        })

        binding.title.tvCenterTitle.text = getString(R.string.sleep_title)
        binding.tvLeftTotalSum.text = sleepTimeSum24sp(
            getString(R.string.sleep_no_data_text_symbol_tips),
            getString(R.string.sleep_no_data_text_symbol_tips)
        )
        binding.tvRightTotalSum.text = sleepTimeSum24sp(
            getString(R.string.sleep_no_data_text_symbol_tips),
            getString(R.string.sleep_no_data_text_symbol_tips)
        )
        observe()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initData() {
        super.initData()
        startVisibleTimeTimer()


        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
        if (deviceSettingBean != null) {
            //后台未配置快速眼动
            if (!deviceSettingBean!!.settingsRelated.sleep_rapid_eye_movement_switch) {
                //表格中快速眼动
                binding.lyRemRatioTips.visibility = View.GONE
                //快速眼动占比
                binding.lyRemRatioPercent.visibility = View.GONE
            }
        }


        binding.mSleepView.setOnSlidingListener { index, time ->
            if (index == -1) return@setOnSlidingListener
            val calendar = Calendar.getInstance()
            var typeText = ""
            var tvTotalDate = ""
            var totalTimeText = ""
            var bottomTimeText = ""
            when (showDateType) {
                Global.DateType.TODAY -> {
                    if (dayList != null && !TextUtils.isEmpty(time) && index < dayList!!.sectionList.size) {
                        typeText =
                            when (dayList!!.sectionList[index].sleepDistributionType.trim()
                                ?.toInt()) {
                                SleepType.AWAKE.day -> {
                                    getString(R.string.sleep_awake_right_tips)
                                }
                                SleepType.LIGHT_SLEEP.day -> {
                                    getString(R.string.sleep_shallow_sleep_right_tips)
                                }
                                SleepType.DEEP_SLEEP.day -> {
                                    getString(R.string.sleep_deep_sleep_of_right_tips)
                                }
                                SleepType.RAPID_EYE_MOVEMENT.day -> {
                                    getString(R.string.sleep_rem_ratio_right_tips)
                                }
                                else -> {
                                    "--"
                                }
                            }
                        calendar.timeInMillis = DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD)
                        tvTotalDate = DateUtils.getStringDate(calendar.timeInMillis, "MM/dd")
                        val startTime = dayList!!.sectionList[index].startTimestamp.trim().toLong() * 1000
                        val endTime = (dayList!!.sectionList[index].startTimestamp.trim().toLong() +
                                (dayList!!.sectionList[index].sleepDuration.trim().toLong() * 60)) * 1000
                        bottomTimeText = "${DateUtils.getStringDate(startTime, "HH:mm")}-${DateUtils.getStringDate(endTime, "HH:mm")}"
                        totalTimeText = if (dayList!!.sectionList[index].sleepDuration.toInt() > 60) {
                            sleepTimeSum18sp(
                                TimeUtils.getSleepTimeH(
                                    dayList!!.sectionList[index].sleepDuration,
                                    "--"
                                ), TimeUtils.getSleepTimeM(
                                    dayList!!.sectionList[index].sleepDuration,
                                    "--"
                                )
                            ).toString()
                        } else {
                            "${dayList!!.sectionList[index].sleepDuration}${getString(R.string.minutes_text)}"
                        }
                        binding.tvTotalSumTime.text = bottomTimeText
                        binding.tvTotalLeft.text = typeText
                        binding.tvTotalDate.text = tvTotalDate
                        binding.tvTotalSum.text = totalTimeText
                        binding.lyDailyRightTips.visibility = View.VISIBLE
                    }
                }
                Global.DateType.WEEK -> {
                    if (weekList != null && !TextUtils.isEmpty(time)) {
                        typeText = getString(R.string.sleep_total_text_tips)
                        tvTotalDate = DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd")
                        val date = DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd")
                        val positions = weekList!!.dataList.indexOfFirst { it.date == date }

                        totalTimeText = if (positions != -1) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            if (weekList!!.dataList[positions].sleepDuration.toInt() > 60) {
                                sleepTimeSum18sp(
                                    TimeUtils.getSleepTimeH(
                                        weekList!!.dataList[positions].sleepDuration,
                                        "--"
                                    ), TimeUtils.getSleepTimeM(
                                        weekList!!.dataList[positions].sleepDuration,
                                        "--"
                                    )
                                ).toString()
                            } else {
                                "${weekList!!.dataList[positions].sleepDuration}${getString(R.string.minutes_text)}"
                            }
                        } else {
                            "0${getString(R.string.minutes_text)}"
                        }
                        if (positions != -1) {
                            binding.tvTotalLeft.text = typeText
                            binding.tvTotalDate.text = tvTotalDate
                            binding.tvTotalSum.text = totalTimeText
                            binding.tvTotalSumTime.text = ""
                        }
                    }
                }
                Global.DateType.MONTH -> {
                    if (monthList != null && !TextUtils.isEmpty(time)) {
                        typeText = getString(R.string.sleep_total_text_tips)
                        tvTotalDate = DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd")
                        val date = DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd")
                        val positions = monthList!!.dataList.indexOfFirst { it.date == date }

                        totalTimeText = if (positions != -1) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            if (monthList!!.dataList[positions].sleepDuration.toInt() > 60) {
                                sleepTimeSum18sp(
                                    TimeUtils.getSleepTimeH(
                                        monthList!!.dataList[positions].sleepDuration,
                                        "--"
                                    ), TimeUtils.getSleepTimeM(
                                        monthList!!.dataList[positions].sleepDuration,
                                        "--"
                                    )
                                ).toString()
                            } else {
                                "${monthList!!.dataList[positions].sleepDuration}${getString(R.string.minutes_text)}"
                            }
                        } else {
                            "0${getString(R.string.minutes_text)}"
                        }
                        if (positions != -1) {
                            binding.tvTotalLeft.text = typeText
                            binding.tvTotalDate.text = tvTotalDate
                            binding.tvTotalSum.text = totalTimeText
                            binding.tvTotalSumTime.text = ""
                        }
                    }
                }
            }

        }

        binding.mSleepView.setOnTouchListener { v: View?, event: MotionEvent ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.mSleepView.setTouchPos(eventX)
                }
//                MotionEvent.ACTION_UP -> mSleepView.setTouchPos(-1f)
            }
            binding.mSleepView.invalidate()
            true
        }

        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        showView(true)
        binding.dailySelect.tvDate.text = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
        viewModel.viewModelScope.launch {
            viewModel.queryUnUploadSleepData(true)
        }
        dialog = DialogUtils.showLoad(this)
        getDayData()
    }

    private fun observe() {
        viewModel.getSleepDayData.observe(this, androidx.lifecycle.Observer {
            dismissDialog()
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        dayList = null
                        dayList = it.data
                        showDayData()
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        dayList = null
                        showDayData()
                    }
                }
            }
        })

        viewModel.getSleepListByDateRange.observe(this, androidx.lifecycle.Observer {
            dismissDialog()
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        var temp = weekList
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                weekList = it.data
                                temp = weekList
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                monthList = it.data
                                temp = monthList
                            }
                            else -> {}
                        }
                        showWeekOrMonthView(temp)
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        var temp = weekList
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                temp = weekList
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                temp = monthList
                            }
                            else -> {}
                        }
                        showWeekOrMonthView(temp)
                    }
                }
            }
        })
    }

    private fun sleepTimeSum24sp(hour: String, min: String): SpannableStringBuilder {
        return SpannableStringTool.get()
            .append(hour)
            .setFontSize(24f)
            .append(getString(R.string.hours_text))
            .setFontSize(16f)
            .append(min)
            .setFontSize(24f)
            .append(getString(R.string.minutes_text))
            .setFontSize(16f)
            .create()
    }

    fun sleepTimeSum18sp(hour: String, min: String): SpannableStringBuilder {
        return SpannableStringTool.get()
            .append(hour)
            .setFontSize(18f)
            .append(getString(R.string.hours_text))
            .setFontSize(12f)
            .append(min)
            .setFontSize(18f)
            .append(getString(R.string.minutes_text))
            .setFontSize(12f)
            .create()
    }

    private fun rotateArrow() {
        var degree = 0
        if (binding.dailySelect.ivDateArrow.tag == null || binding.dailySelect.ivDateArrow.tag == true) {
            binding.dailySelect.ivDateArrow.tag = false
            degree = -180
            binding.calendarView.visibility = View.VISIBLE
            binding.dailySelect.tvDatePreview.visibility = View.VISIBLE
            binding.dailySelect.tvDate.visibility = View.GONE
        } else {
            degree = 0
            binding.dailySelect.ivDateArrow.tag = true
            binding.calendarView.visibility = View.GONE
            binding.dailySelect.tvDatePreview.visibility = View.GONE
            binding.dailySelect.tvDate.visibility = View.VISIBLE
        }
        binding.dailySelect.ivDateArrow.animate().setDuration(350).rotation(degree.toFloat())
    }

    private fun showView(isDay: Boolean) {
        if (isDay) {
            binding.lyDaySleepData.visibility = View.VISIBLE
            binding.lyWeekOrMonthSleepData.visibility = View.GONE
        } else {
            binding.lyDaySleepData.visibility = View.GONE
            binding.lyWeekOrMonthSleepData.visibility = View.VISIBLE
        }
    }

    private fun getDayData() {
        dismissDialog()
        viewModel.getSleepDayData(curDay)
        dialog?.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showDayData() {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        binding.dailySelect.tvDate.text = DateUtils.getStringDate(DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
        if (dayList != null) {
            val timeSleepTotal = dayList!!.sleepDuration.trim().toInt()
            binding.tvSleepTotalSum.text =
                sleepTimeSum18sp((timeSleepTotal / 60).toString(), (timeSleepTotal % 60).toString())
            val progress1 = dayList!!.deepSleepTime.trim().toInt()
            val progress2 = dayList!!.lightSleepTime.trim().toInt()
            val progress3 = dayList!!.awakeTime.trim().toInt()
            val progress4 = dayList!!.rapidEyeMovementTime.trim().toInt()
            binding.blockProgress.start(progress1, progress2, progress3, progress4)
            val timeDeepSleep = dayList!!.deepSleepTime.trim().toInt()
            binding.tvDeepSleepTime.text =
                sleepTimeSum18sp((timeDeepSleep / 60).toString(), (timeDeepSleep % 60).toString())
            binding.tvDeepSleepPercent.text = "${(dayList!!.deepSleepTimePercentage.trim().toFloat()).toInt()}${getString(R.string.healthy_sports_item_percent_sign)}"
            val timeShallowSleep = dayList!!.lightSleepTime.trim().toInt()
            binding.tvShallowSleepTime.text = sleepTimeSum18sp(
                (timeShallowSleep / 60).toString(),
                (timeShallowSleep % 60).toString()
            )
            binding.tvShallowSleepPercent.text = "${(dayList!!.lightSleepTimePercentage.trim().toFloat()).toInt()}${getString(R.string.healthy_sports_item_percent_sign)}"
            val timeAwake = dayList!!.awakeTime.trim().toInt()
            binding.tvAwakeTime.text =
                sleepTimeSum18sp((timeAwake / 60).toString(), (timeAwake % 60).toString())
            binding.tvAwakePercent.text = "${(dayList!!.awakeTimePercentage.trim().toFloat()).toInt()}${getString(R.string.healthy_sports_item_percent_sign)}"
            val timeRemRatio = dayList!!.rapidEyeMovementTime.trim().toInt()
            binding.tvRemRatioTime.text =
                sleepTimeSum18sp((timeRemRatio / 60).toString(), (timeRemRatio % 60).toString())
            binding.tvRemRatioPercent.text =
                "${(dayList!!.rapidEyeMovementTimePercentage.trim().toFloat()).toInt()}${getString(R.string.healthy_sports_item_percent_sign)}"

            //显示睡眠分数
//            binding.layoutScore.visibility = View.VISIBLE
            binding.tvSleepScore.text = dayList!!.sleepScore
            val sleepQuality = dayList!!.sleepScore.trim().toInt()
            binding.tvSleepQuality.text = when {
                sleepQuality <= 60 -> {
                    getString(R.string.sleep_quality_poor_text_tips)
                }
                sleepQuality in 61..80 -> {
                    getString(R.string.sleep_quality_general_text_tips)
                }
                else -> {
                    getString(R.string.sleep_quality_high_text_tips)
                }
            }

            val progressSleepTime = IntArray(dayList!!.sectionList.size)
            val progressSleepType = IntArray(dayList!!.sectionList.size)
            for (i in dayList!!.sectionList.indices) {
                progressSleepTime[i] = dayList!!.sectionList[i].sleepDuration.trim().toInt()
                progressSleepType[i] =
                    when (dayList!!.sectionList[i].sleepDistributionType.trim().toInt()) {
                        SleepType.AWAKE.day -> {
                            3
                        }
                        SleepType.LIGHT_SLEEP.day -> {
                            2
                        }
                        SleepType.DEEP_SLEEP.day -> {
                            1
                        }
                        SleepType.RAPID_EYE_MOVEMENT.day -> {
                            4
                        }
                        else -> 0
                    }
            }
            var startTime = (dayList!!.startSleepTimestamp.trim().toLong() * 1000).toString()
            var endTime = (dayList!!.endSleepTimestamp.trim().toLong() * 1000).toString()
            binding.mSleepView.setProgress(progressSleepTime, progressSleepType, startTime, endTime)
        } else {
            binding.layoutScore.visibility = View.GONE
            binding.lyDailyRightTips.visibility = View.GONE
            binding.tvSleepTotalSum.text = sleepTimeSum18sp("--", "--")
            binding.blockProgress.clean()
            binding.blockProgress.background =
                ContextCompat.getDrawable(this, R.drawable.block_progress_no_data)
            binding.tvDeepSleepTime.text = sleepTimeSum18sp("--", "--")
            binding.tvDeepSleepPercent.text = getString(R.string.sleep_no_data_text_percent_tips)
            binding.tvShallowSleepTime.text = sleepTimeSum18sp("--", "--")
            binding.tvShallowSleepPercent.text = getString(R.string.sleep_no_data_text_percent_tips)
            binding.tvAwakeTime.text = sleepTimeSum18sp("--", "--")
            binding.tvAwakePercent.text = getString(R.string.sleep_no_data_text_percent_tips)
            binding.tvRemRatioTime.text = sleepTimeSum18sp("--", "--")
            binding.tvRemRatioPercent.text = getString(R.string.sleep_no_data_text_percent_tips)
            binding.mSleepView.setProgress(IntArray(0), IntArray(0), "--", "--")
        }
    }

    var weekDate = ""
    var monthDate = ""
    private fun getWeekOrMonthData() {
        showView(false)
        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var type = 1
        var selectDate = ""
        when (showDateType) {
            Global.DateType.WEEK -> {
                beginDay = DateUtils.getDayOfWeekMonday(curWeek)
                calendar.timeInMillis = DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                endDay = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIME_YYYY_MM_DD)
                type = Global.REQUEST_DATA_TYPE_WEEK
                val beginTemp = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                val endTemp = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                selectDate = "$beginTemp-$endTemp"
                weekDate = selectDate
            }
            Global.DateType.MONTH -> {
                beginDay = "$curMonth-01"
                endDay = DateUtils.getDayOfMonthEnd(beginDay)
                type = Global.REQUEST_DATA_TYPE_MONTH
                val beginTemp = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMM_SLASH_RAIL)
                selectDate = "$beginTemp"
                monthDate = selectDate
            }
            else -> {}
        }

        binding.dailySelect.tvDate.text = selectDate
        viewModel.getSleepListByDateRange(beginDay, endDay)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthView(data: SleepListResponse?) {

        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var length = 0
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        when (showDateType) {
            Global.DateType.WEEK -> {
                beginDay = DateUtils.getDayOfWeekMonday(curWeek)
                calendar.timeInMillis =
                    DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                endDay = DateUtils.getStringDate(
                    calendar.timeInMillis,
                    DateUtils.TIME_YYYY_MM_DD
                )
                length = 7
            }
            Global.DateType.MONTH -> {
                beginDay = "$curMonth-01"
                endDay = DateUtils.getDayOfMonthEnd(beginDay)
                calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    calendar.timeInMillis = System.currentTimeMillis();
                }
                length = endDay.substring(8).toInt() - beginDay.substring(8).toInt() + 1
            }
            else -> {}
        }

        val progressTime = arrayOfNulls<String>(length)
        val sleepInfo = mutableListOf<SleepView.SleepInfo>()

        for (i in 0 until length) {
            progressTime[i] = DateUtils.getNextDay(beginDay, i).toString()
            calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                continue
            }
        }

        if (data != null) {
            val totalSleepDuration = data!!.totalSleepDuration.trim().toInt()
            binding.tvLeftTotalSum.text = sleepTimeSum24sp(
                (totalSleepDuration / 60).toString(),
                (totalSleepDuration % 60).toString()
            )
            val avgSleepDuration = data!!.avgSleepDuration.trim().toInt()
            binding.tvRightTotalSum.text = sleepTimeSum24sp(
                (avgSleepDuration / 60).toString(),
                (avgSleepDuration % 60).toString()
            )

            for (i in 0 until length) {
                val curShowDay = DateUtils.getStringDate(progressTime[i]!!.toLong(), DateUtils.TIME_YYYY_MM_DD)
                var index: Int = -1

                for (m in data.dataList.indices) {
                    if (curShowDay.equals(data.dataList[m].date)) {
                        index = m
                        break
                    }
                }

                if (index != -1) {
                    val info = SleepView.SleepInfo()
                    info.sleepTime1 = data.dataList[index].deepSleepTime.trim().toInt()
                    info.sleepTime2 = data.dataList[index].lightSleepTime.trim().toInt()
                    info.sleepTime3 = data.dataList[index].awakeTime.trim().toInt()
                    info.sleepTime4 = data.dataList[index].rapidEyeMovementTime.trim().toInt()
                    sleepInfo.add(info)
                } else {
                    val info = SleepView.SleepInfo()
                    info.sleepTime1 = 0
                    info.sleepTime2 = 0
                    info.sleepTime3 = 0
                    info.sleepTime4 = 0
                    sleepInfo.add(info)
                }
            }
        } else {
            binding.lyDailyRightTips.visibility = View.GONE
            binding.tvLeftTotalSum.text = sleepTimeSum24sp(
                getString(R.string.sleep_no_data_text_symbol_tips),
                getString(R.string.sleep_no_data_text_symbol_tips)
            )
            binding.tvRightTotalSum.text = sleepTimeSum24sp(
                getString(R.string.sleep_no_data_text_symbol_tips),
                getString(R.string.sleep_no_data_text_symbol_tips)
            )
        }
        binding.mSleepView.setProgress(sleepInfo, progressTime, 2)
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.blankj.utilcode.util.LogUtils.d("visibleTime:$visibleTime")
        if (visibleTime >= 9999) {
            visibleTime = 9999
        }
        behaviorTrackingLog?.let {
            it.functionStatus = "1"
            it.durationSec = visibleTime.toString()
            AppTrackingManager.saveBehaviorTracking(it)
        }
    }
}