package com.jwei.publicone.https.download

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.IOException
import kotlin.Throws

class ProgressResponseBody internal constructor(private val url: String, private val responseBody: ResponseBody?, private val progressListener: ProgressListener?) : ResponseBody() {
    private var bufferedSource: BufferedSource? = null
    override fun contentType(): MediaType? {
        return responseBody?.contentType()
    }

    override fun contentLength(): Long {
        return responseBody?.contentLength() ?: 0
    }

    override fun source(): BufferedSource {
        if (bufferedSource == null) {
            responseBody?.source()?.let {
                bufferedSource = source(it)?.buffer()
            }
        }
        return bufferedSource ?: throw NullPointerException("bufferedSource is an null object!")
    }

    private fun source(source: Source?): Source? {
        if (source == null) return null
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressListener?.update(url, totalBytesRead, responseBody?.contentLength() ?: 0, bytesRead == -1L)
                return bytesRead
            }
        }
    }
}