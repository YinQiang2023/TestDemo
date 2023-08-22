package com.smartwear.publicwatch.ui.device

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ThreadUtils
import com.zhapp.ble.ControlBleTools
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.ui.adapter.SimpleAdapter
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.ActivityDeviceManageBinding
import com.smartwear.publicwatch.databinding.DeviceItemLayoutBinding
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.response.BindListResponse
import com.smartwear.publicwatch.ui.HomeActivity
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.devicecontrol.NotEnabledActivity
import com.smartwear.publicwatch.ui.device.scan.ScanDeviceActivity
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.view.ViewForLayoutNoInternet
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.viewmodel.DeviceModel
import org.greenrobot.eventbus.EventBus

class DeviceManageActivity : BaseActivity<ActivityDeviceManageBinding, DeviceModel>(
    ActivityDeviceManageBinding::inflate,
    DeviceModel::class.java
), View.OnClickListener {

    //刷新设备列表的时间
    private var mLastRefreshTime = 0L

    //设备列表
    var dataList: MutableList<BindListResponse.DeviceItem> = mutableListOf()

    lateinit var loadDialog: Dialog


    private val deviceAdapter by lazy {
        initDeviceAdapter()
    }

    private fun initDeviceAdapter(): SimpleAdapter<BindListResponse.DeviceItem, DeviceItemLayoutBinding> {
        return object :
            SimpleAdapter<BindListResponse.DeviceItem, DeviceItemLayoutBinding>(
                DeviceItemLayoutBinding::inflate,
                dataList
            ) {
            override fun onBindingData(
                binding: DeviceItemLayoutBinding?,
                t: BindListResponse.DeviceItem,
                position: Int,
            ) {
                binding?.tvName?.text = t.deviceName
                binding?.tvMac?.text =
                    if (dataList[position].deviceStatus == 1)
                        getString(R.string.device_fragment_using)
                    else getString(R.string.device_fragment_using_no)

                var productLogoUrl = ""
                val deviceType = t.deviceType.toString()
                for (j in Global.productList.indices) {
                    if (deviceType == Global.productList[j].deviceType) {
                        productLogoUrl = Global.productList[j].productLogo
                        break
                    }
                }
                binding?.ivDevice?.let {
                    if (TextUtils.isEmpty(productLogoUrl)) {
                        GlideApp.with(this@DeviceManageActivity).load(R.mipmap.device_no_bind_right_img).into(binding.ivDevice)
                    } else {
                        GlideApp.with(this@DeviceManageActivity).load(productLogoUrl).into(binding.ivDevice)
                    }
                }

                binding?.root?.setOnClickListener {
                    if (position < dataList.size) {
                        if (dataList[position].deviceStatus == 1) {
                            val intent =
                                Intent(this@DeviceManageActivity, DeviceSetActivity::class.java)
                            intent.putExtra("name", dataList[position].deviceName)
                            intent.putExtra("type", deviceType)
                            intent.putExtra("mac", dataList[position].deviceMac)
                            startActivityForResult(intent, HomeActivity.UNBIND_REQUEST_CODE)
                        } else {
                            // 跳转非启用界面
                            val intent =
                                Intent(this@DeviceManageActivity, NotEnabledActivity::class.java)
                            intent.putExtra("oldDevice", t.deviceName)
                            intent.putExtra("list", dataList[position])
                            startActivityForResult(intent, HomeActivity.UNBIND_REQUEST_CODE)
                        }
                    }
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun initView() {
        setTvTitle(R.string.device_fragment_my)
        binding.rvDevice
        binding.lyRefresh.setOnRefreshListener {
            viewModel.getBindList(false)
        }

        binding.rvDevice.apply {
            adapter = deviceAdapter
        }

        setViewsClickListener(
            this,
            binding.btnAdd
        )
        binding.lyNoNetWork.setRetryListener(object : ViewForLayoutNoInternet.OnRetryListener {
            override fun onOnRetry() {
                viewModel.getBindList(false)
            }

        })
        binding.lyNoNetWork.hideHead()
        binding.lyNoNetWork.visibility = View.GONE
        loadDialog = DialogUtils.showLoad(this)
        showDialog()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getBindList(false)
    }

    override fun initData() {

        dataList.clear()
        for (i in DeviceManager.dataList.indices) {
            dataList.add(DeviceManager.dataList[i])
        }
        binding.btnAdd.isEnabled = dataList.size < 5
        deviceAdapter.notifyDataSetChanged()

        viewModel.getBindListCode.observe(this, MyBindListCodeObserver())
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }
    //region 刷新设备列表

    /**
     * 刷新设备列表结果状态码观察者
     */
    inner class MyBindListCodeObserver : Observer<String> {
        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged(it: String?) {
            binding.lyRefresh.complete()
            dismissDialog()
            if (TextUtils.isEmpty(it)) return
            when (it) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (DeviceManager.dataList.size > 0) {
                        refDeviceListUI()
                        dataList.clear()
                        for (i in DeviceManager.dataList.indices) {
                            dataList.add(DeviceManager.dataList[i])
                        }
                        binding.btnAdd.isEnabled = dataList.size < 5
                        deviceAdapter.notifyDataSetChanged()
                    } else {
                        SpUtils.setValue(SpUtils.DEVICE_NAME, "")
                        SpUtils.setValue(SpUtils.DEVICE_MAC, "")
                        ControlBleTools.getInstance().disconnect()
                    }
                }
                HttpCommonAttributes.SERVER_ERROR -> {
                    showNoHaveDevice() //用于HealthyFragment刷新设备状态
                    binding.lyRefresh.visibility = View.GONE
                    binding.btnAdd.visibility = View.GONE
                    binding.lyNoNetWork.visibility = View.VISIBLE
                }
                HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                    finish()
                }
            }
        }
    }

    /**
     * 刷新设备列表界面
     */
    fun refDeviceListUI() {
        if (DeviceManager.dataList.size > 0) {
            if (Math.abs(System.currentTimeMillis() - mLastRefreshTime) < 1000L) {
                //请求刷新列表时间过短，延时一秒加载页面，防止页面跳闪
                ThreadUtils.runOnUiThreadDelayed({
                    refDeviceListUI()
                }, 1000)
                return
            }
            binding.lyRefresh.visibility = View.VISIBLE
            binding.btnAdd.visibility = View.VISIBLE
            binding.lyNoNetWork.visibility = View.GONE
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

        binding.lyRefresh.visibility = View.GONE
        binding.btnAdd.visibility = View.GONE
        binding.lyNoNetWork.visibility = View.VISIBLE
        if (DeviceManager.dataList != null) {
            DeviceManager.dataList.clear()
//            binding.layoutDeviceList.removeAllViews()
        }
        var txt = getString(R.string.device_welcome_user)
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_NO_DEVICE_BINDING))
    }

    fun showDialog() {
        if (!loadDialog.isShowing) {
            loadDialog.show()
        }
    }

    fun dismissDialog() {
        DialogUtils.dismissDialog(loadDialog, 500)
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

                PermissionUtils.checkRequestPermissions(
                    lifecycle,
                    BaseApplication.mContext.getString(R.string.permission_bluetooth),
                    PermissionUtils.PERMISSION_BLE12
                ) {
                    startAddDevice()
                }
                return
            }
        }
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
                } else {
                    if (!AppUtils.isGPSOpen(this)) {
                        AppUtils.showGpsOpenDialog()
                        return@checkRequestPermissions
                    }
                    startActivity(Intent(this, ScanDeviceActivity::class.java))
                }
            }
        } else {
            AppUtils.enableBluetooth(this, 0)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnAdd.id -> {
                startAddDevice()
            }
        }
    }
    //endregion

}