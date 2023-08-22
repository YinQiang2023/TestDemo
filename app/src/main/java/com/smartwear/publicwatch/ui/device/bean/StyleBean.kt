package com.smartwear.publicwatch.ui.device.bean

import android.graphics.Bitmap

/**
 * @author YinQiang
 * @date 2023/3/21
 */
data class StyleBean(
    var type: Int,
    var img: Bitmap,
    var imgData: Bitmap,
    var binData: ByteArray,
    var isSelected: Boolean,
)