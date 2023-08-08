package com.smartwear.xzfit.https.tracking

import com.smartwear.xzfit.https.Response
import com.smartwear.xzfit.https.params.TrackingAppParam
import com.smartwear.xzfit.https.response.NoResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by Android on 2023/4/10.
 * 接口文档
 * https://qrk9bouqk0.feishu.cn/docx/HiGidsrPioKqROxcoElclDlvnQg
 */
interface TrackingApi {

    /**
     * app埋点异常日志上传
     * @param requestBody TrackingAppParam
     * @see com.smartwear.xzfit.https.params.TrackingAppParam
     */
    @POST("app/error/bulk")
    suspend fun appTrackingErrorBulk(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("app/error/bulk")
    suspend fun appTrackingErrorBulk(@Body requestBody: List<TrackingAppParam>): Response<NoResponse>

    /**
     * 用户行为埋点
     * @param requestBody List<UserBehaviorParam> max = 20
     * @see com.smartwear.xzfit.https.params.UserBehaviorParam
     */
    @POST("app/use/bulk")
    suspend fun appUserBehaviorTrackingBulk(@Body requestBody: RequestBody): Response<NoResponse>

    /**
     * 设备日志埋点上传
     * @param requestBody DevTrackingParam
     * @see com.smartwear.xzfit.https.params.DevTrackingParam
     */
    @POST("dev/log/bulk")
    suspend fun devTrackingErrorBulk(@Body requestBody: RequestBody): Response<NoResponse>





}