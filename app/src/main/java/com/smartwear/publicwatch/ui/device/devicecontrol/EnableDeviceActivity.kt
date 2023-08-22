package com.smartwear.publicwatch.ui.device.devicecontrol

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.NetworkUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityEnableDeviceBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.https.response.BindListResponse
import com.smartwear.publicwatch.ui.GlobalEventManager
import com.smartwear.publicwatch.ui.device.DeviceManageActivity
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.user.QAActivity
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import org.greenrobot.eventbus.EventBus

class EnableDeviceActivity : BaseActivity<ActivityEnableDeviceBinding, DeviceModel>(
    ActivityEnableDeviceBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    val TAG = EnableDeviceActivity::class.java.simpleName
    private var dialog: Dialog? = null
    private var oldDevice = ""

    //private var id = ""
    private var newDevice: BindListResponse.DeviceItem? = null
    private var isEnable = IS_ENABLE_FAILURE
    private var newDeviceName = ""

    //处理启用超时
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    //启用设备
    private val enableDeviceTrackingLog by lazy { TrackingLog.getSerTypeTrack("启用设备", "设备启用", "infowear/device/enable") }

    companion object {
        val IS_ENABLE_SUCCESS: Int = 0x011
        val IS_ENABLE_FAILURE: Int = 0x012
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnEnable.id -> {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getStartTypeTrack("启用"), isStart = true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_BLE12)) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("启用设备异常").apply {
                            log = "android 12 以上无蓝牙权限"
                        }, "1225", true)
                        PermissionUtils.checkRequestPermissions(lifecycle, BaseApplication.mContext.getString(R.string.permission_bluetooth), PermissionUtils.PERMISSION_BLE12) {
                            binding.btnEnable.callOnClick()
                        }
                        return
                    }
                }
                if (AppUtils.isOpenBluetooth()) {
                    val contentText = getString(R.string.device_info_start_device_tips1)
                    dialog = DialogUtils.showDialogContentAndTwoBtn(this, contentText, getString(R.string.dialog_cancel_btn),
                        getString(R.string.device_info_dialog_ok_btn), object : DialogUtils.DialogClickListener {
                            override fun OnOK() {
                                startDevice()
                            }

                            override fun OnCancel() {
                            }
                        })
                    dialog?.show()
                } else {
                    dialog = DialogUtils.showDialogTitleAndOneButton(this, getString(R.string.dialog_title_bluetooth_not_enabled),
                        getString(R.string.dialog_context_open_bluetooth), getString(R.string.dialog_confirm_btn), object : DialogUtils.DialogClickListener {
                            override fun OnOK() {
                                AppUtils.enableBluetooth(this@EnableDeviceActivity, 0x01)
                            }

                            override fun OnCancel() {
                            }
                        })

                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("启用设备异常").apply {
                        log = "蓝牙未开启"
                    }, "1225", true)
                }
            }
            binding.btnCancel.id -> {
                setResult(isEnable)
                //ControlBleTools.getInstance().disconnect()
                this.finish()
            }
            binding.btnConfirm.id -> {
                startSuccess()
                ControlBleTools.getInstance().disconnect()
            }
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
            }
            binding.title.tvTitle.id -> {
                onBackEvent()
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()

        //隐藏标题栏
        binding.title.layoutTitle.visibility = View.INVISIBLE

        //id = intent.getLongExtra("id" , 0).toString()
        oldDevice = if (intent.getStringExtra("oldDevice") != null) {
            intent.getStringExtra("oldDevice").toString()
        } else {
            ""
        }
        newDevice = intent.getSerializableExtra("newDevice") as BindListResponse.DeviceItem?

        setViewsClickListener(
            this, binding.btnEnable, binding.btnCancel,
            binding.btnConfirm, binding.tvHelp, binding.title.tvTitle
        )

        for (i in Global.productList.indices) {
            newDevice?.let { device ->
                if (TextUtils.equals(device.deviceType.toString(), Global.productList[i].deviceType)) {
                    if (TextUtils.isEmpty(Global.productList[i].homeLogo)) {
                        GlideApp.with(this).load(R.mipmap.device_no_bind_right_img).into(binding.ivCenter)
                    } else {
                        GlideApp.with(this).load(Global.productList[i].homeLogo).into(binding.ivCenter)
                    }
                }
            }
        }

        /*val newDeviceIndex = DeviceManager.dataList.indexOfFirst { it.id.toString() == this@EnableDeviceActivity.id }
        if (newDeviceIndex != -1){
            newDeviceName = DeviceManager.dataList[newDeviceIndex].deviceName
        }*/
        newDevice?.let {
            newDeviceName = it.deviceName
        }
        observe()
    }

    private fun observe() {
        viewModel.enableDevice.observe(this, Observer {
            dismissDialog()
            handler.removeCallbacksAndMessages(null)
            if (!it.isNullOrEmpty()) {
                binding.lyBubblesBg.visibility = View.VISIBLE
                binding.tvBubble.visibility = View.GONE
                binding.ivBubble.visibility = View.VISIBLE
                binding.lyFinishBottomView.visibility = View.VISIBLE
                binding.roundView.visibility = View.INVISIBLE
                timerHandler.removeCallbacksAndMessages(null)

                if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_BIND,
                        enableDeviceTrackingLog.apply {
                            log += "\n启用设备网络请求超时/失败"
                        }, "1224", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, enableDeviceTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                    })
                }

                when (it) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        LogUtils.i(TAG, "enableDevice = REQUEST_FAIL", true)
                        binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips4)
                        binding.ivBubble.setImageResource(R.mipmap.enable_view_bubbles_failure)
                        isEnable = IS_ENABLE_FAILURE
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        LogUtils.i(TAG, "enableDevice = REQUEST_SUCCESS", true)
                        binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips3)
                        binding.ivBubble.setImageResource(R.mipmap.enable_view_bubbles_success)
                        isEnable = IS_ENABLE_SUCCESS
                        newDevice?.let {
                            SpUtils.setValue(SpUtils.DEVICE_NAME, it.deviceName)
                            SpUtils.setValue(SpUtils.DEVICE_MAC, it.deviceMac)
                        }
                        ControlBleTools.getInstance().disconnect()
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        LogUtils.i(TAG, "enableDevice = REQUEST_CODE_ERROR", true)
                        binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips4)
                        binding.ivBubble.setImageResource(R.mipmap.enable_view_bubbles_failure)
                        isEnable = IS_ENABLE_FAILURE
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        LogUtils.e(TAG, "enableDevice = SERVER_ERROR", true)
                        binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips4)
                        binding.ivBubble.setImageResource(R.mipmap.enable_view_bubbles_failure)
                        isEnable = IS_ENABLE_FAILURE
                    }
                }
            }
        })
    }

    private fun dismissDialog() {
        if (dialog != null) DialogUtils.dismissDialog(dialog)
    }

    private var count = 0.1f
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable: Runnable = object : Runnable {
        override fun run() {
            timerHandler.removeCallbacksAndMessages(null)
            if (count <= 0.9f) {
                count += 0.1f
                binding.roundView.setProgress(count)
                timerHandler.postDelayed(this, 50)
            }
        }
    }

    private fun startDevice() {
        //网络已连接
        if (NetworkUtils.isConnected()) {
            LogUtils.i(TAG, "startDevice = 网络已连接")
            handler.postDelayed({
                newDevice?.let {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed(EnableRunnable(), 30 * 1000)
                    viewModel.enableDevice(it.id.toString(), enableDeviceTrackingLog)
                }
            }, 1000)
            binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips2)
            binding.lyBubblesBg.visibility = View.GONE
            binding.lyDefaultBottomView.visibility = View.GONE
            binding.textView7.text = newDeviceName
            dialog = DialogUtils.showLoad(this@EnableDeviceActivity)
            dialog?.show()
            count = 0.1f
            binding.roundView.visibility = View.VISIBLE
            timerHandler.postDelayed(timerRunnable, 50)
        }
        //网络未连接
        else {
            dialog = DialogUtils.showDialogTitle(this@EnableDeviceActivity,/*getString(R.string.dialog_title_tips)*/
                null,
                getString(R.string.not_network_tips),
                getString(R.string.dialog_cancel_btn),
                getString(R.string.dialog_retry_btn),
                object : DialogUtils.DialogClickListener {
                    override fun OnOK() {
                        startDevice()
                    }

                    override fun OnCancel() {
                    }
                })
            dialog?.show()

            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("启用设备异常").apply {
                log = "网络未连接"
            }, "1225", true)
        }
    }

    /**
     * 启用设备超时处理
     */
    private inner class EnableRunnable : Runnable {
        override fun run() {
            LogUtils.i(TAG, "enableDevice = timeout", true)
            dismissDialog()
            binding.lyBubblesBg.visibility = View.VISIBLE
            binding.tvBubble.visibility = View.GONE
            binding.ivBubble.visibility = View.VISIBLE
            binding.lyFinishBottomView.visibility = View.VISIBLE
            binding.roundView.visibility = View.INVISIBLE
            binding.tvCenterTitle.text = getString(R.string.device_info_enable_view_center_tips4)
            binding.ivBubble.setImageResource(R.mipmap.enable_view_bubbles_failure)
            isEnable = IS_ENABLE_FAILURE
        }
    }

    private fun startSuccess() {
        ManageActivity.removeActivity(NotEnabledActivity::class.java)
        ManageActivity.removeActivity(DeviceManageActivity::class.java)
        //切换设备后也弹后台保活提醒弹窗
        SpUtils.getSPUtilsInstance().put(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, false)
        SpUtils.getSPUtilsInstance().put(SpUtils.NOTIFY_USER_GUIDANCE_TIPS, false)
        GlobalEventManager.isCanShowFirmwareUpgrade = true
        GlobalEventManager.isCanUpdateAgps = true
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getEndTypeTrack("启用"), isEnd = true)

        this.finish()
    }

    private fun onBackEvent() {
        ControlBleTools.getInstance().disconnect()
        if (isEnable == IS_ENABLE_SUCCESS) {
            startSuccess()
        } else {
            this.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Global.IS_ENABLE_DEVICE = true
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        Global.IS_ENABLE_DEVICE = false
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
        handler.removeCallbacksAndMessages(null)
    }

}