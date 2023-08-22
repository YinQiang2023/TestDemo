package com.smartwear.publicwatch.ui.device.weather

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.blankj.utilcode.util.NetworkUtils
import com.zhapp.ble.ControlBleTools
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.ActivityWeatherBinding
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.device.weather.utils.MapManagerUtils
import com.smartwear.publicwatch.ui.device.weather.utils.WeatherManagerUtils
import com.smartwear.publicwatch.ui.user.QAActivity
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.viewmodel.DeviceModel
import java.lang.ref.WeakReference
import java.util.*

class WeatherActivity : BaseActivity<ActivityWeatherBinding, DeviceModel>(
    ActivityWeatherBinding::inflate,
    DeviceModel::class.java
), View.OnClickListener {
    private val TAG: String = WeatherActivity::class.java.simpleName

    private val RESULT_CODE_LOCATION = 0 // 请求码
    private val RESULT_CODE_SEARCH_CITY = 0x180 // 请求码

    //等待loading
    private var loadDialog: Dialog? = null
    private var timeOutHandler = Handler(Looper.getMainLooper())
    private var timeOutRunnable = Runnable {
        MapManagerUtils.stopGps()
        dismissDialog()
        ToastUtils.showToast(R.string.locate_failure_tips)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.layoutCityName.id -> {
                if (AppUtils.isZh(this)) {
                    //中文不允许搜索
                    return
                }
                startActivityForResult(
                    Intent(this, WeatherCitySearchActivity::class.java),
                    RESULT_CODE_SEARCH_CITY
                )
            }
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
            }
            binding.chbSwitchButton.id -> {
                when (binding.chbSwitchButton.tag) {
                    null -> {
                        binding.chbSwitchButton.tag = true
                    }
                    true -> {
                        binding.chbSwitchButton.tag = false
                    }
                    else -> {
                        binding.chbSwitchButton.tag = true
                    }
                }
                replaceBackground()
                onCheck()
            }
        }
    }

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.weather_title)

        //阿拉伯适配
        val language = Locale.getDefault().language;
        if (language.equals("ar")) {
            val drawableLeft: Drawable? = getDrawable(R.mipmap.img_right_arrow)
            binding.tvCityName?.setCompoundDrawablesWithIntrinsicBounds(
                drawableLeft,
                null, null, null
            )
            binding.tvCityName.compoundDrawablePadding = 4
        }

        loadDialog = DialogUtils.showLoad(this)
        if ((TextUtils.isEmpty(SpUtils.getValue(SpUtils.WEATHER_SWITCH, "")) || SpUtils.getValue(
                SpUtils.WEATHER_SWITCH,
                ""
            ) == "false") && TextUtils.isEmpty(SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, ""))
        ) {
            binding.chbSwitchButton.tag = false
        } else {
            binding.chbSwitchButton.tag = SpUtils.getValue(
                SpUtils.WEATHER_SWITCH,
                "false"
            ).trim().toBoolean()
        }
        replaceBackground()
        if (!TextUtils.isEmpty(SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, ""))) {
            binding.tvCityName.text = SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
        }
        setViewsClickListener(this, binding.chbSwitchButton, binding.layoutCityName, binding.tvHelp)
        binding.tvHelp.paint.flags = Paint.UNDERLINE_TEXT_FLAG
        if (AppUtils.isZh(this)) {
            //中文不允许搜索
            binding.tvCityName.setCompoundDrawables(null, null, null, null)
        }
    }

    private fun replaceBackground() {
        if (binding.chbSwitchButton.tag == null) {
            binding.chbSwitchButton.setImageResource(R.mipmap.sleep_on_switch)
            binding.layoutCityName.visibility = View.VISIBLE
        } else if (binding.chbSwitchButton.tag == true) {
            binding.chbSwitchButton.setImageResource(R.mipmap.sleep_on_switch)
            binding.layoutCityName.visibility = View.VISIBLE
        } else {
            binding.chbSwitchButton.setImageResource(R.mipmap.sleep_off_switch)
            binding.layoutCityName.visibility = View.GONE
        }
    }

    private fun onCheck() {
        com.blankj.utilcode.util.LogUtils.i(TAG, "onCheck()")
        if (!NetworkUtils.isConnected()) {
            binding.tvCityName.text = ""
            binding.chbSwitchButton.tag = false
            replaceBackground()
            SpUtils.setValue(SpUtils.WEATHER_CITY_NAME, "")
            SpUtils.setValue(SpUtils.WEATHER_SWITCH, "false")
            SendCmdUtils.setUserInformation()
            ToastUtils.showToast(R.string.not_network_tips)
            return
        }
        if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
            binding.tvCityName.text = ""
            binding.chbSwitchButton.tag = false
            replaceBackground()
            AppUtils.showGpsOpenDialog()
            return
        }
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getStartTypeTrack("天气"), isStart = true)
        if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_GROUP_LOCATION)) {
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getAppTypeTrack("定位权限未允许或拒绝(Android)"), "1611", true)
        }
        val isPerMission = PermissionUtils.checkRequestPermissions(
            this.lifecycle,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getString(R.string.permission_location_12) else getString(R.string.permission_location),
            PermissionUtils.PERMISSION_GROUP_LOCATION
        ) {
            Log.i(TAG, "onCheck() 定位权限-已授权")
            timeOutHandler.removeCallbacksAndMessages(null)
            if (binding.chbSwitchButton.tag == true) {
                Log.i(TAG, "onCheck() 天气开关-开")
                AppTrackingManager.saveOnlyBehaviorTracking("7", "12")
                timeOutHandler.postDelayed(timeOutRunnable, 30 * 1000)
                loadDialog?.show()
                SpUtils.setValue(SpUtils.WEATHER_SWITCH, "true")
                SendCmdUtils.setUserInformation()
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getAppTypeTrack("开始定位"))
                MapManagerUtils.getLatLon(object : MapManagerUtils.LocationListener {
                    override fun onLocationChanged(gpsInfo: MapManagerUtils.GpsInfo?) {
                        timeOutHandler.removeCallbacksAndMessages(null)
                        Log.i(TAG, "onCheck() 定位获取成功")
                        ToastUtils.showToast(getString(R.string.locate_success_tips))

                        MapManagerUtils.stopGps()
                        val spGps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "");
                        if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                            val gps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                            if (gps.isNotEmpty() && gps.size == 2) {
                                Log.i(TAG, "onCheck() 通过经纬度获取当前天气")
                                //val trackingLog = TrackingLog.getSerTypeTrack("获取当天天气信息","获取当天天气信息","")
                                WeatherManagerUtils.getCurrentWeather(
                                    needUpdataCityInfo = true,
                                    needReturnCityInfo = true,
                                    oldLat = gps[1].toDouble(),
                                    oldLon = gps[0].toDouble(),
                                    getOpenWeatherListener = object : WeatherManagerUtils.GetOpenWeatherListener {
                                        override fun onSuccess(cityName: String) {
                                            Log.i(TAG, "onCheck() 通过经纬度获取当前天气-成功 cityName = $cityName")
                                            runOnUiThread {
                                                dismissDialog()
                                                binding.tvCityName.text = SpUtils.getValue(
                                                    SpUtils.WEATHER_CITY_NAME,
                                                    ""
                                                )
                                                WeatherManagerUtils.sendWeatherDay()
                                            }
                                        }

                                        override fun onFail(msg: String) {
                                            Log.i(TAG, "onCheck() 通过经纬度获取当前天气-失败:$msg")
                                            timeOutHandler.removeCallbacksAndMessages(null)
                                            runOnUiThread {
                                                dismissDialog()
                                            }
                                            if(msg.contains("GPS close")) {
                                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getAppTypeTrack("定位服务不可用").apply {
                                                    log = " GPS未打开"
                                                }, "1610", true)
                                            }
                                        }
                                    })
                                return
                            }
                        }
                        dismissDialog()
                    }

                    override fun onFailure(msg: String) {
                        Log.i(TAG, "onCheck() 定位获取失败")
                        if (ControlBleTools.getInstance().isConnect) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getAppTypeTrack("定位超时/失败"), "1612", true)
                        }

                        timeOutHandler.removeCallbacksAndMessages(null)
                        MapManagerUtils.stopGps()
                        dismissDialog()
                        binding.chbSwitchButton.tag = false
                        replaceBackground()
                        SpUtils.setValue(SpUtils.WEATHER_SWITCH, "false")
                        SendCmdUtils.setUserInformation()
                        if (msg == "close") {
                            Log.i(TAG, "onCheck() 定位获取失败 - 定位未开启，是否打开定位？")
                            val dialog =
                                DialogUtils.showDialogContentAndTwoBtn(this@WeatherActivity,
                                    getString(R.string.locate_tips1),
                                    getString(R.string.dialog_cancel_btn),
                                    getString(R.string.dialog_confirm_btn),
                                    object : DialogUtils.DialogClickListener {
                                        override fun OnOK() {
                                            startActivity(
                                                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                                )
                                            )
                                        }

                                        override fun OnCancel() {
                                        }

                                    })
                            dialog.show()
                        }
                    }

                }, true)
            } else {
                Log.i(TAG, "onCheck() 天气开关-关")
                binding.chbSwitchButton.tag = false
                replaceBackground()
                binding.tvCityName.text = ""
                SpUtils.setValue(SpUtils.WEATHER_CITY_NAME, "")
                SpUtils.setValue(SpUtils.WEATHER_SWITCH, "false")
                SendCmdUtils.setUserInformation()
            }
        }

        if (!isPerMission) {
            Log.e(TAG, "onCheck() 定位权限-未授权")
            binding.chbSwitchButton.tag = false
            replaceBackground()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_CODE_SEARCH_CITY && resultCode == Activity.RESULT_OK) {
            loadDialog?.show()
            SpUtils.setValue(SpUtils.WEATHER_SWITCH, "true")
            SendCmdUtils.setUserInformation()
            val spGps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "");
            if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                val gps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                if (gps.isNotEmpty() && gps.size == 2) {
                    binding.chbSwitchButton.tag = true
                    replaceBackground()
                    binding.tvCityName.text = SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
                    WeatherManagerUtils.getCurrentWeather(
                        needUpdataCityInfo = false,
                        needReturnCityInfo = true,
                        oldLat = gps[1].toDouble(),
                        oldLon = gps[0].toDouble(),
                        getOpenWeatherListener = MyGetOpenWeatherListener(this@WeatherActivity)
                    )
                }
            }
        }
    }

    class MyGetOpenWeatherListener(activity: WeatherActivity) : WeatherManagerUtils.GetOpenWeatherListener {
        private var wrActivity: WeakReference<WeatherActivity>? = null

        init {
            wrActivity = WeakReference(activity)
            if (wrActivity?.get() == null) {
                LogUtils.e("DeviceFragment", "WeatherActivity is Null")
            }
        }

        override fun onSuccess(cityName: String) {
            wrActivity?.get()?.dismissDialog()
            WeatherManagerUtils.sendWeatherDay()
        }

        override fun onFail(msg: String) {
            wrActivity?.get()?.dismissDialog()
        }

    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(loadDialog)
    }
}