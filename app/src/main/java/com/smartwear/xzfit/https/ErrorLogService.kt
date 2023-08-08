package com.smartwear.xzfit.https

import com.smartwear.xzfit.https.response.NoResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ErrorLogService {
    //APP异常日志上传
    @POST("infowear/exceptionLog/upload")
    suspend fun upLoadError(@Body requestBody: RequestBody): Response<NoResponse>
}