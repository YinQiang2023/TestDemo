package com.smartwear.publicwatch.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.fastjson.JSON
import com.android.mycamera.CameraActivity
import com.android.mycamera.util.CameraUtil
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ThreadUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.databinding.FragmentDeviceBinding
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.setting.notify.MsgNotifySetActivity
import com.smartwear.publicwatch.ui.device.setting.remind.RemindSetActivity
import com.smartwear.publicwatch.ui.device.scan.ScanDeviceActivity
import com.smartwear.publicwatch.ui.device.theme.ThemeCenterActivity
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.RealTimeBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.RequestDeviceBindStateCallBack
import com.zhapp.ble.callback.VerifyUserIdCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.blankj.utilcode.util.FileUtils
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.databinding.ItemDialMoreBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.expansion.postDelay
import com.smartwear.publicwatch.https.response.GetDialListResponse
import com.smartwear.publicwatch.ui.adapter.MultiItemCommonAdapter
import com.smartwear.publicwatch.ui.device.DeviceManageActivity
import com.smartwear.publicwatch.ui.device.DeviceSettingLiveData
import com.smartwear.publicwatch.ui.device.backgroundpermission.BackgroundPermissionMainActivity
import com.smartwear.publicwatch.ui.device.bean.DeviceSettingBean
import com.smartwear.publicwatch.ui.device.devicecontrol.UnBindDeviceActivity
import com.smartwear.publicwatch.ui.device.setting.contacts.ContactsActivity
import com.smartwear.publicwatch.ui.device.setting.heartrate.HeartRateSettingActivity
import com.smartwear.publicwatch.ui.device.setting.sleep.SleepSettingActivity
import com.smartwear.publicwatch.ui.device.setting.stress.StressSetActivity
import com.smartwear.publicwatch.ui.device.weather.WeatherActivity
import com.smartwear.publicwatch.ui.livedata.RefreshBatteryState
import com.smartwear.publicwatch.ui.livedata.bean.BatteryBean
import com.smartwear.publicwatch.ui.user.bean.UserBean
import com.smartwear.publicwatch.ui.view.ViewForLayoutNoInternet
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.utils.manager.MicroManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference

@SuppressLint("UseRequireInsteadOfGet")
class DeviceFragment : BaseFragment<FragmentDeviceBinding, DeviceModel>(
    FragmentDeviceBinding::inflate,
    DeviceModel::class.java
), View.OnClickListener {
    private val TAG: String = DeviceFragment::class.java.simpleName

    //用户信息
    private lateinit var mUserBean: UserBean

    //是否进入请求开启蓝牙意图
    private var isRequestBle = false

    //是否进入添加设备意图
    private var isStartAddDevice = false

    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    //设备信息回调
    private var myDeviceInfoCallBack: MyDeviceInfoCallBack? = null

    //设备绑定状态回调
    private var myFRequestDeviceBindStateCallBack: MyFRequestDeviceBindStateCallBack? = null

    //设备列表items
    private val list: MutableList<Bean> = ArrayList()

    //刷新设备列表的时间
    private var mLastRefreshTime = 0L

    //最后一次加载设置列表布局是否启用
    private var lastEnable: Boolean? = null

    //是否可以记录连接设备蓝牙断连
    var canRecordConnectBleOff = false

    //表盘中心示例适配器
    private val onlineAdapter by lazy {
        initSimpleAdapter()
    }

    private var simpleList: MutableList<GetDialListResponse.Data> = mutableListOf()


    companion object {
        var viewIsVisible = false
    }

    class Bean {
        var img: Int = 0
        var text: String = ""
    }

    override fun setTitleId(): Int {
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        EventBus.getDefault().register(this)
        if (this.activity == null) {
            return
        }

        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
        if (AppUtils.isOpenBluetooth()) {
            binding.tvCloseBt.visibility = View.GONE
            binding.layoutState.visibility = View.VISIBLE
        } else {
            binding.tvCloseBt.visibility = View.VISIBLE
            binding.layoutState.visibility = View.GONE
        }
        setViewsClickListener(
            this,
            binding.lyNoDeviceBind.lyNoBindAdd,
            binding.layoutState,
            binding.lySyncing,
            binding.layoutDevManage.root,
            binding.clTheme
        )
        binding.lyNoNetWork.setRetryListener(MyNotNetRetryListener())

        binding.rvSimple.apply {
            adapter = onlineAdapter
            layoutManager = GridLayoutManager(this.context, 3)
            setHasFixedSize(true)
        }
    }

    override fun onResume() {
        super.onResume()
        com.blankj.utilcode.util.LogUtils.d(TAG, "onResume")
        postDelay(200) {
            getUserInfoData()
        }
    }

    private fun getUserInfoData() {
        mUserBean = UserBean().getData()
        refreshHead()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initData() {
        super.initData()
        if (this.context == null) return
        list.clear()
        val texts = resources.getStringArray(R.array.deviceSetStringList)
        val imgs = resources.obtainTypedArray(R.array.deviceSetImageList)
        for (i in 0 until imgs.length()) {
            val bean = Bean()
            bean.img = imgs.getResourceId(i, 0)
            bean.text = texts[i]
            list.add(bean)
        }
        imgs.recycle()

        if (deviceSettingBean == null) {
            binding.layoutDeviceSetList.visibility = View.GONE
        } else {
            loadViews(ControlBleTools.getInstance().isConnect)
        }

        myDeviceInfoCallBack = MyDeviceInfoCallBack(this)
        CallBackUtils.deviceInfoCallBack = myDeviceInfoCallBack

        viewmodel.getBindListCode.observe(this, MyBindListCodeObserver())
        viewmodel.getHomeByProductList.observe(this, Observer {
            if (it == null) return@Observer
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (!it.data.list.isNullOrEmpty()) {
                        simpleList.clear()
                        //取三张表盘
                        var index = 3
                        for (i in it.data.list.indices) {
                            if (index > 0) {
                                if (it.data.list[i].dialList.isNotEmpty()) {
                                    val bean = GetDialListResponse.Data()
                                    val dialList: MutableList<GetDialListResponse.Data2> =
                                        mutableListOf(it.data.list[i].dialList[0])
                                    bean.dialList = dialList
                                    bean.dialType = it.data.list[i].dialType
                                    bean.dialTypeId = it.data.list[i].dialTypeId
                                    bean.dialTypeName = it.data.list[i].dialTypeName
                                    simpleList.add(bean)
                                    index--
                                }
                            } else continue
                        }
                        //添加占位表盘
                        if (simpleList.size < 3)
                            for (index in 1..3 - simpleList.size)
                                simpleList.add(GetDialListResponse.Data())
                        onlineAdapter.notifyDataSetChanged()
                    } else {
                        simpleList.clear()
                        for (i in 0..2) {
                            simpleList.add(GetDialListResponse.Data())
                        }
                        onlineAdapter.notifyDataSetChanged()
                    }
                }
                else -> {

                }
            }
        })

        refreshDeviceView()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
//        com.blankj.utilcode.util.LogUtils.e("DeviceFragment setUserVisibleHint $isVisibleToUser")
        viewIsVisible = if (isVisibleToUser) {
            RealTimeRefreshDataUtils.openRealTime()
            true
        } else {
            RealTimeRefreshDataUtils.closeRealTime()
            false
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.clTheme.id -> {
                startActivity(Intent(activity, ThemeCenterActivity::class.java))
            }

            binding.layoutDevManage.root.id -> {
                startActivity(Intent(activity, DeviceManageActivity::class.java))
            }

            binding.layoutState.id -> {
                if (binding.tvState.text.toString()
                        .trim() == getString(R.string.device_connected_failed) ||
                    binding.tvState.text.toString()
                        .trim() == getString(R.string.device_no_connection)
                ) {
                    ControlBleTools.getInstance().disconnect()
                    SendCmdUtils.connectDevice(
                        SpUtils.getValue(SpUtils.DEVICE_NAME, ""),
                        SpUtils.getValue(SpUtils.DEVICE_MAC, "")
                    )
                }
            }

            binding.lySyncing.id -> {
                if (binding.tvSync.text.toString().trim() == getString(R.string.sync_fail_tips)) {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_SYNC))
                }
            }

            binding.lyNoDeviceBind.lyNoBindAdd.id -> {
                startAddDevice()
            }
        }
    }

    /**
     * 简略表盘适配器初始化
     */
    private fun initSimpleAdapter(): MultiItemCommonAdapter<GetDialListResponse.Data, ItemDialMoreBinding> {
        return object :
            MultiItemCommonAdapter<GetDialListResponse.Data, ItemDialMoreBinding>(simpleList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemDialMoreBinding {
                return ItemDialMoreBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemDialMoreBinding,
                t: GetDialListResponse.Data,
                position: Int,
            ) {
                v.tvItemName.visibility = View.GONE
                v.ivItem.setBackgroundResource(R.drawable.clock_dial_bg_select_off)
                if (t.dialTypeId.isEmpty()) {
                    GlideApp.with(requireActivity())
                        //.placeholder(R.mipmap.no_renderings)
                        .load(R.mipmap.no_data_transparent)
                        .into(v.ivItem)
                    return
                }
                if (t.dialList[0].effectImgUrl.isEmpty()) {
                    GlideApp.with(requireActivity())
                        //.placeholder(R.mipmap.no_renderings)
                        .load(R.mipmap.no_data_transparent)
                        .into(v.ivItem)
                } else {
                    GlideApp.with(requireActivity())
                        //.placeholder(R.mipmap.no_renderings)
                        .load(t.dialList[0].effectImgUrl)
                        .into(v.ivItem)
                }

                if (t.dialList.isNotEmpty())
                    setViewsClickListener({
//                        startActivity(
//                            Intent(activity, DialDetailsActivity::class.java)
//                                .putExtra("url", t.dialList[0].effectImgUrl)
//                                .putExtra("dialId", t.dialList[0].dialId)
//                        )
                        startActivity(Intent(activity, ThemeCenterActivity::class.java))
                    }, v.lyItem)

            }

            override fun getItemType(t: GetDialListResponse.Data): Int {
                return 0
            }

        }
    }

//region 打开添加设备
    /**
     * 打开添加设备
     * @param hint 是否提示确实 gps 蓝牙 未开启
     */
    fun startAddDevice() {

        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getStartTypeTrack("绑定设备"), isStart = true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_BLE12)) {

                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("蓝牙权限"), "1211", isEnd = true)

                PermissionUtils.checkRequestPermissions(lifecycle, BaseApplication.mContext.getString(R.string.permission_bluetooth), PermissionUtils.PERMISSION_BLE12) {
                    startAddDevice()
                }
                return
            }
        }
        isStartAddDevice = true
        if (AppUtils.isOpenBluetooth()) {

            if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_GROUP_LOCATION)) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("定位权限（android)"), "1210", isEnd = true)
            }

            PermissionUtils.checkRequestPermissions(
                this.lifecycle,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getString(R.string.permission_location_12) else getString(
                    R.string.permission_location
                ),
                PermissionUtils.PERMISSION_GROUP_LOCATION
            ) {
                if (DeviceManager.dataList.size >= 5) {
                    ToastUtils.showToast(R.string.device_max_device_bind_tips)
                    isStartAddDevice = false
                } else {
                    if (!AppUtils.isGPSOpen(activity!!)) {
                        AppUtils.showGpsOpenDialog()
                        return@checkRequestPermissions
                    }
                    startActivity(Intent(activity, ScanDeviceActivity::class.java))
                    isStartAddDevice = false
                }
            }
        } else {
            AppUtils.enableBluetooth(requireActivity(), 0)
            isRequestBle = true
        }
    }
    //endregion

    //region EventBus
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                //蓝牙打开
                if (msg.arg == BluetoothAdapter.STATE_ON) {
                    binding.tvCloseBt.visibility = View.GONE
                    binding.layoutState.visibility = View.VISIBLE
                    /*sdk内部已处理
                    ControlBleTools.getInstance().release()
                    BaseApplication.initControlBleTools(BaseApplication.mContext)*/
                    val name = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
                    val mac = SpUtils.getValue(SpUtils.DEVICE_MAC, "")
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(mac) && !Global.IS_BIND_DEVICE) {
                        ThreadUtils.runOnUiThreadDelayed({
                            SendCmdUtils.connectDevice(name, mac)
                        }, 500)
                    } else {
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_BIND_DEVICE))
                    }
                    if (isRequestBle) {
                        isRequestBle = false
                        startAddDevice()
                    }
                } else if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    binding.tvCloseBt.visibility = View.VISIBLE
                    binding.layoutState.visibility = View.GONE
                    binding.layoutBattery.visibility = View.GONE
                    //sdk内部已处理 ControlBleTools.getInstance().release()
                    ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                    if (canRecordConnectBleOff) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("系统蓝牙开关被关闭"), "1311", true)
                    }
                }
            }
            /* EventAction.ACTION_SYNCING_DATA -> {
                 if ((msg.obj as Boolean)) {
                     startSyncAnimation()
                 } else {
                     stopSyncAnimation(msg.arg)
                 }
             }*/
            EventAction.ACTION_REF_DEVICE_SETTING -> {
                deviceSettingBean = JSON.parseObject(
                    SpUtils.getValue(
                        SpUtils.DEVICE_SETTING,
                        ""
                    ), DeviceSettingBean::class.java
                )
                loadViews(ControlBleTools.getInstance().isConnect, true)
            }

            EventAction.ACTION_REFRESH_BATTERY_INFO -> {
                val batteryInfo = msg.obj as RealTimeBean.DeviceBatteryInfo
                refreshBatteryInfo(
                    batteryInfo.capacity.trim().toInt(),
                    batteryInfo.chargeStatus.trim().toInt()
                )
            }

            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        com.blankj.utilcode.util.LogUtils.w(TAG, "device connected")
                        DeviceSettingLiveData.instance.connectTrackingLastLog = ""
                        if (!Global.IS_BIND_DEVICE) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("连接成功"))
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("使能").apply {
                                log = "使能完成：" + ErrorUtils.getErrorTypeConnectLog()
                            })
                            canRecordConnectBleOff = false
                        }
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                        //验证绑定状态---->显示连接设备中
//                        postDelay(1000){
                        bindDeviceByReConnect()
//                        }
                    }

                    BleCommonAttributes.STATE_CONNECTING -> {
                        com.blankj.utilcode.util.LogUtils.w(TAG, "device connecting")
                        binding.tvState.text = BaseApplication.mContext.getString(R.string.device_connecting)
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                        ErrorUtils.clearErrorConnect()
                        ErrorUtils.initType(ErrorUtils.ERROR_TYPE_FOR_CONNECT)

                        if (!Global.IS_BIND_DEVICE) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getStartTypeTrack("重连"), isStart = true)
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("发起连接").apply {
                                log = "connect() name:${SpUtils.getValue(SpUtils.DEVICE_NAME, "")},address:${SpUtils.getValue(SpUtils.DEVICE_MAC, "")}"
                            })
                            canRecordConnectBleOff = true
                        }
                    }

                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        com.blankj.utilcode.util.LogUtils.w(TAG, "device disconnect")
                        MicroManager.findPhone(1)
                        loadViews(false)
                        Global.IS_DEVICE_VERIFY = false
                        ThreadUtils.cancel(bindStateTimeOutTask)
                        if (AppUtils.isOpenBluetooth()) {
                            binding.layoutState.visibility = View.VISIBLE
                            binding.tvState.text = BaseApplication.mContext.getString(R.string.device_connecting)
                            val animation = binding.ivAnimation.background as AnimationDrawable
                            binding.ivState.setImageDrawable(animation)
                            animation.start()
                            binding.layoutBattery.visibility = View.GONE
                        }
                    }

                    BleCommonAttributes.STATE_TIME_OUT -> {
                        LogUtils.w(TAG, "device connect timeout")
                        /*binding.tvState.text = BaseApplication.mContext.getString(R.string.device_connected_failed)
                        binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_ref_connect))*/
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                        if (!Global.IS_BIND_DEVICE) {
                            SendCmdUtils.connectDevice(
                                SpUtils.getValue(SpUtils.DEVICE_NAME, ""),
                                SpUtils.getValue(SpUtils.DEVICE_MAC, ""), false
                            )
                        }
                    }
                }
            }

            EventAction.ACTION_REF_BIND_DEVICE -> {
                //refDeviceListUI()
                refreshDeviceView()
            }

            EventAction.ACTION_NETWORK_DISCONNECTED -> {
                binding.lyNoNetWork.visibility = View.VISIBLE
                binding.lyNoNetWork.setType(ViewForLayoutNoInternet.TYPE_NO_NETWORK)
                binding.lyNoDeviceBind.lyNoBind.visibility = View.GONE
                binding.lyDeviceBind.visibility = View.GONE
                binding.lyNoNetWork.refreshHead()
            }
        }
    }
    //endregion

    //region 设备信息回调
    /**
     * 设备信息回调
     */
    class MyDeviceInfoCallBack(fragment: DeviceFragment) : DeviceInfoCallBack {
        private var wrFragment: WeakReference<DeviceFragment>? = null

        init {
            wrFragment = WeakReference(fragment)
            if (wrFragment?.get() == null) {
                LogUtils.e("DeviceFragment", "DeviceFragment is Null")
            }
        }

        override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean) {
            LogUtils.e("DeviceFragment", "deviceInfoBean --->$deviceInfoBean", true)
            Global.deviceVersion = deviceInfoBean.firmwareVersion
            Global.deviceSn = deviceInfoBean.serialNumber

            val index = DeviceManager.dataList.indexOfFirst { it.deviceStatus == 1 }
            if (index != -1) {
                val version = DeviceManager.dataList[index].deviceVersion
                if (version != Global.deviceVersion) {
                    DeviceManager.dataList[index].deviceVersion = Global.deviceVersion
                    wrFragment?.get()?.viewmodel?.upLoadDeviceVersion(Global.deviceVersion)
                }
                LogUtils.e(
                    "DeviceFragment",
                    "firmwareVersion --->${deviceInfoBean.firmwareVersion}  version--->$version"
                )
            }
            if (SpUtils.getSPUtilsInstance()
                    .getString(SpUtils.DEVICE_VERSION, "") != Global.deviceVersion
            ) {
                SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_VERSION, Global.deviceVersion)
                wrFragment?.get()?.viewmodel?.versionInfo(Global.deviceType, Global.deviceVersion)
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
            //wrFragment?.get()?.refreshBatteryInfo(capacity, chargeStatus)
            Global.deviceCapacity = capacity
            val batteryInfo = RealTimeBean.DeviceBatteryInfo()
            batteryInfo.capacity = capacity.toString()
            batteryInfo.chargeStatus = chargeStatus.toString()
            EventBus.getDefault()
                .post(EventMessage(EventAction.ACTION_REFRESH_BATTERY_INFO, batteryInfo))
        }
    }

    /**
     * 刷新电池信息
     */
    private fun refreshBatteryInfo(capacity: Int, chargeStatus: Int) {
        binding.layoutState.visibility = View.GONE
        binding.layoutBattery.visibility = View.VISIBLE
        binding.battery.setPre(capacity / 100f, chargeStatus == 1)
        if (chargeStatus == 1) {
            binding.tvPower.text = getString(R.string.device_battery_charge_state)
        } else {
            binding.tvPower.text =
                "$capacity${getString(R.string.healthy_sports_item_percent_sign)}"
        }
        RefreshBatteryState.postValue(BatteryBean(capacity, chargeStatus))
    }
    //endregion

    //region 设备设置列表
    private fun loadViews(isEnabled: Boolean, isMustRef: Boolean = false) {
        if (this.context == null) return
        if (isEnabled == lastEnable && !isMustRef) {
            return
        }
        lastEnable = isEnabled
        binding.layoutDeviceSetList.visibility = View.VISIBLE
        binding.layoutDeviceSetList.removeAllViews()

        binding.tvName.text = getString(R.string.device_set_theme)

        val inflater = LayoutInflater.from(requireActivity())
        for (i in list.indices) {
            if (checkload(list[i].text)) {
                val constraintLayout = inflater.inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                image.background = ContextCompat.getDrawable(requireActivity(), list[i].img)
                tvName.text = list[i].text
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                constraintLayout.alpha = if (isEnabled) 1.0f else 0.5f
                constraintLayout.isEnabled = isEnabled
                setViewsClickListener({
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return@setViewsClickListener
                    }
                    when (list[i].text) {
                        getString(R.string.running_permission_title) -> {
                            startActivity(
                                Intent(
                                    activity,
                                    BackgroundPermissionMainActivity::class.java
                                )
                            )
                        }

                        getString(R.string.device_set_contacts) -> {
                            startActivity(Intent(activity, ContactsActivity::class.java))
                        }
                        //消息通知设置
                        getString(R.string.device_fragment_set_message) -> {
                            startActivity(Intent(activity, MsgNotifySetActivity::class.java))
                        }

                        getString(R.string.device_fragment_set_remind) -> {
                            startActivity(Intent(activity, RemindSetActivity::class.java))
                        }

                        getString(R.string.device_set_heart) -> {
                            startActivity(Intent(activity, HeartRateSettingActivity::class.java))
                        }

                        getString(R.string.device_set_pressure) -> {
                            startActivity(Intent(activity, StressSetActivity::class.java))
                        }

                        getString(R.string.device_fragment_set_sleep) -> {
                            startActivity(Intent(activity, SleepSettingActivity::class.java))
                        }

                        getString(R.string.device_set_weather) -> {
                            startActivity(Intent(activity, WeatherActivity::class.java))
                        }

                        getString(R.string.device_set_take_picture) -> {
                            gotoCameraActivity()
                            AppTrackingManager.saveOnlyBehaviorTracking("6", "7")
                        }
                    }
                }, constraintLayout)
                binding.layoutDeviceSetList.addView(constraintLayout)
                if (i == (list.size - 1)) {
                    viewLine01.visibility = View.GONE
                } else {
                    viewLine01.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkload(text: String): Boolean {
        if (text == getString(R.string.running_permission_title)) {
            return true
        }
        //后台配置的
        if (deviceSettingBean != null) {
            when (text) {
                getString(R.string.device_fragment_set_theme_center) -> {
                    return deviceSettingBean!!.functionRelated.dial
                }

                getString(R.string.device_set_contacts) -> {
                    return deviceSettingBean!!.functionRelated.contacts
                }
                //消息通知设置
                getString(R.string.device_fragment_set_message) -> {
                    return deviceSettingBean!!.reminderRelated.notification
                }

                getString(R.string.device_fragment_set_remind) -> {
                    return deviceSettingBean!!.reminderRelated.alarm_clock ||
                            deviceSettingBean!!.reminderRelated.sedentary ||
                            deviceSettingBean!!.reminderRelated.drink_water ||
                            deviceSettingBean!!.reminderRelated.hand_washing_reminder ||
                            deviceSettingBean!!.reminderRelated.reminder_to_take_medicine ||
                            deviceSettingBean!!.reminderRelated.event_reminder
                }

                getString(R.string.device_set_heart) -> {
                    return deviceSettingBean!!.settingsRelated.continuous_heart_rate_switch
                            || deviceSettingBean!!.reminderRelated.heart_rate_warning
                }

                getString(R.string.device_set_pressure) -> {
                    return deviceSettingBean!!.dataRelated.continuous_pressure
                }

                getString(R.string.device_fragment_set_sleep) -> {
                    return deviceSettingBean!!.settingsRelated.sleep_rapid_eye_movement_switch
                }

                getString(R.string.device_set_weather) -> {
                    return deviceSettingBean!!.functionRelated.weather
                }

                getString(R.string.device_set_take_picture) -> {
                    return deviceSettingBean!!.functionRelated.shake_and_shake_to_take_pictures
                }
            }
        }
        return false
    }
    //endregion

    //region 刷新设备列表
    /**
     * 刷新设备列表
     * */
    private fun refreshDeviceView() {
        //加载中界面
        binding.lyNoDeviceBind.lyNoBind.visibility = View.GONE
        binding.lyDeviceBind.visibility = View.GONE
        binding.lyNoNetWork.visibility = View.VISIBLE
        binding.lyNoNetWork.setType(ViewForLayoutNoInternet.TYPE_REF)
        mLastRefreshTime = System.currentTimeMillis()
        viewmodel.getBindList()
    }

    /**
     * 刷新设备列表结果状态码观察者
     */
    inner class MyBindListCodeObserver : Observer<String> {
        override fun onChanged(it: String?) {
            if (TextUtils.isEmpty(it)) return
            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    refDeviceListUI()
                    viewmodel.getHomeByProductList()
                    if (DeviceManager.dataList.size > 0) {
                        for (i in DeviceManager.dataList.indices) {
                            if (DeviceManager.dataList[i].deviceStatus == 1) {
                                val deviceName = DeviceManager.dataList[i].deviceName
                                val deviceMac = DeviceManager.dataList[i].deviceMac
                                //启用新设备连接错误问题
                                if (/*TODO 设备名称与服务器不一致 导致绑定设备后重连 deviceName != SpUtils.getValue(SpUtils.DEVICE_NAME, "")|| */
                                    deviceMac != SpUtils.getValue(SpUtils.DEVICE_MAC, "")
                                ) {
                                    SpUtils.setValue(SpUtils.DEVICE_NAME, deviceName)
                                    SpUtils.setValue(SpUtils.DEVICE_MAC, deviceMac)
                                    ControlBleTools.getInstance().disconnect()
                                }
                                if (AppUtils.isOpenBluetooth()) {
                                    if (!ControlBleTools.getInstance().isConnect
                                        && !Global.IS_BIND_DEVICE
                                        && !Global.IS_ENABLE_DEVICE
                                    ) {
                                        SendCmdUtils.connectDevice(
                                            deviceName,
                                            deviceMac.uppercase()
                                        )
                                    } else {
                                        //请求设备绑定连接状态
                                        bindDeviceByReConnect()
                                    }
                                }
                            }
                        }
                    } else {
                        SpUtils.setValue(SpUtils.DEVICE_NAME, "")
                        SpUtils.setValue(SpUtils.DEVICE_MAC, "")
                        ControlBleTools.getInstance().disconnect()
                    }
                }

                HttpCommonAttributes.SERVER_ERROR -> {
                    showNoHaveDevice() //用于HealthyFragment刷新设备状态
                    binding.lyNoNetWork.visibility = View.VISIBLE
                    binding.lyNoNetWork.setType(ViewForLayoutNoInternet.TYPE_NO_NETWORK)
                    binding.lyNoDeviceBind.lyNoBind.visibility = View.GONE
                    binding.lyDeviceBind.visibility = View.GONE
                    binding.lyNoNetWork.refreshHead()
                }

                HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                    showNoHaveDevice()
                }
            }
        }
    }

    /**
     * 无网络重试点击
     */
    inner class MyNotNetRetryListener : ViewForLayoutNoInternet.OnRetryListener {
        override fun onOnRetry() {
            refreshDeviceView()
        }
    }

    /**
     * 刷新设备列表界面
     */
    fun refDeviceListUI() {
        if (this.context == null) return
        if (DeviceManager.dataList.size > 0) {
            if (Math.abs(System.currentTimeMillis() - mLastRefreshTime) < 1000L) {
                //请求刷新列表时间过短，延时一秒加载页面，防止页面跳闪
                ThreadUtils.runOnUiThreadDelayed({
                    refDeviceListUI()
                }, 1000)
                return
            }
            binding.lyNoDeviceBind.lyNoBind.visibility = View.GONE
            binding.lyNoNetWork.visibility = View.GONE
            binding.lyDeviceBind.visibility = View.VISIBLE
            for (i in DeviceManager.dataList.indices) {
                val deviceName = DeviceManager.dataList[i].deviceName
                if (DeviceManager.dataList[i].deviceStatus == 1) {
                    binding.tvDeviceName.text = AppUtils.biDiFormatterStr(DeviceManager.dataList[i].deviceName)
                    //region 设备产品图
                    var isSetIcon = false
                    for (j in Global.productList.indices) {
                        if (TextUtils.equals(
                                DeviceManager.dataList[i].deviceType.toString(),
                                Global.productList[j].deviceType
                            )
                        ) {
                            if (TextUtils.isEmpty(Global.productList[j].homeLogo)) {
                                GlideApp.with(requireActivity()).load(R.mipmap.device_no_bind_right_img).into(binding.ivIcon)
                            } else {
                                GlideApp.with(requireActivity()).load(Global.productList[j].homeLogo).into(binding.ivIcon)
                            }
                            EventBus.getDefault()
                                .post(EventMessage(EventAction.ACTION_REFRESH_HEALTHY_PAGE_DEVICE_ICON))
                            isSetIcon = true
                            break
                        }
                    }
                    if (!isSetIcon) {
                        GlideApp.with(requireActivity()).load(R.mipmap.device_no_bind_right_img).into(binding.ivIcon)
                        //移除缓存的设备产品图
                        AppUtils.tryBlock {
                            FileUtils.delete(Global.DEVICE_ICON_PATH)
                        }
                    }
                    //endregion
                }
//                setViewsClickListener({
//                    if (DeviceManager.dataList.isNullOrEmpty()) return@setViewsClickListener
//                    if (i < DeviceManager.dataList.size) {
//                        if (DeviceManager.dataList[i].deviceStatus == 1) {
//                            val intent = Intent(this.context, DeviceSetActivity::class.java)
//                            intent.putExtra("name", DeviceManager.dataList[i].deviceName)
//                            intent.putExtra("type", "${DeviceManager.dataList[i].deviceType}")
//                            intent.putExtra("mac", DeviceManager.dataList[i].deviceMac)
//                            startActivityForResult(intent, HomeActivity.UNBIND_REQUEST_CODE)
//                        } else {
//                            // 跳转非启用界面
//                            val intent = Intent(this.context, NotEnabledActivity::class.java)
//                            intent.putExtra("oldDevice", binding.tvDeviceName.text.toString().trim())
//                            intent.putExtra("list", DeviceManager.dataList[i])
//                            startActivityForResult(intent, HomeActivity.UNBIND_REQUEST_CODE)
//                        }
//                    }
//                }, constraintLayout)
//                binding.layoutDeviceList.addView(constraintLayout)
            }
        } else {
            showNoHaveDevice()
        }
    }

    /**
     * 展示暂无设备
     */
    fun showNoHaveDevice() {
        SpUtils.setValue(SpUtils.DEVICE_NAME, "")
        SpUtils.setValue(SpUtils.DEVICE_MAC, "")
        ControlBleTools.getInstance().disconnect()

        binding.lyNoDeviceBind.lyNoBind.visibility = View.VISIBLE
        binding.lyDeviceBind.visibility = View.GONE
        binding.lyNoNetWork.visibility = View.GONE
        if (DeviceManager.dataList != null) {
            DeviceManager.dataList.clear()
//            binding.layoutDeviceList.removeAllViews()
        }
        refreshHead()
        var txt = getString(R.string.device_welcome_user)
        binding.lyNoDeviceBind.tvUserName.text = txt
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_NO_DEVICE_BINDING))
    }
    //endregion

    private fun refreshHead() {
        if (::mUserBean.isInitialized) {
            if (!TextUtils.isEmpty(mUserBean.head)) {
                GlideApp.with(requireActivity()).load(mUserBean.head)
                    .error(R.mipmap.ic_mine_avatar)
                    .placeholder(R.mipmap.ic_mine_avatar)
                    .into(binding.lyNoDeviceBind.ivUserAvatar)
            } else {
                GlideApp.with(requireActivity()).load(R.mipmap.ic_mine_avatar)
                    .error(R.mipmap.ic_mine_avatar)
                    .placeholder(R.mipmap.ic_mine_avatar)
                    .into(binding.lyNoDeviceBind.ivUserAvatar)
            }
        }
    }

    //region 连接设备验证设备有效性
    private var bindStateTrackingLog: TrackingLog? = null
    private fun bindDeviceByReConnect() {
        if (Global.IS_BIND_DEVICE) {
            //绑定设备时不在此验证设备绑定状态
            return
        }
        bindStateTrackingLog = TrackingLog.getDevTyepTrack("检测绑定状态", "请求当前连接设备的绑定状态", "INQUIRY_BINDING_STATUS")
        myFRequestDeviceBindStateCallBack = MyFRequestDeviceBindStateCallBack(this)
        ThreadUtils.cancel(bindStateTimeOutTask)
        bindStateTimeOutTask = BindStateTimeOutTask()
        ThreadUtils.executeByIo(bindStateTimeOutTask)
        CallBackUtils.requestDeviceBindStateCallBack = myFRequestDeviceBindStateCallBack
        ControlBleTools.getInstance().requestDeviceBindState(object : SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                com.blankj.utilcode.util.LogUtils.d("requestDeviceBindState state == $state")
                bindStateTrackingLog?.let {
                    it.endTime = TrackingLog.getNowString()
                    it.log = "state : $state"
                    if (state != SendCmdState.SUCCEED && state != SendCmdState.UNINITIALIZED) {
                        showConnectFail()
                        com.blankj.utilcode.util.LogUtils.d("showVerifyUserIdTimeoutDialog")
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, it.apply { log += "\n获取检测绑定状态失败/超时" }, "1315", true)
                    } else {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, it)
                    }
                }

            }
        })
    }

    /**
     * 设备绑定状态回调
     */
    class MyFRequestDeviceBindStateCallBack(fragment: DeviceFragment) :
        RequestDeviceBindStateCallBack {
        private var wrFragment: WeakReference<DeviceFragment>? = null
        private val TAG = "DeviceFragment"

        init {
            wrFragment = WeakReference(fragment)
            if (wrFragment?.get() == null) {
                LogUtils.e("DeviceFragment", "DeviceFragment is Null")
            }
        }

        override fun onBindState(state: Boolean) {
            if (Global.IS_BIND_DEVICE) {
                //绑定设备时不在此验证设备绑定状态
                return
            }
            LogUtils.e(TAG, "连接校验 是否绑定 --> $state")
            wrFragment?.get()?.apply {
                this.bindStateTrackingLog?.devResult = "连接校验 是否绑定 --> $state"
            }
            ThreadUtils.runOnUiThread {
                if (state) {
                    val verifyTrackingLog = TrackingLog.getDevTyepTrack("检测用户是否id一致", "验证设备的绑定用户id", "VERIFY_USER_NUMBER")
                    //校验重连
                    CallBackUtils.verifyUserIdCallBack = object : VerifyUserIdCallBack {
                        override fun onVerifyState(state: Int) {
                            LogUtils.e(TAG, "当前用户ID与设备用户ID是否一致 -->${state == 0}")
                            verifyTrackingLog.devResult = "当前用户ID与设备用户ID是否一致 -->${state == 0}"
                            wrFragment?.get()?.apply {
                                if (state != 0) {
                                    LogUtils.e(TAG, "连接校验失败,用户id不一致", true)
                                    Global.IS_DEVICE_VERIFY = false
                                    ThreadUtils.cancel(bindStateTimeOutTask)
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, verifyTrackingLog.apply { log += "\n设备被其他用户绑定" }, "1316", true)
                                    showConnectFail()
                                    showConnectFailDialog()
                                } else {
                                    LogUtils.e(TAG, "连接校验成功", true)
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getEndTypeTrack("重连"), isEnd = true)
                                    Global.IS_DEVICE_VERIFY = true
                                    ThreadUtils.cancel(bindStateTimeOutTask)
                                    ControlBleTools.getInstance().setTime(
                                        System.currentTimeMillis(),
                                        SpUtils.getSPUtilsInstance()
                                            .getBoolean(SpUtils.DEVICE_TIME_IS12, false), null
                                    )
                                    ControlBleTools.getInstance().getDeviceInfo(null)
                                    postDelay(150) {
                                        EventBus.getDefault()
                                            .post(EventMessage(EventAction.ACTION_DEVICE_CONNECTED))
                                    }
                                    //wrFragment?.get()?.startSyncAnimation()

                                    /*binding.tvState.text = BaseApplication.mContext.getString(R.string.device_connected)
                                    binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.un_bind_success))
                                    ThreadUtils.runOnUiThreadDelayed({
                                        binding.layoutState.visibility = View.GONE
                                    },1000)*/
                                    binding.layoutState.visibility = View.GONE
                                    /*if (!Global.IS_SYNCING_DATA) {
                                        loadViews(true)
                                    }*/
                                    loadViews(true)
                                    //检测br配对
                                    var bleMac = SpUtils.getValue(SpUtils.DEVICE_MAC, "")
                                    if (bleMac.isEmpty()) {
                                        SpUtils.setValue(
                                            SpUtils.DEVICE_NAME,
                                            ControlBleTools.getInstance().currentDeviceName
                                        )
                                        bleMac = ControlBleTools.getInstance().currentDeviceMac
                                        SpUtils.setValue(SpUtils.DEVICE_MAC, bleMac)
                                        SpUtils.saveHeadsetMac(bleMac, bleMac)
                                    }
                                    EventBus.getDefault()
                                        .post(EventMessage(EventAction.ACTION_HEADSET_BOND, bleMac))
                                }
                            }
                        }
                    }
                    ControlBleTools.getInstance().verifyUserId(SpUtils.getValue(SpUtils.USER_ID, ""), object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState) {
                            verifyTrackingLog.endTime = TrackingLog.getNowString()
                            verifyTrackingLog.log = "state : $state"
                            if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) {
                                wrFragment?.get()?.apply {
                                    LogUtils.e(TAG, "连接校验失败,验证用户id失败", true)
                                    Global.IS_DEVICE_VERIFY = false
                                    ThreadUtils.cancel(wrFragment?.get()?.bindStateTimeOutTask)
                                    showConnectFail()
                                    com.blankj.utilcode.util.LogUtils.d("showVerifyUserIdTimeoutDialog")
                                    showVerifyUserIdTimeoutDialog()
                                }
                                AppTrackingManager.trackingModule(
                                    AppTrackingManager.MODULE_RECONNECT,
                                    verifyTrackingLog.apply { log += "检测用户是否id一致失败/超时" },
                                    "1316",
                                    true
                                )
                            } else {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, verifyTrackingLog)
                            }
                        }
                    })
                } else {
                    // 此设备被恢复出厂
                    Global.IS_DEVICE_VERIFY = false
                    ThreadUtils.cancel(wrFragment?.get()?.bindStateTimeOutTask)
                    LogUtils.e(TAG, "此设备被恢复出厂", true)
                    wrFragment?.get()?.showConnectFail()
                    wrFragment?.get()?.showConnectFailDialog()
                }
            }
        }
    }


    //处理发送请求绑定状态时，系统休眠导致指令丢失，发送指令超时失效，造成设备已连接，app显示一直连接中
    //如果检验设备绑定状态30s超时 设备未校验通过，且设备已连接，重新请求绑定状态
    private var bindStateTimeOutTask: BindStateTimeOutTask? = null

    private inner class BindStateTimeOutTask : ThreadUtils.SimpleTask<Boolean>() {
        override fun doInBackground(): Boolean {
            var i = 0
            while (i < 30) {
                i++
                com.blankj.utilcode.util.LogUtils.d("BindStateTimeOutTask run :$i")
                Thread.sleep(1000)
            }
            return true
        }

        override fun onSuccess(result: Boolean?) {
            if (!Global.IS_DEVICE_VERIFY && ControlBleTools.getInstance().isConnect) {
                //重新请求绑定状态
                bindDeviceByReConnect()
            }
        }

    }
    //endregion

    //region 摇摇拍照
    /**
     * 进入摇摇拍照页面
     * */
    private fun gotoCameraActivity() {
        PermissionUtils.checkRequestPermissions(
            this.lifecycle,
            getString(R.string.permission_camera),
            PermissionUtils.PERMISSION_GROUP_CAMERA
        ) {
            ControlBleTools.getInstance().sendPhonePhotogragh(
                0,
                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        if (state == SendCmdState.SUCCEED) {
                            //相机工具初始化
                            CameraUtil.initialize(activity)
                            startActivity(Intent(activity, CameraActivity::class.java))
                        }
                        ToastUtils.showSendCmdStateTips(state)
                    }
                })
        }
    }
    //endregion

    //region 同步刷新数据状态
    /*    fun startSyncAnimation() {
            binding.lySyncing.visibility = View.VISIBLE
            val animationDrawable = binding.ivSync.background as AnimationDrawable
            binding.ivSyncState.setImageDrawable(animationDrawable)
            animationDrawable.start()
            binding.tvSync.text = getString(R.string.healthy_sports_sync_tips)
            loadViews(false)
        }

        fun stopSyncAnimation(state:Int) {
            loadViews(ControlBleTools.getInstance().isConnect)
            if(state == 0){ //SNYC_SUCCESS = 0
                binding.ivSyncState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.un_bind_success))
                binding.tvSync.text = getString(R.string.sync_success_tips)
                ThreadUtils.runOnUiThreadDelayed({
                    binding.lySyncing.visibility = View.GONE
                },1000)
            }else{ // SNYC_FAIL | TIME_OUT
                binding.ivSyncState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_ref_connect))
                binding.tvSync.text = getString(R.string.sync_fail_tips)
            }
        }*/
    //endregion

    //region 设备连接失败
    fun showConnectFail() {
        ControlBleTools.getInstance().disconnect()
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_DEVICE_CONNECT_FAIL))
        binding.tvState.text = BaseApplication.mContext.getString(R.string.device_connected_failed)
        binding.ivState.setImageDrawable(
            ContextCompat.getDrawable(
                BaseApplication.mContext,
                R.mipmap.icon_ref_connect
            )
        )
    }

    private fun showConnectFailDialog() {
        if (ActivityUtils.getTopActivity() !is UnBindDeviceActivity) {
            DialogUtils.showDialogTitleAndOneButton(
                ActivityUtils.getTopActivity(),
                "",
                getString(R.string.connect_fail_tips),
                getString(R.string.dialog_confirm_btn),
                null
            )
        }
    }

    private fun showVerifyUserIdTimeoutDialog() {
        if (ActivityUtils.getTopActivity() !is UnBindDeviceActivity) {
            DialogUtils.showDialogTitleAndOneButton(
                ActivityUtils.getTopActivity(), "",
                getString(R.string.connect_timeout_tip),
                getString(R.string.dialog_confirm_btn), null
            )
        }
    }
    //endregion

    //region onActivityResult 设备解绑页面结果回调
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            HomeActivity.UNBIND_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    refreshDeviceView()
                }
            }
        }
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        myDeviceInfoCallBack = null
        myFRequestDeviceBindStateCallBack = null
        EventBus.getDefault().unregister(this)
    }
}