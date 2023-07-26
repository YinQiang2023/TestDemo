package com.jwei.xzfit.ui.sport

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.view.View.OnTouchListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.commitNow
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amap.api.location.AMapLocation
import com.amap.api.maps.*
import com.amap.api.maps.LocationSource.OnLocationChangedListener
import com.amap.api.maps.model.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.utils.UnitConversionUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.databinding.ActivityAmpSportBinding
import com.jwei.xzfit.databinding.ItemSportMapSingleDataBinding
import com.jwei.xzfit.db.model.sport.ExerciseApp
import com.jwei.xzfit.db.model.sport.SportModleInfo
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.service.LocationService
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.ui.sport.bean.SportSingleDataBean
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.utils.manager.DevSportManager
import com.jwei.xzfit.viewmodel.SportModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt


/**
 * Created by Android on 2021/9/29.
 * APP运动
 */
@SuppressLint("ClickableViewAccessibility")
class MapSportActivity : BaseActivity<ActivityAmpSportBinding, SportModel>(
    ActivityAmpSportBinding::inflate, SportModel::class.java
), View.OnClickListener, LocationSource, OnMapReadyCallback,
    GoogleMap.OnCameraMoveListener, com.google.android.gms.maps.LocationSource {

    //region 高德地图
    private var mapview: MapView? = null
    private lateinit var mAMap: AMap
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<View>

    //导航
    private var mListener: OnLocationChangedListener? = null
    private lateinit var mPolyLine: PolylineOptions

    //是否第一次定位
    private var mIsFirstLocation = true

    //是否提示了gps信号弱
    private var mWeakGpsTipsTime = 0L

    //以前的定位点
    private lateinit var mOldLatLng: LatLng
    private var mLatLngList: MutableList<LatLng> = mutableListOf()
    //endregion

    //定位坐标的时间集合
    private var mLatLngListTime: MutableList<Long> = mutableListOf()

    //region Google地图
    private lateinit var mGoogleMap: GoogleMap
    private var mMapFragment: SupportMapFragment? = null
    private var mGoogleListener: com.google.android.gms.maps.LocationSource.OnLocationChangedListener? =
        null
    private var mLastLocation: Location? = null
    private var mGOldLatLng: com.google.android.gms.maps.model.LatLng? = null
    private var mGLatLngList: MutableList<com.google.android.gms.maps.model.LatLng> =
        mutableListOf()
    private lateinit var mPolylineOptions: com.google.android.gms.maps.model.PolylineOptions
    //endregion

    //上次定位时间
    private var mOldLongTime: Long = 0

    //开始运动时间
    private var mStartSportTime = 0L

    //界面单个数据items
    private var mDatas = mutableListOf<SportSingleDataBean>()

    //singleData title
    private lateinit var mTitleBean: SportSingleDataBean

    private val REQUEST_CODE = 1000

    override fun initView() {
        super.initView()
        if (!AppUtils.isEnableGoogleMap()) {
            initMap()
        } else {
            binding.aMap.visibility = View.GONE
            initGoogleMap()
        }

        //TODO 如果设备连接上时,展示有设备的详情数据
        if (ControlBleTools.getInstance().isConnect) {

        }
        binding.tvKm2Unit.text =
            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(R.string.unit_distance_1)
        binding.sbUnLock.max = Global.PRESS_SEEKBAR_MAX
        binding.sbUnLock.progress = 0
        binding.sbUnLock.setOnTouchListener { v, event -> return@setOnTouchListener true }
        binding.sbStop.max = Global.PRESS_SEEKBAR_MAX
        binding.sbStop.progress = 0
        binding.sbStop.setOnTouchListener { v, event -> return@setOnTouchListener true }
        val unLockListener = UnLockListener()
        binding.btnUnLock.setOnClickListener(unLockListener)
        binding.btnUnLock.setOnTouchListener(unLockListener)
        binding.btnStop.setOnClickListener(unLockListener)
        binding.btnStop.setOnTouchListener(unLockListener)
        binding.btnLock.setOnClickListener(this)
        binding.btnPause.setOnClickListener(this)
        binding.btnRestart.setOnClickListener(this)
        binding.ivDown.setOnClickListener(this)
        binding.ivUp.setOnClickListener(this)
        binding.ivHoming.setOnClickListener(this)

        binding.tvValue.tag = Global.SPORT_SINGLE_DATA_TYPE_DISTANCE
        setViewsClickListener(this, binding.tvValue)

        mBottomSheetBehavior = BottomSheetBehavior.from(binding.clBottomSheet)
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        mBottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        binding.clSimple.visibility = View.INVISIBLE

                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        binding.clDetail.visibility = View.INVISIBLE
                    }
                    else -> {
                        binding.clDetail.visibility = View.VISIBLE
                        binding.clSimple.visibility = View.VISIBLE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.clSimple.alpha = 1 - slideOffset
                binding.clDetail.alpha = slideOffset
            }

        })

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MapSportActivity, 2)
            setHasFixedSize(true)
            adapter = initAdapter()
            //上边距
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State,
                ) {
                    outRect.top = ConvertUtils.dp2px(20F)
                    outRect.bottom = ConvertUtils.dp2px(30F)
                }
            })
        }
    }

    //region 单个数据adapter
    private fun initAdapter(): CommonAdapter<SportSingleDataBean, ItemSportMapSingleDataBinding> {
        return object : CommonAdapter<SportSingleDataBean, ItemSportMapSingleDataBinding>(mDatas) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int,
            ): ItemSportMapSingleDataBinding {
                return ItemSportMapSingleDataBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemSportMapSingleDataBinding,
                t: SportSingleDataBean,
                position: Int,
            ) {
                v.tvTitle.text = t.describe
                v.tvValue.text = t.value
                // 不需要数据单个展示页面
                v.root.setOnClickListener {
                    if (!(viewModel.isLock.value as Boolean)) {
                        startActivityForResult(
                            Intent(this@MapSportActivity, SportSingleDataActivity::class.java)
                                .putExtra("data", t), REQUEST_CODE
                        )
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val data = data?.getParcelableExtra<SportSingleDataBean>("data")
            if (data != null) {
                if (data.index != -1) {
                    mDatas.removeAt(data.index)
                    mDatas.add(data.index, data)
                    //binding.recyclerView.adapter?.notifyDataSetChanged()
                } else {
                    mTitleBean = data
                }
            }
        }
    }
    //endregion

    //region 高德地图
    /**
     * 地图初始化
     * */
    private fun initMap() {
        mapview = binding.aMap
        mAMap = binding.aMap.map
        val mUiSettings = mAMap.uiSettings
        mUiSettings.isRotateGesturesEnabled = true //是否允许旋转
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(17f)) //将地图的缩放级别调整到17级
        if (!AppUtils.isZh(this)) {
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
        //初始化绘制线
        initPolyline()
    }

    /**
     * 轨迹初始化
     * */
    private fun initPolyline() {
        mPolyLine = PolylineOptions()
        mPolyLine.width(10f)
        mPolyLine.color(ContextCompat.getColor(this, R.color.sport_locus_color))
    }

    /**
     * 开始定位
     * */
    fun startLocation() {
        LocationService.binder?.service?.startLocation()
    }

    override fun activate(listener: OnLocationChangedListener?) {
        mListener = listener
    }

    override fun activate(listener: com.google.android.gms.maps.LocationSource.OnLocationChangedListener) {
        mGoogleListener = listener
    }

    override fun deactivate() {
        LogUtils.d(if (!AppUtils.isEnableGoogleMap()) "AMap" else "GoogleMap" + "--->deactivate")
    }

    //region 设置轨迹
    /**
     * 设置轨迹
     * */
    private fun setPolyline(oldData: LatLng, newData: LatLng) {
        if (::mPolyLine.isInitialized) {
            if (mPolyLine.points.isNotEmpty()) {
                mPolyLine.points.clear()
            }
            mAMap.addPolyline(mPolyLine.add(oldData, newData))
        }
    }
    //endregion

    //endregion

    //region Google地图
    //region 地图配置
    private fun initGoogleMap() {
        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            return
        }
        mMapFragment = SupportMapFragment.newInstance()
        //mMapFragment = supportFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment?
        supportFragmentManager.commitNow(true) {
            add(R.id.google_map, mMapFragment!!)
        }
        //轨迹样式
        mPolylineOptions = com.google.android.gms.maps.model.PolylineOptions()
        mPolylineOptions.width(10f).color(ContextCompat.getColor(this, R.color.sport_locus_color))
        mMapFragment?.getMapAsync(this)
        //开始定位
        LocationService.binder?.service?.startLocation()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: GoogleMap) {
        AppUtils.tryBlock {
            LogUtils.d("onMapReady")
            mGoogleMap = map
            mGoogleMap.setOnCameraMoveListener(this)
            mGoogleMap.setLocationSource(this)
            mGoogleMap.isMyLocationEnabled = true
            mGoogleMap.uiSettings.isMapToolbarEnabled = false
            mGoogleMap.uiSettings.isMyLocationButtonEnabled = false
        }
    }

    override fun onCameraMove() {
        //LogUtils.d("onCameraMove")
    }
    //endregion

    //region 设置轨迹
    /**
     * 设置轨迹
     * */
    private fun setPolyline(
        oldData: com.google.android.gms.maps.model.LatLng,
        newData: com.google.android.gms.maps.model.LatLng,
    ) {
        AppUtils.tryBlock {
            if (!mPolylineOptions.getPoints().isEmpty()) {
                mPolylineOptions.getPoints().clear()
            }
            mGoogleMap.addPolyline(mPolylineOptions.add(oldData, newData))
        }
    }
    //endregion

    /**
     * 计算两点之间距离
     *
     * @param start
     * @param end
     * @return 米
     */
    fun getGoogleDistance(
        start: com.google.android.gms.maps.model.LatLng,
        end: com.google.android.gms.maps.model.LatLng,
    ): Double {
        val lat1 = Math.PI / 180 * start.latitude
        val lat2 = Math.PI / 180 * end.latitude
        val lon1 = Math.PI / 180 * start.longitude
        val lon2 = Math.PI / 180 * end.longitude
        //地球半径
        val R = 6371.0
        //两点间距离 km，如果想要米的话，结果*1000就可以了
        val d = Math.acos(
            Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)
        ) * R
        return d * 1000
    }
    //endregion

    //region 定位回调

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onLocationChange(event: EventMessage?) {
        if (event == null) return
        if (event.action == EventAction.ACTION_LOCATION) {
            if (!AppUtils.isEnableGoogleMap()) {
                //region 高德定位变化
                val location = event.obj as AMapLocation?
                if (location != null) {
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    if (mIsFirstLocation) {
                        mIsFirstLocation = false
                        mListener?.onLocationChanged(location)
                        //进入就算开始运动
                        /*//初次获取定位后 开始计时
                        viewModel.sportStart()
                        mStartSportTime = System.currentTimeMillis()*/
                    }

                    val nowTime = System.currentTimeMillis()
                    //有上次定位 & 非暂停
                    if (::mOldLatLng.isInitialized && !(viewModel.isPause.value as Boolean)) {
                        val xSeconds = (nowTime - mOldLongTime) / 1000
                        val newDistance = AMapUtils.calculateLineDistance(mOldLatLng, newLatLng)
                        if (newDistance < Global.GPS_OFFSET_DISTANCE_MIN ||
                            newDistance / xSeconds > Global.GPS_OFFSET_DISTANCE_MAX
                        )
                            return
                        if (mOldLatLng != newLatLng) {
                            // 刷新显示系统小蓝点
                            mListener!!.onLocationChanged(location)
                            // 画轨迹
                            setPolyline(mOldLatLng, newLatLng)
                            // 计算相关值
                            //val distance = AMapUtils.calculateLineDistance(mOldLatLng, newLatLng)
                            viewModel.calculateSportData(newDistance)
                        }
                    }
                    mLatLngList.add(newLatLng)
                    mLatLngListTime.add(System.currentTimeMillis())
                    mOldLatLng = newLatLng
                    mOldLongTime = nowTime
                }
                //endregion
            } else {
                //region google定位变化
                AppUtils.tryBlock {
                    val location = event.obj as Location?
                    if (location != null) {
                        if (mIsFirstLocation) {
                            mIsFirstLocation = false
                            mGoogleMap.clear()
                            //进入就算开始运动
                            /*//初次获取定位后 开始计时
                            viewModel.sportStart()
                            mStartSportTime = System.currentTimeMillis()*/

                            mGoogleMap.moveCamera(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                    com.google.android.gms.maps.model.LatLng(
                                        location.latitude,
                                        location.longitude
                                    ), 15f
                                )
                            )
                        }

                        val newLatLng = com.google.android.gms.maps.model.LatLng(
                            location.latitude,
                            location.longitude
                        )
                        if (mGOldLatLng != newLatLng && !(viewModel.isPause.value as Boolean)) {
                            val newLongTime = System.currentTimeMillis()
                            mGLatLngList.add(newLatLng)
                            mLatLngListTime.add(System.currentTimeMillis())
                            if (mGOldLatLng != null) {
                                // 画轨迹
                                setPolyline(mGOldLatLng!!, newLatLng)
                                // 计算相关值
                                val distance = getGoogleDistance(mGOldLatLng!!, newLatLng)
                                LogUtils.d(
                                    "GPS运动 移动距离----》${
                                        DecimalFormat(
                                            "#.00",
                                            DecimalFormatSymbols(Locale.ENGLISH)
                                        ).format(distance).toFloat()
                                    }"
                                )
                                viewModel.calculateSportData(
                                    DecimalFormat(
                                        "#.00",
                                        DecimalFormatSymbols(Locale.ENGLISH)
                                    ).format(distance).toFloat()
                                )
                            }
                            mOldLongTime = newLongTime
                        }
                        mGOldLatLng = newLatLng
                        mLastLocation = location
                        // 刷新显示系统小蓝点
                        mGoogleListener?.onLocationChanged(mLastLocation!!)
                    }
                }
                //endregion
            }
        }
        //由NewSportFragment 计算
        /*else if (event.action == EventAction.ACTION_GPS_SATELLITE_CHANGE) {
            //设置gps强度
            if (event.obj != null && event.obj is SatelliteBean) {
                val satellite = event.obj as SatelliteBean
                val gpsssl = viewModel.calculateGPS(satellite.max, satellite.valid)
                //LogUtils.d("更新GPS强度 --> $gpsssl")
                if (gpsssl < 2 && Math.abs(System.currentTimeMillis() - mWeakGpsTipsTime) > 2 * 60 * 1000) {
                    mWeakGpsTipsTime = System.currentTimeMillis()
                    ToastUtils.showToast(R.string.sport_no_gps)
                }
                viewModel.setGPSRssl(gpsssl)
            }
        }*/
    }
    //endregion


    //region 生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.registerEventBus(this)
        binding.aMap.onCreate(savedInstanceState)
        LocationService.binder?.service?.isAppSport = true
        //开始运动
        viewModel.sportStart()
        mStartSportTime = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        mapview?.onResume()
        AppUtils.tryBlock {
            mMapFragment?.onResume()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapview?.onSaveInstanceState(outState)
        AppUtils.tryBlock {
            mMapFragment?.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapview?.onLowMemory()
        AppUtils.tryBlock {
            mMapFragment?.onLowMemory()
        }
    }

    override fun onPause() {
        super.onPause()
        mapview?.onPause()
        AppUtils.tryBlock {
            mMapFragment?.onPause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
        mListener = null
        mapview?.onDestroy()
        AppUtils.tryBlock {
            mMapFragment?.onDestroy()
        }
        LocationService.binder?.service?.isAppSport = false
        if (!DevSportManager.isDeviceSporting) {
            LocationService.binder?.service?.stopLocation()
        }
    }
    //endregion

    override fun initData() {
        super.initData()
        //信号强度
        viewModel.GPSRssl.observe(this) {
            when (it) {
                0 -> {
                    binding.ivRssl1.setImageResource(R.mipmap.sport_gps_rssl_dim_1)
                    binding.ivRssl2.setImageResource(R.mipmap.sport_gps_rssl_dim_2)
                    binding.ivRssl3.setImageResource(R.mipmap.sport_gps_rssl_dim_3)
                }
                1 -> {
                    binding.ivRssl1.setImageResource(R.mipmap.sport_gps_rssl_bright_1)
                    binding.ivRssl2.setImageResource(R.mipmap.sport_gps_rssl_dim_2)
                    binding.ivRssl3.setImageResource(R.mipmap.sport_gps_rssl_dim_3)
                }
                2 -> {
                    binding.ivRssl1.setImageResource(R.mipmap.sport_gps_rssl_bright_1)
                    binding.ivRssl2.setImageResource(R.mipmap.sport_gps_rssl_bright_2)
                    binding.ivRssl3.setImageResource(R.mipmap.sport_gps_rssl_dim_3)
                }
                3 -> {
                    binding.ivRssl1.setImageResource(R.mipmap.sport_gps_rssl_bright_1)
                    binding.ivRssl2.setImageResource(R.mipmap.sport_gps_rssl_bright_2)
                    binding.ivRssl3.setImageResource(R.mipmap.sport_gps_rssl_bright_3)
                }
            }
            if (it < 2 && Math.abs(System.currentTimeMillis() - mWeakGpsTipsTime) > 2 * 60 * 1000) {
                mWeakGpsTipsTime = System.currentTimeMillis()
                ToastUtils.showToast(R.string.sport_no_gps)
            }
        }
        //是否运动上锁
        viewModel.isLock.observe(this) {
            binding.btnLock.visibility = if (it) View.INVISIBLE else View.VISIBLE
            binding.btnPause.visibility = if (it) View.INVISIBLE else View.VISIBLE
            binding.rlUnLock.visibility = if (it) View.VISIBLE else View.INVISIBLE
            binding.tvUnlockTips.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        //是否暂停
        viewModel.isPause.observe(this) {
            binding.btnLock.postDelayed({
                binding.btnLock.visibility = if (it) View.INVISIBLE else View.VISIBLE
                binding.btnPause.visibility = if (it) View.INVISIBLE else View.VISIBLE
            }, 500L)
            val t = Slide(Gravity.BOTTOM)
            t.duration = 600L
            TransitionManager.beginDelayedTransition(binding.clBtn, t)
            binding.rlStop.visibility = if (it) View.VISIBLE else View.INVISIBLE
            binding.tvStopTips.visibility = if (it) View.VISIBLE else View.INVISIBLE
            binding.btnRestart.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }
        //解锁进度/结束进度
        viewModel.seekBARdown.observe(this) {
            binding.sbUnLock.visibility = if (it > 0) View.VISIBLE else View.INVISIBLE
            binding.sbStop.visibility = if (it > 0) View.VISIBLE else View.INVISIBLE
            if (viewModel.isLock.value as Boolean) {
                binding.sbUnLock.progress = it
            }
            if (viewModel.isPause.value as Boolean) {
                binding.sbStop.progress = it
                if (binding.sbStop.progress == binding.sbStop.max) {
                    //运动结束。。。。
                    //ToastUtils.showToast("结束运动")
                    stopSport()
                }
            }
        }


        mTitleBean = SportSingleDataBean(
            -1,
            Global.SPORT_SINGLE_DATA_TYPE_DISTANCE,
            getString(R.string.healthy_sports_list_distance),
            unit = if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                R.string.unit_distance_1
            )
        )
        mDatas.apply {
            add(
                SportSingleDataBean(
                    0,
                    Global.SPORT_SINGLE_DATA_TYPE_TIME,
                    getString(R.string.sport_time),
                    unit = getString(R.string.unit_secs)
                )
            )
            add(
                SportSingleDataBean(
                    1,
                    Global.SPORT_SINGLE_DATA_TYPE_CALORIES,
                    StringBuilder().append(getString(R.string.healthy_sports_list_calories))
                        .append("(")
                        .append(getString(R.string.unit_calories))
                        .append(")")
                        .toString(),
                    unit = getString(R.string.unit_calories)
                )
            )
            add(
                SportSingleDataBean(
                    2,
                    Global.SPORT_SINGLE_DATA_TYPE_SPEED,
                    StringBuilder().append(getString(R.string.sport_data_type_speed))
                        .append("(")
                        .append(
                            StringBuilder().append(
                                if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                    R.string.unit_distance_1
                                )
                            )
                                .append("/")
                                .append(getString(R.string.h))
                                .toString()
                        )
                        .append(")")
                        .toString(),
                    unit = StringBuilder().append(
                        if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                            R.string.unit_distance_1
                        )
                    )
                        .append("/")
                        .append(getString(R.string.h))
                        .toString()
                )
            )
            add(
                SportSingleDataBean(
                    3,
                    Global.SPORT_SINGLE_DATA_TYPE_MINKM,
                    getString(R.string.sport_minkm),
                    unit = StringBuilder().append(getString(R.string.h))
                        .append("/")
                        .append(
                            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                R.string.unit_distance_1
                            )
                        )
                        .toString()
                )
            )
        }


        //运动时间
        viewModel.sportLiveData.getSportTime().observe(this) {
            //binding.tvSportTime.text = TimeUtils.millis2String(it)
            if (mTitleBean.dataType == Global.SPORT_SINGLE_DATA_TYPE_TIME) {
                mTitleBean.apply {
                    unit = ""
                    value = TimeUtils.millis2String(it)
                    binding.tvValue.text = value
                    binding.tvUnit.text = unit
                }
            }
            mDatas.filter { it.dataType == Global.SPORT_SINGLE_DATA_TYPE_TIME }.forEach { item ->
                item.unit = ""
                item.value = TimeUtils.millis2String(it)
                binding.recyclerView.adapter?.notifyDataSetChanged()
            }
            binding.tvSportTime2.text = TimeUtils.millis2String(it)
        }
        //运动距离
        viewModel.sportLiveData.getSportDistance().observe(this) {
            //binding.tvValue.text = "${UnitConversionUtils.bigDecimalFormat(it / 1000f)}"
            if (mTitleBean.dataType == Global.SPORT_SINGLE_DATA_TYPE_DISTANCE) {
                mTitleBean.apply {
                    unit = mTitleBean.unit
                    value = if (AppUtils.getDeviceUnit() == 0)
                        UnitConversionUtils.bigDecimalFormat(it / 1000f)
                    else
                        UnitConversionUtils.bigDecimalFormat(it / 1000f / 1.61f)
                    binding.tvValue.text = value
                    binding.tvUnit.text = unit
                }
            }
            mDatas.filter { it.dataType == Global.SPORT_SINGLE_DATA_TYPE_DISTANCE }
                .forEach { item ->
                    item.unit = item.unit
                    item.value = if (AppUtils.getDeviceUnit() == 0)
                        UnitConversionUtils.bigDecimalFormat(it / 1000f)
                    else
                        UnitConversionUtils.bigDecimalFormat(it / 1000f / 1.61f)
                    binding.recyclerView.adapter?.notifyItemChanged(item.index)
                }
            binding.tvKm2.text = if (AppUtils.getDeviceUnit() == 0)
                UnitConversionUtils.bigDecimalFormat(it / 1000f)
            else
                UnitConversionUtils.bigDecimalFormat(it / 1000f / 1.61f)
        }
        //速度
        viewModel.sportLiveData.getSportSpeed().observe(this) {
            //binding.tvSpeed.text = "${UnitConversionUtils.bigDecimalFormat(it)}"
            if (mTitleBean.dataType == Global.SPORT_SINGLE_DATA_TYPE_SPEED) {
                mTitleBean.apply {
                    unit = mTitleBean.unit
                    value = UnitConversionUtils.bigDecimalFormat(it)
                    binding.tvValue.text = value
                    binding.tvUnit.text = unit
                }
            }
            mDatas.filter { it.dataType == Global.SPORT_SINGLE_DATA_TYPE_SPEED }.forEach { item ->
                item.unit = item.unit
                item.value = UnitConversionUtils.bigDecimalFormat(it)
                binding.recyclerView.adapter?.notifyItemChanged(item.index)
            }
        }
        //配速
        viewModel.sportLiveData.getSportMinkm().observe(this) {
            //binding.tvMinkm.text = it
            if (mTitleBean.dataType == Global.SPORT_SINGLE_DATA_TYPE_MINKM) {
                mTitleBean.apply {
                    unit = ""
                    value = it
                    binding.tvValue.text = value
                    binding.tvUnit.text = unit
                }
            }
            mDatas.filter { it.dataType == Global.SPORT_SINGLE_DATA_TYPE_MINKM }.forEach { item ->
                item.unit = ""
                item.value = it
                binding.recyclerView.adapter?.notifyItemChanged(item.index)
            }
        }
        //卡路里
        viewModel.sportLiveData.getCalories().observe(this) {
            //binding.tvCalories.text = "${UnitConversionUtils.bigDecimalFormat(it)}"
            //binding.tvSpeed.text = "${UnitConversionUtils.bigDecimalFormat(it)}"
            if (mTitleBean.dataType == Global.SPORT_SINGLE_DATA_TYPE_CALORIES) {
                mTitleBean.apply {
                    unit = mTitleBean.unit
                    value = it.roundToInt().toString()
                    binding.tvValue.text = value
                    binding.tvUnit.text = unit
                }
            }
            mDatas.filter { it.dataType == Global.SPORT_SINGLE_DATA_TYPE_CALORIES }
                .forEach { item ->
                    item.unit = item.unit
                    item.value = it.roundToInt().toString()
                    binding.recyclerView.adapter?.notifyItemChanged(item.index)
                }
        }
    }

    //region 结束运动
    /**
     * 结束运动
     * */
    private fun stopSport() {
        //停止运动
        viewModel.sportSport()

        var sportCalory = viewModel.sportLiveData.getCalories().value
        //region 地图数据
        val buffer = StringBuffer()

        var fullSpeedDataBuilder = StringBuilder()

        if (!AppUtils.isEnableGoogleMap()) {
            if (mLatLngList.size >= 2) {
                for (i in mLatLngList.indices) {

                    if (i < mLatLngList.size - 1) {
                        //两点速度
                        val dis =
                            AMapUtils.calculateLineDistance(mLatLngList[i], mLatLngList[i + 1])
                        val time = mLatLngListTime[i + 1] - mLatLngListTime[i]
                        val speed = (dis / (time / 1000f) * 3.6f)
                        fullSpeedDataBuilder.append(UnitConversionUtils.bigDecimalFormat(speed) + ",")

                        //地图数据
                        buffer.append(
                            mLatLngList.get(i).longitude.toString() + "," + mLatLngList.get(
                                i
                            ).latitude + ";"
                        )
                    } else {
                        //去掉最后一个,
                        fullSpeedDataBuilder.deleteCharAt(fullSpeedDataBuilder.lastIndex)
                        buffer.append(
                            mLatLngList.get(i).longitude.toString() + "," + mLatLngList.get(
                                i
                            ).latitude
                        )
                    }
                    /*if (i < mLatLngList.size - 1) {
                        //是否国内
                        val isAMapDataAvailable = CoordinateConverter.isAMapDataAvailable(mLatLngList.get(i).latitude,mLatLngList.get(i).longitude)
                        if(isAMapDataAvailable){
                            val newLatLng = GpsCoordinateUtils.calGCJ02toWGS84(mLatLngList.get(i).latitude,mLatLngList.get(i).longitude)
                            buffer.append(newLatLng[1].toString() + "," + newLatLng[0].toString() + ";")
                        }else {
                            buffer.append(mLatLngList.get(i).longitude.toString() + "," + mLatLngList.get(i).latitude.toString() + ";")
                        }
                    } else {
                        //是否国内
                        val isAMapDataAvailable = CoordinateConverter.isAMapDataAvailable(mLatLngList.get(i).latitude,mLatLngList.get(i).longitude)
                        if(isAMapDataAvailable){
                            val newLatLng = GpsCoordinateUtils.calGCJ02toWGS84(mLatLngList.get(i).latitude,mLatLngList.get(i).longitude)
                            buffer.append(newLatLng[1].toString() + "," + newLatLng[0].toString())
                        }else {
                            buffer.append(mLatLngList.get(i).longitude.toString() + "," + mLatLngList.get(i).latitude)
                        }
                    }*/
                }
            }
        } else {
            if (mGLatLngList != null && mGLatLngList.size >= 2) {
                for (i in mGLatLngList.indices) {
                    if (i < mGLatLngList.size - 1) {
                        //两点速度
                        val dis =
                            getGoogleDistance(mGLatLngList[i], mGLatLngList[i + 1])
                        val time = mLatLngListTime[i + 1] - mLatLngListTime[i]
                        val speed = (dis / (time / 1000f) * 3.6f)
                        fullSpeedDataBuilder.append(UnitConversionUtils.bigDecimalFormat(speed.toFloat()) + ",")

                        buffer.append(
                            mGLatLngList.get(i).longitude.toString() + "," + mGLatLngList.get(
                                i
                            ).latitude + ";"
                        )
                    } else {
                        //去掉最后一个,
                        fullSpeedDataBuilder.deleteCharAt(fullSpeedDataBuilder.lastIndex)

                        buffer.append(
                            mGLatLngList.get(i).longitude.toString() + "," + mGLatLngList.get(
                                i
                            ).latitude
                        )
                    }
                }
            }
        }
        //endregion

        //如果运动时间小于3分钟 || 运动距离小于0.1公里时
        if (viewModel.sportLiveData.getSportTime().value!!.toLong() < 3 * 60 * 1000 ||
            viewModel.sportLiveData.getSportDistance().value!!.toLong() < 100L
        ) {
            shouInvalidDataHintDialog()
            return
        }

        val info = SportModleInfo()
        val userID = SpUtils.getValue(SpUtils.USER_ID, "").toLong()
        info.userId = userID
        info.sportTime = mStartSportTime / 1000
        info.sportEndTime = System.currentTimeMillis() / 1000
        info.exerciseType = viewModel.sportLiveData.getAppSportType().value.toString()
        info.sportDuration = (viewModel.sportLiveData.getSportTime().value!! / 1000).toInt()
        if (sportCalory == null || (sportCalory.roundToInt() < 1)) {
            sportCalory = 1f
        }
        info.burnCalories = sportCalory.roundToInt()
        info.dataSources = 0 //app运动
        info.exerciseApp = ExerciseApp()
        info.exerciseApp!!.sportsMileage =
            viewModel.sportLiveData.getSportDistance().value!!.toInt().toString()
        info.exerciseApp!!.maxPace = viewModel.maxPace
        info.exerciseApp!!.minPace = viewModel.minPace
        info.exerciseApp!!.avgPace = /*viewModel.sportLiveData.getSportMinkm().value!!*/
            (
                    (viewModel.sportLiveData.getSportTime().value!!.toLong() / 1000f) / (viewModel.sportLiveData.getSportDistance().value!! / 1000f)
                    ).toInt().toString()
        info.exerciseApp!!.maxSpeed = viewModel.maxSpeed
        info.exerciseApp!!.minSpeed = viewModel.minSpeed
        info.exerciseApp!!.avgSpeed =
            UnitConversionUtils.bigDecimalFormat(viewModel.sportLiveData.getSportSpeed().value!!)
        info.exerciseApp!!.mapData = buffer.toString()
        info.exerciseApp!!.paceDatas = viewModel.paceDataBuilder.toString()
        info.exerciseApp!!.speedDatas = viewModel.speedDataBuilder.toString()
        info.exerciseApp!!.fullSpeedDatas = fullSpeedDataBuilder.toString()
        //存yyyy-MM-dd HH:mm:ss 用户日期查数据库
        info.date = com.blankj.utilcode.util.TimeUtils.millis2String(mStartSportTime, TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COMM))
        //存数据库
        val isSubSuccess = info.exerciseApp?.save() //子表
        val isSuccess = info.save()
        LogUtils.w("储存运动数据成功? = $isSubSuccess $isSuccess")
        //本地产生数据时刷新首页运动记录
        SendCmdUtils.getSportData()
        // 上报数据
        viewModel.uploadExerciseData(mutableListOf(info))
        viewModel.sportLiveData.getSportModleInfo().value = info
        startActivity(Intent(this, SportDataActivity::class.java))
        viewModel.sportLiveData.resetTempData()
        finish()
    }
    //endregion

    //region 提示无效数据
    /**
     * 提示无效数据
     * */
    private fun shouInvalidDataHintDialog() {
        DialogUtils.showBaseDialog(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.sport_invalid_data),
            isShowBottomBtn = true,
            isShowCenterImg = false,
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    //确认 放弃数据
                    finish()
                    viewModel.sportLiveData.resetTempData()
                }

                override fun OnCancel() {
                    //取消 继续等待
                }
            })
    }
    //endregion

    //region 解锁/终止触摸事件
    inner class UnLockListener : View.OnClickListener, OnTouchListener {
        override fun onClick(v: View?) {  /*LogUtils.d("点击")*/
        }

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    //LogUtils.d("按下去")
                    viewModel.seekBarStart()
                }
                MotionEvent.ACTION_UP -> {
                    //LogUtils.d("抬起")
                    viewModel.seekBarStop()
                }
            }
            return false
        }
    }
    //endregion

    //region 屏蔽返回键
    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }
    //endregion

    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                binding.ivHoming.id -> {
                    if (::mOldLatLng.isInitialized) {
                        mAMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(mOldLatLng.latitude, mOldLatLng.longitude), 17f
                            ), 1000, null
                        )
                    }
                    if (mLastLocation != null) {
                        AppUtils.tryBlock {
                            mGoogleMap.animateCamera(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                    mGOldLatLng!!,
                                    15.0f
                                )
                            )
                        }
                    }
                }
                binding.btnLock.id -> {
                    if (!(viewModel.isLock.value as Boolean) &&
                        !(viewModel.isPause.value as Boolean)
                    ) {
                        viewModel.sportLock()
                    }
                }
                binding.btnPause.id -> {
                    if (!(viewModel.isPause.value as Boolean)) {
                        viewModel.sportPause()
                    }
                }
                binding.btnRestart.id -> {
                    if (viewModel.isPause.value as Boolean) {
                        viewModel.sportRestart()
                    }
                }
                binding.ivDown.id, binding.ivUp.id -> {
                    if (mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
                        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    else
                        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                binding.tvValue.id -> {
                    if (!(viewModel.isLock.value as Boolean)) {
                        startActivityForResult(
                            Intent(this, SportSingleDataActivity::class.java)
                                .putExtra("data", mTitleBean), REQUEST_CODE
                        )
                    }
                }
            }
        }
    }
}