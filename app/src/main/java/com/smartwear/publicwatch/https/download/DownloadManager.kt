package com.smartwear.publicwatch.https.download

import android.text.TextUtils
import com.blankj.utilcode.util.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.*

/**
 * Created by Android on 2022/5/27.
 * 下载管理类
 * 下载进度由 网络下载90% + 文件写入 10% 组成
 */
object DownloadManager {
    val TAG = DownloadManager::class.java.simpleName

    var networkDownloadProgressListener: NetworkDownloadProgressListener? = null

    //缓存流大小
    private const val BUFFER_SIZE = 512 * 1024

    fun download(url: String?, dir: String? = "", name: String? = "", listener: DownloadListener? = null) {
        var filePath = ""
        var fileDir = dir
        var fileName = name
        if (TextUtils.isEmpty(url)) {
            listener?.onFailed("url is empty: $url")
            return
        }
        if (TextUtils.isEmpty(fileDir)) fileDir = PathUtils.getExternalAppDownloadPath()
        if (!FileUtils.createOrExistsDir(fileDir)) {
            fileDir = PathUtils.getExternalDownloadsPath()
            LogUtils.d("fileDir$fileDir")
            if (!FileUtils.createOrExistsDir(fileDir)) {
                listener?.onFailed("fileDir is empty: $fileDir")
                return
            }
        }
        if (TextUtils.isEmpty(fileName)) {
            fileName = FileUtils.getFileName(url)
            if (TextUtils.isEmpty(fileName)) {
                listener?.onFailed("fileName is empty: $fileName")
                return
            }
        }
        networkDownloadProgressListener = NetworkDownloadProgressListener(listener)
        listener?.onStart()
        filePath = fileDir + File.separator + fileName
        DownloadRetrofitClient.service.download(url).enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (FileUtils.createOrExistsFile(filePath)) {
                    writeFile(filePath, response.body(), listener)
                } else {
                    ThreadUtils.runOnUiThread {
                        listener?.onFailed("file not exists: $filePath")
                    }
                }

            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                ThreadUtils.runOnUiThread {
                    val msg = StringBuilder()
                    if (t is HttpException) {
                        //获取对应statusCode和Message
                        val exception: HttpException = t as HttpException
                        val message = exception.response()?.message()
                        val code = exception.response()?.code()
                        if (code != null && message != null) {
                            msg.append("Http code = $code message: $message\n")
                        }
                    }
                    msg.append("download $url Failed : $t")
                    listener?.onFailed(msg.toString())
                }
            }

        })
    }

    /**
     * 文件网络下载进度监听
     */
    class NetworkDownloadProgressListener(var listener: DownloadListener?) : ProgressListener {

        override fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean) {
            ThreadUtils.runOnUiThread {
                listener?.onProgress(contentLength, (bytesRead * 0.9).toLong())
            }
        }
    }

    /**
     * 文件写入进度监听
     */
    private fun writeFile(path: String, body: ResponseBody?, listener: DownloadListener?) {
        ThreadUtils.executeByIo(object : ThreadUtils.Task<Boolean>() {
            override fun doInBackground(): Boolean {
                val inputStream: InputStream? = body?.byteStream()
                val os = BufferedOutputStream(FileOutputStream(FileUtils.getFileByPath(path)), BUFFER_SIZE)
                val totalSize = body?.contentLength() ?: 0
                var curSize = 0L
                ThreadUtils.runOnUiThread {
                    listener?.onProgress(totalSize, curSize)
                }
                val data = ByteArray(BUFFER_SIZE)
                var len: Int
                while (inputStream!!.read(data).also { len = it } != -1) {
                    os.write(data, 0, len)
                    curSize += len
                    ThreadUtils.runOnUiThread {
                        if (totalSize != curSize) {
                            listener?.onProgress(totalSize, (totalSize * 0.9).toLong() + curSize * 0.1.toLong())
                        } else {
                            listener?.onProgress(totalSize, totalSize)
                        }
                    }
                }
                os.close()
                inputStream.close()
                return true
            }

            override fun onSuccess(result: Boolean) {
                if (result) {
                    listener?.onSucceed(path)
                }
            }

            override fun onCancel() {
            }

            override fun onFail(t: Throwable?) {
                t?.printStackTrace()
                listener?.onFailed("download Failed :writeFile $t")
            }

        })

    }
}