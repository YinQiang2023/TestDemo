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
import com.smartwear.publicwatch.databinding.ActivityStressBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.response.PressureListResponse
import com.smartwear.publicwatch.https.response.PressureResponse
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.view.PressureView
import com.smartwear.publicwatch.utils.DateUtils
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.SpannableStringTool
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.DailyModel
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by Android on 2022/10/1
 */
class StressActivity : BaseActivity<ActivityStressBinding, DailyModel>(ActivityStressBinding::inflate, DailyModel::class.java), View.OnClickListener {
    private val TAG: String = StressActivity::class.java.simpleName

    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var dayList: PressureResponse? = null
    private var weekList: PressureListResponse? = null
    private var monthList: PressureListResponse? = null
    private var showDateType = Global.DateType.TODAY
    private var dialog: Dialog? = null
    private var currentDate = ""
    private var PressureFrequency = 5


    //离线心率即历史
    override fun onClick(v: View?) {
        when (v?.id) {
            /*binding.lyHistory.id -> {
                startActivity(Intent(this, PressureHistoryActivity::class.java).putExtra("date", curDay))
            }*/
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

        setViewsClickListener(this, binding.dailySelect.lyDate/*, binding.lyHistory*/)
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
//                        if (weekList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(weekList , PressureView.WEEK)
//                        }
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    2 -> {
                        binding.tvTodayPreview.text = getString(R.string.heart_rate_this_month_preview)
                        showDateType = Global.DateType.MONTH
//                        if (monthList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(monthList,PressureView.MONTH)
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
    }

    private fun observe() {
        /*viewModel.getSingleLastPressureData.observe(this, androidx.lifecycle.Observer {
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        binding.tvLastTime.text = "--"
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        binding.tvLastTime.text = it.data.measureData
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        binding.tvLastTime.text = "--"
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        binding.tvLastTime.text = "--"
                    }
                }
            }
        })*/

        viewModel.getPressureDataByDay.observe(this, androidx.lifecycle.Observer {
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

        viewModel.getPressureListByDateRange.observe(this, androidx.lifecycle.Observer {
            dismissDialog()
            if (it != null) {
                var type = PressureView.WEEK
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
                                type = PressureView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                monthList = it.data
                                temp = monthList
                                type = PressureView.MONTH
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
                                type = PressureView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                temp = monthList
                                type = PressureView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        var temp: Nothing? = null
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                type = PressureView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                type = PressureView.MONTH
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

//        val progressValue = FloatArray(144)
//        val progressTime = arrayOfNulls<String>(144)
//        for (i in 0 until 144) {
//            progressValue[i] = Random().nextInt(160).toFloat()
//            progressTime[i] = i.toString()
//
//            if( progressValue[i]  == 0f){
//                progressValue[i] = 20f
//            }
//
//
//            progressValue[20] = 0f
//            progressValue[21] = 0f
//            progressValue[22] = 0f
//            progressValue[23] = 0f
//            progressValue[24] = 0f
//            progressValue[25] = 0f
//            progressValue[26] = 0f
//        }
//        mPressure.setProgress(progressValue, progressTime)

//        val progressValue = FloatArray(7)
//        val progressMinValue = FloatArray(7)
//        val progressTime = arrayOfNulls<String>(7)
//        val PressureInfo = mutableListOf<PressureView.PressureInfo>()
//        for (i in 0 until 7) {
////            progressValue[i] = Random().nextInt(30).toFloat() + 70
////            progressMinValue[i] = progressValue[i] - Random().nextInt(30).toFloat()
////            if(progressMinValue[i] < 60){
////                progressMinValue[i] = 60f
////            }
////            progressValue[0] = 100f
//
//            val info = PressureView.PressureInfo()
//            info.progressValue = Random().nextInt(30).toFloat() + 70
//            info.maxValue = Random().nextInt(30).toFloat() + 70
//            info.minValue = Random().nextInt(30).toFloat() + 70
//            PressureInfo.add(info)
//        }
//        progressTime[0] = "1641797411000"
//        progressTime[1] = "1641883811000"
//        progressTime[2] = "1641970211000"
//        progressTime[3] = "1642056611000"
//        progressTime[4] = "1642143011000"
//        progressTime[5] = "1642229411000"
//        progressTime[6] = "1642315811000"
//        mPressure.setProgress(PressureInfo, progressTime , PressureView.WEEK)

//        val progressValue = FloatArray(30)
//        val progressTime = arrayOfNulls<String>(31)
//        val PressureInfo = mutableListOf<PressureView.PressureInfo>()
//        //根据日期获取时间戳
//        try {
//            val timeArray = arrayOfNulls<String>(31)
//            var index = 1
//            for (i in 0..30) {
//                if (i < 10){
//                    timeArray[i] = "2022/01/0$index 14:50:11"
//                }else{
//                    timeArray[i] = "2022/01/$index 14:50:11"
//                }
//
//                index++
//                val date = com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy/MM/dd HH:mm:ss").parse(timeArray[i])
//                progressTime[i] = date.time.toString()
//            }
//        } catch (e: ParseException) {
//            e.printStackTrace()
//        }
//
//        for (i in 0 until 31) {
//            val info = PressureView.PressureInfo()
//            info.progressValue = Random().nextInt(30).toFloat() + 70
//            info.maxValue = Random().nextInt(30).toFloat() + 70
//            info.minValue = Random().nextInt(30).toFloat() + 70
//            PressureInfo.add(info)
//        }
//        mPressure.setProgress(PressureInfo, progressTime,PressureView.MONTH)


        binding.mPressure.setOnSlidingListener { data, index, time, step, max, min ->
            Log.i(TAG, "${data}" + "  " + index + " " + time + " step=" + step)
            if (index != -1) {
                when (showDateType) {
                    Global.DateType.TODAY -> {
                        if (step > 0) {
                            var timeTemp = "${(index) * PressureFrequency}"
                            var hour = (timeTemp.toInt() / 60).toString()
                            var minute = (timeTemp.toInt() % 60).toString()
                            var showTime = ""
                            if ((timeTemp.toInt() % 60) == 0) {
                                showTime = if (hour.toInt() < 10) {
                                    if (hour.toInt() == 0) {
                                        "00:00"
                                    } else {
                                        "0${(hour.toInt())}:00"
                                    }
                                } else {
                                    if (hour.toInt() <= 23) {
                                        "${(hour.toInt())}:00"
                                    } else {
                                        "00:00"
                                    }
                                }
                            } else {
                                showTime = if (hour.toInt() < 10) {
                                    if (minute.toInt() < 10) {
                                        "0$hour:0$minute"
                                    } else {
                                        "$hour:$minute"
                                    }
                                } else {
                                    if (hour.toInt() <= 23) {
                                        if (minute.toInt() < 10) {
                                            "$hour:0$minute"
                                        } else {
                                            "$hour:$minute"
                                        }
                                    } else {
                                        if (minute.toInt() < 10) {
                                            "00:0$minute"
                                        } else {
                                            "00:$minute"
                                        }
                                    }
                                }
                            }
                            var showData = if (step.toInt() > 0) step.toInt().toString() else "--"
                            if (step.toInt() > 0) {
                                binding.lyDailyRightTips.visibility = View.VISIBLE
                                showLyRightTips("$showData", null, showTime)
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
                        val dataTemp = if (min != -1f && max != -1f) {
                            if (min.toInt() > 0) {
                                "${min.toInt()}~${max.toInt()}"
                            } else {
                                "${max.toInt()}~${max.toInt()}"
                            }
                        } else {
                            "--"
                        }
                        if (min > 0 || max > 0) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            showLyRightTips(dataTemp, DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd"), "")
                        }
                    }
                }
            }
        }

        binding.mPressure.setOnTouchListener { v: View?, event: MotionEvent ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.mPressure.setTouchPos(eventX)
                }
//                MotionEvent.ACTION_UP -> mPressure.setTouchPos(-1f)
            }
            binding.mPressure.invalidate()
            true
        }

        dialog = DialogUtils.showLoad(this)

        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        /*viewModel.getSingleLastPressureData(curDay)*/
        viewModel.viewModelScope.launch {
            viewModel.queryUnUploadPressure(true)
        }
        getDayData()
        showDayData()

//        showDateType = Global.DateType.WEEK
//        binding.mPressure.testData()
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

    private fun getDayData() {
        dismissDialog()
        viewModel.getPressureDataByDay(curDay)
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

        var totalDataUnit = getString(R.string.hr_unit_bpm)

        showDefaultTimeTips = "00:00"
        val temp = curDay.trim().split("-")
        showDefaultDateTips = "${temp[1]}-${temp[2]}"
        if (dayList != null) {
            max = dayList?.maxPressure.toString()
            min = dayList?.minPressure.toString()
            avg = dayList?.avgPressure.toString()
            list = dayList?.pressureData?.split(",")!!
            PressureFrequency = dayList?.pressureFrequency?.trim()!!.toInt()
            val progressValue = FloatArray(list.size)
            showDefaultDataTips = if (list[0].trim().toFloat() > 0f) list[0] else "--"
            val progressTime = arrayOfNulls<String>(list.size)
            if (list.isNotEmpty()) {
                for (i in list.indices) {
                    if (list.isNotEmpty()) {
                        progressValue[i] = list[i].toFloat()
                    } else {
                        progressValue[i] = 0f
                    }
                    progressTime[i] = i.toString()
                }
            }
            //有效值集合
            val validValue = progressValue.filter { it > 0f }
            if (validValue.size == 1) {
                //替换最大值
                validValue.maxOrNull()?.let { maxValue ->
                    val maxIndex = progressValue.indexOfLast { it == maxValue }
                    if (maxIndex != -1) {
                        if (maxValue < max.toFloat()) {
                            progressValue[maxIndex] = max.toFloat()
                        }
                    }
                }
            } else if (validValue.size > 1) {
                //替换最小值
                validValue.minOrNull()?.let { minValue ->
                    val minIndex = progressValue.indexOfFirst { it == minValue }
                    if (minIndex != -1) {
                        if (minValue > min.toFloat()) {
                            progressValue[minIndex] = min.toFloat()
                        }
                    }
                }
                //替换最大值
                validValue.maxOrNull()?.let { maxValue ->
                    val maxIndex = progressValue.indexOfLast { it == maxValue }
                    if (maxIndex != -1) {
                        if (maxValue < max.toFloat()) {
                            progressValue[maxIndex] = max.toFloat()
                        }
                    }
                }
            }
            binding.mPressure.setProgress(progressValue, progressTime, PressureView.TODAY)
        } else {
            binding.lyDailyRightTips.visibility = View.GONE
            val progressValue = FloatArray(288)
            val progressTime = arrayOfNulls<String>(288)
            for (i in 0 until 288) {
                progressValue[i] = 0f
                progressTime[i] = i.toString()
            }
            binding.mPressure.setProgress(progressValue, progressTime, PressureView.TODAY)
        }

        if (max != "--" && max.trim().toInt() == 0) {
            max = "--"
        }

        if (avg != "--" && avg.trim().toInt() == 0) {
            avg = "--"
        }

        if (min != "--" && min.trim().toInt() == 0) {
            min = "--"
        }

        showLyRightTips(showDefaultDataTips, showDefaultDateTips, showDefaultTimeTips)
        showTodayPreviewData(max, min, avg)
        showTotal(max, totalDataUnit)
    }

    private fun showLyRightTips(data: String, date: String? = null, time: String) {
        binding.tvTotalSum.text = data
        if (date != null) {
            binding.tvTotalDate.text = date
        }
        binding.tvTotalSumTime.text = time
    }

    private fun showTodayPreviewData(max: String, min: String, avg: String) {
        binding.tvMaximumPressure.text = max
        binding.tvMinPressure.text = min
        binding.tvAveragePressure.text = avg
    }

    private fun showTotal(tvDistance: String, unit: String) {
        if (tvDistance.isNotEmpty()) {
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
        viewModel.getPressureListByDateRange(beginDay, endDay)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthData(data: PressureListResponse?, type: Int) {
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
        var totalDataUnit = getString(R.string.hr_unit_bpm)

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

//        val progressValue = FloatArray(length)
        val progressTime = arrayOfNulls<String>(length)
        val PressureInfo = mutableListOf<PressureView.PressureInfo>()

        for (i in 0 until length) {
            progressTime[i] = DateUtils.getNextDay(beginDay, i).toString()
            calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                continue
            }
        }

        if (data != null && data.dataList.isNotEmpty()) {
            max = data.maxPressure
            min = data.minPressure
            avg = data.avgPressure

            if (data.dataList.isNotEmpty()) {
                showDefaultDataTips = data.dataList[0].maxPressure
                for (i in 0 until length) {
                    val curShowDay = DateUtils.getStringDate(progressTime[i]!!.toLong(), DateUtils.TIME_YYYY_MM_DD)
                    var index: Int = -1

                    for (m in data.dataList.indices) {
                        if (curShowDay.equals(data.dataList[m].date)) {
                            index = m
                            break
                        }
                    }
                    val info = PressureView.PressureInfo()
                    if (index != -1) {
                        info.progressValue = data.dataList[index].maxPressure.trim().toFloat()
                        info.maxValue = data.dataList[index].maxPressure.trim().toFloat()
                        info.minValue = data.dataList[index].minPressure.trim().toFloat()
                    } else {
//                        progressValue[i] = 0f
                        info.progressValue = 0f
                        info.maxValue = 0f
                        info.minValue = 0f
                    }
                    PressureInfo.add(info)
                }
            }

        } else {
            binding.lyDailyRightTips.visibility = View.GONE
        }

        showDefaultDateTips = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), "yyyy/MM/dd")
        showLyRightTips(showDefaultDataTips, showDefaultDateTips, "")
        showTotal(max, totalDataUnit)

        if (max != "--" && max.trim().toInt() == 0) {
            max = "--"
        }

        if (avg != "--" && avg.trim().toInt() == 0) {
            avg = "--"
        }

        if (min != "--" && min.trim().toInt() == 0) {
            min = "--"
        }
        showTodayPreviewData(max, min, avg)
        binding.mPressure.setProgress(PressureInfo, progressTime, type)
    }
}