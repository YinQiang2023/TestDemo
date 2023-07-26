package com.jwei.xzfit.ui.healthy.history

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.google.android.material.tabs.TabLayout
import com.haibin.calendarview.CalendarView
import com.zhapp.ble.ControlBleTools
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.DailyDataActivityBinding
import com.jwei.xzfit.db.model.track.BehaviorTrackingLog
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.https.response.DailyDayResponse
import com.jwei.xzfit.https.response.DailyListResponse
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.user.bean.TargetBean
import com.jwei.xzfit.ui.user.utils.UnitConverUtils
import com.jwei.xzfit.ui.user.utils.UnitConverUtils.showDistanceUnitStyle
import com.jwei.xzfit.ui.view.HistogramView
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.viewmodel.DailyModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

class DailyDataActivity : BaseActivity<DailyDataActivityBinding, DailyModel>(DailyDataActivityBinding::inflate, DailyModel::class.java), View.OnClickListener {
    private val TAG: String = DailyDataActivity::class.java.simpleName
    private var curDay = ""
    private var curWeek = ""
    private var curMonth = ""
    private var showDateType = Global.DateType.TODAY
    private var currentStepData = "0"
    private var currentCaloriesData = "0"
    private var currentDistanceData = "0.00"

    private var dayList: DailyDayResponse? = null
    private var weekList: DailyListResponse? = null
    private var monthList: DailyListResponse? = null
    private var dialog: Dialog? = null
    private var isCurrentDay = true
    private var currentDate = ""

    private var type = DailyDataType.STEPS

    //行为埋点
    private var behaviorTrackingLog: BehaviorTrackingLog? = null

    enum class DailyDataType {
        STEPS, DISTANCE, CALORIES
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

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun initView() {
        super.initView()
        type = intent.extras?.get(viewModel.dataTypeTag) as DailyDataType
        behaviorTrackingLog = when (type) {
            DailyDataType.STEPS -> AppTrackingManager.getNewBehaviorTracking("10", "24")
            DailyDataType.DISTANCE -> AppTrackingManager.getNewBehaviorTracking("10", "25")
            DailyDataType.CALORIES -> AppTrackingManager.getNewBehaviorTracking("10", "26")
        }
        currentStepData = intent.extras?.get(viewModel.currentStepTag) as String
        currentCaloriesData = intent.extras?.get(viewModel.currentCaloriesTag) as String
        currentDistanceData = intent.extras?.get(viewModel.currentDistanceTag) as String
        setViewsClickListener(this, binding.dailySelect.lyDate)
        binding.dailySelect.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                //选中
                when (tab.position) {
                    //日
                    0 -> {
                        showDateType = Global.DateType.TODAY
//                        showDayData()
                        getDayData()
                    }
                    //周
                    1 -> {
                        showDateType = Global.DateType.WEEK
                        isCurrentDay = false
//                        if (weekList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(weekList , HistogramView.WEEK)
//                        }
                        binding.dailySelect.tvDate.text = weekDate
                    }
                    //月
                    2 -> {
                        showDateType = Global.DateType.MONTH
                        isCurrentDay = false
//                        if (monthList == null){
                        getWeekOrMonthData()
//                        }else{
//                            showWeekOrMonthData(monthList , HistogramView.MONTH)
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
                try {
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
                                isCurrentDay = calendar.isCurrentDay
                                getDayData()
                                binding.dailySelect.tvDate.text = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
                            }
                            Global.DateType.WEEK -> {
                                isCurrentDay = false
                                getWeekOrMonthData()
                            }
                            Global.DateType.MONTH -> {
                                isCurrentDay = false
                                getWeekOrMonthData()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        when (type) {
            DailyDataType.STEPS -> {
                binding.tvYaxisUnit.visibility = View.GONE
                binding.tvYaxisUnit.text = ""
                binding.lyHistogramBg.setBackgroundResource(R.drawable.layout_daily_total_sum_bg)
                binding.tvTotalSumUnit.text = getString(R.string.unit_step)
//                binding.tvLeftTotalSumUnit.text = getString(R.string.unit_distance_0)
                binding.tvLeftTotalSumUnit.text = showDistanceUnitStyle(this)
                binding.tvLeftTotalSumTips.text = getString(R.string.daily_data_total_mileage_tips)
                binding.tvRightTotalSumTips.text = getString(R.string.total_calories_tips)
                binding.tvRightTotalSumUnit.text = getString(R.string.unit_calories)
                binding.title.tvCenterTitle.text = getString(R.string.healthy_sports_list_step)
                binding.tvTopLeftTitle.text = getString(R.string.healthy_sports_list_step)
                binding.lyDailyRightTips.setBackgroundResource(R.drawable.public_chart_label_bg)
                binding.mHistogramView.setDefaultColor(R.color.chart_step_color)
                binding.mHistogramView.setTouchColor(R.color.chart_step_touch)
                binding.ivTopLeftTitle.setImageResource(R.mipmap.healthy_item_step)
            }
            DailyDataType.DISTANCE -> {
                binding.tvYaxisUnit.visibility = View.GONE
                binding.tvYaxisUnit.text = ""
                binding.lyHistogramBg.setBackgroundResource(R.drawable.layout_daily_total_sum_bg)
//                binding.tvTotalSumUnit.text = getString(R.string.unit_distance_0)
                binding.tvTotalSumUnit.text = showDistanceUnitStyle(this)
                binding.tvLeftTotalSumUnit.text = getString(R.string.unit_step)
                binding.tvLeftTotalSumTips.text = getString(R.string.total_steps_tips)
                binding.tvRightTotalSumTips.text = getString(R.string.total_calories_tips)
                binding.tvRightTotalSumUnit.text = getString(R.string.unit_calories)
                binding.title.tvCenterTitle.text = getString(R.string.healthy_sports_list_distance)
                binding.tvTopLeftTitle.text = getString(R.string.healthy_sports_list_distance)
                binding.lyDailyRightTips.setBackgroundResource(R.drawable.public_chart_label_bg)
                binding.mHistogramView.setDefaultColor(R.color.chart_distance_color)
                binding.mHistogramView.setTouchColor(R.color.chart_distance_touch)
                binding.ivTopLeftTitle.setImageResource(R.mipmap.healthy_item_distance)
            }
            DailyDataType.CALORIES -> {
                binding.tvYaxisUnit.visibility = View.GONE
                binding.tvYaxisUnit.text = ""
                binding.lyHistogramBg.setBackgroundResource(R.drawable.layout_daily_total_sum_bg)
                binding.tvTotalSumUnit.text = getString(R.string.unit_calories)
                binding.tvLeftTotalSumUnit.text = getString(R.string.unit_step)
                binding.tvLeftTotalSumTips.text = getString(R.string.total_steps_tips)
                binding.tvRightTotalSumTips.text = getString(R.string.daily_data_total_mileage_tips)
//                binding.tvRightTotalSumUnit.text = getString(R.string.unit_distance_0)
                binding.tvRightTotalSumUnit.text = showDistanceUnitStyle(this)
                binding.title.tvCenterTitle.text = getString(R.string.healthy_sports_list_calories)
                binding.tvTopLeftTitle.text = getString(R.string.healthy_sports_list_calories)
                binding.lyDailyRightTips.setBackgroundResource(R.drawable.public_chart_label_bg)
                binding.mHistogramView.setDefaultColor(R.color.chart_calorie_color)
                binding.mHistogramView.setTouchColor(R.color.chart_calorie_touch)
                binding.ivTopLeftTitle.setImageResource(R.mipmap.healthy_item_calories)
            }
        }

        observer()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initData() {
        super.initData()
        startVisibleTimeTimer()
        binding.mHistogramView.setOnSlidingListener { data, index, time, value ->
            Log.i(TAG, "${data}" + "  " + index + " " + time + " value=" + value)
            if (index != -1) {
                when (showDateType) {
                    Global.DateType.TODAY -> {
                        //返回错误 eg:1682524800000
                        if (time.length >= 13) return@setOnSlidingListener
                        var timeTemp = time
                        var timeNext = "${time.toLong() + 1}"
                        if (timeTemp.toLong() < 10) {
                            timeTemp = "0$timeTemp"
                            timeNext = if (timeNext.trim().toLong() < 10) {
                                "0$timeNext"
                            } else {
                                "$timeNext"
                            }
                        }
                        if (timeTemp.toLong() == 23L) {
                            timeNext = "00"
                        }
                        var results = when (type) {
                            //步数
                            DailyDataType.STEPS -> {
                                if (value > 0) {
                                    binding.tvTotalSumUnit.text = getString(R.string.unit_steps)
                                }
                                value.toInt().toString()
                            }
                            //距离
                            DailyDataType.DISTANCE -> {
//                                UnitConverUtils.showDistanceKmStyle(value.toString())
                                UnitConverUtils.showDistanceStyleToThreeLen(value.toString())
                            }
                            //卡路里
                            DailyDataType.CALORIES -> {
                                value.toInt().toString()
                            }
                        }
                        if (value > 0) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            showTotalSum(
                                results, DateUtils.getStringDate(
                                    DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD),
                                    DateUtils.TIME_MM_DD
                                ), "$timeTemp:00-$timeNext:00"
                            )
                        }
                    }
                    Global.DateType.WEEK, Global.DateType.MONTH -> {
                        var results = when (type) {
                            DailyDataType.STEPS -> {
                                if (value > 0) {
                                    binding.tvTotalSumUnit.text = getString(R.string.unit_steps)
                                }
                                value.toInt().toString()
                            }
                            DailyDataType.DISTANCE -> {
//                                UnitConverUtils.showDistanceKmStyle(value.toString())
                                UnitConverUtils.showDistanceStyleToThreeLen(value.toString())
                            }
                            DailyDataType.CALORIES -> {
                                value.toInt().toString()
                            }
                        }

                        if (value > 0) {
                            binding.lyDailyRightTips.visibility = View.VISIBLE
                            showTotalSum(results, DateUtils.getStringDate(time.toLong(), DateUtils.TIME_YYYY_MM_DD), "")
                        }
                    }
                }
            }
        }

        binding.mHistogramView.setOnTouchListener { v: View?, event: MotionEvent ->
            val eventX = event.x
            when (event.action) {
                MotionEvent.ACTION_DOWN/*, MotionEvent.ACTION_MOVE*/ -> {
                    binding.mHistogramView.setTouchPos(
                        eventX
                    )
                }
//                MotionEvent.ACTION_UP -> mHistogramView.setTouchPos(-1f)
            }
            binding.mHistogramView.invalidate()
            true
        }

        dialog = DialogUtils.showLoad(this)

        curDay = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM_DD)
        curWeek = curDay
//        curWeek = "2021-08-09"
        curMonth = DateUtils.getStringDate(System.currentTimeMillis(), DateUtils.TIME_YYYY_MM)
        currentDate = curMonth
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                //TODO
                viewModel.queryUnUploadDailyData(true)
            }
        }
        getDayData()
//        viewModel.getDataByDay("2021-09-09")
        showDayData()

//        showDateType = Global.DateType.WEEK
//        binding.mHistogramView.testData()

//        val progressValue = FloatArray(7)
//        val progressTime = arrayOfNulls<String>(7)
//        for (i in 0 until 7) {
//            progressValue[i] = i * 5f
//            progressValue[0] = 130f
//        }
//        progressTime[0] = "1641797411000"
//        progressTime[1] = "1641883811000"
//        progressTime[2] = "1641970211000"
//        progressTime[3] = "1642056611000"
//        progressTime[4] = "1642143011000"
//        progressTime[5] = "1642229411000"
//        progressTime[6] = "1642315811000"
//
//        mHistogramView.setProgress(progressValue, progressTime ,HistogramView.WEEK)
    }

    private fun observer() {
        viewModel.getDataByDay.observe(this) {
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
        }

        viewModel.getDailyListByDateRange.observe(this) {
            dismissDialog()
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        var temp = weekList
                        var type = HistogramView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                weekList = it.data
                                temp = weekList
                                type = HistogramView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                monthList = it.data
                                temp = monthList
                                type = HistogramView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        var temp = weekList
                        var type = HistogramView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                weekList = null
                                temp = weekList
                                type = HistogramView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                monthList = null
                                temp = monthList
                                type = HistogramView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        var temp: Nothing? = null
                        var type = HistogramView.WEEK
                        when (showDateType) {
                            Global.DateType.WEEK -> {
                                type = HistogramView.WEEK
                            }
                            Global.DateType.MONTH -> {
                                type = HistogramView.MONTH
                            }
                            else -> {}
                        }
                        showWeekOrMonthData(temp, type)
                    }
                }
            }
        }
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

    private fun getDayData() {
        dismissDialog()
        viewModel.getDataByDay(curDay)
        dialog?.show()
    }

    private fun showDayData() {
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        binding.dailySelect.tvDate.text = DateUtils.getStringDate(DateUtils.getLongTime(curDay, DateUtils.TIME_YYYY_MM_DD), DateUtils.TIMEYYYYMMDD_SLASH_RAIL)
        var totalStep = ""
        var totalDistance = ""
        var totalCalorie = ""
        var stepData: List<String> = emptyList()
        var distanceData: List<String> = emptyList()
        var calorieData: List<String> = emptyList()
        var max = 0
        val progressValue = FloatArray(24)
        val progressTime = arrayOfNulls<String>(24)
        var totalDataUnit = ""
        var yesterdayStep = ""    //	int	昨日运动总步数
        var yesterdayDistance = ""    //	double	昨日运动总距离
        var yesterdayDistanceTemp = "--"    //	double	昨日运动总距离
        var yesterdayCalorie = ""
        var stepSum = ""
        var distanceSum = ""
        var calorieSum = ""
        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var showDefaultTimeTips = ""
        var totalDistanceResult = ""

        if (dayList != null && dayList!!.todayData.toInt() == 1) {
            totalStep = dayList?.totalStep?.trim().toString()
            totalDistance = dayList?.totalDistance?.trim().toString()
            totalCalorie = dayList?.totalCalorie?.trim()?.toFloat()?.toInt().toString()
            yesterdayStep = dayList?.yesterdayStep?.trim().toString()
            yesterdayDistance = dayList?.yesterdayDistance?.trim().toString()
            yesterdayCalorie = dayList?.yesterdayCalorie?.trim().toString()
            stepData = dayList?.stepData?.split(",")!!
            distanceData = dayList?.distanceData?.split(",")!!
            calorieData = dayList?.calorieData?.split(",")!!

            totalStep = if (TextStringUtils.isNull(totalStep)) {
                "0"
            } else {
                totalStep
            }

            yesterdayStep = if (TextStringUtils.isNull(yesterdayStep)) {
                "0"
            } else {
                yesterdayStep
            }

            totalCalorie = if (TextStringUtils.isNull(totalCalorie)) {
                "0"
            } else {
                totalCalorie
            }

            yesterdayCalorie = if (TextStringUtils.isNull(yesterdayCalorie)) {
                "0"
            } else {
                yesterdayCalorie
            }

            stepSum = if (!TextUtils.isEmpty(totalStep) && !TextUtils.isEmpty(yesterdayStep)) {
                if (totalStep.toInt() > yesterdayStep.toInt()) {
                    (totalStep.toInt() - yesterdayStep.toInt()).toString()
                } else {
                    (totalStep.toInt() - yesterdayStep.toInt()).toString()
                }
            } else {
                totalStep
            }

            distanceSum = if (!TextUtils.isEmpty(totalDistance) && !TextUtils.isEmpty(yesterdayDistance)) {
                if (totalDistance.trim().toFloat() > yesterdayDistance.trim().toFloat()) {
                    UnitConverUtils.showDistanceKmStyle((totalDistance.toFloat() - yesterdayDistance.toFloat()).toString())
                } else {
                    "-${UnitConverUtils.showDistanceKmStyle(abs((totalDistance.toFloat() - yesterdayDistance.toFloat())).toString())}"
                }
            } else {
                UnitConverUtils.showDistanceKmStyle(totalDistance)
            }

            yesterdayDistanceTemp = UnitConverUtils.showDistanceKmStyle(yesterdayDistance)
            yesterdayDistanceTemp = if (TextStringUtils.isNull(yesterdayDistanceTemp)) {
                "0.00"
            } else {
                yesterdayDistanceTemp
            }

            calorieSum = if (!TextUtils.isEmpty(totalCalorie) && !TextUtils.isEmpty(yesterdayCalorie)) {
                if (totalCalorie.toFloat() > yesterdayCalorie.toFloat()) {
                    (totalCalorie.toFloat() - yesterdayCalorie.toFloat()).toString()
                } else {
                    (totalCalorie.toFloat() - yesterdayCalorie.toFloat()).toString()
                }
            } else {
                totalCalorie
            }
            totalDistanceResult = UnitConverUtils.showDistanceKmStyle(totalDistance)
            totalDistanceResult = if (TextStringUtils.isNull(totalDistanceResult)) {
                "0"
            } else {
                totalDistanceResult
            }

            showDefaultTimeTips = "00:00-01:00"
            val temp = curDay.trim().split("-")
            showDefaultDateTips = "${temp[1]}/${temp[2]}"
        } else {
            for (i in 0 until 24) {
                progressValue[i] = 0f
            }
//            binding.lyDailyRightTips.visibility = View.GONE
            totalStep = "0"
            totalCalorie = "0"
            totalDistanceResult = "0.00"
            yesterdayStep = "0"
            yesterdayDistanceTemp = "0.00"
            yesterdayCalorie = "0"
            stepSum = "0"
            distanceSum = "0.00"
            calorieSum = "0"
        }

        when (type) {
            DailyDataType.STEPS -> {
                totalDataUnit = getString(R.string.unit_step)
                if (ControlBleTools.getInstance().isConnect && isCurrentDay) {
                    totalDistanceResult = UnitConverUtils.showDistanceKmStyle(currentDistanceData)
                    totalDistanceResult = if (TextStringUtils.isNull(totalDistanceResult)) {
                        "0"
                    } else {
                        totalDistanceResult
                    }
                    showTotal(totalDistanceResult, currentCaloriesData, currentStepData, totalDataUnit)
                } else {
                    showTotal(totalDistanceResult, totalCalorie, totalStep, totalDataUnit)
                }
                showBottom(totalStep, yesterdayStep, totalDataUnit, stepSum)
                if (stepData.isNotEmpty()) {
                    showDefaultDataTips = stepData[0]
                    for (i in 0 until 24) {
                        if (stepData.isNotEmpty()) {
                            progressValue[i] = stepData[i].toFloat()
                        } else {
                            progressValue[i] = 0f
                        }
                    }
                }
            }
            DailyDataType.DISTANCE -> {
//                totalDataUnit = getString(R.string.healthy_sports_item_distance_unit)
                totalDataUnit = showDistanceUnitStyle(this)
                if (ControlBleTools.getInstance().isConnect && isCurrentDay) {
                    totalDistanceResult = UnitConverUtils.showDistanceKmStyle(currentDistanceData)
                    totalDistanceResult = if (TextStringUtils.isNull(totalDistanceResult)) {
                        "0"
                    } else {
                        totalDistanceResult
                    }
                    showTotal(currentStepData, currentCaloriesData, totalDistanceResult, totalDataUnit)
                } else {
                    showTotal(totalStep, totalCalorie, totalDistanceResult, totalDataUnit)
                }
                showBottom(totalDistanceResult, yesterdayDistanceTemp, totalDataUnit, distanceSum)
                if (distanceData.isNotEmpty()) {
//                    showDefaultDataTips = distanceData[0]
                    showDefaultDataTips = UnitConverUtils.showDistanceKmStyle(distanceData[0])
                    for (i in 0 until 24) {
                        if (distanceData.isNotEmpty()) {
//                            progressValue[i] = distanceData[i].toFloat()
                            progressValue[i] = distanceData[i].trim().toFloat()
                        } else {
                            progressValue[i] = 0f
                        }
                    }
                }
            }
            DailyDataType.CALORIES -> {
                totalDataUnit = getString(R.string.unit_calories)

                if (ControlBleTools.getInstance().isConnect && isCurrentDay) {
                    totalDistanceResult = UnitConverUtils.showDistanceKmStyle(currentDistanceData)
                    totalDistanceResult = if (TextStringUtils.isNull(totalDistanceResult)) {
                        "0"
                    } else {
                        totalDistanceResult
                    }
                    showTotal(currentStepData, totalDistanceResult, currentCaloriesData, totalDataUnit)
                } else {
                    showTotal(totalStep, totalDistanceResult, totalCalorie, totalDataUnit)
                }
                showBottom(totalCalorie, yesterdayCalorie, totalDataUnit, calorieSum)
                if (calorieData.isNotEmpty()) {
                    showDefaultDataTips = calorieData[0]
                    for (i in 0 until 24) {
                        if (calorieData.isNotEmpty()) {
                            progressValue[i] = calorieData[i].toFloat()
                        } else {
                            progressValue[i] = 0f
                        }
                    }
                }
            }
        }

//        showTotalSum(showDefaultDataTips, showDefaultDateTips, showDefaultTimeTips)
        for (i in 0 until 24) {
            progressTime[i] = i.toString()
        }

        for (i in progressValue.indices) {
            if (progressValue[i] > max) {
                max = progressValue[i].toInt()
            }
        }
        binding.mHistogramView.setMaxText(max.toString())
        when (type) {
            DailyDataType.STEPS, DailyDataType.CALORIES -> {
                binding.mHistogramView.setProgress(progressValue, progressTime, HistogramView.TODAY)
            }
            DailyDataType.DISTANCE -> {
                val mTargetBean = TargetBean().getData()
                //英制
                if (mTargetBean.unit != "0") {
                    binding.mHistogramView.setProgress(progressValue, progressTime, HistogramView.TODAY, 1610, true)
                }
                //公制
                else {
                    binding.mHistogramView.setProgress(progressValue, progressTime, HistogramView.TODAY, 1000, true)
                }
            }
        }
    }

    private fun showTotal(tvLeftTotalSum: String, tvRightTotalSum: String, tvDistance: String, unit: String) {
        var unitStr = unit
        if (tvLeftTotalSum.isNotEmpty()) {
            binding.tvLeftTotalSum.text = "$tvLeftTotalSum"
        } else {
            binding.tvLeftTotalSum.text = "--"
        }

        if (tvRightTotalSum.isNotEmpty()) {
            binding.tvRightTotalSum.text = "$tvRightTotalSum"
        } else {
            binding.tvRightTotalSum.text = "--"
        }

        when (type) {
            DailyDataType.STEPS -> {
                unitStr = if (tvDistance.trim().toInt() <= 0) {
                    getString(R.string.unit_step)
                } else {
                    getString(R.string.unit_steps)
                }
            }
            DailyDataType.DISTANCE, DailyDataType.CALORIES -> {
                binding.tvLeftTotalSumUnit.text = if (tvLeftTotalSum.trim().toInt() <= 0) {
                    getString(R.string.unit_step)
                } else {
                    getString(R.string.unit_steps)
                }
            }
        }

        if (tvDistance.isNotEmpty()) {
            binding.tvDistance.text = SpannableStringTool.get()
                .append(tvDistance)
                .setFontSize(24f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
                .append(" ")
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .append(unitStr)
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
                .append(unitStr)
                .setFontSize(12f)
                .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
                .create()
        }
    }

    private fun showBottom(thisData: String, lastData: String, unit: String, results: String) {
        var thisStr = ""
        var lastStr = ""
        var unitStr = unit

        when (showDateType) {
            Global.DateType.TODAY -> {
                thisStr = getString(R.string.daily_bottom_tips_center_today) + ":"
                lastStr = getString(R.string.daily_bottom_tips_center_yesterday) + ":"
            }
            Global.DateType.WEEK -> {
                thisStr = getString(R.string.daily_bottom_tips_center_this_week) + ":"
                lastStr = getString(R.string.daily_bottom_tips_center_last_week) + ":"
            }
            Global.DateType.MONTH -> {
                thisStr = getString(R.string.daily_bottom_tips_center_this_month) + ":"
                lastStr = getString(R.string.daily_bottom_tips_center_last_month) + ":"
            }
        }
        binding.tvBottomLeftThisTime.text = thisStr
        binding.tvBottomLeftLastTime.text = lastStr
        binding.tvBottomRightThisTimeData.text = thisData
        binding.tvBottomRightLastTimeData.text = lastData
        binding.tvBottomRightThisTimeUnit.text = unit
        binding.tvBottomRightLastTimeUnit.text = unit

        when (type) {
            DailyDataType.STEPS -> {
                if (thisData.trim().toInt() <= 0) {
                    binding.tvBottomRightThisTimeUnit.text = getString(R.string.unit_step)
                } else {
                    binding.tvBottomRightThisTimeUnit.text = getString(R.string.unit_steps)
                }
                if (lastData.trim().toInt() <= 0) {
                    binding.tvBottomRightLastTimeUnit.text = getString(R.string.unit_step)
                } else {
                    binding.tvBottomRightLastTimeUnit.text = getString(R.string.unit_steps)
                }
            }
            else -> {}
        }


        var showTipsStr = "--"/*getString(R.string.daily_bottom_tips_left)*/
        /*var actionStr = ""
        actionStr = when(type){
            DailyDataType.STEPS -> {
                getString(R.string.go_tips)
            }
            DailyDataType.DISTANCE -> {
                getString(R.string.go_tips)
            }
            DailyDataType.CALORIES -> {
                getString(R.string.consumed_tips)
            }
        }*/

        if (results.contains("--")) {
            binding.tvDifferenceBetweenData.text = "--"
        } else {
            if (results.trim().isNotEmpty()) {
                when {
                    /*results.toFloat() > 0->{
                        showTipsStr = "$showTipsStr$thisStr${getString(R.string.daily_bottom_than_tips)}$lastStr${getString(R.string.many_go_tips)}" +
                                "$actionStr$results$unit"
                    }
                    results.toFloat() < 0->{
                        showTipsStr = "$showTipsStr$thisStr${getString(R.string.daily_bottom_than_tips)}$lastStr${getString(R.string.less_go_tips)}" +
                                "$actionStr${results.replace("-" , "")}$unit"
                    }*/
                    else -> {
                        //val andStr = getString(R.string.daily_bottom_and_tips)
                        when (type) {
                            DailyDataType.STEPS -> {
                                //showTipsStr = "$showTipsStr$thisStr$andStr$lastStr${getString(R.string.steps_equal_tips)}"
                                showTipsStr = when (showDateType) {
                                    Global.DateType.TODAY -> {
                                        getString(R.string.daily_bottom_steps_yesterday_tips)
                                    }
                                    Global.DateType.WEEK -> {
                                        getString(R.string.daily_bottom_steps_last_week_tips)
                                    }
                                    Global.DateType.MONTH -> {
                                        getString(R.string.daily_bottom_steps_last_month_tips)
                                    }
                                }
                            }
                            DailyDataType.DISTANCE -> {
                                //showTipsStr = "$showTipsStr$thisStr$andStr$lastStr${getString(R.string.distance_equal_tips)}"
                                showTipsStr = when (showDateType) {
                                    Global.DateType.TODAY -> {
                                        getString(R.string.daily_bottom_distance_yesterday_tips)
                                    }
                                    Global.DateType.WEEK -> {
                                        getString(R.string.daily_bottom_distance_last_week_tips)
                                    }
                                    Global.DateType.MONTH -> {
                                        getString(R.string.daily_bottom_distance_last_month_tips)
                                    }
                                }
                            }
                            DailyDataType.CALORIES -> {
                                //showTipsStr = "$showTipsStr$thisStr$andStr$lastStr${getString(R.string.calories_equal_tips)}"
                                showTipsStr = when (showDateType) {
                                    Global.DateType.TODAY -> {
                                        getString(R.string.daily_bottom_calories_yesterday_tips)
                                    }
                                    Global.DateType.WEEK -> {
                                        getString(R.string.daily_bottom_calories_last_week_tips)
                                    }
                                    Global.DateType.MONTH -> {
                                        getString(R.string.daily_bottom_calories_last_month_tips)
                                    }
                                }
                            }
                        }
                    }
                }
                binding.tvDifferenceBetweenData.text = showTipsStr
            } else {
                binding.tvDifferenceBetweenData.text = "--"
            }
        }
    }

    private fun showTotalSum(data: String, date: String? = null, time: String) {
        binding.tvTotalSum.text = data
        if (date != null) {
            binding.tvTotalDate.text = date
        }
        binding.tvTotalSumTime.text = time
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
                selectDate = "$beginTemp - $endTemp"
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
        viewModel.getDailyListByDateRange(beginDay, endDay, type)
        dismissDialog()
        dialog?.show()
    }

    private fun showWeekOrMonthData(data: DailyListResponse?, typeHistogram: Int) {
        Log.i(TAG, "showWeekOrMonthData()")
//        binding.lyDailyRightTips.visibility = View.VISIBLE
        binding.lyDailyRightTips.visibility = View.GONE
        val calendar = Calendar.getInstance()
        var beginDay = ""
        var endDay = ""
        var length = 0
        var max = 0
        var totalStep = ""
        var totalDistance = ""
        var totalCalorie = ""
        //上周/上月总步数
        var previousStep = ""
        //上周/上月总距离
        var previousDistance = ""
        //上周/上月总卡路里
        var previousCalorie = ""
        var totalDataUnit = ""
        var stepSum = ""
        var distanceSum = ""
        var calorieSum = ""

        var tvLeftTotalSum = "--"
        var tvRightTotalSum = "--"
        var tvDistance = "--"
        var thisData = "--"
        var lastData = "--"
        var results = "--"

        var showDefaultDataTips = ""
        var showDefaultDateTips = ""
        var totalDistanceResult = ""

        when (showDateType) {
            Global.DateType.WEEK -> {
                beginDay = DateUtils.getDayOfWeekMonday(curWeek)
                calendar.timeInMillis = DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                endDay = DateUtils.getStringDate(calendar.timeInMillis, DateUtils.TIME_YYYY_MM_DD)
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

        when (type) {
            DailyDataType.STEPS -> {
                totalDataUnit = getString(R.string.unit_step)
            }
            DailyDataType.DISTANCE -> {
                totalDataUnit = showDistanceUnitStyle(this)
            }
            DailyDataType.CALORIES -> {
                totalDataUnit = getString(R.string.unit_calories)
            }
        }

        if (data != null && data.dataList.isNotEmpty()) {
            totalStep = data.step
            totalDistance = data.distance
            totalCalorie = data.calorie
            previousStep = data.previousStep
            previousDistance = data.previousDistance
            previousCalorie = data.previousCalorie

            stepSum = if (totalStep.toInt() > previousStep.toInt()) {
                (totalStep.toInt() - previousStep.toInt()).toString()
            } else {
                (totalStep.toInt() - previousStep.toInt()).toString()
            }

//            distanceSum = if (totalDistance.toFloat() > previousDistance.toFloat()){
//                (totalDistance.toFloat() - previousDistance.toFloat()).toString()
//            }else{
//                (totalDistance.toFloat() - previousDistance.toFloat()).toString()
//            }

            distanceSum = if (!TextUtils.isEmpty(totalDistance) && !TextUtils.isEmpty(previousDistance)) {
                if (totalDistance.trim().toFloat() > previousDistance.trim().toFloat()) {
                    UnitConverUtils.showDistanceKmStyle((totalDistance.toFloat() - previousDistance.toFloat()).toString())
                } else {
                    "-${UnitConverUtils.showDistanceKmStyle(abs((totalDistance.toFloat() - previousDistance.toFloat())).toString())}"
                }
            } else {
                UnitConverUtils.showDistanceKmStyle(totalDistance)
            }

            calorieSum = if (totalCalorie.toFloat() > previousCalorie.toFloat()) {
                (totalCalorie.toFloat() - previousCalorie.toFloat()).toString()
            } else {
                (totalCalorie.toFloat() - previousCalorie.toFloat()).toString()
            }
            totalDistanceResult = UnitConverUtils.showDistanceKmStyle(totalDistance)

            when (type) {
                DailyDataType.STEPS -> {
//                    totalDataUnit = getString(R.string.unit_step)

                    tvLeftTotalSum = totalDistanceResult
                    tvRightTotalSum = totalCalorie.toFloat().toInt().toString()
                    tvDistance = totalStep

                    thisData = totalStep
                    lastData = previousStep
                    results = stepSum

                    showDefaultDataTips = data.dataList[0].totalStep
                }
                DailyDataType.DISTANCE -> {
//                    totalDataUnit = getString(R.string.healthy_sports_item_distance_unit)
//                    totalDataUnit = showDistanceUnitStyle(this)
                    tvLeftTotalSum = totalStep
                    tvRightTotalSum = totalCalorie.toFloat().toInt().toString()
                    tvDistance = totalDistanceResult

                    thisData = totalDistanceResult
                    lastData = UnitConverUtils.showDistanceKmStyle(previousDistance)
                    results = distanceSum
                    showDefaultDataTips = UnitConverUtils.showDistanceKmStyle(data.dataList[0].totalDistance)
                }
                DailyDataType.CALORIES -> {
//                    totalDataUnit = getString(R.string.unit_calories)

                    tvLeftTotalSum = totalStep
                    tvRightTotalSum = totalDistanceResult
                    tvDistance = totalCalorie.toFloat().toInt().toString()

                    thisData = totalCalorie.toFloat().toInt().toString()
                    lastData = previousCalorie.toFloat().toInt().toString()
                    results = calorieSum.toFloat().toInt().toString()
                    showDefaultDataTips = data.dataList[0].totalCalorie.toFloat().toInt().toString()
                }
            }

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
                    when (type) {
                        DailyDataType.STEPS -> {
                            progressValue[i] = data.dataList[index].totalStep.trim().toFloat()
                        }
                        DailyDataType.DISTANCE -> {
                            progressValue[i] = data.dataList[index].totalDistance.toFloat()
                        }
                        DailyDataType.CALORIES -> {
                            progressValue[i] = data.dataList[index].totalCalorie.trim().toFloat()
                        }
                    }
                } else {
                    progressValue[i] = 0f
                }
            }
        } else {
            when (type) {
                DailyDataType.STEPS -> {
                    tvLeftTotalSum = "0.00"
                    tvRightTotalSum = "0"
                    tvDistance = "0"
                    thisData = "0"
                    results = "0"
                    lastData = data?.previousStep ?: "0"
                }
                DailyDataType.DISTANCE -> {
                    tvLeftTotalSum = "0"
                    tvRightTotalSum = "0"
                    tvDistance = "0.00"
                    thisData = "0.00"
                    results = "0"
                    lastData = UnitConverUtils.showDistanceKmStyle(data?.previousDistance ?: "0.0")
                }
                DailyDataType.CALORIES -> {
                    tvLeftTotalSum = "0"
                    tvRightTotalSum = "0.00"
                    tvDistance = "0"
                    thisData = "0"
                    results = "0"
                    if (data?.previousCalorie != null) {
                        lastData = data.previousCalorie.toFloat().toInt().toString()
                    } else {
                        lastData = "0"
                    }
                }
            }
//            binding.lyDailyRightTips.visibility = View.GONE
        }

        showTotal(tvLeftTotalSum, tvRightTotalSum, tvDistance, totalDataUnit)
        showBottom(thisData, lastData, totalDataUnit, results)
        showDefaultDateTips = DateUtils.getStringDate(DateUtils.getLongTime(beginDay, DateUtils.TIME_YYYY_MM_DD), "yyyy/MM/dd")
//        showTotalSum(showDefaultDataTips, showDefaultDateTips, "")

        for (i in progressValue.indices) {
            if (progressValue[i] > max) {
                max = progressValue[i].toInt()
            }
        }
        binding.mHistogramView.setMaxText(max.toString())

        when (type) {
            DailyDataType.STEPS, DailyDataType.CALORIES -> {
                binding.mHistogramView.setProgress(progressValue, progressTime, typeHistogram)
            }
            DailyDataType.DISTANCE -> {
                val mTargetBean = TargetBean().getData()
                //英制
                if (mTargetBean.unit != "0") {
                    binding.mHistogramView.setProgress(progressValue, progressTime, typeHistogram, 1610, true)
                }
                //公制
                else {
                    binding.mHistogramView.setProgress(progressValue, progressTime, typeHistogram, 1000, true)
                }
            }
        }
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