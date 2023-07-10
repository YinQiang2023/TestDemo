package com.jwei.publicone.ui.device.bean

/**
 * @author YinQiang
 * @date 2023/3/18
 */
interface BulkDownloadListener {
    fun onProgress(totalSize: Int, currentSize: Int)
    fun onFailed(msg: String)
    fun onComplete()
}