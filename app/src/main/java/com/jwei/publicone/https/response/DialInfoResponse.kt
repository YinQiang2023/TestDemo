package com.jwei.publicone.https.response

class DialInfoResponse {
    var dialId = "" //	Long	表盘ID
    var dialCode = "" //	String	表盘编码
    var dialName = "" //	String	表盘名称
    var authorName = "" //	String	作者名称
    var dialDescribe = "" //	String	表盘描述
    var deviceWidth = "" //	Integer	设备宽度
    var deviceHeight = "" //	Integer	设备高度
    var deviceShape = "" //	Integer	形状 = 0=方形/1=球拍/2=圆形/3=圆角矩形1
    var deviceIsHeart = "" //	Integer	是否支持心率
    var binSize = "" //	Integer	Bin文件大小
    var dialGroupCode = "" //	String	父表盘编码（0：主表盘）
    var languageCode = "" //	String	表盘支持语言编码
    var languageName = "" //	String	表盘支持语言名称
    var clockDialType = "" //	int	表盘类型  0=普通，1=自定义 此字段不能修改，只能出现0/1
    var clockDialDataFormat = "" //	int	表盘数据格式 0=正向，1=反向
    var clockDialDataGenerationMode = "" //	int	系统0:舟海 1：木兰
    var zhouhaiDeviceIsPointer = "" //	int	1:支持指针表盘
    var downNum = "" //	Integer	下载次数

    //    dialFileList 	集合	附件文件列表
    lateinit var dialFileList: List<DialFile>

    class DialFile {
        var dialFileUrl = "" //	String	附件文件下载地址
        var dialFileType = "" //	int	1:横向扫描 2:竖向扫描 3:效果图 4:缩略图 5:背景图片 6:文字图片
        var md5Value = "" //	String	当dialFileType=1，dialFileType=2时，mdf5Value不为空
        override fun toString(): String {
            return "DialFile(dialFileUrl='$dialFileUrl', dialFileType='$dialFileType', md5Value='$md5Value')"
        }

    }

    //    groupDialList	集合	相同组表盘列表（子表盘集合）
    lateinit var groupDialList: List<GroupDial>

    class GroupDial {
        var dialId = "" //	Long	表盘ID
        var dialName = "" //	String	表盘名称
        var effectImgUrl = "" //	String	表盘效果图URL
        var thumbnailUrl = "" //	String	表盘缩略图URL
        override fun toString(): String {
            return "GroupDial(dialId='$dialId', dialName='$dialName', effectImgUrl='$effectImgUrl', thumbnailUrl='$thumbnailUrl')"
        }

    }

    override fun toString(): String {
        return "DialInfoResponse(dialId='$dialId', dialCode='$dialCode', dialName='$dialName', authorName='$authorName', dialDescribe='$dialDescribe', deviceWidth='$deviceWidth', deviceHeight='$deviceHeight', deviceShape='$deviceShape', deviceIsHeart='$deviceIsHeart', binSize='$binSize', dialGroupCode='$dialGroupCode', languageCode='$languageCode', languageName='$languageName', clockDialType='$clockDialType', clockDialDataFormat='$clockDialDataFormat', clockDialDataGenerationMode='$clockDialDataGenerationMode', zhouhaiDeviceIsPointer='$zhouhaiDeviceIsPointer', downNum='$downNum', dialFileList=$dialFileList, groupDialList=$groupDialList)"
    }


}