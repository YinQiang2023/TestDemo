package com.smartwear.xzfit.ui.debug

import android.Manifest
import android.view.View
import android.widget.Button
import com.blankj.utilcode.util.*
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.UploadBigDataListener
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityDebugFirewareUpgradeBinding
import com.smartwear.xzfit.databinding.DialogDebugBinListBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.dialog.customdialog.CustomDialog
import com.smartwear.xzfit.dialog.customdialog.MyDialog
import com.smartwear.xzfit.ui.device.DevMoreSetActivity
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.ManageActivity
import com.smartwear.xzfit.utils.SendCmdUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.manager.WakeLockManager
import com.smartwear.xzfit.viewmodel.DeviceModel
import java.io.File

/**
 * Created by Android on 2021/11/9.
 */
class DebugFirewareUpgradeActivity : BaseActivity<ActivityDebugFirewareUpgradeBinding, DeviceModel>(
    ActivityDebugFirewareUpgradeBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private val mFilePath = PathUtils.getAppDataPathExternalFirst() + "/otal/fireware"

    private lateinit var file: File

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.dev_more_set_update)
        LogUtils.e(mFilePath)
        FileUtils.createOrExistsDir(mFilePath)

        binding.tvTip1.text = "操作方法\n\n1.将[OTA文件(.bin)]，放到[内部存储\\Android\\data\\com.smartwear.xzfit\\otal\\fireware]目录下\n\n\n2.点击[选择文件]按钮，选择文件\n\n\n3.点击[升级]开始升级"
        binding.btnFile.text = "选择固件文件"
        WakeLockManager.instance.keepUnLock(this.lifecycle)

        binding.btnFile2.visibility = View.GONE
        binding.cbDirection.visibility = View.GONE
        binding.cbRond.visibility = View.GONE
        binding.etColorR.visibility = View.GONE
        binding.etColorG.visibility = View.GONE
        binding.etColorB.visibility = View.GONE
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
                showListDialog(files)

            }
            binding.btnDone.id -> {
                if (!::file.isInitialized) {
                    ToastUtils.showShort("未选择升级文件")
                    return
                }
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showShort(getString(R.string.device_no_connection))
                    return
                }
                upgrade()
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

    private lateinit var dialogBinding: DialogDebugBinListBinding
    private var dialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        if (!::dialogBinding.isInitialized) {
            dialogBinding = DialogDebugBinListBinding.inflate(layoutInflater)
            dialog = CustomDialog.builder(this)
                .setContentView(dialogBinding.root)
                .setWidth(AppUtils.getScreenWidth())
                .build()

            for (file in files) {
                if (file.name.endsWith(".bin")) {
                    val view = Button(this)
                    view.isAllCaps = false
                    view.text = file.name
                    view.setOnClickListener {
                        this.file = file
                        binding.tvName.text = "文件名： ${file.name}"
                        dialog?.dismiss()
                    }
                    dialogBinding.listLayout.addView(view)
                }

            }
        }
        dialog?.show()
    }

    private fun upgrade() {
        val loaddialog = DialogUtils.showLoad(this)
        loaddialog.show()
        ControlBleTools.getInstance().getDeviceLargeFileState(true, "1", "1", object : DeviceLargeFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String?) {
                when (statusName) {
                    "READY" -> {
                        LogUtils.e("ota value = 0")
                        val fileByte: ByteArray = FileIOUtils.readFile2BytesByStream(file)
                        ControlBleTools.getInstance().startUploadBigData(
                            BleCommonAttributes.UPLOAD_BIG_DATA_OTA,
                            fileByte, true,
                            object : UploadBigDataListener {
                                override fun onSuccess() {
                                    showTips("ota onSuccess")
                                    loaddialog.dismiss()

                                    ManageActivity.removeActivity(DebugFirewareUpgradeActivity::class.java)
                                    ManageActivity.removeActivity(DevMoreSetActivity::class.java)
                                    //TODO 可以去除
                                    ControlBleTools.getInstance().disconnect()
                                    //3s后重连
                                    ThreadUtils.runOnUiThreadDelayed({
                                        SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                                    }, 5000)
                                }

                                override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                                    val percentage = curPiece * 100 / dataPackTotalPieceLength
                                    LogUtils.e("onProgress $percentage")
                                    //showTips("onProgress ---->  $percentage")
                                }

                                override fun onTimeout() {

                                    showTips("ota onTimeout")
                                    loaddialog.dismiss()
                                }
                            })
                    }
                    "BUSY" -> {
                        showTips("ota 设备忙碌中")
                        loaddialog.dismiss()
                    }
                    "DOWNGRADE", "DUPLICATED", "LOW_STORAGE" -> {
                        showTips("ota 请求失败")
                        loaddialog.dismiss()
                    }
                    "LOW_BATTERY" -> {
                        showTips("设备电量低")
                        loaddialog.dismiss()
                    }
                }
            }

            override fun timeOut() {
                showTips("ota timeOut")
                loaddialog.dismiss()
            }
        })
    }

    private fun showTips(s: String) {
        ThreadUtils.runOnUiThread {
            LogUtils.e(s)
            ToastUtils.showShort(s)
        }
    }

    override fun onPause() {
        super.onPause()
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
    }
}