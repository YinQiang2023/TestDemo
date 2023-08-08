package com.smartwear.xzfit.ui.device.weather.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.blankj.utilcode.util.GsonUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.utils.*
import java.lang.Exception

object MapManagerUtils {
    private val TAG: String = MapManagerUtils::class.java.simpleName

    private var oldLatLng: LatLng? = null
    private const val old_long_time: Long = 0
    private var locationListener: LocationListener? = null

    fun getLatLon(locationListener: LocationListener, isGpsLocation: Boolean = false) {
        try {
            stopGps()
            MapManagerUtils.locationListener = locationListener

            if (!isGPSOpen(BaseApplication.mContext)) {
                LogUtils.e(TAG, "getLatLon() GPS没开")
//                EventBus.getDefault().post(ShowDialogEvent(1))
//                ToastUtils.showToast(BaseApplication.mContext.getString(R.string.gps_switch_tips))
//                locationListener.onFailure(BaseApplication.mContext.getString(R.string.gps_switch_tips))
                locationListener.onFailure(" GPS close")
                return
            }

            if (AppUtils.isEnableGoogleMap()) {
                /*if (AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                    LogUtils.e(TAG, "getLatLon is google gps")
                    //加载google 定位
                    initGoogleMap(BaseApplication.mContext, isGpsLocation)
                } else {
                    LogUtils.e(TAG, "getLatLon is LocationManager gps")
                    initLocationManager()
                }*/
                LogUtils.e(TAG, "getLatLon is LocationManager")
                initLocationManager()
            } else {
                LogUtils.e(TAG, "getLatLon is amap gps")
                initAMap(BaseApplication.mContext)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "getLatLon Exception " + e.printStackTrace())
            e.printStackTrace()
        }
    }

    //region Google
    private var mLastLocation: Location? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var oldGoogleLatLng: com.google.android.gms.maps.model.LatLng? = null
    private var mLocationRequest: LocationRequest? = null

    private val googleLocationListener =
        LocationListener { location: Location ->
            onLocationChanged(location)
        }

    private fun onLocationChanged(location: Location) {
        val newLatLng =
            com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
        if (oldGoogleLatLng !== newLatLng) {
            if (newLatLng.latitude == 0.0 && newLatLng.longitude == 0.0) {
                return
            }
            val gpsInfo = GpsInfo()
            gpsInfo.latitude = location.latitude
            gpsInfo.longitude = location.longitude
            gpsInfo.altitude = location.altitude
            LogUtils.i(TAG, "  google Accuracy=" + location.accuracy)
            val satellite = location.accuracy.toInt()
            if (satellite >= 1 && satellite < 3) {
                gpsInfo.gpsAccuracy = GpsInfo.GPS_LOW
            } else if (satellite >= 3 && satellite < 6) {
                gpsInfo.gpsAccuracy = GpsInfo.GPS_MEDIUM
            } else if (satellite >= 6) {
                gpsInfo.gpsAccuracy = GpsInfo.GPS_HIGH
            }
            LogUtils.i(TAG, "onLocationChanged gpsInfo = " + gpsInfo.longitude + "," + gpsInfo.latitude)
            SpUtils.getSPUtilsInstance().put(
                SpUtils.WEATHER_LONGITUDE_LATITUDE,
                gpsInfo.longitude.toString() + "," + gpsInfo.latitude
            )
            if (locationListener != null) {
                locationListener!!.onLocationChanged(gpsInfo)
            }
        }
        oldGoogleLatLng = newLatLng
    }

    private fun initGoogleMap(context: Context, isGpsLocation: Boolean) {
        mGoogleApiClient = GoogleApiClient.Builder(context)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                @SuppressLint("MissingPermission")
                override fun onConnected(bundle: Bundle?) {
                    try {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient!!)
                        if (mLastLocation != null) {
                            oldGoogleLatLng =
                                com.google.android.gms.maps.model.LatLng(
                                    mLastLocation!!.latitude,
                                    mLastLocation!!.longitude
                                )
                        }
                        mLocationRequest = LocationRequest.create()
                        //GPS定位
                        if (isGpsLocation) {
                            mLocationRequest?.interval = (3 * 1000).toLong() //5 seconds //间隔
                            mLocationRequest?.fastestInterval = (2 * 1000).toLong() //3 seconds
                            mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //GPS-高精度
                            mLocationRequest?.smallestDisplacement = 3f //定位精度
                        } else {
                            mLocationRequest?.interval = (5 * 1000).toLong() //5 seconds //间隔
                            mLocationRequest?.fastestInterval = (3 * 1000).toLong() //3 seconds
                            mLocationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY //定位模式
                            mLocationRequest?.smallestDisplacement = 5f //定位精度
                        }
                        if (mGoogleApiClient != null && mGoogleApiClient!!.isConnected()) {
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                mGoogleApiClient!!,
                                mLocationRequest!!,
                                googleLocationListener
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onConnectionSuspended(i: Int) {}
            })
            .addOnConnectionFailedListener { connectionResult: ConnectionResult? -> }
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient!!.connect()
    }
    //endregion

    //region 高德AMap
    @SuppressLint("StaticFieldLeak")
    private var mlocationClient: AMapLocationClient? = null

    private fun initAMap(content: Context) {
        mlocationClient = AMapLocationClient(content.applicationContext)
        val mLocationOption: AMapLocationClientOption = getDefaultOption()
        //设置定位参数
        mlocationClient?.setLocationOption(mLocationOption)
        /*notification = ControlBleTools.getInstance().notification
        if (notification != null) {
            mlocationClient?.enableBackgroundLocation(ControlBleTools.getInstance().notificationId, notification)
        }*/

        mlocationClient?.startLocation()
        // 设置定位监听
        mlocationClient?.setLocationListener(AMapLocationListener { location: AMapLocation ->
            if (location.errorCode != 0) {
                LogUtils.e(TAG, "errorcode =" + location.errorCode + " info=" + location.errorInfo + " detail=" + location.locationDetail)
//                locationListener?.onFailure(location.errorInfo)
                locationListener?.onFailure(content.getString(R.string.locate_failure_tips))
                return@AMapLocationListener
            }
            val newLatLng =
                LatLng(location.latitude, location.longitude)
            if (newLatLng.latitude == 0.0 && newLatLng.longitude == 0.0) {
//                locationListener?.onFailure("lat and lon is 0")
                locationListener?.onFailure(content.getString(R.string.locate_failure_tips))
                return@AMapLocationListener
            }
            LogUtils.e(TAG, "  new LatLng" + " lat = " + newLatLng.latitude + " lon = " + newLatLng.longitude)
            val nowTime = System.currentTimeMillis()
            if (oldLatLng != null) {
                val xSeconds: Long = (nowTime - old_long_time) / 1000
                val newDistance = AMapUtils.calculateLineDistance(oldLatLng, newLatLng)
                if (newDistance / xSeconds > 300.0) {
//                    locationListener?.onFailure("newDistance / xSeconds > 300")
                    locationListener?.onFailure(content.getString(R.string.locate_failure_tips))
                    return@AMapLocationListener
                }
                if (oldLatLng !== newLatLng) {
                    sendLocationChanged(location)
                }
            } else {
                sendLocationChanged(location)
            }
            oldLatLng = newLatLng
        })
    }

    private fun getDefaultOption(type: Int = 0): AMapLocationClientOption {
        val mOption = AMapLocationClientOption()
        if (type == 1) {
            mOption.locationMode = AMapLocationClientOption.AMapLocationMode.Device_Sensors //仅设备模式。
            mOption.isMockEnable = true
        } else {
//            mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
            mOption.locationPurpose = AMapLocationClientOption.AMapLocationPurpose.Sport
        }
        mOption.isGpsFirst = true //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.interval = 1000 //可选，设置定位间隔。默认为2秒
        mOption.isNeedAddress = false //可选，设置是否返回逆地理地址信息。默认是true
        mOption.isOnceLocation = false
        return mOption
    }

    private fun sendLocationChanged(location: AMapLocation) {
        val gpsInfo = GpsInfo()
        if (GpsCoordinateUtils.isOutOfChina(location.latitude, location.longitude)) {
            val gps84: DoubleArray = GpsCoordinateUtils.calGCJ02toWGS84(location.latitude, location.longitude)
            gpsInfo.latitude = gps84[0]
            gpsInfo.longitude = gps84[1]
        } else {
            gpsInfo.latitude = location.latitude
            gpsInfo.longitude = location.longitude
        }
        gpsInfo.altitude = location.altitude
        val satellite = location.satellites
        if (satellite in 1..2) {
            gpsInfo.gpsAccuracy = GpsInfo.GPS_LOW
        } else if (satellite in 3..5) {
            gpsInfo.gpsAccuracy = GpsInfo.GPS_MEDIUM
        } else if (satellite >= 6) {
            gpsInfo.gpsAccuracy = GpsInfo.GPS_HIGH
        }
        SpUtils.getSPUtilsInstance().put(
            SpUtils.WEATHER_LONGITUDE_LATITUDE,
            gpsInfo.longitude.toString() + "," + gpsInfo.latitude
        )
        if (locationListener != null) {
            locationListener?.onLocationChanged(gpsInfo)
        }
    }
    //endregion

    class GpsInfo {
        var gpsAccuracy = 0
        var timestamp: Long = 0
        var longitude = 0.0
        var latitude = 0.0
        var altitude = 0.0
        var speed = 0f
        var bearing = 0f
        var horizontal_accuracy = 0f
        var vertical_accuracy = 0f

        companion object {
            const val GPS_LOW = 0x00
            const val GPS_MEDIUM = 0x01
            const val GPS_HIGH = 0x02
            const val GPS_UNKNOWN = 0x0A // no satellite
        }

        override fun toString(): String {
            return "GpsInfo(gpsAccuracy=$gpsAccuracy, timestamp=$timestamp, longitude=$longitude, latitude=$latitude, altitude=$altitude, speed=$speed, bearing=$bearing, horizontal_accuracy=$horizontal_accuracy, vertical_accuracy=$vertical_accuracy)"
        }


    }

    interface LocationListener {
        fun onLocationChanged(gpsInfo: GpsInfo?)
        fun onFailure(msg: String)
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    fun isGPSOpen(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //         通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

//        return true;
    }

    //region 系统定位 LocationManager

    private var locManager: LocationManager? = null
    private var myLocationListener: MyLocationListener? = null

    @SuppressLint("MissingPermission")
    public fun initLocationManager() {
        locManager = BaseApplication.mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var loc = locManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (loc != null) {
            LogUtils.d(TAG, "LocationManager getLastKnownLocation: $loc")
        }
        val providers = locManager?.getProviders(true)
        LogUtils.d(TAG, "LocationManager providers: ${GsonUtils.toJson(providers)}")
        if (providers.isNullOrEmpty()) {
            locationListener?.onFailure(BaseApplication.mContext.getString(R.string.locate_failure_tips))
            return
        }
        if (myLocationListener == null) {
            myLocationListener = MyLocationListener()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locManager?.requestLocationUpdates(
                LocationManager.FUSED_PROVIDER,
                1000, 0F, myLocationListener!!
            )
        } else {
            locManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000, 0F, myLocationListener!!
            )
        }
    }

    private class MyLocationListener : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location == null) {
                locationListener?.onFailure(BaseApplication.mContext.getString(R.string.locate_failure_tips))
                return
            }
            LogUtils.d(TAG, "LocationManager: onLocationChanged${location.toString()}")
            val gpsInfo = GpsInfo()
            gpsInfo.latitude = location.latitude
            gpsInfo.longitude = location.longitude
            gpsInfo.gpsAccuracy = GpsInfo.GPS_UNKNOWN
            SpUtils.getSPUtilsInstance().put(
                SpUtils.WEATHER_LONGITUDE_LATITUDE,
                gpsInfo.longitude.toString() + "," + gpsInfo.latitude
            )
            if (locationListener != null) {
                locationListener?.onLocationChanged(gpsInfo)
            }
        }

        override fun onLocationChanged(locations: MutableList<Location>) {
            super.onLocationChanged(locations)
        }

        override fun onFlushComplete(requestCode: Int) {
            super.onFlushComplete(requestCode)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            super.onStatusChanged(provider, status, extras)
        }

        override fun onProviderEnabled(provider: String) {
            super.onProviderEnabled(provider)
        }

        override fun onProviderDisabled(provider: String) {
            super.onProviderDisabled(provider)
        }
    }
    //endregion

    fun stopGps() {
        LogUtils.d(TAG, "stopGps")
        try {
            //GOOGLE
            if (mGoogleApiClient != null && googleLocationListener != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient!!,
                    googleLocationListener!!
                )
                mGoogleApiClient!!.disconnect()
                mGoogleApiClient = null
            }
            //AMAP
            locationListener = null
            if (null != mlocationClient) {
                mlocationClient!!.disableBackgroundLocation(true)
                mlocationClient!!.onDestroy()
                mlocationClient = null
            }
            //LOCATIONMANAGER
            if (locManager != null && myLocationListener != null) {
                locManager!!.removeUpdates(myLocationListener!!)
                myLocationListener = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
