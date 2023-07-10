package com.jwei.publicone.https.response

/**
 * Created by Android on 2023/3/16.
 */
class DiyDialInfoResponse {
    var dialId: String = ""      //Long	表盘ID
    var code: String = ""        //String	表盘编码
    var name: String = ""        //String	名称
    var desc: String = ""        //String	描述
    var backgroundBinCompressed: String = ""     //Integer	背景/缩略图bin是否压缩标识 1：是
    var pointerCompressed: String = ""       //Integer	指针是否压缩标识 1：是
    var complexBinCompressed: String = ""        //Integer	复杂bin是否压缩标识 1：是
    var highLowMark: String = ""     //Integer	高低位标识 1：是
    var defaultBackgroundImage: String = ""      //String	默认背景图
    var backgroundOverlay: String = ""       //String	背景叠加图
    var complicationsBin: String = ""        //String	复杂功能.bin
    var renderings: String = ""      //String	效果图
    var deviceParsingRules = ""      //String	设备解析规则
    var dpi = ""      //String	表盘高宽 WxH
    var thumbnailDpi = ""      //String	效果图高宽 WxH
    var thumbnailOffset = ""      //String	效果图偏移值 WxH
    var pointerList: List<Pointer>? = null
    var positionList: List<LocationPosition>? = null

    class Pointer {
        var binName: String = ""      //String	文件名称
        var binUrl: String = ""       //String	下载连接
        var renderingsUrl: String = ""    //String	效果图
        var pointerCoordinatesX: String = ""      //String	坐标X
        var pointerCoordinatesY: String = ""      //String	坐标Y
        var type: String = ""     //Integer	1：指针 2：数字
        var pointerPictureUrl:String = ""       //String	指针图片
    }

    class LocationPosition {
        var positionCode: String = ""    //Integer	位置编码（1：左上 2：中上 3：右上 4：左中 5：正中 6：右中 7：左下 8：中下 9：右下）
        var enName: String = ""  //String	英文名称
        var cnName: String = ""  //String	中文名称
        var dataElementList: List<Element>? = null

        class Element {
            var dataElementCode: String = ""  //String	数据元编码
            var dataElementCnName: String = ""  //String	中文名称
            var dataElementEnName: String = ""  //String	英文名称
            var coordinateX: String = ""  //String	坐标X
            var coordinateY: String = ""  //String	坐标Y
            var imgUrl: String = ""  //String	元素图标
            var selected:String = ""  //int	1:默认选中 0：未选中
        }
    }

}