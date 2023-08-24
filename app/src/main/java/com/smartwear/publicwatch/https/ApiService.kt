package com.smartwear.publicwatch.https

import com.smartwear.publicwatch.db.model.sport.SportModleInfo
import com.smartwear.publicwatch.https.response.*
import com.smartwear.publicwatch.ui.device.weather.bean.SearchCityEntity
import io.reactivex.Flowable
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by android
 * on 2021/7/14
 */
interface ApiService {

    @POST("xfit/user/register")
    suspend fun register(@Body requestBody: RequestBody): Response<RegisterResponse>

    @POST("xfit/user/queryByLoginName")
    suspend fun queryByLoginName(@Body requestBody: RequestBody): Response<QureyLoginAccountResponse>

    @POST("xfit/user/login")
    suspend fun login(@Body requestBody: RequestBody): Response<LoginResponse>

    @POST("xfit/thirdParty/ssoLogin")
    suspend fun ssoLogin(@Body requestBody: RequestBody): Response<LoginResponse>

    @POST("xfit/user/forgetPwd")
    suspend fun findPassWord(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("xfit/auth/getCode")
    suspend fun getCode(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("zfit/device/bind")
    suspend fun bindDevice(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("zfit/product/info")
    suspend fun productInfo(@Body requestBody: RequestBody): Response<ProductInfoResponse>

    @POST("zfit/product/versionInfo")
    suspend fun versionInfo(@Body requestBody: RequestBody): Response<VersionInfoResponse>

    @POST("zfit/device/unBind")
    suspend fun unbindDevice(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("zfit/device/bindList")
    suspend fun getBindList(@Body requestBody: RequestBody): Response<BindListResponse>

    //e)获取产品设备号列表
    @POST("zfit/product/list")
    suspend fun getProductList(): Response<ProductListResponse>

    //设备启用
    @POST("zfit/device/enable")
    suspend fun enableDevice(@Body requestBody: RequestBody): Response<NoResponse>

    //获取用户信息
    @POST("xfit/userInfo/getUserInfo")
    suspend fun getUserInfo(@Body requestBody: RequestBody): Response<GetUserInfoResponse>

    @POST("xfit/userInfo/uploadHeadUrl")
    suspend fun upLoadUserAvatar(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("xfit/userInfo/save")
    suspend fun upLoadUserInfo(@Body requestBody: RequestBody): Response<NoResponse>

    //日常运动 批量上传
    @POST("zfit/dailyExercise/bulk")
    suspend fun upLoadDailyData(@Body requestBody: RequestBody): Response<NoResponse>

    //d)查询步数、距离、卡路里周/月数据统计
    @POST("zfit/dailyExercise/getListByDateRange")
    suspend fun getDailyListByDateRange(@Body requestBody: RequestBody): Response<DailyListResponse>

    //获取每日数据
    @POST("zfit/dailyExercise/getDataByDay")
    suspend fun getDailyDataByDay(@Body requestBody: RequestBody): Response<DailyDayResponse>

    //获取睡眠每日数据
    @POST("zfit/sleep/getDataByDay")
    suspend fun getSleepDataByDay(@Body requestBody: RequestBody): Response<SleepDayResponse>

    //睡眠 批量上传
    @POST("zfit/sleep/bulk")
    suspend fun upLoadSleepData(@Body requestBody: RequestBody): Response<NoResponse>

    //睡眠 查询周/月数据
    @POST("zfit/sleep/getListByDateRange")
    suspend fun getSleepListByDateRange(@Body requestBody: RequestBody): Response<SleepListResponse>

    //心率 每日数据
    @POST("zfit/heartRate/getDataByDay")
    suspend fun getHeartRateDataByDay(@Body requestBody: RequestBody): Response<HeartRateResponse>

    //心率 批量上传
    @POST("zfit/heartRate/bulk")
    suspend fun upLoadHeartRateData(@Body requestBody: RequestBody): Response<NoResponse>

    //心率 查询周/月数据
    @POST("zfit/heartRate/getListByDateRange")
    suspend fun getHeartRateListByDateRange(@Body requestBody: RequestBody): Response<HeartRateListResponse>

    //心率单次测量 每日数据
    @POST("zfit/heartRateMeasure/getListByDay")
    suspend fun getSingleHeartRateDataByDay(@Body requestBody: RequestBody): Response<SingleHeartRateResponse>

    //心率单次测量 获取最近一次测量数据
    @POST("zfit/heartRateMeasure/getLatelyData")
    suspend fun getSingleLastHeartRateData(@Body requestBody: RequestBody): Response<SingleHeartRateLastResponse>

    //心率单次测量 批量上传
    @POST("zfit/heartRateMeasure/bulk")
    suspend fun upLoadSingleHeartRateData(@Body requestBody: RequestBody): Response<NoResponse>

    //压力 每日数据
    @POST("zfit/pressure/getDataByDay")
    suspend fun getPressureDataByDay(@Body requestBody: RequestBody): Response<PressureResponse>

    //压力 批量上传
    @POST("zfit/pressure/bulk")
    suspend fun upLoadPressureData(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("zfit/pressure/getLatelyData")
    suspend fun  getLastPressureData(@Body requestBody: RequestBody): Response<PressureLatelyResponse>

    //压力 查询周/月数据
    @POST("zfit/pressure/getListByDateRange")
    suspend fun getPressureListByDateRange(@Body requestBody: RequestBody): Response<PressureListResponse>

    //压力单次测量 每日数据
    @POST("zfit/pressureMeasure/getListByDay")
    suspend fun getSinglePressureDataByDay(@Body requestBody: RequestBody): Response<SinglePressureResponse>

    //压力单次测量 获取最近一次测量数据
    @POST("zfit/pressureMeasure/getLatelyData")
    suspend fun getSingleLastPressureData(@Body requestBody: RequestBody): Response<SinglePressureLastResponse>

    //压力单次测量 批量上传
    @POST("zfit/pressureMeasure/bulk")
    suspend fun upLoadSinglePressureData(@Body requestBody: RequestBody): Response<NoResponse>

    //离线压力 查询周/月数据
    @POST("zfit/pressureMeasure/getListByDateRange")
    suspend fun getSinglePressureListByDateRange(@Body requestBody: RequestBody): Response<SinglePressureListResponse>

    //血氧饱和度 每日数据
    @POST("zfit/bloodOxygen/getDataByDay")
    suspend fun getBloodOxygenDataByDay(@Body requestBody: RequestBody): Response<BloodOxygenResponse>

    //血氧饱和度 查询周/月数据
    @POST("zfit/bloodOxygen/getListByDateRange")
    suspend fun getBloodOxygenListByDateRange(@Body requestBody: RequestBody): Response<BloodOxygenListResponse>

    //血氧饱和度 批量上传
    @POST("zfit/bloodOxygen/bulk")
    suspend fun upLoadBloodOxygen(@Body requestBody: RequestBody): Response<NoResponse>

    //有效站立 每日数据
    @POST("zfit/effectiveStanding/getDataByDay")
    suspend fun getEffectiveStandDataByDay(@Body requestBody: RequestBody): Response<EffectiveStandResponse>

    //有效站立 查询周/月数据
    @POST("zfit/effectiveStanding/getListByDateRange")
    suspend fun getEffectiveStandListByDateRange(@Body requestBody: RequestBody): Response<EffectiveStandListResponse>

    //有效站立 批量上传
    @POST("zfit/effectiveStanding/bulk")
    suspend fun upLoadEffectiveStand(@Body requestBody: RequestBody): Response<NoResponse>

//    //表盘支持语言
//    @POST("xfit/dial/getLanguageCode")
//    suspend fun getLanguageCode(@Body requestBody: RequestBody): Response<GetDialListResponse>

    //首页表盘列表获取
    @POST("xfit/dial/getHomeByProductList")
    suspend fun getHomeByProductList(@Body requestBody: RequestBody): Response<GetDialListResponse>

    //表盘列表分类查询
    @POST("xfit/dial/moreDialPageByProductList")
    suspend fun moreDialPageByProductList(@Body requestBody: RequestBody): Response<MoreDialPageResponse>

    //表盘详情
    @POST("xfit/dial/info")
    suspend fun dialInfo(@Body requestBody: RequestBody): Response<DialInfoResponse>

    //思澈表盘详情
    @POST("xfit/dial/sifiInfo")
    suspend fun siflidialInfo(@Body requestBody: RequestBody): Response<SiflidialInfoResponse>


    //获取系统表盘列表
    @POST("xfit/dial/queryDialSystemList")
    suspend fun queryDialSystemList(@Body requestBody: RequestBody): Response<DialSystemResponse>

    //Diy表盘列表获取
    @POST("zfit/diy/homeList")
    suspend fun getDiyHomeList(@Body requestBody: RequestBody): Response<DiyHomeListResponse>

    //获取分页列表DIY表盘
    @POST("zfit/diy/pageList")
    suspend fun getDiyPageList(@Body requestBody: RequestBody): Response<MoreDialPageResponse>

    //获取DIY表盘详情
    @POST("zfit/diy/info")
    suspend fun getDiyInfo(@Body requestBody: RequestBody): Response<DiyDialInfoResponse>


    //查询目标设置信息
    @POST("xfit/calibration/queryByUserID")
    suspend fun queryTargetInfo(@Body requestBody: RequestBody): Response<TargetSettingResponse>

    //保存目标设置信息
    @POST("xfit/calibration/save")
    suspend fun uploadTargetInfo(@Body requestBody: RequestBody): Response<NoResponse>

    //上传反馈信息
    @POST("zfit/feedBack/save")
    suspend fun uploadFeedbackInfo(@Body body: okhttp3.RequestBody): Response<NoResponse>

    //查询app版本信息
    @POST("xfit/appUpdate/getAppVersion")
    fun checkAppVersion(@Body requestBody: RequestBody): Call<Response<AppVersionResponse>>

    //多运动批量上传
    @POST("zfit/exercise/bulk")
    suspend fun uploadExerciseData(@Body requestBody: RequestBody): Response<NoResponse>

    //查询运动列表
    @POST("zfit/exercise/list")
    suspend fun queryExerciseList(@Body requestBody: RequestBody): Response<SportExerciseResponse?>

    //查询运动详情
    @POST("zfit/exercise/info")
    suspend fun queryExerciseInfo(@Body requestBody: RequestBody): Response<SportModleInfo?>

    //获取语言列表
    @POST("zfit/language/deviceLanguageList")
    suspend fun queryDeviceLanguageList(@Body requestBody: RequestBody): Response<DeviceLanguageListResponse?>

    //获取当前语言应用列表 图表和名称
    @POST("zfit/ico/moduleList")
    suspend fun queryApplicationListInfo(@Body requestBody: RequestBody): Response<ApplicationListResponse?>

    //获取当前语言应用列表 图表和名称
    @POST("zfit/ico/cardSortList")
    suspend fun queryCardListInfo(@Body requestBody: RequestBody): Response<ApplicationListResponse?>

    //获取设备固件版本升级信息
    @POST("xfit/firmware/getFirewareUpgradeVersion")
    suspend fun queryFirewareUpgradeVersion(@Body requestBody: RequestBody): Response<FirewareUpgradeResponse?>

    //固件升级后修改设备版本号
    @POST("zfit/device/upd")
    suspend fun upLoadDeviceVersion(@Body requestBody: RequestBody): Response<NoResponse?>

    /***********************************************************************************************
     *      获取最近一次记录数据
     **********************************************************************************************/
    //获取最近日常运动
    @POST("zfit/dailyExercise/getLatelyData")
    suspend fun getDailyLatelyData(@Body requestBody: RequestBody): Response<DailyLatelyResponse>

    //获取最近睡眠
    @POST("zfit/sleep/getLatelyData")
    suspend fun getSleepLatelyData(@Body requestBody: RequestBody): Response<SleepLatelyResponse>

    //获取最近心率
    @POST("zfit/heartRate/getLatelyData")
    suspend fun getHeartRateData(@Body requestBody: RequestBody): Response<HeartRateLatelyResponse>

    //获取最近血氧
    @POST("zfit/bloodOxygen/getLatelyData")
    suspend fun getBloodOxygenLatelyData(@Body requestBody: RequestBody): Response<BloodOxygenLatelyResponse>

    //获取最近有效站立
    @POST("zfit/effectiveStanding/getLatelyData")
    suspend fun getEffectiveStandingLatelyData(@Body requestBody: RequestBody): Response<EffectiveStandLatelyResponse>


    //血氧单次测量上传
    @POST("zfit/bloodOxygenMeasure/bulk")
    suspend fun upLoadSingleBloodOxygen(@Body requestBody: RequestBody): Response<NoResponse>

    //血氧单次测量每日数据
    @POST("zfit/bloodOxygenMeasure/getListByDay")
    suspend fun getSingleBloodOxygenDataByDay(@Body requestBody: RequestBody): Response<SingleBloodOxygenResponse>

    //血氧单次测量查询周/月数据
    @POST("zfit/bloodOxygenMeasure/getListByDateRange")
    suspend fun getSingleBloodOxygenListByDateRange(@Body requestBody: RequestBody): Response<SingleBloodOxygenListResponse>

    //血氧单次测量最后一次数据
    @POST("zfit/bloodOxygenMeasure/getLatelyData")
    suspend fun getSingleBloodOxygenLatelyData(@Body requestBody: RequestBody): Response<SingleBloodOxygenLastResponse>

    //请求AGPS信息
    @GET("xfit/bream/downloadLto")
    suspend fun requestAgpsInfo(): Response<AgpsResponse>

    //获取后台允许说明图片资源
    @POST("xfit/appKeepAlive/imageList")
    suspend fun getAppKaImgs(@Body requestBody: RequestBody): Response<KeepLiveResponse>

    //注销账号
    @POST("zfit/user/logOut")
    suspend fun logout(@Body requestBody: RequestBody): Response<NoResponse>

    //批量上传-心电数据
    @POST("zfit/ecg/bulk")
    suspend fun uploadEcgList(@Body requestBody: RequestBody): Response<NoResponse>

    //查询指定日期ECG测量数据列表
    @POST("zfit/ecg/getListByDay")
    suspend fun getEcgListByDay(@Body requestBody: RequestBody): Response<EcgResponse>

    //根据数据ID查询数据详情
    @POST("zfit/ecg/info")
    suspend fun getEcgDetailedData(@Body requestBody: RequestBody): Response<EcgDetailsResponse>

    //根据日期查询最近一次数据
    @POST("zfit/ecg/getLatelyData")
    suspend fun getEcgLastlyData(@Body requestBody: RequestBody): Response<EcgLastDataResponse>

    //逆地址解析，天气，经纬度获取城市名
    @POST("zfit/geocoder/adInfo")
    suspend fun getAdInfo(@Body requestBody: RequestBody): Response<AdInfoResponse>

    //表盘下载传输记录
    @POST("xfit/dial/downloadDialLog")
    suspend fun dialLog(@Body requestBody: RequestBody): Response<NoResponse>

    //活跃用户
    @POST("xfit/openapp/startApp")
    suspend fun startApp(@Body requestBody: RequestBody): Response<NoResponse>

    //用户行为上报
    @POST("zfit/trace/save")
    suspend fun traceSave(@Body requestBody: RequestBody): Response<NoResponse>

    //设备断连原因上传
    @POST("zfit/exceptionLog/deviceDisconnection")
    suspend fun deviceDisconnection(@Body requestBody: RequestBody): Response<NoResponse>

    //open weather api ====================================================================================

    /**
     * @param appid : 密钥
     * @param q : 城市名字
     * @param limit : 城市个数限制
     */
    @GET("direct")
    fun searchCityByName(
        @Query("appid") appid: String,
        @Query("q") q: String,
        @Query("limit") limit: String
    ): Flowable<List<SearchCityEntity>>


    /**
     * @param appid : 密钥
     * @param lat : 城市纬度
     * @param lon : 城市经度
     * @param limit : 城市个数限制
     * */
    @GET("reverse")
    fun searchCityByLocation(
        @Query("appid") appid: String,
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("limit") limit: String
    ): Flowable<List<SearchCityEntity>>
}