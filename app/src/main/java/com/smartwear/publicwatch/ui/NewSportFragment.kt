package com.smartwear.publicwatch.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.tabs.TabLayout
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.databinding.NewSportFragmentBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.device.weather.utils.MapManagerUtils
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.sport.SportCountDownActivity
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.viewmodel.SportModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Android on 2023/4/21.
 * 新运动首页
 */
class NewSportFragment : BaseFragment<NewSportFragmentBinding, SportModel>(
    NewSportFragmentBinding::inflate, SportModel::class.java
), View.OnClickListener {
    //运动类型  0 -户外运动  1 -户外骑行  2-户外健走
    private var mSportType = 0

    //是否点击start
    private var isClickStart = false

    //是否初次请求了定位
    private var isFirstLocation = false

    //分钟变化数
    private var minuteChange = 0

    //是否设备请求辅助运动无权限
    private var isDevRequestPer = false


    override fun setTitleId(): Int {
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        AppUtils.registerEventBus(this)
        //获取定位
        if (PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            getLocation()
        }
        setViewsClickListener(this, binding.ivStart)
    }


    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }


    override fun initData() {
        super.initData()

        viewmodel.sportLiveData.getAppSportType().postValue(mSportType)

        //app运动选择
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    mSportType = tab.position
                    viewmodel.sportLiveData.getAppSportType().postValue(mSportType)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        //信号强度
        /*viewmodel.GPSRssl.observe(this) {
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
        }*/
    }

    override fun onVisible() {
        super.onVisible()
        //初次权限提示
        if (SpUtils.getSPUtilsInstance()
                .getBoolean(SpUtils.FIRST_REQUEST_LOCATION_PERMISSION, true)
        ) {
            SpUtils.getSPUtilsInstance().put(SpUtils.FIRST_REQUEST_LOCATION_PERMISSION, false)
            if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                showPermissionExplainDialog()
            }
        }

        //开始运动被拒权限申请后
        if (isClickStart) {
            if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                return
            }
            if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                return
            }
            isClickStart = false
            binding.ivStart.callOnClick()
        }

        if (!isFirstLocation) {
            if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                return
            }
            if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                return
            }
            getLocation()
        }
    }

    //region 获取定位

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            return
        }
        if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
            return
        }
        isFirstLocation = true

        if (stopLocationTask != null) {
            ThreadUtils.cancel(stopLocationTask)
        }
        stopLocationTask = StopLocationTask()
        ThreadUtils.executeByIo(stopLocationTask)


        MapManagerUtils.getLatLon(object : MapManagerUtils.LocationListener {
            override fun onLocationChanged(gpsInfo: MapManagerUtils.GpsInfo?) {
                LogUtils.i("定位获取成功:" + GsonUtils.toJson(gpsInfo))
                //{"altitude":0.0,"bearing":0.0,"gpsAccuracy":0,"horizontal_accuracy":0.0,"latitude":22.628888,"longitude":113.838115,"speed":0.0,"timestamp":0,"vertical_accuracy":0.0}
                if (stopLocationTask != null) {
                    ThreadUtils.cancel(stopLocationTask)
                }
                MapManagerUtils.stopGps()
            }

            override fun onFailure(msg: String) {
                LogUtils.i("定位获取失败：$msg")
                if (stopLocationTask != null) {
                    ThreadUtils.cancel(stopLocationTask)
                }
                MapManagerUtils.stopGps()
            }

        })
    }

    private var stopLocationTask: StopLocationTask? = null

    private inner class StopLocationTask : ThreadUtils.SimpleTask<Boolean>() {
        override fun doInBackground(): Boolean {
            var i = 0
            while (i < 10) {
                i++
                Thread.sleep(1000)
                LogUtils.d("StopLocationTask --> $i")
            }
            return true
        }

        override fun onSuccess(result: Boolean?) {
            MapManagerUtils.stopGps()
            stopLocationTask = null
        }
    }

    //endregion

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun refMap(event: EventMessage) {
        //设备辅助运动无权限
        if (event.action == EventAction.ACTION_DEV_SPORT_NO_PERMISSION) {
            isDevRequestPer = true
            showDeviceSportNoPermissionHint()
        }
        //设备辅助运动未开GPS
        if (event.action == EventAction.ACTION_DEV_SPORT_NO_GPS) {
            showDeviceSportNoGpsHint()
        }
        //GPS卫星强度变化
        /*if (event.action == EventAction.ACTION_GPS_SATELLITE_CHANGE) {
            //设置gps强度
            if (event.obj != null && event.obj is SatelliteBean) {
                val satellite = event.obj as SatelliteBean
                val gpsssl = viewmodel.calculateGPS(satellite.max, satellite.valid)
                LogUtils.d("更新GPS强度 --> $gpsssl")
                viewmodel.setGPSRssl(gpsssl)
            }
        }*/
        //时间变化，每12小时新一次定位
        if (event.action == EventAction.ACTION_TIME_CHANGED) {
            minuteChange++
            if (minuteChange >= 12 * 60) {
                minuteChange = 0
                getLocation()
            }
        }
    }

    override fun onClick(v: View) {
        if (AppUtils.isEnableGoogleMap()) {
            if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                showNoGoogleServiceDialog()
                return
            }
        }
        when (v.id) {
            binding.ivStart.id -> {
                isClickStart = true
                if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION)) {
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
        }
    }

    //region 权限提示相关


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
                    LogUtils.d(com.smartwear.publicwatch.utils.PermissionUtils.TAG, "申请权限")
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
        com.smartwear.publicwatch.utils.PermissionUtils.requestPermissions(
            this.lifecycle,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getString(R.string.permission_location_12) else getString(R.string.permission_location),
            com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_GROUP_LOCATION
        ) {
            //获取定位
            getLocation()
            checkQLocationPermission()
            if(isDevRequestPer){
                isDevRequestPer = false
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_DEV_SPORT_PERMISSION))
            }
        }
    }

    /**
     * android 10 后台定位权限申请
     * */
    fun checkQLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionUtils.isGranted(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_10_LOCATION)) {
                DialogUtils.dialogShowContentAndTwoBtn(requireActivity(), getString(R.string.background_location_tips),
                    getString(R.string.dialog_cancel_btn), getString(R.string.dialog_confirm_btn), object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            PermissionUtils.permission(*com.smartwear.publicwatch.utils.PermissionUtils.PERMISSION_10_LOCATION)
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
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
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
                    LogUtils.d(com.smartwear.publicwatch.utils.PermissionUtils.TAG, "申请权限")
                    requestPermissions()
                }

                override fun OnCancel() {
                    isDevRequestPer = false
                }
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
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
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
    //endregion

}