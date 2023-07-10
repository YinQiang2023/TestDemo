package com.jwei.publicone.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.BleCommonAttributes
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityRemindSetBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.view.wheelview.NumberPicker
import com.jwei.publicone.view.wheelview.OptionPicker
import com.jwei.publicone.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThemeManager
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.bean.SimpleSettingSummaryBean
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.dialog.DownloadDialog
import com.jwei.publicone.expansion.postDelay
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.download.DownloadListener
import com.jwei.publicone.https.download.DownloadManager
import com.jwei.publicone.ui.GlobalEventManager
import com.jwei.publicone.ui.device.bean.DeviceSettingBean
import com.jwei.publicone.ui.device.setting.more.*
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.*
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.FileUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.utils.manager.WakeLockManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.StringBuilder
import java.lang.ref.WeakReference

/**
 * Created by Android on 2021/10/12.
 * 更多设置
 */
class DevMoreSetActivity : BaseActivity<ActivityRemindSetBinding, DeviceModel>(ActivityRemindSetBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG = DevMoreSetActivity::class.java.simpleName

    private lateinit var loadDialog: Dialog
    private var simpleSettingSummaryBean: SimpleSettingSummaryBean? = null
    private val deviceSettingBean by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }
    private var mPowerSavingObserver: Observer<Boolean>? = null
    private var mOverlayScreenObserver: Observer<Boolean>? = null
    private var mVibrationObserver: Observer<Int>? = null
    private var mLanguageObserver: Observer<LanguageListBean>? = null

    //发送固件升级dialog
    private lateinit var uploadDialog: DownloadDialog

    //是否传输ota中
    var isOtaSending = false


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_take_more)
        EventBus.getDefault().register(this)
        loadDialog = DialogUtils.showLoad(this)

        loadViews()

        if (Global.deviceLanguageList == null) Global.getDevLanguage()
    }

    private fun loadViews(isEnabled: Boolean = true) {
        binding.llRemindSetList.removeAllViews()
        var texts = resources.getStringArray(R.array.devMoreSetStrList)
        binding.llRemindSetList.removeAllViews()

        if (deviceSettingBean == null) {
            texts = texts.filter {
                it == getString(R.string.dev_more_set_reboot) ||
                        it == getString(R.string.dev_more_set_ble_mac) ||
                        it == getString(R.string.dev_more_set_update)
            }.toTypedArray()
        }
        texts.forEach { it ->
            if (checkLoad(it)) {
                val constraintLayout = LayoutInflater.from(this).inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                val mSwitchCompat = constraintLayout.findViewById<SwitchCompat>(R.id.mSwitchCompat)
                val tvOther = constraintLayout.findViewById<TextView>(R.id.tvOther)
                val ivNext = constraintLayout.findViewById<ImageView>(R.id.ivNext)
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                tvName.text = it
                image.background = null
                image.visibility = View.GONE
                tvName.setPadding(0, 0, 0, 0)
                constraintLayout.alpha = if (isEnabled) 1.0f else 0.5f
                constraintLayout.isEnabled = isEnabled
                mSwitchCompat.isEnabled = isEnabled

                when (it) {
                    /* getString(R.string.dev_more_set_google_fit) -> {
                         ivNext.visibility = View.GONE
                         mSwitchCompat.visibility = View.VISIBLE

                         mSwitchCompat.isChecked = SpUtils.getSPUtilsInstance()
                             .getBoolean(SpUtils.SWITCH_GOOGLE_FIT, false)
                     }*/
                    getString(R.string.dev_more_set_coverage_screen) -> {
                        ivNext.visibility = View.GONE
                        mSwitchCompat.visibility = View.VISIBLE
                        //覆盖息屏开关
                        simpleSettingSummaryBean?.let { bean ->
                            mSwitchCompat.isChecked = bean.isOverlayScreen
                        }
                        if (mOverlayScreenObserver != null) {
                            viewModel.deviceSettingLiveData.getOverlayScreen().removeObserver(mOverlayScreenObserver!!)
                        }
                        mOverlayScreenObserver = Observer {
                            LogUtils.d("覆盖息屏 ->$it")
                            mSwitchCompat.isChecked = it
                        }
                        viewModel.deviceSettingLiveData.getOverlayScreen().observe(this, mOverlayScreenObserver!!)
                    }
                    getString(R.string.dev_more_set_shake) -> {
                        tvOther.visibility = View.VISIBLE
                        //震动级别
                        simpleSettingSummaryBean?.let { bean ->
                            tvOther.text = "${bean.vibrationIntensityMode + 1}"
                        }

                        if (mVibrationObserver != null) {
                            viewModel.deviceSettingLiveData.getVibration().removeObserver(mVibrationObserver!!)
                        }
                        mVibrationObserver = Observer {
                            LogUtils.d("震动强度 ->$it")
                            tvOther.text = "${it + 1}"
                        }
                        viewModel.deviceSettingLiveData.getVibration().observe(this, mVibrationObserver!!)
                    }
                    getString(R.string.dev_more_set_ble_mac) -> {
                        tvOther.visibility = View.VISIBLE
                        ivNext.visibility = View.GONE
                        tvOther.setPadding(0, 0, 0, 0)
                        tvOther.text = Global.deviceMac
                    }
                    getString(R.string.dev_more_set_update) -> {
                        tvOther.visibility = View.VISIBLE
                        tvOther.text = Global.deviceVersion
                        viewLine01.visibility = View.GONE
                    }
                }

                mSwitchCompat.setOnClickListener { view ->
                    //ToastUtils.showShort("是否打开 "+mSwitchCompat.isChecked)
                    when (it) {
                        /*getString(R.string.dev_more_set_google_fit) -> {
                            SpUtils.getSPUtilsInstance()
                                .put(SpUtils.SWITCH_GOOGLE_FIT, mSwitchCompat.isChecked)
                        }*/
                        getString(R.string.dev_more_set_coverage_screen) -> {
                            mSwitchCompat.isChecked = !mSwitchCompat.isChecked
                            if (!ControlBleTools.getInstance().isConnect) {
                                ToastUtils.showToast(R.string.device_no_connection)
                                return@setOnClickListener
                            }
                            loadDialog.show()
                            ControlBleTools.getInstance().setOverlayScreen(!mSwitchCompat.isChecked,
                                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                    override fun onState(state: SendCmdState) {
                                        DialogUtils.dismissDialog(loadDialog)
                                        ToastUtils.showSendCmdStateTips(
                                            state
                                        )
                                        ControlBleTools.getInstance().getOverlayScreen(null)
                                    }
                                })
                        }
                    }
                }
                constraintLayout.tag = it
                setViewsClickListener(this, constraintLayout)
                binding.llRemindSetList.addView(constraintLayout)

            }
        }
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkLoad(it: String?): Boolean {
        if (it == null) return false
        if (/*it == getString(R.string.dev_more_set_google_fit) ||*/
            it == getString(R.string.dev_more_set_reboot) ||
            it == getString(R.string.dev_more_set_ble_mac) ||
            it == getString(R.string.dev_more_set_update) ||
            it == getString(R.string.dev_more_set_time)
        ) {
            return true
        }
        //后台配置的
        if (deviceSettingBean != null) {
            when (it) {
                getString(R.string.dev_more_set_coverage_screen) -> {
                    return deviceSettingBean.settingsRelated.cover_the_screen_off
                }
                getString(R.string.dev_more_set_lum) -> {
                    return deviceSettingBean.settingsRelated.bright_adjustment ||
                            deviceSettingBean.settingsRelated.bright_screen_time ||
                            deviceSettingBean.settingsRelated.double_click_to_brighten_the_screen
                }
                getString(R.string.dev_more_set_shake) -> {
                    return deviceSettingBean.settingsRelated.vibration_adjustment
                }
                getString(R.string.dev_more_set_language_sel) -> {
                    return deviceSettingBean.settingsRelated.language
                }
                getString(R.string.dev_more_set_app_sort) -> {
                    return deviceSettingBean.settingsRelated.application_list_sorting
                }
            }
        }
        return false
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getSimpleSettingSummary().observe(this, {
            if (it == null) return@observe
            LogUtils.d("简单设置汇总 ->${GsonUtils.toJson(it)}")
            simpleSettingSummaryBean = it
            loadViews()
        })

        ControlBleTools.getInstance().getSimpleSetting(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                ToastUtils.showSendCmdStateTips(state)
            }
        })


        viewModel.queryFirewareCode.observe(this) {
            if (it == null) return@observe
            //DialogUtils.dismissDialog(loadDialog)
            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                }
                HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                    showNonewverDialog()
                }
            }
        }

        viewModel.firewareUpgradeData.observe(this) {
            if (it == null) return@observe
            checkOtaLog.endTime = TrackingLog.getNowString()

            if (it.versionBefore != it.versionAfter && it.versionUrl.isNotEmpty() && !GlobalEventManager.isUpload) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getStartTypeTrack("OTA"), isStart = true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, checkOtaLog)
                showUpdateDialog(it.remark)
            } else {
                if (it.id.isEmpty()) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getStartTypeTrack("OTA"), isStart = true)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, checkOtaLog.apply {
                        log += "\n请求失败/超时"
                    }, "1910", true)
                }
            }
        }

    }

    //region 震动强度
    /**
     * 设置震动强度
     * */
    private fun setVibrationIntensity() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            //item.toInt()
            loadDialog.show()
            ControlBleTools.getInstance()
                .setDeviceVibrationIntensity(item.toInt() - 1, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                        ControlBleTools.getInstance().getDeviceVibrationIntensity(null)
                    }
                })
        }
        picker.setRangeStep(1, 3, 1)
        simpleSettingSummaryBean?.let { bean ->
            picker.setDefaultValue(bean.vibrationIntensityMode + 1)
        }
        viewModel.deviceSettingLiveData.getVibration().value?.let {
            picker.setDefaultValue(it + 1)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }
    //endregion

    //region 时间设置
    private fun timeSet() {
        val data = mutableListOf<String>()
        data.add("12")
        data.add("24")
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            loadDialog.show()
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(),
                (item.toString() == "12").apply {
                    SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_TIME_IS12, this)
                },
                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                    }
                })
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setDefaultPosition(
            if (SpUtils.getSPUtilsInstance().getBoolean(SpUtils.DEVICE_TIME_IS12, false)) 0 else 1
        )
        picker.show()
    }
    //endregion

    //region 重启设备
    private fun rebootDev() {
        showRebootDialog()
    }

    private fun showRebootDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.dev_more_set_reboot),
            getString(R.string.reboot_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    ControlBleTools.getInstance().rebootDevice(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                            ManageActivity.removeActivity(DeviceSetActivity::class.java)
                            ManageActivity.removeActivity(DevMoreSetActivity::class.java)
                            ControlBleTools.getInstance().disconnect()
                            //3s后重连
                            ThreadUtils.runOnUiThreadDelayed({
                                SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                            }, 5000)
                        }
                    })
                }

                override fun OnCancel() {}
            })
        dialog.show()
    }

    private fun showRebootFailedDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.reset_reboot_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    //TODO 测试 重启设备
                    ThreadUtils.runOnUiThreadDelayed({
                        DialogUtils.dismissDialog(loadDialog)
                        showRebootFailedDialog()
                    }, 1000)
                }

                override fun OnCancel() {

                }
            })
        dialog.show()
    }
    //endregion

    //region 设备固件更新
    private val checkOtaLog by lazy { TrackingLog.getSerTypeTrack("app请求获取OTA文件", "固件升级", "ffit/firmware/getFirewareUpgradeVersion") }
    private fun updateDevice() {
        //loadDialog.show()
        viewModel.checkFirewareUpgrade(checkOtaLog)
    }

    /**
     * 无升级提示
     */
    private fun showNonewverDialog() {
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.nonewver_tips),
            getString(R.string.dialog_confirm_btn),
            null
        )
    }

    /**
     * 升级提示
     */
    private fun showUpdateDialog(remark: String) {
        GlobalEventManager.isUpload = true
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.find_new_ota_version),
            StringBuilder().append(getString(R.string.find_new_ota_version_tips))/*.append("\n").append(remark)*/.toString(),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.immediately_to),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    NetworkUtils.isAvailableAsync { isAvailable ->
                        if (!isAvailable) {
                            ToastUtils.showToast(R.string.not_network_tips)
                            GlobalEventManager.isUpload = false
                            return@isAvailableAsync
                        }
                        showDownloadingDialog()
                    }
                }

                override fun OnCancel() {
                    GlobalEventManager.isUpload = false
                }
            })
        dialog.show()
    }

    /**
     * 固件文件下载
     */
    private fun showDownloadingDialog() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            GlobalEventManager.isUpload = false
            return
        }
        if (!AppUtils.isOpenBluetooth()) {
            ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
            GlobalEventManager.isUpload = false
            return
        }
        GlobalEventManager.isUpload = true
        //保持不息屏
        WakeLockManager.instance.keepUnLock(this@DevMoreSetActivity.lifecycle)
        val downloadDialog = DownloadDialog(
            this@DevMoreSetActivity,
            getString(R.string.theme_center_dial_down_load_title), ""
        )
        downloadDialog.showDialog()
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_OTA)
        ErrorUtils.onLogResult("ota start")
        val downloadOtaLog = TrackingLog.getSerTypeTrack("下载ota文件", "下载", "${viewModel.firewareUpgradeData.value?.versionUrl}")
        viewModel.firewareUpgradeData.value?.let {
            DownloadManager.download(it.versionUrl, listener = object : DownloadListener {
                override fun onStart() {
                    LogUtils.d(TAG, "showDownloadingDialog onStart")
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
                    LogUtils.e(TAG, "showUpdateDialog DownloadManagerUtils onFailed: $msg", true)
                    ErrorUtils.onLogResult("ota Download onFailed :$msg")
                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)
                    downloadOtaLog.endTime = TrackingLog.getNowString()
                    downloadOtaLog.log += "\nota Download onFailed :$msg"
                    downloadOtaLog.serResult = "失败"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog.apply {
                        log += "\n下载文件失败/超时"
                    }, "1911", isEnd = true)
                    downloadDialog.cancel()
                    this@DevMoreSetActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                        FileUtils.deleteAll(it.path)
                    }
                    showUpdateFailedDialog()
                }

                override fun onSucceed(path: String) {
                    LogUtils.e(TAG, "showUpdateDialog DownloadManagerUtils onSuccess", true)
                    downloadDialog.cancel()

                    downloadOtaLog.endTime = TrackingLog.getNowString()
                    downloadOtaLog.log += "\nota Download onSuccess"
                    if (!path.isNullOrEmpty()) {
                        downloadOtaLog.serResult = "成功"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog)
                        deviceLargeFileState(path)
                    } else {
                        ErrorUtils.onLogResult("ota file path is null")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
                        showUpdateFailedDialog()
                        downloadOtaLog.serResult = "失败"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, downloadOtaLog.apply {
                            log += "\n下载文件失败/超时"
                        }, "1911", isEnd = true)
                    }
                }
            })
        }
    }

    /**
     * 升级失败提示
     */
    private fun showUpdateFailedDialog() {
        GlobalEventManager.isUpload = false
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.update_failed_tips),
            getString(R.string.dialog_cancel_btn),
            if (AppUtils.isOpenBluetooth() && ControlBleTools.getInstance().isConnect) getString(R.string.reset_update) else getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    NetworkUtils.isAvailableAsync { isAvailable ->
                        if (!isAvailable) {
                            ToastUtils.showToast(R.string.not_network_tips)
                            return@isAvailableAsync
                        }
                        if (AppUtils.isOpenBluetooth() && ControlBleTools.getInstance().isConnect) {
                            showDownloadingDialog()
                        }
                    }
                }

                override fun OnCancel() {}
            })
        dialog.show()
    }

    private val fileStatusTrackingLog by lazy { TrackingLog.getDevTyepTrack("请求设备传文件状态", "获取发送OTA文件状态", "PREPARE_OTA_VALUE") }

    /**
     * 请求设备传文件状态
     */
    private fun deviceLargeFileState(path: String, version: String = "111", md5: String = "222") {
        loadDialog.show()
        fileStatusTrackingLog.startTime = TrackingLog.getNowString()
        fileStatusTrackingLog.log = "isForce:true"
        ControlBleTools.getInstance().getDeviceLargeFileState(true, version, md5, MyDeviceLargeFileStatusListener(this@DevMoreSetActivity, path))
    }

    class MyDeviceLargeFileStatusListener(
        activity: DevMoreSetActivity,
        var path: String
    ) : DeviceLargeFileStatusListener {
        private var wrActivity: WeakReference<DevMoreSetActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }

        @SuppressLint("SuspiciousIndentation")
        override fun onSuccess(statusValue: Int, statusName: String?) {
            wrActivity?.get()?.apply {
                fileStatusTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                    devResult = "statusValue:$statusValue,statusName : $statusName"

                }
                if (statusName != "READY") {
                    GlobalEventManager.isUpload = false
                    showUpdateFailedDialog()
                    if (statusName != "LOW_BATTERY") {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, fileStatusTrackingLog.apply {
                            log += "\nstatus == $statusName != READY\n请求文件状态超时/失败"
                        }, "1914", true)
                    }
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, fileStatusTrackingLog)
                }
                LogUtils.e(TAG, "showUpdateDialog getDeviceLargeFileState onSuccess $statusName", true)
                DialogUtils.dismissDialog(loadDialog)
                when (statusName) {
                    "READY" -> {
                        val fileByte: ByteArray? = FileUtils.getBytes(path)
                        if (fileByte == null) {
                            ErrorUtils.onLogResult("ota fileByte is null")
                            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
                            showUpdateFailedDialog()
                            return
                        }
                        uploadFile(fileByte)
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
            }
        }

        override fun timeOut() {
            wrActivity?.get()?.apply {
                LogUtils.e(TAG, "showUpdateDialog getDeviceLargeFileState timeOut")
                DialogUtils.dismissDialog(loadDialog)
                ErrorUtils.onLogResult("ota getDeviceLargeFileState timeOut")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                if (ControlBleTools.getInstance().isConnect) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getAppTypeTrack("请求文件状态超时/失败").apply {
                        log = "timeOut"
                    }, "1912", true)
                }
                ToastUtils.showToast(R.string.ota_device_timeout_tips)
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    FileUtils.deleteAll(it.path)
                }
                showUpdateFailedDialog()
            }
        }
    }

    val uploadOtaTrackingLog by lazy { TrackingLog.getDevTyepTrack("传输OTA文件", "上传大文件数据", "sendThemeByProto4") }

    /**
     * 发送文件至设备
     */
    private fun uploadFile(fileByte: ByteArray) {
        uploadDialog = DownloadDialog(this@DevMoreSetActivity, getString(R.string.theme_fireware_update_title), "")
        uploadDialog.showDialog()
        uploadDialog.tvSize?.text = getString(R.string.theme_center_dial_up_load_tips)
        isOtaSending = true
        uploadOtaTrackingLog.startTime = TrackingLog.getNowString()
        uploadOtaTrackingLog.log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_WATCH},fileByte:${fileByte.size},isResumable:true"
        ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_OTA, fileByte, true, MyUploadBigDataListener(this@DevMoreSetActivity))
    }

    class MyUploadBigDataListener(activity: DevMoreSetActivity) : UploadBigDataListener {
        private var wrActivity: WeakReference<DevMoreSetActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }


        override fun onSuccess() {
            wrActivity?.get()?.apply {
                LogUtils.e(TAG, "showUpdateDialog startUploadBigData onSuccess", true)
                uploadDialog.cancel()
                isOtaSending = false
                GlobalEventManager.isUpload = false

                uploadOtaTrackingLog.log += "\nota startUploadBigData onSuccess"
                uploadOtaTrackingLog.endTime = TrackingLog.getNowString()
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, uploadOtaTrackingLog)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getEndTypeTrack("OTA"), isEnd = true)

                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    FileUtils.deleteAll(it.path)
                }
                if (ThemeManager.getInstance().packetLossTimes > 0) {
                    postDelay(100) {
                        ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                    }
                }
                ManageActivity.removeActivity(DeviceSetActivity::class.java)
                ManageActivity.removeActivity(DevMoreSetActivity::class.java)
                ControlBleTools.getInstance().disconnect()
                //3s后重连
                ThreadUtils.runOnUiThreadDelayed({
                    SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                }, 5000)
                ErrorUtils.clearErrorBigData()
            }
        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            wrActivity?.get()?.apply {
                runOnUiThread {
                    val percentage = (curPiece / dataPackTotalPieceLength.toFloat()) * 100
                    uploadDialog.progressView?.max = dataPackTotalPieceLength
                    uploadDialog.progressView?.progress = curPiece
                    uploadDialog.tvProgress?.text = "${/*UnitConversionUtils.bigDecimalFormat(percentage)*/percentage.toInt()}%"
                }
                uploadOtaTrackingLog.log += "\n${((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()}%"
            }
        }

        override fun onTimeout() {
            wrActivity?.get()?.apply {
                LogUtils.e(TAG, "showUpdateDialog startUploadBigData onTimeout", true)
                ErrorUtils.onLogResult("ota startUploadBigData onTimeout")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                GlobalEventManager.isUpload = false

                uploadOtaTrackingLog.log += "ota startUploadBigData onTimeout; isConnect:${ControlBleTools.getInstance().isConnect}"
                uploadOtaTrackingLog.endTime = TrackingLog.getNowString()
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, uploadOtaTrackingLog.apply {
                    log += "\n发送响应超时/失败"
                }, "1914", true)
                isOtaSending = false

                if (!isDestroyed) {
                    ThreadUtils.runOnUiThread {
                        if (uploadDialog.isShowing()) {
                            uploadDialog.cancel()
                            ToastUtils.showToast(
                                R.string.ota_device_timeout_tips
                            )
                            showUpdateFailedDialog()
                        }
                    }
                }
                //FileUtils.deleteAll(path)
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    FileUtils.deleteAll(it.path)
                }
            }
        }
    }
    //endregion

    override fun onClick(v: View?) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        when (v?.tag.toString()) {
            getString(R.string.dev_more_set_time) -> {
                timeSet()
            }
            getString(R.string.dev_more_set_lum) -> {
                startActivity(Intent(this, LightSetActivity::class.java))
            }
            getString(R.string.dev_more_set_shake) -> {
                setVibrationIntensity()
            }
            getString(R.string.dev_more_set_language_sel) -> {
                startActivity(Intent(this, LanguageSetActivity::class.java))
            }
            getString(R.string.dev_more_set_reboot) -> {
                rebootDev()
            }
            getString(R.string.dev_more_set_update) -> {
                updateDevice()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                //蓝牙关闭
                if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    loadViews(false)
                }
            }
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        loadViews(false)
                        if (isOtaSending) {
                            isOtaSending = false
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "1913", true)
                        }
                    }
                }
            }
            EventAction.ACTION_DEVICE_CONNECTED -> {
                loadViews(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }
}