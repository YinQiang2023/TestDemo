package com.jwei.xzfit.https

import com.jwei.xzfit.https.response.NoResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ErrorLogService {
    //APP异常日志上传
    @POST("infowear/exceptionLog/upload")
    suspend fun upLoadError(@Body requestBody: RequestBody): Response<NoResponse>
}