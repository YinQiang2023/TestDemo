package com.smartwear.xzfit.ui.device.bean

import com.smartwear.xzfit.ui.healthy.bean.DragBean

/**
 * Created by Android on 2021/10/28.
 * 设备应用
 */
class DevAppItem : DragBean() {
    var protocolId: Int = 0
    var iconUrl: String = ""
    var haveHide = false //是否可以隐藏
}

