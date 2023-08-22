package com.smartwear.publicwatch.ui.sport

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.*
import com.blankj.utilcode.util.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.zhapp.ble.parsing.SportParsing
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.databinding.FragmentSportLocusBinding
import com.smartwear.publicwatch.db.model.sport.SportModleInfo
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.sport.amap.PathSmoothTool
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.TimeUtils
import com.smartwear.publicwatch.utils.manager.DevSportManager
import com.smartwear.publicwatch.viewmodel.SportModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Created by Android on 2021/10/11.
 * 运动数据轨迹
 */
class SportLocusFragment : BaseFragment<FragmentSportLocusBinding, SportModel>(
    FragmentSportLocusBinding::inflate, SportModel::class.java
), View.OnClickListener {
    //data
    private var sportModleInfo: SportModleInfo? = null

    //region 地图模块
    //高德
    private var savedInstanceState: Bundle? = null
    private var mapview: TextureMapView? = null
    private lateinit var mAMap: AMap
    private var mPolyOptions: PolylineOptions? = null
    private val mLatLngList: MutableList<LatLng> = mutableListOf()

    //Google
    private lateinit var mGoogleMap: GoogleMap
    private var mGMapView: MapView? = null
    private var mPolyline: Polyline? = null
    private var mPolylineOptions: com.google.android.gms.maps.model.PolylineOptions? = null
    private val mGLatLngList: MutableList<com.google.android.gms.maps.model.LatLng> =
        mutableListOf()

    //当前是否卫星地图
    private var isSatellite = false

    //当前是否隐藏地图
    private var isNoShowMap = false

    //是否有地图数据
    private var isNoMapData = false
    //endregion

    override fun initView() {
        super.initView()

        //region 地图模块
        binding.mapLayout.tvCaloriesDescribe.text =
            StringBuilder().append(getString(R.string.healthy_sports_list_calories))
                .append("/").append(getString(R.string.unit_calories))
        binding.mapLayout.tvMinkmDescribe.text =
            StringBuilder().append(getString(R.string.sport_avg_minkm))
                .append("/").append(
                    if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                        R.string.unit_distance_1
                    )
                )

        if (!AppUtils.isEnableGoogleMap()) {
            initAMap()
        } else {
            initGoogleMap()
        }

        setViewsClickListener(
            this,
            binding.mapLayout.ivHoming,
            binding.mapLayout.ivNoMap,
            binding.mapLayout.ivSatellite
        )
        //endregion
    }

    //region 地图模块
    fun setMapData() {
        LogUtils.d("sportModleInfo ----------->")
        LogUtils.json("${GsonUtils.toJson(sportModleInfo)}")
        binding.mapLayout.tvCalories.text = "${sportModleInfo?.burnCalories}"
        binding.mapLayout.tvTime.text =
            TimeUtils.millis2String(sportModleInfo!!.sportDuration * 1000L)

        //TODO 分运动类型
        var mapData = ""
        if (sportModleInfo!!.dataSources == 0) {
            binding.mapLayout.tvMinkm.text =
                DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.avgPace.toInt())
            /*"${(sportModleInfo!!.exerciseApp!!.avgPace.toInt()/(if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)).toInt() / 60}'${(sportModleInfo!!.exerciseApp!!.avgPace.toInt()/(if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)).toInt() % 60}\""*/

            mapData = sportModleInfo!!.exerciseApp!!.mapData
        } else if (sportModleInfo!!.dataSources == 2) {
            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
            ) {
                if (sportModleInfo!!.exerciseOutdoor != null) {
                    binding.mapLayout.tvMinkm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat()
                    )
                    mapData = sportModleInfo!!.exerciseOutdoor!!.gpsMapDatas
                }
            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
            ) {
                /*if (sportModleInfo!!.exerciseIndoor != null) {
                    binding.mapLayout.tvMinkm.text = DevSportInfoManager.calculateMinkm(
                        sportModleInfo!!.sportDuration*1000L,
                        sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat()
                    )
                    mapData = sportModleInfo!!.exerciseIndoor!!
                }*/
            } else {
                if (sportModleInfo!!.exerciseSwimming != null) {
                    binding.mapLayout.tvMinkm.text = DevSportManager.calculateSwimMinkm(
                        sportModleInfo!!.sportDuration,
                        sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat()
                    )
                    mapData = sportModleInfo!!.exerciseSwimming!!.gpsMapDatas

                    binding.mapLayout.tvMinkmDescribe.text =
                        StringBuilder().append(getString(R.string.sport_avg_minkm))
                            .append("/").append("100").append(
                                if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_meter) else getString(
                                    R.string.unit_ft
                                )
                            )
                }
            }
        }

        if (!AppUtils.isEnableGoogleMap()) {
            setAMapData(mapData)
        } else {
            setGoogleMapData(mapData)
        }
    }

    //region 高德地图
    private fun initAMap() {
        //高德没有隐藏地图api ，隐藏图标
        binding.mapLayout.ivNoMap.visibility = View.GONE
        binding.mapLayout.aMap.onCreate(savedInstanceState)
        mapview = binding.mapLayout.aMap
        mAMap = binding.mapLayout.aMap.map
        val mUiSettings = mAMap.uiSettings
        mUiSettings.isRotateGesturesEnabled = true //是否允许旋转
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(16f)) //将地图的缩放级别调整到16级
        if (!AppUtils.isZh(requireActivity())) {
            mAMap.setMapLanguage("en")
        }
        // 设置默认定位按钮是否显示
        mUiSettings.isMyLocationButtonEnabled = false
        // 设置缩放按钮是否显示
        mUiSettings.isZoomControlsEnabled = false
        initpolyline()
    }

    private fun initpolyline() {
        mPolyOptions = PolylineOptions()
        mPolyOptions?.width(ConvertUtils.dp2px(4f).toFloat())
        mPolyOptions?.color(
            ContextCompat.getColor(
                requireActivity(), R.color.sport_locus_color
            )
        )
        mPolyOptions!!.useGradient(true)
    }

    private fun setAMapData(mapData: String?) {
        if (mapData.isNullOrEmpty()) {
            binding.mapLayout.tvNoData.visibility = View.VISIBLE
            binding.mapLayout.ivNoData.visibility = View.VISIBLE
            isNoMapData = true
            return
        }
        val pointDataArray: Array<String> = mapData.split(";").toTypedArray()
//        val testData = "113.838359,22.628666;113.83820311005617,22.629014172411146;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.8382266663925,22.629017348460803;113.838336,22.628724;113.838336,22.628724;113.838336,22.628724;113.838336,22.628724"
//        if(!testData.isNullOrEmpty()){
//            val pointDataArray: Array<String> = testData.split(";").toTypedArray()
        mLatLngList.clear()
        for (i in pointDataArray.indices) {
            AppUtils.tryBlock {
                val latlng = LatLng(
                    pointDataArray[i].split(",").toTypedArray()[1].toDouble(),
                    pointDataArray[i].split(",").toTypedArray()[0].toDouble()
                )
                mLatLngList.add(latlng)
            }
        }
        //少于两个点
        if (mLatLngList.size < 2) {
            binding.mapLayout.tvNoData.visibility = View.VISIBLE
            binding.mapLayout.ivNoData.visibility = View.VISIBLE
            isNoMapData = true
            return
        }
        //展示地图
        mAMap.moveCamera(CameraUpdateFactory.changeLatLng(mLatLngList[0]))
        mAMap.moveCamera(CameraUpdateFactory.zoomTo(16f))
        if (!AppUtils.isZh(requireActivity())) {
            mAMap.setMapLanguage("en")
        }
        val mpathSmoothTool = PathSmoothTool()
        mpathSmoothTool.setIntensity(4)
        val pathoptimizeList: List<LatLng> =
            mpathSmoothTool.pathOptimize(mLatLngList) //不优化 mLatLngList
        if (pathoptimizeList != null && pathoptimizeList.size > 0) {
            drawAMap(pathoptimizeList)
        } else {
            drawAMap(mLatLngList)
        }
    }

    private fun drawAMap(list: List<LatLng>) {
        mPolyOptions!!.addAll(list)
        //根据坐标点设置线段颜色(暂时只支持手机数据类型)
        if (sportModleInfo!!.dataSources == 0)
            mPolyOptions!!.colorValues(getPolyLineColors())
        mAMap.addPolyline(mPolyOptions)
        mAMap.moveCamera(
            CameraUpdateFactory.newLatLngBoundsRect(
                getBounds(list),
                ConvertUtils.dp2px(50f),  //left padding
                ConvertUtils.dp2px(50f),  //right padding
                ConvertUtils.dp2px(50f),  //top padding
                ConvertUtils.dp2px(130f)
            )
        )
        mAMap.addMarker(
            MarkerOptions().position(list[0])
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.sport_start_point))
        )
        mAMap.addMarker(
            MarkerOptions().position(list[list.size - 1])
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.sport_end_point))
        )
    }

    /**
     * 获取高德地图渐变线段参数
     */
    private fun getPolyLineColors(): List<Int> {
        val polylineColors: MutableList<Int> = mutableListOf()
        if (sportModleInfo!!.exerciseApp == null) return polylineColors
        val fullSpeedDatas = sportModleInfo!!.exerciseApp!!.fullSpeedDatas.split(",")
        if (fullSpeedDatas.size >= 16)
            for (index in 1 until fullSpeedDatas.size - 1) {
                var speed = fullSpeedDatas[index].toFloat()
                //去掉波峰速度
                speed = mutableListOf(
                    fullSpeedDatas[index - 1].toFloat(),
                    speed,
                    fullSpeedDatas[index + 1].toFloat()
                ).average().toFloat()

                polylineColors.add(getResColor(speed))
            }
        return polylineColors
    }

    /**
     * 获取谷歌地图渐变线段参数
     * 谷歌地图StyleSpan采用A-B渐变
     */
    private fun getPolyLineStyles(): List<StyleSpan> {
        val styles: MutableList<StyleSpan> = mutableListOf()
        if (sportModleInfo!!.exerciseApp == null) return styles
        val fullSpeedDatas = sportModleInfo!!.exerciseApp!!.fullSpeedDatas.split(",")
        if (fullSpeedDatas.size >= 16) {
            var tempSpeed: Float = fullSpeedDatas[0].toFloat()
            for (index in 1 until fullSpeedDatas.size - 1) {
                var speed = fullSpeedDatas[index].toFloat()
                speed = mutableListOf(
                    fullSpeedDatas[index - 1].toFloat(),
                    speed,
                    fullSpeedDatas[index + 1].toFloat()
                ).average().toFloat()
                @androidx.annotation.ColorRes val fromColor: Int =
                    getResColor(tempSpeed)
                @androidx.annotation.ColorRes val toColor: Int =
                    getResColor(speed)
                styles.add(
                    StyleSpan(
                        StrokeStyle.gradientBuilder(
                            fromColor,
                            toColor
                        ).build()
                    )
                )
                tempSpeed = speed
            }
        }

        return styles
    }

    private fun getResColor(speed: Float): Int {
        return ContextCompat.getColor(
            requireActivity(),
            if (speed < 5)
                R.color.sport_locus_color
            else if (speed < 10) {
                R.color.sport_speed_level_1
            } else if (speed < 15) {
                R.color.sport_speed_level_2
            } else if (speed < 20) {
                R.color.sport_speed_level_3
            } else if (speed < 25) {
                R.color.sport_speed_level_4
            } else {
                R.color.sport_speed_level_5
            }
        )
    }

    private fun getBounds(pointlist: List<LatLng>?): LatLngBounds? {
        val b = LatLngBounds.builder()
        if (pointlist == null) {
            return b.build()
        }
        for (i in pointlist.indices) {
            b.include(pointlist[i])
        }
        return b.build()
    }
    //endregion

    //region google地图
    private fun initGoogleMap() {
        binding.mapLayout.aMap.visibility = View.GONE
        if (AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            binding.mapLayout.googleMapLayout.visibility = View.VISIBLE
        }

        mGMapView = binding.mapLayout.gMap
        mGMapView?.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(googleMap: GoogleMap) {
                LogUtils.d("运动轨迹   nMapReady()")
                googleMap?.let {
                    mGoogleMap = googleMap
                    setMapData()
                }
            }
        })

        mGMapView?.onCreate(savedInstanceState)
    }

    private fun setGoogleMapData(mapData: String?) {
        if (mapData.isNullOrEmpty() || !AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            binding.mapLayout.tvNoData.visibility = View.VISIBLE
            binding.mapLayout.ivNoData.visibility = View.VISIBLE
            isNoMapData = true
            return
        }
        mLatLngList.clear()
        val pointDataArray: Array<String> = mapData.split(";").toTypedArray()
        //region 地图优化
        for (i in pointDataArray.indices) {
            val latlng = LatLng(
                pointDataArray[i].split(",").toTypedArray()[1].toDouble(),
                pointDataArray[i].split(",").toTypedArray()[0].toDouble()
            )
            mLatLngList.add(latlng)
        }
        val mpathSmoothTool = PathSmoothTool()
        mpathSmoothTool.setIntensity(4)
        val pathoptimizeList: List<LatLng>? = mpathSmoothTool.pathOptimize(mLatLngList)
        //endregion
        if (!pathoptimizeList.isNullOrEmpty()) {
            mGLatLngList.clear()
            pathoptimizeList.forEach {
                val latlng = com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude)
                mGLatLngList.add(latlng)
            }
        }
        //少于两个点
        if (mGLatLngList.size < 2) {
            binding.mapLayout.tvNoData.visibility = View.VISIBLE
            binding.mapLayout.ivNoData.visibility = View.VISIBLE
            isNoMapData = true
            return
        }

        AppUtils.tryBlock {
            if (::mGoogleMap.isInitialized) {
                mGoogleMap.clear()
                mPolylineOptions = com.google.android.gms.maps.model.PolylineOptions()
                mPolylineOptions!!.width(ConvertUtils.dp2px(4f).toFloat())
                    .color(ContextCompat.getColor(requireActivity(), R.color.sport_locus_color))
                    .geodesic(false)
                mPolylineOptions?.addAll(mGLatLngList)
                if (sportModleInfo!!.dataSources == 0)
                    mPolylineOptions?.addAllSpans(getPolyLineStyles())
                val southwest: com.google.android.gms.maps.model.LatLng = mGLatLngList[0]
                val northeast: com.google.android.gms.maps.model.LatLng =
                    mGLatLngList[mGLatLngList.size - 1]
                val markerOptions1 = com.google.android.gms.maps.model.MarkerOptions()
                markerOptions1.position(southwest)
                markerOptions1.icon(
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(
                        R.mipmap.sport_start_point
                    )
                )
                mGoogleMap.addMarker(markerOptions1)
                val markerOptions2 = com.google.android.gms.maps.model.MarkerOptions()
                markerOptions2.position(northeast)
                markerOptions2.icon(
                    com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource(
                        R.mipmap.sport_end_point
                    )
                )
                mGoogleMap.addMarker(markerOptions2)
                if (mGoogleMap != null) {
                    mPolyline = mGoogleMap.addPolyline(mPolylineOptions!!)
                    //                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mGlatLngList.get(0), 16.0f));
                    val myLatLng = com.google.android.gms.maps.model.LatLng(
                        (southwest.latitude + northeast.latitude) / 2,
                        (southwest.longitude + northeast.longitude) / 2
                    )
                    mGoogleMap.animateCamera(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                            myLatLng,
                            16.0f
                        )
                    )
                    LogUtils.d("运动轨迹 不等于空")
                } else {
                    LogUtils.d("运动轨迹 等于空了")
                }
            }
        }

    }


    //endregion

    //region 生命周期
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.savedInstanceState = savedInstanceState
    }

    override fun onResume() {
        super.onResume()
        mapview?.onResume()
        AppUtils.tryBlock {
            mGMapView?.onResume()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapview?.onSaveInstanceState(outState)
        AppUtils.tryBlock {
            mGMapView?.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapview?.onLowMemory()
        AppUtils.tryBlock {
            mGMapView?.onLowMemory()
        }
    }

    /*  override fun onPause() {
          super.onPause()
          mapview?.onPause()
          AppUtils.tryBlock {
              mGMapView?.onPause()
          }
      }*/

    override fun onDestroyView() {
        super.onDestroyView()
        AppUtils.tryBlock {
            mapview?.onDestroy()
            mGMapView?.onDestroy()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    //endregion
    //endregion

    override fun initData() {
        super.initData()
        sportModleInfo = viewmodel.sportLiveData.getSportModleInfo().value
        if (sportModleInfo == null) {
            return
        }

        //region 地图模块
        binding.mapLayout.tvNoData.visibility = View.GONE
        binding.mapLayout.ivNoData.visibility = View.GONE
        setMapData()
        //endregion
    }

    //获取高德地图bitmap
    private suspend fun getAMapBitmap(): Bitmap? {
        if (isNoMapData) {
            return null
        }

        return withTimeoutOrNull(3000) { //增加3秒超时
            suspendCancellableCoroutine<Bitmap?> { result ->
                if (!AppUtils.isEnableGoogleMap()) {
                    // 高德地图 截屏
                    mAMap.getMapScreenShot(object : AMap.OnMapScreenShotListener {
                        override fun onMapScreenShot(bm: Bitmap?) {
                            result.resume(bm)
                        }

                        override fun onMapScreenShot(bm: Bitmap?, p1: Int) {
                            //result.resume(bm)
                        }
                    })
                } else {

                    // Google地图 截屏
                    if (::mGoogleMap.isInitialized) {
                        mGoogleMap.snapshot(object : GoogleMap.SnapshotReadyCallback {
                            override fun onSnapshotReady(bm: Bitmap?) {
                                result.resume(bm)
                            }
                        })
                    } else {
                        result.resume(null)
                    }
                }
            }
        }
    }

    /**
     * 拼接bitmap
     * */
    fun toConformBitmap(background: Bitmap, foreground: Bitmap?): Bitmap? {
        if (foreground == null) return null
        val newbmp = Bitmap.createBitmap(background.width, background.height, Bitmap.Config.RGB_565)
        val paint = Paint()
        paint.isAntiAlias = true
        val canvas = Canvas(newbmp)
        canvas.drawBitmap(background, 0f, 0f, null)
        canvas.drawBitmap(foreground, 0f, 0f, null)
        canvas.save()
        canvas.restore()
        return newbmp
    }

    //成分享的bitmap
    suspend fun createDataBitMap(): Bitmap? {
        return toConformBitmap(ImageUtils.view2Bitmap(binding.data), getAMapBitmap())
    }


    override fun onClick(v: View) {
        when (v.id) {
            binding.mapLayout.ivHoming.id -> { //定位
                AppUtils.tryBlock {
                    if (!AppUtils.isEnableGoogleMap()) {
                        if (mLatLngList.isNotEmpty()) {
                            val homingLatlng = mLatLngList.get(mLatLngList.size / 2)
                            mAMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(homingLatlng.latitude, homingLatlng.longitude), 17f
                                ), 1000, null
                            )
                        }
                    } else {
                        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                            showNoGoogleServiceDialog()
                            return@tryBlock
                        }
                        if (::mGoogleMap.isInitialized) {
                            if (mGLatLngList.isNotEmpty()) {
                                val homingLatlng = mGLatLngList.get(mGLatLngList.size / 2)
                                mGoogleMap.animateCamera(
                                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(
                                        homingLatlng,
                                        15.0f
                                    )
                                )
                            }
                        }
                    }
                }
            }
            binding.mapLayout.ivNoMap.id -> { //不显示地图，只显示标记和轨迹
                binding.mapLayout.ivNoMap.isSelected = !isNoShowMap
                AppUtils.tryBlock {
                    if (!AppUtils.isEnableGoogleMap()) {
                        mAMap.mapType = if (isNoShowMap) {
                            isNoShowMap = false
                            if (isSatellite) {
                                GoogleMap.MAP_TYPE_SATELLITE
                            } else {
                                GoogleMap.MAP_TYPE_NORMAL
                            }
                        } else {
                            isNoShowMap = true
                            //TODO ！！！！！！！
                            GoogleMap.MAP_TYPE_NONE
                        }
                    } else {

                        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                            showNoGoogleServiceDialog()
                            return@tryBlock
                        }
                        if (::mGoogleMap.isInitialized) {
                            mGoogleMap.mapType =
                                if (isNoShowMap) {
                                    isNoShowMap = false
                                    if (isSatellite) {
                                        AMap.MAP_TYPE_SATELLITE
                                    } else {
                                        AMap.MAP_TYPE_NORMAL
                                    }
                                } else {
                                    isNoShowMap = true
                                    GoogleMap.MAP_TYPE_NONE
                                }
                        }
                    }
                }
            }
            binding.mapLayout.ivSatellite.id -> { //切换卫星地图
                AppUtils.tryBlock {
                    if (!AppUtils.isEnableGoogleMap()) {
                        mAMap.mapType = if (isSatellite) {
                            isSatellite = false
                            AMap.MAP_TYPE_NORMAL
                        } else {
                            isSatellite = true
                            AMap.MAP_TYPE_SATELLITE
                        }
                    } else {
                        if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                            showNoGoogleServiceDialog()
                            return@tryBlock
                        }
                        if (::mGoogleMap.isInitialized) {
                            mGoogleMap.mapType = if (isSatellite) {
                                isSatellite = false
                                GoogleMap.MAP_TYPE_NORMAL
                            } else {
                                isSatellite = true
                                GoogleMap.MAP_TYPE_SATELLITE
                            }
                        }
                    }
                }
            }
        }
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
}