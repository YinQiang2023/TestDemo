package com.smartwear.publicwatch.ui.healthy.bean

import java.io.Serializable

class HealthyItemBean : Serializable {
    //主页使用
    var topTitleImg = 0
    var topTitleText = ""
    var subTitleText = ""
    var topTitleTextId = 0
    var context = ""
    var contextId = 0
    var bottomText = ""
    var bottomText2 = ""
    var bottomTextId = 0
    var bg = 0
    var tag = ""
    override fun toString(): String {
        return "HealthyItemBean(topTitleImg=$topTitleImg, topTitleText='$topTitleText', subTitleText='$subTitleText', topTitleTextId=$topTitleTextId, context='$context', contextId=$contextId, bottomText='$bottomText', bottomText2='$bottomText2', bottomTextId=$bottomTextId, bg=$bg, tag='$tag')"
    }


}