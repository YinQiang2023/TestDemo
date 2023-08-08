package com.smartwear.xzfit.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.databinding.DeviceSetActivityBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.device.devicecontrol.UnBindDeviceActivity
import com.smartwear.xzfit.ui.device.wristbright.WristBrightActivity
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.RealTimeBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.ui.device.bean.DeviceSettingBean
import com.smartwear.xzfit.ui.device.setting.more.ApplicationSortActivity
import com.smartwear.xzfit.ui.device.setting.more.DoNotDisturbActivity
import com.smartwear.xzfit.ui.device.setting.more.ScreenDisplayActivity
import com.smartwear.xzfit.ui.device.setting.more.ShortReplyActivity
import com.smartwear.xzfit.ui.device.setting.sportmode.SportsModeActivity
import com.smartwear.xzfit.ui.device.setting.worldclock.WorldClockActivity
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.livedata.RefreshBatteryState
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.LogUtils
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class DeviceSetActivity : BaseActivity<DeviceSetActivityBinding, DeviceModel>(DeviceSetActivityBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG: String = DeviceSetActivity::class.java.simpleName

    private lateinit var loadDialog: Dialog

    //确认解绑弹窗
    private var dialog: Dialog? = null

    private var mDeviceName = ""
    private var mDeviceType = ""
    private var mDeviceMac = ""

    //产品功能列表
    private val deviceSettingBean by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    //获取设备省电设置监听
    private var mPowerSavingObserver: Observer<Boolean>? = null

    //最后一次加载设置列表布局是否启用
    private var lastEnable: Boolean? = null

    //设备列表items
    private val list: MutableList<Bean> = ArrayList()

    class Bean {
        var img: Int = 0
        var text: String = ""
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_title)
        EventBus.getDefault().register(this)

        mDeviceName = intent.getStringExtra("name") ?: ""
        mDeviceType = intent.getStringExtra("type") ?: ""
        mDeviceMac = intent.getStringExtra("mac") ?: ""

        loadDialog = DialogUtils.showLoad(this)
        binding.tvDeviceName.text = mDeviceName
        refreshBatteryInfo(0, 0)

        if (AppUtils.isOpenBluetooth()) {
            binding.tvCloseBt.visibility = View.GONE
            binding.layoutState.visibility = View.VISIBLE
        } else {
            binding.tvCloseBt.visibility = View.VISIBLE
            binding.layoutState.visibility = View.GONE
            binding.layoutBattery.visibility = View.GONE
        }

        if (ControlBleTools.getInstance().isConnect || ControlBleTools.getInstance().isConnecting) {
            if (Global.IS_DEVICE_VERIFY) {
                /*binding.tvState.text = resources.getString(R.string.device_connected)
                binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.un_bind_success))
                ThreadUtils.runOnUiThreadDelayed({
                    binding.layoutState.visibility = View.GONE
                },1000)*/
                binding.layoutState.visibility = View.GONE
                ControlBleTools.getInstance().getDeviceInfo(null)
            } else {
                binding.tvState.text = resources.getString(R.string.device_connecting)
                val animation = binding.ivAnimation.background as AnimationDrawable
                binding.ivState.setImageDrawable(animation)
                animation.start()
                binding.layoutBattery.visibility = View.GONE
            }
        } else {
            binding.tvState.text = resources.getString(R.string.device_connected_failed)
            binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_ref_connect))
            binding.layoutBattery.visibility = View.GONE
        }

        loadViews(ControlBleTools.getInstance().isConnect && Global.IS_DEVICE_VERIFY)


        for (j in Global.productList.indices) {
            if (TextUtils.equals(mDeviceType, Global.productList[j].deviceType)) {
                if (TextUtils.isEmpty(Global.productList[j].homeLogo)) {
                    GlideApp.with(this).load(R.mipmap.device_no_bind_right_img).into(binding.ivIcon)
                } else {
                    GlideApp.with(this).load(Global.productList[j].homeLogo).into(binding.ivIcon)
                }
            }
        }

        setViewsClickListener(this, binding.btUnbind, binding.layoutState, binding.lySyncing)
    }

    override fun initData() {
        super.initData()
    }

    override fun onResume() {
        super.onResume()
        RealTimeRefreshDataUtils.closeRealTime()
    }

    override fun onPause() {
        super.onPause()
        RealTimeRefreshDataUtils.openRealTime()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btUnbind.id -> {
                startActivity(
                    Intent(this@DeviceSetActivity, UnBindDeviceActivity::class.java)
                        .putExtra("type", mDeviceType)
                        .putExtra("mac", mDeviceMac)
                        .putExtra("isEnable", true)
                        .putExtra("clazz", this@DeviceSetActivity::class.java)
                )
            }
            binding.layoutState.id -> {
                if (binding.tvState.text.toString().trim() == getString(R.string.device_connected_failed)) {
                    ControlBleTools.getInstance().disconnect()
                    SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                }
            }
            binding.lySyncing.id -> {
                if (binding.tvSync.text.toString().trim() == getString(R.string.sync_fail_tips)) {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_REF_SYNC))
                }
            }
        }
    }

    //region 设备设置列表
    @SuppressLint("ResourceType")
    fun loadViews(isEnabled: Boolean) {
        if (isEnabled == lastEnable) {
            return
        }
        lastEnable = isEnabled
        list.clear()
        var texts = resources.getStringArray(R.array.deviceSetString2List)
        var imgs = resources.obtainTypedArray(R.array.deviceSetImage2List)
        for (i in 0 until imgs.length()) {
            val bean = Bean()
            bean.img = imgs.getResourceId(i, 0)
            bean.text = texts[i]
            list.add(bean)
        }
        imgs.recycle()
        //加载设置
        if (deviceSettingBean == null) {
            val temp = list.get(list.size - 1)
            list.clear()
            list.add(temp)
        }
        binding.layoutDeviceSetList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (i in list.indices) {
            if (checkLoad(list[i].text)) {
                val constraintLayout = inflater.inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val ivNext = constraintLayout.findViewById<ImageView>(R.id.ivNext)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                val mSwitchCompat = constraintLayout.findViewById<SwitchCompat>(R.id.mSwitchCompat)
                image.background = ContextCompat.getDrawable(this, list[i].img)
                tvName.text = list[i].text
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                //tvName.setTextColor(ContextCompat.getColorStateList(this,R.drawable.text_color_enabled_sl))
                //tvName.isEnabled = isEnabled
                constraintLayout.alpha = if (isEnabled) 1.0f else 0.5f
                constraintLayout.isEnabled = isEnabled
                mSwitchCompat.isEnabled = isEnabled

                if (list[i].text == getString(R.string.device_set_find_device) || list[i].text == getString(
                        R.string.device_set_take_picture
                    )
                ) {
                    ivNext.visibility = View.INVISIBLE
                }

                //region 省电设置
                if (list[i].text == getString(R.string.dev_more_set_power_saving)) {
                    tvName.setPadding(ConvertUtils.dp2px(10f), 0, ConvertUtils.dp2px(50f), 0)
                    mSwitchCompat.visibility = View.VISIBLE
                    ivNext.visibility = View.GONE
                    if (mPowerSavingObserver != null) {
                        viewModel.deviceSettingLiveData.getPowerSaving().removeObserver(mPowerSavingObserver!!)
                    }
                    mPowerSavingObserver = Observer {
                        if (it != null) {
                            com.blankj.utilcode.util.LogUtils.d("省电设置 ->$it")
                            mSwitchCompat.isChecked = it
                            SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_POWER_SAVING, it)
                        }
                    }
                    viewModel.deviceSettingLiveData.getPowerSaving().observe(this, mPowerSavingObserver!!)
                    if (isEnabled && ControlBleTools.getInstance().isConnect && Global.IS_DEVICE_VERIFY) {
                        //查状态
                        ControlBleTools.getInstance().getPowerSaving(null)
                    }

                    mSwitchCompat.setOnClickListener {
                        mSwitchCompat.isChecked = !mSwitchCompat.isChecked
                        if (!ControlBleTools.getInstance().isConnect) {
                            ToastUtils.showToast(R.string.device_no_connection)
                            return@setOnClickListener
                        }

                        if (mSwitchCompat.isChecked) {
                            loadDialog.show()
                            ControlBleTools.getInstance().setPowerSaving(false,
                                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                    override fun onState(state: SendCmdState) {
                                        DialogUtils.dismissDialog(loadDialog)
                                        ToastUtils.showSendCmdStateTips(state)
                                        ControlBleTools.getInstance().getPowerSaving(null)
                                    }
                                })
                        } else {
                            showPowerSavingTipsDialog()
                        }
                    }
                }
                //endregion

                var a = 0
                setViewsClickListener({
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return@setViewsClickListener
                    }
                    when (list[i].text) {
                        getString(R.string.device_set_wrist_bright) -> {
                            startActivity(Intent(this, WristBrightActivity::class.java))
                        }
                        getString(R.string.dev_more_set_not_disturb) -> {
                            startActivity(Intent(this, DoNotDisturbActivity::class.java))
                        }
                        getString(R.string.dev_more_set_app_sort) -> {
                            startActivity(Intent(this, ApplicationSortActivity::class.java))
                        }
                        getString(R.string.dev_more_set_card_sort) -> {
                            startActivity(
                                Intent(this, ApplicationSortActivity::class.java)
                                    .putExtra("type", 1)
                            )
                        }
                        getString(R.string.dev_more_set_screen_display) -> {
                            startActivity(Intent(this, ScreenDisplayActivity::class.java))
                        }
                        getString(R.string.dev_more_set_short_reply) -> {
                            startActivity(Intent(this, ShortReplyActivity::class.java))
                        }
                        getString(R.string.device_set_world_clock) ->{
                            startActivity(Intent(this, WorldClockActivity::class.java))
                        }
                        getString(R.string.device_set_find_device) -> {
                            ControlBleTools.getInstance()
                                .sendFindWear(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                    override fun onState(state: SendCmdState) {
                                        if(state == SendCmdState.SUCCEED){
                                            AppTrackingManager.saveOnlyBehaviorTracking("7","17")
                                        }
                                        ToastUtils.showSendCmdStateTips(state)
                                    }
                                })
                        }
                        getString(R.string.device_set_sports_selection) -> {
                            startActivity(Intent(this, SportsModeActivity::class.java))
                        }
                        getString(R.string.device_set_take_more) -> {
                            startActivity(Intent(this, DevMoreSetActivity::class.java))
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
    private fun checkLoad(text: String): Boolean {
        if (text == getString(R.string.device_set_take_more)) {
            return true
        }
        //后台配置的
        if (deviceSettingBean != null) {
            when (text) {
                getString(R.string.dev_more_set_power_saving) -> {
                    return deviceSettingBean.functionRelated.power_saving_mode
                }
                getString(R.string.device_set_wrist_bright) -> {
                    return deviceSettingBean.settingsRelated.raise_your_wrist_to_brighten_the_screen
                }
                getString(R.string.dev_more_set_not_disturb) -> {
                    return deviceSettingBean.settingsRelated.do_not_disturb
                }
                getString(R.string.dev_more_set_app_sort) -> {
                    return deviceSettingBean.settingsRelated.application_list_sorting
                }
                getString(R.string.dev_more_set_card_sort) -> {
                    return deviceSettingBean.settingsRelated.card_sort_list
                }
                getString(R.string.dev_more_set_screen_display) -> {
                    return deviceSettingBean.settingsRelated.off_screen_display
                }
                getString(R.string.dev_more_set_short_reply) -> {
                    return deviceSettingBean.reminderRelated.quick_reply
                }
                getString(R.string.device_set_world_clock) ->{
                    return deviceSettingBean.settingsRelated.world_clock
                }
                getString(R.string.device_set_find_device) -> {
                    return deviceSettingBean.functionRelated.find_device
                }
                getString(R.string.device_set_sports_selection) -> {
                    return deviceSettingBean.settingsRelated.multi_sport_sorting
                }
            }
        }
        return false
    }
    //endregion

    //region 设置设置提醒
    private fun showPowerSavingTipsDialog() {
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.power_saving_tips),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    ControlBleTools.getInstance().setPowerSaving(true,
                        object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(state: SendCmdState) {
                                DialogUtils.dismissDialog(loadDialog)
                                ToastUtils.showSendCmdStateTips(state)
                                ControlBleTools.getInstance().getPowerSaving(null)
                                AppTrackingManager.saveOnlyBehaviorTracking("7","14")
                            }
                        })
                }

                override fun OnCancel() {
                }
            })
    }
    //endregion

    //region EventBus
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                if (msg.arg == BluetoothAdapter.STATE_ON) {
                    binding.tvCloseBt.visibility = View.GONE
                    binding.layoutState.visibility = View.VISIBLE
                    binding.tvState.text = resources.getString(R.string.device_no_connection)
                    binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_device_not_connected))
                } else if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    binding.tvCloseBt.visibility = View.VISIBLE
                    binding.layoutState.visibility = View.GONE
                    binding.layoutBattery.visibility = View.GONE
                }
            }
            EventAction.ACTION_REFRESH_BATTERY_INFO -> {
                val batteryInfo = msg.obj as RealTimeBean.DeviceBatteryInfo
                refreshBatteryInfo(batteryInfo.capacity.trim().toInt(), batteryInfo.chargeStatus.trim().toInt())
            }
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        LogUtils.w(TAG, "device connected")
                        //验证绑定状态---->显示连接设备中
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                    }
                    BleCommonAttributes.STATE_CONNECTING -> {
                        LogUtils.w(TAG, "device connecting")
                        binding.tvState.text = resources.getString(R.string.device_connecting)
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        LogUtils.w(TAG, "device distonnect")
                        loadViews(false)
                        if (AppUtils.isOpenBluetooth()) {
                            binding.layoutState.visibility = View.VISIBLE
                            binding.tvState.text = resources.getString(R.string.device_connecting)
                            val animation = binding.ivAnimation.background as AnimationDrawable
                            binding.ivState.setImageDrawable(animation)
                            animation.start()
                            binding.layoutBattery.visibility = View.GONE
                        }
                    }
                    BleCommonAttributes.STATE_TIME_OUT -> {
                        LogUtils.w(TAG, "STATE_TIME_OUT")
                        val animation = binding.ivAnimation.background as AnimationDrawable
                        binding.ivState.setImageDrawable(animation)
                        animation.start()
                        binding.layoutBattery.visibility = View.GONE
                        loadViews(false)
                    }
                }
            }
            EventAction.ACTION_DEVICE_CONNECTED -> {
                binding.layoutState.visibility = View.GONE
                binding.layoutBattery.visibility = View.VISIBLE
                loadViews(true)
            }
            EventAction.ACTION_DEVICE_CONNECT_FAIL -> {
                binding.tvState.text = resources.getString(R.string.device_connected_failed)
                binding.ivState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_ref_connect))

            }
        }
    }
    //endregion

    //region 刷新电池信息
    private fun refreshBatteryInfo(capacity: Int, chargeStatus: Int) {
        binding.battery.setPre(capacity / 100f, chargeStatus == 1)
        var capacityData = capacity
        RefreshBatteryState.observe(this, androidx.lifecycle.Observer {
            if (it == null) return@Observer
            if (it.chargeStatus == 1) {
                binding.tvPower.text = getString(R.string.device_battery_charge_state)
            } else {
                capacityData = it.capacity
                binding.tvPower.text = "${capacityData}${getString(R.string.healthy_sports_item_percent_sign)}"
            }
        })
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }


}