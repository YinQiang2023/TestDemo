package com.jwei.xzfit.utils

import android.os.Build
import android.text.TextUtils
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.TimeUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.UserInfo
import com.zhapp.ble.callback.ZHInitStatusCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.db.model.sport.SportModleInfo
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.service.MyNotificationsService
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean
import com.jwei.xzfit.ui.livedata.RefreshHealthyFragment
import com.jwei.xzfit.ui.user.bean.TargetBean
import com.jwei.xzfit.ui.user.bean.UserBean
import com.jwei.xzfit.utils.manager.AppTrackingManager
import org.litepal.LitePal

object SendCmdUtils {

    private const val TAG = "SendCmdUtils"
    private var mDelayTime = 0L

    //主页连接设备刷新发送
    fun refreshSend() {
        if (ControlBleTools.getInstance().isConnect && !Global.IS_BIND_DEVICE) {
//            if (System.currentTimeMillis() - mDelayTime < 500) return
//            mDelayTime = System.currentTimeMillis()
            com.blankj.utilcode.util.LogUtils.e("onRefreshAction SendCmdUtils refreshSend")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getStartTypeTrack("同步数据"), isStart = true)
            val trackingLog = TrackingLog.getDevTyepTrack("同步时间", "同步时间", "SET_SYSTEM_TIME")
            //下拉发送时间给设备 TODO 不能放在同步日常数据之后
            val time = System.currentTimeMillis()
            SpUtils.setValue(SpUtils.TIME_SET_SEND_TIME, "$time")
            ControlBleTools.getInstance().setTime(
                time,
                object : SendCmdStateListener() {
                    override fun onState(state: SendCmdState?) {
                        trackingLog.log = "setTime:$time; state : $state"
                        trackingLog.endTime = TrackingLog.getNowString()

                        if (state == SendCmdState.SUCCEED) {
                            SpUtils.setValue(SpUtils.TIME_SET_SUCCESS_TIME, "${System.currentTimeMillis()}")
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        } else {
                            if (ControlBleTools.getInstance().isConnect) {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                                    log += "\n同步时间失败/超时"
                                }, "1415", true)
                            }
                        }
                    }
                }
            )
            getDeviceInfo()
            getDeviceBatteryInfo()
            getDailyHistoryData()
            getDisconnectReason()
            getDevTrackingLog()
        }
    }

    fun getDailyHistoryData() {
        if (ControlBleTools.getInstance().isConnect) {
            ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_SYNC)
            val trackingLog = TrackingLog.getDevTyepTrack("同步设备日常数据", "获取日常数据ID集合", "GET_FITNESS_TYPE_ID_LIST")
            ControlBleTools.getInstance().getDailyHistoryData(object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState) {

                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.log = "state : $state"

                    if (state != SendCmdState.SUCCEED) {
                        LogUtils.d(TAG, "sync cmd send failed:$state", true)
                        if (state == SendCmdState.TIMEOUT && ControlBleTools.getInstance().isConnect) {
                            ErrorUtils.onLogResult("sync cmd send failed:$state")
                            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                                log += "\n同步设备日常数据失败/超时"
                            }, "1424", true)
                        }
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                    }
                }
            })
        }
    }

//    fun sendUserInfo() {
//        if (ControlBleTools.getInstance().isConnect) {
//            val mUserBean = UserBean().getData()
//            val mTarget = TargetBean().getData()
//
//            val distanceUnit = if (TextUtils.isEmpty(mTarget.unit)) 1 else mTarget.unit.toInt()
//
//            val userProfile = UserInfo()
//            userProfile.age = -1
//            userProfile.birthday = DateUtils.getLongTime(mUserBean.birthDate, DateUtils.TIME_YYYY_MM_DD).toInt()
//            userProfile.calGoal = mTarget.consumeTarget.toInt()
//            userProfile.distanceGoal = mTarget.getDistanceTargetMi().toInt()
//            userProfile.standingTimesGoal = -1
//            userProfile.goalSleepMinute = mTarget.sleepTarget.toInt()
//            userProfile.stepGoal = mTarget.sportTarget.toInt()
//            userProfile.height = mUserBean.height.trim().toInt()
//            userProfile.maxHr = -1
//            userProfile.sex = if (mUserBean.sex.toInt() == 0) 1 else 2
//            userProfile.weight = mUserBean.weight.trim().toFloat()
//
//            ControlBleTools.getInstance().setTemperatureUnit(mTarget.temperature.trim().toInt())
//            ControlBleTools.getInstance().setUserProfile(userProfile)
//            ControlBleTools.getInstance().setDistanceUnit(distanceUnit)
//        }
//    }

    fun setUserInformation() {
        if (ControlBleTools.getInstance().isConnect) {
            val mUserBean = UserBean().getData()
            val mTarget = TargetBean().getData()

            val distanceUnit = if (TextUtils.isEmpty(mTarget.unit)) Constant.DISTANCE_TARGET_DEFAULT_VALUE.toInt() else mTarget.unit.toInt()
            val temperatureUnit = if (TextUtils.isEmpty(mTarget.temperature)) Constant.TEMPERATURE_DEFAULT_VALUE.toInt() else mTarget.temperature.toInt()

            val userProfile = UserInfo()
            userProfile.age =
                DateUtils.getAgeFromBirthTime(TimeUtils.string2Date(mUserBean.birthDate, com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat(DateUtils.TIME_YYYY_MM_DD)))
            userProfile.birthday =
                0 /* TODO 设备未用到此值 (TimeUtils.string2Millis(mUserBean.birthDate,DateUtils.TIME_YYYY_MM_DD)/1000).toInt()*/ /*TODO 设备异常越界 DateUtils.getLongTime(mUserBean.birthDate, DateUtils.TIME_YYYY_MM_DD).toInt()*/
            userProfile.calGoal = mTarget.consumeTarget.toInt()
            userProfile.distanceGoal = mTarget.getDistanceTargetMi().toInt()
            userProfile.standingTimesGoal = -1
            userProfile.goalSleepMinute = mTarget.sleepTarget.toInt()
            userProfile.stepGoal = mTarget.sportTarget.toInt()
            userProfile.height = mUserBean.height.trim().toInt()
            userProfile.maxHr = -1
            userProfile.sex = if (mUserBean.sex.toInt() == 0) 1 else 2
            userProfile.weight = mUserBean.weight.trim().toFloat()
            userProfile.appWeatherSwitch = SpUtils.getValue(SpUtils.WEATHER_SWITCH, "false").trim().toBoolean()
            LogUtils.d(TAG, "setUserInformation -- distanceUnit =$distanceUnit , temperatureUnit=$temperatureUnit,userProfile=${GsonUtils.toJson(userProfile)}")
            ControlBleTools.getInstance().setUserInformation(distanceUnit, temperatureUnit, userProfile, null)
        }
    }

    fun getSportData() {
        LogUtils.i(TAG, "getSportData()")
        val userId = SpUtils.getValue(SpUtils.USER_ID, "")
        if (userId.isNotEmpty()) {
            val newTime = TimeUtils.getNowMills() / 1000
            var sqlList = LitePal.where(
                "userId = ? and sportTime < ?", userId, "$newTime"
            ).limit(1).order("sportTime desc").find(SportModleInfo::class.java, true)
            com.blankj.utilcode.util.LogUtils.d("首页运动记录---》${sqlList.size}")
            //LogUtils.json(sqlList)
            if (sqlList != null && sqlList.size > 0) {
                val info = sqlList.get(0)
                val index = Global.healthyItemList.indexOfFirst {
                    it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_sports_list_sport_record)
                }
                if (index != -1) {
                    Global.healthyItemList.get(index).apply {
                        context = "${info.burnCalories}"
                        bottomText = "${info.sportTime * 1000}"
                        subTitleText = SportTypeUtils.getSportTypeName(info.dataSources, info.exerciseType)
                    }
                    RefreshHealthyFragment.postValue(true)
                }
            }
        }
    }

    fun getDeviceInfo() {
        if (ControlBleTools.getInstance().isConnect) {
            val trackingLog = TrackingLog.getDevTyepTrack("获取设备信息", "获取设备信息", "GET_DEVICE_INFO")
            ControlBleTools.getInstance().getDeviceInfo(object : SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.log = "state : $state"
                    if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n获取设备信息失败/超时"
                        }, "1413", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                    }
                }
            })
        }
    }

    fun getDeviceBatteryInfo() {
        if (ControlBleTools.getInstance().isConnect) {
            val trackingLog = TrackingLog.getDevTyepTrack("获取设备电量信息", "获取设备电量信息", "GET_DEVICE_BATTERY_STATUS")
            ControlBleTools.getInstance().getDeviceBattery(object : SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    trackingLog.endTime = TrackingLog.getNowString()
                    trackingLog.log = "state : $state"
                    if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                            log += "\n获取设备电量信息失败/超时"
                        }, "1413", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                    }
                }
            })
        }
    }

    fun getDeviceLanguage() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getLanguageList(null)
        }
    }

    fun getDisconnectReason() {
        //产品功能列表
        val deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(SpUtils.DEVICE_SETTING, ""),
            DeviceSettingBean::class.java
        )
        if (deviceSettingBean != null && deviceSettingBean.settingsRelated != null && deviceSettingBean.settingsRelated.device_disconnection_log) {
            if (ControlBleTools.getInstance().isConnect) {
                val trackingLog = TrackingLog.getDevTyepTrack("获取设备断连原因", "获取设备断连原因", "GET_DEVICE_DISCONNECT_REASON")
                ControlBleTools.getInstance().getDisconnectReason(object : SendCmdStateListener() {
                    override fun onState(state: SendCmdState?) {
                        trackingLog.endTime = TrackingLog.getNowString()
                        trackingLog.log = "state : $state"
                        if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                                log += "\n获取设备断连原因失败/超时"
                            }, "1416", true)
                        } else {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        }
                    }
                })
            }
        }
    }

    private fun getDevTrackingLog() {
        val deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(SpUtils.DEVICE_SETTING, ""),
            DeviceSettingBean::class.java
        )
        if (deviceSettingBean != null && deviceSettingBean.settingsRelated != null && (deviceSettingBean.settingsRelated.dev_trace_switch)) {
            if (ControlBleTools.getInstance().isConnect) {
                val trackingLog = TrackingLog.getDevTyepTrack("获取设备埋点日志", "获取设备埋点日志", "PHONE_REQUEST_UPLOAD_BURIED_FILES")
                ControlBleTools.getInstance().getFirmwareTrackingLog(object : SendCmdStateListener() {
                    override fun onState(state: SendCmdState?) {
                        trackingLog.endTime = TrackingLog.getNowString()
                        trackingLog.log = "state : $state"
                        if (state != SendCmdState.SUCCEED && state != SendCmdState.NOT_SUPPORT && state != SendCmdState.UNINITIALIZED && ControlBleTools.getInstance().isConnect) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog.apply {
                                log += "\n获取设备埋点日志失败/超时"
                            }, "1428", true)
                        } else {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
                        }
                    }
                })
            }
        }
    }

    fun connectDevice(name: String, address: String, isActiveConnection: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_BLE12)) {
                if (!SpUtils.getSPUtilsInstance().getBoolean(SpUtils.FIRST_MAIN_PERMISSION_EXPLAIN, false)) return
                if (MyNotificationsService.isShowingRequestNotification) return
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getStartTypeTrack("重连"), isStart = true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("发起连接").apply {
                    log = "connect() name:$name,address:$address"
                })
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("蓝牙权限未允许或拒绝"), "1312", true)

                com.jwei.xzfit.utils.PermissionUtils.checkRequestPermissions(
                    null,
                    BaseApplication.mContext.getString(R.string.permission_bluetooth),
                    com.jwei.xzfit.utils.PermissionUtils.PERMISSION_BLE12
                ) {
                    connectDevice(name, address, isActiveConnection)
                }
                return
            }
        }
        if(!ControlBleTools.getInstance().isSdkServiceRunning(BaseApplication.application)){
            ControlBleTools.getInstance().init(BaseApplication.application, ZHInitStatusCallBack {
                connectDevice(name, address, isActiveConnection)
            })
            return
        }
        LogUtils.e(TAG, "connect device name = $name address = $address")
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_CONNECT)

        val mtu = SpUtils.getValue(SpUtils.THE_SERVER_MTU_INFO_MTU, "0")
        val maxValue = SpUtils.getValue(SpUtils.THE_SERVER_MTU_INFO_MAX_VALUE, "0")
        val minValue = SpUtils.getValue(SpUtils.THE_SERVER_MTU_INFO_MIN_VALUE, "0")
        val sendInterval = SpUtils.getSPUtilsInstance().getInt(SpUtils.THE_SERVER_PACK_SEND_INTERVAL, -1)
        if (mtu.trim().toInt() > 0) {
            ControlBleTools.getInstance().setMtu(mtu.trim().toInt(), maxValue.trim().toInt(), minValue.trim().toInt())
        } else {
            ControlBleTools.getInstance().setMtu(0, 0, 0)
        }
        if (sendInterval != -1) {
            ControlBleTools.getInstance().setCmdDelayTime(sendInterval)
        }
        ControlBleTools.getInstance().connect(name, address)
    }

}