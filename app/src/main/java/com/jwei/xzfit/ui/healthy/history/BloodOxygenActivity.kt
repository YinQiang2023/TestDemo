package com.jwei.xzfit.ui.healthy.history

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.haibin.calendarview.CalendarView
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.BloodOxygenActivityBinding
import com.jwei.xzfit.db.model.track.BehaviorTrackingLog
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.https.response.SingleBloodOxygenListResponse
import com.jwei.xzfit.https.response.SingleBloodOxygenResponse
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.view.BloodOxygenView
import com.jwei.xzfit.utils.DateUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.SpannableStringTool
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.viewmodel.DailyModel
import kotlinx.coroutines.launch
import java.util.*

class BloodOxygenActivity : BaseActivity<BloodOxygenActivityBinding, DailyModel>(BloodOxygenActivityBinding::inflate, DailyModel::class.java), View.OnClickListener {
    private val TAG: String = BloodOxygenActivity::class.java.simpleName
    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var dayList: SingleBloodOxygenResponse? = null
    private var weekList: SingleBloodOxygenListResponse? = null
    private var monthList: SingleBloodOxygenListResponse? = null
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
        binding.title.tvCenterTitle.text = getString(R.string.blood_oxy_title)
        behaviorTrackingLog = AppTrackingManager.getNewBehaviorTracking("10", "29")

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
//                        if (weekList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(weekList , BloodOxygenView.WEEK)
//                        }
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    2 -> {
                        binding.tvTodayPreview.text = getString(R.string.heart_rate_this_month_preview)
                        showDateType = Global.DateType.MONTH
//                        if (monthList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(monthList , BloodOxygenView.MONTH)
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
        viewModel.getSingleBloodOxygenDataByDay.observe(this, androidx.lifecycle.Observer {
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

        viewModel.getSingleBloodOxygenListByDateRange.observe(this, androidx.lifecycle.Observer {
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
        startVisibleTimeTimer()
//        val progressValue = FloatArray(30)
//        val progressTime = arrayOfNulls<String>(30)
//        for (i in 0 until 30) {
//            progressValue[i] = Random().nextInt(30).toFloat() + 70
//            progressTime[i] = i.toString()
//
//            progressValue[0] = 100f
//        }
//        mBloodOxygenView.setProgress(progressValue, progressTime)

//        showDateType = Global.DateType.WEEK
//        val progressValue = FloatArray(7)
//        val progressMinValue = FloatArray(7)
//        val progressTime = arrayOfNulls<String>(7)
//        for (i in 0 until 7) {
//            progressValue[i] = Random().nextInt(30).toFloat() + 70
//            progressMinValue[i] = progressValue[i] - Random().nextInt(30).toFloat()
//            if(progressMinValue[i] < 60){
//                progressMinValue[i] = 60f
//            }
//            progressValue[0] = 100f
//        }
//        progressTime[0] = System.currentTimeMillis().toString()
//        progressTime[1] = System.currentTimeMillis().toString()
//        progressTime[2] = System.currentTimeMillis().toString()
//        progressTime[3] = System.currentTimeMillis().toString()
//        progressTime[4] = System.currentTimeMillis().toString()
//        progressTime[5] = System.currentTimeMillis().toString()
//        progressTime[6] = System.currentTimeMillis().toString()
//        mBloodOxygenView.setProgress(progressValue, progressMinValue, progressTime)

//        val progressTime = arrayOfNulls<String>(7)
//        val bloodOxygenInfo = mutableListOf<BloodOxygenView.BloodOxygenInfo>()
//        for (i in 0 until 7) {
//            val info = BloodOxygenView.BloodOxygenInfo()
//            info.progressValue = Random().nextInt(30).toFloat() + 70
//            info.progressMaxValue = Random().nextInt(30).toFloat() + 70
//            info.progressMinValue = Random().nextInt(30).toFloat() + 70
//            bloodOxygenInfo.add(info)
//        }
//        progressTime[0] = "1641797411000"
//        progressTime[1] = "1641883811000"
//        progressTime[2] = "1641970211000"
//        progressTime[3] = "1642056611000"
//        progressTime[4] = "1642143011000"
//        progressTime[5] = "1642229411000"
//        progressTime[6] = "1642315811000"
//        mBloodOxygenView.setProgress(bloodOxygenInfo, progressTime , HeartRateView.WEEK)

//        showDateType = Global.DateType.MONTH
//        val progressValue = FloatArray(30)
//        val progressMinValue = FloatArray(30)
//        val progressTime = arrayOfNulls<String>(30)
//        for (i in 0 until 30) {
//            progressValue[i] = Random().nextInt(30).toFloat() + 70
//            progressMinValue[i] = progressValue[i] - Random().nextInt(30).toFloat()
//            if(progressMinValue[i] < 60){
//                progressMinValue[i] = 60f
//            }
//            progressValue[0] = 100f
//
//            progressTime[i] = System.currentTimeMillis().toString()
//        }
//        mBloodOxygenView.setProgress(progressValue, progressMinValue, progressTime)


        binding.mBloodOxygenView.setOnSlidingListener { data, index, time, step, max, min ->
            Log.i(TAG, "${data}" + "  " + index + " " + time + " step=" + step)

            if (index != -1) {
                when (showDateType) {
                    Global.DateType.TODAY -> {
//                        var timeTemp = time
//                        if (timeTemp.toInt() < 10) {
//                            timeTemp = "0$timeTemp:00"
//                        }else{
//                            timeTemp = "$timeTemp:00"
//                        }
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
//                        val date = DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd")
//                        val positions = temp?.dataList?.indexOfFirst { it.date == date }
                        val dataTemp = if (min > 0f && max > 0f) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            "${min.toInt()}~${max.toInt()}"
                        } else {
//                            binding.lyDailyRightTips.visibility = View.GONE
                            "--"
                        }
                        if (dataTemp != "--")
                            showLyRightTips(dataTemp, DateUtils.getStringDate(time.toLong(), "yyyy-MM-dd"), "")
                    }
                }
            }
        }

        binding.mBloodOxygenView.setOnTouchListener { v: View?, event: MotionEvent ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.mBloodOxygenView.setTouchPos(eventX)
                }
//                MotionEvent.ACTION_UP -> mBloodOxygenView.setTouchPos(-1f)
            }
            binding.mBloodOxygenView.invalidate()
            true
        }

        dialog = DialogUtils.showLoad(this)
        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        viewModel.viewModelScope.launch {
            viewModel.queryUnUploadSingleBloodOxygen(true)
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
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
                .append(" ")
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .create()
        } else {
            binding.tvDistance.text = SpannableStringTool.get()
                .append("--")
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
                .append(" ")
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .append(unit)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .create()
        }
    }

    private fun getDayData() {
        dismissDialog()
        viewModel.getSingleBloodOxygenDataByDay(curDay)
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
        val bloodOxygenInfo = mutableListOf<BloodOxygenView.BloodOxygenDayInfo>()

        if (dayList != null && dayList?.dataList!!.isNotEmpty()) {
//            max = dayList?.maxBloodOxygen.toString()
            max = if (dayList?.maxBloodOxygen?.trim().toString() == "" || dayList?.maxBloodOxygen?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.maxBloodOxygen.toString()
            }
//            min = dayList?.minBloodOxygen.toString()
            min = if (dayList?.minBloodOxygen?.trim().toString() == "" || dayList?.minBloodOxygen?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.minBloodOxygen.toString()
            }
//            avg = dayList?.avgBloodOxygen.toString()
            avg = if (dayList?.avgBloodOxygen?.trim().toString() == "" || dayList?.avgBloodOxygen?.trim().toString() == "0") {
                "--"
            } else {
                dayList?.avgBloodOxygen.toString()
            }
            if (dayList?.dataList != null) {
                for (i in dayList?.dataList!!.indices) {
                    val bean = BloodOxygenView.BloodOxygenDayInfo()
                    bean.progressValue = dayList?.dataList!![i].measureData.trim().toFloat()
                    bean.time = dayList?.dataList!![i].measureTime
                    bloodOxygenInfo.add(bean)
                }

//                for (i in 0 until 24){
//                    val curShowDay = i.toString()
//                    var index: Int = -1
//
//                    for (m in dayList?.dataList!!.indices) {
//                        val time = DateUtils.getStringDate(DateUtils.getLongTime(
//                            dayList!!.dataList[m].measureTime, DateUtils.TIME_YYYY_MM_DD_HHMMSS), "HH")
//                        if (curShowDay == time) {
//                            index = m
//                            break
//                        }
//                    }
//
//                    if (index != -1){
//                        progressValue[i] = dayList?.dataList!![index].measureData.trim().toFloat()
//                        progressTime[i] = dayList?.dataList!![index].measureTime
//                    }else{
//                        progressValue[i] = 0f
//                    }
//                }
            }
        } else {
            binding.lyDailyRightTips.visibility = View.GONE
//            for (i in 0 until 24) {
//                progressValue[i] = 0f
//                progressTime[i] = i.toString()
//            }
        }

        showLyRightTips(showDefaultDataTips, showDefaultDateTips, showDefaultTimeTips)
        showTodayPreviewData(max, min, avg)
        showTotal(max, totalDataUnit)

//        mBloodOxygenView.setProgress(progressValue, progressTime , BloodOxygenView.TODAY)
        binding.mBloodOxygenView.setDayProgress(bloodOxygenInfo, progressTime, BloodOxygenView.TODAY)
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
        viewModel.getSingleBloodOxygenListByDateRange(beginDay, endDay)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthData(data: SingleBloodOxygenListResponse?, type: Int) {
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

//        val progressValue = FloatArray(length)
        val progressTime = arrayOfNulls<String>(length)
//        val progressMinValue = FloatArray(length)
        val bloodOxygenInfo = mutableListOf<BloodOxygenView.BloodOxygenInfo>()

        for (i in 0 until length) {
            progressTime[i] = DateUtils.getNextDay(beginDay, i).toString()
            calendar.timeInMillis = DateUtils.getLongTime(endDay, DateUtils.TIME_YYYY_MM_DD)
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            if (calendar.timeInMillis > System.currentTimeMillis()) {
                continue
            }
        }

        if (data != null && data.dataList.isNotEmpty()) {
//            max = data.maxBloodOxygen
//            min = data.minBloodOxygen
//            avg = data.avgBloodOxygen
            max = if (data.maxBloodOxygen.trim().toString() == "null" || data.maxBloodOxygen.trim().toString() == ""
                || data.maxBloodOxygen.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.maxBloodOxygen.toString()
            }
//            min = data.minBloodOxygen.toString()
            min = if (data.minBloodOxygen.trim().toString() == "null" || data.minBloodOxygen.trim().toString() == ""
                || data.minBloodOxygen.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.minBloodOxygen.toString()
            }
//            avg = data.avgBloodOxygen.toString()
            avg = if (data.avgBloodOxygen.trim().toString() == "null" || data.avgBloodOxygen.trim().toString() == ""
                || data.avgBloodOxygen.trim().toString() == "0"
            ) {
                "--"
            } else {
                data.avgBloodOxygen.toString()
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

                    val info = BloodOxygenView.BloodOxygenInfo()
                    if (index != -1) {
                        try {
                            info.progressValue = data.dataList[index].maxBloodOxygen.trim().toFloat()
                            info.progressMaxValue = data.dataList[index].maxBloodOxygen.trim().toFloat()
                            info.progressMinValue = data.dataList[index].minBloodOxygen.trim().toFloat()
                        } catch (e: Exception) {
                            info.progressValue = 0f
                            info.progressMaxValue = 0f
                            info.progressMinValue = 0f
                        }
                    } else {
                        info.progressValue = 0f
                        info.progressMaxValue = 0f
                        info.progressMinValue = 0f
                    }
                    bloodOxygenInfo.add(info)
                    showDefaultDataTips = if (bloodOxygenInfo[0].progressMaxValue.toInt() == 0) {
                        "--"
                    } else {
                        bloodOxygenInfo[0].progressMaxValue.toInt().toString()
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
        binding.mBloodOxygenView.setProgress(bloodOxygenInfo, progressTime, type)
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