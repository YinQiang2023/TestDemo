package com.smartwear.xzfit.https.response


class AdInfoResponse {
    var nation = ""//String	国籍
    var province = ""//String	省
    var city = ""//String	城市
    var district = ""//String	区/县
    override fun toString(): String {
        return "AdInfoResponse(nation='$nation', province='$province', city='$city', district='$district')"
    }
}