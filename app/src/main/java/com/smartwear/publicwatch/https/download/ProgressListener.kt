package com.smartwear.publicwatch.https.download

interface ProgressListener {
    fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean)
}