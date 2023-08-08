package com.smartwear.xzfit.ui.debug

import android.app.Dialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.*
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.ActivityDebugBinding
import com.smartwear.xzfit.databinding.DialogDebugBinListBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.dialog.customdialog.CustomDialog
import com.smartwear.xzfit.dialog.customdialog.MyDialog
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.debug.manager.DebugAppBean
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.PermissionUtils
import com.smartwear.xzfit.utils.SendCmdUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * Created by Android on 2021/11/9.
 */
class DebugActivity : BaseActivity<ActivityDebugBinding, DeviceModel>(
    ActivityDebugBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    val loadDialog: Dialog by lazy { DialogUtils.showLoad(this) }

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("内部测试")
        EventBus.getDefault().register(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.tvChangeDevice.id -> {
                changeDevice()
            }
            binding.tvDebug.id -> {
                startActivity(Intent(this, DebugListActivity::class.java))
            }
            binding.tvFirewareUpgrade.id -> {
                startActivity(Intent(this, DebugFirewareUpgradeActivity::class.java))
            }
            binding.tvTheme.id -> {
                startActivity(Intent(this, DebugDialActivity::class.java))
            }
            binding.tvDevConnect.id -> {
                startActivity(Intent(this, DebugConnectActivity::class.java))
            }
            binding.tvClassicBle.id -> {
                startActivity(Intent(this, DebugClassicBleActivity::class.java))
            }
            binding.tvFeedbackDecoding.id -> {
                startActivity(Intent(this, DebugLogDecodingActivity::class.java))
            }
            binding.tvApps.id -> {
                PermissionUtils.checkRequestPermissions(
                    this.lifecycle, getString(R.string.permission_sdcard),
                    PermissionUtils.PERMISSION_GROUP_SDCARD
                ) {
                    val filePath = PathUtils.getAppDataPathExternalFirst() + "/log/appPackage.txt"
                    FileUtils.createFileByDeleteOldFile(filePath)
                    shareApps(filePath)
                }
            }
        }
    }

    private var isShowConnectStatus = false

    //region 切换设备
    /**
     * 切换设备
     */
    private fun changeDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*com.smartwear.xzfit.utils.PermissionUtils.PERMISSION_BLE12)) {
                com.smartwear.xzfit.utils.PermissionUtils.checkRequestPermissions(
                    null,
                    BaseApplication.mContext.getString(R.string.permission_bluetooth),
                    com.smartwear.xzfit.utils.PermissionUtils.PERMISSION_BLE12
                ) {
                    changeDevice()
                }
                return
            }
        }
        val dialogBinding = DialogDebugBinListBinding.inflate(layoutInflater)
        val dialog = CustomDialog.builder(this)
            .setContentView(dialogBinding.root)
            .setWidth(AppUtils.getScreenWidth())
            .setHeight((AppUtils.getScreenHeight() * 0.8f).toInt())
            .setGravity(Gravity.CENTER)
            .build()
        dialog.setOnShowDismissListener(object : MyDialog.OnShowDismissListener {
            override fun onShow() {
                ToastUtils.showShort("扫描设备中")
                ControlBleTools.getInstance().startScanDevice { device ->
                    device?.let {
                        LogUtils.d(GsonUtils.toJson(device))
                        if (!TextUtils.isEmpty(device.address) && !TextUtils.isEmpty(device.deviceType)) {
                            val view = Button(this@DebugActivity)
                            view.isAllCaps = false
                            view.text = "${device.deviceType} \n ${device.name} \n ${device.address}"
                            view.setTextColor(ContextCompat.getColor(this@DebugActivity, R.color.app_index_color))
                            view.setOnClickListener {
                                Global.IS_BIND_DEVICE = true
                                SendCmdUtils.connectDevice(if (!TextUtils.isEmpty(device.name)) device.name else "-", device.address)
                                isShowConnectStatus = true
                                dialog.dismiss()
                                loadDialog.show()
                            }
                            dialogBinding.listLayout.addView(view)
                        }

                    }
                }
            }

            override fun onDismiss() {
                ControlBleTools.getInstance().stopScanDevice()
            }

        })
        dialog.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                if (isShowConnectStatus) {
                    when (msg.arg) {
                        BleCommonAttributes.STATE_CONNECTED -> {
                            ToastUtils.showShort(getString(R.string.device_connected))
                            isShowConnectStatus = false
                            DialogUtils.dismissDialog(loadDialog)
                            ControlBleTools.getInstance().sendFindWear(null)
                        }
                        BleCommonAttributes.STATE_CONNECTING -> {
                            ToastUtils.showShort(getString(R.string.device_connecting))
                        }
                        BleCommonAttributes.STATE_DISCONNECTED -> {
                            ToastUtils.showShort(getString(R.string.device_disconnected))
                        }
                        BleCommonAttributes.STATE_TIME_OUT -> {
                        }
                    }
                }
            }
        }
    }
    //endregion

    //region 分享手机已安装应用
    /**
     * 分享手机已安装应用
     * */
    private fun shareApps(filePath: String) {
        ThreadUtils.executeByIo(object : ThreadUtils.Task<File>() {
            override fun doInBackground(): File? {
                var file: File? = null
                //已安装应用
                val appList: MutableList<DebugAppBean> = mutableListOf()
                val pm = BaseApplication.mContext.packageManager
                //获取所有已安装应用
                val resolveIntent = Intent(Intent.ACTION_MAIN, null)
                resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val rList: List<ResolveInfo> = pm.queryIntentActivities(resolveIntent, 0)
                for (r in rList) {
                    LogUtils.d("    " + r.activityInfo.packageName + "----" + r.loadLabel(pm).toString())
                    if (r.activityInfo != null && r.activityInfo.packageName != null) {
                        appList.add(
                            DebugAppBean(r.loadLabel(pm).toString(), r.activityInfo.packageName)
                        )
                    }
                }
                val appsText = GsonUtils.toJson(appList)
                if (!TextUtils.isEmpty(appsText)) {
                    FileIOUtils.writeFileFromString(filePath, appsText)
                    file = FileUtils.getFileByPath(filePath)
                }
                return file
            }

            override fun onSuccess(result: File?) {
                try {
                    result?.let {
                        var intent = Intent(Intent.ACTION_SEND)
                        intent.type = "application/octet-stream"
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(Intent.EXTRA_STREAM, UriUtils.file2Uri(it))
                        intent = Intent.createChooser(intent, "已安装应用包名分享")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    ToastUtils.showShort("分享失败")
                }
            }

            override fun onCancel() {}

            override fun onFail(t: Throwable?) {
                ToastUtils.showShort("分享失败")
            }

        })
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        Global.IS_BIND_DEVICE = false
        ControlBleTools.getInstance().disconnect()
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
    }
}