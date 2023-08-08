package com.smartwear.xzfit.https.download

interface DownloadListener {
    fun onStart()
    fun onProgress(totalSize: Long, currentSize: Long)
    fun onFailed(msg: String)
    fun onSucceed(path: String)
}