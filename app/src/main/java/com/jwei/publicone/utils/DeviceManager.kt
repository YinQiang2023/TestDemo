package com.jwei.publicone.utils

import com.jwei.publicone.https.response.BindListResponse

/**
 * Created by android
 * on 2021/9/9
 */
object DeviceManager {
    private val TAG = DeviceManager::class.java.simpleName
    var dataList: MutableList<BindListResponse.DeviceItem> = mutableListOf()
    fun saveBindList(dataList: MutableList<BindListResponse.DeviceItem>) {
        this.dataList = dataList
    }


}