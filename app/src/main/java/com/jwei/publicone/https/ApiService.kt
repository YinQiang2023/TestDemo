package com.jwei.publicone.https

import com.jwei.publicone.db.model.sport.SportModleInfo
import com.jwei.publicone.https.response.*
import com.jwei.publicone.ui.device.weather.bean.SearchCityEntity
import io.reactivex.Flowable
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by android
 * on 2021/7/14
 */
interface ApiService {

    @POST("ffit/user/register")
    suspend fun register(@Body requestBody: RequestBody): Response<RegisterResponse>

    @POST("ffit/user/queryByLoginName")
    suspend fun queryByLoginName(@Body requestBody: RequestBody): Response<QureyLoginAccountResponse>

    @POST("ffit/user/login")
    suspend fun login(@Body requestBody: RequestBody): Response<LoginResponse>
    
    @POST("ffit/thirdParty/ssoLogin")
    suspend fun ssoLogin(@Body requestBody: RequestBody): Response<LoginResponse>

    @POST("ffit/user/forgetPwd")
    suspend fun findPassWord(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("ffit/auth/getCode")
    suspend fun getCode(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("infowear/device/bind")
    suspend fun bindDevice(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("infowear/product/info")
    suspend fun productInfo(@Body requestBody: RequestBody): Response<ProductInfoResponse>

    @POST("infowear/product/versionInfo")
    suspend fun versionInfo(@Body requestBody: RequestBody): Response<VersionInfoResponse>

    @POST("infowear/device/unBind")
    suspend fun unbindDevice(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("infowear/device/bindList")
    suspend fun getBindList(@Body requestBody: RequestBody): Response<BindListResponse>

    //e)获取产品设备号列表
    @POST("infowear/product/list")
    suspend fun getProductList(): Response<ProductListResponse>

    //设备启用
    @POST("infowear/device/enable")
    suspend fun enableDevice(@Body requestBody: RequestBody): Response<NoResponse>

    //获取用户信息
    @POST("ffit/userInfo/getUserInfo")
    suspend fun getUserInfo(@Body requestBody: RequestBody): Response<GetUserInfoResponse>

    @POST("ffit/userInfo/uploadHeadUrl")
    suspend fun upLoadUserAvatar(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("ffit/userInfo/save")
    suspend fun upLoadUserInfo(@Body requestBody: RequestBody): Response<NoResponse>

    //日常运动 批量上传
    @POST("infowear/dailyExercise/bulk")
    suspend fun upLoadDailyData(@Body requestBody: RequestBody): Response<NoResponse>

    //d)查询步数、距离、卡路里周/月数据统计
    @POST("infowear/dailyExercise/getListByDateRange")
    suspend fun getDailyListByDateRange(@Body requestBody: RequestBody): Response<DailyListResponse>

    //获取每日数据
    @POST("infowear/dailyExercise/getDataByDay")
    suspend fun getDailyDataByDay(@Body requestBody: RequestBody): Response<DailyDayResponse>

    //获取睡眠每日数据
    @POST("infowear/sleep/getDataByDay")
    suspend fun getSleepDataByDay(@Body requestBody: RequestBody): Response<SleepDayResponse>

    //睡眠 批量上传
    @POST("infowear/sleep/bulk")
    suspend fun upLoadSleepData(@Body requestBody: RequestBody): Response<NoResponse>

    //睡眠 查询周/月数据
    @POST("infowear/sleep/getListByDateRange")
    suspend fun getSleepListByDateRange(@Body requestBody: RequestBody): Response<SleepListResponse>

    //心率 每日数据
    @POST("infowear/heartRate/getDataByDay")
    suspend fun getHeartRateDataByDay(@Body requestBody: RequestBody): Response<HeartRateResponse>

    //心率 批量上传
    @POST("infowear/heartRate/bulk")
    suspend fun upLoadHeartRateData(@Body requestBody: RequestBody): Response<NoResponse>

    //心率 查询周/月数据
    @POST("infowear/heartRate/getListByDateRange")
    suspend fun getHeartRateListByDateRange(@Body requestBody: RequestBody): Response<HeartRateListResponse>

    //心率单次测量 每日数据
    @POST("infowear/heartRateMeasure/getListByDay")
    suspend fun getSingleHeartRateDataByDay(@Body requestBody: RequestBody): Response<SingleHeartRateResponse>

    //心率单次测量 获取最近一次测量数据
    @POST("infowear/heartRateMeasure/getLatelyData")
    suspend fun getSingleLastHeartRateData(@Body requestBody: RequestBody): Response<SingleHeartRateLastResponse>

    //心率单次测量 批量上传
    @POST("infowear/heartRateMeasure/bulk")
    suspend fun upLoadSingleHeartRateData(@Body requestBody: RequestBody): Response<NoResponse>

    //压力 每日数据
    @POST("infowear/pressure/getDataByDay")
    suspend fun getPressureDataByDay(@Body requestBody: RequestBody): Response<PressureResponse>

    //压力 批量上传
    @POST("infowear/pressure/bulk")
    suspend fun upLoadPressureData(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("infowear/pressure/getLatelyData")
    suspend fun  getLastPressureData(@Body requestBody: RequestBody): Response<PressureLatelyResponse>

    //压力 查询周/月数据
    @POST("infowear/pressure/getListByDateRange")
    suspend fun getPressureListByDateRange(@Body requestBody: RequestBody): Response<PressureListResponse>

    //压力单次测量 每日数据
    @POST("infowear/pressureMeasure/getListByDay")
    suspend fun getSinglePressureDataByDay(@Body requestBody: RequestBody): Response<SinglePressureResponse>

    //压力单次测量 获取最近一次测量数据
    @POST("infowear/pressureMeasure/getLatelyData")
    suspend fun getSingleLastPressureData(@Body requestBody: RequestBody): Response<SinglePressureLastResponse>

    //压力单次测量 批量上传
    @POST("infowear/pressureMeasure/bulk")
    suspend fun upLoadSinglePressureData(@Body requestBody: RequestBody): Response<NoResponse>

    //离线压力 查询周/月数据
    @POST("infowear/pressureMeasure/getListByDateRange")
    suspend fun getSinglePressureListByDateRange(@Body requestBody: RequestBody): Response<SinglePressureListResponse>

    //血氧饱和度 每日数据
    @POST("infowear/bloodOxygen/getDataByDay")
    suspend fun getBloodOxygenDataByDay(@Body requestBody: RequestBody): Response<BloodOxygenResponse>

    //血氧饱和度 查询周/月数据
    @POST("infowear/bloodOxygen/getListByDateRange")
    suspend fun getBloodOxygenListByDateRange(@Body requestBody: RequestBody): Response<BloodOxygenListResponse>

    //血氧饱和度 批量上传
    @POST("infowear/bloodOxygen/bulk")
    suspend fun upLoadBloodOxygen(@Body requestBody: RequestBody): Response<NoResponse>

    //有效站立 每日数据
    @POST("infowear/effectiveStanding/getDataByDay")
    suspend fun getEffectiveStandDataByDay(@Body requestBody: RequestBody): Response<EffectiveStandResponse>

    //有效站立 查询周/月数据
    @POST("infowear/effectiveStanding/getListByDateRange")
    suspend fun getEffectiveStandListByDateRange(@Body requestBody: RequestBody): Response<EffectiveStandListResponse>

    //有效站立 批量上传
    @POST("infowear/effectiveStanding/bulk")
    suspend fun upLoadEffectiveStand(@Body requestBody: RequestBody): Response<NoResponse>

//    //表盘支持语言
//    @POST("ffit/dial/getLanguageCode")
//    suspend fun getLanguageCode(@Body requestBody: RequestBody): Response<GetDialListResponse>

    //首页表盘列表获取
    @POST("ffit/dial/getHomeByProductList")
    suspend fun getHomeByProductList(@Body requestBody: RequestBody): Response<GetDialListResponse>

    //表盘列表分类查询
    @POST("ffit/dial/moreDialPageByProductList")
    suspend fun moreDialPageByProductList(@Body requestBody: RequestBody): Response<MoreDialPageResponse>

    //表盘详情
    @POST("ffit/dial/info")
    suspend fun dialInfo(@Body requestBody: RequestBody): Response<DialInfoResponse>

    //获取系统表盘列表
    @POST("ffit/dial/queryDialSystemList")
    suspend fun queryDialSystemList(@Body requestBody: RequestBody): Response<DialSystemResponse>

    //Diy表盘列表获取
    @POST("infowear/diy/homeList")
    suspend fun getDiyHomeList(@Body requestBody: RequestBody): Response<DiyHomeListResponse>

   //获取分页列表DIY表盘
    @POST("infowear/diy/pageList")
    suspend fun getDiyPageList(@Body requestBody: RequestBody): Response<MoreDialPageResponse>

    //获取DIY表盘详情
    @POST("infowear/diy/info")
    suspend fun getDiyInfo(@Body requestBody: RequestBody): Response<DiyDialInfoResponse>


    //查询目标设置信息
    @POST("ffit/calibration/queryByUserID")
    suspend fun queryTargetInfo(@Body requestBody: RequestBody): Response<TargetSettingResponse>

    //保存目标设置信息
    @POST("ffit/calibration/save")
    suspend fun uploadTargetInfo(@Body requestBody: RequestBody): Response<NoResponse>

    //上传反馈信息
    @POST("infowear/feedBack/save")
    suspend fun uploadFeedbackInfo(@Body body: okhttp3.RequestBody): Response<NoResponse>

    //查询app版本信息
    @POST("ffit/appUpdate/getAppVersion")
    fun checkAppVersion(@Body requestBody: RequestBody): Call<Response<AppVersionResponse>>

    //多运动批量上传
    @POST("infowear/exercise/bulk")
    suspend fun uploadExerciseData(@Body requestBody: RequestBody): Response<NoResponse>

    //查询运动列表
    @POST("infowear/exercise/list")
    suspend fun queryExerciseList(@Body requestBody: RequestBody): Response<SportExerciseResponse?>

    //查询运动详情
    @POST("infowear/exercise/info")
    suspend fun queryExerciseInfo(@Body requestBody: RequestBody): Response<SportModleInfo?>

    //获取语言列表
    @POST("infowear/language/deviceLanguageList")
    suspend fun queryDeviceLanguageList(@Body requestBody: RequestBody): Response<DeviceLanguageListResponse?>

    //获取当前语言应用列表 图表和名称
    @POST("infowear/ico/moduleList")
    suspend fun queryApplicationListInfo(@Body requestBody: RequestBody): Response<ApplicationListResponse?>

    //获取当前语言应用列表 图表和名称
    @POST("infowear/ico/cardSortList")
    suspend fun queryCardListInfo(@Body requestBody: RequestBody): Response<ApplicationListResponse?>

    //获取设备固件版本升级信息
    @POST("ffit/firmware/getFirewareUpgradeVersion")
    suspend fun queryFirewareUpgradeVersion(@Body requestBody: RequestBody): Response<FirewareUpgradeResponse?>

    //固件升级后修改设备版本号
    @POST("infowear/device/upd")
    suspend fun upLoadDeviceVersion(@Body requestBody: RequestBody): Response<NoResponse?>

    /***********************************************************************************************
     *      获取最近一次记录数据
     **********************************************************************************************/
    //获取最近日常运动
    @POST("infowear/dailyExercise/getLatelyData")
    suspend fun getDailyLatelyData(@Body requestBody: RequestBody): Response<DailyLatelyResponse>

    //获取最近睡眠
    @POST("infowear/sleep/getLatelyData")
    suspend fun getSleepLatelyData(@Body requestBody: RequestBody): Response<SleepLatelyResponse>

    //获取最近心率
    @POST("infowear/heartRate/getLatelyData")
    suspend fun getHeartRateData(@Body requestBody: RequestBody): Response<HeartRateLatelyResponse>

    //获取最近血氧
    @POST("infowear/bloodOxygen/getLatelyData")
    suspend fun getBloodOxygenLatelyData(@Body requestBody: RequestBody): Response<BloodOxygenLatelyResponse>

    //获取最近有效站立
    @POST("infowear/effectiveStanding/getLatelyData")
    suspend fun getEffectiveStandingLatelyData(@Body requestBody: RequestBody): Response<EffectiveStandLatelyResponse>


    //血氧单次测量上传
    @POST("infowear/bloodOxygenMeasure/bulk")
    suspend fun upLoadSingleBloodOxygen(@Body requestBody: RequestBody): Response<NoResponse>

    //血氧单次测量每日数据
    @POST("infowear/bloodOxygenMeasure/getListByDay")
    suspend fun getSingleBloodOxygenDataByDay(@Body requestBody: RequestBody): Response<SingleBloodOxygenResponse>

    //血氧单次测量查询周/月数据
    @POST("infowear/bloodOxygenMeasure/getListByDateRange")
    suspend fun getSingleBloodOxygenListByDateRange(@Body requestBody: RequestBody): Response<SingleBloodOxygenListResponse>

    //血氧单次测量最后一次数据
    @POST("infowear/bloodOxygenMeasure/getLatelyData")
    suspend fun getSingleBloodOxygenLatelyData(@Body requestBody: RequestBody): Response<SingleBloodOxygenLastResponse>

    //请求AGPS信息
    @GET("ffit/bream/downloadLto")
    suspend fun requestAgpsInfo(): Response<AgpsResponse>

    //获取后台允许说明图片资源
    @POST("ffit/appKeepAlive/imageList")
    suspend fun getAppKaImgs(@Body requestBody: RequestBody): Response<KeepLiveResponse>

    //注销账号
    @POST("infowear/user/logOut")
    suspend fun logout(@Body requestBody: RequestBody): Response<NoResponse>

    //批量上传-心电数据
    @POST("infowear/ecg/bulk")
    suspend fun uploadEcgList(@Body requestBody: RequestBody): Response<NoResponse>

    //查询指定日期ECG测量数据列表
    @POST("infowear/ecg/getListByDay")
    suspend fun getEcgListByDay(@Body requestBody: RequestBody): Response<EcgResponse>

    //根据数据ID查询数据详情
    @POST("infowear/ecg/info")
    suspend fun getEcgDetailedData(@Body requestBody: RequestBody): Response<EcgDetailsResponse>

    //根据日期查询最近一次数据
    @POST("infowear/ecg/getLatelyData")
    suspend fun getEcgLastlyData(@Body requestBody: RequestBody): Response<EcgLastDataResponse>

    //逆地址解析，天气，经纬度获取城市名
    @POST("infowear/geocoder/adInfo")
    suspend fun getAdInfo(@Body requestBody: RequestBody): Response<AdInfoResponse>

    //表盘下载传输记录
    @POST("ffit/dial/downloadDialLog")
    suspend fun dialLog(@Body requestBody: RequestBody): Response<NoResponse>

    //活跃用户
    @POST("ffit/openapp/startApp")
    suspend fun startApp(@Body requestBody: RequestBody): Response<NoResponse>

    //用户行为上报
    @POST("infowear/trace/save")
    suspend fun traceSave(@Body requestBody: RequestBody): Response<NoResponse>

    //设备断连原因上传
    @POST("infowear/exceptionLog/deviceDisconnection")
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