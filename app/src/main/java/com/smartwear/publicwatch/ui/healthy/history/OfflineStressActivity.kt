package com.smartwear.publicwatch.ui.healthy.history

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.haibin.calendarview.CalendarView
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityOfflineStressBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.response.SinglePressureListResponse
import com.smartwear.publicwatch.https.response.SinglePressureResponse
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.view.BloodOxygenView
import com.smartwear.publicwatch.ui.view.OfflineStressView
import com.smartwear.publicwatch.utils.DateUtils
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.SpannableStringTool
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.DailyModel
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by Android on 2023/2/6.
 */
class OfflineStressActivity : BaseActivity<ActivityOfflineStressBinding, DailyModel>(ActivityOfflineStressBinding::inflate, DailyModel::class.java), View.OnClickListener {
    private val TAG: String = OfflineStressActivity::class.java.simpleName
    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var dayList: SinglePressureResponse? = null
    private var weekList: SinglePressureListResponse? = null
    private var monthList: SinglePressureListResponse? = null
    private var showDateType = Global.DateType.TODAY
    private var dialog: Dialog? = null
    private var currentDate = ""

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
        binding.title.tvCenterTitle.text = getString(R.string.healthy_pressure_title)

        setViewsClickListener(this, binding.dailySelect.lyDate)
        binding.dailySelect.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                //选中
                when (tab.position) {
                    0 -> {
                        //日
                        binding.tvTodayPreview.text = getString(R.string.heart_rate_today_preview)
                        showDateType = Global.DateType.TODAY
//                        showDayData()
                        getDayData()
                    }
                    1 -> {
                        binding.tvTodayPreview.text = getString(R.string.heart_rate_this_week_preview)
                        showDateType = Global.DateType.WEEK
                        getWeekOrMonthData()
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    2 -> {
                        binding.tvTodayPreview.text = getString(R.string.heart_rate_this_month_preview)
                        showDateType = Global.DateType.MONTH
                        getWeekOrMonthData()
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
    }

    private fun observe() {
        viewModel.getSinglePressureDataByDay.observe(this, androidx.lifecycle.Observer {
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

        viewModel.getSinglePressureListByDateRange.observe(this, androidx.lifecycle.Observer {
            dismissDialog()
            if (it != null) {
                var type = BloodOxygenView.WEEK
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
                                type = BloodOxygenView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                monthList = it.data
                                temp = monthList
                                type = BloodOxygenView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        var temp = weekList
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                temp = weekList
                                type = BloodOxygenView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                temp = monthList
                                type = BloodOxygenView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        var temp: Nothing? = null
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                type = BloodOxygenView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                type = BloodOxygenView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                }
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initData() {
        super.initData()

        binding.mStressView.setOnSlidingListener { data, index, time, step, max, min ->
            Log.i(TAG, "${data}" + "  " + index + " " + time + " step=" + step)

            if (index != -1) {
                when (showDateType) {
                    Global.DateType.TODAY -> {
                        //日视图使用max!=-1记录真实选中
                        if (time != null && max != -1f) {
                            var timeTemp = DateUtils.getStringDate(DateUtils.getLongTime(time, DateUtils.TIME_YYYY_MM_DD_HHMMSS), "HH:mm")
                            if (step.toInt() == 0) {
                                binding.lyDailyRightTips.visibility = View.GONE
                                showLyRightTips("--", null, timeTemp)
                            } else {
                                binding.lyDailyRightTips.visibility = View.VISIBLE
                                showLyRightTips("${step.toInt()}", null, timeTemp)
                            }
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
                        val dataTemp = if (min > 0f && max > 0f) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            "${min.toInt()}~${max.toInt()}"
                        } else {
                            "--"
                        }
                        if (dataTemp != "--")
                            showLyRightTips(dataTemp, DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd"), "")
                    }
                }
            }
        }

        binding.mStressView.setOnTouchListener { v: View?, event: MotionEvent ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.mStressView.setTouchPos(eventX)
                }
            }
            binding.mStressView.invalidate()
            true
        }

        dialog = DialogUtils.showLoad(this)
        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        viewModel.viewModelScope.launch {
            viewModel.queryUnUploadSinglePressure(true)
        }
        getDayData()
        showDayData()
//        showDateType = Global.DateType.WEEK
//        binding.mBloodOxygenView.testData()
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

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }

    private fun showLyRightTips(data: String, date: String? = null, time: String) {
        binding.tvTotalSum.text = data
        if (date != null) {
            binding.tvTotalDate.text = date
        }
        binding.tvTotalSumTime.text = time
        when (showDateType) {
            Global.DateType.TODAY -> {
                binding.tvTotalLeft.text = getString(R.string.blood_oxy_ly_total_left_text)
            }
            else -> {
                binding.tvTotalLeft.text = getString(R.string.range_tips)
            }
        }
    }

    private fun showTodayPreviewData(max: String, min: String, avg: String) {
        binding.tvMaximumBloodOxygen.text = max
        binding.tvMinBloodOxygen.text = min
        binding.tvAverageBloodOxygen.text = avg
    }

    private fun showTotal(tvDistance: String, unit: String) {
        if (tvDistance.isNotEmpty()) {
            binding.tvDistance.text = SpannableStringTool.get()
                .append(tvDistance)
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF))
                .append(" ")
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF_70))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF_70))
                .create()
        } else {
            binding.tvDistance.text = SpannableStringTool.get()
                .append("--")
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF))
                .append(" ")
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF_70))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_FFFFFF_70))
                .create()
        }
    }

    private fun getDayData() {
        dismissDialog()
        viewModel.getSinglePressureDataByDay(curDay)
        dialog?.show()
    }

    private fun showDayData() {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        binding.dailySelect.tvDate.text = DateUtils.getStringDate(DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var showDefaultTimeTips = ""
        var max = "--"
        var min = "--"
        var avg = "--"
        var list: List<String> = emptyList()
        val progressValue = FloatArray(24)
        val progressTime = arrayOfNulls<String>(24)
        var totalDataUnit = getString(R.string.healthy_sports_item_percent_sign)

        showDefaultTimeTips = "00:00"
        val temp = curDay.trim().split("-")
        showDefaultDateTips = "${temp[1]}-${temp[2]}"
        val bloodOxygenInfo = mutableListOf<OfflineStressView.DayInfo>()

        if (dayList != null && dayList?.dataList!!.isNotEmpty()) {
            max = if (dayList?.maxPressure?.trim().toString() == "" || dayList?.maxPressure?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.maxPressure.toString()
            }
            min = if (dayList?.minPressure?.trim().toString() == "" || dayList?.minPressure?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.minPressure.toString()
            }
            avg = if (dayList?.avgPressure?.trim().toString() == "" || dayList?.avgPressure?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.avgPressure.toString()
            }
            if (dayList?.dataList != null) {
                for (i in dayList?.dataList!!.indices) {
                    val bean = OfflineStressView.DayInfo()
                    bean.progressValue = dayList?.dataList!![i].measureData.trim().toFloat()
                    bean.time = dayList?.dataList!![i].measureTime
                    bloodOxygenInfo.add(bean)
                }
            }
        } else {
            binding.lyDailyRightTips.visibility = View.GONE
        }

        showLyRightTips(showDefaultDataTips, showDefaultDateTips, showDefaultTimeTips)
        showTodayPreviewData(max, min, avg)
        showTotal(max, totalDataUnit)
        binding.mStressView.setDayProgress(bloodOxygenInfo, progressTime, BloodOxygenView.TODAY)
    }

    var weekDate = ""
    var monthDate = ""
    private fun getWeekOrMonthData() {
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
        viewModel.getSinglePressureListByDateRange(beginDay, type, endDay)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthData(data: SinglePressureListResponse?, type: Int) {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var length = 0
        var max = "--"
        var min = "--"
        var avg = "--"
        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var totalDataUnit = getString(R.string.healthy_sports_item_percent_sign)

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
        val dataInfos = mutableListOf<OfflineStressView.DataInfo>()

        for (i in 0 until length) {
            progressTime[i] = DateUtils.getNextDay(beginDay, i).toString()
            calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                continue
            }
        }

        if (data != null && data.dataList.isNotEmpty()) {
            max = if (data.maxPressure.trim().toString() == "null" || data.maxPressure.trim().toString() == ""
                || data.maxPressure.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.maxPressure.toString()
            }
            min = if (data.minPressure.trim().toString() == "null" || data.minPressure.trim().toString() == ""
                || data.minPressure.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.minPressure.toString()
            }
            avg = if (data.avgPressure.trim().toString() == "null" || data.avgPressure.trim().toString() == ""
                || data.avgPressure.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.avgPressure.toString()
            }

            if (data.dataList.isNotEmpty()) {
                for (i in 0 until length) {
                    val curShowDay = DateUtils.getStringDate(progressTime[i]!!.toLong(), DateUtils.TIME_YYYY_MM_DD)
                    var index: Int = -1

                    for (m in data.dataList.indices) {
                        if (curShowDay.equals(data.dataList[m].date)) {
                            index = m
                            break
                        }
                    }

                    val info = OfflineStressView.DataInfo()
                    if (index != -1) {
                        info.progressValue = data.dataList[index].maxPressure.trim().toFloat()
                        info.progressMaxValue = data.dataList[index].maxPressure.trim().toFloat()
                        info.progressMinValue = data.dataList[index].minPressure.trim().toFloat()
                    } else {
                        info.progressValue = 0f
                        info.progressMaxValue = 0f
                        info.progressMinValue = 0f
                    }
                    dataInfos.add(info)
                    showDefaultDataTips = if (dataInfos[0].progressMaxValue.toInt() == 0) {
                        "--"
                    } else {
                        dataInfos[0].progressMaxValue.toInt().toString()
                    }
                }
            }

        } else {
            binding.lyDailyRightTips.visibility = View.GONE
        }

        showDefaultDateTips = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), "yyyy/MM/dd")
        showLyRightTips(showDefaultDataTips, showDefaultDateTips, "")
        showTotal(max, totalDataUnit)

        showTodayPreviewData(max, min, avg)
        binding.mStressView.setProgress(dataInfos, progressTime, type)
    }
}