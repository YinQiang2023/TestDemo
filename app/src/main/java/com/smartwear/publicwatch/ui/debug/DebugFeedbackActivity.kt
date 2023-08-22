package com.smartwear.publicwatch.ui.debug

import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.FirmwareLogStateCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityDebugFeedbackBinding
import com.smartwear.publicwatch.databinding.DebugDialogFeedbackIllustratingBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.dialog.customdialog.CustomDialog
import com.smartwear.publicwatch.dialog.customdialog.MyDialog
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.PermissionUtils
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.viewmodel.DeviceModel
import java.io.File
import java.util.*

/**
 * Created by Android on 2022/5/13.
 */
class DebugFeedbackActivity : BaseActivity<ActivityDebugFeedbackBinding, DeviceModel>(
    ActivityDebugFeedbackBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    val loadDialog: Dialog by lazy { DialogUtils.showLoad(this) }

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("问题反馈")
        /*setRightIconOrTitle(rightText = "使用说明") {
            showIllustratingDialog()
        }*/
        setViewsClickListener(this, binding.btnCopy, binding.btnShare, binding.btnShare2, binding.btnShare3, binding.btnShare4, binding.btnShareAll)
    }

    override fun initData() {
        super.initData()
        //用户信息
        binding.tvUserInfo.text = StringBuilder()
            .append(SpUtils.getSPUtilsInstance().getString(SpUtils.USER_NAME)).append(" - ")
            .append(SpUtils.getSPUtilsInstance().getString(SpUtils.USER_ID))
        //手机信息
        binding.tvPhoneInfo.text = StringBuilder()
            .append(DeviceUtils.getManufacturer()).append(" - ")
            .append(DeviceUtils.getModel()).append(" - ")
            .append(DeviceUtils.getSDKVersionCode())
        //app
        val appV = StringBuilder().append(AppUtils.getAppVersionName())
        if (AppUtils.isBetaApp()) {
            appV.append("_Beta")
        }
        binding.tvAppInfo.text = appV
        //设备信息
        binding.tvDeviceInfo.text = StringBuilder()
            .append(SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_NAME)).append(" - ")
            .append(SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_VERSION))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnCopy.id -> {
                val info = StringBuilder().apply {
                    append("反馈时间:\n").append(TimeUtils.getNowString(com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat(com.smartwear.publicwatch.utils.TimeUtils.DATEFORMAT_COMM))).append("\n\n")
                    append("用户信息:\n").append(binding.tvUserInfo.text.toString()).append("\n\n")
                    append("手机信息:\n").append(binding.tvPhoneInfo.text.toString()).append("\n\n")
                    append("APP版本:\n").append(binding.tvAppInfo.text.toString()).append("\n\n")
                    append("设备信息:\n").append(binding.tvDeviceInfo.text.toString()).append("\n\n")
                    if (!TextUtils.isEmpty(binding.etContext.text.toString())) {
                        append("问题描述：\n").append(binding.etContext.text.toString())
                    }
                }.toString()
                LogUtils.e(info)
                ClipboardUtils.copyText(info)
                ToastUtils.showShort("复制成功")

                var intent = Intent(Intent.ACTION_SEND)
                intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, info)
                intent = Intent.createChooser(intent, "分享反馈内容")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            binding.btnShare.id -> {

                    val zipFilePath = getExternalFilesDir("logZip")?.absolutePath + File.separator +
                            "Android_log_" + TimeUtils.date2String(Date(), com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd")) + ".zip"
                    val logDir = getExternalFilesDir("log")?.absolutePath
                    LogUtils.d(logDir + "\n" + zipFilePath)
                    if (logDir != null) {
                        shareZip(logDir, zipFilePath)
                    } else {
                        ToastUtils.showLong("暂无日志可分享。")
                    }
            }
            binding.btnShare2.id -> {

                    val zipFilePath = getExternalFilesDir("logZip")?.absolutePath + File.separator +
                            "Android_log_" + System.currentTimeMillis() + ".zip"
                    val logDir = getExternalFilesDir("log")?.absolutePath
                    LogUtils.d(logDir + "\n" + zipFilePath)
                    if (logDir != null) {
                        shareZip(logDir, zipFilePath, false)
                    } else {
                        ToastUtils.showLong("暂无日志可分享。")
                    }
            }
            binding.btnShare3.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showShort(getString(R.string.device_no_connection))
                    return
                }

                loadDialog.show()
                delayDismissDialog()
                CallBackUtils.setFirmwareLogStateCallBack(object : FirmwareLogStateCallBack {
                    override fun onFirmwareLogState(state: Int) {
                        when (state) {
                            FirmwareLogStateCallBack.FirmwareLogState.START.state -> {
                                LogUtils.d("start upload Firmware log")
                            }
                            FirmwareLogStateCallBack.FirmwareLogState.UPLOADING.state -> {
                                LogUtils.d("uploading Firmware log")
                            }
                            FirmwareLogStateCallBack.FirmwareLogState.END.state -> {
                                LogUtils.d("end upload Firmware log")
                                delayDismissDialog()
                            }
                        }
                    }

                    override fun onFirmwareLogFilePath(filePath: String?) {

                    }

                })
                ControlBleTools.getInstance().getFirmwareLog(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState?) {
                        if (state != SendCmdState.SUCCEED) {
                            ToastUtils.showLong(getString(R.string.set_fail) + ":" + state)
                        }
                    }
                })
            }
            binding.btnShare4.id -> {
                    val zipFilePath = getExternalFilesDir("logZip")?.absolutePath + File.separator +
                            "Device_log_" + System.currentTimeMillis() + ".zip"
                    val logDir = getExternalFilesDir("deviceLog")?.absolutePath
                    LogUtils.d(logDir + "\n" + zipFilePath)
                    if (logDir != null) {
                        shareZip(logDir, zipFilePath, false)
                    } else {
                        ToastUtils.showLong("暂无日志可分享。")
                    }
            }
            binding.btnShareAll.id -> {
                loadDialog.show()
                delayDismissDialog()
                CallBackUtils.setFirmwareLogStateCallBack(object : FirmwareLogStateCallBack {
                    override fun onFirmwareLogState(state: Int) {
                        when (state) {
                            FirmwareLogStateCallBack.FirmwareLogState.START.state -> {
                                LogUtils.d("start upload Firmware log")
                            }
                            FirmwareLogStateCallBack.FirmwareLogState.UPLOADING.state -> {
                                LogUtils.d("uploading Firmware log")
                            }
                            FirmwareLogStateCallBack.FirmwareLogState.END.state -> {
                                LogUtils.d("end upload Firmware log")
                                delayDismissDialog()
                                delayShareAllLog()
                            }
                        }
                    }

                    override fun onFirmwareLogFilePath(filePath: String?) {

                    }

                })
                ControlBleTools.getInstance().getFirmwareLog(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState?) {
                        if (state != SendCmdState.SUCCEED) {
                            ToastUtils.showLong(getString(R.string.set_fail) + ":" + state)
                            delayShareAllLog()
                        }
                    }
                })
            }
        }
    }

    //region 使用说明
    private lateinit var dialogBinding: DebugDialogFeedbackIllustratingBinding
    private lateinit var mReplyMyDialog: MyDialog
    private fun showIllustratingDialog() {
        if (!::mReplyMyDialog.isInitialized) {
            dialogBinding = DebugDialogFeedbackIllustratingBinding.inflate(layoutInflater)
            mReplyMyDialog = CustomDialog
                .builder(this)
                .setContentView(dialogBinding.root)
                .setWidth((ScreenUtils.getScreenWidth() * 0.8).toInt())
                .setHeight((ScreenUtils.getScreenHeight()*0.8).toInt())
                .setCancelable(false)
                .build()
            dialogBinding.btnTvLeft.setOnClickListener { mReplyMyDialog.dismiss() }
            dialogBinding.btnTvRight.setOnClickListener { mReplyMyDialog.dismiss() }

        }
        mReplyMyDialog.show()
    }
    //endregion


    //region 延迟关闭等待弹窗
    var delayDismissDialogTask: DelayDismissDialogTask? = null

    fun delayDismissDialog(){
        if(delayDismissDialogTask!=null){
            ThreadUtils.cancel(delayDismissDialogTask)
        }
        delayDismissDialogTask = DelayDismissDialogTask()
        ThreadUtils.executeByIo(delayDismissDialogTask)
    }

    inner class DelayDismissDialogTask : ThreadUtils.SimpleTask<Int>() {
        override fun doInBackground(): Int {
            var timer = 3
            while (timer > 0) {
                timer--
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            if (!isFinishing && !isDestroyed && loadDialog.isShowing) {
                DialogUtils.dismissDialog(loadDialog)
            }
        }

    }
    //endregion

    //region 延迟关闭等待弹窗
    var delayShareAllLogTask: DelayShareAllLogTask? = null

    fun delayShareAllLog(){
        if(delayShareAllLogTask!=null){
            ThreadUtils.cancel(delayShareAllLogTask)
        }
        delayShareAllLogTask = DelayShareAllLogTask()
        ThreadUtils.executeByIo(delayShareAllLogTask)
    }

    inner class DelayShareAllLogTask : ThreadUtils.SimpleTask<Int>() {
        override fun doInBackground(): Int {
            var timer = 3
            while (timer > 0) {
                timer--
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            shareAllLog()
        }

    }
    //endregion

    fun shareAllLog() {
            val zipFilePath = getExternalFilesDir("logZip")?.absolutePath + File.separator +
                    "Xzfit_log_" + System.currentTimeMillis() + ".zip"
            val logDirs = arrayListOf<String>()
            getExternalFilesDir("log")?.absolutePath?.let {
                logDirs.add(it)
            }
            getExternalFilesDir("deviceLog")?.absolutePath?.let {
                logDirs.add(it)
            }
            LogUtils.d(zipFilePath)
            shareAllZip(logDirs, zipFilePath)
    }


    private fun shareZip(logDir: String, zipFilePath: String, isToday: Boolean = true) {
        loadDialog.show()
        ThreadUtils.executeByIo(object : ThreadUtils.Task<String>() {
            override fun onCancel() {}
            override fun onFail(t: Throwable?) {
                LogUtils.e("shareZip ->$t")
                DialogUtils.dismissDialog(loadDialog)
            }

            override fun doInBackground(): String {
                var zipPath = ""
                val logPaths = mutableListOf<String>()
                for (file in listAllFilesInDir(logDir)) {
                    //只分享今日log
                    if (isToday) {
                        if (file.absolutePath.contains(TimeUtils.date2String(Date(), com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd")))) {
                            logPaths.add(file.absolutePath)
                        }
                    } else {
                        logPaths.add(file.absolutePath)
                    }

                }
                if (logPaths.size > 0) {
                    ZipUtils.zipFiles(logPaths, zipFilePath)
                    zipPath = zipFilePath
                }
                return zipPath
            }

            override fun onSuccess(result: String?) {
                DialogUtils.dismissDialog(loadDialog)
                if (result.isNullOrEmpty()) {
                    ToastUtils.showLong("暂无日志可分享")
                    return
                }
                val zipFile = FileUtils.getFileByPath(result)
                var intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/zip"
                intent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(zipFile))
                intent.putExtra(Intent.EXTRA_TEXT, "abc")
                intent = Intent.createChooser(intent, "日志分享")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

        })
    }

    private fun shareAllZip(logDirs: List<String>, zipFilePath: String) {
        loadDialog.show()
        ThreadUtils.executeByIo(object : ThreadUtils.Task<String>() {
            override fun onCancel() {}
            override fun onFail(t: Throwable?) {
                LogUtils.e("shareZip ->$t")
                DialogUtils.dismissDialog(loadDialog)
            }

            override fun doInBackground(): String {
                var zipPath = ""
                val logPaths = mutableListOf<String>()
                for (logDir in logDirs) {
                    for (file in listAllFilesInDir(logDir)) {
                        logPaths.add(file.absolutePath)
                    }
                }
                if (logPaths.size > 0) {
                    ZipUtils.zipFiles(logPaths, zipFilePath)
                    zipPath = zipFilePath
                }
                return zipPath
            }

            override fun onSuccess(result: String?) {
                DialogUtils.dismissDialog(loadDialog)
                if (result.isNullOrEmpty()) {
                    ToastUtils.showLong("暂无日志可分享")
                    return
                }
                val zipFile = FileUtils.getFileByPath(result)
                var intent = Intent(Intent.ACTION_SEND)
                intent.type = "application/zip"
                intent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(zipFile))
                intent.putExtra(Intent.EXTRA_TEXT, "abc")
                intent = Intent.createChooser(intent, "日志分享")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

        })
    }

    fun listAllFilesInDir(dir: String): List<File> {
        val list = mutableListOf<File>()
        for (file in FileUtils.listFilesInDir(dir)) {
            if (file.isFile) {
                list.add(file)
            } else if (file.isDirectory) {
                list.addAll(listAllFilesInDir(file.absolutePath))
            }
        }
        return list
    }

    override fun onDestroy() {
        super.onDestroy()
        FileUtils.delete(getExternalFilesDir("logZip")?.absolutePath)
    }
}