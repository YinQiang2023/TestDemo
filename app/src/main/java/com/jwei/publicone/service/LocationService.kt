package com.jwei.publicone.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.*
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.AppUtils
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference
import android.location.GpsSatellite
import com.jwei.publicone.ui.sport.bean.SatelliteBean
import com.amap.api.location.*
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.utils.GpsCoordinateUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.utils.manager.DevSportManager


/**
 * Created by Android on 2021/11/18.
 */
@SuppressLint("StaticFieldLeak")
class LocationService : Service(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    //是否在定位中
    var isLocationDoing = false

    //是否app发起运动
    var isAppSport = false

    companion object {
        val TAG: String = LocationService::class.java.simpleName
        const val id = "channel_loc_service"
        const val name = "location_service"

        private var mContext: Context? = null

        var binder: LocationBinder? = null

        private val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                try {
                    binder = service as LocationBinder
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                LogUtils.d(TAG, "定位服务--> onServiceDisconnected")
            }
        }

        fun initLocationService(context: Context?) {
            if (context == null) throw NullPointerException("context cannot be NULL")
            if (mContext != null) {
                mContext!!.unbindService(conn)
            }
            mContext = context
            val intent = Intent(mContext, LocationService::class.java)
            mContext!!.bindService(intent, conn, BIND_AUTO_CREATE)
        }
    }

    inner class LocationBinder : Binder() {
        val service: LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder {
        LogUtils.d(TAG, "定位服务--> onBind")
        return LocationBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        LogUtils.d(TAG, "定位服务--> onUnbind")
        return super.onUnbind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "定位服务--> onCreate")
        initLocation()
    }

    fun initLocation() {
        //创建通知属性
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        //gps强度监听
        if (satelliteLevelListener == null) {
            setSatelliteLevelListener(object : SatelliteLevelListener {
                override fun onLevelChange(max: Int, valid: Int) {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_GPS_SATELLITE_CHANGE, SatelliteBean(max, valid)))
                }
            })
        }
        //初始化
        if (PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            initAMapLocation()
            initGoogleMapLocation()
            initGPSSatelliteListener()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "定位服务--> onDestroy")
        AppUtils.tryBlock {
            if (mMyALocationListener != null) {
                mLocationClient?.unRegisterLocationListener(mMyALocationListener)
            }
            mLocationClient?.stopLocation()
            mLocationClient?.onDestroy()
            mLocationClient = null
            mMyALocationListener = null

            if (mGoogleApiClient != null && mLocationListener != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient!!,
                    mLocationListener!!
                )
                mGoogleApiClient!!.disconnect()
                mLocationListener = null
                mGoogleApiClient = null
            }
        }
    }

    //region 卫星 （gps信号）
    private var mLocationManager: LocationManager? = null

    private var mMaxSatellites = -1

    fun getMaxSatellites(): Int {
        return mMaxSatellites
    }

    private var mValidCount = -1  //上次有效值

    fun getValidCount(): Int {
        return mValidCount
    }

    private var satelliteLevelListener: SatelliteLevelListener? = null

    fun setSatelliteLevelListener(listener: SatelliteLevelListener) {
        satelliteLevelListener = listener
    }

    @SuppressLint("MissingPermission")
    private fun initGPSSatelliteListener() {
        if (PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (mLocationManager == null) {
                mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mLocationManager?.registerGnssStatusCallback(GnssStatusCallback(this), null)
                } else {
                    //ANDROID 31 以下可用
                    mLocationManager?.addGpsStatusListener(GpsStatusListener(this))
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    class GnssStatusCallback(service: LocationService) : GnssStatus.Callback() {
        private var wrService: WeakReference<LocationService>? = null

        init {
            wrService = WeakReference(service)
        }

        override fun onSatelliteStatusChanged(status: GnssStatus) {
            super.onSatelliteStatusChanged(status)
            if (wrService != null && wrService!!.get() != null) {
                //获取支持卫星最大数
                val maxSatellites = status.satelliteCount
                //卫星有效颗数统计
                var validCount = 0
                //LogUtils.d("卫星总数量：${status.satelliteCount}")
                for (i in 0 until maxSatellites) {
                    if (status.usedInFix(i)) {
                        validCount++
                    }
                }
                //LogUtils.d("有效卫星数量 1 --->：$validCount")
                wrService!!.get()!!.mMaxSatellites = maxSatellites
                wrService!!.get()!!.mValidCount = validCount
                wrService!!.get()!!.satelliteLevelListener?.onLevelChange(maxSatellites, validCount)
            }
        }
    }

    class GpsStatusListener(service: LocationService) : GpsStatus.Listener {
        private var wrService: WeakReference<LocationService>? = null

        init {
            wrService = WeakReference(service)
        }

        @SuppressLint("MissingPermission")
        override fun onGpsStatusChanged(event: Int) {
            if (wrService != null && wrService!!.get() != null) {
                when (event) {
                    GpsStatus.GPS_EVENT_SATELLITE_STATUS -> {
                        //获取当前状态
                        val gpsStatus = wrService!!.get()!!.mLocationManager!!.getGpsStatus(null)
                        //获取支持卫星最大数
                        val maxSatellites = gpsStatus!!.maxSatellites
                        //LogUtils.d("卫星总数量 2 --->：$maxSatellites ")
                        //获取所有的卫星
                        val iters: Iterator<GpsSatellite> = gpsStatus!!.satellites.iterator()
                        //卫星有效颗数统计
                        var validCount = 0
                        while (iters.hasNext() && validCount <= maxSatellites) {
                            val s = iters.next()
                            //LogUtils.d("snr：$snr")
                            if (s.usedInFix()) {
                                validCount++
                            }
                        }
                        //LogUtils.d("有效卫星数量 2 --->:$validCount")
                        //计算 max valid
                        wrService!!.get()!!.mMaxSatellites = maxSatellites
                        wrService!!.get()!!.mValidCount = validCount
                        wrService!!.get()!!.satelliteLevelListener?.onLevelChange(maxSatellites, validCount)
                    }
                }
            }
        }

    }

    /**
     * gps卫星强度接口
     * */
    interface SatelliteLevelListener {
        fun onLevelChange(max: Int, valid: Int)
    }
    //endregion

    //region 前台服务通知
    private var mNotificationManager: NotificationManager? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        if (mNotificationManager == null) {
            mNotificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            mNotificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * 开启前台通知
     * */
    private fun startForgeGroundNotification() {
        try {
            val notification = NotificationCompat.Builder(applicationContext, id)
                .setContentTitle(getString(R.string.main_app_name))
                .setContentText(getString(R.string.notificattion_locatioin_text))
                .setSmallIcon(R.mipmap.icon_notification)
                .setAutoCancel(true)
                .setOngoing(true).build()
            startForeground(View.generateViewId(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭前台通知
     * */
    private fun stopForgeGroundNotification() {
        //NotificationUtils.cancel(mNotificationId)
        stopForeground(true)
    }
    //endregion

    //region 定位相关
    /**
     * 启动定位
     * */
    fun startLocation() {
        AppUtils.tryBlock {
            if (!isLocationDoing) {
                if ((PermissionUtils.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
                            PermissionUtils.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) &&
                    AppUtils.isGPSOpen(BaseApplication.mContext)
                ) {
                    LogUtils.d(TAG, "开始定位")
                    if (mLocationClient == null || mGoogleApiClient == null) {
                        initLocation()
                    }
                    isLocationDoing = true
                    isCanPostLocationError = true
                    stopForgeGroundNotification()
                    startForgeGroundNotification()
                    //启动定位
                    if (!AppUtils.isEnableGoogleMap()) {
                        mLocationClient?.startLocation()
                    } else {
                        if (mGoogleApiClient == null) {
                            initGoogleMapLocation()
                            mGoogleApiClient?.connect()
                        } else {
                            mGoogleApiClient?.connect()
                        }
                    }
                }
            }
        }
    }

    /**
     * 结束定位
     * */
    fun stopLocation() {
        AppUtils.tryBlock {
            LogUtils.d(TAG, "结束定位")
            isLocationDoing = false
            //取消通知
            stopForgeGroundNotification()
            //停止定位
            mLocationClient?.stopLocation()

            if (mGoogleApiClient != null && mLocationListener != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient!!,
                    mLocationListener!!
                )
                mGoogleApiClient!!.disconnect()
            }
        }
    }

    //region 高德定位
    private var mMyALocationListener: MyAMapLocationListener? = null
    private var mLocationClient: AMapLocationClient? = null
    private var mLocationOption: AMapLocationClientOption? = null
    private fun initAMapLocation() {
        AppUtils.tryBlock {
            val wContext = WeakReference(BaseApplication.mContext)
            if (mLocationClient == null) {
                LogUtils.d("initAMapLocation")
                //初始化定位
                mLocationClient = AMapLocationClient(wContext.get())
                //初始化定位参数
                if (mLocationOption == null) {
                    mLocationOption = AMapLocationClientOption()
                    //仅设备模式
                    //mLocationOption?.locationMode = AMapLocationMode.Device_Sensors
                    //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
                    mLocationOption?.isGpsFirst = true
                    //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
                    mLocationOption?.httpTimeOut = 30000
                    //可选，设置定位间隔。默认为2秒
                    mLocationOption?.interval = 1000
                    //可选，设置是否返回逆地理地址信息。默认是true
                    mLocationOption?.isNeedAddress = true
                    //可选，设置是否单次定位。默认是false
                    mLocationOption?.isOnceLocation = false
                    //可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
                    mLocationOption?.isOnceLocationLatest = false
                    //可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
                    AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP)
                    //可选，设置是否使用传感器。默认是false
                    mLocationOption?.isSensorEnable = false
                    //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
                    mLocationOption?.isWifiScan = true
                    //可选，设置是否使用缓存定位，默认为true
                    mLocationOption?.isLocationCacheEnable = true
                }
                //设置定位回调监听
                mMyALocationListener = MyAMapLocationListener()
                mLocationClient?.setLocationListener(mMyALocationListener)
                //设置为高精度定位模式
                mLocationOption?.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                //设置定位参数
                mLocationClient?.setLocationOption(mLocationOption)
            }
        }
    }

    private var isCanPostLocationError = true

    inner class MyAMapLocationListener : AMapLocationListener {
        private var sportingTipsNoGps = 10
        override fun onLocationChanged(location: AMapLocation?) {
            if (location == null) {
                LogUtils.e("location == null")
                return
            }
            //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
            if (location.errorCode != 0) {
                LogUtils.e("errorcode =" + location.errorCode + " info=" + location.errorInfo + " detail=" + location.locationDetail)
                if (DevSportManager.isDeviceSporting) {
                    if(isCanPostLocationError) {
                        isCanPostLocationError = false
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("定位超时/失败"), "2115", true)
                    }
                }
                if (AppUtils.isGPSOpen(BaseApplication.mContext)) {
                    ToastUtils.showToast(R.string.sport_no_gps)
                } else {
                    if (sportingTipsNoGps == 0) {
                        ToastUtils.showToast(BaseApplication.mContext.getString(R.string.device_sport_no_gps_tips))
                        sportingTipsNoGps = 10
                    } else {
                        sportingTipsNoGps--
                    }
                }
                return
            }
            LogUtils.d("高德定位 - $location")
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_LOCATION, location))
        }
    }
    //endregion


    //region Google定位
    private var mLocationRequest: LocationRequest? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationListener: MyGoogleLocationListener? = null

    @SuppressLint("MissingPermission")
    fun initGoogleMapLocation() {
        AppUtils.tryBlock {
            if (AppUtils.isEnableGoogleMap() && mGoogleApiClient == null) {
                LogUtils.d("initGoogleMapLocation")
                mGoogleApiClient = GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
            }
        }
    }

    inner class MyGoogleLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            AppUtils.tryBlock {
                if (!GpsCoordinateUtils.isOutOfChina(location.latitude, location.longitude)) {
                    val newLatlng = GpsCoordinateUtils.calWGS84toGCJ02(location.latitude, location.longitude)
                    LogUtils.d("Google定位 2 - $location \nGoogle定位 - 中国定位点 wgs84ToGcj02 位置 = ${newLatlng[0]},${newLatlng[1]}") //22.628857,113.838142
                    location.latitude = newLatlng[0]
                    location.longitude = newLatlng[1]
                } else {
                    LogUtils.d("Google定位 2 - $location")
                }
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_LOCATION, location))
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        AppUtils.tryBlock {
            if (mGoogleApiClient == null) return@tryBlock
            val location: Location? = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient!!)
            location?.let { location ->
                //中国内定位点修正赋值
                if (!GpsCoordinateUtils.isOutOfChina(location.latitude, location.longitude)) {
                    val newLatlng = GpsCoordinateUtils.calWGS84toGCJ02(location.latitude, location.longitude)
                    LogUtils.d("Google定位 1 - $location \nGoogle定位 - 中国定位点 wgs84ToGcj02 位置 = ${newLatlng[0]},${newLatlng[1]}") //22.628857,113.838142
                    location.latitude = newLatlng[0]
                    location.longitude = newLatlng[1]
                } else {
                    LogUtils.d("Google定位 1 - $location")
                }
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_LOCATION, location))
            }
            if (mLocationRequest == null) {
                mLocationRequest = LocationRequest.create()
                mLocationRequest!!.interval = (1 * 1000).toLong() //1 seconds //间隔
                mLocationRequest!!.fastestInterval = (1 * 1000).toLong() //1 seconds
                mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //定位模式 - 高精度
                mLocationRequest!!.smallestDisplacement = 5f //定位精度
            }
            if (mLocationListener == null) {
                mLocationListener = MyGoogleLocationListener()
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient!!,
                mLocationRequest!!,
                mLocationListener!!
            )
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        LogUtils.d(TAG, "Google  --> onConnectionSuspended ")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        LogUtils.d("Google  --> onConnectionFailed ")
    }
    //endregion
    //endregion
}