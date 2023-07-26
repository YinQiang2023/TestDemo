package com.jwei.xzfit.utils.manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.amap.api.maps.model.LatLng
import com.blankj.utilcode.util.*
import com.zhapp.ble.bean.DevSportInfoBean
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.https.params.StravaOauthTokenParam
import com.jwei.xzfit.https.response.StravaTokenResponse
import com.jwei.xzfit.https.strava.StravaRetrofitClient
import com.jwei.xzfit.ui.user.StravaWebLoginActivity
import com.jwei.xzfit.utils.GpsCoordinateUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.SportTypeUtils
import kotlinx.coroutines.*
import me.himanshusoni.gpxparser.GPXWriter
import me.himanshusoni.gpxparser.modal.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Android on 2022/5/16.
 * strava 管理类
 * Strava API 的使用受到每个应用程序的限制，使用 15 分钟和每日请求限制。默认速率限制允许每 15 分钟 100 个请求，每天最多 1,000 个请求
 * https://www.strava.com/settings/api      strava获取client_id client_secret
 * https://developers.strava.com/docs/getting-started/      strava入门指南
 * https://www.topografix.com/gpx.asp   GPX网站
 * gpx解析生成第三方库  https://github.com/himanshu-soni/gpx-parser
 */
object StravaManager {
    const val TAG = "StravaManager"

    private var scope = MainScope()

    //client_id
    private const val STRAVA_CLIENT_ID = "81611"

    //client_secret
    private const val STRAVA_CLIENT_SECRET = "1360833fe72f15b01b451b6591e0645cebe18ba6"

    //strava app 包名
    private const val STRAVA_APP_PACKAGE = "com.strava"

    //strava APP登录url
    private const val APP_LOGIN_URL = "https://www.strava.com/oauth/mobile/authorize"

    //InfoWear scheme uri
    const val INFOWEAR_SCHEME_URL = "infowear://wearheart"

    //web登录url
    private var STRAVA_WBE_LOGIN_URL = "http://www.strava.com/oauth/authorize?client_id=" +
            "$STRAVA_CLIENT_ID&response_type=code&redirect_uri=" +
            "http://localhost/exchange_token&approval_prompt=force&scope=activity:write,activity:read_all"

    //获取toktn 授予类型
    private const val GET_TOKEN_GRANT_TYPE = "authorization_code"

    //刷新token 授予类型
    private const val REFRESH_TOKEN_GRANT_TYPE = "refresh_token"

    //请求授权的页面
    private var mActivity: WeakReference<Activity>? = null

    //登录回调接口
    private var mListener: StravaAuthListener? = null

    //TOKEN
    private var tokenResponse: StravaTokenResponse? = null

    //GPX 缓存路径
    private val GPX_FILE_DIR: String = PathUtils.getAppDataPathExternalFirst() + File.separator + "cache" + File.separator + "gpxCache" + File.separator


    /**
     * Strava授权登录请求码 //TODO 注意尽量不要与其它启动页面的code相同
     * @see com.jwei.xzfit.ui.HomeActivity.ACTIVITY_REQUEST_CODE
     * @see com.jwei.xzfit.utils.manager.GoogleFitManager.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE
     * */
    const val ACTIVITY_WEB_RESULE_REQUEST_CODE = 1001
    const val ACTIVITY_ACT_RESULE_REQUEST_CODE = 1002

    /**
     * 获取登录链接
     */
    fun getWebLoginUrl(): String {
        return STRAVA_WBE_LOGIN_URL
    }

    /**
     * 授权登录
     */
    fun authorizationStrava(activity: Activity?, listener: StravaAuthListener?) {
        mListener = listener
        if (activity == null || activity.isDestroyed || activity.isFinishing) {
            if (mListener != null) mListener?.onFailure("a invalid Context object!")
            return
        }
        mActivity = WeakReference(activity)
        mActivity?.get()?.let { context ->
            val tokenJson = SpUtils.getSPUtilsInstance().getString(SpUtils.STRAVA_TOKEN_KEY, "")
            if (!TextUtils.isEmpty(tokenJson)) {
                //判断是否过期
                tokenResponse = GsonUtils.fromJson(tokenJson, StravaTokenResponse::class.java)
                if (tokenResponse != null) {
                    if (tokenResponse!!.expires_at < System.currentTimeMillis() / 1000) {
                        refreshExpiredToken(tokenResponse!!.refresh_token, mListener)
                    } else {
                        LogUtils.d(TAG, "tokenResponse :${GsonUtils.toJson(tokenResponse)}")
                        mListener?.onAccessSucceeded()
                    }
                    return
                }
            }

            if (AppUtils.isAppInstalled(STRAVA_APP_PACKAGE)) {
                //已安装strava app
                val intentUri = Uri.parse(APP_LOGIN_URL)
                    .buildUpon()
                    .appendQueryParameter("client_id", STRAVA_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", INFOWEAR_SCHEME_URL)
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("approval_prompt", "auto")
                    .appendQueryParameter("scope", "activity:write,read_all")
                    .build()

                val intent = Intent(Intent.ACTION_VIEW, intentUri)
                context.startActivityForResult(intent, ACTIVITY_ACT_RESULE_REQUEST_CODE)
            } else {
                //内置web
                context.startActivityForResult(Intent(context, StravaWebLoginActivity::class.java), ACTIVITY_WEB_RESULE_REQUEST_CODE)
            }
        }
    }

    /**
     * 处理授权页面回调
     * */
    fun resultRequestPermissions(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTIVITY_WEB_RESULE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val code = data.getStringExtra("code")
                if (!TextUtils.isEmpty(code)) {
                    getToken(code!!)
                } else {
                    mListener?.onFailure("strava Login Failure: code == null")
                }
            } else {
                mListener?.onFailure("strava Login Failure: Activity Result err")
            }
        } else if (requestCode == ACTIVITY_ACT_RESULE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (mListener != null) mListener?.onFailure("canceled!")
            }
        }
    }

    /**
     * 获取token
     */
    private fun getToken(code: String) {
        scope.launch {
            try {
                tokenResponse = StravaRetrofitClient.service.oauthtoken(
                    StravaOauthTokenParam(
                        STRAVA_CLIENT_ID,
                        STRAVA_CLIENT_SECRET,
                        code,
                        GET_TOKEN_GRANT_TYPE
                    )
                )
                if (tokenResponse == null) {
                    mListener?.onFailure("strava getToken Failure: tokenResponse")
                    return@launch
                }
                LogUtils.d(TAG, "tokenResponse :${GsonUtils.toJson(tokenResponse)}")
                //tokenResponse :{"access_token":"38ce730f3570fd56f7de92d25398718bd218f296","athlete":{"badge_type_id":0,"bio":null,"city":null,"country":"Taiwan","created_at":"2022-05-04T03:12:51Z","firstname":"DongQI","follower":null,"friend":null,"id":102476333,"lastname":"Deng","premium":false,"profile":"https://d3nn82uaxijpm6.cloudfront.net/assets/avatar/athlete/large-800a7033cc92b2a5548399e26b1ef42414dd1a9cb13b99454222d38d58fd28ef.png","profile_medium":"https://d3nn82uaxijpm6.cloudfront.net/assets/avatar/athlete/medium-bee27e393b8559be0995b6573bcfde897d6af934dac8f392a6229295290e16dd.png","resource_state":2,"sex":"M","state":"Kaohsiung City","summit":false,"updated_at":"2022-05-16T12:51:47Z","username":null,"weight":null},"expires_at":1652792489,"expires_in":21600,"refresh_token":"9bf1cf104d204e79a5717f64411f648ab1fbc2ca","token_type":"Bearer"}
                SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, GsonUtils.toJson(tokenResponse))
                withContext(Dispatchers.Main) {
                    mListener?.onAccessSucceeded()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    mListener?.onFailure("strava getToken Failure")
                }
            }
        }
    }

    /**
     * 刷新过期token
     * 使用过期Token 发出的请求都将收到 401 Unauthorized 响应
     */
    private fun refreshExpiredToken(refresh_token: String, mListener: StravaAuthListener?) {
        scope.launch {
            try {
                tokenResponse = StravaRetrofitClient.service.oauthtoken(
                    StravaOauthTokenParam(
                        STRAVA_CLIENT_ID,
                        STRAVA_CLIENT_SECRET,
                        null,
                        REFRESH_TOKEN_GRANT_TYPE,
                        refresh_token
                    )
                )
                if (tokenResponse == null) {
                    mListener?.onFailure("strava getToken Failure: tokenResponse == null")
                    return@launch
                }
                LogUtils.d(TAG, "tokenResponse :${GsonUtils.toJson(tokenResponse)}")
                //tokenResponse :{"access_token":"38ce730f3570fd56f7de92d25398718bd218f296","athlete":null,"expires_at":1652792489,"expires_in":21541,"refresh_token":"9bf1cf104d204e79a5717f64411f648ab1fbc2ca","token_type":"Bearer"}
                SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, GsonUtils.toJson(tokenResponse))
                withContext(Dispatchers.Main) {
                    mListener?.onAccessSucceeded()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    mListener?.onFailure("strava getToken Failure")
                }
                SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, "")
            }
        }
    }

    /**
     * 关闭授权
     */
    fun deauthorizeStrava(listener: StravaAuthListener?) {
        if (tokenResponse == null) {
            listener?.onFailure("tokenResponse == null")
            return
        }
        scope.launch {
            try {
                val result = StravaRetrofitClient.service.deauthorize(tokenResponse!!.access_token)
                LogUtils.d(TAG, "deauthorize result :$result")
                //{access_token=5431aecd5866befdbdf93cdfdd9a6d4f2f8c91ab}
            } catch (e: Exception) {
                //OkHttp: {"message":"Authorization Error","errors":[{"resource":"Athlete","field":"access_token","code":"invalid"}]}
                LogUtils.d(TAG, "" + e.localizedMessage)//HTTP 401
                e.printStackTrace()
            } finally {
                //取消授权无论成功失败都清除token
                withContext(Dispatchers.Main) {
                    listener?.onAccessSucceeded()
                }
                SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, "")
            }
        }
    }

    /**
     * 获取token
     */
    fun getTokenResponse(): StravaTokenResponse? {
        return tokenResponse
    }

    //region 同步数据
    /**
     * 是否有轨迹的设备运动类型
     */
    private fun isNeedUpload(exerciseType: Int): Boolean {
        return when (exerciseType) {
            1 -> true       //户外跑步    Outdoor running
            2 -> true       //户外健走    Outdoor walking
            4 -> true       //登山    Trekking
            5 -> true       //越野    Trail run
            6 -> true       //户外骑行    Outdoor cycling
            13 -> true      //户外徒步    Outdoor hiking
            14 -> true      //小轮车    BMX
            15 -> true      //打猎    Hunting
            16 -> true      //帆船运动    Sailing
            17 -> true      //滑板    Skateboarding
            18 -> true      //轮滑    Roller skating
            19 -> true      //户外滑冰    Outdoorskating
            20 -> true      //马术    Equestrian
            124 -> true     //山地自行车    Mountain cycling
            201 -> true     //公开水域游泳-H    Open water
            204 -> true     //铁人三项-H    Triathlon
            else -> false
        }
    }

    /**
     * 生成GPX文件
     *
     * @return filePath gpx文件路径
     */
    private fun createGpxFile(type: Int, recordGpsTime: String, mapData: String): String {
        LogUtils.d("mapData :$mapData")
        if (!mapData.contains(";") || !mapData.contains(",") || !recordGpsTime.contains(",")) {
            com.jwei.xzfit.utils.LogUtils.e(TAG, "mapData is invalid", true)
            return ""
        }
        try {
            //region gpx 定位数据格式为84坐标系 国内地图定位点 火星修正为84
            val mLatLngList: MutableList<LatLng> = mutableListOf()
            val mOkLatLngList: MutableList<LatLng> = mutableListOf()
            val oldPointDataArray: Array<String> = mapData.split(";").toTypedArray()
            for (i in oldPointDataArray.indices) {
                val latlng = LatLng(
                    oldPointDataArray[i].split(",").toTypedArray()[1].toDouble(),
                    oldPointDataArray[i].split(",").toTypedArray()[0].toDouble()
                )
                mLatLngList.add(latlng)
            }
            for (location in mLatLngList) {
                //修正 国内 火星转84
                if (!GpsCoordinateUtils.isOutOfChina(location.latitude, location.longitude)) {
                    val newLatlng = GpsCoordinateUtils.calGCJ02toWGS84(location.latitude, location.longitude)
                    mOkLatLngList.add(LatLng(newLatlng[0], newLatlng[1]))
                } else {
                    mOkLatLngList.add(LatLng(location.latitude, location.longitude))
                }
            }
            var newMapData = StringBuilder()
            for (i in 0 until mOkLatLngList.size) {
                val okLatlng = mOkLatLngList.get(i)
                newMapData.append(okLatlng.longitude).append(",").append(okLatlng.latitude)
                if (i != mOkLatLngList.size - 1) {
                    newMapData.append(";")
                }
            }
            LogUtils.d("newMapData :$newMapData")
            //endregion
            val fileName = "sport_${type}_${TimeUtils.date2String(Date(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy_MM_dd_HHmmss"))}.gpx"
            val gpxPath = GPX_FILE_DIR + fileName
            //创建文件输出流
            var gpxOutputStream: FileOutputStream? = null
            if (FileUtils.createFileByDeleteOldFile(gpxPath)) {
                gpxOutputStream = FileOutputStream(gpxPath)
            }
            if (gpxOutputStream == null) {
                com.jwei.xzfit.utils.LogUtils.e(TAG, "gpxOutputStream is null", true)
                return ""
            }
            //定位数据
            val pointDataArray: Array<String> = newMapData.split(";").toTypedArray()
            //时间戳数据
            val pointTimeArray: Array<String> = recordGpsTime.split(",").toTypedArray()
            //数据大小
            val effectiveSize = Math.min(pointDataArray.size, pointTimeArray.size)

            val gpxWriter = GPXWriter()
            val gpx = GPX()
            gpx.creator = "StravaGPX"
            gpx.version = "1.1"
            gpx.metadata = Metadata().apply { this.time = Date() }
            val track = Track()
            track.name = "sport_${type}_${TimeUtils.date2String(Date(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy_MM_dd_HHmmss"))}"
            track.type = "1"
            val trackSegments = arrayListOf<TrackSegment>()
            //填充定位数据
            for (i in 0 until effectiveSize) {
                val trackSegment = TrackSegment()
                val waypoint = Waypoint(
                    pointDataArray[i].split(",").toTypedArray()[1].toDouble(),
                    pointDataArray[i].split(",").toTypedArray()[0].toDouble()
                )
                //waypoint.elevation = 0.0 海拔
                waypoint.time = TimeUtils.millis2Date(pointTimeArray[i].toLong() * 1000L)
                trackSegment.addWaypoint(waypoint)
                trackSegments.add(trackSegment)
            }
            track.trackSegments = trackSegments
            gpx.addTrack(track)
            gpxWriter.writeGPX(gpx, gpxOutputStream)
            gpxOutputStream.close()
            com.jwei.xzfit.utils.LogUtils.e(TAG, "createGpxFile path:$gpxPath")
            return gpxPath
        } catch (e: Exception) {
            com.jwei.xzfit.utils.LogUtils.e(TAG, "createGpxFile err:", true)
            e.printStackTrace()
        }
        return ""
    }

    /**
     * 上传带轨迹的运动数据
     */
    fun uploads(model: DevSportInfoBean?) {
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("检查Strava开关状态").apply {
            log = "Strava 开关状态：${SpUtils.getStravaSwitch()}"
        })
        if (!SpUtils.getStravaSwitch()) return
        if (model == null) return
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("检查是否需要上传的运动").apply {
            log = "是否需要上传：${isNeedUpload(model.recordPointSportType)}"
        })
        if (!isNeedUpload(model.recordPointSportType)) {
            return
        }
        if (TextUtils.isEmpty(model.map_data)) return
        NetworkUtils.isAvailableAsync { isAvailable ->
            if (!isAvailable) {
                com.jwei.xzfit.utils.LogUtils.e(TAG, "uploads sport Failure: network unavailable", true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("分享轨迹到Strava失败").apply {
                    log = "uploads sport Failure: network unavailable"
                }, "1514", true)
                return@isAvailableAsync
            }
            val tokenJson = SpUtils.getSPUtilsInstance().getString(SpUtils.STRAVA_TOKEN_KEY, "")
            if (!TextUtils.isEmpty(tokenJson)) {
                val trackingLog = TrackingLog.getAppTypeTrack("检查是否刷新Strava Token")

                //判断是否过期
                tokenResponse = GsonUtils.fromJson(tokenJson, StravaTokenResponse::class.java)
                if (tokenResponse != null) {
                    if (tokenResponse!!.expires_at < System.currentTimeMillis() / 1000) {
                        trackingLog.log = "token 已经过期，执行刷新"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog)
                        refreshExpiredToken(tokenResponse!!.refresh_token, model)
                    } else {
                        trackingLog.log = "token 未过期，上传数据"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog)
                        pushDataToStrava(model)
                    }
                }
            }
        }
    }

    /**
     * 刷新过期token
     * 使用过期Token 发出的请求都将收到 401 Unauthorized 响应
     */
    private fun refreshExpiredToken(refresh_token: String, model: DevSportInfoBean) {
        scope.launch {
            try {
                tokenResponse = StravaRetrofitClient.service.oauthtoken(
                    StravaOauthTokenParam(
                        STRAVA_CLIENT_ID,
                        STRAVA_CLIENT_SECRET,
                        null,
                        REFRESH_TOKEN_GRANT_TYPE,
                        refresh_token
                    )
                )
                if (tokenResponse == null) {
                    com.jwei.xzfit.utils.LogUtils.d(TAG, "strava getToken Failure: tokenResponse == null", true)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("Strava token过期并且刷新失败").apply {
                        log = "strava getToken Failure: tokenResponse == null"
                    }, "1510", true)
                    return@launch
                }
                LogUtils.d(TAG, "tokenResponse :${GsonUtils.toJson(tokenResponse)}")
                //tokenResponse :{"access_token":"38ce730f3570fd56f7de92d25398718bd218f296","athlete":null,"expires_at":1652792489,"expires_in":21541,"refresh_token":"9bf1cf104d204e79a5717f64411f648ab1fbc2ca","token_type":"Bearer"}
                SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, GsonUtils.toJson(tokenResponse))
                // 发送数据
                pushDataToStrava(model)
            } catch (e: Exception) {
                e.printStackTrace()
                com.jwei.xzfit.utils.LogUtils.d(TAG, "strava getToken Failure:$e", true)
                //SpUtils.getSPUtilsInstance().put(SpUtils.STRAVA_TOKEN_KEY, "")
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("Strava token过期并且刷新失败").apply {
                    log = "strava getToken Failure:$e"
                }, "1510", true)
            }
        }
    }

    /**
     * 发送带轨迹的运动数据至strava
     */
    private fun pushDataToStrava(model: DevSportInfoBean) {
        com.jwei.xzfit.utils.LogUtils.d(TAG, "start upload sportInfo to Strava.")
        scope.launch {
            try {
                val trackingLog = TrackingLog.getSerTypeTrack("分享轨迹数据到Strava", "运动上传Strava", "api/v3/uploads")
                val path = createGpxFile(model.recordPointSportType, model.recordGpsTime, model.map_data)
                if (TextUtils.isEmpty(path)) {
                    com.jwei.xzfit.utils.LogUtils.e(TAG, "uploads sport Failure: map data error", true)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog.apply {
                        log = "uploads sport Failure: map data error"
                    }, "1514", true)
                    return@launch
                }
                val file: File? = FileUtils.getFileByPath(path)
                var fileBody: RequestBody? = file?.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                if (fileBody == null) {
                    com.jwei.xzfit.utils.LogUtils.e(TAG, "uploads sport Failure:gpx file asRequestBody null", true)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog.apply {
                        log = "uploads sport Failure:gpx file asRequestBody null"
                    }, "1514", true)
                    return@launch
                }
                val result = StravaRetrofitClient.service.uploads(
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file!!.name, fileBody)
                        //he format of the uploaded file. May take one of the following values: fit, fit.gz, tcx, tcx.gz, gpx, gpx.gz
                        .addFormDataPart("data_type", "gpx")
                        .addFormDataPart("name", SportTypeUtils.getSportTypeName(2, model.recordPointSportType.toString()))
                        //AlpineSki, BackcountrySki, Canoeing, Crossfit, EBikeRide, Elliptical, Golf, Handcycle, Hike, IceSkate, InlineSkate,
                        //Kayaking, Kitesurf, NordicSki, Ride, RockClimbing, RollerSki, Rowing, Run, Sail, Skateboard, Snowboard, Snowshoe,
                        //Soccer, StairStepper, StandUpPaddling, Surfing, Swim, Velomobile, VirtualRide, VirtualRun, Walk, WeightTraining,
                        //Wheelchair, Windsurf, Workout, Yoga
                        .addFormDataPart("activity_type", SportTypeUtils.getStravaActivityType(2, model.recordPointSportType.toString()))
                        //.addFormDataPart("description", "Form the InfoWear")
                        //trainer           Whether the resulting activity should be marked as having been performed on a trainer.
                        //commute           Whether the resulting activity should be tagged as a commute.
                        //external_id       The desired external identifier of the resulting activity.
                        .build()
                )
                trackingLog.endTime = TrackingLog.getNowString()
                trackingLog.serResJson = com.jwei.xzfit.utils.AppUtils.toSimpleJsonString(result ?: "{\"data:\":null}")
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog)

                com.jwei.xzfit.utils.LogUtils.d(TAG, "uploads sport Succeeded:$result")
            } catch (e: Exception) {
                com.jwei.xzfit.utils.LogUtils.e(TAG, "uploads sport Failure!$e", true)
                e.printStackTrace()
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getAppTypeTrack("分享轨迹到Strava失败").apply {
                    log = "uploads sport Failure!$e"
                }, "1514", true)
            }
        }
    }


    //endregion

    interface StravaAuthListener {
        fun onAccessSucceeded()
        fun onFailure(msg: String)
    }
}