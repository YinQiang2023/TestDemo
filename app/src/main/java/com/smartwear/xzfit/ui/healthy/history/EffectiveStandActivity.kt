package com.smartwear.xzfit.ui.healthy.history

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.haibin.calendarview.CalendarView
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityEffectiveStandBinding
import com.smartwear.xzfit.db.model.track.BehaviorTrackingLog
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.https.response.EffectiveStandListResponse
import com.smartwear.xzfit.https.response.EffectiveStandResponse
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.view.EffectiveStandView
import com.smartwear.xzfit.utils.DateUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.SpannableStringTool
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import com.smartwear.xzfit.viewmodel.DailyModel
import kotlinx.coroutines.launch
import java.util.*

class EffectiveStandActivity : BaseActivity<ActivityEffectiveStandBinding, DailyModel>(ActivityEffectiveStandBinding::inflate, DailyModel::class.java),
    View.OnClickListener {
    private val TAG: String = EffectiveStandActivity::class.java.simpleName

    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var dayList: EffectiveStandResponse? = null
    private var weekList: EffectiveStandListResponse? = null
    private var monthList: EffectiveStandListResponse? = null
    private var showDateType = Global.DateType.TODAY
    private var dialog: Dialog? = null
    private var currentDate = ""

    //行为埋点
    private var behaviorTrackingLog: BehaviorTrackingLog? = null

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
        binding.title.tvCenterTitle.text = getString(R.string.effective_stand_title)
        behaviorTrackingLog = AppTrackingManager.getNewBehaviorTracking("10", "30")
        setViewsClickListener(this, binding.dailySelect.lyDate)
        binding.dailySelect.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                //选中
                when (tab.position) {
                    0 -> {
                        //日
                        showDateType = Global.DateType.TODAY
//                        showDayData()
                        getDayData()
                    }
                    1 -> {
                        showDateType = Global.DateType.WEEK
//                        if (weekList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(weekList , EffectiveStandView.WEEK)
//                        }
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    2 -> {
                        showDateType = Global.DateType.MONTH
//                        if (monthList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(monthList , EffectiveStandView.MONTH)
//                        }
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
                    rotateArrow()
                    when (showDateType) {
                        Global.DateType.TODAY -> {
                            getDayData()
                            binding.dailySelect.tvDate.text = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                        }
                        Global.DateType.WEEK -> {
                            getWeekOrMonthData()
                        }
                        Global.DateType.MONTH -> {
                            getWeekOrMonthData()
                        }
                    }
                }
            }
        })

        observe()


//        val progressValue = FloatArray(24)
//        val progressTime = arrayOfNulls<String>(24)
//        for (i in 0 until 24) {
//            if (i == 5 || i == 8){
//                progressValue[i] = 0f
//            }else{
//                progressValue[i] = 10f
//            }
//
//            if (i == 18){
//                progressValue[i] = 30f
//            }
//
//            progressTime[i] = i.toString()
//        }
//        binding.effectiveStandView.setProgress(progressValue, progressTime , EffectiveStandView.TODAY)

//        val progressValue = FloatArray(7)
//        val progressTime = arrayOfNulls<String>(7)
//        for (i in 0 until 7) {
//            progressValue[i] = i * 5f
//            progressValue[0] = 100f
//            progressValue[4] = 0f
//        }
//        progressTime[0] = "1632301207313"
//        progressTime[1] = "1632301207313"
//        progressTime[2] = "1632301207313"
//        progressTime[3] = "1632301207313"
//        progressTime[4] = "1632301207313"
//        progressTime[5] = "1632301207313"
//        progressTime[6] = "1632301207313"
//        showDateType = Global.DateType.WEEK

//        val progressValue = FloatArray(30)
//        val progressTime = arrayOfNulls<String>(30)
//        for (i in 0 until 30) {
//            progressValue[i] = i * 5f
//            progressTime[i] = "07-" + String.format(Locale.ENGLISH,"%02d", (i + 1))
//
//            progressValue[0] = 100f
//        }
////
//        showDateType = Global.DateType.MONTH
//        binding.effectiveStandView.setProgress(progressValue, progressTime, EffectiveStandView.WEEK)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initData() {
        super.initData()
        startVisibleTimeTimer()
        binding.effectiveStandView.setOnSlidingListener { data, index, time, step ->
            Log.e(TAG, "${data}" + "  " + index + " " + time + " step=" + step)
            if (index != -1) {
                when (showDateType) {
                    Global.DateType.TODAY -> {
                        //返回错误 eg:1682524800000
                        if (time.length >= 13) return@setOnSlidingListener
                        var timeTemp = time
                        var timeNext = "${time.toInt() + 1}"
                        if (timeTemp.toInt() < 10) {
                            timeTemp = "0$timeTemp"
                            timeNext = if (timeNext.trim().toInt() < 10) {
                                "0$timeNext"
                            } else {
                                "$timeNext"
                            }
                        }
                        if (timeTemp.toInt() == 23) {
                            timeNext = "00"
                        }
                        if (step > 0) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            showLyRightTips("${step.toInt()}", null, "$timeTemp:00-$timeNext:00")
                        }
                    }
                    Global.DateType.WEEK, Global.DateType.MONTH -> {
                        var temp = weekList
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                temp = weekList
                            }
                            Global.DateType.MONTH -> {
                                temp = monthList
                            }
                            else -> {}
                        }
                        val dataTemp = step.toInt().toString()
                        if (step > 0) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            showLyRightTips(dataTemp, DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd"), "")
                        }
                    }
                }
            }
        }

        binding.effectiveStandView.setOnTouchListener { v, event ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN/*, MotionEvent.ACTION_MOVE*/ -> {
                    binding.effectiveStandView.setTouchPos(
                        eventX
                    )
                }
//                MotionEvent.ACTION_UP -> binding.effectiveStandView.setTouchPos(-1f)
            }
            binding.effectiveStandView.invalidate()
            true
        }

        dialog = DialogUtils.showLoad(this)
        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        viewModel.viewModelScope.launch {
            viewModel.queryUnUploadEffectiveStand(true)
        }
        getDayData()
        showDayData()
//        showDateType = Global.DateType.WEEK
//        binding.effectiveStandView.testData()
    }

    private fun observe() {
        viewModel.getEffectiveStandDataByDay.observe(this, androidx.lifecycle.Observer {
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
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        dayList = null
                        showDayData()
                    }
                }
            }
        })

        viewModel.getEffectiveStandListByDateRange.observe(this, androidx.lifecycle.Observer {
            dismissDialog()
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        var temp = weekList
                        var type = EffectiveStandView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                weekList = it.data
                                temp = weekList
                                type = EffectiveStandView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                monthList = it.data
                                temp = monthList
                                type = EffectiveStandView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        var temp = weekList
                        var type = EffectiveStandView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                temp = weekList
                                type = EffectiveStandView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                temp = monthList
                                type = EffectiveStandView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        var temp: Nothing? = null
                        var type = EffectiveStandView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                type = EffectiveStandView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                type = EffectiveStandView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                }
            }
        })
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

    private fun showLyRightTips(data: String, date: String? = null, time: String) {
        binding.tvTotalSum.text = data
        if (date != null) {
            binding.tvTotalDate.text = date
        }
        binding.tvTotalSumTime.text = time
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }

    private fun showTotal(tvDistance: String, unit: String) {
        if (tvDistance.isNotEmpty() && tvDistance != "0") {
            Log.i(TAG, "showTotal tvDistance = " + tvDistance)
            binding.tvDistance.text = SpannableStringTool.get()
                .append(tvDistance)
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .create()
        } else {
            binding.tvDistance.text = SpannableStringTool.get()
                .append("--")
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .create()
        }
    }

    private fun getDayData() {
        dismissDialog()
        viewModel.getEffectiveStandDataByDay(curDay)
        dialog?.show()
    }

    private fun showDayData() {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        binding.dailySelect.tvDate.text = DateUtils.getStringDate(DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var showDefaultTimeTips = ""
        var list: List<String> = emptyList()
        val progressValue = FloatArray(24)
        val progressTime = arrayOfNulls<String>(24)
        var sum = "--"

        showDefaultTimeTips = "00:00"
        val temp = curDay.trim().split("-")
        showDefaultDateTips = "${temp[1]}-${temp[2]}"

        if (dayList != null) {
            list = dayList?.effectiveStandingData?.split(",")!!
            if (list.isNotEmpty()) {
                sum = "0"
                for (i in 0 until 24) {
                    if (list.isNotEmpty() && i < list.size) {
                        progressValue[i] = list[i].toFloat()
                    } else {
                        progressValue[i] = 0f
                    }
                    sum = (sum.toInt() + list[i].trim().toInt()).toString()
                }
            }
        } else {
//            binding.lyDailyRightTips.visibility = View.GONE
            for (i in 0 until 24) {
                progressValue[i] = 0f
            }
        }

        showLyRightTips(showDefaultDataTips, showDefaultDateTips, showDefaultTimeTips)
        showTotal(sum, getString(R.string.effective_stand_total_sum_unit))

        for (i in 0 until 24) {
            progressTime[i] = i.toString()
        }

        binding.effectiveStandView.setProgress(progressValue, progressTime, EffectiveStandView.TODAY)
    }

    var weekDate = ""
    var monthDate = ""
    private fun getWeekOrMonthData() {
        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var selectDate = ""
        when (showDateType) {
            Global.DateType.WEEK -> {
                beginDay = DateUtils.getDayOfWeekMonday(curWeek)
                calendar.timeInMillis = DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                endDay = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIME_YYYY_MM_DD)
                val beginTemp = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                val endTemp = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                selectDate = "$beginTemp-$endTemp"
                weekDate = selectDate
            }
            Global.DateType.MONTH -> {
                beginDay = "$curMonth-01"
                endDay = DateUtils.getDayOfMonthEnd(beginDay)
                val beginTemp = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMM_SLASH_RAIL)
                selectDate = "$beginTemp"
                monthDate = selectDate
            }
            else -> {}
        }

        binding.dailySelect.tvDate.text = selectDate
        viewModel.getEffectiveStandListByDateRange(beginDay, endDay)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthData(data: EffectiveStandListResponse?, typeHistogram: Int) {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var length = 0
        var sum = "--"
        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var list: List<String> = emptyList()

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

        val progressValue = FloatArray(length)
        val progressTime = arrayOfNulls<String>(length)

        for (i in 0 until length) {
            progressTime[i] = DateUtils.getNextDay(beginDay, i).toString()
            calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                continue
            }
        }

        if (data != null && data.dataList.isNotEmpty()) {
            sum = data.totalTimes
            showDefaultDataTips = data.dataList[0].effectiveStandingTimes
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
                    progressValue[i] = data.dataList[index].effectiveStandingTimes.trim().toFloat()
                } else {
                    progressValue[i] = 0f
                }
            }

        } else {
            binding.lyDailyRightTips.visibility = View.GONE
        }

        showDefaultDateTips = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), "yyyy/MM/dd")
        showLyRightTips(showDefaultDataTips, showDefaultDateTips, "")
        showTotal(sum, getString(R.string.effective_stand_total_sum_unit))

        binding.effectiveStandView.setProgress(progressValue, progressTime, typeHistogram)
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