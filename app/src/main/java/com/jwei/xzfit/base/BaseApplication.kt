package com.jwei.xzfit.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.BleStateCallBack
import com.zhapp.ble.callback.DeviceLogCallBack
import com.zhapp.ble.callback.NotificationClickCallBack
import com.jwei.xzfit.R
import com.jwei.xzfit.receiver.*
import com.jwei.xzfit.service.LocationService
import com.jwei.xzfit.service.MyNotificationsService
import com.jwei.xzfit.ui.DeviceFragment
import com.jwei.xzfit.ui.GlobalEventManager
import com.jwei.xzfit.ui.HealthyFragment
import com.jwei.xzfit.ui.WelcomeActivity
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.debug.manager.DebugDevConnectMonitor
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.ui.refresh.CustomizeRefreshHeader
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.GoogleFitManager
import com.jwei.xzfit.utils.manager.StravaManager
import com.jwei.xzfit.viewmodel.DeviceModel
import com.sifli.siflidfu.SifliDFUService
import com.sifli.watchfacelibrary.SifliWatchfaceService
import org.greenrobot.eventbus.EventBus
import org.litepal.LitePal
import kotlin.math.abs


/**
 * Created by android
 * on 2021/7/14
 */
class BaseApplication : Application(), OnMapsSdkInitializedCallback {
    companion object {
        var gson: Gson = GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

        @SuppressLint("StaticFieldLeak")
        lateinit var application: BaseApplication

        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        lateinit var mContext: Context
        var isShowIngOfflineDialog = false
        var isIntoBackground = false

        //是否运行了启动页面
        var isLaunchedWelcomeActivity = false


        /**
         * 初始化刷新加载控件
         * */
        fun initCustomizeRefreshLayout() {
            SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
                CustomizeRefreshHeader(context)
            }
            SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
                ClassicsFooter(context)
            }
        }

        /**
         * 数据授权
         */
        @SuppressLint("MissingPermission")
        fun initDataReduction() {
            com.jwei.xzfit.utils.AppUtils.tryBlock {
                if (NetworkUtils.isConnected()) {
                    NetworkUtils.isAvailableAsync { isAvailable ->
                        if (isAvailable) {
                            val topActivity = ActivityUtils.getTopActivity()
                            if (topActivity == null) return@isAvailableAsync
                            //Googl fit
                            if (SpUtils.getGooglefitSwitch()) {
                                GoogleFitManager.authorizationGoogleFit(topActivity, object : GoogleFitManager.GoogleFitAuthListener {
                                    override fun onAccessSucceeded() {
                                        LogUtils.d("DataReduction", "Google Fit 授权成功")
                                    }

                                    override fun onFailure(msg: String) {
                                        LogUtils.e("DataReduction", "Google Fit 授权失败：$msg")
                                    }
                                })
                            }

                            //Strava
                            if (SpUtils.getStravaSwitch()) {
                                StravaManager.authorizationStrava(topActivity, object : StravaManager.StravaAuthListener {
                                    override fun onAccessSucceeded() {
                                        LogUtils.d("DataReduction", "Strava 授权成功")
                                    }

                                    override fun onFailure(msg: String) {
                                        LogUtils.e("DataReduction", "Strava 授权失败：$msg")
                                    }

                                })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val name = ProcessUtils.getCurrentProcessName()
        if (packageName == name) {
            application = this
            mContext = applicationContext
            //记录启动时间戳
            Global.appStartTimestamp = System.currentTimeMillis()
            //定位服务
            LocationService.initLocationService(mContext)
            //网络状态监听
            initNetWorkListener()
            //数据库
            LitePal.initialize(this)
            //debug log
            LogUtils.getConfig().setLogSwitch(com.jwei.xzfit.utils.AppUtils.isBetaApp()).setBorderSwitch(false)
                .setGlobalTag("Beta_Log")
            //异常崩溃记录
            CrashUtils.init()
            //通知服务唤醒
            startMyNotificationsService()
            //广播
            initBroadcast()
            Global.startAppUpData()
            //全局事件处理
            GlobalEventManager.initEventBus()
            //日志保存
            SaveLog.init(this)
            com.zhapp.ble.utils.SaveLog.init(this)
            HttpLog.init(this)
            //bugly
            CrashReport.initCrashReport(mContext, "72e8206b11", AppUtils.isAppDebug())
            //创建固件升级测试文件目录
            initBetaFireWareUpPath()
            //前后台监听
            initAppStatusChangeListener()
            //初始化刷新加载控件
            initCustomizeRefreshLayout()
            //创建鲁班压缩路径文件夹
            FileUtils.createOrExistsDir(Global.LUBAN_CACHE_DIR)

            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
        }
    }

    /**
     * 初始化设备sdk
     */
    fun initControlBleTools(context: Context) {
        //设备日志 仅测试版本写日志
        val isWriteLog = com.jwei.xzfit.utils.AppUtils.isBetaApp()
        ControlBleTools.getInstance().setDeviceLogCallBack(object : DeviceLogCallBack {
            override fun onLogI(tag: String?, msg: String?) {
                if (!tag.isNullOrEmpty() && !msg.isNullOrEmpty()) {
                    Log.i("sdk_log_$tag", msg)
                }
                if (isWriteLog) {
                    com.zhapp.ble.utils.SaveLog.writeFile(tag, msg)
                }
            }

            override fun onLogV(tag: String?, msg: String?) {
                if (!tag.isNullOrEmpty() && !msg.isNullOrEmpty()) {
                    Log.v("sdk_log_$tag", msg)
                }
                if (isWriteLog) {
                    com.zhapp.ble.utils.SaveLog.writeFile(tag, msg)
                }
            }

            override fun onLogE(tag: String?, msg: String?) {
                if (!tag.isNullOrEmpty() && !msg.isNullOrEmpty()) {
                    Log.e("sdk_log_$tag", msg)
                }
                if (isWriteLog) {
                    com.zhapp.ble.utils.SaveLog.writeFile(tag, msg)
                    //E级别蓝牙日志记录到反馈日志内部
                    SaveLog.writeFile(tag, msg, true)
                }
            }

            override fun onLogD(tag: String?, msg: String?) {
                if (!tag.isNullOrEmpty() && !msg.isNullOrEmpty()) {
                    Log.d("sdk_log_$tag", msg)
                }
                if (isWriteLog) {
                    com.zhapp.ble.utils.SaveLog.writeFile(tag, msg)
                }
            }

            override fun onLogW(tag: String?, msg: String?) {
                if (!tag.isNullOrEmpty() && !msg.isNullOrEmpty()) {
                    Log.w("sdk_log_$tag", msg)
                }
                if (isWriteLog) {
                    com.zhapp.ble.utils.SaveLog.writeFile(tag, msg)
                }
            }
        })

        if (ControlBleTools.getInstance().isInit) {
            com.jwei.xzfit.utils.LogUtils.d("SDK", "initControlBleTools return : isInit == true")
            return
        }
        ControlBleTools.getInstance().init(context,
            mContext.getString(R.string.notificattion_title),
            mContext.getString(R.string.notificattion_text),
            R.mipmap.icon_notification,
            object : BleStateCallBack {
                override fun onConnectState(state: Int) {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE, state))
                    DebugDevConnectMonitor.devConnectStateChange(state)
                }
            })

        //sdk 初始化完成，刷新绑定列表
        ControlBleTools.getInstance().setInitStatusCallBack {
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
        }


        //开启无声音乐
        ControlBleTools.getInstance().enableUseSilenceMusic(true, 5000)

        //设置通知点击事件
        ControlBleTools.getInstance().setNotificationClickCallBack(object : NotificationClickCallBack {
            override fun onNotificationClick() {
                com.jwei.xzfit.utils.AppUtils.tryBlock {
                    AppUtils.launchAppDetailsSettings()
                }
            }
        })

        ControlBleTools.getInstance().isScanBroadcastWithoutService(true)
    }

    /**
     * 初始化广播
     * */
    private fun initBroadcast() {
        //没有可用相机广播
        val disableCameraFilter = IntentFilter()
        disableCameraFilter.addAction("android.intent.action.BOOT_COMPLETED")
        registerReceiver(DisableCameraReceiver(), disableCameraFilter)
        //关闭相机广播
        val closeCameraFilter = IntentFilter()
        closeCameraFilter.addAction(Global.TAG_CLOSE_PHOTO_ACTION)
        registerReceiver(CloseCameraReceiver(), closeCameraFilter)
        //蓝牙状态&蓝牙设备与APP连接广播
        val bleFilter = IntentFilter()
        bleFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)   // 监视蓝牙关闭和打开的状态
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) // 监视蓝牙设备与APP断开的状态
        bleFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)    // 监视蓝牙设备与APP连接的状态
        registerReceiver(BluetoothMonitorReceiver(), bleFilter)
        //gps状态变化
        val gpsFilter = IntentFilter()
        gpsFilter.addAction(Global.ACTION_PROVIDERS_CHANGED)
        registerReceiver(GPSReceiver(), gpsFilter)
        //语言切换
        val localeChangeFilter = IntentFilter()
        localeChangeFilter.addAction(Intent.ACTION_LOCALE_CHANGED)
        registerReceiver(LocaleChangeReceiver(), localeChangeFilter)
        //时间变化
        val timeUpdateFilter = IntentFilter()
        timeUpdateFilter.addAction(Intent.ACTION_TIME_TICK)
        timeUpdateFilter.addAction(Intent.ACTION_TIME_CHANGED)
        registerReceiver(TimeUpdateReceiver(), timeUpdateFilter)
        //思澈sdk本地关闭
        val SifliFilter = IntentFilter()
        SifliFilter.addAction(SifliDFUService.BROADCAST_DFU_LOG)
        SifliFilter.addAction(SifliDFUService.BROADCAST_DFU_STATE)
        SifliFilter.addAction(SifliDFUService.BROADCAST_DFU_PROGRESS)
        SifliFilter.addAction(SifliWatchfaceService.BROADCAST_WATCHFACE_STATE)
        SifliFilter.addAction(SifliWatchfaceService.BROADCAST_WATCHFACE_PROGRESS)
        LocalBroadcastManager.getInstance(this).registerReceiver(SifliReceiver(), SifliFilter)
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver!!)

    }

    //region 通知服务
    private var notificationServiceConnection: NotificationServiceConnection? = null

    /**
     * 启动通知服务
     */
    fun startMyNotificationsService() {
        com.jwei.xzfit.utils.AppUtils.tryBlock {
            if (notificationServiceConnection != null) {
                unbindService(notificationServiceConnection!!)
            }
        }
        com.jwei.xzfit.utils.AppUtils.tryBlock {
            notificationServiceConnection = NotificationServiceConnection()
            val notificationsServiceIntent = Intent(this, MyNotificationsService::class.java)
            bindService(notificationsServiceIntent, notificationServiceConnection!!, Service.BIND_AUTO_CREATE)
        }
    }

    class NotificationServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            com.jwei.xzfit.utils.LogUtils.d("MyNotificationsService", "onServiceConnected:$name")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            com.jwei.xzfit.utils.LogUtils.d("MyNotificationsService", "onServiceDisconnected:$name")
        }
    }
    //endregion

    /**
     * 初始化测试版本固件路径
     * */
    private fun initBetaFireWareUpPath() {
        if (com.jwei.xzfit.utils.AppUtils.isBetaApp()) {
            FileUtils.createOrExistsDir(PathUtils.getAppDataPathExternalFirst() + "/otal/fireware")
            FileUtils.createOrExistsDir(PathUtils.getAppDataPathExternalFirst() + "/otal/dial")
            FileUtils.createOrExistsDir(PathUtils.getAppDataPathExternalFirst() + "/otal/customDial")
            FileUtils.createOrExistsDir(PathUtils.getExternalAppFilesPath() + "/decoding")
        }
    }

    /**
     * 网络状态监听
     * */
    private fun initNetWorkListener() {
        var oldTime = 0L
        val filtration = 5000L //过滤时间间隔，5s内不重复
        NetworkUtils.registerNetworkStatusChangedListener(object : NetworkUtils.OnNetworkStatusChangedListener {
            override fun onDisconnected() {
                if (abs(System.currentTimeMillis() - oldTime) < filtration) {
                    oldTime = System.currentTimeMillis()
                    return
                }
                oldTime = System.currentTimeMillis()

                com.jwei.xzfit.utils.LogUtils.e("NetWork", "网络断开连接", true)
                ToastUtils.showToast(R.string.not_network_tips)
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_NETWORK_DISCONNECTED))
            }

            override fun onConnected(networkType: NetworkUtils.NetworkType?) {
                if (abs(System.currentTimeMillis() - oldTime) < filtration) {
                    oldTime = System.currentTimeMillis()
                    return
                }
                oldTime = System.currentTimeMillis()

                com.jwei.xzfit.utils.LogUtils.e("NetWork", "网络连接上", true)
                //刷新设备语言列表
                if (Global.deviceLanguageList == null) {
                    Global.getDevLanguage()
                }
                //刷新设备产品列表
                if (Global.productList.isNullOrEmpty()) {
                    DeviceModel().getProductList()
                }
                //刷新绑定设备列表
                if (!Global.IS_BIND_DEVICE) {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_NETWORK_CONNECTED))
                }
                //数据授权
                initDataReduction()
            }
        })
    }

    /**
     * 应用前后台切换监听
     * */
    private fun initAppStatusChangeListener() {
        AppUtils.registerAppStatusChangedListener(object : Utils.OnAppStatusChangedListener {
            override fun onForeground(activity: Activity?) {
                LogUtils.d(" 返回前台 ----> ${AppUtils.getAppPackageName()}")
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_APP_STATUS_CHANGE))
                isIntoBackground = false
                if (HealthyFragment.viewIsVisible || DeviceFragment.viewIsVisible) {
                    RealTimeRefreshDataUtils.openRealTime()
                }
            }

            override fun onBackground(activity: Activity?) {
                LogUtils.d(" 进入后台 ----> ${AppUtils.getAppPackageName()}")
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_APP_STATUS_CHANGE))
                isIntoBackground = true
                RealTimeRefreshDataUtils.closeRealTime()
                //DialogUtils.clearAllDialog()
                ToastUtils.cancel()
            }
        })

        ActivityUtils.addActivityLifecycleCallbacks(object : Utils.ActivityLifecycleCallbacks() {
            override fun onActivityCreated(activity: Activity) {
                super.onActivityCreated(activity)
                if (activity is WelcomeActivity) {
                    isLaunchedWelcomeActivity = true
                } else {
                    if (!isLaunchedWelcomeActivity) {
                        AppUtils.relaunchApp(true)
                    }
                }
            }

            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                //解决部分(三星 google)手机任务栏关闭app后进程存活，导致设备连接异常，功能异常问题
                ThreadUtils.runOnUiThreadDelayed({
                    LogUtils.d(" ActivityList size ----> ${ActivityUtils.getActivityList().size}")
                    if (ActivityUtils.getActivityList().size == 0) {
                        com.jwei.xzfit.utils.LogUtils.e("sdk release", "live Activity size == 0")
                        //清除sdk内部设备信息
                        ControlBleTools.getInstance().disconnect()
                        ControlBleTools.getInstance().release()
                        AppUtils.exitApp()
                    }
                }, 1000)
            }
        })
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            MapsInitializer.Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }
}