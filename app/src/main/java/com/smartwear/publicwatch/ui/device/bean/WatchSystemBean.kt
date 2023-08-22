package com.smartwear.publicwatch.ui.device.bean

import java.io.Serializable

class WatchSystemBean : Serializable {
    var dialCode = ""    //String	表盘ID
    var dialImageUrl = ""    //String	效果图URL
    var isCurrent = false
    var isRemove = false
    override fun toString(): String {
        return "WatchSystemBean(dialCode='$dialCode', dialImageUrl='$dialImageUrl', isCurrent=$isCurrent, isRemove=$isRemove)"
    }
}