package com.jwei.publicone.https.tracking

import com.jwei.publicone.https.Response
import com.jwei.publicone.https.params.TrackingAppParam
import com.jwei.publicone.https.response.NoResponse
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
     * @see com.jwei.publicone.https.params.TrackingAppParam
     */
    @POST("app/error/bulk")
    suspend fun appTrackingErrorBulk(@Body requestBody: RequestBody): Response<NoResponse>

    @POST("app/error/bulk")
    suspend fun appTrackingErrorBulk(@Body requestBody: List<TrackingAppParam>): Response<NoResponse>

    /**
     * 用户行为埋点
     * @param requestBody List<UserBehaviorParam> max = 20
     * @see com.jwei.publicone.https.params.UserBehaviorParam
     */
    @POST("app/use/bulk")
    suspend fun appUserBehaviorTrackingBulk(@Body requestBody: RequestBody): Response<NoResponse>

    /**
     * 设备日志埋点上传
     * @param requestBody DevTrackingParam
     * @see com.jwei.publicone.https.params.DevTrackingParam
     */
    @POST("dev/log/bulk")
    suspend fun devTrackingErrorBulk(@Body requestBody: RequestBody): Response<NoResponse>





}