package com.jwei.publicone.https.params

/**
 * Created by Android on 2021/11/8.
 */
data class FirewareUpdateParam(
    var deviceType: String = "",
    var versionBefore: String = "",
    var firmwarePlatform: String = "",
    var userId: String = ""
)