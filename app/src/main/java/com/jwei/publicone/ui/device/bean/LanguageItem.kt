package com.jwei.publicone.ui.device.bean

/**
 * Created by Android on 2021/10/28.
 */
data class LanguageItem(
    var languageId: Int = -1,
    var title: String = "",
    var subTitle: String = "",
    var isDef: Boolean = false,
    var isSelect: Boolean = false
)