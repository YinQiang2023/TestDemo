package com.jwei.xzfit.ui.device.bean

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose

/**
 * Created by Android on 2021/10/7.
 * 消息列表
 */
data class NotifyItem(
    //类别   1:系统消息  2其它应用消息
    @Expose var type: Int = 0,
    @Expose var packageName: String = "",
    @Expose var title: String = "",
    @Expose var imgName: String = "",  //储图标资源name
    @Expose(serialize = false, deserialize = false) var icon: Drawable? = null, //type = 2 其它app时使用
    @Expose var isTypeHeader: Boolean = false,
    @Expose var isShowLine: Boolean = true,
    @Expose var isCanNext: Boolean = false,
    //开关
    @Expose var isOpen: Boolean = false
)