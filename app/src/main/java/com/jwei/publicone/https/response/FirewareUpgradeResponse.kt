package com.jwei.publicone.https.response

/**
 * Created by Android on 2021/11/8.
 */
data class FirewareUpgradeResponse(
    var id: String = "",                 //OTA升级文件ID
    var firmwarePlatform:String = "",    //固件平台
    var deviceType: String = "",         //设备号
    var versionBefore: String = "",      //待升级版本号
    var versionAfter: String = "",       //升级后版本号
    var resType: String = "",            //1：zip 2:bin
    var versionUrl: String = "",         //URL
    var remark: String = "",             //备注
    var mustUpdate: String = ""          //1:必须更新 0：可选

) {
    override fun toString(): String {
        return "FirewareUpgradeResponse(id='$id', firmwarePlatform='$firmwarePlatform', deviceType='$deviceType', versionBefore='$versionBefore', versionAfter='$versionAfter', resType='$resType', versionUrl='$versionUrl', remark='$remark', mustUpdate='$mustUpdate')"
    }
}