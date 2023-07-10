package com.jwei.publicone.ui.guide.item

import android.app.Dialog
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.jwei.publicone.R
import com.jwei.publicone.databinding.FragmentSelectWeatherBinding
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.device.weather.utils.MapManagerUtils
import com.jwei.publicone.ui.device.weather.utils.WeatherManagerUtils
import com.jwei.publicone.utils.*
import com.jwei.publicone.viewmodel.DeviceModel

class SelectWeatherFragment : BaseFragment<FragmentSelectWeatherBinding, DeviceModel>(
    FragmentSelectWeatherBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private lateinit var loadDialog: Dialog

    private val timeOutHandler = Handler(Looper.getMainLooper())
    private val timeOutRunnable = Runnable {
        MapManagerUtils.stopGps()
        binding.switchWeather.isChecked = false
        if (loadDialog.isShowing) loadDialog.dismiss()
    }

    companion object {
        val instance: SelectWeatherFragment by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SelectWeatherFragment()
        }
    }

    override fun initView() {
        super.initView()
        loadDialog = DialogUtils.showLoad(requireActivity())
        loadDialog.setCanceledOnTouchOutside(false)
        loadDialog.setCancelable(false)

        binding.switchWeather.isChecked = false
        binding.switchWeather.setOnCheckedChangeListener { _, isChecked ->
            if (!AppUtils.isGPSOpen(requireActivity())) {
                AppUtils.showGpsOpenDialog()
                binding.switchWeather.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (!AppUtils.isOpenBluetooth()) {
                ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                binding.switchWeather.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (!ControlBleTools.getInstance().isConnect) {
                ToastUtils.showToast(R.string.device_no_connection)
                binding.switchWeather.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getString(R.string.permission_location_12)
                    } else {
                        getString(R.string.permission_location)
                    },
                    PermissionUtils.PERMISSION_GROUP_LOCATION
                ) {
                    if (isChecked) {
                        if (loadDialog.isShowing) {
                            DialogUtils.dismissDialog(loadDialog)
                        }
                        loadDialog.show()
                        binding.llCityInfo.visibility = View.VISIBLE
                        binding.llCityHint.visibility = View.VISIBLE
                        requestCurrentCityWeather()
                    } else {
                        if (loadDialog.isShowing) {
                            DialogUtils.dismissDialog(loadDialog)
                        }
                        binding.llCityInfo.visibility = View.GONE
                        binding.llCityHint.visibility = View.GONE
                        SpUtils.setValue(SpUtils.WEATHER_SWITCH, "false")
                    }
                }
            ) else {
                binding.switchWeather.isChecked = false
            }
        }
    }

    override fun onClick(v: View?) = Unit

    private fun requestCurrentCityWeather() {
        LogUtils.i("权限齐全可以发起定位")
        timeOutHandler.postDelayed(timeOutRunnable, 30000)
        MapManagerUtils.getLatLon(object : MapManagerUtils.LocationListener {
            override fun onLocationChanged(gpsInfo: MapManagerUtils.GpsInfo?) {
                timeOutHandler.removeCallbacksAndMessages(null)
                LogUtils.i("定位获取成功")
                val stopGpsStatus = MapManagerUtils.stopGps()
                LogUtils.i("停止定位结果:$stopGpsStatus")
                val spGps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")
                if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                    val gps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                    if (gps.isNotEmpty() && gps.size == 2) {
                        LogUtils.i("通过经纬度获取当前天气")
                        WeatherManagerUtils.getCurrentWeather(
                            needUpdataCityInfo = true,
                            needReturnCityInfo = true,
                            oldLat = gps[1].toDouble(),
                            oldLon = gps[0].toDouble(),
                            getOpenWeatherListener = object : WeatherManagerUtils.GetOpenWeatherListener {
                                override fun onSuccess(cityName: String) {
                                    if (loadDialog.isShowing) {
                                        DialogUtils.dismissDialog(loadDialog)
                                    }
                                    LogUtils.i("通过经纬度获取当前天气-成功 cityName = $cityName")
                                    binding.tvCityName.text = SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
                                    SpUtils.setValue(SpUtils.WEATHER_SWITCH, "true")
                                    SpUtils.setValue(SpUtils.WEATHER_SYNC_TIME, "${System.currentTimeMillis()}")
                                    WeatherManagerUtils.sendWeatherDay()
                                }

                                override fun onFail(msg:String) {
                                    LogUtils.i("通过经纬度获取当前天气-失败")
                                    timeOutHandler.removeCallbacksAndMessages(null)
                                    if (loadDialog.isShowing) {
                                        DialogUtils.dismissDialog(loadDialog)
                                    }
                                }
                            })
                        return
                    }
                }
            }

            override fun onFailure(msg: String) {
                LogUtils.i("onCheck() 定位获取失败")
                if (loadDialog.isShowing) {
                    DialogUtils.dismissDialog(loadDialog)
                }
                timeOutHandler.removeCallbacksAndMessages(null)
                MapManagerUtils.stopGps()
                SpUtils.setValue(SpUtils.WEATHER_SWITCH, "false")
                SendCmdUtils.setUserInformation()
            }
        }, true)
    }
}