package com.smartwear.publicwatch.ui.device.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.yalantis.ucrop.UCrop
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThemeManager
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.zhapp.ble.bean.diydial.DiyParamsBean
import com.zhapp.ble.callback.*
import com.zhapp.ble.custom.DiyDialUtils
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.ActivityDiyDialBinding
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.dialog.DownloadDialog
import com.smartwear.publicwatch.expansion.postDelay
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.response.DiyDialInfoResponse
import com.smartwear.publicwatch.ui.adapter.FunctionsAdapter
import com.smartwear.publicwatch.ui.adapter.StyleAdapter
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.bean.BulkDownloadListener
import com.smartwear.publicwatch.ui.device.bean.StyleBean
import com.smartwear.publicwatch.ui.device.bean.diydial.MyDiyDialUtils
import com.smartwear.publicwatch.ui.device.bean.diydial.MyDiyDialUtils.getDiyDialJsonByDiyDialInfoResponse
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.livedata.RefreshMyDialListState
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.FileUtils
import com.smartwear.publicwatch.utils.PermissionUtils
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.viewmodel.DeviceModel
import kotlinx.android.synthetic.main.activity_diy_dial.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class DiyDialActivity : BaseActivity<ActivityDiyDialBinding, DeviceModel>(
    ActivityDiyDialBinding::inflate,
    DeviceModel::class.java
), View.OnClickListener {
    private var TAG = DialDetailsActivity::class.java.simpleName

    private var dialog: Dialog? = null

    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private val RESULT_GET_PHOTOGRAPH_CODE = 0x161
    private var takePictureUri: Uri? = null     //拍照
    private var cropUri: Uri? = null //裁剪
    private var dialUri: Uri? = null //最终资源uri
    private var deviceWith = 0
    private var deviceHeith = 0
    private var deviceShape = ""  //主题形状 = 0=方形/1=球拍/2=圆形/3=圆角矩形1
    private var newBgBitmap: Bitmap? = null
    private var preViewBitmap: Bitmap? = null

    private val styles = mutableListOf<StyleBean>()

    //diy表盘描述对象
    private var dataJson = ""

    private var styleSelect: StyleBean? = null

    private var diyWatchFaceConfig: DiyWatchFaceConfigBean? = null

    //功能选择跳转返回
    private lateinit var functionSelectResultLauncher: ActivityResultLauncher<Intent>


    private val dialInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("app获取后台表盘详情", "Diy表盘详情", "ffit/dial/info") }

    //相机权限未开启异常
    private var perMissTrackingLog: TrackingLog? = null

    //相册资源获取失败异常
    private var photoTrackingLog: TrackingLog? = null

    //裁剪异常
    private var cropTrackingLog: TrackingLog? = null

    //表盘发送状态
    private val fileStateTrackingLog by lazy { TrackingLog.getDevTyepTrack("请求设备传文件状态", "获取发送表盘文件状态", "PREPARE_INSTALL_WATCH_FACE") }

    private var isSending = false

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        setTvTitle(getString(R.string.diy_watch_face))
        AppUtils.registerEventBus(this)
        dialog = DialogUtils.showLoad(this)
        dialog?.show()
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getStartTypeTrack("DIY表盘"), isStart = true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCallBack()
        event()
        activityResultRegister()
        observe()
        intent.getStringExtra("dialId")?.let { viewModel.getDiyInfoById(it, dialInfoTrackingLog) }
    }

    fun observe() {
        viewModel.photoData.observe(this) {
            if (it == null) return@observe
            refPreView()
        }
        viewModel.diyInfo.observe(this) {
            if (it == null) return@observe
            dialInfoTrackingLog.endTime = TrackingLog.getNowString()
            if (it.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, dialInfoTrackingLog)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("DIY表盘"))
                binding.tvName.text = it.data.name
                binding.tvIntroduction.text = it.data.desc
                initConfig(it.data)

                viewModel.downloadInfoRes(it.data, object : BulkDownloadListener {
                    override fun onProgress(totalSize: Int, currentSize: Int) {

                    }

                    override fun onFailed(msg: String) {
                        DialogUtils.dismissDialog(dialog)
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("下载表盘资源失败/超时").apply {
                            log = msg
                        }, "1814", true)
                        finishAfterTransition()
                    }

                    override fun onComplete() {
                        //文件下载完成
                        DialogUtils.dismissDialog(dialog)
                        loadStyles()
                        refPreView()
                        //获取设备表盘功能状态
                        ControlBleTools.getInstance().getDiyWatchFaceConfig(
                            diyWatchFaceConfig?.id,
                            object : ParsingStateManager.SendCmdStateListener() {
                                override fun onState(state: SendCmdState?) {
//                                when (state) {
//                                    SendCmdState.SUCCEED -> ToastUtils.showShort("成功")
//                                    else -> ToastUtils.showShort("失败")
//                                }
                                }
                            })
                    }
                })
            } else {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, dialInfoTrackingLog.apply {
                    log += "\n请求失败/超时"
                }, "1810", true)
                finish()
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun eventBusMsg(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        com.smartwear.publicwatch.utils.LogUtils.i(TAG, "device connected")
                    }
                    BleCommonAttributes.STATE_CONNECTING -> {
                        com.smartwear.publicwatch.utils.LogUtils.i(TAG, "device connecting")
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        com.smartwear.publicwatch.utils.LogUtils.e(TAG, "device disconnect")
                        if (isSending) {
                            isSending = false
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "1817", true)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStyles() {
        //指针
        viewModel.diyInfo.value!!.data.pointerList!!.forEach {
            styles.add(
                StyleBean(
                    if (TextUtils.equals(it.type, "1")) DiyParamsBean.StyleResBean.StyleType.POINTER.type else DiyParamsBean.StyleResBean.StyleType.NUMBER.type,
                    ConvertUtils.bytes2Bitmap(FileUtils.getBytes(UrlPathUtils.getUrlPath(it.renderingsUrl))),
                    ConvertUtils.bytes2Bitmap(FileUtils.getBytes(UrlPathUtils.getUrlPath(it.pointerPictureUrl))),
                    FileUtils.getBytes(UrlPathUtils.getUrlPath(it.binUrl))!!,
                    false
                )
            )
            if (styles.size > 0) {
                styles[0].isSelected = true
                styleSelect = styles[0]
                binding.tvSize.text = AppUtils.biDiFormatterStr(FileUtils.getSizeForKb(styleSelect!!.binData.size / 1024))
            }
            rvStyle.adapter!!.notifyDataSetChanged()
        }
        rvStyle.adapter!!.notifyDataSetChanged()
    }

    private fun activityResultRegister() {
        functionSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                LogUtils.d("functionSelectResultLauncher: ${result.resultCode}")
                if (result.resultCode == Activity.RESULT_OK) {
                    val json = result.data?.getStringExtra(FunctionSelectActivity.RESULT_DATA_TEXT)
                    if (json != null) {
                        val configBean = GsonUtils.fromJson(json, DiyWatchFaceConfigBean.FunctionsConfig::class.java)
                        for (i in 0..diyWatchFaceConfig!!.functionsConfigs!!.size) {
                            val info = diyWatchFaceConfig!!.functionsConfigs!!.get(i)
                            if (info.position == configBean.position) {
                                val configs = diyWatchFaceConfig!!.functionsConfigs as MutableList
                                configs[i] = configBean
                                diyWatchFaceConfig!!.functionsConfigs = configs
                                break
                            }
                        }
                        refFunctionsAdapter()
                        refPreView()
                    }
                }
            }

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result == true) {
                if (takePictureUri != null) {
                    cropImage(takePictureUri!!)
                }
            }else{
                delAllCacheImg()
            }
        }
    }


    private fun initConfig(data: DiyDialInfoResponse) {
        dataJson = getDiyDialJsonByDiyDialInfoResponse(data)

        deviceWith = data.dpi.substringBefore("x").toInt()
        deviceHeith = data.dpi.substringAfter("x").toInt()

        binding.rvStyle.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStyle.adapter = StyleAdapter(this@DiyDialActivity, styles) { selectedP ->
            styleSelect = styles[selectedP]
            binding.tvSize.text = AppUtils.biDiFormatterStr(FileUtils.getSizeForKb(styleSelect!!.binData.size / 1024))
            refPreView()
        }

        //复杂功能
        diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(dataJson)

        if (diyWatchFaceConfig != null && diyWatchFaceConfig!!.functionsConfigs != null) {
            binding.tvComplex.visibility = View.VISIBLE
            binding.rvComplex.visibility = View.VISIBLE
            binding.rvComplex.apply {
                layoutManager =
                    LinearLayoutManager(this@DiyDialActivity, LinearLayoutManager.VERTICAL, false)
                addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
                adapter = FunctionsAdapter(
                    this@DiyDialActivity,
                    diyWatchFaceConfig!!.functionsConfigs!!
                ) { clickPosition ->
                    val info = diyWatchFaceConfig!!.functionsConfigs!![clickPosition]
                    val details = info.functionsConfigTypes
                    if (details.isNullOrEmpty()) {
                        return@FunctionsAdapter
                    }
                    val intent = Intent(this@DiyDialActivity, FunctionSelectActivity::class.java)
                    intent.putExtra(FunctionSelectActivity.ACTIVITY_DATA_TEXT, GsonUtils.toJson(info))
                    intent.putExtra(FunctionSelectActivity.ACTIVITY_DATA_TITLE, MyDiyDialUtils.getFunctionsLocationNameByType(context, info.position))
                    functionSelectResultLauncher.launch(intent)
                }
            }
        }else{
            binding.tvComplex.visibility = View.GONE
            binding.rvComplex.visibility = View.GONE
        }

    }

    private fun initCallBack() {
        CallBackUtils.diyWatchFaceCallBack = DiyWatchFaceCallBack { config ->
            //根据 DiyWatchFaceConfigBean更新UI
            if (config != null) {
                LogUtils.d("config:" + GsonUtils.toJson(config))
                //复杂功能为空时，取默认配置类的复杂功能设置
                if (config.functionsConfigs == null || config.functionsConfigs.isEmpty()) {
                    val cfConfig = diyWatchFaceConfig?.functionsConfigs
                    diyWatchFaceConfig = config
                    diyWatchFaceConfig?.functionsConfigs = cfConfig
                } else {
                    diyWatchFaceConfig = config
                }
                //刷新复杂功能
                refFunctionsAdapter()
                //刷新背景
                if (diyWatchFaceConfig?.backgroundFileConfig != null && !diyWatchFaceConfig!!.backgroundFileConfig.watchFaceFiles.isNullOrEmpty()) {
                    var selectedMd5 = ""
                    for (faceFile in diyWatchFaceConfig!!.backgroundFileConfig!!.watchFaceFiles) {
                        if (faceFile.fileNumber == diyWatchFaceConfig!!.backgroundFileConfig!!.usedFileNumber) {
                            selectedMd5 = faceFile.fileMd5
                            LogUtils.d("background selectedMd5:$selectedMd5")
                        }
                    }

                }
                //刷新指针或者数字
                if (diyWatchFaceConfig?.pointerFileConfig != null && !diyWatchFaceConfig!!.pointerFileConfig.watchFaceFiles.isNullOrEmpty()) {
                    var selectedMd5 = ""
                    for (faceFile in diyWatchFaceConfig!!.pointerFileConfig!!.watchFaceFiles) {
                        if (faceFile.fileNumber == diyWatchFaceConfig!!.pointerFileConfig!!.usedFileNumber) {
                            selectedMd5 = faceFile.fileMd5
                            LogUtils.d("pointer selectedMd5:$selectedMd5")
                        }
                    }
                    for (style in styles) {
                        style.isSelected = TextUtils.equals(
                            selectedMd5,
                            DiyDialUtils.getDiyBinBytesMd5(style.binData)
                        )
                        if (style.isSelected)
                            styleSelect = style
                        LogUtils.d("pointer md5:${DiyDialUtils.getDiyBinBytesMd5(style.binData)}")
                    }
                }
                if (diyWatchFaceConfig?.numberFileConfig != null && !diyWatchFaceConfig!!.numberFileConfig.watchFaceFiles.isNullOrEmpty()) {
                    var selectedMd5 = ""
                    for (faceFile in diyWatchFaceConfig!!.numberFileConfig!!.watchFaceFiles) {
                        if (faceFile.fileNumber == diyWatchFaceConfig!!.numberFileConfig!!.usedFileNumber) {
                            selectedMd5 = faceFile.fileMd5
                            LogUtils.d("number selectedMd5:$selectedMd5")
                        }
                    }
                    for (style in styles) {
                        style.isSelected = TextUtils.equals(
                            selectedMd5,
                            DiyDialUtils.getDiyBinBytesMd5(style.binData)
                        )
                        if (style.isSelected) styleSelect = style
                        LogUtils.d("number md5:${DiyDialUtils.getDiyBinBytesMd5(style.binData)}")
                    }

                }
                refStyleAdapter()
                refPreView()
            }
        }
    }

    private fun event() {
        setViewsClickListener(
            this, binding.btnSync, binding.clUpload
        )

    }

    //region 传输表盘
    private fun uploadWatch(watchId: String, data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        if (!ControlBleTools.getInstance().isConnect) {
            DialogUtils.dismissDialog(dialog)
            return
        }
        fileStateTrackingLog.startTime = TrackingLog.getNowString()

        ControlBleTools.getInstance().getDeviceDiyWatchFace(watchId, data.size, true, configBean, object :
            DeviceWatchFaceFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String) {
                DialogUtils.dismissDialog(dialog)
                var isReady = false
                when (statusValue) {
                    DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state -> {
                        sendWatchData(data, configBean)
                        isReady = true
                    }
                    DeviceWatchFaceFileStatusListener.PrepareStatus.BUSY.state -> {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.ota_device_busy_tips)
                    }
                    DeviceWatchFaceFileStatusListener.PrepareStatus.DUPLICATED.state -> {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.ota_device_is_duplicated)
                    }
                    DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_STORAGE.state -> {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.healthy_sports_sync_fail)
                    }
                    DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_BATTERY.state -> {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.ota_device_low_power_tips)
                    }
                    DeviceWatchFaceFileStatusListener.PrepareStatus.DOWNGRADE.state -> {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.ota_device_timeout_tips)
                    }
                }
                if (!isReady) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("请求文件状态超时/失败").apply {
                        log = "status == $statusName != READY"
                    }, "1816", true)
                }
            }

            override fun timeOut() {
//                MyApplication.showToast("timeOut")
                DialogUtils.dismissDialog(dialog)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件失败").apply {
                    log = "getDeviceDiyWatchFace timeOut"
                }, "1815", true)
            }
        })
    }

    private fun sendWatchData(data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        var wrUploadDialog: WeakReference<DownloadDialog>? = null
        //BUG:表盘传输中来电，APP会卡在表盘传输中，无法退出
        //来电后弱引用wrUploadDialog被GC清空，增加强引用（一旦指向的对象没有更多的强/软引用并且 GC具有回收的内存，WeakReference将返回null）
        var strongDialog: DownloadDialog? = null

        val uploadDialog = DownloadDialog(
            this@DiyDialActivity,
            "",
            getString(R.string.theme_center_dial_up_load_title),
            preViewBitmap, //TODO
            null
        )

        strongDialog = uploadDialog
        wrUploadDialog = WeakReference(uploadDialog)

        wrUploadDialog?.get()?.showDialog()
        wrUploadDialog?.get()?.tvSize?.text =
            getString(R.string.theme_center_dial_up_load_tips)

        val uploadTrackingLog = TrackingLog.getDevTyepTrack("传输表盘文件", "上传大文件数据", "sendThemeByProto4").apply {
            log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_WATCH},fileByte:${data.size},isResumable:true"
        }
        isSending = true
        ControlBleTools.getInstance().startUploadBigData(
            BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
            data, true, object : UploadBigDataListener {
                override fun onSuccess() {
                    isSending = false
                    diyWatchFaceConfig = configBean
                    wrUploadDialog.get()?.cancel()
//                    strongDialog = null
                    com.smartwear.publicwatch.utils.LogUtils.i(TAG, "sendCustomDial startUploadBigData onSuccess")
                    com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.sync_success_tips)
                    //同步成功后缓存效果图路径
                    UrlPathUtils.getUrlPath(viewModel.diyInfo.value!!.data.renderings)?.let {
                        SpUtils.setValue(SpUtils.DIY_RENDERINGS_PATH, it)
                    }
                    uploadTrackingLog.log += "\nsendCustomDial startUploadBigData onSuccess"
                    uploadTrackingLog.endTime = TrackingLog.getNowString()
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getEndTypeTrack("表盘"), isEnd = true)
//                                                            FileUtils.deleteAll(path)
//                    BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { FileUtils.deleteAll(it.path) }
                    if (ThemeManager.getInstance().packetLossTimes > 0) {
                        postDelay(100) {
                            ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                        }
                    }
                    postDialLog(3)
                    RefreshMyDialListState.postValue(true)
                    ErrorUtils.clearErrorBigData()
                }

                @SuppressLint("SetTextI18n")
                override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                    val percentage = curPiece * 100 / dataPackTotalPieceLength
                    Log.i("下载", "onProgress $percentage")
                    ThreadUtils.runOnUiThread {
                        wrUploadDialog?.get()?.progressView?.max = dataPackTotalPieceLength
                        wrUploadDialog?.get()?.progressView?.progress = curPiece
                        wrUploadDialog?.get()?.tvProgress?.text = "${
//                                                                UnitConversionUtils.bigDecimalFormat((curPiece / dataPackTotalPieceLength.toFloat()) * 100)
                            ((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()
                        }%"
                    }
                    uploadTrackingLog.log += "\nonProgress $percentage"
                }

                override fun onTimeout() {
                    if (isSending) {
                        com.smartwear.publicwatch.utils.LogUtils.e(TAG, "sendCustomDial startUploadBigData onTimeout")
                        ErrorUtils.onLogResult("CustomDial startUploadBigData onTimeout")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                        uploadTrackingLog.log += "\nsendOnlineDial startUploadBigData onTimeout; isConnect:${ControlBleTools.getInstance().isConnect}"
                        uploadTrackingLog.endTime = TrackingLog.getNowString()
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog.apply {
                            log += "\n发送响应超时/失败"
                        }, "1818", true)
                    }
                    strongDialog = null
                    isSending = false
                    postDialLog(4)
                    if (!this@DiyDialActivity.isDestroyed) {
                        wrUploadDialog.get()?.cancel()
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.ota_device_timeout_tips)
//                                                            FileUtils.deleteAll(path)
                        BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                            FileUtils.deleteAll(it.path)
                        }
                    }
                }
            })
    }

    //endregion


    @SuppressLint("NotifyDataSetChanged")
    private fun refFunctionsAdapter() {
        (binding.rvComplex.adapter as FunctionsAdapter?)?.let {
            it.data = diyWatchFaceConfig!!.functionsConfigs
            it.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refStyleAdapter() {
        (binding.rvStyle.adapter as StyleAdapter?)?.notifyDataSetChanged()
        binding.tvSize.text = AppUtils.biDiFormatterStr(FileUtils.getSizeForKb(styleSelect!!.binData.size / 1024))
    }

    //region 预览
    private fun refPreView() {
        ControlBleTools.getInstance()
            .getPreviewBitmap(getDiyParamsBean(), object : DiyDialPreviewCallBack {
                override fun onDialPreview(preview: Bitmap?) {
                    if (preview != null) {
                        preViewBitmap = preview
                        GlideApp.with(BaseApplication.mContext).load(preview).into(binding.ivPreview)
                    }
                }

                override fun onError(errMsg: String?) {
                    errMsg?.let {
//                    MyApplication.showToast(it)
                    }
                }
            })
    }
    //endregion

    /**
     * 获取diy表盘请求参数
     */
    private fun getDiyParamsBean(): DiyParamsBean {
        val diyParamsBean = DiyParamsBean()
        //json
        diyParamsBean.jsonStr = dataJson
        //背景资源
        val backgroundResBean = DiyParamsBean.BackgroundResBean()
        val background: Bitmap =
            if (viewModel.photoData.value == null) {
                //默认背景
                ConvertUtils.bytes2Bitmap(FileUtils.getBytes(UrlPathUtils.getUrlPath(viewModel.diyInfo.value!!.data.defaultBackgroundImage)))
            } else {
                //本地资源
                viewModel.photoData.value!!
                //TODO 相册/相机
            }

        val backgroundOverlay =
            ConvertUtils.bytes2Bitmap(
                FileUtils.getBytes(UrlPathUtils.getUrlPath(viewModel.diyInfo.value!!.data.backgroundOverlay))
            )
        backgroundResBean.background = background
        backgroundResBean.backgroundOverlay = backgroundOverlay
        diyParamsBean.backgroundResBean = backgroundResBean


        if (styleSelect != null) {
            val styleResBean = DiyParamsBean.StyleResBean()
            styleResBean.type = styleSelect!!.type
            styleResBean.styleBm = styleSelect!!.imgData
            styleResBean.styleBin = styleSelect!!.binData
            diyParamsBean.styleResBean = styleResBean
        }

        //复杂功能
        val functionsResBean = DiyParamsBean.FunctionsResBean()
        functionsResBean.functionsBin =
            FileUtils.getBytes(UrlPathUtils.getUrlPath(viewModel.diyInfo.value!!.data.complicationsBin))

        val functionsBitmapBeans =
            mutableListOf<DiyParamsBean.FunctionsResBean.FunctionsBitmapBean>()

        viewModel.diyInfo.value!!.data.positionList!!.forEach {
            it.dataElementList!!.forEach { it ->
                val functionsBitmapBean = DiyParamsBean.FunctionsResBean.FunctionsBitmapBean()
                if (UrlPathUtils.getUrlPath(it.imgUrl) != null)
                    functionsBitmapBean.bitmap =
                        ConvertUtils.bytes2Bitmap(FileUtils.getBytes(UrlPathUtils.getUrlPath(it.imgUrl)))
                val type = when (it.dataElementCode) {
                    "1" -> DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY.function
                    "2" -> DiyWatchFaceCallBack.DiyWatchFaceFunction.GENERAL_DATE.function
                    "3" -> DiyWatchFaceCallBack.DiyWatchFaceFunction.STEP.function
                    "4" -> DiyWatchFaceCallBack.DiyWatchFaceFunction.CALORIE.function
                    "5" -> DiyWatchFaceCallBack.DiyWatchFaceFunction.HEART_RATE.function
                    else -> DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY.function
                }
                functionsBitmapBean.function = type
                functionsBitmapBeans.add(functionsBitmapBean)
            }
        }

        functionsResBean.functionsBitmaps = functionsBitmapBeans
        //复杂功能资源
        diyParamsBean.functionsResBean = functionsResBean
        //复杂功能设置
        diyParamsBean.diyWatchFaceConfigBean = diyWatchFaceConfig?.clone()
        return diyParamsBean
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSync.id -> {
                dialog?.show()
                ControlBleTools.getInstance()
                    .getDiyDialData(getDiyParamsBean(), object : DiyDialDataCallBack {
                        override fun onDialData(
                            diyDialId: String?,
                            data: ByteArray?,
                            configBean: DiyWatchFaceConfigBean?,
                        ) {
                            if (diyDialId == null || data == null || configBean == null) {
                                DialogUtils.dismissDialog(dialog)
                                return
                            }

                            LogUtils.d(
                                "diyDialId:" + diyDialId + ",Data size = ${data.size}" + "configBean:" + GsonUtils.toJson(
                                    configBean
                                )
                            )
                            //需要更新文件和配置类
                            uploadWatch(diyDialId, data, configBean)
                        }

                        override fun onChangeConfig(configBean: DiyWatchFaceConfigBean?) {
                            if (configBean == null) {
                                DialogUtils.dismissDialog(dialog)
                                return
                            }

                            //只需要更新配置类
                            ControlBleTools.getInstance().setDiyWatchFaceConfig(
                                configBean,
                                object : ParsingStateManager.SendCmdStateListener() {
                                    override fun onState(state: SendCmdState?) {
                                        DialogUtils.dismissDialog(dialog)
                                        DialogUtils.dismissDialog(dialog)
                                        if (state == SendCmdState.SUCCEED) {
                                            diyWatchFaceConfig = configBean
                                        }
                                    }
                                })
                        }

                        override fun onError(errMsg: String?) {
                            errMsg?.let { ToastUtils.showShort(it) }
                            DialogUtils.dismissDialog(dialog)
                        }
                    })
            }
            binding.clUpload.id -> {
                PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    getString(R.string.permission_camera),
                    PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    showAvatarDialog()
                }
            }

        }
    }

    // 弹框的信息
    private var dialogAvatar: Dialog? = null
    private fun showAvatarDialog() {
        if (dialogAvatar != null && dialogAvatar!!.isShowing) {
            return
        }
        val view: View = layoutInflater.inflate(
            R.layout.dialog_photo_choose, null
        )
        dialogAvatar = Dialog(this, R.style.transparentFrameWindowStyle)
        dialogAvatar?.setContentView(
            view, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val window: Window = dialogAvatar?.window!!
        window.setWindowAnimations(R.style.picker_view_slide_anim)
        val wl = window.attributes
        wl.x = 0
        wl.y = windowManager.defaultDisplay.height
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT
        view.findViewById<View>(R.id.photograph).setOnClickListener {
            dialogAvatar?.dismiss()
            delAllCacheImg()
            PermissionUtils.checkRequestPermissions(
                this.lifecycle,
                getString(R.string.permission_camera),
                PermissionUtils.PERMISSION_GROUP_CAMERA
            ) {
                takePictures()
            }
        }
        view.findViewById<View>(R.id.albums).setOnClickListener {
            dialogAvatar?.dismiss()
            delAllCacheImg()
            PermissionUtils.checkRequestPermissions(
                this.lifecycle,
                getString(R.string.permission_camera),
                PermissionUtils.PERMISSION_GROUP_CAMERA
            ) {
                getPhotograph()
            }
        }
        view.findViewById<View>(R.id.cancel).setOnClickListener { dialogAvatar?.dismiss() }
        dialogAvatar?.onWindowAttributesChanged(wl)
        dialogAvatar?.setCanceledOnTouchOutside(true)
        dialogAvatar?.show()
    }

    fun delAllCacheImg() {
        AppUtils.tryBlock {
            com.blankj.utilcode.util.FileUtils.deleteAllInDir(Global.LUBAN_CACHE_DIR)
        }
    }

    /**
     * 拍照
     */
    @SuppressLint("SimpleDateFormat")
    private fun takePictures() {
        takePictureUri = UriUtils.file2Uri(AppUtils.createImageFile())
        takePhotoLauncher.launch(takePictureUri)
    }

    /**
     * 获取相册资源
     */
    private fun getPhotograph() {
        try {
            val intent = Intent(Intent.ACTION_PICK, null)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            this.startActivityForResult(intent, RESULT_GET_PHOTOGRAPH_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
            com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_GET_PHOTOGRAPH_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (data == null || data.data == null) {
                        com.smartwear.publicwatch.utils.LogUtils.d(TAG, "获取相册异常：data == null")
                        photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                            log = "获取相册异常：data == null"
                        }
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, photoTrackingLog!!, "1812", true)
                        return
                    }
                    try {
                        val resFile = UriUtils.uri2File(data.data)
                        // 跳转到图片裁剪页面，copy相册文件到缓存路径，防止裁剪异常导致相册文件出错
                        val destFile = AppUtils.copyPhotograph(resFile)
                        if (destFile == null) {
                            com.smartwear.publicwatch.utils.LogUtils.d(TAG, "获取相册异常：复制相册图片失败！")
                            photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                                log = "获取相册异常：复制相册图片失败！"
                            }
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, photoTrackingLog!!, "1812", true)
                            com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                            return
                        }
                        cropImage(UriUtils.file2Uri(destFile))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        com.smartwear.publicwatch.utils.LogUtils.d(TAG, "获取相册异常：$e")
                        photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                            log = "获取相册异常：$e"
                        }
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, photoTrackingLog!!, "1812", true)
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                //裁剪后
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        com.smartwear.publicwatch.utils.LogUtils.d(TAG, "裁剪异常：data == null")
                        cropTrackingLog = TrackingLog.getAppTypeTrack("裁剪图片资源错误").apply {
                            log = "裁剪异常：data == null"
                        }
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, cropTrackingLog!!, "1813", true)
                        delAllCacheImg()
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                        return
                    }
                    if (dialUri != null) {
                        //删除复制来的相册资源
                        com.blankj.utilcode.util.FileUtils.delete(UriUtils.uri2File(dialUri))
                    }
                    dialUri = UCrop.getOutput(data)
                    cropDone()
                } else {
                    if (data != null) {
                        val cropError = UCrop.getError(data)
                        if (cropError != null) {
                            cropError.printStackTrace()
                            com.smartwear.publicwatch.utils.LogUtils.d(TAG, "裁剪异常：$cropError")
                            cropTrackingLog = TrackingLog.getAppTypeTrack("裁剪图片资源错误").apply {
                                log = "裁剪异常：$cropError"
                            }
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, cropTrackingLog!!, "1813", true)
                            com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                        }
                    }
                    delAllCacheImg()
                }
            }
        }
    }


    private fun cropImage(uri: Uri) {
        cropUri = UriUtils.file2Uri(AppUtils.createImageFile())
        dialUri = AppUtils.limitMaximumBitmap(uri)
        val option = UCrop.Options()
        //状态栏色
        option.setStatusBarColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //标题栏色
        option.setToolbarColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //背景色
        option.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //标题控件-取消按钮
        option.setToolbarCancelDrawable(R.mipmap.img_back_btn)
        //标题控件-完成按钮
        option.setToolbarCropDrawable(R.mipmap.scan_right_finish)
        //标题文字
        option.setToolbarTitle("")
        //标题控件颜色
        option.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.color_FFFFFF))
        //底部控制按钮选中状态色
        option.setActiveControlsWidgetColor(
            ContextCompat.getColor(
                this,
                R.color.selector_public_button_on
            )
        )
        //圆形
        option.setCircleDimmedLayer(TextUtils.equals(deviceShape, "2"))
        //启动裁剪 //裁剪后再缩放
        UCrop.of(dialUri!!, cropUri!!)
            .withOptions(option)
            .withAspectRatio(deviceWith * 1f, deviceHeith * 1f)
            .withMaxResultSize(3000, 3000)
            .start(this)
    }

    /**
     * 裁剪完成
     */
    private fun cropDone() {
        if (dialUri == null) {
            com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
            return
        }
        //压缩
        Luban.with(this)
            .load(UriUtils.uri2File(dialUri))
            .ignoreBy(100) //压缩上限 100KB
            .filter { path ->
                !(TextUtils.isEmpty(path) || path.toLowerCase(Locale.ENGLISH).endsWith(".gif"))
            }
            .setTargetDir(Global.LUBAN_CACHE_DIR)
            .setCompressListener(object : OnCompressListener {
                override fun onStart() {
                }

                override fun onSuccess(file: File?) {
                    if (file == null) {
                        com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                        return
                    }
                    AppUtils.tryBlock(getString(R.string.img_fail_tip)) {
                        val bs = FileIOUtils.readFile2BytesByStream(file)
                        var bitmap = ConvertUtils.bytes2Bitmap(bs)
                        bitmap = BmpUtils.zoomImg(bitmap, deviceWith, deviceHeith)
                        //ImageUtils.save(bitmap,AppUtils.createImageFile(), Bitmap.CompressFormat.JPEG)
                        if (TextUtils.equals(deviceShape, "2")) {
                            newBgBitmap = BmpUtils.getCoverBitmap(this@DiyDialActivity, bitmap)
                            viewModel.photoData.value =
                                BmpUtils.getCoverBitmap2_1(this@DiyDialActivity, bitmap)
                        } else {
                            newBgBitmap = bitmap
                            viewModel.photoData.value = bitmap
                        }
                    }
                }

                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                    com.smartwear.publicwatch.utils.ToastUtils.showToast(R.string.img_fail_tip)
                }

            }).launch()
    }

    /**
     * 上传表盘日志
     * 1、表盘下载 2、表盘传输 3、表盘传输成功 4、传输失败
     */
    fun postDialLog(dataType: Int) {
        //上传表盘
        intent.getStringExtra("dialId")?.let {
//            viewModel.postDialLog(it, if (clockDialDataFormat == 0) 1 else 2, dataType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }
}