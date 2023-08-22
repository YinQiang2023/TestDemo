package com.smartwear.publicwatch.ui.device.devicecontrol

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ThreadUtils
import com.zhapp.ble.BleBCManager
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityUnBindDeviceBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.UnbindDeviceCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.ui.GlobalEventManager
import com.smartwear.publicwatch.ui.device.DeviceManageActivity
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.user.QAActivity
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference

class UnBindDeviceActivity : BaseActivity<ActivityUnBindDeviceBinding, DeviceModel>(
    ActivityUnBindDeviceBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    val TAG = UnBindDeviceActivity::class.java.simpleName
    private var type = ""
    private var mac = ""
    private var dialog: Dialog? = null
    private var clazz: Class<Activity>? = null
    private var isUnbindForDevice = false
    private var isUnbindForService = false
    private var isUnbind = false
    private var isEnableDevice = false
    var mTimerHandler = Handler(Looper.getMainLooper())

    private var loadingDialog: Dialog? = null

    //后台解绑设备
    private val serUnbindDeviceTrackingLog by lazy { TrackingLog.getSerTypeTrack("后台解绑", "设备解绑", "infowear/device/unBind") }

    //设备解绑
    private val devUnBindTrackingLog by lazy { TrackingLog.getDevTyepTrack("解绑设备", "解绑设备", "UNBIND_REQUEST") }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnCancel.id -> {
                finish()
            }
            binding.btnRemoveBind.id -> {
                askUnBindDevice()
            }
            binding.title.tvTitle.id,
            binding.btnConfirm.id,
            -> {
                onBackPress()
            }
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
            }
        }
    }

    private fun askUnBindDevice() {
        dialog?.show()
        NetworkUtils.isAvailableAsync { isAvailable ->
            DialogUtils.dismissDialog(dialog)
            if (!isAvailable) {
                ToastUtils.showToast(R.string.not_network_tips)
                return@isAvailableAsync
            }
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getStartTypeTrack("解绑"), isStart = true)
            if (isEnableDevice) {
                if (ControlBleTools.getInstance().isConnect) {
                    dialog = DialogUtils.showDialogTitle(this@UnBindDeviceActivity,
                        "",
                        getString(R.string.dialog_confirm_unbind_device_tips),
                        getString(R.string.dialog_cancel_btn),
                        getString(R.string.dialog_unbind_btn),
                        object : DialogUtils.DialogClickListener {
                            override fun OnOK() {
                                if (NetworkUtils.isConnected()) {
                                    isUnbind = true
                                    startUnbindDevice()
                                } else {
                                    ToastUtils.showToast(R.string.not_network_tips)
                                }
                            }

                            override fun OnCancel() {
                            }
                        })
                } else {
                    dialog = DialogUtils.showDialogTitle(this@UnBindDeviceActivity,
                        "",
                        getString(R.string.dialog_confirm_unbind_device_tips2),
                        getString(R.string.dialog_cancel_btn),
                        getString(R.string.dialog_unbind_btn2),
                        object : DialogUtils.DialogClickListener {
                            override fun OnOK() {
                                isUnbind = true
                                startUnbindDevice()
                            }

                            override fun OnCancel() {
                            }
                        })
                }
            } else {
                dialog = DialogUtils.showDialogTitle(this@UnBindDeviceActivity,
                    "",
                    getString(R.string.dialog_confirm_unbind_device_tips3),
                    getString(R.string.dialog_cancel_btn),
                    getString(R.string.dialog_unbind_btn2),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            isUnbind = true
                            startUnbindDevice()
                        }

                        override fun OnCancel() {
                        }
                    })
            }
            if (!isDestroyed && !isFinishing) {
                dialog?.show()
            }
        }
    }

    private fun startUnbindDevice() {
        if (isUnbind) {
            NetworkUtils.isAvailableAsync { isAvailable ->
                if (isAvailable) {
                    startUnbind()
                } else {
                    dismissDialog()
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
        type = if (intent.getStringExtra("type") != null) {
            intent.getStringExtra("type").toString()
        } else {
            ""
        }
        mac = if (intent.getStringExtra("mac") != null) {
            intent.getStringExtra("mac").toString()
        } else {
            ""
        }
        clazz = intent.getSerializableExtra("clazz") as Class<Activity>
        isEnableDevice = intent.getBooleanExtra("isEnable", false)

        setViewsClickListener(
            this, binding.btnCancel, binding.btnRemoveBind, binding.title.tvTitle,
            binding.btnConfirm, binding.tvHelp
        )

        observe()
        dialog = DialogUtils.showLoad(this)
    }

    private fun observe() {
        viewModel.unbindDeviceCode.observe(this, Observer {
            if (TextUtils.isEmpty(it)) return@Observer



            if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_BIND,
                    serUnbindDeviceTrackingLog.apply {
                        log += "\n后台解绑网络请求超时/失败"
                    }, "1226", true
                )
            } else {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, serUnbindDeviceTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                })
            }

            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("设备状态").apply {
                log = "设备是否启用：${mac == SpUtils.getValue(SpUtils.DEVICE_MAC, "")},设备是否连接：${ControlBleTools.getInstance().isConnect}"
            })

            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    LogUtils.i(TAG, "unbindDeviceCode REQUEST_SUCCESS")
                    if (ControlBleTools.getInstance().isConnect && mac == SpUtils.getValue(SpUtils.DEVICE_MAC, "")) {
                        LogUtils.i(TAG, "unbindDeviceCode REQUEST_SUCCESS 设备已连接，且解绑的是当前启用的设备")

                        devUnBindTrackingLog.startTime = TrackingLog.getNowString()
                        CallBackUtils.unbindDeviceCallBack = MyUnbindDeviceCallBack(this)
                        ControlBleTools.getInstance().unbindDevice(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                            override fun onState(state: SendCmdState) {
                                LogUtils.d(TAG,"unbindDevice state ->$state")
                                devUnBindTrackingLog.endTime = TrackingLog.getNowString()
                                devUnBindTrackingLog.devResult = "state = $state"
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, devUnBindTrackingLog)
                                if (state != SendCmdState.SUCCEED) {
                                    ThreadUtils.runOnUiThread {
                                        //指令失败/超时后-解绑成功 避免一直卡在loading
                                        dismissDialog()
                                        showUnbindSuccessView()
                                        ControlBleTools.getInstance().disconnect()//断开连接,清除SDK缓存
                                    }
                                }
                                //在线解绑行为
                                AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("2", "5"))
                            }
                        })
                    } else {
                        LogUtils.i(TAG, "unbindDeviceCode REQUEST_SUCCESS 设备未连接")
                        dismissDialog()
                        showUnbindSuccessView()
                        //离线解绑行为
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("2", "6"))
                    }
                }
                HttpCommonAttributes.REQUEST_FAIL -> {
                    dismissDialog()
                    ToastUtils.showToast(getString(R.string.operation_unbind_failed_tips))
                    binding.tvHelp.visibility = View.VISIBLE
                    isUnbindForService = false
                    isUnbindForDevice = false
                }
                HttpCommonAttributes.SERVER_ERROR -> {
                    binding.lyDefaultBottomView.visibility = View.VISIBLE
                    binding.tvTips.visibility = View.VISIBLE
                    dismissDialog()
                    ToastUtils.showToast(getString(R.string.operation_request_failed_tips))
                }

            }
        })


    }

    class MyUnbindDeviceCallBack(activity: UnBindDeviceActivity) : UnbindDeviceCallBack {
        private var wrActivity: WeakReference<UnBindDeviceActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }

        override fun unbindDeviceSuccess() {
            wrActivity?.get()?.apply {
                devUnBindTrackingLog.devResult = "unbindDeviceSuccess() callback"
                isUnbindForDevice = true
                dismissDialog()
                showUnbindSuccessView()
                ControlBleTools.getInstance().disconnect()//断开连接,清除SDK缓存
            }
        }

    }

    private fun showUnbindSuccessView() {
        binding.lyDefaultBottomView.visibility = View.GONE
        binding.lyFinishBottomView.visibility = View.VISIBLE
        binding.ivTopCenterState.visibility = View.VISIBLE
        binding.tvTips.visibility = View.GONE
        binding.tvHelp.visibility = View.GONE
        binding.title.tvTitle.isEnabled = true
        binding.btnConfirm.isEnabled = true
        binding.tvTopCenterTips.text = getString(R.string.un_bind_top_un_bundling_success)
        binding.ivTopCenterState.setImageResource(R.mipmap.un_bind_success)
        isUnbindForService = true
        viewModel.deviceSettingLiveData.resetData()
        //解绑后也弹后台保活提醒弹窗
        SpUtils.getSPUtilsInstance().put(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, false)
        SpUtils.getSPUtilsInstance().put(SpUtils.NOTIFY_USER_GUIDANCE_TIPS, false)
        GlobalEventManager.isCanShowFirmwareUpgrade = true
        GlobalEventManager.isCanUpdateAgps = true
        //解除通话蓝牙配对
        val headsetMac = SpUtils.getHeadsetMac(mac)
        if (!TextUtils.isEmpty(headsetMac)) {
            BleBCManager.getInstance().removeBond(headsetMac)
            SpUtils.saveHeadsetMac(mac, "")
            SpUtils.saveHeadsetName(mac, "")
        }

        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getEndTypeTrack("解绑"), isEnd = true)
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }

    private fun startUnbind() {
        viewModel.unbindDevice(type, mac, serUnbindDeviceTrackingLog)
        binding.title.tvTitle.isEnabled = false
        binding.ivTopCenterState.setImageResource(R.mipmap.bind_wait)
        dialog = DialogUtils.showLoad(this@UnBindDeviceActivity)
        dialog?.show()
    }

    override fun onBackPress() {
        if (isUnbindForService) {
            onFinish()
        } else {
            super.onBackPress()
        }
    }

    private fun onFinish() {
        if (isUnbindForDevice) {
            ControlBleTools.getInstance().disconnect()
            SpUtils.setValue(SpUtils.DEVICE_NAME, "")
            SpUtils.setValue(SpUtils.DEVICE_MAC, "")
            SpUtils.setValue(SpUtils.DEVICE_VERSION, "")
        }
        if (isUnbindForService) {
            ManageActivity.removeActivity(clazz!!)
            ManageActivity.removeActivity(DeviceManageActivity::class.java)
            //解绑了当前启用的设备
            if (isEnableDevice) {
                SpUtils.setValue(SpUtils.DEVICE_NAME, "")
                SpUtils.setValue(SpUtils.DEVICE_MAC, "")
                SpUtils.setValue(SpUtils.DEVICE_VERSION, "")
            }
            //服务器解绑后，如果sdk内部连接的设备是解绑的设备，清除sdk内部连接信息 //http://jira.wearheart.cn/browse/INA-2126
            if (TextUtils.equals(ControlBleTools.getInstance().getCurrentDeviceMac(), mac)) {
                ControlBleTools.getInstance().disconnect()
            }

            DeviceManager.dataList.lastOrNull { it.deviceMac == mac }?.let {
                LogUtils.d(TAG, "unbind finish remove device :" + it.deviceName)
                DeviceManager.dataList.remove(it)
            }
            if (DeviceManager.dataList.size == 0) {
                ControlBleTools.getInstance().disconnect()
            }

            EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
        }
        finish()
    }
}