package com.jwei.publicone.https.download

interface ProgressListener {
    fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean)
}