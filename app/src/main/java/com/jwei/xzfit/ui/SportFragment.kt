package com.jwei.xzfit.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.commit
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.*
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.blankj.utilcode.util.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.tabs.TabLayout
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.databinding.FragmentSportBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.service.LocationService
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.ui.sport.SportCountDownActivity
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.GpsCoordinateUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.viewmodel.SportModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import com.jwei.xzfit.R


class SportFragment : BaseFragment<FragmentSportBinding, SportModel>(
    FragmentSportBinding::inflate, SportModel::class.java
), View.OnClickListener, LocationSource, OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnCameraMoveListener, com.google.android.gms.maps.LocationSource {
    //运动类型  0 -户外运动  1 -户外骑行  2-户外健走
    private var mSportType = 0

    private var isInitMap = false

    //是否点击start
    private var isClickStart = false

    private var isClickHome = false

    //region 地图
    private var savedInstanceState: Bundle? = null

    //地图是否第一次准备完毕
    private var isFirstLoadMap = false

    //高德
    private var mapview: TextureMapView? = null
    private lateinit var mAMap: AMap
    private var mListener: LocationSource.OnLocationChangedListener? = null
    private var mMyALocationListener: MyFragmentAMapLocationListener? = null
    private var mLocationClient: AMapLocationClient? = null
    private var mLocationOption: AMapLocationClientOption? = null
    private var mOldLatLng: LatLng? = null

    //Google
    private var mGoogleMap: GoogleMap? = null
    private var mMapFragment: SupportMapFragment? = null

    //private var mGMapView: MapView? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mGoogleListener: com.google.android.gms.maps.LocationSource.OnLocationChangedListener? = null
    private var mGoogleLocationListener: LocationListener? = null
    private var mLastLocation: Location? = null
    private var mGOldLatLng: com.google.android.gms.maps.model.LatLng? = null
    //endregion

    override fun setTitleId(): Int {
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        AppUtils.registerEventBus(this)

        if (!AppUtils.isEnableGoogleMap()) {
            binding.aMap.visibility = View.VISIBLE
            binding.frGoogleMap.visibility = View.GONE
        } else {
            binding.aMap.visibility = View.GONE
            if (AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                binding.frGoogleMap.visibility = View.VISIBLE
            }
        }
        if (PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            isFirstLoadMap = true
            isInitMap = true
            if (!AppUtils.isEnableGoogleMap()) {
                initAMap()
            } else {
                initGoogleMap()
            }
        }
        setViewsClickListener(this, binding.ivStart, binding.ivHoming)
    }

    //region 高德地图
    private fun initAMap() {
        //高德没有隐藏地图api ，隐藏图标
        binding.aMap.onCreate(savedInstanceState)
        mapview = binding.aMap
        mAMap = binding.aMap.map
        val mUiSettings = mAMap.uiSettings
        mUiSettings.isRotateGesturesEnabled = true //是否允许旋转
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(17f)) //将地图的缩放级别调整到16级
        if (!AppUtils.isZh(BaseApplication.mContext)) {
            mAMap.setMapLanguage("en")
        }
        // 设置定位监听
        mAMap.setLocationSource(this)
        // 设置默认定位按钮是否显示
        mUiSettings.isMyLocationButtonEnabled = false
        // 设置缩放按钮是否显示
        mUiSettings.isZoomControlsEnabled = false
        // 自定义系统定位蓝点
        val myLocationStyle = MyLocationStyle()
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.mipmap.sport_anchor))
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0))
        // 自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(0f)
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0))
        //连续定位、且将视角移动到地图中心点，定位蓝点跟随设备移动
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
        // 将自定义的 myLocationStyle 对象添加到地图上
        mAMap.myLocationStyle = myLocationStyle
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        mAMap.isMyLocationEnabled = true
        //开始定位
        startLocation()
    }

    /**
     * 开始定位
     * */
    fun startLocation() {

        val wContext = WeakReference(/*this as Context act泄露*/ BaseApplication.mContext)
        //初始化定位
        mLocationClient = AMapLocationClient(wContext.get())
        //初始化定位参数
        mLocationOption = AMapLocationClientOption()
        //省电模式
        mLocationOption?.locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
        //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mLocationOption?.isGpsFirst = true
        //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        //mLocationOption?.httpTimeOut = 30000
        //只定一次位
        mLocationOption?.isOnceLocation = true
        mLocationOption?.isOnceLocationLatest = true
        //可选，设置定位间隔。默认为2秒
        mLocationOption?.interval = 1000L
        //可选，设置是否返回逆地理地址信息。默认是true
        mLocationOption?.isNeedAddress = true
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
        //设置定位回调监听
        mMyALocationListener = MyFragmentAMapLocationListener(this)
        mLocationClient?.setLocationListener(mMyALocationListener)
        //设置定位参数
        mLocationClient?.setLocationOption(mLocationOption)
        //启动定位
        mLocationClient?.startLocation()
    }

    override fun activate(listener: LocationSource.OnLocationChangedListener?) {
        mListener = listener
    }

    /**
     * 定位回调
     * */
    inner class MyFragmentAMapLocationListener(fragment: SportFragment) : AMapLocationListener {
        private var wrFragment: WeakReference<SportFragment>? = null

        init {
            wrFragment = WeakReference(fragment)
        }

        override fun onLocationChanged(location: AMapLocation?) {
            wrFragment?.get()?.apply {
                binding.ivNoMap.visibility = View.GONE
                //LogUtils.e("onLocationChanged")
                if (location == null) {
                    LogUtils.d("location == null")
                    return
                }
                //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                if (location.errorCode != 0) {
                    LogUtils.d("errorCode =" + location.errorCode + " info=" + location.errorInfo + " detail=" + location.locationDetail)
                    ToastUtils.showToast(R.string.sport_no_gps)
                    return
                }
                LogUtils.d("高德定位 - ${location.latitude} , ${location.longitude}") //22.628657 , 113.838232
                mOldLatLng = LatLng(location.latitude, location.longitude)
                //SP 存定位点
                if (mOldLatLng != null) {
                    if (SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").isEmpty()) {
                        SpUtils.setValue(
                            SpUtils.WEATHER_LONGITUDE_LATITUDE,
                            "${mOldLatLng!!.longitude},${mOldLatLng!!.latitude}"
                        )
                    }
                    SpUtils.setValue(SpUtils.LOCATION_CACHE, "${mOldLatLng!!.longitude},${mOldLatLng!!.latitude}")
                }
                // 刷新显示系统小蓝点
                mListener?.onLocationChanged(location)
                //地图放大
                ThreadUtils.runOnUiThreadDelayed({
                    mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mOldLatLng, 17f), 1000, null)
                }, if (isFirstLoadMap) 3000 else 0)
                isFirstLoadMap = false
            }
        }
    }
    //endregion

    //region google地图
    private fun initGoogleMap() {
        AppUtils.tryBlock {
            if (AppUtils.isEnableGoogleMap()) {
                if (mMapFragment == null) {
                    LogUtils.d("initGoogleMap --->" + Thread.currentThread())
                    mMapFragment = SupportMapFragment.newInstance()
                    parentFragmentManager.commit(true) {
                        add(R.id.fr_google_map, mMapFragment!!)
                    }
                    mMapFragment?.getMapAsync(this)
                }
            }
        }
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        AppUtils.tryBlock {
            mGoogleApiClient = GoogleApiClient.Builder(BaseApplication.mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        AppUtils.tryBlock {
            LogUtils.d("onMapReady")
            if (mGoogleMap == null) {
                mGoogleMap = map
                mGoogleMap?.setOnCameraMoveListener(this)
                mGoogleMap?.setLocationSource(this)
                mGoogleMap?.isMyLocationEnabled = true
                mGoogleMap?.uiSettings?.isMapToolbarEnabled = false
                mGoogleMap?.uiSettings?.isMyLocationButtonEnabled = false
                buildGoogleApiClient()
                mGoogleApiClient!!.connect()
                //移动地图至历史定位
                val spGps = SpUtils.getValue(SpUtils.LOCATION_CACHE, "");
                if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                    val gps = SpUtils.getValue(SpUtils.LOCATION_CACHE, "").trim().split(",")
                    if (gps.isNotEmpty() && gps.size == 2) {
                        val gpsLatlng = GpsCoordinateUtils.calGCJ02toWGS84(gps[1].toDouble(), gps[0].toDouble())
                        mGOldLatLng = com.google.android.gms.maps.model.LatLng(
                            gpsLatlng.get(0),
                            gpsLatlng.get(1)
                        )
                        mGoogleMap?.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(mGOldLatLng!!, 16f))
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onConnected(bundle: Bundle?) {
        AppUtils.tryBlock {
            LogUtils.d("onConnected ")
            //请求一次定位
            requestGoogleLocation()
        }
    }

    /**
     * 请求一次定位
     */
    @SuppressLint("MissingPermission")
    private fun requestGoogleLocation() {
        AppUtils.tryBlock {
            mGoogleLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    mLastLocation = location
                    googleLocationChange()
                    //取消定位
                    if (mGoogleLocationListener != null && mGoogleApiClient != null) {
                        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient!!, mGoogleLocationListener!!)
                        mGoogleLocationListener = null
                    }
                }
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient!!,
                LocationRequest.create().apply {
                    interval = (60 * 60 * 1000).toLong() //小时 //间隔
                    fastestInterval = 0L
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY //定位模式 - 同时兼顾了位置精确度和电量消耗。很少用GPS，一般用WiFi和小区广播计算位置信息
                    smallestDisplacement = 5f //定位精度
                }, mGoogleLocationListener!!
            )
        }
    }

    fun googleLocationChange() {
        LogUtils.d("Google定位 - 位置 = $mLastLocation") //22.631818,113.833136
        binding.ivNoMap.visibility = View.GONE
        if (mLastLocation != null) {
            //中国内定位点修正赋值
            if (!GpsCoordinateUtils.isOutOfChina(mLastLocation!!.latitude, mLastLocation!!.longitude)) {
                val newLatlng = GpsCoordinateUtils.calWGS84toGCJ02(mLastLocation!!.latitude, mLastLocation!!.longitude)
                LogUtils.d("Google定位 - 中国定位点 wgs84ToGcj02 位置 = ${newLatlng[0]},${newLatlng[1]}") //22.628857,113.838142
                mLastLocation!!.latitude = newLatlng[0]
                mLastLocation!!.longitude = newLatlng[1]
            }
            mGOldLatLng = com.google.android.gms.maps.model.LatLng(
                mLastLocation!!.latitude,
                mLastLocation!!.longitude
            )
            //SP 存定位点
            if (mGOldLatLng != null) {
                if (SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").isEmpty()) {
                    SpUtils.setValue(
                        SpUtils.WEATHER_LONGITUDE_LATITUDE,
                        "${mGOldLatLng!!.longitude},${mGOldLatLng!!.latitude}"
                    )
                }
                SpUtils.setValue(SpUtils.LOCATION_CACHE, "${mGOldLatLng!!.longitude},${mGOldLatLng!!.latitude}")

                // 刷新显示系统小蓝点
                mGoogleListener?.onLocationChanged(mLastLocation!!)
                //移动地图
                ThreadUtils.runOnUiThreadDelayed({
                    mGoogleMap?.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(mGOldLatLng!!, 16f))
                }, if (isFirstLoadMap) 3000 else 0)
                isFirstLoadMap = false
            }
        }

    }

    override fun onConnectionSuspended(p: Int) {
        LogUtils.d("onConnectionSuspended")
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        LogUtils.d("onConnectionFailed ")
    }

    override fun onCameraMove() {
        //LogUtils.d("onCameraMove---->")
    }

    override fun activate(listener: com.google.android.gms.maps.LocationSource.OnLocationChangedListener) {
        mGoogleListener = listener
    }
    //endregion

    /**
     * google 高德 共有回调
     */
    override fun deactivate() {
        LogUtils.d(if (!AppUtils.isEnableGoogleMap()) "AMap" else "GoogleMap" + "--->deactivate")
    }

    //region 生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
    }

    override fun onResume() {
        super.onResume()
        mapview?.onResume()
        AppUtils.tryBlock {
            //处理 java.lang.NullPointerException: Attempt to invoke interface method 'void com.google.maps.api.android.lib6.impl.bq.x()' on a null object reference
            //GOOGLE map 未初始化成功mGoogleMap == null时调用生命周期方法闪退
            if (mMapFragment != null && mGoogleMap != null) {
                mMapFragment?.onResume()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapview?.onSaveInstanceState(outState)
        AppUtils.tryBlock {
            if (mMapFragment != null && mGoogleMap != null) {
                mMapFragment?.onSaveInstanceState(outState)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapview?.onLowMemory()
        AppUtils.tryBlock {
            if (mMapFragment != null && mGoogleMap != null) {
                mMapFragment?.onLowMemory()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mapview?.onPause()
        AppUtils.tryBlock {
            if (mMapFragment != null && mGoogleMap != null) {
                mMapFragment?.onPause()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        AppUtils.tryBlock {
            mapview?.onDestroy()
            if (mMyALocationListener != null) {
                mLocationClient?.unRegisterLocationListener(mMyALocationListener)
            }
            mLocationClient?.stopLocation()
            mLocationClient?.onDestroy()
            mLocationClient = null
            /*java.lang.RuntimeException: Unable to destroy activity {com.jwei.publicone/com.jwei.publicone.ui.HomeActivity}: java.lang.NullPointerException: Attempt to invoke interface method 'boolean com.google.maps.api.android.lib6.impl.bq.W()' on a null object reference
            if (mMapFragment != null && mGoogleMap != null) {
                mMapFragment?.onDestroy()
            }*/
            mMyALocationListener = null
            if (mGoogleApiClient != null) {
                mGoogleApiClient!!.disconnect()
                mGoogleApiClient = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }
    //endregion

    override fun initData() {
        super.initData()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    mSportType = tab.position
                    viewmodel.sportLiveData.getAppSportType().postValue(mSportType)
//                    viewmodel.getOdometer(mSportType)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        viewmodel.sportLiveData.getAppSportType().postValue(mSportType)
    }

    override fun onVisible() {
        super.onVisible()
        if (SpUtils.getSPUtilsInstance()
                .getBoolean(SpUtils.FIRST_REQUEST_LOCATION_PERMISSION, true)
        ) {
            SpUtils.getSPUtilsInstance().put(SpUtils.FIRST_REQUEST_LOCATION_PERMISSION, false)
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                showPermissionExplainDialog()
            }
        }

        if (!isInitMap && PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            if (!AppUtils.isEnableGoogleMap()) {
                initAMap()
            } else {
                initGoogleMap()
            }
            isFirstLoadMap = true
            isInitMap = true
        }

        if (isClickStart) {
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                return
            }
            if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                return
            }
            /*if (LocationService.binder.service.isLocationDoing) {
                return
            }*/
            isClickStart = false
            binding.ivStart.callOnClick()
        }
        if (isClickHome) {
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                return
            }
            if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                return
            }
            isClickHome = false
            binding.ivHoming.callOnClick()
        }
    }

    //region 其它

    /**
     * Google play 要求
     * 定位权限说明
     * */
    fun showPermissionExplainDialog() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*BaseApplication.mContext.getString(R.string.apply_permission)*/null,
            getString(R.string.open_location_per_explain),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    LogUtils.d(com.jwei.xzfit.utils.PermissionUtils.TAG, "申请权限")
                    requestPermissions()
                }

                override fun OnCancel() {}
            }
        ).show()
    }

    /**
     * 检查权限
     * */
    fun requestPermissions() {

        com.jwei.xzfit.utils.PermissionUtils.requestPermissions(
            this.lifecycle,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getString(R.string.permission_location_12) else getString(R.string.permission_location),
            com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION
        ) {
            checkQLocationPermission()
        }
    }

    /**
     * android 10 后台定位权限申请
     * */
    fun checkQLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_10_LOCATION)) {
                DialogUtils.dialogShowContentAndTwoBtn(requireActivity(), getString(R.string.background_location_tips),
                    getString(R.string.dialog_cancel_btn), getString(R.string.dialog_confirm_btn), object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            PermissionUtils.permission(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_10_LOCATION)
                                .callback(object : PermissionUtils.FullCallback {
                                    override fun onGranted(granted: MutableList<String>) {

                                    }

                                    override fun onDenied(deniedForever: MutableList<String>, denied: MutableList<String>) {
                                        if (deniedForever.size > 0) {
                                            PermissionUtils.launchAppDetailsSettings()
                                        }
                                    }

                                }).request()
                        }

                        override fun OnCancel() {}
                    })
                return false
            }
        }
        return true
    }

    /**
     * 辅助运动中提示
     * */
    fun showDeviceSportingDialog() {
        DialogUtils.showDialogTitleAndOneButton(ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.device_sporting_tips),
            getString(R.string.know),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                }

                override fun OnCancel() {
                }
            })
    }

    /**
     * 辅助运动无权限
     */
    fun showDeviceSportNoPermissionHint() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.device_sport_no_per_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.running_permission_set),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    LogUtils.d(com.jwei.xzfit.utils.PermissionUtils.TAG, "申请权限")
                    requestPermissions()
                }

                override fun OnCancel() {}
            }
        ).show()
    }

    /**
     * 辅助运动未开GPS
     */
    fun showDeviceSportNoGpsHint() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.device_sport_no_gps_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    AppUtils.openGpsActivity()
                }

                override fun OnCancel() {}
            }
        ).show()
    }

    /**
     * 缺少Google服务
     * */
    private fun showNoGoogleServiceDialog() {
        DialogUtils.showDialogTitleAndOneButton(ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.phone_no_google_service),
            getString(R.string.know),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                }

                override fun OnCancel() {
                }
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun refMap(event: EventMessage) {
        if (event.action == EventAction.ACTION_MAP_CHANGE) {
            //切换地图
            if (!AppUtils.isEnableGoogleMap()) {
                binding.aMap.visibility = View.VISIBLE
                binding.ivNoMap.visibility = View.VISIBLE
                binding.frGoogleMap.visibility = View.GONE
            } else {
                binding.aMap.visibility = View.GONE
                binding.ivNoMap.visibility = View.VISIBLE
                if (AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                    binding.frGoogleMap.visibility = View.VISIBLE
                }
            }
            //定位服务在定位中， 重启定位
            if (LocationService.binder?.service?.isLocationDoing!!) {
                LocationService.binder?.service?.stopLocation()
                LocationService.binder?.service?.startLocation()
            }
            binding.ivHoming.callOnClick()
        }
        //设备辅助运动无权限
        if (event.action == EventAction.ACTION_DEV_SPORT_NO_PERMISSION) {
            showDeviceSportNoPermissionHint()
        }
        //设备辅助运动未开GPS
        if (event.action == EventAction.ACTION_DEV_SPORT_NO_GPS) {
            showDeviceSportNoGpsHint()
        }
    }

    override fun onClick(v: View?) {
        if (AppUtils.isEnableGoogleMap()) {
            if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                showNoGoogleServiceDialog()
                return
            }
        }
        v?.let {
            when (it.id) {
                binding.ivStart.id -> {
                    isClickHome = false
                    isClickStart = true
                    if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                        showPermissionExplainDialog()
                        return
                    }
                    if (!checkQLocationPermission()) {
                        isClickStart = false
                        return
                    }
                    if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                        AppUtils.showGpsOpenDialog()
                        return
                    }
                    /*if (LocationService.binder.service.isLocationDoing) {
                        isClickStart = false
                        showDeviceSportingDialog()
                        return
                    }*/
                    isClickStart = false
                    // 0 -户外运动  1 -户外骑行  2-户外健走
                    when (mSportType) {
                        0 -> AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("9", "20").apply {
                            functionStatus = "1"
                        })
                        1 -> AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("9", "21").apply {
                            functionStatus = "1"
                        })
                        2 -> AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("9", "22").apply {
                            functionStatus = "1"
                        })
                    }
                    startActivity(Intent(activity, SportCountDownActivity::class.java))
                }
                binding.ivHoming.id -> {
                    isClickStart = false
                    isClickHome = true
                    if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                        showPermissionExplainDialog()
                        return
                    }
                    if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                        AppUtils.showGpsOpenDialog()
                        return
                    }
                    isClickHome = false
                    if (!AppUtils.isEnableGoogleMap()) {
                        //刷新高德
                        if (mLocationClient != null) {
                            mLocationClient?.startLocation()
                        } else {
                            initAMap()
                        }
                    } else {
                        //刷新google
                        AppUtils.tryBlock {
                            if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected) {
                                requestGoogleLocation()
                            } else {
                                initGoogleMap()
                            }
                        }
                    }
                }
                else -> {
                }
            }
        }
    }
//endregion


}