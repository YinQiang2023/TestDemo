package com.smartwear.xzfit.utils

import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.ThreadUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.RealTimeBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.RealTimeDataCallBack
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.ui.HealthyFragment
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.healthy.bean.SyncDailyInfoBean
import com.smartwear.xzfit.ui.healthy.history.womenhealth.CalcCycleDataUtils
import com.smartwear.xzfit.ui.livedata.RefreshHealthyMainData
import org.greenrobot.eventbus.EventBus

object RealTimeRefreshDataUtils {
    private const val TAG = "RealTimeRefreshingDataUtils"
    var isRefreshing = false
    var isInconsistent = false
    private var lastSteps = "0"
    var realTimeStep = "0"
    var syncStep = "0"
    var realTimeCalories = "0"
    var realTimeDistance = "0.00"

    init {
        CallBackUtils.realTimeDataCallback = object :
            RealTimeDataCallBack {
            override fun onResult(bean: RealTimeBean) {
                if (bean != null) {
                    //如果程序进入后台后，只要收到设备实时同步数据，即关闭同步实时数据开关，数据不作处理
                    if (BaseApplication.isIntoBackground) {
                        closeRealTime()
                        return
                    }

                    val stepPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_step
                        )
                    }
                    val caloriesPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_calories
                        )
                    }
                    val distancePosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_distance
                        )
                    }
                    val sleepPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_sleep
                        )
                    }
                    val heartRatePosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_heart
                        )
                    }
                    val bloodOxygenPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_blood_oxygen
                        )
                    }
                    //有效站立
                    val effectiveStandPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_effective_stand
                        )
                    }

                    val womenHealthPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_women_health
                        )
                    }

                    val pressurePosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_pressure_title
                        )
                    }

                    realTimeStep = if (bean.steps.isNullOrEmpty()) "0" else bean.steps
                    realTimeCalories = if (bean.calories.isNullOrEmpty()) "0" else bean.calories
                    realTimeDistance = if (bean.distance.isNullOrEmpty() || bean.distance == "0") {
                        "0.00"
                    } else {
                        bean.distance
                    }

                    val steps = realTimeStep
                    val calories = realTimeCalories
                    val distance = realTimeDistance

                    if (stepPosition != -1) {
//                        isInconsistent = Global.healthyItemList[stepPosition].context == steps
                        isInconsistent = syncStep == steps
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[stepPosition].context = steps
                        }
                    }

                    if (caloriesPosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[caloriesPosition].context = calories
                        }
                    }

                    if (distancePosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[distancePosition].context = distance
                        }
                    }
                    if (sleepPosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[sleepPosition].context = bean.sleepDuration
                        }
                    }

                    if (heartRatePosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[heartRatePosition].context = bean.heartRate
                        }
                    }
                    if (bloodOxygenPosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[bloodOxygenPosition].context = bean.bloodOxygen
                        }
                    }
                    //有效站立
                    if (effectiveStandPosition != -1) {
                        var temp = if (bean.effectiveStandingHour.trim().toInt() in 1..9) {
                            "0${
                                bean.effectiveStandingHour.trim().toInt() - 1
                            }:00 - 0${bean.effectiveStandingHour.trim().toInt()}:00"
                        } else if (bean.effectiveStandingHour.trim().toInt() >= 10) {
                            if (bean.effectiveStandingHour.trim().toInt() == 24) {
                                "${bean.effectiveStandingHour.trim().toInt() - 1}:00 - 00:00"
                            } else {
                                "${
                                    bean.effectiveStandingHour.trim().toInt() - 1
                                }:00 - ${bean.effectiveStandingHour.trim().toInt()}:00"
                            }
                        } else {
                            ""
                        }
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[effectiveStandPosition].context = temp
                        }
                    }

                    //压力
                    if (pressurePosition != -1) {
                        if (HealthyFragment.viewIsVisible) {
                            Global.healthyItemList[pressurePosition].context = bean.pressure ?: "0"
                        }
                    }


                    if (womenHealthPosition != -1) {
                        var data = bean.physiologicalCycle
                        Global.physiologicalCycleBean = bean.physiologicalCycle
                        CalcCycleDataUtils.loadCycleData()
                        var date = DateUtils.getStringDate(
                            System.currentTimeMillis(),
                            DateUtils.TIME_YYYY_MM_DD
                        )
                        val array = date.trim().split("-")
                        if (array.size >= 3) {
                            val year = array[0].trim().toInt()
                            val month = array[1].trim().toInt()
                            val day = array[2].trim().toInt()
                            CalcCycleDataUtils.getCycData(year, month, day)
                        }
//                            if (data.preset){
//                                Global.healthyItemList[womenHealthPosition].context = mContext.getString(R.string.healthy_sports_list_women_health_context)
//                                Global.healthyItemList[womenHealthPosition].bottomText = Global.womenHealthSumSafetyPeriod.toString()
//                            }else{
//                                Global.healthyItemList[womenHealthPosition].context = ""
//                            }
                    }


                    val batteryInfo = RealTimeBean.DeviceBatteryInfo()
                    batteryInfo.capacity = bean.batteryInfo.capacity
                    batteryInfo.chargeStatus = bean.batteryInfo.chargeStatus

                    val syncDailyBean = SyncDailyInfoBean()
                    syncDailyBean.steps = steps
                    syncDailyBean.calories = calories
                    syncDailyBean.distance = distance
                    ThreadUtils.runOnUiThread {
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_REFRESH_BATTERY_INFO, batteryInfo))

                        if (HealthyFragment.viewIsVisible) {
                            RefreshHealthyMainData.postValue(true)
                            EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYNC_DAILY_INFO, syncDailyBean))
                        }
//                        if (TimerUtils.calcTimer(System.currentTimeMillis()) && (!isInconsistent && HealthyFragment.viewIsVisible)) {
//                            Log.e("RealTimeRefreshingDataUtils" , "isInconsistent = $isInconsistent")
//                            isRefreshing = true
//                            SendCmdUtils.refreshSend()
//                        }
                        if (TimerUtils.calcTimer(System.currentTimeMillis())) {
                            if (!isInconsistent) {
                                if (HealthyFragment.viewIsVisible) {
                                    isRefreshing = true
                                    syncStep = steps
                                    SendCmdUtils.refreshSend()
                                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_SYNC))
                                }
                            }
                        }

                    }
                }
            }

            override fun onFail() {
                ThreadUtils.runOnUiThread {
                    RefreshHealthyMainData.postValue(false)
                }
            }
        }
    }

    private var timeHandler = Handler(Looper.getMainLooper())
    private const val delayedRealTime = 3000L

    @JvmStatic
    fun openRealTime() {
        timeHandler.removeCallbacksAndMessages(null)
        timeHandler.postDelayed({
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().realTimeDataSwitch(true, null)
            }
        }, delayedRealTime)
    }

    @JvmStatic
    fun closeRealTime() {
        timeHandler.removeCallbacksAndMessages(null)
        timeHandler.postDelayed({
            ControlBleTools.getInstance().realTimeDataSwitch(false, null)
        }, delayedRealTime)
    }
}