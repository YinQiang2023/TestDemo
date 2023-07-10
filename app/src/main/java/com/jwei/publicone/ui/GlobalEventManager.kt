package com.jwei.publicone.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.TimeUtils
import com.google.gson.reflect.TypeToken
import com.zhapp.ble.BleBCManager
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThemeManager
import com.zhapp.ble.callback.AgpsCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.utils.UnitConversionUtils
import com.jwei.publicone.BuildConfig
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.databinding.DialogHeadsetBondFailedBinding
import com.jwei.publicone.databinding.DialogKeExplainBinding
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.dialog.DialogUtils.DialogClickListener
import com.jwei.publicone.dialog.DownloadDialog
import com.jwei.publicone.dialog.customdialog.CustomDialog
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.Response
import com.jwei.publicone.https.download.DownloadListener
import com.jwei.publicone.https.download.DownloadManager
import com.jwei.publicone.https.response.AgpsResponse
import com.jwei.publicone.https.response.FirewareUpgradeResponse
import com.jwei.publicone.ui.device.backgroundpermission.BackgroundPermissionMainActivity
import com.jwei.publicone.ui.device.bean.DeviceSettingBean
import com.jwei.publicone.ui.device.bean.NotifyItem
import com.jwei.publicone.ui.device.setting.notify.MsgNotifySetActivity
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.*
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.FileUtils
import com.jwei.publicone.utils.LogUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.utils.manager.DevSportManager
import com.jwei.publicone.viewmodel.DeviceModel
import com.jwei.publicone.viewmodel.UserModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Android on 2022/4/7.
 * 全局事件处理
 */
object GlobalEventManager {
    private val TAG = GlobalEventManager.javaClass.simpleName

    fun initEventBus() {
        EventBus.getDefault().register(this)
    }


    //获取最上层activity
    private lateinit var topActivity: WeakReference<AppCompatActivity>

    /**
     * 获取上下文
     * */
    private fun getContext(): Activity? {
        val top = ActivityUtils.getTopActivity()
        if (top != null && !top.isDestroyed) {
            return top
        }
        return null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            //region 连接设备同步完成后
            EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE -> {
                //连接设备刷新数据完成
                //有保活说明弹窗未关闭,暂缓执行
                if (!SpUtils.getSPUtilsInstance().getBoolean(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, true)) {
                    isWiatKlDialog = true
                    return
                }
                //检测固件升级
                if (isCanShowFirmwareUpgrade) {
                    isCanShowFirmwareUpgrade = false
                    checkFirmwareUpgrade()
                    return
                }
                //请求更新agps
                if (isCanUpdateAgps) {
                    isCanUpdateAgps = false
                    val deviceSettingBean = JSON.parseObject(
                        SpUtils.getValue(SpUtils.DEVICE_SETTING, ""),
                        DeviceSettingBean::class.java
                    )
                    if (deviceSettingBean != null && deviceSettingBean.functionRelated.AGPS) {
                        CallBackUtils.agpsCallBack = MyAgpsCallBask()
                        devAgpsLog.startTime = TrackingLog.getNowString()
                        ControlBleTools.getInstance().requestAgpsState(object : SendCmdStateListener() {
                            override fun onState(state: SendCmdState?) {
                                if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, TrackingLog.getStartTypeTrack("AGPS"), isStart = true)
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, devAgpsLog.apply {
                                        log = "state : $state \n 设备请求失败/超时"
                                        endTime = TrackingLog.getNowString()
                                    }, "2010", true)

                                    //重新执行同步完成事件
                                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                                }
                            }
                        })
                    } else {
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                    }
                    return
                }

                //通知用户引导提示 HomeActivity 弹
                if (!SpUtils.getSPUtilsInstance().getBoolean(SpUtils.NOTIFY_USER_GUIDANCE_TIPS, true)) {
                    notifyUserGuidanceTips()
                }

            }
            //endregion

            //region 设备连接上 检测是否刚绑定设备
            EventAction.ACTION_DEVICE_CONNECTED -> {
                //执行保活提示
                AppUtils.tryBlock {
                    executionKeepLiveExplanation()
                }
            }
            //endregion

            //region 发送短信无权限
            EventAction.ACTION_SMS_NOT_PER -> {
                //val phoneDtoModel = msg.getObj() as PhoneDtoModel
                val act = ActivityUtils.getTopActivity()
                if (act != null && !act.isDestroyed && !act.isFinishing) {
                    showSmsNoPerDialog(act)
                } else {
                    ToastUtils.showToast(BaseApplication.mContext.getString(R.string.reply_error_tips))
                }
            }
            //endregion

            //region 检测bt配对
            EventAction.ACTION_HEADSET_BOND -> {
                val bleName = msg.obj as String?
                LogUtils.i(TAG, "ACTION_HEADSET_BOND bleMac = $bleName")
                AppUtils.tryBlock {
                    topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
                    if (topActivity.get() == null) return@tryBlock
                    showHeadsetBondLoading(bleName)
                }
            }
            //endregion

            //region 通话蓝牙配对失败
            EventAction.ACTION_HEADSETBOND_FAILED -> {
                topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
                if (topActivity.get() == null) return
                val hBleName = msg.obj.toString()
                if (!TextUtils.isEmpty(hBleName) && !isHeadsetBondFailedDialog) {
                    showHeadsetBondFailedDialog(hBleName)
                }
            }
            //endregion

            //region 蓝牙状态改变
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                if (msg.arg == BleCommonAttributes.STATE_DISCONNECTED) {
                    if (isOtaSending) {
                        isOtaSending = false
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "1913", true)
                    }
                    if (isAGPSSending) {
                        isAGPSSending = false
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "2014", true)
                    }
                    if (DevSportManager.isDeviceSporting) {
                        DevSportManager.isDeviceSporting = false
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SPORT, TrackingLog.getAppTypeTrack("运动过程中蓝牙断连"), "2117", true)
                    }
                }
            }
            //endregion

        }
    }

    //region 检测固件升级
    //是否固件升级
    var isCanShowFirmwareUpgrade = true

    //是否升级固件中
    var isUpload = false

    //是否传输ota中
    private var isOtaSending = false

    private var firmwareObserver: Observer<FirewareUpgradeResponse?>? = null

    private val checkOtaLog by lazy { TrackingLog.getSerTypeTrack("app请求获取OTA文件", "固件升级", "ffit/firmware/getFirewareUpgradeVersion") }

    private fun checkFirmwareUpgrade() {
        topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
        if (topActivity.get() == null) {
            return
        }
        val deviceModel: DeviceModel = ViewModelProvider(topActivity.get()!!).get(DeviceModel::class.java)

        if (firmwareObserver != null) {
            deviceModel.firewareUpgradeData.removeObserver(firmwareObserver!!)
        }
        firmwareObserver = Observer {
            if (it == null) return@Observer
            checkOtaLog.endTime = TrackingLog.getNowString()


            if (it.versionBefore != it.versionAfter && it.versionUrl.isNotEmpty() && TextUtils.equals(it.mustUpdate, "1") && !isUpload) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getStartTypeTrack("OTA"), isStart = true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, checkOtaLog)
                showUpdateDialog(deviceModel, it.remark)
            } else {
                if (it.id.isEmpty()) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getStartTypeTrack("OTA"), isStart = true)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, checkOtaLog.apply { log += "请求失败/超时" }, "1910", true)
                }
                //重新执行同步完成事件
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
            }
        }
        deviceModel.firewareUpgradeData.observe(topActivity.get()!!, firmwareObserver!!)
        deviceModel.checkFirewareUpgrade(checkOtaLog)
    }

    private fun showUpdateDialog(deviceModel: DeviceModel, remark: String) {
        topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
        if (topActivity.get() == null) return
        isUpload = true
        val dialog = DialogUtils.showDialogTwoBtn(
            topActivity.get(),
            BaseApplication.mContext.getString(R.string.find_new_ota_version),
            StringBuilder().append(BaseApplication.mContext.getString(R.string.find_new_ota_version_tips))/*.append("\n").append(remark)*/.toString(),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.upgrade_immediately),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    NetworkUtils.isAvailableAsync { isAvailable ->
                        if (!isAvailable) {
                            ToastUtils.showToast(R.string.not_network_tips)
                            isUpload = false
                            return@isAvailableAsync
                        }
                        showDownloadingDialog(deviceModel)
                    }
                }

                override fun OnCancel() {
                    isUpload = false
                    //重新执行同步完成事件
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                }
            })
        dialog.show()
    }

    /**
     * 固件文件下载
     */
    private fun showDownloadingDialog(deviceModel: DeviceModel) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            isUpload = false
            return
        }
        if (!AppUtils.isOpenBluetooth()) {
            ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
            isUpload = false
            return
        }
        isUpload = true
        if (topActivity.get() == null) {
            return
        }
        val downloadDialog = DownloadDialog(
            topActivity.get(),
            BaseApplication.mContext.getString(R.string.theme_center_dial_down_load_title), ""
        )
        downloadDialog.showDialog()
        val downloadOtaLog = TrackingLog.getSerTypeTrack("下载ota文件", "下载", "${deviceModel.firewareUpgradeData.value?.versionUrl}")
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_OTA)
        ErrorUtils.onLogResult("ota start")
        deviceModel.firewareUpgradeData.value?.let {
            DownloadManager.download(it.versionUrl, listener = object : DownloadListener {
                override fun onStart() {
                    LogUtils.d(TAG, "ota showDownloadingDialog onStart", true)
                    downloadOtaLog.log += "ota onStart"
                }

                @SuppressLint("SetTextI18n")
                override fun onProgress(totalSize: Long, currentSize: Long) {
                    LogUtils.d(TAG, "showDownloadingDialog onProgress totalSize: $totalSize,currentSize: $currentSize")
                    downloadDialog.progressView?.max = totalSize.toInt()
                    downloadDialog.progressView?.progress = currentSize.toInt()
                    downloadDialog.tvProgress?.text = "${((currentSize * 1f / totalSize) * 100).toInt()}%"
                    downloadDialog.tvSize?.text = "${FileUtils.getSize(currentSize)}/${FileUtils.getSize(totalSize)}"
                    downloadOtaLog.log += "\n${((currentSize * 1f / totalSize) * 100).toInt()}%"
                }

                override fun onFailed(msg: String) {
                    LogUtils.e(TAG, "ota showUpdateDialog DownloadManagerUtils onFailed: $msg", true)
                    ErrorUtils.onLogResult("ota Download onFailed :$msg")
                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)

                    downloadOtaLog.endTime = TrackingLog.getNowString()
                    downloadOtaLog.log += "\nota Download onFailed :$msg"
                    downloadOtaLog.serResult = "失败"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog.apply { log += "\n 下载文件失败/超时" }, "1911", isEnd = true)

                    downloadDialog.cancel()
                    topActivity.get()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                        FileUtils.deleteAll(it.path)
                    }
                    showUpdateFailedDialog(deviceModel)
                }

                override fun onSucceed(path: String) {
                    LogUtils.e(TAG, "showUpdateDialog DownloadManagerUtils onSuccess", true)

                    downloadDialog.cancel()
                    if (!path.isNullOrEmpty()) {

                        downloadOtaLog.endTime = TrackingLog.getNowString()
                        downloadOtaLog.log += "\nota Download onSuccess"
                        downloadOtaLog.serResult = "成功"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog)

                        deviceLargeFileState(deviceModel, path)
                    } else {
                        ErrorUtils.onLogResult("ota file path is null")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
                        showUpdateFailedDialog(deviceModel)
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog.apply {
                            log = "\n ota file path is null \n 下载文件失败/超时"
                        }, "1911", isEnd = true)
                    }
                }
            })
        }
    }

    /**
     * 升级失败提示
     */
    private fun showUpdateFailedDialog(deviceModel: DeviceModel) {
        isUpload = false
        topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
        if (topActivity.get() == null) return
        val dialog = DialogUtils.showDialogTwoBtn(
            topActivity.get(),
            /*getString(R.string.dialog_title_tips)*/null,
            BaseApplication.mContext.getString(R.string.update_failed_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            /*if (AppUtils.isOpenBluetooth() && ControlBleTools.getInstance().isConnect) BaseApplication.mContext.getString(R.string.reset_update) else BaseApplication.mContext.getString(
                R.string.dialog_confirm_btn
            )*/BaseApplication.mContext.getString(R.string.reset_update),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    NetworkUtils.isAvailableAsync { isAvailable ->
                        if (!isAvailable) {
                            ToastUtils.showToast(R.string.not_network_tips)
                            return@isAvailableAsync
                        }
                        if (AppUtils.isOpenBluetooth() && ControlBleTools.getInstance().isConnect) {
                            showDownloadingDialog(deviceModel)
                        } else {
                            ToastUtils.showToast(R.string.device_no_connection)
                        }
                    }
                }

                override fun OnCancel() {
                    //重新执行同步完成事件
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                }
            })
        dialog.show()
    }


    private val fileStatusTrackingLog by lazy { TrackingLog.getDevTyepTrack("请求设备传文件状态", "获取发送OTA文件状态", "PREPARE_OTA_VALUE") }

    /**
     * 请求设备传文件状态
     */
    private fun deviceLargeFileState(deviceModel: DeviceModel, path: String, version: String = "111", md5: String = "222") {
        fileStatusTrackingLog.startTime = TrackingLog.getNowString()
        fileStatusTrackingLog.log = "isForce:true"
        ControlBleTools.getInstance().getDeviceLargeFileState(true, version, md5, MyDeviceLargeFileStatusListener(deviceModel, path))
    }

    class MyDeviceLargeFileStatusListener(
        model: DeviceModel,
        var path: String
    ) : DeviceLargeFileStatusListener {
        private lateinit var deviceModel: DeviceModel

        init {
            deviceModel = model
        }

        @SuppressLint("SuspiciousIndentation")
        override fun onSuccess(statusValue: Int, statusName: String?) {
            topActivity.get()?.apply {
                LogUtils.e(TAG, "showUpdateDialog getDeviceLargeFileState onSuccess $statusName", true)
                when (statusName) {
                    "READY" -> {
                        val fileByte: ByteArray? = FileUtils.getBytes(path)
                        if (fileByte == null) {
                            ErrorUtils.onLogResult("ota fileByte is not null")
                            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
                            showUpdateFailedDialog(deviceModel)
                            return
                        }
                        fileStatusTrackingLog.apply {
                            endTime = TrackingLog.getNowString()
                            devResult = "statusValue:$statusValue,statusName : $statusName"
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, this)
                        }
                        uploadFile(deviceModel, fileByte)
                    }
                    "BUSY" -> {
                        ToastUtils.showToast(R.string.ota_device_busy_tips)
                    }
                    "DOWNGRADE", "DUPLICATED", "LOW_STORAGE" -> {
                        ToastUtils.showToast(R.string.ota_device_request_failed_tips)
                    }
                    "LOW_BATTERY" -> {
                        ToastUtils.showToast(R.string.ota_device_low_power_tips)
                    }
                }
                if (statusName != "READY") {
                    isUpload = false
                    showUpdateFailedDialog(deviceModel)
                    if (statusName != "LOW_BATTERY") {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, fileStatusTrackingLog.apply {
                            endTime = TrackingLog.getNowString()
                            devResult = "statusValue:$statusValue,statusName : $statusName"
                            log += "\n请求文件状态超时/失败"
                        }, "1912", true)
                    }
                }
            }
        }

        override fun timeOut() {
            topActivity.get()?.apply {
                LogUtils.e(TAG, "showUpdateDialog getDeviceLargeFileState timeOut")
                ErrorUtils.onLogResult("ota getDeviceLargeFileState timeOut")
                if (ControlBleTools.getInstance().isConnect) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, fileStatusTrackingLog.apply {
                        log += "\nota getDeviceLargeFileState timeOut \n请求文件状态超时/失败"
                    }, "1912", true)
                }
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                //ToastUtils.showToast(R.string.ota_device_timeout_tips)
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    FileUtils.deleteAll(it.path)
                }
                showUpdateFailedDialog(deviceModel)
            }
        }
    }

    private val uploadOtaTrackingLog by lazy { TrackingLog.getDevTyepTrack("传输OTA文件", "上传大文件数据", "sendThemeByProto4") }

    /**
     * 发送文件至设备
     */
    private fun uploadFile(model: DeviceModel, fileByte: ByteArray) {
        uploadDialog = DownloadDialog(topActivity.get()!!, BaseApplication.mContext.getString(R.string.theme_fireware_update_title), "")
        uploadDialog?.showDialog()
        uploadDialog?.tvSize?.text = BaseApplication.mContext.getString(R.string.theme_center_dial_up_load_tips)
        isOtaSending = true
        uploadOtaTrackingLog.startTime = TrackingLog.getNowString()
        uploadOtaTrackingLog.log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_WATCH},fileByte:${fileByte.size},isResumable:true"
        ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_OTA, fileByte, true, MyUploadBigDataListener(model))
    }

    class MyUploadBigDataListener(model: DeviceModel) : UploadBigDataListener {
        private lateinit var deviceModel: DeviceModel

        init {
            deviceModel = model
        }

        override fun onSuccess() {
            LogUtils.e(TAG, "showUpdateDialog startUploadBigData onSuccess", true)
            uploadDialog?.cancel()
            isOtaSending = false
            isUpload = false

            uploadOtaTrackingLog.log += "\nota startUploadBigData onSuccess"
            uploadOtaTrackingLog.endTime = TrackingLog.getNowString()
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, uploadOtaTrackingLog)
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getEndTypeTrack("OTA"), isEnd = true)

            topActivity.get()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                FileUtils.deleteAll(it.path)
            }
            if (ThemeManager.getInstance().packetLossTimes > 0) {
                ThreadUtils.runOnUiThreadDelayed({
                    ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                }, 100)
            }
            ControlBleTools.getInstance().disconnect()
            //3s后重连
            ThreadUtils.runOnUiThreadDelayed({
                SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
            }, 5000)
            ErrorUtils.clearErrorBigData()
        }

        @SuppressLint("SetTextI18n")
        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            ThreadUtils.runOnUiThread {
                val percentage = (curPiece / dataPackTotalPieceLength.toFloat()) * 100
                uploadDialog?.progressView?.max = dataPackTotalPieceLength
                uploadDialog?.progressView?.progress = curPiece
                uploadDialog?.tvProgress?.text = "${percentage.toInt()}%"
            }
            uploadOtaTrackingLog.log += "\n${((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()}%"
        }

        override fun onTimeout() {
            if (isUpload) {
                LogUtils.e(TAG, "showUpdateDialog startUploadBigData onTimeout", true)
                ErrorUtils.onLogResult("ota startUploadBigData onTimeout")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                uploadOtaTrackingLog.log += "ota startUploadBigData onTimeout; isConnect:${ControlBleTools.getInstance().isConnect}"
                uploadOtaTrackingLog.endTime = TrackingLog.getNowString()
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, uploadOtaTrackingLog.apply {
                    log += "\n 发送响应超时/失败"
                }, "1914", true)
            }
            isUpload = false
            isOtaSending = false
            ThreadUtils.runOnUiThread {
                uploadDialog?.isShowing()?.let {
                    if (it) {
                        uploadDialog?.cancel()
                        ToastUtils.showToast(
                            R.string.ota_device_timeout_tips
                        )
                        showUpdateFailedDialog(deviceModel)
                    }
                }

            }
            topActivity.get()?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                FileUtils.deleteAll(it.path)
            }
        }
    }
    //endregion


    //region agps更新

    var isCanUpdateAgps = true
    private var agpsdialog: Dialog? = null
    private var agpsObserver: Observer<Response<AgpsResponse>>? = null
    private var uploadDialog: DownloadDialog? = null

    //是否agps更新中
    private var isAGPSUpDating = false

    //是否下载agps中
    private var isAGPSDownloading = false

    //是否推送agps中
    private var isAGPSSending = false

    //是否等待保活弹窗
    private var isWiatKlDialog = false

    //是否配对失败弹窗
    private var isHeadsetBondFailedDialog = false

    private val devAgpsLog by lazy { TrackingLog.getDevTyepTrack("查询AGPS状态", "请求设备是否需要更新AGPS", "REQUEST_AGPS_STATE") }

    private val serAgpsLog by lazy { TrackingLog.getSerTypeTrack("后台请求获取AGPS文件", "获取博通GPS文件", "ffit/bream/downloadLto") }

    class MyAgpsCallBask : AgpsCallBack {
        override fun onRequestState(isNeed: Boolean) {
            LogUtils.e(TAG, "requestAgpsState  isNeed --> $isNeed,isAGPSUpDatIng -->$isAGPSUpDating", true)
            topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
            if (topActivity.get() == null) return
            val viewModel: UserModel = ViewModelProvider(topActivity.get()!!).get(UserModel::class.java)
            if (isNeed && !isAGPSUpDating) {
                ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_AGPS)
                ErrorUtils.onLogResult("请求后台AGPS数据 isNeed = $isNeed")
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, TrackingLog.getStartTypeTrack("AGPS"), isStart = true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, devAgpsLog.apply {
                    devResult = "isNeed = $isNeed"
                    endTime = TrackingLog.getNowString()
                })
                if (agpsObserver != null) {
                    viewModel.requestAgpsInfo.removeObserver(agpsObserver!!)
                }
                agpsObserver = Observer {
                    if (it != null) {
                        serAgpsLog.apply {
                            endTime = TrackingLog.getNowString()
                        }
                        if (it.code != HttpCommonAttributes.REQUEST_SUCCESS) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, serAgpsLog.apply {
                                log += "\n后台请求失败/超时"
                            }, "2011", true)
                        } else {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, serAgpsLog)
                        }
                        if (it.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                            if (!isAGPSUpDating) {
                                isAGPSUpDating = true
                                LogUtils.e(TAG, "downLoadAgps  ------->")
                                NetworkUtils.isAvailableAsync { isAvailable ->
                                    if (isAvailable) {
                                        getContext()?.let { context ->
                                            agpsdialog = DialogUtils.showLoad(context)
                                            agpsdialog?.show()
                                        }
                                        downLoadAgps(it.data.dataUrl)
                                    } else {
                                        isAGPSUpDating = false
                                        //重新执行同步完成事件
                                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                                    }
                                }
                            }
                        } else {
                            isAGPSUpDating = false
                            //重新执行同步完成事件
                            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                            getContext()?.let { context ->
                                ToastUtils.showToast(context.getString(R.string.err_network_tips))
                            }
                        }
                        //防止回灌！！
                        viewModel.requestAgpsInfo.postValue(null)
                        if (agpsObserver != null) {
                            viewModel.requestAgpsInfo.removeObserver(agpsObserver!!)
                        }
                    }
                }
                viewModel.requestAgpsInfo.observe(topActivity.get()!!, agpsObserver!!)

                NetworkUtils.isAvailableAsync { isAvailable ->
                    if (isAvailable) {
                        viewModel.requestAgpsInfo(serAgpsLog)
                    } else {
                        isAGPSUpDating = false
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                    }
                }
            } else {
                //重新执行同步完成事件
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
            }
        }
    }

    private fun downLoadAgps(url: String) {
        if (getContext() == null) {
            isAGPSUpDating = false
            //重新执行同步完成事件
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
            return
        }
        getContext()?.let { context ->
            if (!isAGPSDownloading) {
                isAGPSDownloading = true
                val downloadLog = TrackingLog.getSerTypeTrack("下载AGPS文件", "下载", "${url}")
                DownloadManager.download(url, listener = object : DownloadListener {
                    override fun onStart() {
                        com.blankj.utilcode.util.LogUtils.d(TAG, "download Agps onStart")
                        downloadLog.log += "ota onStart"
                    }

                    override fun onProgress(totalSize: Long, currentSize: Long) {
                        com.blankj.utilcode.util.LogUtils.d(TAG, "download Agps onProgress totalSize: $totalSize,currentSize: $currentSize")
                        downloadLog.log += "\n${((currentSize * 1f / totalSize) * 100).toInt()}%"
                    }

                    override fun onFailed(msg: String) {
                        LogUtils.e(TAG, "download Agps Failed: $msg", true)
                        isAGPSUpDating = false
                        isAGPSDownloading = false
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                        DialogUtils.dismissDialog(agpsdialog)
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                            FileUtils.deleteAll(it.path)
                        }
                        ToastUtils.showToast(context.getString(R.string.err_network_tips))
                        ErrorUtils.onLogResult("下载AGPS数据失败:$msg")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)

                        downloadLog.endTime = TrackingLog.getNowString()
                        downloadLog.log += "\nagps Download onFailed :$msg"
                        downloadLog.serResult = "失败"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, downloadLog.apply { log += "下载文件失败/超时" }, "2012", isEnd = true)
                    }

                    override fun onSucceed(path: String) {
                        LogUtils.d(TAG, "download Agps onSucceed: $path", true)
                        SpUtils.setValue(
                            SpUtils.AGPS_DOWNLOAD_TIME,
                            TimeUtils.getNowString(com.jwei.publicone.utils.TimeUtils.getSafeDateFormat(com.jwei.publicone.utils.TimeUtils.DATEFORMAT_COMM))
                        )
                        isAGPSDownloading = false
                        if (path != null) {
                            downloadLog.endTime = TrackingLog.getNowString()
                            downloadLog.log += "\nota Download onSuccess"
                            downloadLog.serResult = "成功"
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, downloadLog)

                            sendAgpsState(path)
                        } else {
                            isAGPSUpDating = false
                            //重新执行同步完成事件
                            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))

                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, downloadLog.apply {
                                log += "\nAGPS file path is null\n 下载文件失败/超时"
                            }, "2012", isEnd = true)
                        }
                    }

                })
            }
        }
    }

    private val agpsFileStatusTrackingLog by lazy { TrackingLog.getDevTyepTrack("请求设备传文件状态", "获取发送大文件文件状态", "PREPARE_OTA_VALUE") }

    private fun sendAgpsState(filePath: String) {
        if (getContext() == null) {
            isAGPSUpDating = false
            //重新执行同步完成事件
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
            return
        }
        getContext()?.let { context ->
            if (!isAGPSSending) {
                isAGPSSending = true
                DialogUtils.dismissDialog(agpsdialog)

                agpsFileStatusTrackingLog.startTime = TrackingLog.getNowString()
                agpsFileStatusTrackingLog.log = "isForce:true"

                ControlBleTools.getInstance().getDeviceLargeFileState(true, "1", "1", object : DeviceLargeFileStatusListener {
                    override fun onSuccess(statusValue: Int, statusName: String?) {
                        LogUtils.e(TAG, "sendAgpsState  ------->$statusName", true)
                        agpsFileStatusTrackingLog.apply {
                            endTime = TrackingLog.getNowString()
                            devResult = "statusValue:$statusValue,statusName : $statusName"
                        }
                        if (statusName != "READY" && statusName != "LOW_BATTERY") {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, agpsFileStatusTrackingLog.apply {
                                log = "status == $statusName != READY \n请求文件状态超时/失败"
                            }, "2013", true)
                        } else {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, agpsFileStatusTrackingLog)
                        }
                        when (statusName) {
                            "READY" -> {
                                val fileByte: ByteArray? = FileUtils.getBytes(filePath)
                                if (fileByte == null) {
                                    ToastUtils.showToast(context.getString(R.string.agps_update_failed_tips))
                                    isAGPSUpDating = false
                                    //重新执行同步完成事件
                                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                                    return
                                }
                                uploadDialog?.cancel()
                                val topContext = getContext()
                                if (topContext == null) {
                                    isAGPSUpDating = false
                                    //重新执行同步完成事件
                                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                                    return
                                }
                                uploadDialog = DownloadDialog(topContext, context.getString(R.string.agps_updating_tips), "")
                                uploadDialog?.showDialog()
                                sendAgps(fileByte)
                            }
                            "BUSY" -> {
                                ToastUtils.showToast(R.string.ota_device_busy_tips)
                                isAGPSUpDating = false
                                isAGPSSending = false
                                //重新执行同步完成事件
                                EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                            }
                            "DOWNGRADE", "DUPLICATED", "LOW_STORAGE" -> {
                                ToastUtils.showToast(R.string.ota_device_request_failed_tips)
                                isAGPSUpDating = false
                                isAGPSSending = false
                                //重新执行同步完成事件
                                EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                            }
                            "LOW_BATTERY" -> {
                                ToastUtils.showToast(R.string.ota_device_low_power_tips)
                                isAGPSUpDating = false
                                isAGPSSending = false
                                //重新执行同步完成事件
                                EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                            }
                        }
                    }

                    override fun timeOut() {
                        ToastUtils.showToast(R.string.agps_update_failed_tips)
                        uploadDialog?.cancel()
                        isAGPSUpDating = false
                        isAGPSSending = false
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                        if (ControlBleTools.getInstance().isConnect) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, TrackingLog.getAppTypeTrack("请求文件状态超时/失败").apply {
                                log = "timeOut"
                            }, "2013", true)
                        }
                    }

                })
            }
        }
    }

    private val uploadAgpsTrackingLog by lazy { TrackingLog.getDevTyepTrack("传输AGPS文件", "上传大文件数据", "sendThemeByProto4") }

    private fun sendAgps(fileByte: ByteArray) {
        if (getContext() == null) {
            ToastUtils.showToast(R.string.agps_update_failed_tips)
            uploadDialog?.cancel()
            isAGPSUpDating = false
            isAGPSSending = false
            //重新执行同步完成事件
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
            return
        }
        getContext()?.let { context ->
            LogUtils.e(TAG, "sendAgps  -------> size == ${fileByte.size}", true)
            uploadAgpsTrackingLog.startTime = TrackingLog.getNowString()
            uploadAgpsTrackingLog.log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_LTO},fileByte:${fileByte.size},isResumable:true"
            ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_LTO,
                fileByte, true, object : UploadBigDataListener {
                    override fun onSuccess() {
                        uploadDialog?.cancel()
                        isAGPSUpDating = false
                        isAGPSSending = false
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                        LogUtils.e(TAG, "sendAgps startUploadBigData onSuccess", true)

                        uploadAgpsTrackingLog.log += "\nota startUploadBigData onSuccess"
                        uploadAgpsTrackingLog.endTime = TrackingLog.getNowString()
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, uploadAgpsTrackingLog)
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, TrackingLog.getEndTypeTrack("AGPS"), isEnd = true)

                        SpUtils.setValue(
                            SpUtils.AGPS_SYNC_TIME,
                            TimeUtils.getNowString(com.jwei.publicone.utils.TimeUtils.getSafeDateFormat(com.jwei.publicone.utils.TimeUtils.DATEFORMAT_COMM))
                        )
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                            FileUtils.deleteAll(it.path)
                        }
                        if (ThemeManager.getInstance().packetLossTimes > 0) {
                            ThreadUtils.runOnUiThreadDelayed({
                                ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                            }, 100)
                        }
                        ToastUtils.showToast(context.getString(R.string.agps_update_success_tips))
                        ErrorUtils.clearErrorBigData()
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                        val percentage = curPiece * 100 / dataPackTotalPieceLength
                        LogUtils.e(TAG, "sendAgps onProgress $percentage", true)
                        ThreadUtils.runOnUiThread {
                            uploadDialog?.progressView?.max = dataPackTotalPieceLength
                            uploadDialog?.progressView?.progress = curPiece
                            uploadDialog?.tvProgress?.text =
                                "${((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()}%"
                            val currentSize =
                                UnitConversionUtils.bigDecimalFormat((curPiece / dataPackTotalPieceLength.toFloat()))
                                    .trim().toFloat() * fileByte.size
                            uploadDialog?.tvSize?.text =
                                "${FileUtils.getSize(currentSize.toLong())}/${FileUtils.getSize(fileByte.size.toLong())}"
                        }
                        uploadAgpsTrackingLog.log += "\n${((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()}%"
                    }

                    override fun onTimeout() {
                        if (isAGPSSending) {
                            LogUtils.e(TAG, "sendAgps startUploadBigData onTimeout", true)
                            ErrorUtils.onLogResult("Agps startUploadBigData onTimeout")
                            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)

                            uploadAgpsTrackingLog.log += "ota startUploadBigData onTimeout ; isConnect:${ControlBleTools.getInstance().isConnect}"
                            uploadAgpsTrackingLog.endTime = TrackingLog.getNowString()
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_AGPS, uploadAgpsTrackingLog.apply {
                                log += "\n 发送响应超时/失败"
                            }, "2015", true)
                        }
                        isAGPSUpDating = false
                        isAGPSSending = false
                        //重新执行同步完成事件
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                        getContext()?.let { context ->
                            ThreadUtils.runOnUiThread {
                                uploadDialog?.cancel()
                                ToastUtils.showToast(R.string.agps_update_failed_tips)
                            }
                            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                                FileUtils.deleteAll(it.path)
                            }
                        }
                    }
                })
        }
    }
    //endregion

    //region 提醒后台保活说明弹窗
    private fun executionKeepLiveExplanation() {
        AppUtils.tryBlock {
            topActivity = WeakReference(ActivityUtils.getTopActivity() as AppCompatActivity)
            if (topActivity.get() == null) return@tryBlock
            val isShowKeepLive = SpUtils.getSPUtilsInstance().getBoolean(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, true)
            if (!isShowKeepLive) {
                showKeepLiveExplanationDialog()
            }
        }
    }

    private lateinit var timer: CountDownTimer
    private fun showKeepLiveExplanationDialog() {
        topActivity.get()?.let { activity ->
            if (!com.blankj.utilcode.util.AppUtils.isAppForeground()) {
                ActivityUtils.addActivityLifecycleCallbacks(activity, object : Utils.ActivityLifecycleCallbacks() {
                    override fun onActivityResumed(act: Activity) {
                        super.onActivityResumed(act)
                        showKeepLiveExplanationDialog()
                        ActivityUtils.removeActivityLifecycleCallbacks(activity)
                    }
                })
                return
            }
            val dialogBinding = DialogKeExplainBinding.inflate(activity.layoutInflater)
            val keExplainDialog = CustomDialog.builder(activity)
                .setContentView(dialogBinding.root)
                .setCancelable(false)
                //.setWidth((ScreenUtils.getScreenWidth() * 0.7).toInt())
                .build()
            keExplainDialog.show()

            if (!BuildConfig.DEBUG) { //debug模式下快速关闭
                dialogBinding.btnGotIt.isEnabled = false
            }
            dialogBinding.btnGotIt.setTextColor(ContextCompat.getColor(activity, R.color.app_index_color_30))//定制
            timer = object : CountDownTimer(3 * 1000L, 1000L) {
                @SuppressLint("SetTextI18n")
                override fun onTick(millisUntilFinished: Long) {
                    dialogBinding.btnGotIt.text = activity.getString(R.string.know) + " (" + (millisUntilFinished / 1000 + 1) + activity.getString(R.string.s) + ")"
                }

                override fun onFinish() {
                    dialogBinding.btnGotIt.isEnabled = true
                    dialogBinding.btnGotIt.setTextColor(ContextCompat.getColor(activity, R.color.app_index_color))//定制
                    dialogBinding.btnGotIt.text = activity.getString(R.string.know)
                }
            }
            timer.start()
            ClickUtils.applySingleDebouncing(dialogBinding.btnGotIt) {
                keExplainDialog.dismiss()
                SpUtils.getSPUtilsInstance().put(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, true)
                ActivityUtils.startActivity(BackgroundPermissionMainActivity::class.java)
                if (isWiatKlDialog) {
                    //重新执行同步完成事件
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                }

            }
        }

    }
    //endregion

    //region 快捷回复短信无权限
    private fun showSmsNoPerDialog(context: Context) {
        DialogUtils.showDialogTwoBtn(
            context,
            "",
            context.getString(R.string.reply_error_tips),
            BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
            BaseApplication.mContext.getString(R.string.running_permission_set),
            object : DialogClickListener {
                override fun OnOK() {
                    com.blankj.utilcode.util.PermissionUtils.launchAppDetailsSettings()
                }

                override fun OnCancel() {}
            }).show()
    }
    //endregion

    //region 通话蓝牙配对
    private var isCreateBonding = false     //是否通话蓝牙配对中
    private var brDialog: Dialog? = null
    private fun showHeadsetBondLoading(bleMac: String?) {
        val headsetMac = if (bleMac.isNullOrEmpty()) {
            SpUtils.getHeadsetMac(SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
        } else {
            SpUtils.getHeadsetMac(bleMac)
        }
        if (headsetMac.isNotEmpty()) {
            topActivity.get()?.let { activity ->
                if (!BleBCManager.getInstance().checkBondByMac(headsetMac)) {   //未配对
                    ToastUtils.showToast(R.string.br_bond_tips, Toast.LENGTH_LONG)
                    brDialog = DialogUtils.showLoad(activity)
                    brDialog!!.setCancelable(true)
                    brDialog!!.show()
                    isCreateBonding = true
                    BleBCManager.getInstance().createBond(headsetMac, SearchHeadsetBondListener(headsetMac))
//                    BleBCManager.getInstance().companionDeviceCreateBond(activity, headsetMac, "", SearchHeadsetBondListener(headsetMac))
                } else {
                    //执行连接，可以不处理结果
                    BleBCManager.getInstance().connectHeadsetBluetoothDevice(headsetMac, MyHeadsetConnectListener(headsetMac))
                }
            }
        }
    }

    /**
     * 扫描点击绑定配对回调
     */
    class SearchHeadsetBondListener(var mac: String) : BleBCManager.BondListener {
        override fun onWaiting() {
            LogUtils.i(TAG, "等待配对中 $mac")
        }

        override fun onBondError(e: Exception?) {
            LogUtils.i(TAG, "配对中异常 $mac $e")
            isCreateBonding = false
            if (brDialog != null && brDialog!!.isShowing) brDialog?.dismiss()
            ToastUtils.showToast(R.string.bond_fail)
            EventBus.getDefault().post(
                EventMessage(
                    EventAction.ACTION_HEADSETBOND_FAILED,
                    SpUtils.getHeadsetName(
                        ControlBleTools.getInstance().currentDeviceMac
                    )
                )
            )
        }

        override fun onBonding() {
            LogUtils.i(TAG, "配对中 $mac")
            if (brDialog != null && brDialog!!.isShowing) brDialog?.dismiss()
        }

        override fun onBondFailed() {
            LogUtils.i(TAG, "配对失败 $mac")
            isCreateBonding = false
            if (brDialog != null && brDialog!!.isShowing) brDialog?.dismiss()
            val hName = if (mac.isNotEmpty()) {
                SpUtils.getHeadsetName(mac)
            } else {
                SpUtils.getHeadsetName(SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
            }
            if (hName.isEmpty()) {
                ToastUtils.showToast(R.string.bond_fail)
            } else {
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_HEADSETBOND_FAILED, hName))
            }
        }

        override fun onBondSucceeded() {
            LogUtils.i(TAG, "配对成功 $mac")
            isCreateBonding = false
            //执行连接，可以不处理结果
            BleBCManager.getInstance().connectHeadsetBluetoothDevice(mac, MyHeadsetConnectListener(mac))
            if (brDialog != null && brDialog!!.isShowing) brDialog?.dismiss()
        }
    }

    /**
     * 连接回调
     */
    class MyHeadsetConnectListener(var mac: String) : BleBCManager.ConnectListener {

        override fun onConnectError(e: Exception?) {
            LogUtils.i(TAG, "连接异常 $mac $e")
        }

        override fun onStartConnect() {
            LogUtils.i(TAG, "开始连接 $mac")
        }

        override fun onConnecting() {
            LogUtils.i(TAG, "连接中 $mac")
        }

        override fun onDisconnecting() {
            LogUtils.i(TAG, "断开连接中 $mac")
        }

        override fun onDisconnected() {
            LogUtils.i(TAG, "连接断开 $mac")
        }

        override fun onConnected() {
            LogUtils.i(TAG, "连接成功 $mac")
        }
    }
    //endregion

    //region 通话蓝牙配对失败
    private fun showHeadsetBondFailedDialog(hBleName: String) {
        com.blankj.utilcode.util.LogUtils.d("配对失败：$hBleName")
        topActivity.get()?.let { activity ->
            val dialogBinding = DialogHeadsetBondFailedBinding.inflate(activity.layoutInflater)
            val bondFailedDialog = CustomDialog.builder(activity)
                .setContentView(dialogBinding.root)
                .setCancelable(false)
                .setWidth((ScreenUtils.getScreenWidth() * 0.8).toInt())
                .build()
            bondFailedDialog.show()
            isHeadsetBondFailedDialog = true
            dialogBinding.ivIcon.setImageDrawable(
                if (AppUtils.isZh(activity))
                    ContextCompat.getDrawable(activity, R.mipmap.icon_headset_bond_hint_zh)
                else
                    ContextCompat.getDrawable(activity, R.mipmap.icon_headset_bond_hint_en)
            )
            dialogBinding.tvHint.text = "${activity.getString(R.string.headset_bond_failed)}”$hBleName“"
            ClickUtils.applySingleDebouncing(dialogBinding.btnGotIt) {
                bondFailedDialog.dismiss()
                isHeadsetBondFailedDialog = false
                //进入蓝牙设置
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(intent)
            }
        }
    }
    //endregion


    //region 通知用户引导提示 HomeActivity 弹
    /**
     * 通知用户引导提示
     */
    private fun notifyUserGuidanceTips() {
        if (!isNotifySwitchOpen()) {
            if (DeviceManager.dataList.size > 0) {
                SpUtils.getSPUtilsInstance().put(SpUtils.NOTIFY_USER_GUIDANCE_TIPS, true)
                showNotifySwitchTips()
            }
        }
    }

    /**
     * 消息通知总开关是否打开
     */
    private fun isNotifySwitchOpen(): Boolean {
        val notifyJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, "")
        val tempList: MutableList<NotifyItem>? = GsonUtils.fromJson(notifyJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
        if (!tempList.isNullOrEmpty()) {
            for (n in tempList) {
                //总开关状态
                if (n.type == 1 && n.isTypeHeader && !n.isCanNext) {
                    return n.isOpen
                }
            }
        }
        return false
    }

    private fun showNotifySwitchTips() {
        for (activity in ActivityUtils.getActivityList()){
            if(activity is HomeActivity){
                DialogUtils.showDialogTwoBtn(
                    activity,
                    null,
                    BaseApplication.mContext.getString(R.string.notify_guided_hint),
                    BaseApplication.mContext.getString(R.string.set_up_later),
                    BaseApplication.mContext.getString(R.string.running_permission_set),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            getContext()?.let {
                                it.startActivity(Intent(it, MsgNotifySetActivity::class.java))
                            }
                        }

                        override fun OnCancel() {
                        }
                    }
                ).show()
            }
        }
    }
    //endregion


    /**
     * 透传ActivityResult
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        BleBCManager.getInstance().dealCompanionDeviceActivityResult(requestCode, resultCode, data)
    }

}