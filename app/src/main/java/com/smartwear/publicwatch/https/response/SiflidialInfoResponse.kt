package com.smartwear.publicwatch.https.response

/**
 * Created by Android on 2023/7/24.
 * 思澈表盘详情
 */
class SiflidialInfoResponse {
    var dialId = "" //	表盘ID  思澈下发 2023.07.17
    var dialCode = "" //	String	表盘编码  思澈下发 2023.07.17
    var dialName = "" //	String	表盘名称  思澈下发 2023.07.17
    var dialDescribe = "" //	String	表盘描述  思澈下发 2023.07.17
    //var dialFileList: List<DialInfoResponse.DialFile>? = null //	集合	附件文件列表
    var dialFileUrl = "" //	String	附件文件下载地址
    var binSize = "" //	Integer	Bin文件大小
    var effectImgUrl = "" //	String	表盘效果图URL
    var groupDialList: List<DialInfoResponse.GroupDial>? = null //	集合	相同组表盘列表（子表盘集合）

    override fun toString(): String {
        return "SiflidialInfoResponse(dialId='$dialId', dialCode='$dialCode', dialName='$dialName', dialDescribe='$dialDescribe', dialFileUrl='$dialFileUrl', binSize='$binSize', effectImgUrl='$effectImgUrl', groupDialList=$groupDialList)"
    }

}