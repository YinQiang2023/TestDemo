package com.smartwear.xzfit.ui.device


import android.text.TextUtils
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.smartwear.xzfit.base.UnFlawedLiveData
import com.zhapp.ble.bean.*
import com.zhapp.ble.callback.*
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.db.model.track.TrackingLog
import com.smartwear.xzfit.https.MyRetrofitClient
import com.smartwear.xzfit.https.params.DisconnectionBean
import com.smartwear.xzfit.service.MyNotificationsService
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.device.bean.PhoneDtoModel
import com.smartwear.xzfit.ui.device.weather.utils.WeatherManagerUtils
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.JsonUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import com.smartwear.xzfit.utils.manager.MicroManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import kotlin.collections.ArrayList

/**
 * Created by Android on 2021/10/19.
 * 设备设置全局LiveData
 */
class DeviceSettingLiveData private constructor() {

    companion object {
        @JvmStatic
        val instance = SingletonHolder.INSTANCE
    }

    private object SingletonHolder {
        val INSTANCE = DeviceSettingLiveData()
    }

    //协程作用域
    private var scope = MainScope()

    //设备快捷回复信息
    private val mShortReply = UnFlawedLiveData<ArrayList<String>>()
    fun getShortReply() = mShortReply

    //震动强度
    private val mVibrationMode = UnFlawedLiveData<Int>()
    fun getVibration() = mVibrationMode

    //省电设置
    private val mPowerSaving = UnFlawedLiveData<Boolean>()
    fun getPowerSaving() = mPowerSaving

    //覆盖息屏
    private val mOverlayScreen = UnFlawedLiveData<Boolean>()
    fun getOverlayScreen() = mOverlayScreen

    //快速眼动
    private val mRapidEyeMovement = UnFlawedLiveData<Boolean>()
    fun getRapidEyeMovement() = mRapidEyeMovement

    //抬腕亮屏
    private val mWristScreen = UnFlawedLiveData<WristScreenBean>()
    fun getWristScreen() = mWristScreen

    //勿扰模式
    private val mDoNotDisturbMode = UnFlawedLiveData<DoNotDisturbModeBean>()
    fun getDoNotDisturbMode() = mDoNotDisturbMode

    //心率检测
    private val mHeartRateMonitor = UnFlawedLiveData<HeartRateMonitorBean>()
    fun getHeartRateMonitorBean() = mHeartRateMonitor

    //息屏设置
    private val mScreenDisplay = UnFlawedLiveData<ScreenDisplayBean>()
    fun getScreenDisplay() = mScreenDisplay

    //屏幕设置
    private val mScreenSetting = UnFlawedLiveData<ScreenSettingBean>()
    fun getScreenSetting() = mScreenSetting

    //生理周期
    private val mPhysiologicalCycle = UnFlawedLiveData<PhysiologicalCycleBean>()
    fun getPhysiologicalCycle() = mPhysiologicalCycle

    //久坐提醒
    private val mSedentaryReminder = UnFlawedLiveData<CommonReminderBean>()
    fun getSedentaryReminder() = mSedentaryReminder

    //喝水提醒
    private val mDrinkWaterReminder = UnFlawedLiveData<CommonReminderBean>()
    fun getDrinkWaterReminder() = mDrinkWaterReminder

    //吃药提醒
    private val mMedicationReminder = UnFlawedLiveData<CommonReminderBean>()
    fun getMedicationReminder() = mMedicationReminder

    private var mHandWashingRemider = UnFlawedLiveData<CommonReminderBean>()
    fun getHandWashingRemider() = mHandWashingRemider

    //事件提醒
    private val mEventInfo = UnFlawedLiveData<MutableList<EventInfoBean>>()
    fun getEventInfos() = mEventInfo
    private val mEventMax = UnFlawedLiveData<Int>()
    fun getEventMax() = mEventMax

    //闹钟提醒
    private val mClockInfo = UnFlawedLiveData<MutableList<ClockInfoBean>>()
    fun getClockInfo() = mClockInfo
    private val mClockMax = UnFlawedLiveData<Int>()
    fun getClockMax() = mClockMax

    //设备应用列表
    private val mWidgetList = UnFlawedLiveData<MutableList<WidgetBean>>()
    fun getWidgetList() = mWidgetList

    //卡片列表
    private val mCardList = UnFlawedLiveData<MutableList<WidgetBean>>()
    fun getCardList() = mCardList

    //语言设置
    private val mLanguageList = UnFlawedLiveData<LanguageListBean>()
    fun getLanguageList() = mLanguageList

    private val classicBluetoothState = UnFlawedLiveData<ClassicBluetoothStateBean>()
    fun getClassicBluetoothStateBean() = classicBluetoothState

    //简单设置汇总
    private val simpleSettingSummary = UnFlawedLiveData<SimpleSettingSummaryBean>()
    fun getSimpleSettingSummary() = simpleSettingSummary

    //联系人
    private val contactList = UnFlawedLiveData<ArrayList<ContactBean>>()
    fun getContactList() = contactList

    //运动排序
    private val mSportList = UnFlawedLiveData<MutableList<WidgetBean>>()
    fun getSportList() = mSportList

    //世界时钟
    private val mWorldClockList = UnFlawedLiveData<MutableList<WorldClockBean>>()
    fun getWorldClock() = mWorldClockList

    //压力模式
    private val mPressureMode = UnFlawedLiveData<PressureModeBean>()
    fun getPressureMode() = mPressureMode


    init {
        initCallBack()
    }

    private fun initCallBack() {
        //region 设备快捷回复
        /**
         * 设备快捷回复
         * */
        CallBackUtils.quickReplyCallBack = object : QuickReplyCallBack {
            override fun onQuickReplyResult(data: ArrayList<String>) {
                LogUtils.d("查询设备快捷回复 --》 " + GsonUtils.toJson(data))
                mShortReply.postValue(data)
            }

            override fun onMessage(phone_number: String, text: String) {
                LogUtils.d("收到设备快捷回复 --》$phone_number, $text ")
                //TODO 快捷回复处理 挂电话 发短信 17727905153, 11111111
                val phone = PhoneDtoModel()
                phone.telPhone = phone_number
                phone.smsContext = text
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_REPLY_SEND_SMS, phone))

            }
        }
        //endregion

        //region 设备设置相关
        /**
         * 设备设置相关
         * */
        CallBackUtils.settingMenuCallBack = object : SettingMenuCallBack {

            override fun onVibrationResult(model: Int) {
                LogUtils.d("震动强度 ->$model")
                mVibrationMode.postValue(model)
            }

            override fun onVibrationDurationResult(duration: Int) {

            }

            override fun onPowerSavingResult(isOpen: Boolean) {
                LogUtils.d("省电设置 ->$isOpen")
                mPowerSaving.postValue(isOpen)
            }

            override fun onOverlayScreenResult(isOpen: Boolean) {
                LogUtils.d("覆盖息屏 ->$isOpen")
                mOverlayScreen.postValue(isOpen)
            }

            override fun onRapidEyeMovementResult(isOpen: Boolean) {
                LogUtils.d("快速眼动 ->$isOpen")
                mRapidEyeMovement.postValue(isOpen)
            }

            override fun onWristScreenResult(bean: WristScreenBean?) {
                LogUtils.d("抬腕亮屏 ->${GsonUtils.toJson(bean)}")
                mWristScreen.postValue(bean)
            }

            override fun onDoNotDisturbModeResult(bean: DoNotDisturbModeBean?) {
                LogUtils.d("勿扰模式 ->${GsonUtils.toJson(bean)}")
                mDoNotDisturbMode.postValue(bean)
            }

            override fun onHeartRateMonitorResult(bean: HeartRateMonitorBean?) {
                LogUtils.d("心率检测 ->${GsonUtils.toJson(bean)}")
                mHeartRateMonitor.postValue(bean)
            }

            override fun onScreenDisplayResult(bean: ScreenDisplayBean?) {
                LogUtils.d("息屏设置 ->${GsonUtils.toJson(bean)}")
                mScreenDisplay.postValue(bean)
            }

            override fun onScreenSettingResult(bean: ScreenSettingBean?) {
                LogUtils.d("屏幕设置 ->${GsonUtils.toJson(bean)}")
                mScreenSetting.postValue(bean)
            }

//            override fun onPhysiologicalCycleResult(bean: PhysiologicalCycleBean?) {
//                LogUtils.d("生理周期 ->${GsonUtils.toJson(bean)}")
//                mPhysiologicalCycle.postValue(bean)
//            }

            override fun onSedentaryReminderResult(bean: CommonReminderBean?) {
                LogUtils.d("久坐提醒 ->${GsonUtils.toJson(bean)}")
                mSedentaryReminder.postValue(bean)
            }

            override fun onDrinkWaterReminderResult(bean: CommonReminderBean?) {
                LogUtils.d("喝水提醒 ->${GsonUtils.toJson(bean)}")
                mDrinkWaterReminder.postValue(bean)
            }

            override fun onMedicationReminderResult(bean: CommonReminderBean?) {
                LogUtils.d("吃药提醒 ->${GsonUtils.toJson(bean)}")
                mMedicationReminder.postValue(bean)
            }

            override fun onHaveMealsReminderResult(haveMealsReminder: CommonReminderBean?) {
                LogUtils.d("吃饭提醒 ->${GsonUtils.toJson(haveMealsReminder)}")
            }

            override fun onWashHandReminderResult(washHandReminder: CommonReminderBean?) {
                LogUtils.d("洗手提醒 ->${GsonUtils.toJson(washHandReminder)}")
                mHandWashingRemider.postValue(washHandReminder)
            }

            override fun onSleepReminder(washHandReminder: SleepReminder?) {

            }

            override fun onEventInfoResult(list: MutableList<EventInfoBean>?, supportMax: Int) {
                LogUtils.d("事件提醒 —>${GsonUtils.toJson(list)},支持最大数max = $supportMax")
                mEventMax.postValue(supportMax)
                mEventInfo.postValue(list)
            }

            override fun onClockInfoResult(list: MutableList<ClockInfoBean>?, supportMax: Int) {
                LogUtils.d("闹钟提醒 —>${GsonUtils.toJson(list)},支持最大数max = $supportMax")
                mClockMax.postValue(supportMax)
                mClockInfo.postValue(list)
            }

            override fun onSimpleSettingResult(bean: SimpleSettingSummaryBean?) {
                LogUtils.d("简单设置汇总 ->${GsonUtils.toJson(bean)}")
                simpleSettingSummary.postValue(bean)
            }

            override fun onMotionRecognitionResult(isAutoRecognition: Boolean, isAutoPause: Boolean) {
            }

            override fun onWorldClockResult(list: MutableList<WorldClockBean>?) {
                LogUtils.d("世界时钟 ->${GsonUtils.toJson(list)}")
                mWorldClockList.postValue(list)
            }

            override fun onBodyTemperatureSettingResult(bodyTemperatureSettingBean: BodyTemperatureSettingBean?) {
                LogUtils.d("连续体温设置 ->${GsonUtils.toJson(bodyTemperatureSettingBean)}")
            }

            override fun onClassicBleStateSetting(classicBluetoothStateBean: ClassicBluetoothStateBean?) {
                classicBluetoothState.postValue(classicBluetoothStateBean)
            }

            override fun onSchoolModeResult(schoolBean: SchoolBean?) {
                LogUtils.d("学校模式设置 ->${GsonUtils.toJson(schoolBean)}")
            }

            override fun onSchedulerResult(schedulerBean: SchedulerBean?) {
                LogUtils.d("调度器设置 ->${GsonUtils.toJson(schedulerBean)}")
            }

            override fun onSleepModeResult(sleepModeBean: SleepModeBean?) {
                LogUtils.d("睡眠模式 ->${GsonUtils.toJson(sleepModeBean)}")
            }

            override fun onPressureModeResult(pressureModeBean: PressureModeBean?) {
                LogUtils.d("压力模式 ->${GsonUtils.toJson(pressureModeBean)}")
                mPressureMode.postValue(pressureModeBean)
            }

            override fun onNotificationSetting(settingsBean: NotificationSettingsBean?) {
                LogUtils.d("通知设置 ->${GsonUtils.toJson(settingsBean)}")
            }

            override fun onContinuousBloodOxygenSetting(settingsBean: ContinuousBloodOxygenSettingsBean?) {
                LogUtils.d("连续血氧设置 ->${GsonUtils.toJson(settingsBean)}")
            }

            override fun onFindWearSettings(settingsBean: FindWearSettingsBean?) {
                LogUtils.d("找手表设置 ->${GsonUtils.toJson(settingsBean)}")
            }
        }
        //endregion

        //region 语言设置列表
        CallBackUtils.languageCallback = object : LanguageCallBack {
            override fun onResult(bean: LanguageListBean?) {
                if (bean != null) {
                    LogUtils.d("语言设置列表 ->${GsonUtils.toJson(bean)}")
                    mLanguageList.postValue(bean)
                    Global.deviceSelectLanguageId = bean.selectLanguageId
                }
            }
        }
        //endregion

        //region 联系人
        CallBackUtils.contactCallBack = object : ContactCallBack {
            override fun onContactResult(data: ArrayList<ContactBean>) {
                if (data != null) {
                    contactList.postValue(data)
                }
            }
        }
        //endregion

        //设备小功能综合
        MicroManager.initMicroCallBack()

        //通知权限未开启，提示音乐无权限
        if (!MyNotificationsService.isEnabled(BaseApplication.mContext)) {
            CallBackUtils.musicCallBack = object : MusicCallBack {
                override fun onRequestMusic() {
                    //无权限
                    ControlBleTools.getInstance().syncMusicInfo(MusicInfoBean(1, "", 0, 0), null)
                }

                override fun onSyncMusic(errorCode: Int) {}

                override fun onQuitMusic() {}

                override fun onSendMusicCmd(command: Int) {}
            }
        }

        //断连原因
        CallBackUtils.setDisconnectReasonCallBack(object : DisconnectReasonCallBack {
            override fun onReason(deviceInfoBean: DeviceInfoBean?) {
                if (deviceInfoBean == null) return
                LogUtils.d(
                    "DisconnectReason",
                    "断连信息：${deviceInfoBean} \n" +
                            "time: ${deviceInfoBean?.lastDisconnectTimestamp} ," +
                            "reason: ${deviceInfoBean?.lastDisconnectReason} , \n " +
                            "log: " + "${deviceInfoBean?.sdkLastDisconnectLog}"
                )
                if (!TextUtils.isEmpty(deviceInfoBean.equipmentNumber) &&
                    !TextUtils.isEmpty(deviceInfoBean.firmwareVersion) &&
                    !TextUtils.isEmpty(deviceInfoBean.mac) &&
                    deviceInfoBean.lastDisconnectTimestamp != 0L
                    && deviceInfoBean.lastDisconnectReason != 0L
                ) {
                    postDisconnectReason(deviceInfoBean)
                }
            }
        })

        //埋点日志
        CallBackUtils.setFirmwareTrackingLogCallBack(object : FirmwareTrackingLogCallBack {
            override fun onTrackingLog(trackingLogBean: TrackingLogBean?) {
                if (trackingLogBean != null) {
                    //ConvertUtils.bytes2String(trackingLogBean.trackingLog, "UTF-8")
                    com.smartwear.xzfit.utils.LogUtils.d("FirmwareTracking","设备埋点日志：" + trackingLogBean.fileName + ",\n" + ConvertUtils.bytes2String(trackingLogBean.trackingLog, "UTF-8"))
                    AppTrackingManager.postDeviceTrackingLog(trackingLogBean)

                    val trackingLog = TrackingLog.getSerTypeTrack("设备埋点日志上传服务器", "设备日志埋点上传", "dev/log/bulk")
                    trackingLog.serReqJson = GsonUtils.toJson("设备埋点日志：" + trackingLogBean.fileName + ",\n" + ConvertUtils.bytes2String(trackingLogBean.trackingLog, "UTF-8"))
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)

                }
            }
        })

        //天气
        CallBackUtils.setWeatherCallBack(object : WeatherCallBack {
            override fun onRequestWeather() {
                WeatherManagerUtils.syncWeather()
            }
        })

        /*CallBackUtils.setWithoutServiceBroadcastCallBck(object :WithoutServiceBroadcastCallBck{
            override fun onBroadcastDevice(device: ScanDeviceBean?) {
                if(device == null) return
                //EventBus.getDefault().post(EventMessage(EventAction.ACTION_SIFLI_WITHOUT_SERVICE,device))
            }
        })*/
    }

    //连接超时失败上次日志,避免多次重复日志上传
    var connectTrackingLastLog = ""

    private fun postDisconnectReason(deviceInfoBean: DeviceInfoBean) {
        scope.launch {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(userId)) return@launch
                val disBean = DisconnectionBean()
                disBean.userId = userId
                disBean.deviceType = deviceInfoBean.equipmentNumber
                disBean.deviceVersion = deviceInfoBean.firmwareVersion
                disBean.deviceMac = deviceInfoBean.mac
                disBean.sn = deviceInfoBean.serialNumber
                disBean.keepAlivePermission = "0"
                disBean.phoneModel = AppUtils.getPhoneType()
                disBean.phoneSystemVersion = AppUtils.getOsVersion()
                disBean.appPushTime = "${System.currentTimeMillis()}"
                disBean.startAppTimestamp = Global.appStartTimestamp.toString()
                disBean.batteryInfo = Global.deviceCapacity.toString()
                disBean.disconnectionTimestamp = deviceInfoBean.lastDisconnectTimestamp.toString()
                disBean.disconnectReasonCode = deviceInfoBean.lastDisconnectReason.toString()
                if (!TextUtils.isEmpty(deviceInfoBean.sdkLastDisconnectLog))
                    disBean.deviceLog = deviceInfoBean.sdkLastDisconnectLog
                MyRetrofitClient.service.deviceDisconnection(JsonUtils.getRequestJson("disconnection", disBean, DisconnectionBean::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //region 设备应用列表排序
    fun postWidgetList(list: List<WidgetBean>) {
        mWidgetList.postValue(list.toMutableList())
    }
    //endregion

    //region 设备运动排序
    fun postSportList(list: List<WidgetBean>) {
        mSportList.postValue(list.toMutableList())
    }

    //endregion

    /**
     * 解绑/更换启用设备时重置数据
     * */
    fun resetData() {
        mShortReply.value = null
        mVibrationMode.value = null
        mPowerSaving.value = null
        mWristScreen.value = null
        mDoNotDisturbMode.value = null
        mHeartRateMonitor.value = null
        mScreenDisplay.value = null
        mScreenSetting.value = null
        mPhysiologicalCycle.value = null
        mSedentaryReminder.value = null
        mDrinkWaterReminder.value = null
        mMedicationReminder.value = null
        mHandWashingRemider.value = null
        mEventInfo.value = null
        mEventMax.value = null
        mClockInfo.value = null
        mClockMax.value = null
        mLanguageList.value = null
        mWidgetList.value = null
        //清除与设备相关的SP存储
        SpUtils.remove(SpUtils.WEATHER_SWITCH)
        SpUtils.remove(SpUtils.WOMEN_HEALTH_SUM_SAFETY_PERIOD)
    }

}