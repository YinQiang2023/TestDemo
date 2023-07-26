package com.jwei.xzfit.ui.debug

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.*
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.custom.CustomClockDialNewUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityDebugFirewareUpgradeBinding
import com.jwei.xzfit.databinding.DialogDebugBinListBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.dialog.customdialog.CustomDialog
import com.jwei.xzfit.dialog.customdialog.MyDialog
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.BmpUtils
import com.jwei.xzfit.utils.GlideApp
import com.jwei.xzfit.utils.manager.WakeLockManager
import com.jwei.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.custom.MyCustomClockUtils
import org.greenrobot.eventbus.EventBus
import java.io.File

/**
 * Created by Android on 2021/11/26.
 */
class DebugDialActivity : BaseActivity<ActivityDebugFirewareUpgradeBinding, DeviceModel>(
    ActivityDebugFirewareUpgradeBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private val mFilePath = PathUtils.getAppDataPathExternalFirst() + "/otal/dial"
    private var imgPath = ""
    private var textPath = ""
    private lateinit var file: File

    private val mCustomFilePath = PathUtils.getAppDataPathExternalFirst() + "/otal/customDial"

    //是否选中相册表盘
    private var mSelectedCustom = false

    private var oldTextBitmap: Bitmap? = null
    private var newTextBitmap: Bitmap? = null
    private var oldBgBitmap: Bitmap? = null
    private var newBgBitmap: Bitmap? = null
    private var mIsDialDirection = false
    private var mRond = false
    private var mColor_r = 255
    private var mColor_g = 255
    private var mColor_b = 255

    val loadDialog: Dialog by lazy { DialogUtils.showLoad(this) }

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.theme_center_dial_details_sync_btn)
        LogUtils.e(mFilePath)
        FileUtils.createOrExistsDir(mFilePath)
        binding.tvTip1.text =
            "操作方法\n\n1.将表盘文件夹(文件夹名不能有空格，在线必须有 img_effect.png 和 hor.bin 文件，相册必须有img_zdy_bg.png 和 img_zdy_text.png)\n\n" +
                    "2.在线表盘放到[内部存储\\Android\\data\\com.jwei.publicone\\otal\\dial]目录下\n\n" +
                    "3.相册表盘放到[内部存储\\Android\\data\\com.jwei.publicone\\otal\\customDial]目录下\n\n" +
                    "4.点击[选择文件]按钮，选择文件\n\n" +
                    "5.点击[更新表盘]开始更新表盘"
        binding.btnDone.text = "更新表盘"
        WakeLockManager.instance.keepUnLock(this.lifecycle)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnFile.id -> {
                if (!checkPermissions()) return
                val files = FileUtils.listFilesInDir(mFilePath)
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mFilePath 目录文件为空")
                    return
                }
                showListDialog(files, false)
            }
            binding.btnFile.id -> {
                if (!checkPermissions()) return
                val files = FileUtils.listFilesInDir(mFilePath)
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mFilePath 目录文件为空")
                    return
                }
                showListDialog(files, true)
            }
            binding.btnFile2.id -> {
                if (!checkPermissions()) return
                val files = FileUtils.listFilesInDir(mCustomFilePath)
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mFilePath 目录文件为空")
                    return
                }
                showListDialog(files, true)
            }
            binding.btnDone.id -> {
                if (!::file.isInitialized) {
                    ToastUtils.showShort("未选择表盘文件")
                    return
                }
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showShort(getString(R.string.device_no_connection))
                    return
                }
                try {
                    mIsDialDirection = binding.cbDirection.isChecked
                    mRond = binding.cbRond.isChecked
                    mColor_r = binding.etColorR.text.toString().trim().toInt()
                    mColor_g = binding.etColorG.text.toString().trim().toInt()
                    mColor_b = binding.etColorB.text.toString().trim().toInt()
                } catch (e: Exception) {
                    ToastUtils.showShort("相册表盘参数异常，请重新输入")
                }
                if (mSelectedCustom) {
                    dealCustomImgDial()
                } else {
                    upgrade()
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (!PermissionUtils.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionUtils.permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .callback(object : PermissionUtils.FullCallback {
                    override fun onGranted(
                        granted: MutableList<String>
                    ) = LogUtils.d("申请权限同意$granted")

                    override fun onDenied(deniedForever: MutableList<String>, denied: MutableList<String>) {
                        LogUtils.d("申请权限永远拒绝 = $deniedForever,\n拒绝 = $denied")
                    }
                })
                .request()
            return false
        }
        return true
    }

    private fun dealCustomImgDial() {
        loadDialog.show()
        try {
            oldBgBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(imgPath))
            newBgBitmap = if (mRond) {
                BmpUtils.getCoverBitmap(this@DebugDialActivity, oldBgBitmap)
            } else {
                oldBgBitmap
            }
            dealCustomTextDial()
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort("表盘文件异常")
            if (loadDialog.isShowing) DialogUtils.dismissDialog(loadDialog)
        }
    }

    private fun dealCustomTextDial() {
        try {
            oldTextBitmap = ImageUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(textPath))
            ThreadUtils.executeByIo(object : ThreadUtils.Task<Bitmap?>() {
                override fun doInBackground(): Bitmap? {
                    if (oldTextBitmap == null) {
                        return null
                    }
                    return MyCustomClockUtils.getNewTextBitmap(oldTextBitmap, mColor_r, mColor_g, mColor_b)
                }

                override fun onSuccess(result: Bitmap?) {
                    if (result == null) {
                        ToastUtils.showShort("表盘文件异常")
                        if (loadDialog.isShowing) DialogUtils.dismissDialog(loadDialog)
                        return
                    }
                    newTextBitmap = result
                    upgrade()
                }

                override fun onCancel() {
                }

                override fun onFail(t: Throwable?) {
                    t?.printStackTrace()
                    ToastUtils.showShort("表盘文件异常")
                    if (loadDialog.isShowing) DialogUtils.dismissDialog(loadDialog)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort("表盘文件异常")
            if (loadDialog.isShowing) DialogUtils.dismissDialog(loadDialog)
        }
    }

    private lateinit var dialogBinding: DialogDebugBinListBinding
    private var dialog: MyDialog? = null

    @SuppressLint("CheckResult")
    private fun showListDialog(files: List<File>, isCustom: Boolean) {
        dialogBinding = DialogDebugBinListBinding.inflate(layoutInflater)
        dialog = CustomDialog.builder(this)
            .setContentView(dialogBinding.root)
            .setWidth(AppUtils.getScreenWidth())
            .setHeight((AppUtils.getScreenHeight() * 0.8f).toInt())
            .setGravity(Gravity.CENTER)
            .build()
        for (file in files) {
            val view = Button(this)
            view.isAllCaps = false
            view.text = file.name
            view.setTextColor(ContextCompat.getColor(this, R.color.app_index_color))
            if (FileUtils.isDir(file)) {
                for (f in FileUtils.listFilesInDir(file)) {
                    if (TextUtils.equals(f.name, "img_effect.png")) {
                        imgPath = f.path
                        GlideApp.with(this).load(f).into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                view.setCompoundDrawables(resource, null, null, null)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                LogUtils.d("获取表盘图片失败")
                            }

                        })
                    }
                }
            }

            view.setOnClickListener {
                mSelectedCustom = isCustom
                var isLegal = false
                if (FileUtils.isDir(file)) {
                    var isImg = false
                    var isBin = false
                    var isText = false
                    val list = FileUtils.listFilesInDir(file);
                    for (l in list) {
                        LogUtils.d("l:" + l.toString())
                    }
                    for (f in FileUtils.listFilesInDir(file)) {
                        if (mSelectedCustom) {
                            if (TextUtils.equals(f.name, "img_zdy_bg.png")) {
                                isImg = true
                                this.imgPath = f.path
                            }
                        } else {
                            if (TextUtils.equals(f.name, "img_effect.png")) {
                                isImg = true
                                this.imgPath = f.path
                            }
                            this.textPath = ""
                        }
                        if (TextUtils.equals(f.name, "hor.bin")) {
                            isBin = true
                            this.file = f
                        }
                        if (TextUtils.equals(f.name, "img_zdy_text.png")) {
                            isText = true
                            this.textPath = f.path
                        }

                    }
                    isLegal = isImg && isBin && (!mSelectedCustom || isText)
                }
                if (!isLegal) {
                    showTips("非法表盘文件,请仔细检查后按说明放置相关文件！！！")
                    return@setOnClickListener
                }
                binding.tvName.text = "文件名： ${file.name}"
                binding.ivImg.visibility = if (!TextUtils.isEmpty(imgPath)) View.VISIBLE else View.GONE
                if (!TextUtils.isEmpty(imgPath))
                    GlideApp.with(this).load(FileUtils.getFileByPath(imgPath)).into(binding.ivImg)
                binding.ivTextImg.visibility = if (!TextUtils.isEmpty(textPath)) View.VISIBLE else View.GONE
                if (!TextUtils.isEmpty(textPath))
                    GlideApp.with(this).load(FileUtils.getFileByPath(textPath)).into(binding.ivTextImg)

                dialog?.dismiss()
            }
            dialogBinding.listLayout.addView(view)
        }
        dialog?.show()
    }

    /**
     * 上传表盘
     */
    private fun upgrade() {
        loadDialog.show()
        ControlBleTools.getInstance().getDeviceLargeFileState(true, "1", "1", object : DeviceLargeFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String?) {
                when (statusName) {
                    "READY" -> {
                        LogUtils.e("Dial value = 0")
                        try {
                            val fileByte: ByteArray = if (mSelectedCustom) {
                                CustomClockDialNewUtils.getMyNewCustomClockDialData(
                                    mIsDialDirection,
                                    file.path,
                                    mColor_r,
                                    mColor_g,
                                    mColor_b,
                                    newBgBitmap,
                                    newTextBitmap
                                )
                            } else {
                                FileIOUtils.readFile2BytesByStream(file)
                            }
                            ControlBleTools.getInstance().startUploadBigData(
                                BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                                fileByte, true,
                                object : UploadBigDataListener {
                                    override fun onSuccess() {
                                        showTips("Dial onSuccess")
                                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
                                        DialogUtils.dismissDialog(loadDialog)
                                    }

                                    override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                                        val percentage = curPiece * 100 / dataPackTotalPieceLength
                                        LogUtils.e("onProgress $percentage")
                                        //showTips("onProgress ---->  $percentage")
                                    }

                                    override fun onTimeout() {
                                        showTips("Dial onTimeout")
                                        DialogUtils.dismissDialog(loadDialog)
                                    }
                                })
                        } catch (e: Exception) {
                            showTips("非法表盘文件")
                            DialogUtils.dismissDialog(loadDialog)
                        }

                    }
                    "BUSY" -> {
                        showTips(getString(R.string.ota_device_busy_tips))
                        DialogUtils.dismissDialog(loadDialog)
                    }
                    "DOWNGRADE", "DUPLICATED", "LOW_STORAGE" -> {
                        showTips(getString(R.string.ota_device_request_failed_tips))
                        DialogUtils.dismissDialog(loadDialog)
                    }
                    "LOW_BATTERY" -> {
                        showTips(getString(R.string.ota_device_low_power_tips))
                        DialogUtils.dismissDialog(loadDialog)
                    }
                }
            }

            override fun timeOut() {
                showTips("Dial timeOut")
                DialogUtils.dismissDialog(loadDialog)
            }
        })
    }

    private fun showTips(s: String) {
        ThreadUtils.runOnUiThread {
            LogUtils.e(s)
            ToastUtils.showShort(s)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}