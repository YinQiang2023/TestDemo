package com.jwei.xzfit.ui.healthy.history.womenhealth

import android.text.TextUtils
import com.haibin.calendarview.Calendar
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.livedata.RefreshHealthyMainData
import com.jwei.xzfit.utils.DateUtils
import com.jwei.xzfit.utils.SpUtils
import java.util.*

object CalcCycleDataUtils {

    //计算Cycl数据
    fun loadCycleData() {
        var know_cyc: Int = Global.physiologicalCycleBean?.totalCycleDay!!
        var know_period: Int = Global.physiologicalCycleBean?.physiologicalCycleDay!!
        if (know_cyc < 15 || know_cyc > 100) {
            know_cyc = 28
        }
        if (know_period < 1 || know_period > 60) {
            know_period = 5
        }
        //经期天数
        var day_period = 0
        //安全期1天数
        var day_security_one = 0
        //危险期
        var day_danger = 0
        //安全期2天数
        var day_security_two = 0

//===========================================================
        //经期天数
        day_period = know_period
        //安全期2天数-固定
        day_security_two = 9
        //系数1-固定
        val coe_1 = 10
        //安全期1天数
        day_security_one = know_cyc - day_period - coe_1 - day_security_two

        //当安全期1小于0，也就是负数的时候
        if (day_security_one < 0) {
            day_danger = coe_1 + day_security_one
            day_security_one = 0
            //否则大于等于0
        } else {
            day_danger = coe_1
        }

        SpUtils.setValue(SpUtils.WOMEN_HEALTH_MENSTRUAL_PERIOD_DAY, day_period.toString())
        SpUtils.setValue(SpUtils.WOMEN_HEALTH_OVIPOSIT_PERIOD_DAY, day_danger.toString())
        SpUtils.setValue(SpUtils.WOMEN_HEALTH_SAFETY1_PERIOD_DAY, day_security_one.toString())
        SpUtils.setValue(SpUtils.WOMEN_HEALTH_SAFETY2_PERIOD_DAY, day_security_two.toString())
    }

    var TODAY_COLOCR = -0x10000

    fun getCycData(vYear: Int, vMonth: Int, vDay: Int): Map<String, Calendar>? {
        val todayYear: Int = vYear
        val todayMonth: Int = vMonth
        val todayDay: Int = vDay
        val period: Int = SpUtils.getValue(SpUtils.WOMEN_HEALTH_MENSTRUAL_PERIOD_DAY, "0").trim().toInt()
        val one: Int = SpUtils.getValue(SpUtils.WOMEN_HEALTH_SAFETY1_PERIOD_DAY, "0").trim().toInt()
        val danger: Int = SpUtils.getValue(SpUtils.WOMEN_HEALTH_OVIPOSIT_PERIOD_DAY, "0").trim().toInt()
        val two: Int = SpUtils.getValue(SpUtils.WOMEN_HEALTH_SAFETY2_PERIOD_DAY, "0").trim().toInt()
        val date = Global.physiologicalCycleBean?.physiologicalStartDate
        val dateStr = "${date?.year}-${date?.month}-${date?.day}"
        var cycle_start_date: String = dateStr
        if (TextUtils.isEmpty(cycle_start_date)) {
            cycle_start_date = getTime()
        }
        var my_year = cycle_start_date.split("-").toTypedArray()[0].toInt()
        var my_mon = cycle_start_date.split("-").toTypedArray()[1].toInt()
        var my_day = cycle_start_date.split("-").toTypedArray()[2].toInt()

        val map: MutableMap<String, Calendar> = HashMap()
        val max_lenght = period + one + danger + two
        val number_2 = period + one
        val number_3 = period + one + danger
        val number_4 = period + one + danger + two
        val years: Int = my_year
        val month: Int = my_mon
        val day: Int = my_day
        var is_start = false
        var xiabiao = 0
        var sumSafetyPeriod = 0
        val list = mutableListOf<Calendar>()
        if (max_lenght <= 0) return null
        for (y in my_year until my_year + 3) {
            for (m in 1..12) {
//                获取某月的天数
                val d_max: Int = DateUtils.getMonthDaysCount(y, m)
                for (d in 1..d_max) {
                    if (years == y && month == m && day == d) {
                        is_start = true
                    }
                    if (is_start) {
                        var number = xiabiao % max_lenght
                        number = number + 1
                        if (number > 0 && number <= period) {
                            map[getSchemeCalendar(y, m, d, Global.ONE_BG_COLOR, Global.ONE_TYPE).toString()] =
                                getSchemeCalendar(y, m, d, Global.ONE_BG_COLOR, Global.ONE_TYPE)
                            list.add(getSchemeCalendar(y, m, d, Global.ONE_BG_COLOR, Global.ONE_TYPE))
                            if (y == todayYear && m == todayMonth && d == todayDay) {
                                TODAY_COLOCR = Global.ONE_TEXT_COLOR
                            }
                        } else if (number > period && number <= number_2) {
                            map[getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.TWO_TYPE).toString()] =
                                getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.TWO_TYPE)
                            list.add(getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.TWO_TYPE))
                            if (y == todayYear && m == todayMonth && d == todayDay) {
                                TODAY_COLOCR = Global.TWO_TEXT_COLOR
                            }
                            if (y == todayYear && m == todayMonth && d > todayDay) {
                                sumSafetyPeriod++
                            }
                        } else if (number > number_2 && number <= number_3) {
                            map[getSchemeCalendar(y, m, d, Global.THREE_BG_COLOR, Global.THREE_TYPE).toString()] =
                                getSchemeCalendar(y, m, d, Global.THREE_BG_COLOR, Global.THREE_TYPE)
                            list.add(getSchemeCalendar(y, m, d, Global.THREE_BG_COLOR, Global.THREE_TYPE))
                            if (y == todayYear && m == todayMonth && d == todayDay) {
                                TODAY_COLOCR = Global.THREE_TEXT_COLOR
                            }
                        } else if (number > number_3 && number <= number_4) {
                            map[getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.TWO_TYPE).toString()] =
                                getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.TWO_TYPE)
                            list.add(getSchemeCalendar(y, m, d, Global.TWO_BG_COLOR, Global.FOUR_TYPE))
                            if (y == todayYear && m == todayMonth && d == todayDay) {
                                TODAY_COLOCR = Global.TWO_TEXT_COLOR
                            }
                            if (y == todayYear && m == todayMonth && d > todayDay) {
                                sumSafetyPeriod++
                            }
                        }
                        xiabiao += 1
                    }
                }
            }
        }
        com.blankj.utilcode.util.LogUtils.d("sumSafetyPeriod", "$sumSafetyPeriod  -----------------------")

        var isOneType = false
        var isTwoType = false
        var isThreeType = false
        var isFourType = false
        var sum = 0
        var currentType = Global.ONE_TYPE
        var nextType = ""
        for (i in list.indices) {
            if (list[i].year == todayYear && list[i].month == todayMonth && list[i].day == todayDay) {
                currentType = when (list[i].scheme) {
                    Global.ONE_TYPE -> {
                        isOneType = true
                        isTwoType = false
                        isThreeType = false
                        isFourType = false
                        Global.ONE_TYPE
                    }
                    Global.TWO_TYPE -> {
                        isOneType = false
                        isTwoType = true
                        isThreeType = false
                        isFourType = false
                        Global.TWO_TYPE
                    }
                    Global.THREE_TYPE -> {
                        isOneType = false
                        isTwoType = false
                        isThreeType = true
                        isFourType = false
                        Global.THREE_TYPE
                    }
                    Global.FOUR_TYPE -> {
                        isOneType = false
                        isTwoType = false
                        isThreeType = false
                        isFourType = true
                        Global.FOUR_TYPE
                    }
                    else -> {
                        isOneType = true
                        isTwoType = false
                        isThreeType = false
                        isFourType = false
                        Global.ONE_TYPE
                    }
                }
            }
            if (list[i].year == todayYear &&
                list[i].month == todayMonth &&
                list[i].day > todayDay &&
                list[i].scheme == currentType
            ) {
                sum++
            } else if (list[i].year == todayYear &&
                list[i].month == todayMonth &&
                list[i].day > todayDay &&
                list[i].scheme != currentType
            ) {
                nextType = getNextTypeByscheme(list[i].scheme)
                //结束循环
                break
            } else if (TextUtils.isEmpty(nextType) && list[i].year == todayYear && list[i].month > todayMonth) {
                //当前周期是本月最后一个周期，没有下一周期数据，查询下月前几天
                if (list[i].scheme == currentType) {
                    sum++
                } else if (list[i].scheme != currentType) {
                    nextType = getNextTypeByscheme(list[i].scheme)
                    //结束循环
                    break
                }
            }
        }

        sum++ //http://jira.wearheart.cn/browse/INA-2291?filter=-1
        SpUtils.setValue(SpUtils.WOMEN_HEALTH_SUM_SAFETY_PERIOD, sum.toString())
        Global.womenHealthSumSafetyPeriod = sum

        val womenHealthPosition = Global.healthyItemList.indexOfFirst {
            it.topTitleText == BaseApplication.mContext.getString(
                R.string.healthy_sports_list_women_health
            )
        }
        if (womenHealthPosition != -1 && Global.physiologicalCycleBean != null) {

            if (Global.physiologicalCycleBean?.preset!!) {
                when {
                    isOneType -> {
                        Global.healthyItemList[womenHealthPosition].context = BaseApplication.mContext.getString(R.string.healthy_sports_list_women_health_context)
                    }
                    isTwoType -> {
                        Global.healthyItemList[womenHealthPosition].context = BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time)
                    }
                    isThreeType -> {
                        Global.healthyItemList[womenHealthPosition].context = BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_ovulatory_time)
                    }
                    isFourType -> {
                        Global.healthyItemList[womenHealthPosition].context = BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time)
                    }
                }
                Global.healthyItemList[womenHealthPosition].bottomText = when (nextType) {
                    BaseApplication.mContext.getString(R.string.healthy_sports_list_women_health_context) -> {
                        BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time_yet_tips1)
                    }
                    BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time) -> {
                        BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time_yet_tips2)
                    }
                    BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_ovulatory_time) -> {
                        BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time_yet_tips3)
                    }
                    else -> {
                        BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time_yet_tips1)
                    }
                }
                Global.healthyItemList[womenHealthPosition].bottomText2 = Global.womenHealthSumSafetyPeriod.toString()
//                Global.healthyItemList[womenHealthPosition].bottomText = dayData.toString()
                RefreshHealthyMainData.postValue(true)
            }
        }

        return map
    }

    private fun getNextTypeByscheme(scheme: String): String {
        return when (scheme) {
            Global.ONE_TYPE -> { //月经期
                BaseApplication.mContext.getString(R.string.healthy_sports_list_women_health_context)
            }
            Global.TWO_TYPE -> { //安全期
                BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time)
            }
            Global.THREE_TYPE -> { //排卵期
                BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_ovulatory_time)
            }
            Global.FOUR_TYPE -> {  //安全期
                BaseApplication.mContext.getString(R.string.healthy_sports_item_women_health_safety_time)
            }
            else -> { //月经期
                BaseApplication.mContext.getString(R.string.healthy_sports_list_women_health_context)
            }
        }
    }


    private fun getTime(): String {
        val format = com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd")
        val date = Date()
        return format.format(date)
    }

    private fun getSchemeCalendar(
        year: Int,
        month: Int,
        day: Int,
        color: Int,
        text: String,
    ): Calendar {
        val calendar = Calendar()
        calendar.year = year
        calendar.month = month
        calendar.day = day;
        calendar.schemeColor = color//如果单独标记颜色、则会使用这个颜色
        calendar.scheme = text
        return calendar
    }

}