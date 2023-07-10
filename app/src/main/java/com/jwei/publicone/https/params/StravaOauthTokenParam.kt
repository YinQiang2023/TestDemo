package com.jwei.publicone.https.params

/**
 * Created by Android on 2022/5/17.
 */
data class StravaOauthTokenParam(
    var client_id:String,
    var client_secret:String,
    var code:String? = null,
    var grant_type:String,
    var refresh_token:String? = null
)