package com.smartwear.publicwatch.utils

import com.blankj.utilcode.util.ThreadUtils
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.ErrorLogCallBack
import com.zhapp.ble.utils.ErrorLogTyepAttr
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.https.ErrorLogRetrofitClient
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.params.ErrorLogBean
import com.smartwear.publicwatch.ui.data.Global
import kotlinx.coroutines.*

object ErrorUtils {
    private val TAG = ErrorUtils::class.java.simpleName
    private var errorTypeForConnect: StringBuffer? = null
    private var errorTypeForBindDevice: StringBuffer? = null
    private var errorTypeForSync: StringBuffer? = null
    private var errorTypeForBigData: StringBuffer? = null

    //以下记录上报
    val ERROR_TYPE_FOR_CONNECT = "01"//"连接"
    val ERROR_TYPE_FOR_BINDDEVICE = "02"//"绑定"
    val ERROR_TYPE_FOR_SYNC = "03"//""同步数据"
    val ERROR_TYPE_FOR_OTA = "04" // OTA
    val ERROR_TYPE_FOR_DIAL = "05" // 表盘
    val ERROR_TYPE_FOR_AGPS = "06" //AGPS

    //以下单独上报
    val ERROR_TYPE_EMAIL = "07" //邮箱异常
    val ERROR_TYPE_HTTP = "08" //网络异常

    private var currentBigData = ERROR_TYPE_FOR_OTA
    private val linkBreak = "\r\n"//换行符
    private val scope = MainScope()

    //子模块
    val ERROR_MODE_BIND_DEVICE_FOR_BIND_INFO = "绑定信息不匹配"
    val ERROR_MODE_BIND_DEVICE_FOR_DEVICE = "设备回复绑定失败"
    val ERROR_MODE_BIND_DEVICE_FOR_SERVICE_1 = "后台回复重复绑定"
    val ERROR_MODE_BIND_DEVICE_FOR_SERVICE_2 = "后台绑定网络异常"
    val ERROR_MODE_BIND_DEVICE_FOR_SERVICE_3 = "后台回复绑定失败"
    val ERROR_MODE_SYNC_TIME_OUT = "同步超时"
    val ERROR_MODE_REQUEST_BACKGROUND_DATA = "请求后台数据失败"
    val ERROR_MODE_DOWNLOAD_FAIL = "下载文件失败"
    val ERROR_MODE_TRANSMISSION_TIMEOUT = "传输超时"
    val OTA_LOSS = "大文件丢包"
    val ERROR_MODE_OTHER = "本地文件获取异常"
    val EMAIL_ERROR = "邮箱格式错误"
    val EMAIL_OR_PHONE_ERROR = "邮箱或手机格式错误"
    val HTTP_ERROR = "网络异常"

    init {
        //集中处理数据
        CallBackUtils.errorLogCallBack = object : ErrorLogCallBack {

            //追加数据记录
            override fun onResult(log: String?) {
                systemError {
                    if (errorTypeForConnect != null) {
                        if (errorTypeForConnect!!.length < Int.MAX_VALUE - 100) {

//                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("蓝牙日志").apply {
//                                log = errorTypeForConnect!!.toString()
//                            })

                            errorTypeForConnect!!.append("$log$linkBreak")
                        }
                    }
                    if (errorTypeForBindDevice != null) {
                        if (errorTypeForBindDevice!!.length < Int.MAX_VALUE - 100) {
                            errorTypeForBindDevice!!.append("$log$linkBreak")
                        }
                    }
                    if (errorTypeForSync != null) {
                        if (errorTypeForSync!!.length < Int.MAX_VALUE - 100) {
                            errorTypeForSync!!.append("$log$linkBreak")
                        }

                    }
                    if (errorTypeForBigData != null) {
                        if (errorTypeForBigData!!.length < Int.MAX_VALUE / 2) {
                            errorTypeForBigData!!.append("$log$linkBreak")
                        }
                    }
                }
            }

            //调用后台接口上传log
            override fun onError(errorType: String?) {
                systemError {
                    when (errorType) {
                        ErrorLogTyepAttr.TYPE_MODEL_DISCOVERY_SERVICE -> {
                            sendLog(ERROR_TYPE_FOR_CONNECT, errorType)
                        }
                        ErrorLogTyepAttr.TYPE_MODEL_TIME_OUT -> {
                            sendLog(ERROR_TYPE_FOR_CONNECT, errorType)
                        }
                        ErrorLogTyepAttr.TYPE_MODEL_BLUETOOTH_DISCONNECT -> {
                            sendLog(ERROR_TYPE_FOR_CONNECT, errorType)
                        }
                        ErrorLogTyepAttr.TYPE_MODEL_MTU -> {
                            sendLog(ERROR_TYPE_FOR_CONNECT, errorType)
                        }
                        ERROR_MODE_BIND_DEVICE_FOR_BIND_INFO, ERROR_MODE_BIND_DEVICE_FOR_DEVICE,
                        ERROR_MODE_BIND_DEVICE_FOR_SERVICE_1, ERROR_MODE_BIND_DEVICE_FOR_SERVICE_2,
                        ERROR_MODE_BIND_DEVICE_FOR_SERVICE_3 -> {
                            sendLog(ERROR_TYPE_FOR_BINDDEVICE, errorType)
                        }
                        ERROR_MODE_SYNC_TIME_OUT -> {
                            sendLog(ERROR_TYPE_FOR_SYNC, errorType)
                        }
                        ERROR_MODE_DOWNLOAD_FAIL -> {
                            sendLog(currentBigData, errorType)
                        }
                        ERROR_MODE_REQUEST_BACKGROUND_DATA -> {
                            sendLog(currentBigData, errorType)
                        }
                        ERROR_MODE_TRANSMISSION_TIMEOUT -> {
                            sendLog(currentBigData, errorType)
                        }
                        ERROR_MODE_OTHER -> {
                            sendLog(currentBigData, errorType)
                        }
                        OTA_LOSS -> {
                            sendLog(currentBigData, errorType)
                        }
                    }

                }
            }

            override fun onModelFinish(Type: String?) {
                systemError {

                }
            }

        }
    }

    private fun sendLog(type: String, typeId: String) {
        scope.launch {
            when (type) {
                ERROR_TYPE_FOR_CONNECT -> {
                    if (errorTypeForConnect != null) {
                        startUpLoadLog(type, typeId, errorTypeForConnect!!)
                        clearErrorConnect()
                    }
                }
                ERROR_TYPE_FOR_BINDDEVICE -> {
                    if (errorTypeForBindDevice != null) {
                        startUpLoadLog(type, typeId, errorTypeForBindDevice!!)
                        clearErrorBindDevice()
                    }
                }
                ERROR_TYPE_FOR_SYNC -> {
                    if (errorTypeForSync != null) {
                        startUpLoadLog(type, typeId, errorTypeForSync!!)
                        clearErrorSync()
                    }
                }
                ERROR_TYPE_FOR_OTA -> {
                    if (errorTypeForBigData != null) {
                        startUpLoadLog(type, typeId, errorTypeForBigData!!)
                        clearErrorBigData()
                    }
                }
                ERROR_TYPE_FOR_DIAL -> {
                    if (errorTypeForBigData != null) {
                        startUpLoadLog(type, typeId, errorTypeForBigData!!)
                        clearErrorBigData()
                    }
                }
                ERROR_TYPE_FOR_AGPS -> {
                    if (errorTypeForBigData != null) {
                        startUpLoadLog(type, typeId, errorTypeForBigData!!)
                        clearErrorBigData()
                    }
                }
            }
        }
    }

    /**
     * 发送邮箱错误异常日志
     */
    public fun sendEmailError(email: String, type: String) {
        scope.launch {
            val log =
                StringBuffer("time:${com.blankj.utilcode.util.TimeUtils.getNowString(com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COMM))} , Error email:" + email)
            startUpLoadLog(ERROR_TYPE_EMAIL, type, log)
        }
    }

    /**
     * 发送网络错误日志
     */
    public fun sendHttpError() {
        val httpLogDir = BaseApplication.mContext.getExternalFilesDir("log/http")
        if (httpLogDir != null && httpLogDir.isDirectory && httpLogDir.exists()) {
            ThreadUtils.executeByIo(object : ThreadUtils.Task<StringBuffer>() {
                override fun doInBackground(): StringBuffer {
                    val logData = StringBuffer()
                    val logList = com.blankj.utilcode.util.FileUtils.listFilesInDir(httpLogDir)
                    if (!logList.isNullOrEmpty()) {
                        for (logFile in logList) {
                            logData.append(com.blankj.utilcode.util.FileIOUtils.readFile2String(logFile))
                        }
                    }
                    return logData
                }

                override fun onCancel() {

                }

                override fun onFail(t: Throwable?) {
                }

                override fun onSuccess(logData: StringBuffer?) {
                    if (!logData.isNullOrEmpty()) {
                        scope.launch {
                            startUpLoadLog(ERROR_TYPE_HTTP, HTTP_ERROR, logData)
                        }
                    }
                }

            })

        }
    }

    fun getErrorTypeConnectLog(): StringBuffer {
        return errorTypeForConnect ?: StringBuffer("")
    }

    fun clearErrorConnect() {
        if (errorTypeForConnect != null) {
            errorTypeForConnect!!.setLength(0)
            errorTypeForConnect = null
        }
    }

    fun clearErrorBindDevice() {
        if (errorTypeForBindDevice != null) {
            errorTypeForBindDevice!!.setLength(0)
            errorTypeForBindDevice = null
        }
    }

    fun clearErrorSync() {
        if (errorTypeForSync != null) {
            errorTypeForSync!!.setLength(0)
            errorTypeForSync = null
        }
    }

    fun clearErrorBigData() {
        if (errorTypeForBigData != null) {
            errorTypeForBigData!!.setLength(0)
            errorTypeForBigData = null
        }
    }

    @JvmStatic
    fun initType(type: String) {
        when (type) {
            ERROR_TYPE_FOR_CONNECT -> {
                errorTypeForConnect = null
                errorTypeForConnect = StringBuffer()
//                errorTypeForConnect!!.append("$ERROR_TYPE_FOR_CONNECT$linkBreak")
            }
            ERROR_TYPE_FOR_BINDDEVICE -> {
                errorTypeForBindDevice = null
                errorTypeForBindDevice = StringBuffer()
//                errorTypeForBindDevice!!.append("$ERROR_TYPE_FOR_BINDDEVICE$linkBreak")
            }
            ERROR_TYPE_FOR_SYNC -> {
                errorTypeForSync = null
                errorTypeForSync = StringBuffer()
//                errorTypeForSync!!.append("$ERROR_TYPE_FOR_SYNC$linkBreak")
            }
            ERROR_TYPE_FOR_OTA -> {
                currentBigData = ERROR_TYPE_FOR_OTA
                errorTypeForBigData = null
                errorTypeForBigData = StringBuffer()
//                errorTypeForBigData!!.append("$ERROR_TYPE_FOR_BIGDATA$linkBreak")
            }
            ERROR_TYPE_FOR_DIAL -> {
                currentBigData = ERROR_TYPE_FOR_DIAL
                errorTypeForBigData = null
                errorTypeForBigData = StringBuffer()
            }
            ERROR_TYPE_FOR_AGPS -> {
                currentBigData = ERROR_TYPE_FOR_AGPS
                errorTypeForBigData = null
                errorTypeForBigData = StringBuffer()
            }
        }
    }

    private fun systemError(block: () -> Unit) {
        try {
            runBlocking { block() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun startUpLoadLog(type: String, typeId: String, log: StringBuffer) {
        try {
            val bean = ErrorLogBean()
            bean.userId = SpUtils.getValue(SpUtils.USER_ID, "0")
            bean.logModule = type
            bean.moduleId = typeId
            bean.deviceType = Global.deviceType
            bean.deviceVersion = Global.deviceVersion
            bean.appVersion = AppUtils.getAppVersionName()
            bean.appId = CommonAttributes.APP_ID
            bean.phoneModel = android.os.Build.BRAND + " " + android.os.Build.MODEL
            bean.phoneSystem = "2"
            bean.remark = log.toString()
            val result = ErrorLogRetrofitClient.service.upLoadError(
                JsonUtils.getRequestJson(
                    TAG,
                    bean,
                    ErrorLogBean::class.java
                )
            )
            LogUtils.e(TAG, "startUpLoadLog type = $type result = $result")
            if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                if (type == ERROR_TYPE_HTTP) {
                    HttpLog.clearLog()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun onLogResult(msg: String) {
        if (CallBackUtils.errorLogCallBack != null) {
            CallBackUtils.errorLogCallBack.onResult(msg)
        }
    }

    @JvmStatic
    fun onLogError(errorType: String) {
        if (CallBackUtils.errorLogCallBack != null) {
            CallBackUtils.errorLogCallBack.onError(errorType)
        }
    }

    fun onLogModelFinish(type: String) {
        if (CallBackUtils.errorLogCallBack != null) {
            CallBackUtils.errorLogCallBack.onModelFinish(type)
        }
    }
}