package com.jwei.publicone.viewmodel

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.PathUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ScanDeviceBean
import com.zhapp.ble.bean.WatchFaceListBean
import com.zhapp.ble.bean.WorldClockBean
import com.zhapp.ble.callback.ScanDeviceCallBack
import com.zhapp.ble.callback.WatchFaceListCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.base.UnFlawedLiveData
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.MyRetrofitClient
import com.jwei.publicone.https.Response
import com.jwei.publicone.https.download.DownloadListener
import com.jwei.publicone.https.download.DownloadManager
import com.jwei.publicone.https.params.*
import com.jwei.publicone.https.response.*
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.ui.device.DeviceSettingLiveData
import com.jwei.publicone.ui.device.bean.BulkDownloadListener
import com.jwei.publicone.ui.device.bean.DeviceSettingBean
import com.jwei.publicone.ui.device.bean.WatchSystemBean
import com.jwei.publicone.ui.device.bean.WorldClockItem
import com.jwei.publicone.ui.device.bean.diydial.MyDiyDialUtils
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.*
import com.jwei.publicone.utils.manager.AppTrackingManager
import kotlinx.coroutines.*
import okhttp3.internal.platform.Jdk9Platform
import org.greenrobot.eventbus.EventBus
import retrofit2.HttpException
import java.io.File
import java.lang.ref.WeakReference
import java.text.Collator
import java.util.*
import kotlin.coroutines.resume


/**
 * Created by android
 * on 2021/7/16
 */
class DeviceModel : BaseViewModel() {
    private val TAG: String = DeviceModel::class.java.simpleName
    val deviceSettingLiveData = DeviceSettingLiveData.instance
    val scanDevice = MutableLiveData(0)

    fun checkProductList(): Boolean {
        return Global.productList.size > 0;
    }

    fun startScanDevice(lists: ArrayList<ScanDeviceBean>) {
//        if (Global.productList.size <= 0) {
//            getProductList()
//        }
        ControlBleTools.getInstance().startScanDevice(object : ScanDeviceCallBack {
            override fun onBleScan(device: ScanDeviceBean) {
//                if (device.name.contains(CommonAttributes.PLUS_VIBE_PRO) || device.name.contains(CommonAttributes.DEVICE_E15)) {
                for (i in DeviceManager.dataList.indices) {
                    if (DeviceManager.dataList[i].deviceMac == device.address) {
                        return
                    }
                }
                val index = Global.productList.indexOfFirst { it.deviceType == device.deviceType }
                if (index != -1) {
                    for (i in 0 until lists.size) {
                        if (lists[i].address.equals(device.address)) {
                            return
                        }
                    }
                    LogUtils.w(TAG, "startScanDevice serviceData = ${GsonUtils.toJson(device)}")
                    lists.add(device)
                    scanDevice.postValue(1)
                }
            }
        })
    }

    fun stopScanDevice() {
        ControlBleTools.getInstance().stopScanDevice()
    }

    //该方法目前只有绑定设备时调用
    fun connect(name: String, address: String) {
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_BINDDEVICE)
        SendCmdUtils.connectDevice(name, address)
    }

    val bindDeviceCode = MutableLiveData("")
    fun bindDevice(
        deviceType: String,
        deviceMac: String,
        deviceName: String,
        deviceSn: String,
        firmwareVersion: String,
    ) {
        launchUI {
            try {
                val bean = BindDeviceBean()
                bean.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(bean.userId)) {
                    return@launchUI
                }
                bean.deviceType = deviceType
                bean.deviceMac = deviceMac
                bean.deviceName = deviceName
                bean.deviceSn = deviceSn
                bean.deviceVersion = firmwareVersion
                val result = MyRetrofitClient.service.bindDevice(
                    JsonUtils.getRequestJson(TAG, bean, BindDeviceBean::class.java)
                )
                LogUtils.e(TAG, "bindDevice result = $result")
                bindDeviceCode.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "bindDevice e =$e", true)
                bindDeviceCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    val versionInfo = MutableLiveData("")
    fun versionInfo(deviceType: String, firmwareVersion: String) {
        launchUI {
            try {
                val bean = VersionInfoBean()
                bean.deviceType = deviceType
                bean.deviceVersion = firmwareVersion
                val result = MyRetrofitClient.service.versionInfo(
                    JsonUtils.getRequestJson(
                        TAG,
                        bean,
                        VersionInfoBean::class.java
                    )
                )
                LogUtils.e(TAG, "versionInfo result = $result")
                when (result.code) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        LogUtils.e(TAG, result.data.toString())
                        val dataBean = DeviceSettingBean()
                        LogUtils.d(
                            TAG,
                            "versionInfo result.data = ${GsonUtils.toJson(result.data)}"
                        )
                        dataBean.bluetoothName = result.data.bluetoothName
                        dataBean.pushLengthLimit = result.data.pushLengthLimit
                        dataBean.deviceVersion = result.data.deviceVersion
                        dataBean.deviceType = result.data.deviceType
                        val reminderRelated = result.data.reminderRelated.trim().split(",")
                        val settingsRelated = result.data.settingsRelated.trim().split(",")
                        val functionRelated = result.data.functionRelated.trim().split(",")
                        val dataRelated = result.data.dataRelated.trim().split(",")
                        dataBean.reminderRelated.alarm_clock =
                            reminderRelated.contains("alarm_clock")
                        dataBean.reminderRelated.notification =
                            reminderRelated.contains("notification")
                        dataBean.reminderRelated.incoming_call_rejection =
                            reminderRelated.contains("incoming_call_rejection")
                        dataBean.reminderRelated.sedentary = reminderRelated.contains("sedentary")
                        dataBean.reminderRelated.drink_water =
                            reminderRelated.contains("drink_water")
                        dataBean.reminderRelated.reminder_to_take_medicine =
                            reminderRelated.contains("reminder_to_take_medicine")
                        dataBean.reminderRelated.hand_washing_reminder =
                            reminderRelated.contains("hand_washing_reminder")
                        dataBean.reminderRelated.meeting = reminderRelated.contains("meeting")
                        dataBean.reminderRelated.event_reminder =
                            reminderRelated.contains("event_reminder")
                        dataBean.reminderRelated.quick_reply =
                            reminderRelated.contains("quick_reply")
                        dataBean.reminderRelated.heart_rate_warning =
                            reminderRelated.contains("heart_rate_warning")

                        dataBean.settingsRelated.step_goal = settingsRelated.contains("step_goal")
                        dataBean.settingsRelated.calorie_goal =
                            settingsRelated.contains("calorie_goal")
                        dataBean.settingsRelated.distance_target =
                            settingsRelated.contains("distance_target")
                        dataBean.settingsRelated.sleep_goal = settingsRelated.contains("sleep_goal")
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.settingsRelated.wearing_method = settingsRelated.contains("wearing_method")
                        dataBean.settingsRelated.wearing_method = false
                        dataBean.settingsRelated.language = settingsRelated.contains("language")
                        dataBean.settingsRelated.raise_your_wrist_to_brighten_the_screen =
                            settingsRelated.contains("raise_your_wrist_to_brighten_the_screen")
                        dataBean.settingsRelated.continuous_heart_rate_switch =
                            settingsRelated.contains("continuous_heart_rate_switch")
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.settingsRelated.continuous_blood_oxygen_switch = settingsRelated.contains("continuous_blood_oxygen_switch")
                        //dataBean.settingsRelated.continuous_body_temperature_switch = settingsRelated.contains("continuous_body_temperature_switch")
                        dataBean.settingsRelated.continuous_blood_oxygen_switch = false
                        dataBean.settingsRelated.continuous_body_temperature_switch = false
                        dataBean.settingsRelated.sleep_rapid_eye_movement_switch =
                            settingsRelated.contains("sleep_rapid_eye_movement_switch")
                        dataBean.settingsRelated.do_not_disturb =
                            settingsRelated.contains("do_not_disturb")
                        dataBean.settingsRelated.bright_adjustment =
                            settingsRelated.contains("bright_adjustment")
                        dataBean.settingsRelated.vibration_adjustment =
                            settingsRelated.contains("vibration_adjustment")
                        dataBean.settingsRelated.off_screen_display =
                            settingsRelated.contains("off_screen_display")
                        dataBean.settingsRelated.bright_screen_time =
                            settingsRelated.contains("bright_screen_time")
                        dataBean.settingsRelated.cover_the_screen_off =
                            settingsRelated.contains("cover_the_screen_off")
                        dataBean.settingsRelated.double_click_to_brighten_the_screen =
                            settingsRelated.contains("double_click_to_brighten_the_screen")
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.settingsRelated.notification_does_not_turn_on = settingsRelated.contains("notification_does_not_turn_on")
                        dataBean.settingsRelated.notification_does_not_turn_on = false
                        dataBean.settingsRelated.application_list_sorting =
                            settingsRelated.contains("application_list_sorting")
                        dataBean.settingsRelated.card_sort_list =
                            settingsRelated.contains("card_sort_list")
                        dataBean.settingsRelated.Strava = settingsRelated.contains("Strava")
                        //TODO 上架屏蔽 - 开发中
//                        dataBean.settingsRelated.GoogleFit = settingsRelated.contains("GoogleFit")
                        dataBean.settingsRelated.GoogleFit = false
                        //其它APP定制
                        dataBean.settingsRelated.sleep_mode_settings = false

                        dataBean.settingsRelated.world_clock = settingsRelated.contains("world_clock")
                        dataBean.settingsRelated.multi_sport_sorting = settingsRelated.contains("multi_sport_sorting")
                        dataBean.settingsRelated.device_disconnection_log = settingsRelated.contains("device_disconnection_log")
                        //TODO 上架屏蔽 - 开发中
                        //dataBean.settingsRelated.dev_trace_switch = settingsRelated.contains("dev_trace_switch")
                        dataBean.settingsRelated.dev_trace_switch = false

                        dataBean.functionRelated.find_phone = functionRelated.contains("find_phone")
                        dataBean.functionRelated.find_device =
                            functionRelated.contains("find_device")
                        dataBean.functionRelated.shake_and_shake_to_take_pictures =
                            functionRelated.contains("shake_and_shake_to_take_pictures")
                        dataBean.functionRelated.music_control =
                            functionRelated.contains("music_control")
                        dataBean.functionRelated.dial = functionRelated.contains("dial")
                        dataBean.functionRelated.contacts = functionRelated.contains("contacts")
                        dataBean.functionRelated.altitude = functionRelated.contains("altitude")

                        dataBean.functionRelated.weather = functionRelated.contains("weather")
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.functionRelated.secondary_screen_movement = functionRelated.contains("secondary_screen_movement")
                        dataBean.functionRelated.secondary_screen_movement = false
                        dataBean.functionRelated.auxiliary_exercise =
                            functionRelated.contains("auxiliary_exercise")
                        dataBean.functionRelated.AGPS = functionRelated.contains("AGPS")
                        dataBean.functionRelated.power_saving_mode = functionRelated.contains("power_saving_mode")
                        dataBean.functionRelated.binding_language = false/*functionRelated.contains("binding_language") 新版本已用引导页替代，该字段作废*/

                        dataBean.dataRelated.step_count = dataRelated.contains("step_count")
                        dataBean.dataRelated.calories = dataRelated.contains("calories")
                        dataBean.dataRelated.distance = dataRelated.contains("distance")
                        dataBean.dataRelated.continuous_heart_rate =
                            dataRelated.contains("continuous_heart_rate")
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.dataRelated.offline_heart_rate = dataRelated.contains("offline_heart_rate")
                        dataBean.dataRelated.offline_heart_rate = false
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.dataRelated.continuous_body_temperature = dataRelated.contains("continuous_body_temperature")
                        //dataBean.dataRelated.offline_body_temperature = dataRelated.contains("offline_body_temperature")
                        dataBean.dataRelated.continuous_body_temperature = false
                        dataBean.dataRelated.offline_body_temperature = false
                        dataBean.dataRelated.offline_blood_oxygen =
                            dataRelated.contains("offline_blood_oxygen")
                        dataBean.dataRelated.menstrual_cycle =
                            dataRelated.contains("menstrual_cycle")
                        dataBean.dataRelated.effective_standing =
                            dataRelated.contains("effective_standing")
                        //TODO 上架屏蔽 - 开发中
//                        dataBean.dataRelated.ecg = dataRelated.contains("ecg")
//                        dataBean.dataRelated.blood_pressure = dataRelated.contains("blood_pressure")
                        dataBean.dataRelated.ecg = false//屏蔽心电,屏蔽ECG
                        dataBean.dataRelated.blood_pressure = false//屏蔽血压
                        //TODO 上架屏蔽 - 未开发
                        //dataBean.dataRelated.continuous_blood_oxygen = dataRelated.contains("continuous_blood_oxygen")
                        dataBean.dataRelated.continuous_blood_oxygen = false
                        //TODO 上架屏蔽 - 开发中
                        dataBean.dataRelated.pressure = false
//                        dataBean.dataRelated.pressure = dataRelated.contains("pressure")
                        //TODO 上架屏蔽 - 开发中
                        dataBean.dataRelated.continuous_pressure = false
//                        dataBean.dataRelated.continuous_pressure = dataRelated.contains("continuous_pressure")
                        dataBean.dataRelated.offline_pressure =
                            dataRelated.contains("offline_pressure")

                        dataBean.bluetoothName = result.data.bluetoothName
                        SpUtils.getSPUtilsInstance().remove(SpUtils.DEVICE_SETTING)
                        SpUtils.setValue(SpUtils.DEVICE_SETTING, JSON.toJSONString(dataBean))
                        Global.fillListData()
                        EventBus.getDefault()
                            .post(EventMessage(EventAction.ACTION_REF_DEVICE_SETTING))
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        SpUtils.getSPUtilsInstance().remove(SpUtils.DEVICE_SETTING)
                        Global.fillListData()
                        EventBus.getDefault()
                            .post(EventMessage(EventAction.ACTION_REF_DEVICE_SETTING))
                    }

                }
                versionInfo.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "versionInfo e =$e", true)
                versionInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    val unbindDeviceCode = MutableLiveData("")
    fun unbindDevice(deviceType: String, deviceMac: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val bean = BindDeviceBean()
                bean.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(bean.userId)) {
                    return@launchUI
                }
                bean.deviceType = deviceType
                bean.deviceMac = deviceMac

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(bean)
                    }
                }

                val result = MyRetrofitClient.service.unbindDevice(JsonUtils.getRequestJson(bean, BindDeviceBean::class.java))
                LogUtils.e(TAG, "unbindDevice result = $result")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                unbindDeviceCode.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "unbindDevice e =$e", true)

                val result = Response("", "unbindDevice e =$e", HttpCommonAttributes.SERVER_ERROR, "", QureyLoginAccountResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                unbindDeviceCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    val getBindListCode = MutableLiveData("")
    fun getBindList(isVersionInfo: Boolean = true) {
        launchUI {
            try {
                val bean = BindListBean()
                bean.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(bean.userId)) {
                    return@launchUI
                }
                val result = MyRetrofitClient.service.getBindList(
                    JsonUtils.getRequestJson(
                        TAG,
                        bean,
                        BindListBean::class.java
                    )
                )
                LogUtils.e(TAG, "getBindList result = $result")

//                if (isSwitchDevice){
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    if (result.data != null) result
                    val temp = result.data.dataList
                    if (temp != null) {
                        temp.sortByDescending { it.deviceStatus }
                        DeviceManager.dataList.clear()
                        DeviceManager.saveBindList(temp)
                    }
                    for (i in DeviceManager.dataList.indices) {
                        if (DeviceManager.dataList[i].deviceStatus == 1) {
                            Global.deviceType = DeviceManager.dataList[i].deviceType.toString()
//                            Global.deviceVersion = DeviceManager.dataList[i].deviceVersion
                            Global.deviceMac = DeviceManager.dataList[i].deviceMac
                            Global.deviceSn = DeviceManager.dataList[i].deviceSn
                            val version = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_VERSION, "")
                            if (isVersionInfo) {
                                async {
                                    if (version.isNotEmpty()) {
                                        versionInfo(
                                            DeviceManager.dataList[i].deviceType.toString(),
                                            version
                                        )
                                    } else {
                                        versionInfo(
                                            DeviceManager.dataList[i].deviceType.toString(),
                                            DeviceManager.dataList[i].deviceVersion
                                        )
                                    }
                                }
                                async { getProductInfo(isSave = true) }
                            }
                        }
                    }
                } else {
                    DeviceManager.dataList.clear()
                    Global.deviceSettingBean = null
                    SpUtils.getSPUtilsInstance().remove(SpUtils.DEVICE_SETTING)
//                    Global.noDataFillList()
//                    Global.saveMainList()
                    Global.fillListData()
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_DEVICE_SETTING))
                }
//                }
                getBindListCode.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "getBindList e =$e", true)
                getBindListCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //查询设备快捷回复数据
    fun getDeviceShortReply(callBack: ParsingStateManager.SendCmdStateListener?) {
        ControlBleTools.getInstance().getDevShortReplyData(callBack)
    }

    //设置快捷回复信息
    fun postDeviceShortReply(
        datas: ArrayList<String>,
        callBack: ParsingStateManager.SendCmdStateListener?,
    ) {
        ControlBleTools.getInstance().setDevShortReplyData(datas, callBack)
    }

    //获取产品设备号列表
    val getProductList = MutableLiveData("")
    fun getProductList(vararg tracks: TrackingLog) {
        launchUI {
            try {
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = ""
                    }
                }
                val result = MyRetrofitClient.service.getProductList()
                LogUtils.e(TAG, "getProductList result = $result")
                Global.productList.addAll(result.data.dataList)

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                getProductList.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "getProductList e =$e", true)

                val result = Response("", "getProductList e =$e", HttpCommonAttributes.SERVER_ERROR, "", QureyLoginAccountResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                getProductList.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //设备启用
    val enableDevice = MutableLiveData("")
    fun enableDevice(id: String, vararg tracks: TrackingLog) {
        LogUtils.i(TAG, "enableDevice start")
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(EnableDeviceBean(userId, id))
                    }
                }

                val result = MyRetrofitClient.service.enableDevice(JsonUtils.getRequestJson(EnableDeviceBean(userId, id), EnableDeviceBean::class.java))
                LogUtils.i(TAG, "enableDevice result = $result")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                enableDevice.postValue(result.code)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "enableDevice e =$e", true)

                val result = Response("", "enableDevice e =$e", HttpCommonAttributes.SERVER_ERROR, "", QureyLoginAccountResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                enableDevice.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

//    //表盘支持语言
//    fun getLanguageCode(languageCode : String = "0") {
//        launchUI {
//            try {
//                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
//                if (TextUtils.isEmpty(userId)) {
//                    return@launchUI
//                }
//                val bean = GetDialListBean()
//                bean.userId = userId
//                bean.productNo = Global.deviceType
//                bean.productVersion = "1"
//                bean.languageCode = languageCode
//                val result = MyRetrofitClient.service.getLanguageCode(JsonUtils.getRequestJson(bean , GetDialListBean::class.java))
//                Log.e(TAG, "getHomeByProductList result = $result")
//                enableDevice.postValue(result.code)
//                userLoginOut(result.code)
//            } catch (e: Exception) {
//                Log.e(TAG, "getHomeByProductList e =$e")
//                enableDevice.postValue(HttpCommonAttributes.SERVER_ERROR)
//            }
//        }
//    }

    //首页表盘列表获取
    val getHomeByProductList = MutableLiveData<Response<GetDialListResponse>>()
    fun getHomeByProductList() {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                val bean = GetDialListBean()
                bean.userId = userId
                bean.productNo = Global.deviceType
                bean.productVersion = "1"
                bean.languageCode = if (AppUtils.isZh(BaseApplication.mContext)) "1" else "0"
                val result = MyRetrofitClient.service.getHomeByProductList(
                    JsonUtils.getRequestJson(
                        bean,
                        GetDialListBean::class.java
                    )
                )
                LogUtils.e(TAG, "getHomeByProductList result = $result")
                getHomeByProductList.postValue(result)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "getHomeByProductList e =$e", true)
                val result = Response("", "", HttpCommonAttributes.REQUEST_CODE_ERROR, "", GetDialListResponse())
                getHomeByProductList.postValue(result)
            }
        }
    }

    //表盘列表分类查询
    val moreDialPageByProductList = MutableLiveData<Response<MoreDialPageResponse>>()
    fun moreDialPageByProductList(dialId: String, index: String = "1", isDiy: Boolean = false) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                val bean = MoreDialPageBean()
                bean.userId = userId
                bean.productNo = Global.deviceType
                bean.productVersion = "1"
                bean.languageCode = if (AppUtils.isZh(BaseApplication.mContext)) "1" else "0"
                bean.dialTypeId = dialId
                bean.pageSize = "20"
                bean.pageIndex = index

                val result = if (isDiy)
                    MyRetrofitClient.service.getDiyPageList(
                        JsonUtils.getRequestJson(
                            bean,
                            MoreDialPageBean::class.java
                        )
                    ) else
                    MyRetrofitClient.service.moreDialPageByProductList(
                        JsonUtils.getRequestJson(
                            bean,
                            MoreDialPageBean::class.java
                        )
                    )
                LogUtils.e(TAG, "moreDialPageByProductList result = $result")
                moreDialPageByProductList.postValue(result)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "moreDialPageByProductList e =$e", true)
                val result = Response("", "", HttpCommonAttributes.REQUEST_CODE_ERROR, "", MoreDialPageResponse())
                moreDialPageByProductList.postValue(result)
            }
        }
    }

    //表盘详情
    val dialInfo = MutableLiveData<Response<DialInfoResponse>>()
    fun dialInfo(dialId: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                val bean = DialInfoBean()
                bean.userId = userId
                bean.dialId = dialId

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.startTime = TrackingLog.getNowString()
                        track.serReqJson = AppUtils.toSimpleJsonString(bean)
                    }
                }

                val result = MyRetrofitClient.service.dialInfo(JsonUtils.getRequestJson(bean, DialInfoBean::class.java))
                LogUtils.e(TAG, "dialInfo result = $result")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                dialInfo.postValue(result)

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.endTime = TrackingLog.getNowString()
                    }
                }
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "dialInfo e =$e")
                val msg = StringBuilder()
                if (e is HttpException) {
                    //获取对应statusCode和Message
                    val exception: HttpException = e as HttpException
                    val message = exception.response()?.message()
                    val code = exception.response()?.code()
                    if (code != null && message != null) {
                        msg.append("Http code = $code message: $message\n")
                    }
                }
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "dialInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                    ErrorUtils.onLogResult("dialInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className)
                    msg.append(e)
                }
                if (msg.isNotEmpty()) {
                    ErrorUtils.onLogResult("请求后台表盘详情数据失败：dialId：$dialId, msg:$msg network isAvailable:" + Jdk9Platform.isAvailable)
                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_REQUEST_BACKGROUND_DATA)
                }

                val result = Response("", "dialInfo e =$e", HttpCommonAttributes.SERVER_ERROR, "", DialInfoResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                dialInfo.postValue(result)

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.endTime = TrackingLog.getNowString()
                    }
                }
            }
        }
    }

    val getDialFromDevice = MutableLiveData<MutableList<WatchFaceListBean>>()
    fun getDialFromDeviceAndOnline() {
        Log.i(TAG, "getDialFromDeviceAndOnline")
        launchUI {
            val operationFromNet = async { getHomeByProductList() }
            var resultList: MutableList<WatchSystemBean> = mutableListOf()
            val operationFromDevice = async {
                ControlBleTools.getInstance().getWatchFaceList(/*object : WatchFaceListCallBack {
                    override fun onResponse(list: MutableList<WatchFaceListBean>) {
                        getDialFromDevice.postValue(list)
                        var dialCodes = StringBuffer()
                        for (i in list.indices) {
                            if (i == (list.size - 1)) {
                                dialCodes.append(list[i].id)
                            } else {
                                dialCodes.append("${list[i].id},")
                            }
                        }
                        getDialSystemInfo(dialCodes.toString())
                    }
                }*/MyWatchFaceListCallBack(this@DeviceModel)
                )
            }
            operationFromNet.await()
            operationFromDevice.await()
        }
    }

    fun getDialFromDevice() {
        launchUI {
            ControlBleTools.getInstance().getWatchFaceList(/*object : WatchFaceListCallBack {
                override fun onResponse(list: MutableList<WatchFaceListBean>) {
                    getDialFromDevice.postValue(list)
                    var dialCodes = StringBuffer()
                    for (i in list.indices) {
                        if (i == (list.size - 1)) {
                            dialCodes.append(list[i].id)
                        } else {
                            dialCodes.append("${list[i].id},")
                        }
                    }
                    getDialSystemInfo(dialCodes.toString())
                }
            }*/MyWatchFaceListCallBack(this@DeviceModel)
            )
        }
    }

    class MyWatchFaceListCallBack(viewModel: DeviceModel) : WatchFaceListCallBack {
        private var wrVm: WeakReference<DeviceModel>? = null

        init {
            wrVm = WeakReference(viewModel)
        }

        override fun onResponse(list: MutableList<WatchFaceListBean>) {
            wrVm?.get()?.apply {
                getDialFromDevice.postValue(list)
                var dialCodes = StringBuffer()
                for (i in list.indices) {
                    if (i == (list.size - 1)) {
                        dialCodes.append(list[i].id)
                    } else {
                        dialCodes.append("${list[i].id},")
                    }
                }
                getDialSystemInfo(dialCodes.toString())
            }
        }

        override fun timeOut() {
            wrVm?.get()?.apply {
                getDialFromDevice.postValue(mutableListOf())
            }
        }
    }

    //获取系统表盘列表
    val getDialSystemInfo = MutableLiveData<Response<DialSystemResponse>>()
    fun getDialSystemInfo(dialCodes: String) {
        Log.i(TAG, "getDialSystemInfo")
        launchUI {
            try {
                val bean = DialSystemBean()
                bean.dialCodes = dialCodes
                bean.deviceType = Global.deviceType
                val result = MyRetrofitClient.service.queryDialSystemList(
                    JsonUtils.getRequestJson(
                        TAG,
                        bean,
                        DialSystemBean::class.java
                    )
                )
                LogUtils.e(TAG, "getDialSystemInfo result = $result")
                getDialSystemInfo.postValue(result)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "getDialSystemInfo e =$e", true)
                val result = Response("", "", HttpCommonAttributes.REQUEST_CODE_ERROR, "", DialSystemResponse())
                getDialSystemInfo.postValue(result)
            }
        }
    }

    fun setDialDefault(id: String) {
        launchUI {
            ControlBleTools.getInstance().setDeviceWatchFromId(id, null)
        }
    }

    fun deleteDialDefault(id: String) {
        launchUI {
            ControlBleTools.getInstance().deleteDeviceWatchFromId(id, null)
        }
    }

    /**
     * 获取语言列表
     * */
    val devLanguageCode = MutableLiveData<String>()
    val devLanguageList = MutableLiveData<DeviceLanguageListResponse>()
    fun queryLanguageList(code: Int) {
        launchUI {
            try {
                val result = MyRetrofitClient.service.queryDeviceLanguageList(
                    JsonUtils.getRequestJson(
                        QueryDevLanguageListParam(code),
                        QueryDevLanguageListParam::class.java
                    )
                )
                com.blankj.utilcode.util.LogUtils.e(TAG, "devLanguageList result = $result")
                com.blankj.utilcode.util.LogUtils.json(result.data)
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    if (code == 0) {
                        Global.deviceLanguageList = result.data
                        LogUtils.e(TAG, "Global.deviceLanguageList = ${Global.deviceLanguageList}")
                    } else {
                        result.data?.let {
                            devLanguageList.postValue(it)
                        }
                    }
                }
                userLoginOut(result.code)
                devLanguageCode.postValue(result.code)
            } catch (e: Exception) {
                e.printStackTrace()
                devLanguageCode.postValue(HttpCommonAttributes.REQUEST_FAIL)
            }
        }
    }

    /**
     * 固件升级
     * */
    val queryFirewareCode = UnFlawedLiveData<String>()
    val firewareUpgradeData = UnFlawedLiveData<FirewareUpgradeResponse?>()
    fun checkFirewareUpgrade(vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                var firmwarePlatform = ""
                Global.productList.firstOrNull { it.deviceType == Global.deviceType }?.let {
                    firmwarePlatform = it.firmwarePlatform
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.startTime = TrackingLog.getNowString()
                        track.serReqJson = AppUtils.toSimpleJsonString(FirewareUpdateParam(Global.deviceType, Global.deviceVersion, firmwarePlatform, userId))
                    }
                }

                val result = MyRetrofitClient.service.queryFirewareUpgradeVersion(
                    JsonUtils.getRequestJson(
                        FirewareUpdateParam(
                            Global.deviceType,
                            Global.deviceVersion,
                            firmwarePlatform,
                            userId
                        ),
                        FirewareUpdateParam::class.java
                    )
                )
                LogUtils.e(TAG, "queryFirewareUpgradeVersion result = $result")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                queryFirewareCode.postValue(result.code)
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    val firewareUpgrade = result.data
                    LogUtils.e(TAG, firewareUpgrade.toString())
                    firewareUpgradeData.postValue(firewareUpgrade)
                } else {
                    val firewareUpgrade = FirewareUpgradeResponse().apply { id = if (result.code == HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA) "-1" else "" } //无数据
                    firewareUpgradeData.postValue(firewareUpgrade)
                }
                userLoginOut(result.code)
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = StringBuilder()
                if (e is HttpException) {
                    //获取对应statusCode和Message
                    val exception: HttpException = e as HttpException
                    val message = exception.response()?.message()
                    val code = exception.response()?.code()
                    if (code != null && message != null) {
                        msg.append("Http code = $code message: $message\n")
                    }
                }
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "checkFirewareUpgrade e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                    ErrorUtils.onLogResult("checkFirewareUpgrade e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className)
                    msg.append(e)
                }
                if (msg.isNotEmpty()) {
                    //请求前判断网络是否可用
                    NetworkUtils.isAvailableAsync {
                        ErrorUtils.onLogResult("请求后台OTA数据失败：msg:$msg network isAvailable:$it")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_REQUEST_BACKGROUND_DATA)
                    }
                }

                val firewareUpgrade = FirewareUpgradeResponse()
                val result = Response("", "checkFirewareUpgrade e =$e", HttpCommonAttributes.SERVER_ERROR, "", firewareUpgrade)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }
                firewareUpgradeData.postValue(firewareUpgrade)
            }
        }
    }
    /**
     * 产品设备信息
     * */
    val productInfo = MutableLiveData<Response<ProductInfoResponse>>()
    fun getProductInfo(deviceType: String = "", isSave: Boolean = false, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val productInfoBean = ProductInfoBean().apply {
                    if (TextStringUtils.isNull(deviceType)) {
                        this.deviceType = Global.deviceType
                    } else {
                        this.deviceType = deviceType
                    }
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(productInfoBean)
                    }
                }

                val result = MyRetrofitClient.service.productInfo(
                    JsonUtils.getRequestJson(TAG, productInfoBean, ProductInfoBean::class.java)
                )

                LogUtils.e(TAG, "getProductInfo result = $result")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                val info = result.data
                productInfo.postValue(result)
                LogUtils.e(TAG, "产品设备信息 -> " + GsonUtils.toJson(info))
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    LogUtils.e(
                        TAG,
                        "产品设备信息 -> mtu info = ${
                            result.data.mtuList?.toTypedArray()?.contentToString()
                        }"
                    )
                    if (isSave) {
                        LogUtils.w(TAG, "getProductInfo isSave = $isSave")
                        var mtu = "0"
                        var maxValue = "0"
                        var minValue = "0"
                        var isMatch = false
                        if (!result.data.mtuList.isNullOrEmpty()) {
                            for (i in result.data.mtuList!!.indices) {
                                if (result.data.mtuList!![i].phoneSystem == "2") {
                                    LogUtils.w(
                                        TAG,
                                        "getProductInfo phone device model = ${DeviceUtils.getModel()}"
                                    )
                                    if (result.data.mtuList!![i].phoneModel == DeviceUtils.getModel()) {
                                        isMatch = true
                                        mtu = result.data.mtuList!![i].mtu.trim()
                                        maxValue = result.data.mtuList!![i].maxValue.trim()
                                        minValue = result.data.mtuList!![i].minValue.trim()
                                    }
                                }
                            }
                            if (!isMatch) {
                                val position =
                                    result.data.mtuList!!.indexOfFirst { it -> it.phoneSystem == "2" && it.phoneModel == "通用" }
                                if (position != -1) {
                                    mtu = result.data.mtuList!![position].mtu.trim()
                                    maxValue = result.data.mtuList!![position].maxValue.trim()
                                    minValue = result.data.mtuList!![position].minValue.trim()
                                }
                            }
                            SpUtils.setValue(SpUtils.THE_SERVER_MTU_INFO_MTU, mtu)
                            SpUtils.setValue(SpUtils.THE_SERVER_MTU_INFO_MAX_VALUE, maxValue)
                            SpUtils.setValue(SpUtils.THE_SERVER_MTU_INFO_MIN_VALUE, minValue)
                            if (mtu.trim().toInt() > 0) {
                                ControlBleTools.getInstance().setMtu(
                                    mtu.trim().toInt(),
                                    maxValue.trim().toInt(),
                                    minValue.trim().toInt()
                                )
                            }
                        }
                        LogUtils.w(TAG, "getProductInfo gpsType = ${result.data.gpsType}")
                        if (TextUtils.equals(result.data.gpsType, "01")) {
                            SpUtils.getSPUtilsInstance().put(SpUtils.THE_SERVER_GPS_TYPE, "01")
                        } else {
                            SpUtils.getSPUtilsInstance().put(SpUtils.THE_SERVER_GPS_TYPE, "02")
                        }
                        var sendInterval = result.data.packSendTimeIntervalAndroid
                        LogUtils.w(
                            TAG,
                            "getProductInfo packSendTimeIntervalAndroid = ${sendInterval}"
                        )
                        if (sendInterval !in 0..200) {
                            sendInterval = 5
                        }
                        SpUtils.getSPUtilsInstance().put(
                            SpUtils.THE_SERVER_PACK_SEND_INTERVAL,
                            result.data.packSendTimeIntervalAndroid
                        )
                        ControlBleTools.getInstance().setCmdDelayTime(sendInterval)
                    }

                    //表盘方向
                    if (result.data.dialRule.isNullOrEmpty()) {
                        Global.dialDirection = true
                    } else Global.dialDirection =
                        result.data.dialRule.trim().toInt() == 1 || result.data.dialRule.trim()
                            .toInt() == 4 ||
                                result.data.dialRule.trim()
                                    .toInt() == 5 || result.data.dialRule.trim().toInt() == 8

                    LogUtils.e(TAG, "getProductInfo 产品设备信息 -> 表盘方向 = ${Global.dialDirection}")
                }
                userLoginOut(result.code)
            } catch (e: java.lang.Exception) {
                LogUtils.e(TAG, "getProductInfo e =$e", true)
                val result = Response("", "getProductInfo e =$e", HttpCommonAttributes.REQUEST_CODE_ERROR, "", ProductInfoResponse())

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                productInfo.postValue(result)
            }
        }
    }

    /**
     * 应用列表
     * */
    val applicationList = MutableLiveData<ApplicationListResponse?>()
    val applicationCode = MutableLiveData<String>("")
    fun getApplicationList(code: Int) {
        launchUI {
            try {
                val result = MyRetrofitClient.service.queryApplicationListInfo(
                    JsonUtils.getRequestJson(
                        "getApplicationList",
                        QueryDevLanguageListParam(code),
                        QueryDevLanguageListParam::class.java
                    )
                )
                LogUtils.e(TAG, "getApplicationList result = $result")
                applicationCode.postValue(result.code)
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    val info = result.data
                    LogUtils.e(TAG, "设备应用列表 -> " + GsonUtils.toJson(info))
                    applicationList.postValue(result.data)
                }
                userLoginOut(result.code)
            } catch (e: Exception) {
                e.printStackTrace()
                applicationCode.postValue(HttpCommonAttributes.REQUEST_FAIL)
            }
        }
    }

    /**
     * 应用列表
     * */
    val cardList = MutableLiveData<ApplicationListResponse>()
    val cardListCode = MutableLiveData<String>("")
    fun getCardList(code: Int) {
        launchUI {
            try {
                val result = MyRetrofitClient.service.queryCardListInfo(
                    JsonUtils.getRequestJson(
                        "cardList",
                        QueryDevLanguageListParam(code),
                        QueryDevLanguageListParam::class.java
                    )
                )
                LogUtils.e(TAG, "cardList result = $result")
                applicationCode.postValue(result.code)
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    val info = result.data
                    LogUtils.e(TAG, "设备卡片列表 -> " + GsonUtils.toJson(info))
                    applicationList.postValue(result.data)
                }
                userLoginOut(result.code)
            } catch (e: Exception) {
                e.printStackTrace()
                applicationCode.postValue(HttpCommonAttributes.REQUEST_FAIL)
            }
        }
    }

    fun upLoadDeviceVersion(version: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val idIndex = DeviceManager.dataList.indexOfFirst { it.deviceStatus == 1 }
                if (userId.isNotEmpty() && idIndex != -1) {
                    val id = DeviceManager.dataList[idIndex].id.toString()
                    val result =
                        MyRetrofitClient.service.upLoadDeviceVersion(
                            JsonUtils.getRequestJson(
                                UpLoadDeviceVersionBean(userId, id, version),
                                UpLoadDeviceVersionBean::class.java
                            )
                        )
                    LogUtils.e(TAG, "upLoadDeviceVersion result = $result")
                    userLoginOut(result.code)
                } else {
                    userLoginOut(HttpCommonAttributes.LOGIN_OUT)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "upLoadDeviceVersion e =$e", true)
            }
        }
    }

    /**
     * 根据手机类型获取后台运行权限说明图
     * */
    val kaImgs = MutableLiveData<List<String>>()
    fun getBPDImageByPhoneType(phoneType: Int, module: String) {
        launchUI {
            try {
                var languageCode = getLanguageId()
                val result = MyRetrofitClient.service.getAppKaImgs(
                    JsonUtils.getRequestJson(
                        AppKeepLiveBean(
                            languageCode,
                            module,
                            "$phoneType",
                            CommonAttributes.APP_ID
                        ), AppKeepLiveBean::class.java
                    )
                )
                LogUtils.e(TAG, "getAppKaImgs result = $result")
                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    kaImgs.postValue(result.data.imgUrls)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "getAppKaImgs e =$e", true)
            }
        }
    }

    /**
     * 获取服务器语言id
     */
    private fun getLanguageId(): String {
        return when (Locale.getDefault().language) {
            //英语
            Locale.ENGLISH.language -> "1"
            //中文
            Locale.CHINA.language -> "2"
            //波兰
            "pl" -> "3"
            //德语
            Locale.GERMAN.language -> "4"
            //俄语
            "ru" -> "5"
            //法语
            Locale.FRENCH.language -> "6"
            //韩语
            Locale.KOREAN.language -> "7"
            //葡萄牙语
            "pt" -> "8"
            //日语
            Locale.JAPAN.language -> "9"
            //土耳其语
            "tr" -> "10"
            //乌克兰语
            "uk" -> "11"
            //西班牙语
            "es" -> "12"
            //意大利语
            Locale.ITALIAN.language -> "13"
            //印地语
            "hi" -> "13"
            //默认英语
            else -> "1"
        }
    }

    /**
     * 上报下载传输记录
     *
     * @param dialId 表盘ID
     * @param dialFileType 1:横向扫描 2:竖向扫描
     * @param dataType 1、表盘下载 2、表盘传输 3、表盘传输成功 4、传输失败
     */
    fun postDialLog(dialId: String, dialFileType: Int, dataType: Int) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                /*if (DeviceUtils.getAndroidID().isNullOrEmpty()) {
                    //安卓设备唯一标识为空，不要调用接口
                    return@launchUI
                }*/
                val dialLogBean = DialLogBean()
                dialLogBean.userId = userId
                dialLogBean.dialId = dialId
                dialLogBean.dialFileType = dialFileType
                dialLogBean.deviceType = Global.deviceType
                dialLogBean.dataType = dataType
                dialLogBean.phoneSystemType = 2
                dialLogBean.phoneSystemLanguage = Locale.getDefault().language
                dialLogBean.imei = /*DeviceUtils.getAndroidID()*/"0"
                dialLogBean.appVersion = AppUtils.getAppVersionName()
                //非必须
                dialLogBean.phoneType = DeviceUtils.getManufacturer()
                dialLogBean.phoneSystemVersion = DeviceUtils.getSDKVersionName()
                dialLogBean.phoneMac = /*DeviceUtils.getMacAddress()*/"0"
                dialLogBean.phoneName = DeviceUtils.getModel()
                dialLogBean.appUnix = Date().time.toString()
                dialLogBean.country = Locale.getDefault().country
                //dialLogBean.province = Locale.getDefault().country
                dialLogBean.city =
                    SpUtils.getSPUtilsInstance().getString(SpUtils.WEATHER_CITY_NAME, "")
                dialLogBean.internetType = when (NetworkUtils.getNetworkType()) {
                    NetworkUtils.NetworkType.NETWORK_ETHERNET -> {
                        "ETHERNET"
                    }
                    NetworkUtils.NetworkType.NETWORK_WIFI -> {
                        "WIFI"
                    }
                    NetworkUtils.NetworkType.NETWORK_4G -> {
                        "4G"
                    }
                    NetworkUtils.NetworkType.NETWORK_3G -> {
                        "3G"
                    }
                    NetworkUtils.NetworkType.NETWORK_2G -> {
                        "2G"
                    }
                    NetworkUtils.NetworkType.NETWORK_NO -> {
                        "NO"
                    }
                    NetworkUtils.NetworkType.NETWORK_UNKNOWN -> {
                        "UNKNOWN"
                    }
                    else -> {
                        "UNKNOWN"
                    }
                }
                dialLogBean.simType = NetworkUtils.getNetworkOperatorName()
                val spGps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")
                if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                    val gps =
                        SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                    if (gps.isNotEmpty() && gps.size == 2) {
                        dialLogBean.longitude = gps[1]
                        dialLogBean.latitude = gps[0]
                    }
                }
                dialLogBean.ip = NetworkUtils.getIPAddress(false)
                dialLogBean.phoneSystemArea = Locale.getDefault().country
                //上传信息
                val result = MyRetrofitClient.service.dialLog(
                    JsonUtils.getRequestJson(
                        dialLogBean, DialLogBean::class.java
                    )
                )
                LogUtils.e(TAG, "postDialLog result = $result")
            } catch (e: Exception) {
                e.printStackTrace()
                LogUtils.d(TAG, "postDialLog Log Failed$e", true)
            }
        }
    }


    /**
     * 获取所有世界时钟
     */
    val worldClockCityList = MutableLiveData<List<WorldClockBean>>()
    private var worldClockData: String = ""
    fun getWorldClockCityStr(filter: String = "", filterData: MutableList<WorldClockBean>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            //region 数据
            /*@{@"serial":@"1" ,@"offset": @  "-10" , @"citiName":LS(@"阿留申群岛") , @"citiId":@"Pacific/Honolulu"} ,
        {@"serial":@"2" ,@"offset": @  "-10" , @"citiName":LS(@"夏威夷") , @"citiId":@"Pacific/Honolulu"} ,
        {@"serial":@"3" ,@"offset": @  "-9.5" , @"citiName":LS(@"马克萨斯群岛") , @"citiId":@"Pacific/Marquesas"} ,
        {@"serial":@"4" ,@"offset": @  "-9" , @"citiName":LS(@"阿拉斯加") , @"citiId":@"America/Anchorage"} ,
        {@"serial":@"5" ,@"offset": @  "-8" , @"citiName":LS(@"太平洋时间美国") , @"citiId":@"America/Los_Angeles"} ,
        {@"serial":@"6" ,@"offset": @  "-8" , @"citiName":LS(@"太平洋时间加拿大") , @"citiId":@"America/Los_Angeles"} ,
        {@"serial":@"7" ,@"offset": @  "-8" , @"citiName":LS(@"下加利福尼亚州") , @"citiId":@"America/Los_Angeles"} ,
        {@"serial":@"8" ,@"offset": @  "-7" , @"citiName":LS(@"奇瓦瓦") , @"citiId":@"America/Chihuahua"} ,
        {@"serial":@"9" ,@"offset": @  "-7" , @"citiName":LS(@"马萨特兰") , @"citiId":@"America/Mazatlan"} ,
        {@"serial":@"10" ,@"offset":  @"-7" , @"citiName":LS(@"山地时间美国") , @"citiId":@"America/Denver"} ,
        {@"serial":@"11" ,@"offset":  @"-7" , @"citiName":LS(@"山地时间加拿大") , @"citiId":@"America/Denver"} ,
        {@"serial":@"12" ,@"offset":  @"-7" , @"citiName":LS(@"亚利桑那") , @"citiId":@"America/Phoenix"} ,
        {@"serial":@"13" ,@"offset":  @"-6" , @"citiName":LS(@"复活节岛") , @"citiId":@"Pacific/Easter"} ,
        {@"serial":@"14" ,@"offset":  @"-6" , @"citiName":LS(@"瓜达拉哈拉") , @"citiId":@"America/Mexico_City"} ,
        {@"serial":@"15" ,@"offset":  @"-6" , @"citiName":LS(@"墨西哥城") , @"citiId":@"America/Mexico_City"} ,
        {@"serial":@"16" ,@"offset":  @"-6" , @"citiName":LS(@"蒙特雷") , @"citiId":@"America/Monterrey"} ,
        {@"serial":@"17" ,@"offset":  @"-6" , @"citiName":LS(@"萨斯喀彻温") , @"citiId":@"America/Regina"} ,
        {@"serial":@"18" ,@"offset":  @"-6" , @"citiName":LS(@"中部时间美国") , @"citiId":@"America/Chicago"} ,
        {@"serial":@"19" ,@"offset":  @"-6" , @"citiName":LS(@"中部时间加拿大") , @"citiId":@"America/Chicago"} ,
        {@"serial":@"20" ,@"offset":  @"-6" , @"citiName":LS(@"中美洲") , @"citiId":@"America/Guatemala"} ,
        {@"serial":@"21" ,@"offset":  @"-5" , @"citiName":LS(@"波哥大") , @"citiId":@"America/Bogota"} ,
        {@"serial":@"22" ,@"offset":  @"-5" , @"citiName":LS(@"利马") , @"citiId":@"America/Lima"} ,
        {@"serial":@"23" ,@"offset":  @"-5" , @"citiName":LS(@"基多") , @"citiId":@"America/Guayaquil"} ,
        {@"serial":@"24" ,@"offset":  @"-5" , @"citiName":LS(@"里奥布朗库") , @"citiId":@"America/Rio_Branco"} ,
        {@"serial":@"25" ,@"offset":  @"-5" , @"citiName":LS(@"东部时间美国") , @"citiId":@"America/New_York"} ,
        {@"serial":@"26" ,@"offset":  @"-5" , @"citiName":LS(@"东部时间加拿大") , @"citiId":@"America/New_York"} ,
        {@"serial":@"27" ,@"offset":  @"-5" , @"citiName":LS(@"哈瓦那") , @"citiId":@"America/Havana"} ,
        {@"serial":@"28" ,@"offset":  @"-5" , @"citiName":LS(@"海地") , @"citiId":@"America/Port-au-Prince"} ,
        {@"serial":@"29" ,@"offset":  @"-5" , @"citiName":LS(@"切图马尔") , @"citiId":@"America/Cancun"} ,
        {@"serial":@"30" ,@"offset":  @"-5" , @"citiName":LS(@"印地安那州东部") , @"citiId":@"America/Indiana/Indianapolis"} ,
        {@"serial":@"31" ,@"offset":  @"-4" , @"citiName":LS(@"亚松森") , @"citiId":@"America/Asuncion"} ,
        {@"serial":@"32" ,@"offset":  @"-4" , @"citiName":LS(@"大西洋时间加拿大") , @"citiId":@"America/Halifax"} ,
        {@"serial":@"33" ,@"offset":  @"-4" , @"citiName":LS(@"加拉加斯") , @"citiId":@"America/Caracas"} ,
        {@"serial":@"34" ,@"offset":  @"-4" , @"citiName":LS(@"库亚巴") , @"citiId":@"America/Cuiaba"} ,
        {@"serial":@"35" ,@"offset":  @"-4" , @"citiName":LS(@"乔治敦") , @"citiId":@"America/Guyana"} ,
        {@"serial":@"37" ,@"offset":  @"-4" , @"citiName":LS(@"马瑙斯") , @"citiId":@"America/Manaus"} ,
        {@"serial":@"38" ,@"offset":  @"-4" , @"citiName":LS(@"圣胡安") , @"citiId":@"America/Puerto_Rico"} ,
        {@"serial":@"39" ,@"offset":  @"-4" , @"citiName":LS(@"圣地亚哥") , @"citiId":@"America/Santiago"} ,
        {@"serial":@"40" ,@"offset":  @"-5" , @"citiName":LS(@"特克斯和凯科斯群岛") , @"citiId":@"America/Grand_Turk"} ,
        {@"serial":@"41" ,@"offset":  @"-3.5" , @"citiName":LS(@"纽芬兰") , @"citiId":@"America/St_Johns"} ,
        {@"serial":@"42" ,@"offset":  @"-3" , @"citiName":LS(@"阿拉瓜伊纳") , @"citiId":@"America/Araguaina"} ,
        {@"serial":@"43" ,@"offset":  @"-3" , @"citiName":LS(@"巴西利亚") , @"citiId":@"America/Sao_Paulo"} ,
        {@"serial":@"44" ,@"offset":  @"-3" , @"citiName":LS(@"布宜诺斯艾利斯") , @"citiId":@"America/Argentina/Buenos_Aires"} ,
        {@"serial":@"45" ,@"offset":  @"-3" , @"citiName":LS(@"格陵兰") , @"citiId":@"America/Nuuk"} ,
        {@"serial":@"46" ,@"offset":  @"-3" , @"citiName":LS(@"卡宴") , @"citiId":@"America/Cayenne"} ,
        {@"serial":@"47" ,@"offset":  @"-3" , @"citiName":LS(@"福塔雷萨") , @"citiId":@"America/Fortaleza"} ,
        {@"serial":@"48" ,@"offset":  @"-3" , @"citiName":LS(@"蒙得维的亚") , @"citiId":@"America/Montevideo"} ,
        {@"serial":@"49" ,@"offset":  @"-3" , @"citiName":LS(@"萨尔瓦多") , @"citiId":@"America/Bahia"} ,
        {@"serial":@"50" ,@"offset":  @"-3" , @"citiName":LS(@"圣皮埃尔和密克隆群岛") , @"citiId":@"America/Miquelon"} ,
        {@"serial":@"51" ,@"offset":  @"-2" , @"citiName":LS(@"中大西洋") , @"citiId":@"Atlantic/South_Georgia"} ,
        {@"serial":@"52" ,@"offset":  @"-1" , @"citiName":LS(@"佛得角群岛") , @"citiId":@"Atlantic/Cape_Verde"} ,
        {@"serial":@"53" ,@"offset":  @"-1" , @"citiName":LS(@"亚速尔群岛") , @"citiId":@"Atlantic/Azores"} ,
        {@"serial":@"54" ,@"offset":  @"0" , @"citiName":LS(@"都柏林") , @"citiId":@"Europe/Dublin"} ,
        {@"serial":@"55" ,@"offset":  @"0" , @"citiName":LS(@"爱丁堡") , @"citiId":@"Europe/London"} ,
        {@"serial":@"56" ,@"offset":  @"0" , @"citiName":LS(@"里斯本") , @"citiId":@"Europe/Lisbon"} ,
        {@"serial":@"57" ,@"offset":  @"0" , @"citiName":LS(@"伦敦") , @"citiId":@"Europe/London"} ,
        {@"serial":@"58" ,@"offset":  @"0" , @"citiName":LS(@"卡萨布兰卡") , @"citiId":@"Africa/Casablanca"} ,
        {@"serial":@"59" ,@"offset":  @"0" , @"citiName":LS(@"蒙罗维亚") , @"citiId":@"Africa/Monrovia"} ,
        {@"serial":@"60" ,@"offset":  @"0" , @"citiName":LS(@"雷克雅未克") , @"citiId":@"Atlantic/Reykjavik"} ,
        {@"serial":@"61" ,@"offset":  @"1" , @"citiName":LS(@"阿姆斯特丹") , @"citiId":@"Europe/Amsterdam"} ,
        {@"serial":@"62" ,@"offset":  @"1" , @"citiName":LS(@"柏林") , @"citiId":@"Europe/Berlin"} ,
        {@"serial":@"63" ,@"offset":  @"1" , @"citiName":LS(@"伯尔尼") , @"citiId":@"Europe/Zurich"} ,
        {@"serial":@"64" ,@"offset":  @"1" , @"citiName":LS(@"罗马") , @"citiId":@"Europe/Rome"} ,
        {@"serial":@"65" ,@"offset":  @"1" , @"citiName":LS(@"斯德哥尔摩") , @"citiId":@"Europe/Stockholm"} ,
        {@"serial":@"66" ,@"offset":  @"1" , @"citiName":LS(@"维也纳") , @"citiId":@"Europe/Vienna"} ,
        {@"serial":@"67" ,@"offset":  @"1" , @"citiName":LS(@"贝尔格莱德") , @"citiId":@"Europe/Belgrade"} ,
        {@"serial":@"68" ,@"offset":  @"1" , @"citiName":LS(@"布拉迪斯拉发") , @"citiId":@"Europe/Bratislava"} ,
        {@"serial":@"69" ,@"offset":  @"1" , @"citiName":LS(@"布达佩斯") , @"citiId":@"Europe/Budapest"} ,
        {@"serial":@"70" ,@"offset":  @"1" , @"citiName":LS(@"卢布尔雅那") , @"citiId":@"Europe/Ljubljana"} ,
        {@"serial":@"71" ,@"offset":  @"1" , @"citiName":LS(@"布拉格") , @"citiId":@"Europe/Prague"} ,
        {@"serial":@"72" ,@"offset":  @"1" , @"citiName":LS(@"布鲁塞尔") , @"citiId":@"Europe/Brussels"} ,
        {@"serial":@"73" ,@"offset":  @"1" , @"citiName":LS(@"哥本哈根") , @"citiId":@"Europe/Copenhagen"} ,
        {@"serial":@"74" ,@"offset":  @"1" , @"citiName":LS(@"马德里") , @"citiId":@"Europe/Madrid"} ,
        {@"serial":@"75" ,@"offset":  @"1" , @"citiName":LS(@"巴黎") , @"citiId":@"Europe/Paris"} ,
        {@"serial":@"76" ,@"offset":  @"1" , @"citiName":LS(@"萨拉热窝") , @"citiId":@"Europe/Sarajevo"} ,
        {@"serial":@"77" ,@"offset":  @"1" , @"citiName":LS(@"斯科普里") , @"citiId":@"Europe/Skopje"} ,
        {@"serial":@"78" ,@"offset":  @"1" , @"citiName":LS(@"华沙") , @"citiId":@"Europe/Warsaw"} ,
        {@"serial":@"79" ,@"offset":  @"1" , @"citiName":LS(@"萨格勒布") , @"citiId":@"Europe/Zagreb"} ,
        {@"serial":@"80" ,@"offset":  @"1" , @"citiName":LS(@"中非西部") , @"citiId":@"Africa/Lagos"} ,
        {@"serial":@"81" ,@"offset":  @"2" , @"citiName":LS(@"安曼") , @"citiId":@"Asia/Amman"} ,
        {@"serial":@"82" ,@"offset":  @"2" , @"citiName":LS(@"温得和克") , @"citiId":@"Africa/Windhoek"} ,
        {@"serial":@"83" ,@"offset":  @"2" , @"citiName":LS(@"贝鲁特") , @"citiId":@"Asia/Beirut"} ,
        {@"serial":@"84" ,@"offset":  @"2" , @"citiName":LS(@"大马士革") , @"citiId":@"Asia/Damascus"} ,
        {@"serial":@"85" ,@"offset":  @"2" , @"citiName":LS(@"的黎波里") , @"citiId":@"Africa/Tripoli"} ,
        {@"serial":@"86" ,@"offset":  @"2" , @"citiName":LS(@"哈拉雷") , @"citiId":@"Africa/Harare"} ,
        {@"serial":@"87" ,@"offset":  @"2" , @"citiName":LS(@"比勒陀利亚") , @"citiId":@"Africa/Johannesburg"} ,
        {@"serial":@"88" ,@"offset":  @"2" , @"citiName":LS(@"赫尔辛基") , @"citiId":@"Europe/Helsinki"} ,
        {@"serial":@"89" ,@"offset":  @"2" , @"citiName":LS(@"基辅") , @"citiId":@"Europe/Kiev"} ,
        {@"serial":@"90" ,@"offset":  @"2" , @"citiName":LS(@"里加") , @"citiId":@"Europe/Riga"} ,
        {@"serial":@"91" ,@"offset":  @"2" , @"citiName":LS(@"索非亚") , @"citiId":@"Europe/Sofia"} ,
        {@"serial":@"92" ,@"offset":  @"2" , @"citiName":LS(@"塔林") , @"citiId":@"Europe/Tallinn"} ,
        {@"serial":@"93" ,@"offset":  @"2" , @"citiName":LS(@"维尔纽斯") , @"citiId":@"Europe/Vilnius"} ,
        {@"serial":@"94" ,@"offset":  @"2" , @"citiName":LS(@"基希讷乌") , @"citiId":@"Europe/Chisinau"} ,
        {@"serial":@"95" ,@"offset":  @"2" , @"citiName":LS(@"加里宁格勒") , @"citiId":@"Europe/Kaliningrad"} ,
        {@"serial":@"96" ,@"offset":  @"2" , @"citiName":LS(@"加沙") , @"citiId":@"Asia/Gaza"} ,
        {@"serial":@"97" ,@"offset":  @"2" , @"citiName":LS(@"希伯伦") , @"citiId":@"Asia/Hebron"} ,
        {@"serial":@"98" ,@"offset":  @"2" , @"citiName":LS(@"开罗") , @"citiId":@"Africa/Cairo"} ,
        {@"serial":@"99" ,@"offset":  @"2" , @"citiName":LS(@"雅典") , @"citiId":@"Europe/Athen"} ,
        {@"serial":@"100" ,@"offset": @"2" , @"citiName":LS(@"布加勒斯特") , @"citiId":@"Europe/Bucharest"} ,
        {@"serial":@"101" ,@"offset": @"2" , @"citiName":LS(@"耶路撒冷") , @"citiId":@"Asia/Jerusalem"} ,
        {@"serial":@"102" ,@"offset": @"3" , @"citiName":LS(@"巴格达") , @"citiId":@"Asia/Baghdad"} ,
        {@"serial":@"103" ,@"offset": @"3" , @"citiName":LS(@"科威特") , @"citiId":@"Asia/Kuwait"} ,
        {@"serial":@"104" ,@"offset": @"3" , @"citiName":LS(@"利雅得") , @"citiId":@"Asia/Riyadh"} ,
        {@"serial":@"105" ,@"offset": @"3" , @"citiName":LS(@"明斯克") , @"citiId":@"Europe/Minsk"} ,
        {@"serial":@"106" ,@"offset": @"3" , @"citiName":LS(@"莫斯科") , @"citiId":@"Europe/Moscow"} ,
        {@"serial":@"107" ,@"offset": @"3" , @"citiName":LS(@"圣彼得堡") , @"citiId":@"Europe/Moscow"} ,
        {@"serial":@"108" ,@"offset": @"3" , @"citiName":LS(@"伏尔加格勒") , @"citiId":@"Europe/Volgograd"} ,
        {@"serial":@"109" ,@"offset": @"3" , @"citiName":LS(@"内罗毕") , @"citiId":@"Africa/Nairobi"} ,
        {@"serial":@"110" ,@"offset": @"3" , @"citiName":LS(@"伊斯坦布尔") , @"citiId":@"Europe/Istanbul"} ,
        {@"serial":@"111" ,@"offset": @"3.5" , @"citiName":LS(@"德黑兰") , @"citiId":@"Asia/Tehran"} ,
        {@"serial":@"112" ,@"offset": @"4" , @"citiName":LS(@"阿布扎比") , @"citiId":@"Asia/Dubai"} ,
        {@"serial":@"113" ,@"offset": @"4" , @"citiName":LS(@"马斯喀特") , @"citiId":@"Asia/Muscat"} ,
        {@"serial":@"114" ,@"offset": @"4" , @"citiName":LS(@"阿斯特拉罕") , @"citiId":@"Europe/Samara"} ,
        {@"serial":@"115" ,@"offset": @"4" , @"citiName":LS(@"乌里扬诺夫斯克") , @"citiId":@"Europe/Samara"} ,
        {@"serial":@"116" ,@"offset": @"4" , @"citiName":LS(@"埃里温") , @"citiId":@"Asia/Yerevan"} ,
        {@"serial":@"117" ,@"offset": @"4" , @"citiName":LS(@"巴库") , @"citiId":@"Asia/Baku"} ,
        {@"serial":@"118" ,@"offset": @"4" , @"citiName":LS(@"第比利斯") , @"citiId":@"Asia/Tbilisi"} ,
        {@"serial":@"119" ,@"offset": @"4" , @"citiName":LS(@"路易港") , @"citiId":@"Indian/Mauritius"} ,
        {@"serial":@"120" ,@"offset": @"4" , @"citiName":LS(@"伊热夫斯克") , @"citiId":@"Europe/Samara"} ,
        {@"serial":@"121" ,@"offset": @"4" , @"citiName":LS(@"萨马拉") , @"citiId":@"Europe/Samara"} ,
        {@"serial":@"122" ,@"offset": @"4.5" , @"citiName":LS(@"喀布尔") , @"citiId":@"Asia/Kabul"} ,
        {@"serial":@"123" ,@"offset": @"5" , @"citiName":LS(@"阿什哈巴德") , @"citiId":@"Asia/Ashgabat"} ,
        {@"serial":@"124" ,@"offset": @"5" , @"citiName":LS(@"塔什干") , @"citiId":@"Asia/Tashkent"} ,
        {@"serial":@"125" ,@"offset": @"5" , @"citiName":LS(@"叶卡捷琳堡") , @"citiId":@"Asia/Yekaterinburg"} ,
        {@"serial":@"126" ,@"offset": @"5" , @"citiName":LS(@"伊斯兰堡") , @"citiId":@"Asia/Karachi"} ,
        {@"serial":@"127" ,@"offset": @"5" , @"citiName":LS(@"卡拉奇") , @"citiId":@"Asia/Karachi"} ,
        {@"serial":@"128" ,@"offset": @"5.5" , @"citiName":LS(@"钦奈") , @"citiId":@"Asia/Kolkata"} ,
        {@"serial":@"129" ,@"offset": @"5.5" , @"citiName":LS(@"加尔各答") , @"citiId":@"Asia/Kolkata"} ,
        {@"serial":@"130" ,@"offset": @"5.5" , @"citiName":LS(@"孟买") , @"citiId":@"Asia/Kolkata"} ,
        {@"serial":@"131" ,@"offset": @"5.5" , @"citiName":LS(@"新德里") , @"citiId":@"Asia/Kolkata"} ,
        {@"serial":@"132" ,@"offset": @"5.5" , @"citiName":LS(@"斯里加亚渥登普拉") , @"citiId":@"Asia/Colombo"} ,
        {@"serial":@"133" ,@"offset": @"5.75" , @"citiName":LS(@"加德满都") , @"citiId":@"Asia/Kathmandu"} ,
        {@"serial":@"134" ,@"offset": @"6" , @"citiName":LS(@"阿斯塔纳") , @"citiId":@"Asia/Almaty"} ,
        {@"serial":@"135" ,@"offset": @"6" , @"citiName":LS(@"达卡") , @"citiId":@"Asia/Dhaka"} ,
        {@"serial":@"136" ,@"offset": @"6" , @"citiName":LS(@"鄂木斯克") , @"citiId":@"Asia/Omsk"} ,
        {@"serial":@"137" ,@"offset": @"6.5" , @"citiName":LS(@"仰光") , @"citiId":@"Asia/Yangon"} ,
        {@"serial":@"138" ,@"offset": @"7" , @"citiName":LS(@"巴尔瑙尔") , @"citiId":@"Asia/Krasnoyarsk"} ,
        {@"serial":@"139" ,@"offset": @"7" , @"citiName":LS(@"戈尔诺阿尔泰斯克") , @"citiId":@"Asia/Barnaul"} ,
        {@"serial":@"140" ,@"offset": @"7" , @"citiName":LS(@"科布多") , @"citiId":@"Asia/Hovd"} ,
        {@"serial":@"141" ,@"offset": @"7" , @"citiName":LS(@"克拉斯诺亚尔斯克") , @"citiId":@"Asia/Krasnoyarsk"} ,
        {@"serial":@"142" ,@"offset": @"7" , @"citiName":LS(@"曼谷") , @"citiId":@"Asia/Bangkok"} ,
        {@"serial":@"143" ,@"offset": @"7" , @"citiName":LS(@"河内") , @"citiId":@"Asia/Bangkok"} ,
        {@"serial":@"144" ,@"offset": @"7" , @"citiName":LS(@"雅加达") , @"citiId":@"Asia/Jakarta"} ,
        {@"serial":@"145" ,@"offset": @"7" , @"citiName":LS(@"托木斯克") , @"citiId":@"Asia/Omsk"} ,
        {@"serial":@"146" ,@"offset": @"7" , @"citiName":LS(@"新西伯利亚") , @"citiId":@"Asia/Novosibirsk"} ,
        {@"serial":@"147" ,@"offset": @"8" , @"citiName":LS(@"北京") , @"citiId":@"Asia/Shanghai"} ,
        {@"serial":@"148" ,@"offset": @"8" , @"citiName":LS(@"重庆") , @"citiId":@"Asia/Shanghai"} ,
        {@"serial":@"149" ,@"offset": @"8" , @"citiName":LS(@"香港特别行政区") , @"citiId":@"Asia/Hong_Kong"} ,
        {@"serial":@"150" ,@"offset": @"8" , @"citiName":LS(@"乌鲁木齐") , @"citiId":@"Asia/Shanghai"} ,
        {@"serial":@"151" ,@"offset": @"8" , @"citiName":LS(@"吉隆坡") , @"citiId":@"Asia/Kuala_Lumpur"} ,
        {@"serial":@"152" ,@"offset": @"8" , @"citiName":LS(@"新加坡") , @"citiId":@"Asia/Singapore"} ,
        {@"serial":@"153" ,@"offset": @"8" , @"citiName":LS(@"珀斯") , @"citiId":@"Australia/Perth"} ,
        {@"serial":@"154" ,@"offset": @"8" , @"citiName":LS(@"台北") , @"citiId":@"Asia/Taipei"} ,
        {@"serial":@"155" ,@"offset": @"8" , @"citiName":LS(@"乌兰巴托") , @"citiId":@"Asia/Ulaanbaatar"} ,
        {@"serial":@"156" ,@"offset": @"8" , @"citiName":LS(@"伊尔库茨克") , @"citiId":@"Asia/Irkutsk"} ,
        {@"serial":@"157" ,@"offset": @"9" , @"citiName":LS(@"平壤") , @"citiId":@"Asia/Pyongyang"} ,
        {@"serial":@"158" ,@"offset": @"8.75" , @"citiName":LS(@"尤克拉") , @"citiId":@"Australia/Perth"} ,
        {@"serial":@"159" ,@"offset": @"9" , @"citiName":LS(@"赤塔市") , @"citiId":@"Asia/Chita"} ,
        {@"serial":@"160" ,@"offset": @"9" , @"citiName":LS(@"大阪") , @"citiId":@"Asia/Tokyo"} ,
        {@"serial":@"161" ,@"offset": @"9" , @"citiName":LS(@"札幌") , @"citiId":@"Asia/Tokyo"} ,
        {@"serial":@"162" ,@"offset": @"9" , @"citiName":LS(@"东京") , @"citiId":@"Asia/Tokyo"} ,
        {@"serial":@"163" ,@"offset": @"9" , @"citiName":LS(@"首尔") , @"citiId":@"Asia/Seoul"} ,
        {@"serial":@"164" ,@"offset": @"9" , @"citiName":LS(@"雅库茨克") , @"citiId":@"Asia/Yakutsk"} ,
        {@"serial":@"165" ,@"offset": @"9.5" , @"citiName":LS(@"阿德莱德") , @"citiId":@"Australia/Adelaide"} ,
        {@"serial":@"166" ,@"offset": @"9.5" , @"citiName":LS(@"达尔文") , @"citiId":@"Australia/Darwin"} ,
        {@"serial":@"167" ,@"offset": @"10" , @"citiName":LS(@"布里斯班") , @"citiId":@"Australia/Brisbane"} ,
        {@"serial":@"168" ,@"offset": @"10" , @"citiName":LS(@"符拉迪沃斯托克") , @"citiId":@"Asia/Vladivostok"} ,
        {@"serial":@"169" ,@"offset": @"10" , @"citiName":LS(@"关岛") , @"citiId":@"Pacific/Guam"} ,
        {@"serial":@"170" ,@"offset": @"10" , @"citiName":LS(@"莫尔兹比港") , @"citiId":@"Pacific/Port_Moresby"} ,
        {@"serial":@"171" ,@"offset": @"10" , @"citiName":LS(@"霍巴特") , @"citiId":@"Australia/Hobart"} ,
        {@"serial":@"172" ,@"offset": @"10" , @"citiName":LS(@"堪培拉") , @"citiId":@"Australia/Sydney"} ,
        {@"serial":@"173" ,@"offset": @"10" , @"citiName":LS(@"墨尔本") , @"citiId":@"Australia/Melbourne"} ,
        {@"serial":@"174" ,@"offset": @"10" , @"citiName":LS(@"悉尼") , @"citiId":@"Australia/Sydney"} ,
        {@"serial":@"175" ,@"offset": @"10.5" , @"citiName":LS(@"豪勋爵岛") , @"citiId":@"Australia/Lord_Howe"} ,
        {@"serial":@"176" ,@"offset": @"11" , @"citiName":LS(@"布干维尔岛") , @"citiId":@"Pacific/Guadalcanal"} ,
        {@"serial":@"177" ,@"offset": @"11" , @"citiName":LS(@"马加丹") , @"citiId":@"Asia/Magadan"} ,
        {@"serial":@"178" ,@"offset": @"11" , @"citiName":LS(@"诺福克岛") , @"citiId":@"Pacific/Norfolk"} ,
        {@"serial":@"179" ,@"offset": @"11" , @"citiName":LS(@"乔库尔达赫") , @"citiId":@"Asia/Sakhali"} ,
        {@"serial":@"180" ,@"offset": @"11" , @"citiName":LS(@"萨哈林") , @"citiId":@"Asia/Sakhalin"} ,
        {@"serial":@"181" ,@"offset": @"11" , @"citiName":LS(@"所罗门群岛") , @"citiId":@"Pacific/Guadalcanal"} ,
        {@"serial":@"182" ,@"offset": @"11" , @"citiName":LS(@"新喀里多尼亚") , @"citiId":@"Pacific/Noumea"} ,
        {@"serial":@"183" ,@"offset": @"12" , @"citiName":LS(@"阿纳德尔") , @"citiId":@"Asia/Anadyr"} ,
        {@"serial":@"184" ,@"offset": @"12" , @"citiName":LS(@"堪察加彼得罗巴甫洛夫斯克") , @"citiId":@"Asia/Kamchatka"} ,
        {@"serial":@"185" ,@"offset": @"12" , @"citiName":LS(@"奥克兰") , @"citiId":@"Pacific/Auckland"} ,
        {@"serial":@"186" ,@"offset": @"12" , @"citiName":LS(@"惠灵顿") , @"citiId":@"Pacific/Auckland"} ,
        {@"serial":@"187" ,@"offset": @"12" , @"citiName":LS(@"斐济") , @"citiId":@"Pacific/Fiji"} ,
        {@"serial":@"188" ,@"offset": @"12.75" , @"citiName":LS(@"查塔姆群岛") , @"citiId":@"Pacific/Chatham"} ,
        {@"serial":@"189" ,@"offset": @"13" , @"citiName":LS(@"努库阿洛法") , @"citiId":@"Pacific/Tongatapu"} ,
        {@"serial":@"190" ,@"offset": @"13" , @"citiName":LS(@"萨摩亚群岛") , @"citiId":@"Pacific/Apia"} ,
        {@"serial":@"191" ,@"offset": @"7" , @"citiName":LS(@"圣诞岛") , @"citiId":@"Pacific/Chatham"}];*/
            if (worldClockData.isNullOrEmpty()) {
                worldClockData = StringBuffer().append(
                    "1@-10@${BaseApplication.mContext.getString(R.string.world_clock_city_1)}@Pacific/Honolulu" + "\n"
                ).append(
                    "2@-10@${BaseApplication.mContext.getString(R.string.world_clock_city_2)}@Pacific/Honolulu" + "\n"
                ).append(
                    "3@-9.5@${BaseApplication.mContext.getString(R.string.world_clock_city_3)}@Pacific/Marquesas" + "\n"
                ).append(
                    "4@-9@${BaseApplication.mContext.getString(R.string.world_clock_city_4)}@America/Anchorage" + "\n"
                ).append(
                    "5@-8@${BaseApplication.mContext.getString(R.string.world_clock_city_5)}@America/Los_Angeles" + "\n"
                ).append(
                    "6@-8@${BaseApplication.mContext.getString(R.string.world_clock_city_6)}@America/Los_Angeles" + "\n"
                ).append(
                    "7@-8@${BaseApplication.mContext.getString(R.string.world_clock_city_7)}@America/Los_Angeles" + "\n"
                ).append(
                    "8@-7@${BaseApplication.mContext.getString(R.string.world_clock_city_8)}@America/Chihuahua" + "\n"
                ).append(
                    "9@-7@${BaseApplication.mContext.getString(R.string.world_clock_city_9)}@America/Mazatlan" + "\n"
                ).append(
                    "10@-7@${BaseApplication.mContext.getString(R.string.world_clock_city_10)}@America/Denver" + "\n"
                ).append(
                    "11@-7@${BaseApplication.mContext.getString(R.string.world_clock_city_11)}@America/Denver" + "\n"
                ).append(
                    "12@-7@${BaseApplication.mContext.getString(R.string.world_clock_city_12)}@America/Phoenix" + "\n"
                ).append(
                    "13@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_13)}@Pacific/Easter" + "\n"
                ).append(
                    "14@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_14)}@America/Mexico_City" + "\n"
                ).append(
                    "15@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_15)}@America/Mexico_City" + "\n"
                ).append(
                    "16@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_16)}@America/Monterrey" + "\n"
                ).append(
                    "17@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_17)}@America/Regina" + "\n"
                ).append(
                    "18@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_18)}@America/Chicago" + "\n"
                ).append(
                    "19@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_19)}@America/Chicago" + "\n"
                ).append(
                    "20@-6@${BaseApplication.mContext.getString(R.string.world_clock_city_20)}@America/Guatemala" + "\n"
                ).append(
                    "21@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_21)}@America/Bogota" + "\n"
                ).append(
                    "22@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_22)}@America/Lima" + "\n"
                ).append(
                    "23@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_23)}@America/Guayaquil" + "\n"
                ).append(
                    "24@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_24)}@America/Rio_Branco" + "\n"
                ).append(
                    "25@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_25)}@America/New_York" + "\n"
                ).append(
                    "26@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_26)}@America/New_York" + "\n"
                ).append(
                    "27@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_27)}@America/Havana" + "\n"
                ).append(
                    "28@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_28)}@America/Port-au-Prince" + "\n"
                ).append(
                    "29@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_29)}@America/Cancun" + "\n"
                ).append(
                    "30@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_30)}@America/Indiana/Indianapolis" + "\n"
                ).append(
                    "31@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_31)}@America/Asuncion" + "\n"
                ).append(
                    "32@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_32)}@America/Halifax" + "\n"
                ).append(
                    "33@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_33)}@America/Caracas" + "\n"
                ).append(
                    "34@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_34)}@America/Cuiaba" + "\n"
                ).append(
                    "35@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_35)}@America/Guyana" + "\n"
                ).append(
                    "37@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_37)}@America/Manaus" + "\n"
                ).append(
                    "38@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_38)}@America/Puerto_Rico" + "\n"
                ).append(
                    "39@-4@${BaseApplication.mContext.getString(R.string.world_clock_city_39)}@America/Santiago" + "\n"
                ).append(
                    "40@-5@${BaseApplication.mContext.getString(R.string.world_clock_city_40)}@America/Grand_Turk" + "\n"
                ).append(
                    "41@-3.5@${BaseApplication.mContext.getString(R.string.world_clock_city_41)}@America/St_Johns" + "\n"
                ).append(
                    "42@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_42)}@America/Araguaina" + "\n"
                ).append(
                    "43@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_43)}@America/Sao_Paulo" + "\n"
                ).append(
                    "44@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_44)}@America/Argentina/Buenos_Aires" + "\n"
                ).append(
                    "45@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_45)}@America/Nuuk" + "\n"
                ).append(
                    "46@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_46)}@America/Cayenne" + "\n"
                ).append(
                    "47@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_47)}@America/Fortaleza" + "\n"
                ).append(
                    "48@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_48)}@America/Montevideo" + "\n"
                ).append(
                    "49@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_49)}@America/Bahia" + "\n"
                ).append(
                    "50@-3@${BaseApplication.mContext.getString(R.string.world_clock_city_50)}@America/Miquelon" + "\n"
                ).append(
                    "51@-2@${BaseApplication.mContext.getString(R.string.world_clock_city_51)}@Atlantic/South_Georgia" + "\n"
                ).append(
                    "52@-1@${BaseApplication.mContext.getString(R.string.world_clock_city_52)}@Atlantic/Cape_Verde" + "\n"
                ).append(
                    "53@-1@${BaseApplication.mContext.getString(R.string.world_clock_city_53)}@Atlantic/Azores" + "\n"
                ).append(
                    "54@0@${BaseApplication.mContext.getString(R.string.world_clock_city_54)}@Europe/Dublin" + "\n"
                ).append(
                    "55@0@${BaseApplication.mContext.getString(R.string.world_clock_city_55)}@Europe/London" + "\n"
                ).append(
                    "56@0@${BaseApplication.mContext.getString(R.string.world_clock_city_56)}@Europe/Lisbon" + "\n"
                ).append(
                    "57@0@${BaseApplication.mContext.getString(R.string.world_clock_city_57)}@Europe/London" + "\n"
                ).append(
                    "58@0@${BaseApplication.mContext.getString(R.string.world_clock_city_58)}@Africa/Casablanca" + "\n"
                ).append(
                    "59@0@${BaseApplication.mContext.getString(R.string.world_clock_city_59)}@Africa/Monrovia" + "\n"
                ).append(
                    "60@0@${BaseApplication.mContext.getString(R.string.world_clock_city_60)}@Atlantic/Reykjavik" + "\n"
                ).append(
                    "61@1@${BaseApplication.mContext.getString(R.string.world_clock_city_61)}@Europe/Amsterdam" + "\n"
                ).append(
                    "62@1@${BaseApplication.mContext.getString(R.string.world_clock_city_62)}@Europe/Berlin" + "\n"
                ).append(
                    "63@1@${BaseApplication.mContext.getString(R.string.world_clock_city_63)}@Europe/Zurich" + "\n"
                ).append(
                    "64@1@${BaseApplication.mContext.getString(R.string.world_clock_city_64)}@Europe/Rome" + "\n"
                ).append(
                    "65@1@${BaseApplication.mContext.getString(R.string.world_clock_city_65)}@Europe/Stockholm" + "\n"
                ).append(
                    "66@1@${BaseApplication.mContext.getString(R.string.world_clock_city_66)}@Europe/Vienna" + "\n"
                ).append(
                    "67@1@${BaseApplication.mContext.getString(R.string.world_clock_city_67)}@Europe/Belgrade" + "\n"
                ).append(
                    "68@1@${BaseApplication.mContext.getString(R.string.world_clock_city_68)}@Europe/Bratislava" + "\n"
                ).append(
                    "69@1@${BaseApplication.mContext.getString(R.string.world_clock_city_69)}@Europe/Budapest" + "\n"
                ).append(
                    "70@1@${BaseApplication.mContext.getString(R.string.world_clock_city_70)}@Europe/Ljubljana" + "\n"
                ).append(
                    "71@1@${BaseApplication.mContext.getString(R.string.world_clock_city_71)}@Europe/Prague" + "\n"
                ).append(
                    "72@1@${BaseApplication.mContext.getString(R.string.world_clock_city_72)}@Europe/Brussels" + "\n"
                ).append(
                    "73@1@${BaseApplication.mContext.getString(R.string.world_clock_city_73)}@Europe/Copenhagen" + "\n"
                ).append(
                    "74@1@${BaseApplication.mContext.getString(R.string.world_clock_city_74)}@Europe/Madrid" + "\n"
                ).append(
                    "75@1@${BaseApplication.mContext.getString(R.string.world_clock_city_75)}@Europe/Paris" + "\n"
                ).append(
                    "76@1@${BaseApplication.mContext.getString(R.string.world_clock_city_76)}@Europe/Sarajevo" + "\n"
                ).append(
                    "77@1@${BaseApplication.mContext.getString(R.string.world_clock_city_77)}@Europe/Skopje" + "\n"
                ).append(
                    "78@1@${BaseApplication.mContext.getString(R.string.world_clock_city_78)}@Europe/Warsaw" + "\n"
                ).append(
                    "79@1@${BaseApplication.mContext.getString(R.string.world_clock_city_79)}@Europe/Zagreb" + "\n"
                ).append(
                    "80@1@${BaseApplication.mContext.getString(R.string.world_clock_city_80)}@Africa/Lagos" + "\n"
                ).append(
                    "81@2@${BaseApplication.mContext.getString(R.string.world_clock_city_81)}@Asia/Amman" + "\n"
                ).append(
                    "82@2@${BaseApplication.mContext.getString(R.string.world_clock_city_82)}@Africa/Windhoek" + "\n"
                ).append(
                    "83@2@${BaseApplication.mContext.getString(R.string.world_clock_city_83)}@Asia/Beirut" + "\n"
                ).append(
                    "84@2@${BaseApplication.mContext.getString(R.string.world_clock_city_84)}@Asia/Damascus" + "\n"
                ).append(
                    "85@2@${BaseApplication.mContext.getString(R.string.world_clock_city_85)}@Africa/Tripoli" + "\n"
                ).append(
                    "86@2@${BaseApplication.mContext.getString(R.string.world_clock_city_86)}@Africa/Harare" + "\n"
                ).append(
                    "87@2@${BaseApplication.mContext.getString(R.string.world_clock_city_87)}@Africa/Johannesburg" + "\n"
                ).append(
                    "88@2@${BaseApplication.mContext.getString(R.string.world_clock_city_88)}@Europe/Helsinki" + "\n"
                ).append(
                    "89@2@${BaseApplication.mContext.getString(R.string.world_clock_city_89)}@Europe/Kiev" + "\n"
                ).append(
                    "90@2@${BaseApplication.mContext.getString(R.string.world_clock_city_90)}@Europe/Riga" + "\n"
                ).append(
                    "91@2@${BaseApplication.mContext.getString(R.string.world_clock_city_91)}@Europe/Sofia" + "\n"
                ).append(
                    "92@2@${BaseApplication.mContext.getString(R.string.world_clock_city_92)}@Europe/Tallinn" + "\n"
                ).append(
                    "93@2@${BaseApplication.mContext.getString(R.string.world_clock_city_93)}@Europe/Vilnius" + "\n"
                ).append(
                    "94@2@${BaseApplication.mContext.getString(R.string.world_clock_city_94)}@Europe/Chisinau" + "\n"
                ).append(
                    "95@2@${BaseApplication.mContext.getString(R.string.world_clock_city_95)}@Europe/Kaliningrad" + "\n"
                ).append(
                    "96@2@${BaseApplication.mContext.getString(R.string.world_clock_city_96)}@Asia/Gaza" + "\n"
                ).append(
                    "97@2@${BaseApplication.mContext.getString(R.string.world_clock_city_97)}@Asia/Hebron" + "\n"
                ).append(
                    "98@2@${BaseApplication.mContext.getString(R.string.world_clock_city_98)}@Africa/Cairo" + "\n"
                ).append(
                    "99@2@${BaseApplication.mContext.getString(R.string.world_clock_city_99)}@Europe/Athen" + "\n"
                ).append(
                    "100@2@${BaseApplication.mContext.getString(R.string.world_clock_city_100)}@Europe/Bucharest" + "\n"
                ).append(
                    "101@2@${BaseApplication.mContext.getString(R.string.world_clock_city_101)}@Asia/Jerusalem" + "\n"
                ).append(
                    "102@3@${BaseApplication.mContext.getString(R.string.world_clock_city_102)}@Asia/Baghdad" + "\n"
                ).append(
                    "103@3@${BaseApplication.mContext.getString(R.string.world_clock_city_103)}@Asia/Kuwait" + "\n"
                ).append(
                    "104@3@${BaseApplication.mContext.getString(R.string.world_clock_city_104)}@Asia/Riyadh" + "\n"
                ).append(
                    "105@3@${BaseApplication.mContext.getString(R.string.world_clock_city_105)}@Europe/Minsk" + "\n"
                ).append(
                    "106@3@${BaseApplication.mContext.getString(R.string.world_clock_city_106)}@Europe/Moscow" + "\n"
                ).append(
                    "107@3@${BaseApplication.mContext.getString(R.string.world_clock_city_107)}@Europe/Moscow" + "\n"
                ).append(
                    "108@3@${BaseApplication.mContext.getString(R.string.world_clock_city_108)}@Europe/Volgograd" + "\n"
                ).append(
                    "109@3@${BaseApplication.mContext.getString(R.string.world_clock_city_109)}@Africa/Nairobi" + "\n"
                ).append(
                    "110@3@${BaseApplication.mContext.getString(R.string.world_clock_city_110)}@Europe/Istanbul" + "\n"
                ).append(
                    "111@3.5@${BaseApplication.mContext.getString(R.string.world_clock_city_111)}@Asia/Tehran" + "\n"
                ).append(
                    "112@4@${BaseApplication.mContext.getString(R.string.world_clock_city_112)}@Asia/Dubai" + "\n"
                ).append(
                    "113@4@${BaseApplication.mContext.getString(R.string.world_clock_city_113)}@Asia/Muscat" + "\n"
                ).append(
                    "114@4@${BaseApplication.mContext.getString(R.string.world_clock_city_114)}@Europe/Samara" + "\n"
                ).append(
                    "115@4@${BaseApplication.mContext.getString(R.string.world_clock_city_115)}@Europe/Samara" + "\n"
                ).append(
                    "116@4@${BaseApplication.mContext.getString(R.string.world_clock_city_116)}@Asia/Yerevan" + "\n"
                ).append(
                    "117@4@${BaseApplication.mContext.getString(R.string.world_clock_city_117)}@Asia/Baku" + "\n"
                ).append(
                    "118@4@${BaseApplication.mContext.getString(R.string.world_clock_city_118)}@Asia/Tbilisi" + "\n"
                ).append(
                    "119@4@${BaseApplication.mContext.getString(R.string.world_clock_city_119)}@Indian/Mauritius" + "\n"
                ).append(
                    "120@4@${BaseApplication.mContext.getString(R.string.world_clock_city_120)}@Europe/Samara" + "\n"
                ).append(
                    "121@4@${BaseApplication.mContext.getString(R.string.world_clock_city_121)}@Europe/Samara" + "\n"
                ).append(
                    "122@4.5@${BaseApplication.mContext.getString(R.string.world_clock_city_122)}@Asia/Kabul" + "\n"
                ).append(
                    "123@5@${BaseApplication.mContext.getString(R.string.world_clock_city_123)}@Asia/Ashgabat" + "\n"
                ).append(
                    "124@5@${BaseApplication.mContext.getString(R.string.world_clock_city_124)}@Asia/Tashkent" + "\n"
                ).append(
                    "125@5@${BaseApplication.mContext.getString(R.string.world_clock_city_125)}@Asia/Yekaterinburg" + "\n"
                ).append(
                    "126@5@${BaseApplication.mContext.getString(R.string.world_clock_city_126)}@Asia/Karachi" + "\n"
                ).append(
                    "127@5@${BaseApplication.mContext.getString(R.string.world_clock_city_127)}@Asia/Karachi" + "\n"
                ).append(
                    "128@5.5@${BaseApplication.mContext.getString(R.string.world_clock_city_128)}@Asia/Kolkata" + "\n"
                ).append(
                    "129@5.5@${BaseApplication.mContext.getString(R.string.world_clock_city_129)}@Asia/Kolkata" + "\n"
                ).append(
                    "130@5.5@${BaseApplication.mContext.getString(R.string.world_clock_city_130)}@Asia/Kolkata" + "\n"
                ).append(
                    "131@5.5@${BaseApplication.mContext.getString(R.string.world_clock_city_131)}@Asia/Kolkata" + "\n"
                ).append(
                    "132@5.5@${BaseApplication.mContext.getString(R.string.world_clock_city_132)}@Asia/Colombo" + "\n"
                ).append(
                    "133@5.75@${BaseApplication.mContext.getString(R.string.world_clock_city_133)}@Asia/Kathmandu" + "\n"
                ).append(
                    "134@6@${BaseApplication.mContext.getString(R.string.world_clock_city_134)}@Asia/Almaty" + "\n"
                ).append(
                    "135@6@${BaseApplication.mContext.getString(R.string.world_clock_city_135)}@Asia/Dhaka" + "\n"
                ).append(
                    "136@6@${BaseApplication.mContext.getString(R.string.world_clock_city_136)}@Asia/Omsk" + "\n"
                ).append(
                    "137@6.5@${BaseApplication.mContext.getString(R.string.world_clock_city_137)}@Asia/Yangon" + "\n"
                ).append(
                    "138@7@${BaseApplication.mContext.getString(R.string.world_clock_city_138)}@Asia/Krasnoyarsk" + "\n"
                ).append(
                    "139@7@${BaseApplication.mContext.getString(R.string.world_clock_city_139)}@Asia/Barnaul" + "\n"
                ).append(
                    "140@7@${BaseApplication.mContext.getString(R.string.world_clock_city_140)}@Asia/Hovd" + "\n"
                ).append(
                    "141@7@${BaseApplication.mContext.getString(R.string.world_clock_city_141)}@Asia/Krasnoyarsk" + "\n"
                ).append(
                    "142@7@${BaseApplication.mContext.getString(R.string.world_clock_city_142)}@Asia/Bangkok" + "\n"
                ).append(
                    "143@7@${BaseApplication.mContext.getString(R.string.world_clock_city_143)}@Asia/Bangkok" + "\n"
                ).append(
                    "144@7@${BaseApplication.mContext.getString(R.string.world_clock_city_144)}@Asia/Jakarta" + "\n"
                ).append(
                    "145@7@${BaseApplication.mContext.getString(R.string.world_clock_city_145)}@Asia/Omsk" + "\n"
                ).append(
                    "146@7@${BaseApplication.mContext.getString(R.string.world_clock_city_146)}@Asia/Novosibirsk" + "\n"
                ).append(
                    "147@8@${BaseApplication.mContext.getString(R.string.world_clock_city_147)}@Asia/Shanghai" + "\n"
                ).append(
                    "148@8@${BaseApplication.mContext.getString(R.string.world_clock_city_148)}@Asia/Shanghai" + "\n"
                ).append(
                    "149@8@${BaseApplication.mContext.getString(R.string.world_clock_city_149)}@Asia/Hong_Kong" + "\n"
                ).append(
                    "150@8@${BaseApplication.mContext.getString(R.string.world_clock_city_150)}@Asia/Shanghai" + "\n"
                ).append(
                    "151@8@${BaseApplication.mContext.getString(R.string.world_clock_city_151)}@Asia/Kuala_Lumpur" + "\n"
                ).append(
                    "152@8@${BaseApplication.mContext.getString(R.string.world_clock_city_152)}@Asia/Singapore" + "\n"
                ).append(
                    "153@8@${BaseApplication.mContext.getString(R.string.world_clock_city_153)}@Australia/Perth" + "\n"
                ).append(
                    "154@8@${BaseApplication.mContext.getString(R.string.world_clock_city_154)}@Asia/Taipei" + "\n"
                ).append(
                    "155@8@${BaseApplication.mContext.getString(R.string.world_clock_city_155)}@Asia/Ulaanbaatar" + "\n"
                ).append(
                    "156@8@${BaseApplication.mContext.getString(R.string.world_clock_city_156)}@Asia/Irkutsk" + "\n"
                ).append(
                    "157@9@${BaseApplication.mContext.getString(R.string.world_clock_city_157)}@Asia/Pyongyang" + "\n"
                ).append(
                    "158@8.75@${BaseApplication.mContext.getString(R.string.world_clock_city_158)}@Australia/Perth" + "\n"
                ).append(
                    "159@9@${BaseApplication.mContext.getString(R.string.world_clock_city_159)}@Asia/Chita" + "\n"
                ).append(
                    "160@9@${BaseApplication.mContext.getString(R.string.world_clock_city_160)}@Asia/Tokyo" + "\n"
                ).append(
                    "161@9@${BaseApplication.mContext.getString(R.string.world_clock_city_161)}@Asia/Tokyo" + "\n"
                ).append(
                    "162@9@${BaseApplication.mContext.getString(R.string.world_clock_city_162)}@Asia/Tokyo" + "\n"
                ).append(
                    "163@9@${BaseApplication.mContext.getString(R.string.world_clock_city_163)}@Asia/Seoul" + "\n"
                ).append(
                    "164@9@${BaseApplication.mContext.getString(R.string.world_clock_city_164)}@Asia/Yakutsk" + "\n"
                ).append(
                    "165@9.5@${BaseApplication.mContext.getString(R.string.world_clock_city_165)}@Australia/Adelaide" + "\n"
                ).append(
                    "166@9.5@${BaseApplication.mContext.getString(R.string.world_clock_city_166)}@Australia/Darwin" + "\n"
                ).append(
                    "167@10@${BaseApplication.mContext.getString(R.string.world_clock_city_167)}@Australia/Brisbane" + "\n"
                ).append(
                    "168@10@${BaseApplication.mContext.getString(R.string.world_clock_city_168)}@Asia/Vladivostok" + "\n"
                ).append(
                    "169@10@${BaseApplication.mContext.getString(R.string.world_clock_city_169)}@Pacific/Guam" + "\n"
                ).append(
                    "170@10@${BaseApplication.mContext.getString(R.string.world_clock_city_170)}@Pacific/Port_Moresby" + "\n"
                ).append(
                    "171@10@${BaseApplication.mContext.getString(R.string.world_clock_city_171)}@Australia/Hobart" + "\n"
                ).append(
                    "172@10@${BaseApplication.mContext.getString(R.string.world_clock_city_172)}@Australia/Sydney" + "\n"
                ).append(
                    "173@10@${BaseApplication.mContext.getString(R.string.world_clock_city_173)}@Australia/Melbourne" + "\n"
                ).append(
                    "174@10@${BaseApplication.mContext.getString(R.string.world_clock_city_174)}@Australia/Sydney" + "\n"
                ).append(
                    "175@10.5@${BaseApplication.mContext.getString(R.string.world_clock_city_175)}@Australia/Lord_Howe" + "\n"
                ).append(
                    "176@11@${BaseApplication.mContext.getString(R.string.world_clock_city_176)}@Pacific/Guadalcanal" + "\n"
                ).append(
                    "177@11@${BaseApplication.mContext.getString(R.string.world_clock_city_177)}@Asia/Magadan" + "\n"
                ).append(
                    "178@11@${BaseApplication.mContext.getString(R.string.world_clock_city_178)}@Pacific/Norfolk" + "\n"
                ).append(
                    "179@11@${BaseApplication.mContext.getString(R.string.world_clock_city_179)}@Asia/Sakhali" + "\n"
                ).append(
                    "180@11@${BaseApplication.mContext.getString(R.string.world_clock_city_180)}@Asia/Sakhalin" + "\n"
                ).append(
                    "181@11@${BaseApplication.mContext.getString(R.string.world_clock_city_181)}@Pacific/Guadalcanal" + "\n"
                ).append(
                    "182@11@${BaseApplication.mContext.getString(R.string.world_clock_city_182)}@Pacific/Noumea" + "\n"
                ).append(
                    "183@12@${BaseApplication.mContext.getString(R.string.world_clock_city_183)}@Asia/Anadyr" + "\n"
                ).append(
                    "184@12@${BaseApplication.mContext.getString(R.string.world_clock_city_184)}@Asia/Kamchatka" + "\n"
                ).append(
                    "185@12@${BaseApplication.mContext.getString(R.string.world_clock_city_185)}@Pacific/Auckland" + "\n"
                ).append(
                    "186@12@${BaseApplication.mContext.getString(R.string.world_clock_city_186)}@Pacific/Auckland" + "\n"
                ).append(
                    "187@12@${BaseApplication.mContext.getString(R.string.world_clock_city_187)}@Pacific/Fiji" + "\n"
                ).append(
                    "188@12.75@${BaseApplication.mContext.getString(R.string.world_clock_city_188)}@Pacific/Chatham" + "\n"
                ).append(
                    "189@13@${BaseApplication.mContext.getString(R.string.world_clock_city_189)}@Pacific/Tongatapu" + "\n"
                ).append(
                    "190@13@${BaseApplication.mContext.getString(R.string.world_clock_city_190)}@Pacific/Apia" + "\n"
                ).append(
                    "191@7@${BaseApplication.mContext.getString(R.string.world_clock_city_191)}@Pacific/Chatham"
                ).toString()
            }
            //endregion
            val ctiyLits: MutableList<WorldClockBean> = mutableListOf()
            try {
                val cityStrs = worldClockData.split("\n")
                for (city in cityStrs) {
                    //191@7@圣诞岛@Pacific/Chatham
                    AppUtils.tryBlock {
                        val itemStrs = city.split("@")
                        val item = WorldClockBean()
                        item.cityId = itemStrs[0].toInt()
                        val abbreviation = itemStrs[3]
                        //处理夏令时
                        val timeZone = TimeZone.getTimeZone(abbreviation)
                        val b = timeZone.inDaylightTime(TimeUtils.getGreenDate()) // 判断当前时间是否处于夏令时
                        if (b) {
                            item.offset = timeZone.getOffset(
                                TimeUtils.getGreenDate().getTime()
                            ) / (60 * 1000) / 15
                        } else {
                            item.offset = timeZone.rawOffset / (60 * 1000) / 15 // 不带令时的与GMT的时差 分钟
                            /*TODO 用写死的值 TimeUtils.toTimezoneInt(split[offsetIndex]) / 15;*/
                        }
                        item.cityName = itemStrs[2]
                        var canAdd = true
                        //过滤搜索
                        if (!TextUtils.isEmpty(filter) && !item.cityName.contains(filter)) {
                            canAdd = false
                        }
                        //过滤已经添加过的
                        if (!filterData.isNullOrEmpty()) {
                            for (dev in filterData) {
                                if (dev.cityId == item.cityId) {
                                    canAdd = false
                                }
                            }
                        }
                        if (canAdd) ctiyLits.add(item)
                    }
                }
                //排序
                ctiyLits.sortWith(Comparator { o1, o2 ->
                    val collator = Collator.getInstance(Locale.getDefault())
                    return@Comparator if (o1 == null || o2 == null) {
                        -1
                    } else {
                        collator.compare(o1.cityName, o2.cityName)
                    }
                })
                //中英 & 不是过滤搜索时  加A_Z 标题
                var tempList: MutableList<WorldClockBean> = mutableListOf()
                if (AppUtils.isZhOrEn(BaseApplication.mContext) && TextUtils.isEmpty(filter)) {
                    val words = arrayOf(
                        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K",
                        "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
                        "W", "X", "Y", "Z", "#"
                    )
                    for (w in words) {
                        var num = 0
                        //添加头
                        val item = WorldClockBean()
                        item.cityId = -1
                        item.cityName = w
                        tempList.add(item)
                        //添加子item
                        for (city in ctiyLits) {
                            AppUtils.tryBlock {
                                if (Locale.getDefault().language.equals(Locale.CHINA.language)) {
                                    //中文环境
                                    val py = PinyinUtils.getFirstSpell(city.cityName)
                                    if (!TextUtils.isEmpty(py) && TextUtils.equals(
                                            w,
                                            py.substring(0, 1).uppercase()
                                        )
                                    ) {
                                        tempList.add(city)
                                        num++
                                    }
                                } else {
                                    //英文环境
                                    if (!TextUtils.isEmpty(city.cityName) && TextUtils.equals(
                                            w,
                                            city.cityName.substring(0, 1).uppercase()
                                        )
                                    ) {
                                        tempList.add(city)
                                        num++
                                    }
                                }
                            }
                        }
                        if (num == 0) {
                            tempList.remove(item)
                        }
                    }
                } else {
                    tempList.addAll(ctiyLits)
                }
                ctiyLits.clear()
                ctiyLits.addAll(tempList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            worldClockCityList.postValue(ctiyLits)
        }
    }

    /**
     * 获取世界时钟昵称
     */
    fun getWorldClockNameById(
        context: Context = BaseApplication.mContext,
        cityId: Int,
        languageType: String = "",
    ): String {
        var languageContext: Context? = context
        if (!languageType.isNullOrEmpty()) {
            val locale = Locale(languageType)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            languageContext = context.createConfigurationContext(configuration)
        }
        return when (cityId) {
            1 -> {
                languageContext?.getString(R.string.world_clock_city_1) ?: ""
            }
            2 -> {
                languageContext?.getString(R.string.world_clock_city_2) ?: ""
            }
            3 -> {
                languageContext?.getString(R.string.world_clock_city_3) ?: ""
            }
            4 -> {
                languageContext?.getString(R.string.world_clock_city_4) ?: ""
            }
            5 -> {
                languageContext?.getString(R.string.world_clock_city_5) ?: ""
            }
            6 -> {
                languageContext?.getString(R.string.world_clock_city_6) ?: ""
            }
            7 -> {
                languageContext?.getString(R.string.world_clock_city_7) ?: ""
            }
            8 -> {
                languageContext?.getString(R.string.world_clock_city_8) ?: ""
            }
            9 -> {
                languageContext?.getString(R.string.world_clock_city_9) ?: ""
            }
            10 -> {
                languageContext?.getString(R.string.world_clock_city_10) ?: ""
            }
            11 -> {
                languageContext?.getString(R.string.world_clock_city_11) ?: ""
            }
            12 -> {
                languageContext?.getString(R.string.world_clock_city_12) ?: ""
            }
            13 -> {
                languageContext?.getString(R.string.world_clock_city_13) ?: ""
            }
            14 -> {
                languageContext?.getString(R.string.world_clock_city_14) ?: ""
            }
            15 -> {
                languageContext?.getString(R.string.world_clock_city_15) ?: ""
            }
            16 -> {
                languageContext?.getString(R.string.world_clock_city_16) ?: ""
            }
            17 -> {
                languageContext?.getString(R.string.world_clock_city_17) ?: ""
            }
            18 -> {
                languageContext?.getString(R.string.world_clock_city_18) ?: ""
            }
            19 -> {
                languageContext?.getString(R.string.world_clock_city_19) ?: ""
            }
            20 -> {
                languageContext?.getString(R.string.world_clock_city_20) ?: ""
            }
            21 -> {
                languageContext?.getString(R.string.world_clock_city_21) ?: ""
            }
            22 -> {
                languageContext?.getString(R.string.world_clock_city_22) ?: ""
            }
            23 -> {
                languageContext?.getString(R.string.world_clock_city_23) ?: ""
            }
            24 -> {
                languageContext?.getString(R.string.world_clock_city_24) ?: ""
            }
            25 -> {
                languageContext?.getString(R.string.world_clock_city_25) ?: ""
            }
            26 -> {
                languageContext?.getString(R.string.world_clock_city_26) ?: ""
            }
            27 -> {
                languageContext?.getString(R.string.world_clock_city_27) ?: ""
            }
            28 -> {
                languageContext?.getString(R.string.world_clock_city_28) ?: ""
            }
            29 -> {
                languageContext?.getString(R.string.world_clock_city_29) ?: ""
            }
            30 -> {
                languageContext?.getString(R.string.world_clock_city_30) ?: ""
            }
            31 -> {
                languageContext?.getString(R.string.world_clock_city_31) ?: ""
            }
            32 -> {
                languageContext?.getString(R.string.world_clock_city_32) ?: ""
            }
            33 -> {
                languageContext?.getString(R.string.world_clock_city_33) ?: ""
            }
            34 -> {
                languageContext?.getString(R.string.world_clock_city_34) ?: ""
            }
            35 -> {
                languageContext?.getString(R.string.world_clock_city_35) ?: ""
            }
            37 -> {
                languageContext?.getString(R.string.world_clock_city_37) ?: ""
            }
            38 -> {
                languageContext?.getString(R.string.world_clock_city_38) ?: ""
            }
            39 -> {
                languageContext?.getString(R.string.world_clock_city_39) ?: ""
            }
            40 -> {
                languageContext?.getString(R.string.world_clock_city_40) ?: ""
            }
            41 -> {
                languageContext?.getString(R.string.world_clock_city_41) ?: ""
            }
            42 -> {
                languageContext?.getString(R.string.world_clock_city_42) ?: ""
            }
            43 -> {
                languageContext?.getString(R.string.world_clock_city_43) ?: ""
            }
            44 -> {
                languageContext?.getString(R.string.world_clock_city_44) ?: ""
            }
            45 -> {
                languageContext?.getString(R.string.world_clock_city_45) ?: ""
            }
            46 -> {
                languageContext?.getString(R.string.world_clock_city_46) ?: ""
            }
            47 -> {
                languageContext?.getString(R.string.world_clock_city_47) ?: ""
            }
            48 -> {
                languageContext?.getString(R.string.world_clock_city_48) ?: ""
            }
            49 -> {
                languageContext?.getString(R.string.world_clock_city_49) ?: ""
            }
            50 -> {
                languageContext?.getString(R.string.world_clock_city_50) ?: ""
            }
            51 -> {
                languageContext?.getString(R.string.world_clock_city_51) ?: ""
            }
            52 -> {
                languageContext?.getString(R.string.world_clock_city_52) ?: ""
            }
            53 -> {
                languageContext?.getString(R.string.world_clock_city_53) ?: ""
            }
            54 -> {
                languageContext?.getString(R.string.world_clock_city_54) ?: ""
            }
            55 -> {
                languageContext?.getString(R.string.world_clock_city_55) ?: ""
            }
            56 -> {
                languageContext?.getString(R.string.world_clock_city_56) ?: ""
            }
            57 -> {
                languageContext?.getString(R.string.world_clock_city_57) ?: ""
            }
            58 -> {
                languageContext?.getString(R.string.world_clock_city_58) ?: ""
            }
            59 -> {
                languageContext?.getString(R.string.world_clock_city_59) ?: ""
            }
            60 -> {
                languageContext?.getString(R.string.world_clock_city_60) ?: ""
            }
            61 -> {
                languageContext?.getString(R.string.world_clock_city_61) ?: ""
            }
            62 -> {
                languageContext?.getString(R.string.world_clock_city_62) ?: ""
            }
            63 -> {
                languageContext?.getString(R.string.world_clock_city_63) ?: ""
            }
            64 -> {
                languageContext?.getString(R.string.world_clock_city_64) ?: ""
            }
            65 -> {
                languageContext?.getString(R.string.world_clock_city_65) ?: ""
            }
            66 -> {
                languageContext?.getString(R.string.world_clock_city_66) ?: ""
            }
            67 -> {
                languageContext?.getString(R.string.world_clock_city_67) ?: ""
            }
            68 -> {
                languageContext?.getString(R.string.world_clock_city_68) ?: ""
            }
            69 -> {
                languageContext?.getString(R.string.world_clock_city_69) ?: ""
            }
            70 -> {
                languageContext?.getString(R.string.world_clock_city_70) ?: ""
            }
            71 -> {
                languageContext?.getString(R.string.world_clock_city_71) ?: ""
            }
            72 -> {
                languageContext?.getString(R.string.world_clock_city_72) ?: ""
            }
            73 -> {
                languageContext?.getString(R.string.world_clock_city_73) ?: ""
            }
            74 -> {
                languageContext?.getString(R.string.world_clock_city_74) ?: ""
            }
            75 -> {
                languageContext?.getString(R.string.world_clock_city_75) ?: ""
            }
            76 -> {
                languageContext?.getString(R.string.world_clock_city_76) ?: ""
            }
            77 -> {
                languageContext?.getString(R.string.world_clock_city_77) ?: ""
            }
            78 -> {
                languageContext?.getString(R.string.world_clock_city_78) ?: ""
            }
            79 -> {
                languageContext?.getString(R.string.world_clock_city_79) ?: ""
            }
            80 -> {
                languageContext?.getString(R.string.world_clock_city_80) ?: ""
            }
            81 -> {
                languageContext?.getString(R.string.world_clock_city_81) ?: ""
            }
            82 -> {
                languageContext?.getString(R.string.world_clock_city_82) ?: ""
            }
            83 -> {
                languageContext?.getString(R.string.world_clock_city_83) ?: ""
            }
            84 -> {
                languageContext?.getString(R.string.world_clock_city_84) ?: ""
            }
            85 -> {
                languageContext?.getString(R.string.world_clock_city_85) ?: ""
            }
            86 -> {
                languageContext?.getString(R.string.world_clock_city_86) ?: ""
            }
            87 -> {
                languageContext?.getString(R.string.world_clock_city_87) ?: ""
            }
            88 -> {
                languageContext?.getString(R.string.world_clock_city_88) ?: ""
            }
            89 -> {
                languageContext?.getString(R.string.world_clock_city_89) ?: ""
            }
            90 -> {
                languageContext?.getString(R.string.world_clock_city_90) ?: ""
            }
            91 -> {
                languageContext?.getString(R.string.world_clock_city_91) ?: ""
            }
            92 -> {
                languageContext?.getString(R.string.world_clock_city_92) ?: ""
            }
            93 -> {
                languageContext?.getString(R.string.world_clock_city_93) ?: ""
            }
            94 -> {
                languageContext?.getString(R.string.world_clock_city_94) ?: ""
            }
            95 -> {
                languageContext?.getString(R.string.world_clock_city_95) ?: ""
            }
            96 -> {
                languageContext?.getString(R.string.world_clock_city_96) ?: ""
            }
            97 -> {
                languageContext?.getString(R.string.world_clock_city_97) ?: ""
            }
            98 -> {
                languageContext?.getString(R.string.world_clock_city_98) ?: ""
            }
            99 -> {
                languageContext?.getString(R.string.world_clock_city_99) ?: ""
            }
            100 -> {
                languageContext?.getString(R.string.world_clock_city_100) ?: ""
            }
            101 -> {
                languageContext?.getString(R.string.world_clock_city_101) ?: ""
            }
            102 -> {
                languageContext?.getString(R.string.world_clock_city_102) ?: ""
            }
            103 -> {
                languageContext?.getString(R.string.world_clock_city_103) ?: ""
            }
            104 -> {
                languageContext?.getString(R.string.world_clock_city_104) ?: ""
            }
            105 -> {
                languageContext?.getString(R.string.world_clock_city_105) ?: ""
            }
            106 -> {
                languageContext?.getString(R.string.world_clock_city_106) ?: ""
            }
            107 -> {
                languageContext?.getString(R.string.world_clock_city_107) ?: ""
            }
            108 -> {
                languageContext?.getString(R.string.world_clock_city_108) ?: ""
            }
            109 -> {
                languageContext?.getString(R.string.world_clock_city_109) ?: ""
            }
            110 -> {
                languageContext?.getString(R.string.world_clock_city_110) ?: ""
            }
            111 -> {
                languageContext?.getString(R.string.world_clock_city_111) ?: ""
            }
            112 -> {
                languageContext?.getString(R.string.world_clock_city_112) ?: ""
            }
            113 -> {
                languageContext?.getString(R.string.world_clock_city_113) ?: ""
            }
            114 -> {
                languageContext?.getString(R.string.world_clock_city_114) ?: ""
            }
            115 -> {
                languageContext?.getString(R.string.world_clock_city_115) ?: ""
            }
            116 -> {
                languageContext?.getString(R.string.world_clock_city_116) ?: ""
            }
            117 -> {
                languageContext?.getString(R.string.world_clock_city_117) ?: ""
            }
            118 -> {
                languageContext?.getString(R.string.world_clock_city_118) ?: ""
            }
            119 -> {
                languageContext?.getString(R.string.world_clock_city_119) ?: ""
            }
            120 -> {
                languageContext?.getString(R.string.world_clock_city_120) ?: ""
            }
            121 -> {
                languageContext?.getString(R.string.world_clock_city_121) ?: ""
            }
            122 -> {
                languageContext?.getString(R.string.world_clock_city_122) ?: ""
            }
            123 -> {
                languageContext?.getString(R.string.world_clock_city_123) ?: ""
            }
            124 -> {
                languageContext?.getString(R.string.world_clock_city_124) ?: ""
            }
            125 -> {
                languageContext?.getString(R.string.world_clock_city_125) ?: ""
            }
            126 -> {
                languageContext?.getString(R.string.world_clock_city_126) ?: ""
            }
            127 -> {
                languageContext?.getString(R.string.world_clock_city_127) ?: ""
            }
            128 -> {
                languageContext?.getString(R.string.world_clock_city_128) ?: ""
            }
            129 -> {
                languageContext?.getString(R.string.world_clock_city_129) ?: ""
            }
            130 -> {
                languageContext?.getString(R.string.world_clock_city_130) ?: ""
            }
            131 -> {
                languageContext?.getString(R.string.world_clock_city_131) ?: ""
            }
            132 -> {
                languageContext?.getString(R.string.world_clock_city_132) ?: ""
            }
            133 -> {
                languageContext?.getString(R.string.world_clock_city_133) ?: ""
            }
            134 -> {
                languageContext?.getString(R.string.world_clock_city_134) ?: ""
            }
            135 -> {
                languageContext?.getString(R.string.world_clock_city_135) ?: ""
            }
            136 -> {
                languageContext?.getString(R.string.world_clock_city_136) ?: ""
            }
            137 -> {
                languageContext?.getString(R.string.world_clock_city_137) ?: ""
            }
            138 -> {
                languageContext?.getString(R.string.world_clock_city_138) ?: ""
            }
            139 -> {
                languageContext?.getString(R.string.world_clock_city_139) ?: ""
            }
            140 -> {
                languageContext?.getString(R.string.world_clock_city_140) ?: ""
            }
            141 -> {
                languageContext?.getString(R.string.world_clock_city_141) ?: ""
            }
            142 -> {
                languageContext?.getString(R.string.world_clock_city_142) ?: ""
            }
            143 -> {
                languageContext?.getString(R.string.world_clock_city_143) ?: ""
            }
            144 -> {
                languageContext?.getString(R.string.world_clock_city_144) ?: ""
            }
            145 -> {
                languageContext?.getString(R.string.world_clock_city_145) ?: ""
            }
            146 -> {
                languageContext?.getString(R.string.world_clock_city_146) ?: ""
            }
            147 -> {
                languageContext?.getString(R.string.world_clock_city_147) ?: ""
            }
            148 -> {
                languageContext?.getString(R.string.world_clock_city_148) ?: ""
            }
            149 -> {
                languageContext?.getString(R.string.world_clock_city_149) ?: ""
            }
            150 -> {
                languageContext?.getString(R.string.world_clock_city_150) ?: ""
            }
            151 -> {
                languageContext?.getString(R.string.world_clock_city_151) ?: ""
            }
            152 -> {
                languageContext?.getString(R.string.world_clock_city_152) ?: ""
            }
            153 -> {
                languageContext?.getString(R.string.world_clock_city_153) ?: ""
            }
            154 -> {
                languageContext?.getString(R.string.world_clock_city_154) ?: ""
            }
            155 -> {
                languageContext?.getString(R.string.world_clock_city_155) ?: ""
            }
            156 -> {
                languageContext?.getString(R.string.world_clock_city_156) ?: ""
            }
            157 -> {
                languageContext?.getString(R.string.world_clock_city_157) ?: ""
            }
            158 -> {
                languageContext?.getString(R.string.world_clock_city_158) ?: ""
            }
            159 -> {
                languageContext?.getString(R.string.world_clock_city_159) ?: ""
            }
            160 -> {
                languageContext?.getString(R.string.world_clock_city_160) ?: ""
            }
            161 -> {
                languageContext?.getString(R.string.world_clock_city_161) ?: ""
            }
            162 -> {
                languageContext?.getString(R.string.world_clock_city_162) ?: ""
            }
            163 -> {
                languageContext?.getString(R.string.world_clock_city_163) ?: ""
            }
            164 -> {
                languageContext?.getString(R.string.world_clock_city_164) ?: ""
            }
            165 -> {
                languageContext?.getString(R.string.world_clock_city_165) ?: ""
            }
            166 -> {
                languageContext?.getString(R.string.world_clock_city_166) ?: ""
            }
            167 -> {
                languageContext?.getString(R.string.world_clock_city_167) ?: ""
            }
            168 -> {
                languageContext?.getString(R.string.world_clock_city_168) ?: ""
            }
            169 -> {
                languageContext?.getString(R.string.world_clock_city_169) ?: ""
            }
            170 -> {
                languageContext?.getString(R.string.world_clock_city_170) ?: ""
            }
            171 -> {
                languageContext?.getString(R.string.world_clock_city_171) ?: ""
            }
            172 -> {
                languageContext?.getString(R.string.world_clock_city_172) ?: ""
            }
            173 -> {
                languageContext?.getString(R.string.world_clock_city_173) ?: ""
            }
            174 -> {
                languageContext?.getString(R.string.world_clock_city_174) ?: ""
            }
            175 -> {
                languageContext?.getString(R.string.world_clock_city_175) ?: ""
            }
            176 -> {
                languageContext?.getString(R.string.world_clock_city_176) ?: ""
            }
            177 -> {
                languageContext?.getString(R.string.world_clock_city_177) ?: ""
            }
            178 -> {
                languageContext?.getString(R.string.world_clock_city_178) ?: ""
            }
            179 -> {
                languageContext?.getString(R.string.world_clock_city_179) ?: ""
            }
            180 -> {
                languageContext?.getString(R.string.world_clock_city_180) ?: ""
            }
            181 -> {
                languageContext?.getString(R.string.world_clock_city_181) ?: ""
            }
            182 -> {
                languageContext?.getString(R.string.world_clock_city_182) ?: ""
            }
            183 -> {
                languageContext?.getString(R.string.world_clock_city_183) ?: ""
            }
            184 -> {
                languageContext?.getString(R.string.world_clock_city_184) ?: ""
            }
            185 -> {
                languageContext?.getString(R.string.world_clock_city_185) ?: ""
            }
            186 -> {
                languageContext?.getString(R.string.world_clock_city_186) ?: ""
            }
            187 -> {
                languageContext?.getString(R.string.world_clock_city_187) ?: ""
            }
            188 -> {
                languageContext?.getString(R.string.world_clock_city_188) ?: ""
            }
            189 -> {
                languageContext?.getString(R.string.world_clock_city_189) ?: ""
            }
            190 -> {
                languageContext?.getString(R.string.world_clock_city_190) ?: ""
            }
            191 -> {
                languageContext?.getString(R.string.world_clock_city_191) ?: ""
            }
            else -> {
                ""
            }
        }
    }

    /**
     * 根据设备语言id获取App支持的本地语言类型
     * @return App支持的本地语言类型 默认英语 en
     * https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry
     */
    fun getLocaleLanguageTypeByDevLgeId(languageId: Int): String {
        return when (languageId) {
            //英语        ENGLISH = 0X68; //英语
            104 -> Locale.ENGLISH.language
            //德语         GERMAN = 0X13; //德语
            19 -> Locale.GERMAN.language
            //西班牙语     SPANISH = 0X61; //西班牙语
            97 -> "es"
            //法语         FRENCH = 0X15; //法语
            21 -> Locale.FRENCH.language
            //印地语       HINDI= 0X66; //印地语
            102 -> "hi"
            //意大利语      ITALIAN = 0X65; //意大利语
            101 -> Locale.ITALIAN.language
            //日语        JAPANESE = 0X47; //日语
            71 -> Locale.JAPANESE.language
            //韩语        KOREAN = 0X23; //韩语
            35 -> Locale.KOREAN.language
            //波兰语       POLISH = 0X09; //波兰语
            9 -> "pl"
            //葡萄牙语      PORTUGUESE = 0X46; //葡萄牙语
            70 -> "pt"
            //俄语        RUSSIAN = 0X14; //俄语
            20 -> "ru"
            //土耳其语      TURKISH = 0X57; //土耳其语
            87 -> "tr"
            //乌克兰语      UKRAINIAN = 0X59; //乌克兰语
            89 -> "uk"
            //中文简体语     TRADITIONAL_CHINESE = 0X70; //繁体中文
            112 -> Locale.CHINA.language
            //默认英文
            else -> Locale.ENGLISH.language
        }
    }

    fun postWorldClockToDevice(
        items: List<WorldClockItem>,
        languageId: Int,
        callBack: ParsingStateManager.SendCmdStateListener?,
    ) {
        val devList = mutableListOf<WorldClockBean>()
        for (item in items) {
            val bean = WorldClockBean()
            bean.cityId = item.cityId
            val name = getWorldClockNameById(
                cityId = item.cityId,
                languageType = getLocaleLanguageTypeByDevLgeId(languageId)
            )
            bean.cityName = name.ifEmpty { item.cityName }
            bean.offset = item.offset
            bean.latitude = item.latitude
            bean.longitude = item.longitude
            devList.add(bean)
        }
        ControlBleTools.getInstance().setWorldClockList(devList, callBack)
    }


    val photoData: MutableLiveData<Bitmap> = MutableLiveData()

    //首页获取DIY表盘
    val diyHomeList = MutableLiveData<Response<DiyHomeListResponse>>()
    fun getDiyHomeList() {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                val bean = DiyHomeListBean()
                bean.userId = userId
                bean.productNo = Global.deviceType
                bean.productVersion = "1"
                bean.languageCode = if (AppUtils.isZh(BaseApplication.mContext)) "1" else "0"
                val result = MyRetrofitClient.service.getDiyHomeList(
                    JsonUtils.getRequestJson(
                        bean,
                        DiyHomeListBean::class.java
                    )
                )
                LogUtils.e(TAG, "diyHomeList result = $result")
                //DiyHomeListResponse(dialSystemList=[Data(dialId='21', dialName='worksName_0:Inspiration', effectImgUrl='http://file.wearheart.cn/upload/icon/20230316/bbfa151bc41c437a96b292d44505c870.png')]))
                diyHomeList.postValue(result)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "diyHomeList e =$e")
                val result = Response(
                    "",
                    "",
                    HttpCommonAttributes.REQUEST_CODE_ERROR,
                    "",
                    DiyHomeListResponse()
                )
                diyHomeList.postValue(result)
            }
        }
    }

    //表盘详情
    val diyInfo = MutableLiveData<Response<DiyDialInfoResponse>>()
    fun getDiyInfoById(dialId: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) {
                    return@launchUI
                }
                val bean = QureyDiyInfoBean()
                bean.userId = userId
                bean.dialId = dialId

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(bean)
                    }
                }

                val result = MyRetrofitClient.service.getDiyInfo(
                    JsonUtils.getRequestJson(
                        bean,
                        QureyDiyInfoBean::class.java
                    )
                )
                LogUtils.e(TAG, "getDiyInfoById result = $result")
                com.blankj.utilcode.util.LogUtils.d(MyDiyDialUtils.getDiyDialJsonByDiyDialInfoResponse(result.data))

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = "data"
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                diyInfo.postValue(result)
                userLoginOut(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "getDiyInfoById e =$e")
                val result = Response(
                    "",
                    "",
                    HttpCommonAttributes.REQUEST_CODE_ERROR,
                    "",
                    DiyDialInfoResponse()
                )

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                diyInfo.postValue(result)
            }
        }
    }

    //region 下载DIY配置资源

    /**
     * 下载配置资源
     * */
    fun downloadInfoRes(data: DiyDialInfoResponse, param: BulkDownloadListener) {

        val downUrls: MutableList<String> = mutableListOf()
        //创建系统图片忽略文件
        createNoMediaFile(data.code)
        //要下载的文件
        detectionPathAndLoad(data.code, data.defaultBackgroundImage, downUrls)
        detectionPathAndLoad(data.code, data.backgroundOverlay, downUrls)
        detectionPathAndLoad(data.code, data.complicationsBin, downUrls)
        detectionPathAndLoad(data.code, data.renderings, downUrls)
        data.pointerList!!.forEach {
            detectionPathAndLoad(data.code, it.binUrl, downUrls)
            detectionPathAndLoad(data.code, it.renderingsUrl, downUrls)
            detectionPathAndLoad(data.code, it.pointerPictureUrl, downUrls)
        }
        data.positionList!!.forEach { it ->
            it.dataElementList!!.forEach {
                detectionPathAndLoad(data.code, it.imgUrl, downUrls)
            }
        }

        if (downUrls.size > 0)
            addDownloadTask(data.code, downUrls, param)
        else {
            //文件全部已下载
            param.onComplete()
        }
    }

    /**
     * 获取diy路径
     */
    private fun getDiyDir(dialId: String): String {
        return "${PathUtils.getAppDataPathExternalFirst()}${File.separator}otal${File.separator}dial${File.separator}diy_$dialId"
    }

    /**
     * 检测文件是否已下载
     * */
    private fun detectionPathAndLoad(dialId: String, url: String, downUrls: MutableList<String>) {
        val path = getDiyDir(dialId) + File.separator + url.substringAfterLast("/")
        if (!File(path).exists())
            downUrls.add(url)
        else
            UrlPathUtils.putPath(url, path)
    }

    /**
     * 创建系统图片忽略文件
     */
    private fun createNoMediaFile(dialId: String) {
        val dir = getDiyDir(dialId)
        if (FileUtils.createOrExistsDir(dir)) {
            FileUtils.createOrExistsFile(dir + File.separator + MediaStore.MEDIA_IGNORE_FILENAME)
        }
    }

    /**
     * 下载文件
     */
    private fun addDownloadTask(dialId: String, urls: MutableList<String>, param: BulkDownloadListener) {
        LogUtils.d(TAG, "任务总数:${urls.size}")
        launchUI {
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getSerTypeTrack("下载表盘资源", "下载", GsonUtils.toJson(urls)))

            val deferreds = mutableListOf<Deferred<String?>>()
            for (index in 0 until urls.size) {
                deferreds.add(
                    async { downloadUrl(dialId, urls[index]) }
                )
            }
            for (d in deferreds) {
                val result = d.await()
                LogUtils.d(TAG, "$result")
                if (result == null) {
                    param.onFailed("")
                    return@launchUI
                }
                //param.onProgress(0,0)
            }
            LogUtils.d(TAG, "下载 param.onComplete()")
            param.onComplete()
        }
    }

    /**
     * 下载文件
     */
    suspend fun downloadUrl(dialId: String, url: String, errorTime: Int = 0): String? {
        return withTimeoutOrNull(30 * 1000) {
            suspendCancellableCoroutine<String?> {
                DownloadManager.download(url = url, dir = getDiyDir(dialId), listener = object : DownloadListener {
                    override fun onStart() {
                    }

                    override fun onProgress(totalSize: Long, currentSize: Long) {
                        LogUtils.d("downloadFile", "totalSize=$totalSize  currentSize=$currentSize")
                    }

                    override fun onFailed(msg: String) {
                        LogUtils.d(TAG, "下载:$url 失败, 失败次数：$errorTime")
                        if (errorTime > 4) {
                            it.resume(null)
                        } else {
                            launchUI {
                                it.resume(downloadUrl(dialId, url, errorTime + 1))
                            }
                        }
                    }

                    override fun onSucceed(path: String) {
                        LogUtils.d(TAG, "下载:$url 完成")
                        UrlPathUtils.putPath(url, path)
                        it.resume(path)
                    }
                })
            }
        }
    }
    //endregion


}