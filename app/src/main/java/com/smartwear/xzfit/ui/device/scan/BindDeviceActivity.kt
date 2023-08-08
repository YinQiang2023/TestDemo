package com.smartwear.xzfit.ui.device.scan

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.NetworkUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.BindDeviceActivityBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.ui.device.devicecontrol.EnableDeviceActivity
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.BindDeviceBean
import com.zhapp.ble.callback.BindDeviceStateCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.RequestDeviceBindStateCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.db.model.track.BehaviorTrackingLog
import com.smartwear.xzfit.db.model.track.TrackingLog
import com.smartwear.xzfit.expansion.postDelay
import com.smartwear.xzfit.https.MyRetrofitClient
import com.smartwear.xzfit.https.Response
import com.smartwear.xzfit.ui.GlobalEventManager
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.device.DeviceManageActivity
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.guide.GuideConfigActivity
import com.smartwear.xzfit.ui.user.QAActivity
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference

class BindDeviceActivity : BaseActivity<BindDeviceActivityBinding, DeviceModel>(
    BindDeviceActivityBinding::inflate,
    DeviceModel::class.java
), View.OnClickListener {
    private val TAG: String = BindDeviceActivity::class.java.simpleName

    companion object {
        const val EXTRA_LOGO_URL = "url"
        const val EXTRA_IS_SCAN_CODE = "isScanCode"
        const val EXTRA_NAME = "name"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CODE = "verificationCode"
        const val EXTRA_DEVICE_TYPE = "deviceType"
        const val EXTRA_DEVICE_ISBR = "isBr"
        const val N008_SKIP_RELE_NAME = "-"
    }

    /**
     * 扫码直连，不需要设备打勾
     */
    private var type: Int = 0
    private var state: Int = 1
    private var name: String = ""
    private var address: String = ""
    private var deviceType = ""
    private var verificationCode: String = ""
    private var isBrDevice: Boolean = false

    //绑定完成请求版本信息
    private var isBindSuccessVersionInfo = false

    //绑定完成进入语言选择请求码
    private val BIND_COMPLETE_LANGUAGE_SET_REQUEST_CODE = 1000
    private var mtu = 0
    private var maxValue = 0
    private var minValue = 0
    private var isMatch = false
    private var isRequestSuccess = false
    private var dialog: Dialog? = null

    //连接超时次数
    private var timeoutNum = 3

    //是否进入请求开启蓝牙意图
    private var isRequestBle = false

    //是否是绑定界面请求的设备绑定列表
    private var isBindActRequestList = false

    //是否进入关闭
    private var isMyFinishing = false

    //获取设备产品信息
    private val getProductInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("获取设备产品信息", "获取产品设备信息", "infowear/product/info") }

    //查询设备绑定状态
    private val bindStateTrackingLog by lazy { TrackingLog.getDevTyepTrack("查询设备绑定状态", "请求当前连接设备的绑定状态", "INQUIRY_BINDING_STATUS") }

    //绑定
    private val bindTrackingLog by lazy { TrackingLog.getDevTyepTrack("设备绑定", "绑定设备", "BINDING_CHECK") }

    //ser绑定
    private val serBindDeviceTrackingLog by lazy { TrackingLog.getSerTypeTrack("后台绑定", "设备绑定", "infowear/device/bind") }

    //绑定行为
    private var bindBehavior: BehaviorTrackingLog? = null

    //是否设备绑定中
    private var isDevBinding = false

    //是否上报了绑定连接超时
    private var isPostBindConnectTimeOut = false

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btRetry.id -> {
                if (isSuccess) {
                    if (DeviceManager.dataList.size > 0) {
                        isBindActRequestList = true
                        viewModel.getBindList()
                    } else {
                        finishBindDevice()
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_BLE12)) {
                            PermissionUtils.checkRequestPermissions(
                                lifecycle, BaseApplication.mContext.getString(R.string.permission_bluetooth), PermissionUtils.PERMISSION_BLE12
                            ) {
                                binding.btRetry.callOnClick()
                            }
                            return
                        }
                    }
                    if (!AppUtils.isOpenBluetooth()) {
                        AppUtils.enableBluetooth(this, 0)
                        isRequestBle = true
                        return
                    }
                    //重试允许重新上传连接超时
                    isPostBindConnectTimeOut = false
                    when (state) {
                        1 -> {
                            refreshView(1)
                            if (isRequestSuccess) {
                                ControlBleTools.getInstance().setMtu(mtu, maxValue, minValue)
                                connectDevice()
                            } else {
                                getProductInfo()
                            }
                        }
                        2 -> {
                            requestBindState()
                        }
                        3 -> {
                            requestService()
                        }
                    }
                    initRetry()
                }
            }
//            ivDeviceIcon.id->{}
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
            }
        }
    }

    override fun initView() {
        super.initView()
        AppUtils.registerEventBus(this)
        setTvTitle(R.string.scan_device_title)
        bindHandler = Handler(Looper.myLooper()!!)
        finishHandler = Handler(Looper.myLooper()!!)
        setViewsClickListener(this, binding.tvHelp, binding.btRetry)
        binding.tvHelp.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线
        tvTitle?.setOnClickListener {
            if (DeviceManager.dataList.size > 0) {
                isBindActRequestList = true
                viewModel.getBindList()
            } else {
                finishBindDevice()
            }
        }
        dialog = DialogUtils.showLoad(this@BindDeviceActivity)
    }

    private fun dismissDialog() {
        if (dialog != null) DialogUtils.dismissDialog(dialog)
    }

    override fun initData() {
        super.initData()
        Global.IS_BIND_DEVICE = true
        observeInit()
        refreshView(1)

        var url = intent.getStringExtra(EXTRA_LOGO_URL) ?: ""
        if (!TextUtils.isEmpty(url)) {
            GlideApp.with(this).load(url)
                .error(R.mipmap.device_no_bind_right_img)
                .into(binding.ivDeviceIcon)
        } else {
            GlideApp.with(this).load(R.mipmap.device_no_bind_right_img)
                .error(R.mipmap.device_no_bind_right_img)
                .into(binding.ivDeviceIcon)
        }

        type = if (intent.getBooleanExtra(EXTRA_IS_SCAN_CODE, false)) 0 else 1
        name = intent.getStringExtra(EXTRA_NAME) ?: ""
        address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""
        verificationCode = intent.getStringExtra(EXTRA_CODE) ?: ""
        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE) ?: ""
        isBrDevice = intent.getBooleanExtra(EXTRA_DEVICE_ISBR, false)

        bindBehavior = AppTrackingManager.getNewBehaviorTracking("2", if (type == 0) "2" else "1")

        LogUtils.w(TAG, "bind info :$name  $address deviceType = $deviceType")
        viewModel.productInfo.observe(this, Observer {
            if (it == null || isMyFinishing) return@Observer

            if (it.code != HttpCommonAttributes.REQUEST_SUCCESS
                //没有数据一直点重试，会重复上报
                && it.code != HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA
            ) {
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_BIND,
                    getProductInfoTrackingLog.apply {
                        log += "获取设备产品信息超时/失败"
                    }, "1216", true
                )
            } else {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, getProductInfoTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                })
            }

            timerOutHandler.removeCallbacksAndMessages(null)
            dismissDialog()
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (isFinishing || isDestroyed) {
                        return@Observer
                    }
                    if (!it.data.mtuList.isNullOrEmpty()) {
                        for (i in it.data.mtuList!!.indices) {
                            if (it.data.mtuList!![i].phoneSystem == "2") {
                                LogUtils.w(TAG, "phone device model = ${DeviceUtils.getModel()}")
                                if (it.data.mtuList!![i].phoneModel == DeviceUtils.getModel()) {
                                    isMatch = true
                                    mtu = it.data.mtuList!![i].mtu.trim().toInt()
                                    maxValue = it.data.mtuList!![i].maxValue.trim().toInt()
                                    minValue = it.data.mtuList!![i].minValue.trim().toInt()
                                }
                            }
                        }
                        if (!isMatch) {
                            val position =
                                it.data.mtuList!!.indexOfFirst { it -> it.phoneSystem == "2" && it.phoneModel == "通用" }
                            mtu = it.data.mtuList!![position].mtu.trim().toInt()
                            maxValue = it.data.mtuList!![position].maxValue.trim().toInt()
                            minValue = it.data.mtuList!![position].minValue.trim().toInt()
                        }
                        isRequestSuccess = true
                        ControlBleTools.getInstance().setMtu(mtu, maxValue, minValue)
                        connectDevice()
                    } else {
                        connectDevice()
                    }
                }

                else -> {
                    if (it.code == HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA) { //无数据 ， 后台未配置
                        ToastUtils.showToast(getString(R.string.not_support_device_bind))
                    } else {
                        ToastUtils.showToast(getString(R.string.err_network_tips))
                    }
                    binding.tvConnectState1.text = getString(R.string.bind_device_state1_3)
                    binding.ivConnecting1.background =
                        ContextCompat.getDrawable(this, R.mipmap.bind_error)
                    bindError()
                }
            }
        })
        if (!TextUtils.isEmpty(deviceType)) {
            getProductInfo()
        } else {
            ToastUtils.showToast(getString(R.string.bind_device_state1_3))
            finishBindDevice()
        }


        binding.tvDeviceName.text = name

        viewModel.getBindListCode.observe(this, Observer {
            if (TextUtils.isEmpty(it) || !isBindActRequestList) {
                isBindActRequestList = false
                return@Observer
            }
            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    val isStatus = DeviceManager.dataList.indexOfFirst { it.deviceStatus == 1 }
                    val oldDeviceName = DeviceManager.dataList[isStatus].deviceName
                    val newDevice = DeviceManager.dataList.lastOrNull { it.deviceMac == address }
                    val currentId = DeviceManager.dataList.indexOfFirst { it.deviceMac == address }
                    if (currentId != -1 && newDevice != null && !TextUtils.equals(newDevice.deviceName, oldDeviceName) && !TextUtils.equals(
                            newDevice.deviceMac, DeviceManager.dataList[isStatus].deviceMac
                        )
                    ) {
                        com.blankj.utilcode.util.LogUtils.d(
                            "DeviceManager.dataList ->" + GsonUtils.toJson(
                                DeviceManager.dataList
                            )
                        )
                        ControlBleTools.getInstance().disconnect()
                        finish()
                        ManageActivity.removeActivity(DeviceManageActivity::class.java)
                        startActivity(
                            Intent(this@BindDeviceActivity, EnableDeviceActivity::class.java).putExtra("newDevice", newDevice).putExtra("oldDevice", oldDeviceName)
                        )
                        //多设备绑定成功行为记录
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("2", "4"))
                    } else {
                        finishBindDevice()
                    }
                }
            }
        })
    }

    private fun getProductInfo() {
        viewModel.getProductInfo(deviceType = deviceType, tracks = arrayOf(getProductInfoTrackingLog))
        dialog?.show()
        timerOutHandler.postDelayed(timerOutRunnable, 30 * 1000)
    }

    private fun connectDevice() {
        isDevBinding = true
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("连接").apply {
            log = "连接设备：name:$name,mac：$address"
        })
        if (!TextUtils.isEmpty(address)) {
            if (!TextUtils.isEmpty(name)) {
                viewModel.connect(name, address)
            }
            /* else {
                 if (*//*false && *//*(deviceType == "30000" || deviceType == "30002" || deviceType == "30004" || deviceType == "30006")) {
                    //deviceScanQrCodeBean.name = "N008_skip_rule_Name"
                    name = N008_SKIP_RELE_NAME
                    binding.tvDeviceName.text = name
                    viewModel.connect(name, address)
                }
            }*/
        } else {
            ToastUtils.showToast(getString(R.string.bind_device_state1_3))
            finishBindDevice()
        }
    }

    private val timerOutHandler = Handler(Looper.getMainLooper())
    private val timerOutRunnable: Runnable = Runnable { dismissDialog() }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun eventBusMsg(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        LogUtils.w(TAG, "device connected")
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("使能成功"))
                        requestBindState()
                        /* if (*//*false && *//*(deviceType == "30000" || deviceType == "30002" || deviceType == "30004" || deviceType == "30006")) {
                            //deviceScanQrCodeBean.name = "N008_skip_rule_Name"
                            name = ControlBleTools.getInstance().getCurrentDeviceName()
                            if (!TextUtils.equals(name, N008_SKIP_RELE_NAME)) {
                                binding.tvDeviceName.text = name
                            }
                        }*/
                    }
                    BleCommonAttributes.STATE_CONNECTING -> {
                        LogUtils.w(TAG, "device connecting")
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        LogUtils.w(TAG, "device disconnect")
                        if (isDevBinding) {
                            isDevBinding = false
                            AppTrackingManager.trackingModule(
                                AppTrackingManager.MODULE_BIND,
                                TrackingLog.getAppTypeTrack("绑定过程中蓝牙断连"),
                                "1220",
                                true
                            )
                        }
                    }
                    BleCommonAttributes.STATE_TIME_OUT -> {
                        //timeoutNum 次重连 3 min
                        timeoutNum--
                        LogUtils.w(TAG, "device time_out --" + timeoutNum)
                        if (timeoutNum > 0) {
                            connectDevice()
                        } else {
                            binding.tvConnectState1.text = getString(R.string.bind_device_state1_3)
                            binding.ivConnecting1.background = ContextCompat.getDrawable(this, R.mipmap.bind_error)
                            bindError()
                            timeoutNum = 3
                            if (!isPostBindConnectTimeOut) {
                                isPostBindConnectTimeOut = true
                                AppTrackingManager.trackingModule(
                                    AppTrackingManager.MODULE_BIND,
                                    TrackingLog.getAppTypeTrack("连接超时/失败"), "1217", true
                                )
                            }
                        }
                    }
                }
            }
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    if (binding.tvConnectState1.text.toString().trim() == getString(R.string.bind_device_state1_2)) {
                        binding.tvConnectState2.text = getString(R.string.bind_device_state2_3)
                        binding.ivConnecting2.background = ContextCompat.getDrawable(this, R.mipmap.bind_error)
                    }
                    binding.tvConnectState1.text = getString(R.string.bind_device_state1_3)
                    binding.ivConnecting1.background = ContextCompat.getDrawable(this, R.mipmap.bind_error)
                    bindError()
                    state = 1
                    isRequestSuccess = false
                    ActivityUtils.finishActivity(ScanDeviceActivity::class.java)
                    ActivityUtils.finishActivity(ScanCodeActivity::class.java)
                } else if (msg.arg == BluetoothAdapter.STATE_ON) {
                    if (isRequestBle) {
                        isRequestBle = false
                        binding.btRetry.callOnClick()
                    }
                }
            }
        }
    }

    //初始化重试
    private fun initRetry() {
        binding.btRetry.visibility = View.GONE
        binding.tvBindResult.visibility = View.GONE
    }

    private fun bindError() {
        binding.lyBottomView.visibility = View.VISIBLE
        binding.btRetry.visibility = View.VISIBLE
        binding.tvBindResult.visibility = View.VISIBLE
//        tvHelp.text =
//                SpannableStringTool.get().append(getString(R.string.bind_device_help)).setUnderline()
//                        .setFontSize(14f)
//                        .setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color)).create()
    }

    private fun requestBindState() {
        refreshView(2)
        bindStateTrackingLog.startTime = TrackingLog.getNowString()
        CallBackUtils.requestDeviceBindStateCallBack = MyRequestDeviceBindStateCallBack(this)
        ControlBleTools.getInstance().requestDeviceBindState(object : SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                bindStateTrackingLog.log = "请求状态码：state == $state"
                if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindStateTrackingLog.apply {
                        log += "\n查询设备绑定失败/超时"
                    }, "1229", true)
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindStateTrackingLog)
                }
            }
        })
        com.blankj.utilcode.util.LogUtils.d("BIND requestDeviceBindState")
        bindHandler?.postDelayed({
            bindDeviceError()
        }, 30 * 1000)
    }

    class MyRequestDeviceBindStateCallBack(activity: BindDeviceActivity) :
        RequestDeviceBindStateCallBack {
        private var wrActivity: WeakReference<BindDeviceActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }

        override fun onBindState(state: Boolean) {

            wrActivity?.get()?.apply {
                runOnUiThread {

                    bindStateTrackingLog.endTime = TrackingLog.getNowString()
                    bindStateTrackingLog.devResult = "state : $state"

                    if (state) {
                        ErrorUtils.onLogResult("设备已被绑定state=$state")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_BIND_DEVICE_FOR_BIND_INFO)
//                        ControlBleTools.getInstance().disconnect()
                        bindDeviceError()
                        val dialog = DialogUtils.dialogShowContent(
                            this,
                            getString(R.string.bind_device_reset),
                            getString(R.string.dialog_confirm_btn),
                            object : DialogUtils.DialogClickListener {
                                override fun OnOK() {
//                                    finishBindDevice()
                                }

                                override fun OnCancel() {

                                }
                            })
                        dialog.setCancelable(true)

                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindStateTrackingLog.apply {
                            log += "\n设备已被绑定"
                        }, "1219", true)
                    } else {
                        bindDevice()
                    }
                }
            }
        }
    }

    private var bindHandler: Handler? = null

    private var serialNumber: String = ""
    private var deviceNumber: String = ""
    private var firmwareVersion: String = ""

    private fun bindDevice() {
        CallBackUtils.bindDeviceStateCallBack = MyBindDeviceStateCallBack(this)

        LogUtils.w(TAG, "bindDevice type = " + type)
        //二维码绑定-直连
        if (type == 0) {
            LogUtils.w(TAG, "bindDevice verificationCode =$verificationCode")
            ControlBleTools.getInstance().bindDevice(verificationCode, object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    bindTrackingLog.log = "二维码绑定-直连：verificationCode = $verificationCode,state == $state"
                    if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindTrackingLog.apply {
                            log += "\n设备绑定失败"
                        }, "1222", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindTrackingLog)
                    }
                }
            })
        }
        //设备弹框-绑定-非直连
        else {
            ControlBleTools.getInstance().bindDevice(object : SendCmdStateListener(this.lifecycle) {
                override fun onState(state: SendCmdState?) {
                    bindTrackingLog.log = "设备弹框-绑定-非直连,state == $state"
                    if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindTrackingLog.apply {
                            log += "\n设备绑定失败"
                        }, "1222", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindTrackingLog)
                    }
                }
            })

        }
    }

    class MyBindDeviceStateCallBack(activity: BindDeviceActivity) : BindDeviceStateCallBack {
        private var wrActivity: WeakReference<BindDeviceActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }

        override fun onDeviceInfo(bindDeviceBean: BindDeviceBean) {
            wrActivity?.get()?.apply {
                bindHandler?.removeCallbacksAndMessages(null)
                runOnUiThread {
                    bindTrackingLog.endTime = TrackingLog.getNowString()
                    bindTrackingLog.devResult = GsonUtils.toJson(bindDeviceBean)

                    if (bindDeviceBean.deviceVerify) {
                        this.deviceNumber = bindDeviceBean.deviceNumber
                        this.serialNumber = bindDeviceBean.serialNumber
                        this.firmwareVersion = bindDeviceBean.firmwareVersion
                        this.name = ControlBleTools.getInstance().getCurrentDeviceName()
                        if (!TextUtils.equals(name, N008_SKIP_RELE_NAME)) {
                            binding.tvDeviceName.text = name
                        }
                        requestService()
                    } else {
                        ErrorUtils.onLogResult("设备绑定失败deviceVerify=${bindDeviceBean.deviceVerify}")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_BIND_DEVICE_FOR_DEVICE)
                        bindDeviceError()
                        state = 2

                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, bindTrackingLog.apply {
                            log += "\n绑定-bindCheck失败"
                        }, "1221", true)
                    }
                }
            }
        }
    }

    private fun bindDeviceError() {
        binding.tvConnectState2.text = getString(R.string.bind_device_state2_3)
        binding.ivConnecting2.background = ContextCompat.getDrawable(this, R.mipmap.bind_error)
        bindError()
    }

    private fun requestService() {
        LogUtils.e(TAG, "requestService")
        isSuccess = false
        //解决livedata 造成的问题 http://jira.wearheart.cn/browse/ACP-1137?filter=-1
        //viewModel.bindDevice(deviceNumber, address, name, serialNumber, firmwareVersion)
        refreshView(3)
        viewModel.launchUI {
            try {
                val bean = com.smartwear.xzfit.https.params.BindDeviceBean()
                bean.userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (TextUtils.isEmpty(bean.userId)) {
                    return@launchUI
                }
                bean.deviceType = deviceType
                bean.deviceMac = address
                bean.deviceName = name
                bean.deviceSn = serialNumber
                bean.deviceVersion = firmwareVersion

                serBindDeviceTrackingLog.serReqJson = AppUtils.toSimpleJsonString(bean)

                val result = MyRetrofitClient.service.bindDevice(JsonUtils.getRequestJson(TAG, bean, com.smartwear.xzfit.https.params.BindDeviceBean::class.java))
                LogUtils.e(TAG, "bindDevice result = $result")

                serBindDeviceTrackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                serBindDeviceTrackingLog.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"

                viewModel.userLoginOut(result.code)
                bindDeviceResult(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "bindDevice e =$e")
                val result = Response("", "bindDevice e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                serBindDeviceTrackingLog.serResJson = AppUtils.toSimpleJsonString(result)
                serBindDeviceTrackingLog.serResult = "失败"
                bindDeviceResult(HttpCommonAttributes.SERVER_ERROR)
            }
        }

    }

    private fun bindDeviceResult(it: String) {
        if (!TextUtils.isEmpty(it)) {


            if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_BIND,
                    serBindDeviceTrackingLog.apply {
                        log += "\n账号绑定请求超时/失败"
                    }, "1223", true
                )
            } else {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, serBindDeviceTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                })
            }

            //绑定成功
            if (it == HttpCommonAttributes.REQUEST_SUCCESS) {
                LogUtils.i(TAG, "后台返回-绑定成功")
                binding.tvConnectState3.text = getString(R.string.bind_device_state3_4)
                binding.ivConnecting3.background = ContextCompat.getDrawable(this, R.mipmap.bind_success)
                isSuccess = true
//                    ToastUtils.showToast(getString(R.string.successful_operation_tips))
                ManageActivity.removeActivity(ScanDeviceActivity::class.java)
                isBindSuccessVersionInfo = true
                viewModel.versionInfo(deviceNumber, firmwareVersion)
                sendUserIdToDevice()
                SpUtils.getSPUtilsInstance().put(SpUtils.BIND_DEVICE_CONNECTED_KEEPLIVE_EXPLANATION, false)
                SpUtils.getSPUtilsInstance().put(SpUtils.NOTIFY_USER_GUIDANCE_TIPS, false)
                GlobalEventManager.isCanShowFirmwareUpgrade = true
                GlobalEventManager.isCanUpdateAgps = true
                isDevBinding = false
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getEndTypeTrack("绑定"), isEnd = true)
                //绑定成功行为记录
                bindBehavior?.let {
                    //it.durationSec = (System.currentTimeMillis()/1000 - it.startTimestamp.toLong()).toString()
                    AppTrackingManager.saveBehaviorTracking(it)
                }

            }
            //重复绑定
            else if (it == HttpCommonAttributes.DUPLICATE_BINDING) {
                LogUtils.i(TAG, "后台返回-重复绑定")
                ErrorUtils.onLogResult("后台返回-重复绑定")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_BIND_DEVICE_FOR_SERVICE_1)
                sendUserIdError()

                DialogUtils.dialogShowContent(
                    ActivityUtils.getTopActivity(),
                    getString(R.string.bind_device_duplicate),
                    getString(R.string.know),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                        }

                        override fun OnCancel() {
                        }
                    })

            } else if (it == HttpCommonAttributes.SERVER_ERROR) {
                LogUtils.i(TAG, "后台返回-网络异常")
                ErrorUtils.onLogResult("后台返回-网络异常")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_BIND_DEVICE_FOR_SERVICE_2)
                sendUserIdError()
                ToastUtils.showToast(getString(R.string.err_network_tips))
            } else {
                LogUtils.i(TAG, "后台返回-绑定失败 = it = $it")
                ErrorUtils.onLogResult("后台返回-绑定失败")
                ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_BIND_DEVICE_FOR_SERVICE_3)
                sendUserIdError()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Global.IS_BIND_DEVICE = false
        isDevBinding = false
        AppUtils.unregisterEventBus(this)
        bindHandler?.removeCallbacksAndMessages(null)
        finishHandler?.removeCallbacksAndMessages(null)
        timerOutHandler.removeCallbacksAndMessages(null)
    }

    private var isSuccess: Boolean = false
    private fun observeInit() {
        viewModel.versionInfo.observe(this, Observer {
            LogUtils.i(TAG, "versionInfo -- $it")
            if (!TextUtils.isEmpty(it) && isBindSuccessVersionInfo) {
                isBindSuccessVersionInfo = false
                if (it == HttpCommonAttributes.REQUEST_SUCCESS) {
                    //如果有配置引导页数据跳引导页
                    if (viewModel.productInfo.value?.data?.guidePageData?.isNotEmpty() == true) {
                        val intent = Intent(this, GuideConfigActivity::class.java)
                        intent.putExtra(
                            GuideConfigActivity.GUIDE_PAGE_DATA, viewModel.productInfo.value?.data?.guidePageData
                        ).putExtra(GuideConfigActivity.BIND_DEVICE_MAC, address)
                        startActivityForResult(intent, BIND_COMPLETE_LANGUAGE_SET_REQUEST_CODE)
                        return@Observer
                    }
                }
                //未获取到后台配置的语言绑定
                binding.lyBottomView.visibility = View.VISIBLE
                binding.btRetry.visibility = View.VISIBLE
                binding.btRetry.text = getString(R.string.dialog_complete_btn)
                binding.tvBindResult.visibility = View.GONE
                binding.tvHelp.visibility = View.GONE
                //检测bt配对
                EventBus.getDefault().post(EventMessage(EventAction.ACTION_HEADSET_BOND, address))
            }
        })
    }

    private fun sendUserIdToDevice() {
        ControlBleTools.getInstance().sendAppBindResult(SpUtils.getValue(SpUtils.USER_ID, ""), null)
        ControlBleTools.getInstance().setTime(System.currentTimeMillis(), null)

    }

    private fun sendUserIdError() {
        isSuccess = false
        binding.lyBottomView.visibility = View.VISIBLE
        binding.tvConnectState3.text = getString(R.string.bind_device_state3_3)
        binding.ivConnecting3.background = ContextCompat.getDrawable(this, R.mipmap.bind_error)
        bindError()
    }

    private fun refreshView(state: Int) {
        this.state = state
        when (state) {
            1 -> {
                binding.tvConnectState1.text = getString(R.string.bind_device_state1_1)
                binding.tvConnectState2.text = getString(R.string.bind_device_state2_1)
                binding.tvConnectState3.text = getString(R.string.bind_device_state3_1)
                binding.tvTopTips.text = getString(R.string.bind_device_tip)


                binding.ivConnecting1.background = ContextCompat.getDrawable(this, R.drawable.loading)
                binding.ivConnecting2.background = ContextCompat.getDrawable(this, R.mipmap.round_bg)
                binding.ivConnecting3.background = ContextCompat.getDrawable(this, R.mipmap.round_bg)

                val animationDrawable = binding.ivConnecting1.background as AnimationDrawable
                animationDrawable.start()

                binding.lyBottomView.visibility = View.INVISIBLE
                initRetry()
            }
            2 -> {
                binding.tvConnectState1.text = getString(R.string.bind_device_state1_2)
                binding.tvConnectState2.text = getString(R.string.bind_device_state2_2)
                binding.tvTopTips.text = getString(R.string.bind_device_tip2)

                binding.ivConnecting1.background =
                    ContextCompat.getDrawable(this, R.mipmap.bind_success)
                binding.ivConnecting2.background =
                    ContextCompat.getDrawable(this, R.drawable.loading)
                val animationDrawable = binding.ivConnecting2.background as AnimationDrawable
                animationDrawable.start()
                binding.lyBottomView.visibility = View.INVISIBLE
            }
            3 -> {
                binding.tvConnectState2.text = getString(R.string.bind_device_state2_4)
                binding.tvConnectState3.text = getString(R.string.bind_device_state3_2)
                binding.tvTopTips.text = getString(R.string.bind_device_tip)
                binding.ivConnecting2.background = ContextCompat.getDrawable(this, R.mipmap.bind_success)
                binding.ivConnecting3.background = ContextCompat.getDrawable(this, R.drawable.loading)
                val animationDrawable = binding.ivConnecting3.background as AnimationDrawable
                animationDrawable.start()
            }
        }
    }

    private var finishHandler: Handler? = null
    fun finishBindDevice() {
        ErrorUtils.clearErrorBindDevice()
        isMyFinishing = true
        if (!isSuccess) {
            val dialog2 = DialogUtils.dialogShowContentAndTwoBtn(this,
                getString(R.string.bind_view_back_tips),
                getString(R.string.dialog_cancel_btn),
                getString(R.string.dialog_confirm_btn),
                object : DialogUtils.DialogClickListener {
                    override fun OnOK() {
                        postDelay(200) {
                            if (!isSuccess) {
                                isMyFinishing = false
                                Global.IS_BIND_DEVICE = false
                                isDevBinding = false
                                ControlBleTools.getInstance().disconnect()
                                ManageActivity.removeActivity(DeviceManageActivity::class.java)
                                ManageActivity.removeActivity(ScanDeviceActivity::class.java)
                                finish()
                                EventBus.getDefault()
                                    .post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
                            }
                        }
                    }

                    override fun OnCancel() {
                        isMyFinishing = false
                    }

                })
            dialog2.setCancelable(true)
        } else {
            isMyFinishing = false
            Global.IS_BIND_DEVICE = false
            isDevBinding = false
            SpUtils.setValue(SpUtils.DEVICE_NAME, name)
            SpUtils.setValue(SpUtils.DEVICE_MAC, address)
            //ControlBleTools.getInstance().disconnect()
            finish()
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BIND_COMPLETE_LANGUAGE_SET_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                NetworkUtils.isAvailableAsync { isAvailable ->
                    if (isAvailable) {
                        binding.btRetry.callOnClick()
                    } else {
                        finishBindDevice()
                    }
                }
            }
        }
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (DeviceManager.dataList.size > 0) {
                isBindActRequestList = true
                viewModel.getBindList()
            } else {
                finishBindDevice()
            }
            return false
        }
        return super.onKeyDown(keyCode, event)
    }


}