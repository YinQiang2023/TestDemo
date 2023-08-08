package com.smartwear.xzfit.https.strava

import com.smartwear.xzfit.https.params.StravaOauthTokenParam
import com.smartwear.xzfit.https.response.StravaTokenResponse
import okhttp3.MultipartBody
import retrofit2.http.*


/**
 * Created by Android on 2022/5/17.
 */
interface StravaApiService {

    /**
     * 获取strava token
     */
    @POST("oauth/token")
    suspend fun oauthtoken(@Body requestBody: StravaOauthTokenParam): StravaTokenResponse?

    /**
     * 取消strava登录授权
     */
    @POST("oauth/deauthorize")
    suspend fun deauthorize(@Query("access_token") access_token: String): Any?


    //https://developers.strava.com/docs/reference/#api-Uploads-createUpload
    @POST("api/v3/uploads")
    suspend fun uploads(@Body requestBody: MultipartBody): Any?

}