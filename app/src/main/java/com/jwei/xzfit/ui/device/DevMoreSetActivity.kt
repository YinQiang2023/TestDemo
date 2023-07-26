package com.jwei.xzfit.ui.device

import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.BleCommonAttributes
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityRemindSetBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.view.wheelview.NumberPicker
import com.jwei.xzfit.view.wheelview.OptionPicker
import com.jwei.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.bean.SimpleSettingSummaryBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.dialog.DownloadDialog
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.ui.GlobalEventManager
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean
import com.jwei.xzfit.ui.device.setting.more.*
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Android on 2021/10/12.
 * 更多设置
 */
class DevMoreSetActivity : BaseActivity<ActivityRemindSetBinding, DeviceModel>(ActivityRemindSetBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG = DevMoreSetActivity::class.java.simpleName

    private lateinit var loadDialog: Dialog
    private var simpleSettingSummaryBean: SimpleSettingSummaryBean? = null
    private val deviceSettingBean by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }
    private var mPowerSavingObserver: Observer<Boolean>? = null
    private var mOverlayScreenObserver: Observer<Boolean>? = null
    private var mVibrationObserver: Observer<Int>? = null
    private var mLanguageObserver: Observer<LanguageListBean>? = null

    //发送固件升级dialog
    private lateinit var uploadDialog: DownloadDialog

    //是否传输ota中
    var isOtaSending = false


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_take_more)
        EventBus.getDefault().register(this)
        loadDialog = DialogUtils.showLoad(this)

        loadViews()

        if (Global.deviceLanguageList == null) Global.getDevLanguage()
    }

    private fun loadViews(isEnabled: Boolean = true) {
        binding.llRemindSetList.removeAllViews()
        var texts = resources.getStringArray(R.array.devMoreSetStrList)
        binding.llRemindSetList.removeAllViews()

        if (deviceSettingBean == null) {
            texts = texts.filter {
                it == getString(R.string.dev_more_set_reboot) ||
                        it == getString(R.string.dev_more_set_ble_mac) ||
                        it == getString(R.string.dev_more_set_update)
            }.toTypedArray()
        }
        texts.forEach { it ->
            if (checkLoad(it)) {
                val constraintLayout = LayoutInflater.from(this).inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                val mSwitchCompat = constraintLayout.findViewById<SwitchCompat>(R.id.mSwitchCompat)
                val tvOther = constraintLayout.findViewById<TextView>(R.id.tvOther)
                val ivNext = constraintLayout.findViewById<ImageView>(R.id.ivNext)
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                tvName.text = it
                image.background = null
                image.visibility = View.GONE
                tvName.setPadding(0, 0, 0, 0)
                constraintLayout.alpha = if (isEnabled) 1.0f else 0.5f
                constraintLayout.isEnabled = isEnabled
                mSwitchCompat.isEnabled = isEnabled

                when (it) {
                    /* getString(R.string.dev_more_set_google_fit) -> {
                         ivNext.visibility = View.GONE
                         mSwitchCompat.visibility = View.VISIBLE

                         mSwitchCompat.isChecked = SpUtils.getSPUtilsInstance()
                             .getBoolean(SpUtils.SWITCH_GOOGLE_FIT, false)
                     }*/
                    getString(R.string.dev_more_set_coverage_screen) -> {
                        ivNext.visibility = View.GONE
                        mSwitchCompat.visibility = View.VISIBLE
                        //覆盖息屏开关
                        simpleSettingSummaryBean?.let { bean ->
                            mSwitchCompat.isChecked = bean.isOverlayScreen
                        }
                        if (mOverlayScreenObserver != null) {
                            viewModel.deviceSettingLiveData.getOverlayScreen().removeObserver(mOverlayScreenObserver!!)
                        }
                        mOverlayScreenObserver = Observer {
                            LogUtils.d("覆盖息屏 ->$it")
                            mSwitchCompat.isChecked = it
                        }
                        viewModel.deviceSettingLiveData.getOverlayScreen().observe(this, mOverlayScreenObserver!!)
                    }
                    getString(R.string.dev_more_set_shake) -> {
                        tvOther.visibility = View.VISIBLE
                        //震动级别
                        simpleSettingSummaryBean?.let { bean ->
                            tvOther.text = "${bean.vibrationIntensityMode + 1}"
                        }

                        if (mVibrationObserver != null) {
                            viewModel.deviceSettingLiveData.getVibration().removeObserver(mVibrationObserver!!)
                        }
                        mVibrationObserver = Observer {
                            LogUtils.d("震动强度 ->$it")
                            tvOther.text = "${it + 1}"
                        }
                        viewModel.deviceSettingLiveData.getVibration().observe(this, mVibrationObserver!!)
                    }
                    getString(R.string.dev_more_set_ble_mac) -> {
                        tvOther.visibility = View.VISIBLE
                        ivNext.visibility = View.GONE
                        tvOther.setPadding(0, 0, 0, 0)
                        tvOther.text = Global.deviceMac
                    }
                    getString(R.string.dev_more_set_update) -> {
                        tvOther.visibility = View.VISIBLE
                        tvOther.text = Global.deviceVersion
                        viewLine01.visibility = View.GONE
                    }
                }

                mSwitchCompat.setOnClickListener { view ->
                    //ToastUtils.showShort("是否打开 "+mSwitchCompat.isChecked)
                    when (it) {
                        /*getString(R.string.dev_more_set_google_fit) -> {
                            SpUtils.getSPUtilsInstance()
                                .put(SpUtils.SWITCH_GOOGLE_FIT, mSwitchCompat.isChecked)
                        }*/
                        getString(R.string.dev_more_set_coverage_screen) -> {
                            mSwitchCompat.isChecked = !mSwitchCompat.isChecked
                            if (!ControlBleTools.getInstance().isConnect) {
                                ToastUtils.showToast(R.string.device_no_connection)
                                return@setOnClickListener
                            }
                            loadDialog.show()
                            ControlBleTools.getInstance().setOverlayScreen(!mSwitchCompat.isChecked,
                                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                    override fun onState(state: SendCmdState) {
                                        DialogUtils.dismissDialog(loadDialog)
                                        ToastUtils.showSendCmdStateTips(
                                            state
                                        )
                                        ControlBleTools.getInstance().getOverlayScreen(null)
                                    }
                                })
                        }
                    }
                }
                constraintLayout.tag = it
                setViewsClickListener(this, constraintLayout)
                binding.llRemindSetList.addView(constraintLayout)

            }
        }
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkLoad(it: String?): Boolean {
        if (it == null) return false
        if (/*it == getString(R.string.dev_more_set_google_fit) ||*/
            it == getString(R.string.dev_more_set_reboot) ||
            it == getString(R.string.dev_more_set_ble_mac) ||
            it == getString(R.string.dev_more_set_update) ||
            it == getString(R.string.dev_more_set_time)
        ) {
            return true
        }
        //后台配置的
        if (deviceSettingBean != null) {
            when (it) {
                getString(R.string.dev_more_set_coverage_screen) -> {
                    return deviceSettingBean.settingsRelated.cover_the_screen_off
                }
                getString(R.string.dev_more_set_lum) -> {
                    return deviceSettingBean.settingsRelated.bright_adjustment ||
                            deviceSettingBean.settingsRelated.bright_screen_time ||
                            deviceSettingBean.settingsRelated.double_click_to_brighten_the_screen
                }
                getString(R.string.dev_more_set_shake) -> {
                    return deviceSettingBean.settingsRelated.vibration_adjustment
                }
                getString(R.string.dev_more_set_language_sel) -> {
                    return deviceSettingBean.settingsRelated.language
                }
                getString(R.string.dev_more_set_app_sort) -> {
                    return deviceSettingBean.settingsRelated.application_list_sorting
                }
            }
        }
        return false
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getSimpleSettingSummary().observe(this, {
            if (it == null) return@observe
            LogUtils.d("简单设置汇总 ->${GsonUtils.toJson(it)}")
            simpleSettingSummaryBean = it
            loadViews()
        })

        ControlBleTools.getInstance().getSimpleSetting(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                ToastUtils.showSendCmdStateTips(state)
            }
        })


        viewModel.queryFirewareCode.observe(this) {
            if (it == null) return@observe
            //DialogUtils.dismissDialog(loadDialog)
            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                }
                HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                    showNonewverDialog()
                }
            }
        }

    }

    //region 震动强度
    /**
     * 设置震动强度
     * */
    private fun setVibrationIntensity() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            //item.toInt()
            loadDialog.show()
            ControlBleTools.getInstance()
                .setDeviceVibrationIntensity(item.toInt() - 1, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                        ControlBleTools.getInstance().getDeviceVibrationIntensity(null)
                    }
                })
        }
        picker.setRangeStep(1, 3, 1)
        simpleSettingSummaryBean?.let { bean ->
            picker.setDefaultValue(bean.vibrationIntensityMode + 1)
        }
        viewModel.deviceSettingLiveData.getVibration().value?.let {
            picker.setDefaultValue(it + 1)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }
    //endregion

    //region 时间设置
    private fun timeSet() {
        val data = mutableListOf<String>()
        data.add("12")
        data.add("24")
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            loadDialog.show()
            ControlBleTools.getInstance().setTime(System.currentTimeMillis(),
                (item.toString() == "12").apply {
                    SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_TIME_IS12, this)
                },
                object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                    }
                })
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setDefaultPosition(
            if (SpUtils.getSPUtilsInstance().getBoolean(SpUtils.DEVICE_TIME_IS12, false)) 0 else 1
        )
        picker.show()
    }
    //endregion

    //region 重启设备
    private fun rebootDev() {
        showRebootDialog()
    }

    private fun showRebootDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.dev_more_set_reboot),
            getString(R.string.reboot_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    ControlBleTools.getInstance().rebootDevice(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                            ManageActivity.removeActivity(DeviceSetActivity::class.java)
                            ManageActivity.removeActivity(DevMoreSetActivity::class.java)
                            ControlBleTools.getInstance().disconnect()
                            //3s后重连
                            ThreadUtils.runOnUiThreadDelayed({
                                SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                            }, 5000)
                        }
                    })
                }

                override fun OnCancel() {}
            })
        dialog.show()
    }

    private fun showRebootFailedDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.reset_reboot_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    //TODO 测试 重启设备
                    ThreadUtils.runOnUiThreadDelayed({
                        DialogUtils.dismissDialog(loadDialog)
                        showRebootFailedDialog()
                    }, 1000)
                }

                override fun OnCancel() {

                }
            })
        dialog.show()
    }
    //endregion

    //region 设备固件更新
    private fun updateDevice() {
        GlobalEventManager.checkFirmwareUpgrade()
    }

    /**
     * 无升级提示
     */
    private fun showNonewverDialog() {
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.nonewver_tips),
            getString(R.string.dialog_confirm_btn),
            null
        )
    }

    override fun onClick(v: View?) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        when (v?.tag.toString()) {
            getString(R.string.dev_more_set_time) -> {
                timeSet()
            }
            getString(R.string.dev_more_set_lum) -> {
                startActivity(Intent(this, LightSetActivity::class.java))
            }
            getString(R.string.dev_more_set_shake) -> {
                setVibrationIntensity()
            }
            getString(R.string.dev_more_set_language_sel) -> {
                startActivity(Intent(this, LanguageSetActivity::class.java))
            }
            getString(R.string.dev_more_set_reboot) -> {
                rebootDev()
            }
            getString(R.string.dev_more_set_update) -> {
                updateDevice()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                //蓝牙关闭
                if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    loadViews(false)
                }
            }
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        loadViews(false)
                        if (isOtaSending) {
                            isOtaSending = false
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_OTA, TrackingLog.getAppTypeTrack("中途蓝牙断连"), "1913", true)
                        }
                    }
                }
            }
            EventAction.ACTION_DEVICE_CONNECTED -> {
                loadViews(true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }
}