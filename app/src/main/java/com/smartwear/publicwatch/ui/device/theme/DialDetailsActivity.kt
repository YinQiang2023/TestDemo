package com.smartwear.publicwatch.ui.device.theme

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.ActivityDialDetailsBinding
import com.smartwear.publicwatch.databinding.ItemDialDetailsBinding
import com.smartwear.publicwatch.databinding.ItemDialMoreBinding
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.dialog.DownloadDialog
import com.smartwear.publicwatch.expansion.postDelay
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.download.DownloadListener
import com.smartwear.publicwatch.https.download.DownloadManager
import com.smartwear.publicwatch.https.response.DialInfoResponse
import com.smartwear.publicwatch.receiver.SifliReceiver
import com.smartwear.publicwatch.ui.adapter.MultiItemCommonAdapter
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.bean.DeviceSettingBean
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.livedata.RefreshMyDialListState
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.FileUtils
import com.smartwear.publicwatch.utils.LogUtils
import com.smartwear.publicwatch.utils.PermissionUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.utils.manager.WakeLockManager
import com.smartwear.publicwatch.view.ColorPickerView
import com.smartwear.publicwatch.view.ColorRoundView
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.sifli.watchfacelibrary.SifliWatchfaceService
import com.yalantis.ucrop.UCrop
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThemeManager
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.callback.WatchFaceInstallCallBack
import com.zhapp.ble.custom.CustomClockDialNewUtils
import com.zhapp.ble.custom.MyCustomClockUtils
import com.zhapp.ble.utils.UnitConversionUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

class DialDetailsActivity : BaseActivity<ActivityDialDetailsBinding, DeviceModel>(ActivityDialDetailsBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private var TAG = DialDetailsActivity::class.java.simpleName

    private var groupDialList = mutableListOf<DialInfoResponse.GroupDial>()
    private var dialFileList = mutableListOf<DialInfoResponse.DialFile>()
    private var adapter: MultiItemCommonAdapter<DialInfoResponse.GroupDial, ItemDialMoreBinding>? = null
    private var dialog: Dialog? = null
    private var fileSize = "0"
    private var dialCode = "0"
    private var oldTextBitmap: Bitmap? = null
    private var newTextBitmap: Bitmap? = null
    private var oldBgBitmap: Bitmap? = null
    private var newBgBitmap: Bitmap? = null
    private var photoBitmap: Bitmap? = null
    private var color_r = 255
    private var color_g = 255
    private var color_b = 255
    private var clickCount = 0
    private var dialType = 0
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private val RESULT_GET_PHOTOGRAPH_CODE = 0x161
    private var takePictureUri: Uri? = null     //拍照
    private var cropUri: Uri? = null //裁剪
    private var dialUri: Uri? = null //最终资源uri
    private var deviceWith = 0
    private var deviceHeith = 0
    private var deviceShape = ""  //主题形状 = 0=方形/1=球拍/2=圆形/3=圆角矩形1
    private var clockDialDataFormat = 0

    lateinit var layountInflater: LayoutInflater
    private val colorList = intArrayOf(
        R.color.theme_color1,
        R.color.theme_color2,
        R.color.theme_color3,
        R.color.theme_color4,
        R.color.theme_color5,
        R.color.theme_color6,
        R.color.theme_color7,
        R.color.theme_color8,
        R.color.theme_color9
    )

    //获取表盘详情
    //private val dialInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("app获取后台表盘详情", "表盘详情", "ffit/dial/info") }
    private val dialInfoTrackingLog: MutableList<TrackingLog> = mutableListOf()

    //相机权限未开启异常
    private var perMissTrackingLog: TrackingLog? = null

    //相册资源获取失败异常
    private var photoTrackingLog: TrackingLog? = null

    //裁剪异常
    private var cropTrackingLog: TrackingLog? = null

    //在线表盘发送状态
    private val fileStateTrackingLog by lazy { TrackingLog.getDevTyepTrack("请求设备传文件状态", "获取发送表盘文件状态", "PREPARE_INSTALL_WATCH_FACE") }

    private var isSending = false

    //产品功能列表
    private val deviceSettingBean by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    //下载中
    private val downloadDialog by lazy {
        DownloadDialog(
            this,
            BaseApplication.mContext.getString(R.string.theme_center_dial_down_load_title), ""
        )
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.layoutCustomizeColor.id -> {
                showSelectColor()
            }

            binding.ivCustomizeColor.id -> {
                showSelectColor()
            }

            binding.viewNoData.btnRefresh.id -> {
                val tLog = TrackingLog.getSerTypeTrack("app获取后台表盘详情", "表盘详情", "ffit/dial/info")
                dialInfoTrackingLog.add(tLog)
                intent.getStringExtra("dialId")?.let { viewModel.dialInfo(it, tLog) }
                dialog?.show()
            }

            binding.layoutSelectPicture.id -> {

                PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    getString(R.string.permission_camera),
                    PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    showAvatarDialog()
                }
            }

            binding.btnSync.id -> {
                for (dLog in dialInfoTrackingLog) {
                    if (TextUtils.equals(dLog.serResult, "失败")) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, dLog, "1810", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, dLog)
                    }
                }
                val last = dialInfoTrackingLog.get(dialInfoTrackingLog.size - 1)
                dialInfoTrackingLog.clear()
                dialInfoTrackingLog.add(last)

                if (dialType == 0) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("在线表盘"))
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("相册表盘"))
                    if (photoTrackingLog != null) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, photoTrackingLog!!, "1812", true)
                        photoTrackingLog = null
                    }
                    if (cropTrackingLog != null) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, cropTrackingLog!!, "1813", true)
                        cropTrackingLog = null
                    }
                }

                PermissionUtils.checkRequestPermissions(this.lifecycle, getString(R.string.permission_camera), PermissionUtils.PERMISSION_GROUP_CAMERA) {
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return@checkRequestPermissions
                    }
                    if (!NetworkUtils.isConnected()) {
                        ToastUtils.showToast(R.string.not_network_tips)
                        return@checkRequestPermissions
                    }

                    if (dialType == 0) {
                        downloadOnlineDial()
                    } else {
                        downloadCustomDial()
                    }
                }
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        setTvTitle(R.string.theme_center_dial_details_title)
        AppUtils.registerEventBus(this)
        binding.rvList.apply {
            adapter = initAdapter()
            this@DialDetailsActivity.adapter = adapter as MultiItemCommonAdapter<DialInfoResponse.GroupDial, ItemDialMoreBinding>
            layoutManager = LinearLayoutManager(this.context)
            (layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
            setHasFixedSize(true)
        }
        layountInflater = LayoutInflater.from(this)
        setViewsClickListener(
            this,
            binding.layoutCustomizeColor,
            binding.ivCustomizeColor,
            binding.btnSync,
            binding.layoutSelectPicture,
            binding.viewNoData.btnRefresh
        )
        initColorLayout()
        dialog = DialogUtils.showLoad(this)
        dialog?.setCancelable(false)
        observe()
        intent.getStringExtra("dialId")?.let {
            val tLog = TrackingLog.getSerTypeTrack("app获取后台表盘详情", "表盘详情", "ffit/dial/info")
            dialInfoTrackingLog.add(tLog)
            if (SpUtils.getSPUtilsInstance().getString(SpUtils.CURRENT_FIRMWARE_PLATFORM, "") ==
                Global.FIRMWARE_PLATFORM_SIFLI
            ) {
                viewModel.siflidialInfo(it, tLog)
            } else {
                viewModel.dialInfo(it, tLog)
            }

        }
        dialog?.show()

        WakeLockManager.instance.keepUnLock(this.lifecycle)

        CallBackUtils.setWatchFaceInstallCallBack(object : WatchFaceInstallCallBack {
            override fun onresult(result: WatchFaceInstallResultBean?) {
                com.blankj.utilcode.util.LogUtils.d("表盘安装结果：$result")
                DialogUtils.dismissDialog(dialog)
                if (dialInstallTask != null) {
                    ThreadUtils.cancel(dialInstallTask)
                }
                when (result?.code) {
                    WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state -> {
                        //安装成功
                        ToastUtils.showToast(getString(R.string.watch_face_suc))
                    }

                    WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_FAILED.state -> {
                        ToastUtils.showToast(getString(R.string.watch_face_fai))
                    }

                    WatchFaceInstallCallBack.WatchFaceInstallCode.VERIFY_FAILED.state -> {
                        ToastUtils.showToast(getString(R.string.watch_face_verify_fai))
                    }
                }
            }
        })

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result == true) {
                if (takePictureUri != null) {
                    cropImage(takePictureUri!!)
                }
            } else {
                delAllCacheImg()
            }
        }

        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getStartTypeTrack("表盘"), isStart = true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() {
        viewModel.dialInfo.observe(this, Observer {
            DialogUtils.dismissDialog(dialog)
            if (it == null) return@Observer
            if (!TextUtils.isEmpty(it.code)) {
                if (it.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    com.blankj.utilcode.util.LogUtils.i(TAG, "后台返回-成功-dialInfo = $it")
                    binding.viewHaveData.visibility = View.VISIBLE
                    binding.viewNoData.layoutMain.visibility = View.GONE

                    binding.tvName.text = it.data.dialName
//            binding.tvSize.text = "${UnitConversionUtils.bigDecimalFormat(it.data.binSize.trim().toFloat()/1024)} KB"
//                    binding.tvSize.text = "${it.data.binSize.trim()}KB"
                    binding.tvSize.text = AppUtils.biDiFormatterStr("${FileUtils.getSizeForKb(it.data.binSize.trim().toInt())}")
                    dialCode = it.data.dialCode
                    fileSize = it.data.binSize
                    binding.tvIntroduction.text = it.data.dialDescribe
                    groupDialList.clear()
                    dialFileList.clear()
                    groupDialList.addAll(it.data.groupDialList)
                    dialFileList.addAll(it.data.dialFileList)
                    dialType = it.data.clockDialType.trim().toInt()
                    deviceWith = it.data.deviceWidth.trim().toInt()
                    deviceHeith = it.data.deviceHeight.trim().toInt()
                    deviceShape = it.data.deviceShape.trim()
//                    deviceShape = "2"//模拟圆屏
                    clockDialDataFormat = it.data.clockDialDataFormat.trim().toInt()
                    if (it.data.clockDialType.trim().toInt() == 0) {
                        binding.layoutCustomize.visibility = View.GONE
                        binding.layoutSelectPicture.visibility = View.GONE
                        GlideApp.with(this).load(it.data.dialFileList[2].dialFileUrl)
                            .into(object : CustomTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    val bitmapDrawable: BitmapDrawable = resource as BitmapDrawable
                                    val bitmap: Bitmap = bitmapDrawable.bitmap
                                    if (TextUtils.equals(deviceShape, "2")) {
                                        binding.ivThemeMain.setImageBitmap(BmpUtils.getCoverBitmap2_1(this@DialDetailsActivity, bitmap))
                                    } else {
                                        binding.ivThemeMain.setImageBitmap(bitmap)
                                    }
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                            })

                    } else {
                        binding.layoutCustomize.visibility = View.VISIBLE
                        binding.layoutSelectPicture.visibility = View.VISIBLE
                        if (it.data.dialFileList.size >= 6) {
                            GlideApp.with(this).load(it.data.dialFileList[4].dialFileUrl)
                                .into(object : CustomTarget<Drawable>() {
                                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                        val bitmapDrawable: BitmapDrawable = resource as BitmapDrawable
                                        val bitmap: Bitmap = bitmapDrawable.bitmap
                                        oldBgBitmap = bitmap
                                        newBgBitmap = if (photoBitmap == null) bitmap else photoBitmap
                                        if (TextUtils.equals(deviceShape, "2")) {
                                            binding.ivThemeMain.setImageBitmap(BmpUtils.getCoverBitmap2_1(this@DialDetailsActivity, newBgBitmap))
                                        } else {
                                            binding.ivThemeMain.setImageBitmap(newBgBitmap)
                                        }
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                    }

                                })

                            GlideApp.with(this).load(it.data.dialFileList[5].dialFileUrl).into(object : CustomTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    val bitmapDrawable: BitmapDrawable = resource as BitmapDrawable
                                    val bitmap: Bitmap = bitmapDrawable.bitmap
                                    oldTextBitmap = bitmap
                                    newTextBitmap = MyCustomClockUtils.getNewTextBitmap(oldTextBitmap, color_r, color_g, color_b)
                                    binding.ivThemeText.setImageBitmap(newTextBitmap)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {

                                }

                            })
                        }
                    }
                } else {
                    binding.viewHaveData.visibility = View.GONE
                    binding.viewNoData.layoutMain.visibility = View.VISIBLE
                    LogUtils.i(TAG, "表盘详情获取后台返回-失败")
                    ToastUtils.showToast(getString(R.string.err_network_tips))
                }
            }
            adapter?.notifyDataSetChanged()
        })

        viewModel.siflidialInfo.observe(this) {
            DialogUtils.dismissDialog(dialog)
            if (it == null) return@observe
            if (!TextUtils.isEmpty(it.code)) {
                if (it.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    com.blankj.utilcode.util.LogUtils.i(TAG, "后台返回-成功-siflidialInfo = $it")
                    binding.viewHaveData.visibility = View.VISIBLE
                    binding.viewNoData.layoutMain.visibility = View.GONE

                    binding.tvName.text = it.data.dialName
                    dialCode = it.data.dialCode
                    binding.tvIntroduction.text = it.data.dialDescribe
                    groupDialList.clear()
                    dialFileList.clear()
                    it.data.groupDialList?.let { groups ->
                        groupDialList.addAll(groups)
                    }
                    if (!it.data.dialFileUrl.isNullOrEmpty()) {
                        val file = DialInfoResponse.DialFile()
                        file.dialFileUrl = it.data.dialFileUrl
                        dialFileList.add(file)
                    }
                    if (!it.data.binSize.isNullOrEmpty()) {
                        binding.tvSize.text = "${UnitConversionUtils.bigDecimalFormat(it.data.binSize.trim().toFloat() / 1024)} KB"
                    }
                    binding.layoutCustomize.visibility = View.GONE
                    binding.layoutSelectPicture.visibility = View.GONE

                    if (!it.data.effectImgUrl.isNullOrEmpty()) {
                        GlideApp.with(this).load(it.data.effectImgUrl)
                            .into(object : CustomTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    val bitmapDrawable: BitmapDrawable = resource as BitmapDrawable
                                    val bitmap: Bitmap = bitmapDrawable.bitmap
                                    binding.ivThemeMain.setImageBitmap(bitmap)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {

                                }
                            })
                    }

                    for (i in 0 until groupDialList.size) {
                        if (groupDialList[i].dialId == it.data.dialId) {
                            clickCount = i
                        }
                    }
                } else {
                    binding.viewHaveData.visibility = View.GONE
                    binding.viewNoData.layoutMain.visibility = View.VISIBLE
                    LogUtils.i(TAG, "表盘详情获取后台返回-失败")
                    ToastUtils.showToast(getString(R.string.err_network_tips))
                }
                adapter?.notifyDataSetChanged()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun eventBusMsg(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        LogUtils.i(TAG, "device connected")
                    }

                    BleCommonAttributes.STATE_CONNECTING -> {
                        LogUtils.i(TAG, "device connecting")
                    }

                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        LogUtils.e(TAG, "device disconnect")
//                        ToastUtils.showToast(R.string.device_disconnected)
//                        finish()
                        if (isSending) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "1817", true)
                        }
                    }
                }
            }

            EventAction.ACTION_SIFLI_FACE_PROGRESS -> {
                val progress = msg.arg as Int?
                com.blankj.utilcode.util.LogUtils.d("ACTION_SIFLI_FACE_PROGRESS:" + progress)
                sifliFaceProgress(progress)
            }

            EventAction.ACTION_SIFLI_FACE_STATE -> {
                val faceState = msg.obj as SifliReceiver.SifliFaceState
                sifliFaceState(faceState)
            }
        }
    }

    private fun initAdapter(): MultiItemCommonAdapter<DialInfoResponse.GroupDial, ItemDialDetailsBinding> {
        return object :
            MultiItemCommonAdapter<DialInfoResponse.GroupDial, ItemDialDetailsBinding>(groupDialList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemDialDetailsBinding {
                return ItemDialDetailsBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun convert(v: ItemDialDetailsBinding, t: DialInfoResponse.GroupDial, position: Int) {
                GlideApp.with(this@DialDetailsActivity).load(t.effectImgUrl)
//                        .resize(ConvertUtils.dp2px(120f),
//                                ConvertUtils.dp2px(120f))
                    .into(v.ivItem)
//                v.tvItemName.text = t.dialName
                v.tvItemName.visibility = View.GONE
                if (clickCount == position) {
                    v.ivItem.setBackgroundResource(R.drawable.clock_dial_bg_select_on_2)
                } else {
                    v.ivItem.setBackgroundResource(R.drawable.clock_dial_bg_select_off_2)
                }

                setViewsClickListener({
                    val tLog = TrackingLog.getSerTypeTrack("app获取后台表盘详情", "表盘详情", "ffit/dial/info")
                    dialInfoTrackingLog.add(tLog)
                    if (SpUtils.getSPUtilsInstance().getString(SpUtils.CURRENT_FIRMWARE_PLATFORM, "") ==
                        Global.FIRMWARE_PLATFORM_SIFLI
                    ) {
                        viewModel.siflidialInfo(t.dialId, tLog)
                    } else {
                        viewModel.dialInfo(t.dialId, tLog)
                    }
                    dialog?.show()
                    clickCount = position
                    notifyDataSetChanged()
                }, v.lyItem)
            }

            override fun getItemType(t: DialInfoResponse.GroupDial): Int {
                return 0
            }

        }
    }

    /**
     *
     * 颜色选择器
     */
    private fun initColorLayout() {
        binding.layoutColor.removeAllViews()
        for (i in colorList.indices) {
            val mLinearLayout =
                layountInflater.inflate(R.layout.theme_color_layout, null) as LinearLayout

            val colorRoundView = mLinearLayout.findViewById<ColorRoundView>(R.id.colorRoundView)
            val ivColorBg = mLinearLayout.findViewById<ImageView>(R.id.ivColorBg)
            colorRoundView.setBgColor(colorList[i], colorList[i])
            colorRoundView.setOnClickListener {
                for (i in colorList.indices) {
                    val childView = binding.layoutColor.getChildAt(i)
                    val childViewColorBg = childView.findViewById<ImageView>(R.id.ivColorBg)
                    childViewColorBg.background =
                        ContextCompat.getDrawable(this, R.color.transparent)
                }
                ivColorBg.background =
                    ContextCompat.getDrawable(this, R.drawable.theme_select_circle)

                val color: Int = colorRoundView.getcolor()
                setColor(Color.red(color), Color.green(color), Color.blue(color))
            }
            binding.layoutColor.addView(mLinearLayout)
        }
    }

    /**
     * 相册表盘-设置颜色
     */
    private fun setColor(r: Int, g: Int, b: Int) {
        val mainScope = MainScope()
        mainScope.launch {
            color_r = r
            color_g = g
            color_b = b
            if (oldTextBitmap == null) {
                return@launch
            }
            newTextBitmap = MyCustomClockUtils.getNewTextBitmap(oldTextBitmap, r, g, b)
            binding.ivThemeText.setImageBitmap(newTextBitmap)
            mainScope.cancel()
        }
    }

    // 弹框的信息
    private lateinit var dialogColor: Dialog

    private fun showSelectColor() { // TODO Auto-generated method stub
        val view = layoutInflater.inflate(R.layout.select_color_layout, null)
        dialogColor = Dialog(this, R.style.transparentdialog)
        dialogColor.setContentView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val window: Window = dialogColor.window!!
        window.setWindowAnimations(R.style.main_menu_animstyle)
        val wl = window.attributes
        wl.x = 0
        wl.y = windowManager.defaultDisplay.height
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT
        val colorPickerView: ColorPickerView = view.findViewById(R.id.colorPickerView)
        colorPickerView.setOnColorChangedListenner { color, originalColor, saturation ->
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)
            setColor(red, green, blue)
        }
        view.findViewById<View>(R.id.ivCancel).setOnClickListener { dialogColor?.dismiss() }
        dialogColor.onWindowAttributesChanged(wl)
        dialogColor.setCanceledOnTouchOutside(true)
        dialogColor.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_GET_PHOTOGRAPH_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (data == null || data.data == null) {
                        LogUtils.d(TAG, "获取相册异常：data == null", true)
                        photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                            log = "获取相册异常：data == null"
                        }
                        return
                    }
                    try {
                        val resFile = UriUtils.uri2File(data.data)
                        // 跳转到图片裁剪页面，copy相册文件到缓存路径，防止裁剪异常导致相册文件出错
                        val destFile = AppUtils.copyPhotograph(resFile)
                        if (destFile == null) {
                            LogUtils.d(TAG, "获取相册异常：复制相册图片失败！", true)
                            photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                                log = "获取相册异常：复制相册图片失败！"
                            }
                            ToastUtils.showToast(R.string.img_fail_tip)
                            return
                        }
                        cropImage(UriUtils.file2Uri(destFile))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LogUtils.d(TAG, "获取相册异常：$e", true)
                        photoTrackingLog = TrackingLog.getAppTypeTrack("获取相册图片资源错误").apply {
                            log = "获取相册异常：$e"
                        }
                        ToastUtils.showToast(R.string.img_fail_tip)
                    }
                }
            }

            UCrop.REQUEST_CROP -> {
                //裁剪后
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        LogUtils.d(TAG, "裁剪异常：data == null", true)
                        cropTrackingLog = TrackingLog.getAppTypeTrack("裁剪图片资源错误").apply {
                            log = "裁剪异常：data == null"
                        }
                        delAllCacheImg()
                        ToastUtils.showToast(R.string.img_fail_tip)
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
                            LogUtils.d(TAG, "裁剪异常：$cropError", true)
                            cropTrackingLog = TrackingLog.getAppTypeTrack("裁剪图片资源错误").apply {
                                log = "裁剪异常：$cropError"
                            }
                            ToastUtils.showToast(R.string.img_fail_tip)
                        }
                    }
                    delAllCacheImg()
                }
            }
        }
    }

    /**
     * 下载在线表盘
     */
    private fun downloadOnlineDial() {
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_DIAL)
        ErrorUtils.onLogResult("sendOnlineDial downFile start")
        LogUtils.i(TAG, "sendOnlineDial downFile start", true)
        LogUtils.i(TAG, "sendOnlineDial downFile dialCode = " + dialCode)
        var url = ""
        if (SpUtils.getSPUtilsInstance().getString(SpUtils.CURRENT_FIRMWARE_PLATFORM, "") ==
            Global.FIRMWARE_PLATFORM_SIFLI
        ) {
            url = dialFileList[0].dialFileUrl
        } else {
            if (dialFileList.isEmpty()) {
                DialogUtils.dismissDialog(dialog)
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)
                return
            }
            url = if (clockDialDataFormat == 0) {
                dialFileList[0].dialFileUrl
            } else {
                dialFileList[1].dialFileUrl
            }
        }

        val downloadTrackingLog = TrackingLog.getSerTypeTrack("下载表盘资源", "下载", url)
        dialog?.show()
        DownloadManager.download(url, listener = object : DownloadListener {
            override fun onStart() {
                com.blankj.utilcode.util.LogUtils.d("start download $url")
                downloadTrackingLog.log += "start download"
                //上传表盘下载
                postDialLog(1)
                downloadDialog.progressView?.max = 0
                downloadDialog.progressView?.progress = 0
                downloadDialog.tvProgress?.text = "0%"
                downloadDialog.tvSize?.text = "0/0"
                downloadDialog.showDialog()
            }

            override fun onProgress(totalSize: Long, currentSize: Long) {
                com.blankj.utilcode.util.LogUtils.d("download onProgress totalSize: $totalSize,currentSize: $currentSize")
                //downloadTrackingLog.log += "\ndownload onProgress totalSize: $totalSize,currentSize: $currentSize"

                downloadDialog.progressView?.max = totalSize.toInt()
                downloadDialog.progressView?.progress = currentSize.toInt()
                downloadDialog.tvProgress?.text = "${((currentSize * 1f / totalSize) * 100).toInt()}%"
                downloadDialog.tvSize?.text = "${FileUtils.getSize(currentSize)}/${FileUtils.getSize(totalSize)}"
            }

            override fun onFailed(msg: String) {
                ToastUtils.showToast(getString(R.string.err_network_tips))
                LogUtils.e(TAG, "sendOnlineDial download onFailed: $msg", true)
                ErrorUtils.onLogResult("sendOnlineDial download onFailed: $msg")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)
                DialogUtils.dismissDialog(dialog)
                downloadDialog.cancel()
                downloadTrackingLog.log += "\nsendCustomDial download onFailed: $msg"
                downloadTrackingLog.endTime = TrackingLog.getNowString()
                downloadTrackingLog.serResult = "失败"
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, downloadTrackingLog.apply {
                    log += "\n下载表盘资源失败/超时"
                }, "1814", true)
            }

            override fun onSucceed(path: String) {
                LogUtils.d(TAG, "download onSucceed $path", true)
                downloadDialog.cancel()
                downloadTrackingLog.log += "\ndownload onSucceed $path"
                downloadTrackingLog.endTime = TrackingLog.getNowString()
                downloadTrackingLog.serResult = "成功"
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, downloadTrackingLog)
                //TODO 区分平台
                val firmware = SpUtils.getSPUtilsInstance().getString(SpUtils.CURRENT_FIRMWARE_PLATFORM, "")
                if (firmware == Global.FIRMWARE_PLATFORM_SIFLI) {
                    //思澈平台表盘处理
                    getSifliSendState(path)
                } else {
                    sendOnlineDial(path)
                }
            }
        })
    }

    /**
     * 发送在线表盘
     */
    private fun sendOnlineDial(path: String) {
        val fileByte: ByteArray? = FileUtils.getBytes(path!!)
        LogUtils.i(TAG, "sendOnlineDial downFile Success")


        val uploadDialog =
            DownloadDialog(this@DialDetailsActivity, getString(R.string.theme_center_dial_up_load_title), groupDialList[clickCount].effectImgUrl)
        val version = "111"
        val md5 = if (clockDialDataFormat == 0) {
            dialFileList[0].md5Value
        } else {
            dialFileList[1].md5Value
        }
        if (fileByte != null) {
            postDialLog(2)
            LogUtils.i(TAG, "sendOnlineDial getDeviceWatchFace downFile 222 dialCode = $dialCode")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件"))

            fileStateTrackingLog.startTime = TrackingLog.getNowString()

            ControlBleTools.getInstance().getDeviceWatchFace(
                dialCode,
                fileByte.size,
                false,
                MyOnlineDialWatchFaceFileStatusListener(this@DialDetailsActivity, uploadDialog, fileByte, TAG)
            )
        } else {
            ErrorUtils.onLogResult("sendOnlineDial file is null")
            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件").apply {
                log += "在线文件为空\n获取表盘文件失败"
            }, "1815", true)
        }
    }

    /**
     * 在线表盘状态接口回调
     */
    class MyOnlineDialWatchFaceFileStatusListener(
        activity: DialDetailsActivity, uploadDialog: DownloadDialog,
        var fileByte: ByteArray, var TAG: String,
    ) : DeviceWatchFaceFileStatusListener {
        private var wrActivity: WeakReference<DialDetailsActivity>? = null
        private var wrUploadDialog: WeakReference<DownloadDialog>? = null

        //BUG:表盘传输中来电，APP会卡在表盘传输中，无法退出
        //来电后弱引用wrUploadDialog被GC清空，增加强引用（一旦指向的对象没有更多的强/软引用并且 GC具有回收的内存，WeakReference将返回null）
        private var strongDialog: DownloadDialog? = null

        init {
            wrActivity = WeakReference(activity)
            strongDialog = uploadDialog
            wrUploadDialog = WeakReference(uploadDialog)
            if (wrActivity?.get() == null) {
                LogUtils.e("DialDetailsActivity", "DialDetailsActivity is Null")
            }
        }

        override fun onSuccess(statusValue: Int, statusName: String?) {
            LogUtils.i(
                TAG, "sendOnlineDial getDeviceWatchFace statusValue " +
                        "= ${wrActivity?.get()?.dialCode} statusName = $statusName", true
            )
            DialogUtils.dismissDialog(wrActivity?.get()?.dialog)

            if (statusName != "READY") {
                wrActivity?.get()?.postDelay(100) {
                    wrActivity?.get()?.postDialLog(4)
                    DialogUtils.dismissDialog(wrActivity?.get()?.dialog)
                }
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    if (statusName != "LOW_BATTERY") {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this.apply {
                            log += "\nstatus == $statusName != READY\n请求文件状态超时/失败"
                        }, "1816", true)
                    }
                }
            } else {
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    endTime = TrackingLog.getNowString()
                    devResult = "statusValue:$statusValue,statusName : $statusName"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this)
                }
            }

            when (statusName) {
                //准备好了
                "READY" -> {
                    wrUploadDialog?.get()?.showDialog()
                    wrUploadDialog?.get()?.tvSize?.text =
                        wrActivity?.get()?.getString(R.string.theme_center_dial_up_load_tips)

                    val uploadTrackingLog = TrackingLog.getDevTyepTrack("传输表盘文件", "上传大文件数据", "sendThemeByProto4").apply {
                        log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_WATCH},fileByte:${fileByte.size},isResumable:true"
                    }
                    wrActivity?.get()?.isSending = true
                    ControlBleTools.getInstance().startUploadBigData(
                        BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                        fileByte, true,
                        object : UploadBigDataListener {
                            override fun onSuccess() {
                                wrActivity?.get()?.isSending = false
                                strongDialog = null
                                wrUploadDialog?.get()?.cancel()
                                wrUploadDialog?.get()?.tvSize?.text =
                                    wrActivity?.get()?.getString(R.string.theme_center_dial_up_load_tips)
                                LogUtils.i(TAG, "sendOnlineDial startUploadBigData onSuccess", true)

                                uploadTrackingLog.log += "\nsendOnlineDial startUploadBigData onSuccess"
                                uploadTrackingLog.endTime = TrackingLog.getNowString()
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog)
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getEndTypeTrack("表盘"), isEnd = true)

                                AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("8", "19").apply {
                                    functionStatus = "1"
                                })

                                ToastUtils.showToast(R.string.sync_success_tips)
                                BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                                    FileUtils.deleteAll(it.path)
                                }

                                if (ThemeManager.getInstance().packetLossTimes > 0) {
                                    wrActivity?.get()?.postDelay(100) {
                                        ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                                    }
                                }
                                wrActivity?.get()?.postDialLog(3)
                                RefreshMyDialListState.postValue(true)
                                ErrorUtils.clearErrorBigData()

                                //根据后台是否配置启用表盘安装结果，继续弹窗等待安装
                                if (wrActivity?.get()?.deviceSettingBean?.settingsRelated?.dial_installation_completed == true) {
                                    wrActivity?.get()?.dealDialInstall()
                                }
                            }

                            @SuppressLint("SetTextI18n")
                            override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                                val percentage = curPiece * 100 / dataPackTotalPieceLength
                                Log.i(TAG, "onProgress $percentage")
                                ThreadUtils.runOnUiThread {
                                    wrUploadDialog?.get()?.progressView?.max = dataPackTotalPieceLength
                                    wrUploadDialog?.get()?.progressView?.progress = curPiece
                                    wrUploadDialog?.get()?.tvProgress?.text = "${
                                        ((curPiece / dataPackTotalPieceLength.toFloat()) * 100).toInt()
                                    }%"
                                }
                                uploadTrackingLog.log += "\nonProgress $percentage"
                            }

                            override fun onTimeout() {
                                if (wrActivity?.get()?.isSending == true) {
                                    LogUtils.e(TAG, "sendOnlineDial startUploadBigData onTimeout", true)
                                    ErrorUtils.onLogResult("OnlineDial startUploadBigData timeOut")
                                    strongDialog = null
                                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                                    wrActivity?.get()?.postDialLog(4)
                                    uploadTrackingLog.log += "\nsendOnlineDial startUploadBigData onTimeout"
                                    uploadTrackingLog.endTime = TrackingLog.getNowString()
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog.apply {
                                        log += "\n发送响应超时/失败"
                                    }, "1818", true)
                                }
                                wrActivity?.get()?.isSending = false
                                if (!(wrActivity?.get() != null && wrActivity!!.get()!!.isDestroyed)) {
                                    ThreadUtils.runOnUiThread {
                                        wrUploadDialog?.get()?.cancel()
                                        ToastUtils.showToast(R.string.ota_device_timeout_tips)
                                    }
                                }
                                BaseApplication.application.getExternalFilesDir(
                                    Environment.DIRECTORY_DOWNLOADS
                                )?.let {
                                    FileUtils.deleteAll(it.path)
                                }
                            }
                        })
                }
                //重复上传
                "DUPLICATED" -> {
                    ToastUtils.showToast(R.string.ota_device_is_duplicated)
                }
                //电量低
                "LOW_BATTERY" -> {
                    ToastUtils.showToast(R.string.ota_device_low_power_tips)
                }
                //繁忙
                "BUSY" -> {
                    ToastUtils.showToast(R.string.ota_device_busy_tips)
                }

                "LOW_STORAGE" -> {
                    ToastUtils.showToast(R.string.low_storage_tips)
                }
                //错误
                "DOWNGRADE" -> {
                    ToastUtils.showToast(R.string.healthy_sports_sync_fail)
                }
            }

        }

        override fun timeOut() {
            wrActivity?.get()?.apply {
                DialogUtils.dismissDialog(dialog)
                Log.e("下载", "ota timeOut")
                ToastUtils.showToast(R.string.ota_device_timeout_tips)
                ErrorUtils.onLogResult("OnlineDial getDeviceWatchFace timeOut")
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this.apply {
                        log += "\nOnlineDial getDeviceWatchFace timeOut\n请求文件状态超时/失败"
                    }, "1816", true)
                }
                strongDialog = null
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                postDialLog(4)
                LogUtils.e(TAG, "sendOnlineDial getDeviceWatchFace timeOut", true)
                BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                    FileUtils.deleteAll(it.path)
                }
            }
        }
    }

    private fun downloadCustomDial() {
        LogUtils.i(TAG, "sendCustomDial", true)
        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_DIAL)
        ErrorUtils.onLogResult("sendCustomDial start")
        val url = if (clockDialDataFormat == 0) {
            dialFileList[0].dialFileUrl
        } else {
            dialFileList[1].dialFileUrl
        }

        val downloadTrackingLog = TrackingLog.getSerTypeTrack("下载表盘资源", "下载", url)
        dialog?.show()
        DownloadManager.download(url, listener = object : DownloadListener {
            override fun onStart() {
                com.blankj.utilcode.util.LogUtils.d("start download $url")
                downloadTrackingLog.log += "start download"
                //上传表盘下载
                postDialLog(1)

                downloadDialog.progressView?.max = 0
                downloadDialog.progressView?.progress = 0
                downloadDialog.tvProgress?.text = "0%"
                downloadDialog.tvSize?.text = "0/0"
                downloadDialog.showDialog()
            }

            override fun onProgress(totalSize: Long, currentSize: Long) {
                com.blankj.utilcode.util.LogUtils.d("download onProgress totalSize: $totalSize,currentSize: $currentSize")
                //downloadTrackingLog.log += "\ndownload onProgress totalSize: $totalSize,currentSize: $currentSize"

                downloadDialog.progressView?.max = totalSize.toInt()
                downloadDialog.progressView?.progress = currentSize.toInt()
                downloadDialog.tvProgress?.text = "${((currentSize * 1f / totalSize) * 100).toInt()}%"
                downloadDialog.tvSize?.text = "${FileUtils.getSize(currentSize)}/${FileUtils.getSize(totalSize)}"
            }

            override fun onFailed(msg: String) {
                ToastUtils.showToast(getString(R.string.err_network_tips))
                ErrorUtils.onLogResult("sendCustomDial download onFailed: $msg")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_DOWNLOAD_FAIL)
                LogUtils.e(TAG, "sendCustomDial download onFailed: $msg", true)
                downloadDialog.cancel()
                downloadTrackingLog.log += "\nsendCustomDial download onFailed: $msg"
                downloadTrackingLog.endTime = TrackingLog.getNowString()
                downloadTrackingLog.serResult = "失败"
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, downloadTrackingLog.apply {
                    log += "\n下载表盘资源失败/超时"
                }, "1814", true)
            }

            override fun onSucceed(path: String) {
                com.blankj.utilcode.util.LogUtils.d("download onSucceed $path")
                downloadTrackingLog.log += "\ndownload onSucceed $path"
                downloadTrackingLog.endTime = TrackingLog.getNowString()
                downloadTrackingLog.serResult = "成功"
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, downloadTrackingLog)
                //sendOnlineDial(path)
                downloadDialog.cancel()
                sendCustomDial(path)
            }
        })
    }

    /**
     * 发送自定义表盘
     */
    private fun sendCustomDial(path: String) {


        if (newBgBitmap == null) {
            DialogUtils.dismissDialog(dialog)
            ToastUtils.showToast(R.string.ota_device_timeout_tips2)
            this@DialDetailsActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.let { FileUtils.deleteAll(it.path) }
            ErrorUtils.onLogResult("sendCustomDial newBgBitmap is null")
            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件").apply {
                log += "相册表盘newBgBitmap为空\n获取表盘文件失败"
            }, "1815", true)
            return
        }
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件"))
//                        if (newBgBitmap!!.width != newBgBitmap!!.height){
//                            ToastUtils.showToast(R.string.ota_device_timeout_tips3)
//                            this@DialDetailsActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
//                                ?.let { FileUtils.deleteAll(it.path) }
//                            return
//                        }
//                        downloadDialog.cancel()
//                        DialogUtils.dismissDialog(dialog)
//                    if (path != null) {
//                        downloadFilePath = path
//                    }
        LogUtils.i(TAG, "sendCustomDial download onSuccess")
        val uploadDialog = DownloadDialog(
            this@DialDetailsActivity,
            "",
            getString(R.string.theme_center_dial_up_load_title),
            newBgBitmap, //TODO
            newTextBitmap
        )
        val version = "111"
        val md5 = if (clockDialDataFormat == 0) {
            dialFileList[0].md5Value
        } else {
            dialFileList[1].md5Value
        }
        val byteTheme = CustomClockDialNewUtils.getMyNewCustomClockDialData(
            Global.dialDirection,
            path,
            color_r,
            color_g,
            color_b,
            newBgBitmap,
            newTextBitmap
        )
        if (byteTheme == null) {
            DialogUtils.dismissDialog(dialog)
            ToastUtils.showToast(R.string.ota_device_timeout_tips2)
            this@DialDetailsActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.let { FileUtils.deleteAll(it.path) }
            ErrorUtils.onLogResult("sendCustomDial byteTheme is null")
            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件失败").apply {
                log = "相册表盘byteTheme为空"
            }, "1815", true)
            return
        }

        //上传表盘传输
        postDialLog(2)

        fileStateTrackingLog.startTime = TrackingLog.getNowString()

        ControlBleTools.getInstance().getDeviceWatchFace(
            dialCode,
            byteTheme!!.size,
            true,
            MyCustomDialWatchFaceFileStatusListener(this@DialDetailsActivity, uploadDialog, byteTheme, TAG)
        )
    }

    /**
     * 自定义表盘状态接口回调
     */
    class MyCustomDialWatchFaceFileStatusListener(
        activity: DialDetailsActivity, uploadDialog: DownloadDialog,
        var byteTheme: ByteArray, var TAG: String,
    ) : DeviceWatchFaceFileStatusListener {
        private var wrActivity: WeakReference<DialDetailsActivity>? = null
        private var wrUploadDialog: WeakReference<DownloadDialog>? = null

        //BUG:表盘传输中来电，APP会卡在表盘传输中，无法退出
        //来电后弱引用wrUploadDialog被GC清空，增加强引用（一旦指向的对象没有更多的强/软引用并且 GC具有回收的内存，WeakReference将返回null）
        private var strongDialog: DownloadDialog? = null

        init {
            wrActivity = WeakReference(activity)
            strongDialog = uploadDialog
            wrUploadDialog = WeakReference(uploadDialog)
            if (wrActivity?.get() == null) {
                LogUtils.e("DialDetailsActivity", "DialDetailsActivity is Null")
            }
        }

        override fun onSuccess(statusValue: Int, statusName: String?) {
            LogUtils.e(TAG, "sendCustomDial getDeviceWatchFace onSuccess:$statusName", true)
            DialogUtils.dismissDialog(wrActivity?.get()?.dialog)

            if (statusName != "READY") {
                wrActivity?.get()?.postDelay(100) {
                    wrActivity?.get()?.postDialLog(4)
                    DialogUtils.dismissDialog(wrActivity?.get()?.dialog)
                }
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    if (statusName != "LOW_BATTERY") {
                        log += "\nstatus == $statusName != READY\n请求文件状态超时/失败"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this, "1816", true)
                    }
                }
            } else {
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    endTime = TrackingLog.getNowString()
                    devResult = "statusValue:$statusValue,statusName : $statusName"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this)
                }
            }

            when (statusName) {
                "READY" -> {
                    wrUploadDialog?.get()?.showDialog()
                    wrUploadDialog?.get()?.tvSize?.text =
                        wrActivity?.get()?.getString(R.string.theme_center_dial_up_load_tips)

                    val uploadTrackingLog = TrackingLog.getDevTyepTrack("传输表盘文件", "上传大文件数据", "sendThemeByProto4").apply {
                        log = "type:${BleCommonAttributes.UPLOAD_BIG_DATA_WATCH},fileByte:${byteTheme.size},isResumable:true"
                    }
                    wrActivity?.get()?.isSending = true
                    ControlBleTools.getInstance().startUploadBigData(
                        BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                        byteTheme, true,
                        object : UploadBigDataListener {
                            override fun onSuccess() {
                                wrActivity?.get()?.isSending = false
                                wrUploadDialog?.get()?.cancel()
                                strongDialog = null
                                LogUtils.i(TAG, "sendCustomDial startUploadBigData onSuccess", true)
                                ToastUtils.showToast(R.string.sync_success_tips)

                                uploadTrackingLog.log += "\nsendCustomDial startUploadBigData onSuccess"
                                uploadTrackingLog.endTime = TrackingLog.getNowString()
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog)
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getEndTypeTrack("表盘"), isEnd = true)

                                AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("8", "18").apply {
                                    functionStatus = "1"
                                })

                                BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { FileUtils.deleteAll(it.path) }
                                if (ThemeManager.getInstance().packetLossTimes > 0) {
                                    wrActivity?.get()?.postDelay(100) {
                                        ErrorUtils.onLogError(ErrorUtils.OTA_LOSS)
                                    }
                                }
                                wrActivity?.get()?.postDialLog(3)
                                RefreshMyDialListState.postValue(true)
                                ErrorUtils.clearErrorBigData()

                                //根据后台是否配置启用表盘安装结果，继续弹窗等待安装
                                if (wrActivity?.get()?.deviceSettingBean?.settingsRelated?.dial_installation_completed == true) {
                                    wrActivity?.get()?.dealDialInstall()
                                }
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
                                if (wrActivity?.get()?.isSending == true) {
                                    LogUtils.e(TAG, "sendCustomDial startUploadBigData onTimeout", true)
                                    ErrorUtils.onLogResult("CustomDial startUploadBigData onTimeout")
                                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                                    uploadTrackingLog.log += "\nsendOnlineDial startUploadBigData onTimeout"
                                    uploadTrackingLog.endTime = TrackingLog.getNowString()
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, uploadTrackingLog.apply {
                                        log += "\n发送响应超时/失败"
                                    }, "1818", true)
                                }
                                strongDialog = null
                                wrActivity?.get()?.isSending = false
                                wrActivity?.get()?.postDialLog(4)
                                if (!(wrActivity?.get() != null && wrActivity!!.get()!!.isDestroyed)) {
                                    wrUploadDialog?.get()?.cancel()
                                    ToastUtils.showToast(R.string.ota_device_timeout_tips)
//                                                            FileUtils.deleteAll(path)
                                    BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let {
                                        FileUtils.deleteAll(it.path)
                                    }
                                }
                            }
                        })
                }
                //重复上传
                "DUPLICATED" -> {
                    ToastUtils.showToast(R.string.ota_device_is_duplicated)
                }
                //电量低
                "LOW_BATTERY" -> {
                    ToastUtils.showToast(R.string.ota_device_low_power_tips)
                }
                //繁忙
                "BUSY" -> {
                    ToastUtils.showToast(R.string.ota_device_busy_tips)
                }

                "LOW_STORAGE" -> {
                    ToastUtils.showToast(R.string.low_storage_tips)
                }
                //错误
                "DOWNGRADE" -> {
                    ToastUtils.showToast(R.string.healthy_sports_sync_fail)
                }
            }

        }

        override fun timeOut() {
            wrActivity?.get()?.apply {
                LogUtils.e(TAG, "sendCustomDial getDeviceWatchFace onTimeout", true)
                ErrorUtils.onLogResult("CustomDial getDeviceWatchFace onTimeout")
                wrActivity?.get()?.fileStateTrackingLog?.apply {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, this.apply {
                        log += "\n\"CustomDial getDeviceWatchFace onTimeout\n请求文件状态超时/失败"
                    }, "1816", true)
                }
                strongDialog = null
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                postDialLog(4)
                DialogUtils.dismissDialog(dialog)
                ToastUtils.showToast(R.string.ota_device_timeout_tips)
                BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { FileUtils.deleteAll(it.path) }
            }
        }
    }

    //region 思澈平台表盘处理
    /**
     * 获取表盘发送状态
     */
    private fun getSifliSendState(path: String) {
        val fileByte: ByteArray? = FileUtils.getBytes(path)
        if (fileByte != null) {
            postDialLog(2)
            LogUtils.i(TAG, "getSlfiSendState getDeviceWatchFace downFile dialCode = $dialCode")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件"))

            fileStateTrackingLog.startTime = TrackingLog.getNowString()

            ControlBleTools.getInstance().getDeviceWatchFace(
                dialCode,
                fileByte.size,
                false,
                object : DeviceWatchFaceFileStatusListener {
                    override fun onSuccess(statusValue: Int, statusName: String?) {
                        when (statusName) {
                            "READY" -> {
                                sendSlfliOnlineDial(path)
                            }
                            //重复上传
                            "DUPLICATED" -> {
                                ToastUtils.showToast(R.string.ota_device_is_duplicated)
                            }
                            //电量低
                            "LOW_BATTERY" -> {
                                ToastUtils.showToast(R.string.ota_device_low_power_tips)
                            }
                            //繁忙
                            "BUSY" -> {
                                ToastUtils.showToast(R.string.ota_device_busy_tips)
                            }
                            //错误
                            "DOWNGRADE", "LOW_STORAGE" -> {
                                ToastUtils.showToast(R.string.healthy_sports_sync_fail)
                            }
                        }
                        if (statusName != "READY") {
                            DialogUtils.dismissDialog(dialog)
                        }
                    }

                    override fun timeOut() {
                        LogUtils.e(TAG, "sendCustomDial getDeviceWatchFace onTimeout", true)
                        ErrorUtils.onLogResult("CustomDial getDeviceWatchFace onTimeout")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_TRANSMISSION_TIMEOUT)
                        postDialLog(4)
                        DialogUtils.dismissDialog(dialog)
                        ToastUtils.showToast(R.string.ota_device_timeout_tips)
                        BaseApplication.application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { FileUtils.deleteAll(it.path) }
                    }

                }
            )
        } else {
            ErrorUtils.onLogResult("sendOnlineDial file is null")
            ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_OTHER)
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_DIAL, TrackingLog.getAppTypeTrack("获取表盘文件").apply {
                log += "在线文件为空\n获取表盘文件失败"
            }, "1815", true)
        }
    }

    private fun sendSlfliOnlineDial(path: String) {
        ThreadUtils.executeByIo(object : ThreadUtils.Task<String>() {
            override fun doInBackground(): String {
                val faceFile = com.blankj.utilcode.util.FileUtils.getFileByPath(path)
                val newFaceFilePath = faceFile.parent + File.separator + com.blankj.utilcode.util.FileUtils.getFileNameNoExtension(faceFile) + ".zip"
                //重命名
                com.blankj.utilcode.util.FileUtils.rename(faceFile, com.blankj.utilcode.util.FileUtils.getFileNameNoExtension(faceFile) + ".zip")
                val zipFile = com.blankj.utilcode.util.FileUtils.getFileByPath(newFaceFilePath)
                val unZipDirPath = PathUtils.getExternalAppFilesPath() + "/otal/face/" + com.blankj.utilcode.util.FileUtils.getFileNameNoExtension(zipFile)
                //解压
                com.blankj.utilcode.util.FileUtils.createOrExistsDir(unZipDirPath)
                ZipUtils.unzipFile(zipFile, com.blankj.utilcode.util.FileUtils.getFileByPath(unZipDirPath))
                //去掉一层文件夹再压缩
                val faceZipPath = "${PathUtils.getExternalAppFilesPath()}/otal/face/dynamic_app.zip"
                for (file in com.blankj.utilcode.util.FileUtils.listFilesInDir(unZipDirPath)) {
                    ZipUtils.zipFile(file.absolutePath, faceZipPath)
                }
                if (com.blankj.utilcode.util.FileUtils.isFile(faceZipPath)) {
                    return faceZipPath
                }
                return ""
            }

            override fun onCancel() {

            }

            override fun onFail(t: Throwable?) {
                t?.printStackTrace()
                LogUtils.e(TAG, "思澈表盘详情下载异常：$t")
                ToastUtils.showToast(R.string.ota_device_timeout_tips2)
                DialogUtils.dismissDialog(dialog)
            }

            override fun onSuccess(result: String?) {
                if (result.isNullOrEmpty()) {
                    ToastUtils.showToast(R.string.ota_device_timeout_tips2)
                    DialogUtils.dismissDialog(dialog)
                    return
                }
                startOrRefSifliTimeOut()
                SifliWatchfaceService.startActionWatchface(this@DialDetailsActivity, result, SpUtils.getValue(SpUtils.DEVICE_MAC, ""), 0)
            }
        })
    }

    private inner class SifliDFUTask : ThreadUtils.SimpleTask<Int>() {
        var i = 0
        var isOk = false

        fun finish(isOk: Boolean) {
            i = 30
            this.isOk = isOk
        }

        override fun doInBackground(): Int {
            while (i <= 30) {
                i++
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            //超时 或者 完成（成功失败）
            DialogUtils.dismissDialog(dialog)
            sifliFaceUploadDialog?.isShowing()?.let {
                if (it) sifliFaceUploadDialog?.cancel()
            }
            //清除已使用的文件
            com.blankj.utilcode.util.FileUtils.deleteAllInDir(PathUtils.getExternalAppFilesPath() + "/otal/face/")
            if (!isOk) {
                ToastUtils.showToast(R.string.healthy_sports_sync_fail)
            } else {
                ToastUtils.showToast(R.string.sync_success_tips)
                RefreshMyDialListState.postValue(true)
            }
        }
    }

    private var sifliDFUTask: SifliDFUTask? = null
    private fun startOrRefSifliTimeOut() {
        if (sifliDFUTask != null) {
            ThreadUtils.cancel(sifliDFUTask)
        }
        sifliDFUTask = SifliDFUTask()
        ThreadUtils.executeByIo(sifliDFUTask)
    }

    private var sifliFaceUploadDialog: DownloadDialog? = null
    private fun sifliFaceProgress(progress: Int?) {
        DialogUtils.dismissDialog(dialog)
        startOrRefSifliTimeOut()
        progress?.let {
            if (it == 0) {
                com.blankj.utilcode.util.LogUtils.d("effectImgUrl:" + groupDialList[clickCount].effectImgUrl)
                sifliFaceUploadDialog = DownloadDialog(
                    this@DialDetailsActivity,
                    getString(R.string.theme_center_dial_up_load_title),
                    groupDialList[clickCount].effectImgUrl
                )
                sifliFaceUploadDialog?.showDialog()
                sifliFaceUploadDialog?.tvSize?.text = getString(R.string.theme_center_dial_up_load_tips)
            } else {
                sifliFaceUploadDialog?.progressView?.max = 100
                sifliFaceUploadDialog?.progressView?.progress = it
                sifliFaceUploadDialog?.tvProgress?.text = "$it%"
            }
        }
    }

    private fun sifliFaceState(faceState: SifliReceiver.SifliFaceState) {
        sifliDFUTask?.finish(faceState.state == 0)
    }

    //endregion

    //region 表盘安装结果
    private fun dealDialInstall() {
        //继续显示等待框
        dialog?.show()
        startOrRefDialInstallTimeOut()
    }


    private inner class DialInstallTask : ThreadUtils.SimpleTask<Int>() {
        var i = 0
        var isOk = false

        fun finish(isOk: Boolean) {
            i = 30
            this.isOk = isOk
        }

        override fun doInBackground(): Int {
            while (i <= 30) {
                i++
                Thread.sleep(1000)
            }
            return 0
        }

        override fun onSuccess(result: Int?) {
            DialogUtils.dismissDialog(dialog)
            ToastUtils.showToast(getString(R.string.watch_face_fai))
        }
    }

    private var dialInstallTask: DialInstallTask? = null
    private fun startOrRefDialInstallTimeOut() {
        if (dialInstallTask != null) {
            ThreadUtils.cancel(dialInstallTask)
        }
        dialInstallTask = DialInstallTask()
        ThreadUtils.executeByIo(dialInstallTask)
    }


    //endregion

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
            PermissionUtils.checkRequestPermissions(this.lifecycle, getString(R.string.permission_camera), PermissionUtils.PERMISSION_GROUP_CAMERA) {
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
            ToastUtils.showToast(R.string.img_fail_tip)
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
        option.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.selector_public_button_on))
        //圆形
        option.setCircleDimmedLayer(TextUtils.equals(deviceShape, "2"))
        //启动裁剪 //裁剪后再缩放
        UCrop.of(dialUri!!, cropUri!!)
            .withOptions(option)
            .withAspectRatio(deviceWith * 1f, deviceHeith * 1f)
            .withMaxResultSize(3000, 3000)
            .start(this@DialDetailsActivity)
    }

    /**
     * 裁剪完成
     */
    private fun cropDone() {
        if (dialUri == null) {
            ToastUtils.showToast(R.string.img_fail_tip)
            return
        }
        //压缩
        Luban.with(this)
            .load(UriUtils.uri2File(dialUri))
            .ignoreBy(100) //压缩上限 100KB
            .filter { path -> !(TextUtils.isEmpty(path) || path.toLowerCase(Locale.ENGLISH).endsWith(".gif")) }
            .setTargetDir(Global.LUBAN_CACHE_DIR)
            .setCompressListener(object : OnCompressListener {
                override fun onStart() {
                }

                override fun onSuccess(file: File?) {
                    if (file == null) {
                        ToastUtils.showToast(R.string.img_fail_tip)
                        return
                    }
                    AppUtils.tryBlock(getString(R.string.img_fail_tip)) {
                        val bs = FileIOUtils.readFile2BytesByStream(file)
                        var bitmap = ConvertUtils.bytes2Bitmap(bs)
                        bitmap = BmpUtils.zoomImg(bitmap, deviceWith, deviceHeith)
                        photoBitmap = bitmap
                        //ImageUtils.save(bitmap,AppUtils.createImageFile(), Bitmap.CompressFormat.JPEG)
                        if (TextUtils.equals(deviceShape, "2")) {
                            newBgBitmap = BmpUtils.getCoverBitmap(this@DialDetailsActivity, bitmap)
                            binding.ivThemeMain.setImageBitmap(BmpUtils.getCoverBitmap2_1(this@DialDetailsActivity, bitmap))
                        } else {
                            newBgBitmap = bitmap
                            binding.ivThemeMain.setImageBitmap(bitmap)
                        }
                    }
                }

                override fun onError(e: Throwable?) {
                    e?.printStackTrace()
                    ToastUtils.showToast(R.string.img_fail_tip)
                }

            }).launch()


    }

    fun delAllCacheImg() {
        AppUtils.tryBlock {
            com.blankj.utilcode.util.FileUtils.deleteAllInDir(Global.LUBAN_CACHE_DIR)
        }
    }

    /**
     * 上传表盘日志
     * 1、表盘下载 2、表盘传输 3、表盘传输成功 4、传输失败
     */
    fun postDialLog(dataType: Int) {
        //上传表盘
        intent.getStringExtra("dialId")?.let {
            viewModel.postDialLog(it, if (clockDialDataFormat == 0) 1 else 2, dataType)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
        delAllCacheImg()
    }

}