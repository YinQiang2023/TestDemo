package com.smartwear.publicwatch.ui.device.weather.utils

import android.text.TextUtils
import android.util.Log
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.Gson
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThreadPoolService
import com.zhapp.ble.bean.WeatherDayBean
import com.zhapp.ble.bean.WeatherPerHourBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.https.ApiService
import com.smartwear.publicwatch.https.NoAuthRetrofitClient
import com.smartwear.publicwatch.ui.device.bean.DeviceSettingBean
import com.smartwear.publicwatch.ui.device.weather.bean.*
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import java.util.*


const val URL_WEATHER_CORE = "http://api.openweathermap.org/geo/1.0/"

//天气id https://openweathermap.org/weather-conditions
object WeatherManagerUtils {
    private val TAG = WeatherManagerUtils::class.java.simpleName

    interface GetOpenWeatherListener {
        fun onSuccess(cityName: String)
        fun onFail(msg: String)
    }

    private const val appid = "55927c151597caba67e863d486282616"
    private const val lang = "en"
    private const val units = "metric"
    private const val limit = "20"

    //api.openweathermap.org/geo/1.0/reverse?lat=22.631734&lon=113.833296&appid=55927c151597caba67e863d486282616&limit=20
    private const val CURRENT_WEATHER_URL_ZH = "http://api.openweathermap.org/geo/1.0/reverse"

    //api.openweathermap.org/data/2.5/weather?lat=22.547&lon=114.085947&appid=55927c151597caba67e863d486282616&lang=en&units=metric
    private const val CURRENT_WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather"

    public var currentWeather: CurrentWeather? = null

    private var lat = 0.0
    private var lon = 0.0

    private var getOpenWeatherListener: GetOpenWeatherListener? = null
    private val compositeDisposable = CompositeDisposable()


    //获取当天，天气的数据
    fun getCurrentWeather(
        needUpdataCityInfo: Boolean,
        needReturnCityInfo: Boolean,
        oldLat: Double,
        oldLon: Double,
        getOpenWeatherListener: GetOpenWeatherListener?
    ) {
        LogUtils.i(TAG, "getCurrentWeather() getCityOnly = $needUpdataCityInfo lat = $oldLat lon = $oldLon")
        var lat: Double = 0.0
        var lon: Double = 0.0
        lat = oldLat
        lon = oldLon
        WeatherManagerUtils.getOpenWeatherListener = getOpenWeatherListener
        WeatherManagerUtils.lat = lat
        WeatherManagerUtils.lon = lon
        LogUtils.i(TAG, "getCurrentWeather = lat = $lat lon = $lon")
        //国内请求
        val url_zh = "$CURRENT_WEATHER_URL_ZH?lat=$lat&lon=$lon&appid=$appid&limit=$limit"
        val url = "$CURRENT_WEATHER_URL?lat=$lat&lon=$lon&appid=$appid&lang=$lang&units=$units"

        LogUtils.i(TAG, "getCurrentWeather = url_zh = $url_zh")
        LogUtils.i(TAG, "getCurrentWeather = url = $url")
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getAppTypeTrack("是否需要获取城市信息").apply {
            log = "needUpdataCityInfo = $needUpdataCityInfo"
        })
        if (needUpdataCityInfo) {
            val getCityTrackingLog = TrackingLog.getSerTypeTrack("获取城市信息", "获取城市信息", URL_WEATHER_CORE)
            getCityTrackingLog.serReqJson = "appid=$appid,lat=$lat,lon=$lon,limit=5"
            val flowable = NoAuthRetrofitClient.getService(ApiService::class.java, URL_WEATHER_CORE)
                .searchCityByLocation(appid, lat.toString(), lon.toString(), "5")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
            val dispose = flowable.subscribeBy(
                onNext = {
                    LogUtils.w(TAG, "search city by location entity is $it")
                    getCityTrackingLog.endTime = TrackingLog.getNowString()
                    getCityTrackingLog.serResJson = AppUtils.toSimpleJsonString(it)
                    getCityTrackingLog.serResult = "成功"
                    if (it.isNotEmpty()) {
                        val entity = it[0]

                        SpUtils.getSPUtilsInstance().put(
                            SpUtils.WEATHER_LONGITUDE_LATITUDE,
                            "${entity.lon},${entity.lat}"
                        )
                        var currCityName = ""
                        val name: String = entity.name
                        val country: String = entity.country
                        val local_names_zh: String = entity.local_names.zh

                        LogUtils.i(TAG, "获取城市名称 国内 name = $name")
                        LogUtils.i(TAG, "获取城市名称 国内 country = $country")
                        LogUtils.i(TAG, "获取城市名称 国内 local_names_zh = $local_names_zh")

                        currCityName = if (AppUtils.isZh(BaseApplication.mContext) && TextUtils.equals(country, "CN")) {
                            if (!local_names_zh.isNullOrEmpty()) {
                                local_names_zh
                            } else {
                                name
                            }
                        } else {
                            name
                        }
                        LogUtils.i(TAG, "获取城市名称 国内 currCityName = $currCityName")
                        getCityTrackingLog.log = "获取城市名称 国内 currCityName = $currCityName"
                        SpUtils.getSPUtilsInstance().put(
                            SpUtils.WEATHER_CITY_NAME,
                            currCityName
                        )
                        if (needReturnCityInfo) {
                            getOpenWeatherListener?.onSuccess(currCityName)
                        }
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, getCityTrackingLog)
                        getEveryDayWeather()
                    } else {
                        if (ControlBleTools.getInstance().isConnect) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, getCityTrackingLog.apply {
                                log += "\nsearch city by location entity is null\n获取城市信息失败/超时"
                            }, "1615", true)
                        }
                        getOpenWeatherListener?.onFail("search city by location entity is null")
                    }
                },
                onComplete = {
                    LogUtils.w(TAG, "search city by location is complete")
                    compositeDisposable.clear()
                },
                onError = {
                    getCityTrackingLog.endTime = TrackingLog.getNowString()
                    getCityTrackingLog.serResult = "失败"
                    if (ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, getCityTrackingLog.apply {
                            log += "\nsearch city by location error and message is ${it.message}\n获取城市信息失败/超时"
                        }, "1615", true)
                    }
                    LogUtils.e(TAG, "search city by location error and message is ${it.message}", true)
                    getOpenWeatherListener?.onFail("search city by location error and message is ${it.message}")
                    compositeDisposable.clear()
                }
            )
            compositeDisposable.add(dispose)
        } else {
            getEveryDayWeather()
        }



        if (true) return
        //中文简体
        if (LocaleUtils.getLocalLanguage()) {
            //走openWeather服务器
            ThreadPoolService.getInstance().post {
                MyOkHttpArrayClient.getInstance().asynGetCall(
                    DisposeDataHandle(
                        object : DisposeDataListener {
                            override fun onSuccess(responseObj: Any?) {
                                val result = responseObj.toString()
                                Log.i(TAG, "获取城市名称 国内 result = $result")

                                val dataList = JSONArray(result)
                                if (dataList.length() > 0) {
                                    var currCityName = ""
                                    try {
                                        val dataObject = dataList.get(0)
                                        val reverseAddress: ReverseAddress? = Gson().fromJson(
                                            dataObject.toString(),
                                            ReverseAddress::class.java
                                        )
                                        Log.i(TAG, "获取城市名称 国内 dataObject = $dataObject")
                                        val name = reverseAddress?.name
                                        val local_names_zh = reverseAddress?.local_names?.zh
                                        Log.i(TAG, "获取城市名称 国内 reverseAddress.name = $name")
                                        Log.i(
                                            TAG,
                                            "获取城市名称 国内 reverseAddress.local_names_zh = $local_names_zh"
                                        )
                                        if (!local_names_zh.equals("")) {
                                            getOpenWeatherListener?.onSuccess(local_names_zh.toString())
                                            currCityName = local_names_zh.toString()
                                        } else {
                                            currCityName = name.toString()
                                        }
                                    } catch (e: Exception) {
                                    }

                                    LogUtils.i(
                                        TAG,
                                        "getCurrentWeather currCityName = $currCityName"
                                    )

                                    if (needReturnCityInfo) {
                                        SpUtils.getSPUtilsInstance().put(
                                            SpUtils.WEATHER_CITY_NAME,
                                            currCityName
                                        )
                                    }

                                    if (needUpdataCityInfo) {
                                        getOpenWeatherListener?.onSuccess(currCityName)
                                        return
                                    }

                                    // get 5 days weather data
                                    getEveryDayWeather()

                                }
                            }

                            override fun onFailure(reasonObj: Any?) {
                                LogUtils.e(TAG, "getCurrentWeather  onFailure:$reasonObj", true)
                                getOpenWeatherListener?.onFail("getCurrentWeather  onFailure:$reasonObj")
                            }
                        }), url_zh
                )
            }
        }
        //非中文简体
        else {
            ThreadPoolService.getInstance().post {
                MyOkHttpClient.getInstance().asynGetCall(
                    DisposeDataHandle(
                        object : DisposeDataListener {
                            override fun onSuccess(responseObj: Any?) {
                                val result = responseObj.toString()
                                Log.i(TAG, "获取城市名称 国外 result = $result")
                                currentWeather =
                                    Gson().fromJson(result, CurrentWeather::class.java)
                                LogUtils.i(
                                    TAG,
                                    "getCurrentWeather  currentWeather = $currentWeather"
                                )

                                if (needReturnCityInfo) {
                                    SpUtils.getSPUtilsInstance().put(
                                        SpUtils.WEATHER_CITY_NAME,
                                        currentWeather?.name
                                    )
                                }

                                if (needUpdataCityInfo) {
                                    getOpenWeatherListener?.onSuccess(currentWeather?.name.toString())
                                    return
                                }
                                // get 5 days weather data
                                getEveryDayWeather()
                            }

                            override fun onFailure(reasonObj: Any?) {
                                LogUtils.e(TAG, "getCurrentWeather  onFailure:$reasonObj")
                                getOpenWeatherListener?.onFail("getCurrentWeather  onFailure:$reasonObj")
                            }
                        }), url
                )
            }
        }
    }

    private const val getEveryDayWeatherUrl =
        "http://api.openweathermap.org/data/2.5/forecast/daily?"
//    var weatherDays: WeatherDays? = null

    //获取未来天气，5天
    private fun getEveryDayWeather() {

        val trackingLog = TrackingLog.getSerTypeTrack("获取未来4天天气", "获取未来4天天气", getEveryDayWeatherUrl)
        trackingLog.serReqJson = "lat=$lat&lon=$lon&cnt=4&appid=$appid&lang=$lang&units=$units"
        val url: String =
            getEveryDayWeatherUrl + "lat=" + lat + "&lon=" + lon + "&cnt=" + 4 + "&appid=" + appid + "&lang=" + lang + "&units=" + units
        LogUtils.i(TAG, "getEveryDayWeather $url")
        ThreadPoolService.getInstance().post {
            MyOkHttpClient.getInstance().asynGetCall(
                DisposeDataHandle(
                    object : DisposeDataListener {
                        override fun onSuccess(responseObj: Any?) {
                            com.blankj.utilcode.util.LogUtils.d("EveryDayWeather jsonString$responseObj")
                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.serResJson = responseObj.toString()
                            trackingLog.serResult = "成功"
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog)

                            val jsonString = JSON.toJSONString(
                                Gson().fromJson(
                                    responseObj.toString(),
                                    WeatherDays::class.java
                                )
                            )
                            SpUtils.setValue(SpUtils.WEATHER_DAYS_INFO, jsonString)
                            LogUtils.i(TAG, "getEveryDayWeather  jsonString = $jsonString")
                            // get 4 days 96 hours data
                            getWeatherPerHour()
                        }

                        override fun onFailure(reasonObj: Any?) {
                            LogUtils.e(TAG, "getEveryDayWeather  onFailure:$reasonObj", true)
                            getOpenWeatherListener?.onFail("getEveryDayWeather  onFailure:$reasonObj")
                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.log = "getEveryDayWeather  onFailure:$reasonObj"
                            trackingLog.serResult = "失败"
                            if (ControlBleTools.getInstance().isConnect) {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog.apply {
                                    log += "\n获取天气信息失败"
                                }, "1613", true)
                            }
                        }
                    }), url
            )
        }
    }

    private const val getWeatherPerHourUrl =
        "http://pro.openweathermap.org/data/2.5/forecast/hourly?"
//    var weatherPerHour: WeatherPerHour? = null

    //获取4天96小时的，小时详细数据
    fun getWeatherPerHour() {
        val trackingLog = TrackingLog.getSerTypeTrack("获取96小时天气", "获取96小时天气", getWeatherPerHourUrl)
        trackingLog.serReqJson = "lat=$lat&lon=$lon&appid=$appid&lang=$lang&units=$units"
        val url =
            getWeatherPerHourUrl + "lat=" + lat + "&lon=" + lon + "&appid=" + appid + "&lang=" + lang + "&units=" + units
        LogUtils.i(TAG, "getWeatherPerHour = lat = $lat lon = $lon")
        ThreadPoolService.getInstance().post {
            MyOkHttpClient.getInstance().asynGetCall(
                DisposeDataHandle(
                    object : DisposeDataListener {
                        override fun onSuccess(responseObj: Any?) {
                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.serResJson = responseObj.toString()
                            trackingLog.serResult = "成功"
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog)

                            val jsonString = JSON.toJSONString(
                                Gson().fromJson(
                                    responseObj.toString(),
                                    WeatherPerHour::class.java
                                )
                            )
                            SpUtils.setValue(SpUtils.WEATHER_PER_HOUR_INFO, jsonString)
                            LogUtils.i(TAG, "getWeatherPerHour  jsonString = $jsonString")
                            getWeatherAQI()
                        }

                        override fun onFailure(reasonObj: Any?) {
                            LogUtils.e(TAG, "getWeatherPerHour  onFailure:$reasonObj", true)
                            getOpenWeatherListener?.onFail("getWeatherPerHour  onFailure:$reasonObj")

                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.log = "getWeatherPerHour  onFailure:$reasonObj"
                            trackingLog.serResult = "失败"
                            if (ControlBleTools.getInstance().isConnect) {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog.apply {
                                    log += "\n获取天气信息失败"
                                }, "1613", true)
                            }
                        }
                    }), url
            )
        }
    }

    private const val getWeatherAQIUrl =
        "http://api.openweathermap.org/data/2.5/air_pollution/history?"

//    var weatherAQI: WeatherAQI? = null

    fun getWeatherAQI() {

        val start = System.currentTimeMillis() / 1000
        val end = start + 96 * 3600

        val trackingLog = TrackingLog.getSerTypeTrack("获取空气质量", "获取空气质量", getWeatherAQIUrl)
        trackingLog.serReqJson = "start = $start end = $end lon = $lon lat = $lat"

        LogUtils.i(TAG, "getWeatherAQI start = $start end = $end lon = $lon lat = $lat")
        val url =
            getWeatherAQIUrl + "lat=" + lat + "&lon=" + lon + "&appid=" + appid + "&start=" + start + "&end=" + end

        ThreadPoolService.getInstance().post {
            MyOkHttpClient.getInstance().asynGetCall(
                DisposeDataHandle(
                    object : DisposeDataListener {
                        override fun onSuccess(responseObj: Any?) {

                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.serResJson = responseObj.toString()
                            trackingLog.serResult = "成功"
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog)

                            val weatherAQI =
                                Gson().fromJson(responseObj.toString(), WeatherAQI::class.java)
                            SpUtils.setValue(
                                SpUtils.WEATHER_AQI_INFO,
                                JSON.toJSONString(weatherAQI)
                            )
                            if (weatherAQI.list != null) {
                                LogUtils.i(
                                    TAG,
                                    "getWeatherAQI onSuccess size = ${weatherAQI.list.size} weatherAQI = $weatherAQI"
                                )
                            }
                            getOpenWeatherListener?.onSuccess("")
                        }

                        override fun onFailure(reasonObj: Any?) {
                            LogUtils.e(TAG, "getWeatherAQI  onFailure:$reasonObj", true)
                            //getOpenWeatherListener?.onFail()
                            SpUtils.setValue(SpUtils.WEATHER_AQI_INFO, "")
                            getOpenWeatherListener?.onSuccess("")

                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.log = "getWeatherPerHour  onFailure:$reasonObj"
                            trackingLog.serResult = "失败"
                            if (ControlBleTools.getInstance().isConnect) {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog.apply {
                                    log += "\n获取天气信息失败"
                                }, "1613", true)
                            }
                        }
                    }), url
            )
        }
    }


    private const val getWeatherFindBySearchUrl = "http://api.openweathermap.org/data/2.5/find?"
    var weatherFind: WeatherFind? = null

    fun getWeatherFindBySearch(city: String, getOpenWeatherListener: GetOpenWeatherListener) {
        val url =
            getWeatherFindBySearchUrl + "q=" + city + "&appid=" + appid + "&lang=" + lang + "&units=" + units
        MyOkHttpClient.getInstance().asynGetCall(DisposeDataHandle(object : DisposeDataListener {
            override fun onSuccess(responseObj: Any) {
                LogUtils.i(TAG, "getWeatherFindBySearch onSuccess = $responseObj")
                val gson = Gson()
                weatherFind = gson.fromJson(responseObj.toString(), WeatherFind::class.java)
                getOpenWeatherListener.onSuccess("")
            }

            override fun onFailure(reasonObj: Any) {
                LogUtils.e(TAG, "getWeatherFindBySearch onFailure = $reasonObj", true)
                getOpenWeatherListener.onFail("getWeatherFindBySearch onFailure = $reasonObj")
            }
        }), url)
    }

    fun searchCity(city: String): Flowable<List<SearchCityEntity>> {
        return NoAuthRetrofitClient.getService(ApiService::class.java, URL_WEATHER_CORE)
            .searchCityByName(appid, city, "5")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun syncWeather(): Boolean {
        val isOk = (!TextStringUtils.isNull(
            SpUtils.getValue(
                SpUtils.WEATHER_SWITCH,
                ""
            )
        ) && SpUtils.getValue(SpUtils.WEATHER_SWITCH, "false").trim().toBoolean());
        LogUtils.i(TAG, "syncWeather switch isOk = $isOk")
        if (isOk) {
            LogUtils.i(TAG, "WEATHER_SYNC_TIME  ${SpUtils.getValue(SpUtils.WEATHER_SYNC_TIME, "0")}")
            if (SpUtils.getValue(SpUtils.WEATHER_SYNC_TIME, "0") == "0") {
                getWeatherOnline()
            } else {
                val time = SpUtils.getValue(SpUtils.WEATHER_SYNC_TIME, "0")
                if (Math.abs(System.currentTimeMillis() - time.trim().toLong()) > (15 * 60 * 1000L)) {
                    getWeatherOnline()
                } else {
                    sendWeatherDay()
                }
            }
        } else {
            return false
        }
        return false
    }

    private fun getWeatherOnline() {
        if (!TextStringUtils.isNull(SpUtils.getValue(SpUtils.WEATHER_SWITCH, ""))
            && SpUtils.getValue(SpUtils.WEATHER_SWITCH, "false").trim().toBoolean()
        ) {
            SpUtils.setValue(SpUtils.WEATHER_SYNC_TIME, "${System.currentTimeMillis()}")
            val gpsString = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")
            if (!TextUtils.isEmpty(gpsString) && gpsString.contains(",")) {
                val gps = gpsString.trim().split(",")
                if (gps.isNotEmpty() && gps.size == 2) {
                    LogUtils.e(TAG, "getWeatherOnline")
                    getCurrentWeather(false, false, gps[1].toDouble(), gps[0].toDouble(),
                        object : GetOpenWeatherListener {
                            override fun onSuccess(cityName: String) {
                                LogUtils.i(TAG, "timerRunnable getCurrentWeather onSuccess")
                                if (ControlBleTools.getInstance().isConnect) {
                                    sendWeatherDay()
                                }
                            }

                            override fun onFail(msg: String) {
                                LogUtils.e(TAG, "timerRunnable getCurrentWeather onFail:$msg")
                                ErrorUtils.onLogResult("getCurrentWeather onFail:$msg")
                                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                            }
                        })
                    return
                }
            }
            LogUtils.e(TAG, "getWeatherOnline Failed, no latitude and longitude data exists", true)
        }
    }

    fun sendWeatherDay() {
        AppUtils.tryBlock {
            if (TextStringUtils.isNull(SpUtils.getValue(SpUtils.WEATHER_DAYS_INFO, ""))) return@tryBlock
            val weatherDays = JSON.parseObject(
                SpUtils.getValue(SpUtils.WEATHER_DAYS_INFO, ""),
                WeatherDays::class.java
            )
            val weatherAQI: WeatherAQI? =
                JSON.parseObject(SpUtils.getValue(SpUtils.WEATHER_AQI_INFO, ""), WeatherAQI::class.java)

            val bean = WeatherDayBean()
            val ca = Calendar.getInstance()
            ca.timeInMillis = weatherDays?.list?.get(0)?.dt?.trim()?.toLong()!! * 1000
            bean.year = ca.get(Calendar.YEAR)
            bean.month = ca.get(Calendar.MONTH) + 1
            bean.day = ca.get(Calendar.DAY_OF_MONTH)
            bean.hour = ca.get(Calendar.HOUR_OF_DAY)
            bean.minute = ca.get(Calendar.MINUTE)
            bean.second = ca.get(Calendar.SECOND)
            bean.cityName = if (!TextUtils.isEmpty(SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, ""))) {
                SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
            } else {
                weatherDays.city?.name
            }
            bean.locationName = weatherDays.city?.country
            for (i in weatherDays.list!!.indices) {
                val listBean = WeatherDayBean.Data()
                try {
                    if (weatherAQI == null || weatherAQI.list.isNullOrEmpty()) {
                        listBean.aqi = 1
                    } else {
                        if (i == 0) {
                            listBean.aqi = weatherAQI.list[0].main.aqi.toInt()
                        } else {
                            if (i * 24 - 1 < weatherAQI.list.size) {
                                listBean.aqi = weatherAQI.list[i * 24 - 1].main.aqi.toInt()
                            } else {
                                listBean.aqi = 1
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                listBean.now_temperature = weatherDays.list!![i].temp.day.toFloat().toInt()
                listBean.low_temperature = weatherDays.list!![i].temp.min.toFloat().toInt()
                listBean.high_temperature = weatherDays.list!![i].temp.max.toFloat().toInt()
                listBean.humidity = weatherDays.list!![i].humidity.toFloat().toInt()
                listBean.weather_id = weatherDays.list!![i].weather[0].id.toInt()
                listBean.weather_name = weatherDays.list!![i].weather[0].main
                listBean.Wind_speed = weatherDays.list!![i].speed.toFloat().toInt()
                listBean.wind_info = weatherDays.list!![i].deg.toFloat().toInt()
                listBean.Probability_of_rainfall = (weatherDays.list!![i].pop.toFloat() * 100).toInt()
                listBean.sun_rise = weatherDays.list!![i].sunrise
                listBean.sun_set = weatherDays.list!![i].sunset

                bean.list.add(listBean)
            }
            if (ControlBleTools.getInstance().isConnect) {
                val trackingLog = TrackingLog.getDevTyepTrack("发送4天天气数据", "发送天气信息", "FORECAST_WEATHER")
                trackingLog.log = "data.length = ${GsonUtils.toJson(bean).length}"
                ControlBleTools.getInstance().sendWeatherDailyForecast(bean, object : ParsingStateManager.SendCmdStateListener() {
                    override fun onState(state: SendCmdState) {
                        trackingLog.endTime = TrackingLog.getNowString()
                        trackingLog.devResult = "state : $state"

                        when (state) {
                            SendCmdState.SUCCEED -> {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog)
                                EventBus.getDefault()
                                    .post(EventMessage(EventAction.ACTION_SYNC_WEATHER_INFO, true))
                            }
                            else -> {
                                ErrorUtils.onLogResult("sendWeatherDailyForecast send failed:$state")
                                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                                EventBus.getDefault()
                                    .post(EventMessage(EventAction.ACTION_SYNC_WEATHER_INFO, false))
                                if (ControlBleTools.getInstance().isConnect) {
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog.apply {
                                        log += "\n同步天气信息至设备失败"
                                    }, "1614", true)
                                }
                            }
                        }
                    }

                })
            }
            LogUtils.i(TAG, "sendWeatherDay $bean")
            sendWeatherHour()

            //如果后台支持气压（海拔）数据，则发送海拔数据
            val deviceSettingBean = JSON.parseObject(
                SpUtils.getValue(
                    SpUtils.DEVICE_SETTING,
                    ""
                ), DeviceSettingBean::class.java
            )
            if (deviceSettingBean != null) {
                if (deviceSettingBean.functionRelated.altitude) {
                    sendWeatherPressure()
                } else {
                    if (!isSaveNoSupportAltitudeLog) {
                        //本次进程生命避免重复存入日志
                        isSaveNoSupportAltitudeLog = true
                        LogUtils.d(TAG, "设备不支持发送气压数据", true)
                    }
                }
            }
        }
    }

    var isSaveNoSupportAltitudeLog = false

    fun sendWeatherHour() {
        if (TextStringUtils.isNull(SpUtils.getValue(SpUtils.WEATHER_PER_HOUR_INFO, ""))) return
        val weatherPerHour = JSON.parseObject(
            SpUtils.getValue(SpUtils.WEATHER_PER_HOUR_INFO, ""),
            WeatherPerHour::class.java
        )

        val bean = WeatherPerHourBean()
        val ca = Calendar.getInstance()
        ca.timeInMillis = weatherPerHour?.list?.get(0)?.dt?.trim()?.toLong()!! * 1000
        bean.year = ca.get(Calendar.YEAR)
        bean.month = ca.get(Calendar.MONTH) + 1
        bean.day = ca.get(Calendar.DAY_OF_MONTH)
        bean.hour = ca.get(Calendar.HOUR_OF_DAY)
        bean.minute = ca.get(Calendar.MINUTE)
        bean.second = ca.get(Calendar.SECOND)
//        bean.cityName = weatherPerHour.city?.name
        bean.cityName = if (!TextUtils.isEmpty(SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, ""))) {
            SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
        } else {
            weatherPerHour.city?.name
        }
        bean.locationName = weatherPerHour.city?.country

        for (i in weatherPerHour.list!!.indices) {
            val listBean = WeatherPerHourBean.Data()

            listBean.now_temperature = weatherPerHour.list!![i].main.temp.toFloat().toInt()
//            listBean.low_temperature = weatherPerHour.list!![i].main.temp_min.toFloat().toInt()
//            listBean.high_temperature = weatherPerHour.list!![i].main.temp_max.toFloat().toInt()
            listBean.humidity = weatherPerHour.list!![i].main.humidity.toFloat().toInt()
            listBean.weather_id = weatherPerHour.list!![i].weather[0].id.toInt()
//            listBean.weather_name = weatherPerHour.list!![i].weather[0].main
            listBean.Wind_speed = weatherPerHour.list!![i].wind.speed.toFloat().toInt()
            listBean.wind_info = weatherPerHour.list!![i].wind.deg.toFloat().toInt()
            listBean.Probability_of_rainfall =
                (weatherPerHour.list!![i].pop.toFloat() * 100).toInt()

            bean.list.add(listBean)
        }

        LogUtils.i(TAG, "sendWeatherHour $bean")
        if (ControlBleTools.getInstance().isConnect) {
            val trackingLog = TrackingLog.getDevTyepTrack("发送96小时天气数据", "发送96小时天气数据", "HOURLY_WEATHER")
            trackingLog.log = "data.length = ${GsonUtils.toJson(bean).length}"
            ControlBleTools.getInstance()
                .sendWeatherPreHour(bean, object : ParsingStateManager.SendCmdStateListener() {
                    override fun onState(state: SendCmdState) {
                        trackingLog.endTime = TrackingLog.getNowString()
                        trackingLog.devResult = "state : $state"
                        when (state) {
                            SendCmdState.SUCCEED -> {
                                EventBus.getDefault()
                                    .post(EventMessage(EventAction.ACTION_SYNC_WEATHER_INFO, true))
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog)
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, TrackingLog.getEndTypeTrack("天气"), isEnd = true)
                            }
                            else -> {
                                ErrorUtils.onLogResult("sync WeatherHour send failed:$state")
                                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                                EventBus.getDefault()
                                    .post(EventMessage(EventAction.ACTION_SYNC_WEATHER_INFO, false))
                                if (ControlBleTools.getInstance().isConnect) {
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_WEATHER, trackingLog.apply {
                                        log += "\n同步天气信息至设备失败"
                                    }, "1614", true)
                                }
                            }
                        }
                    }
                })
        }
    }

    private fun sendWeatherPressure() {
        if (TextStringUtils.isNull(SpUtils.getValue(SpUtils.WEATHER_DAYS_INFO, ""))) return
        AppUtils.tryBlock {
            val weatherDays = JSON.parseObject(
                SpUtils.getValue(SpUtils.WEATHER_DAYS_INFO, ""),
                WeatherDays::class.java
            )
            if (!weatherDays?.list.isNullOrEmpty() && !weatherDays?.list?.get(0)?.pressure.isNullOrEmpty()) {
                if (ControlBleTools.getInstance().isConnect) {
                    ControlBleTools.getInstance().sendPressureByWeather(weatherDays?.list?.get(0)?.pressure!!.toInt() * 1.0f, null)
                }
            }
        }
    }
}