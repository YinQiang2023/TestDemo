package com.jwei.publicone.ui.device.scan

import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.GsonUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ScanDeviceBean
import com.zhapp.ble.utils.BleUtils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ScanDeviceActivityBinding
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.response.ProductListResponse
import com.jwei.publicone.ui.adapter.ScanDeviceAdapter
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.ui.device.bean.DeviceScanQrCodeBean
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.ui.user.QAActivity
import com.jwei.publicone.utils.*
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.viewmodel.DeviceModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class ScanDeviceActivity : BaseActivity<ScanDeviceActivityBinding, DeviceModel>(ScanDeviceActivityBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG: String = ScanDeviceActivity::class.java.simpleName
    private var dialog: Dialog? = null
    private var lists = ArrayList<ScanDeviceBean>()
    private lateinit var adapter: ScanDeviceAdapter
    private var isScanning = false
    private val SCAN_QR_CODE = 1000

    private val getProductListTrackingLog by lazy { TrackingLog.getSerTypeTrack("获取产品设备号列表", "获取产品设备号列表", "infowear/product/list") }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    var rotate: RotateAnimation? = null
    private fun initRotate() {
        rotate = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        val lin = LinearInterpolator()
        rotate!!.interpolator = lin
        rotate!!.duration = 2000 // 设置动画持续周期
        rotate!!.repeatCount = -1 // 设置重复次数
        rotate!!.fillAfter = true // 动画执行完后是否停留在执行完的状态
        rotate!!.startOffset = 10 // 执行前的等待时间
    }

    private fun startAnimation() {
        binding.ivScanningRightIcon.background = ContextCompat.getDrawable(this, R.mipmap.scan_right)
        binding.ivScanningRightIcon.startAnimation(rotate)
    }

    private fun stopAnimation() {
        binding.ivScanningRightIcon.background = ContextCompat.getDrawable(this, R.mipmap.scan_right_finish)
        binding.ivScanningRightIcon.clearAnimation()
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.scan_device_title)
        AppUtils.registerEventBus(this)
        binding.layoutTitle.layoutRight.visibility = View.VISIBLE
        binding.layoutTitle.ivRightIcon.visibility = View.VISIBLE
        ivRightIcon?.setImageResource(R.mipmap.scan_qrcode)
        setViewsClickListener(this, binding.layoutTitle.layoutRight, binding.layoutScanDevice, binding.tvHelp)

        scanHandler = Handler(Looper.myLooper()!!)

        binding.mRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ScanDeviceAdapter(this, lists, viewModel)
        binding.mRecyclerView.adapter = adapter

        binding.tvHelp.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG); //下划线

        viewModel.scanDevice.observe(this) {
            if (it == 1) {
                adapter.myDeviceSort()
                adapter.notifyDataSetChanged()
            }
        }
        initDeviceItemClick()
        initRotate()
    }

    override fun initData() {
        super.initData()
        observeInit()
        checkProductData()
    }

    //region 扫描
    private var scanHandler: Handler? = null

    private fun scanningDeviceStart() {
        lists.clear()
        adapter.notifyDataSetChanged()
        viewModel.startScanDevice(lists)
        binding.tvScanningTitle.text = getString(R.string.scan_device_scanning)
        binding.tvScanningTip.text = getString(R.string.scan_device_tip)
//        ivScanningRightIcon.background = ContextCompat.getDrawable(this, R.mipmap.scan_right)
        startAnimation()

        scanHandler?.removeCallbacksAndMessages(null)
        scanHandler?.postDelayed({
            isScanning = false;
            scanningDeviceOver()
            LogUtils.i(TAG, "搜索30S自动停止 = adapter.itemCount = " + adapter.itemCount)
            if (adapter.itemCount <= 0 && !isFinishing && !isDestroyed) {
                DialogUtils.showDialogTitle(this,
                    getString(R.string.scan_no_device_title),
                    getString(R.string.scan_no_device_content),
                    getString(R.string.dialog_cancel_btn),
                    getString(R.string.dialog_retry_btn),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            checkProductData()
                        }

                        override fun OnCancel() {
                        }
                    }).show()

                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_BIND,
                    TrackingLog.getAppTypeTrack("搜索超时/失败"), "1213", true
                )
            }
        }, 30 * 1000)
    }

    private fun scanningDeviceOver() {
        viewModel.stopScanDevice()
        binding.tvScanningTitle.text = getString(R.string.scan_device_over)
        binding.tvScanningTip.text = getString(R.string.scan_device_connect)
//        ivScanningRightIcon.background = ContextCompat.getDrawable(this, R.mipmap.scan_right_finish)
        stopAnimation()
    }
    //endregion

    private fun observeInit() {

        //后台绑定回调
        viewModel.getProductList.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {


                if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_BIND,
                        getProductListTrackingLog.apply {
                            log += "\n请求超时/失败"
                        }, "1212", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, getProductListTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                    })
                }

                dismissDialog()
                if (it == HttpCommonAttributes.REQUEST_SUCCESS) {
                    LogUtils.i(TAG, "产品列表-获取成功")
                    if (viewModel.checkProductList()) {
                        LogUtils.i(TAG, "产品列表-获取成功 = 有数据")
                        searchDeviceMethod()
                    } else {
                        LogUtils.i(TAG, "产品列表-获取成功 = 无数据")
                        ToastUtils.showToast(getString(R.string.err_network_tips))
                        finish()
                    }
                } else if (it == HttpCommonAttributes.SERVER_ERROR) {
                    ToastUtils.showToast(getString(R.string.not_network_tips))
                    LogUtils.i(TAG, "产品列表-获取失败 网络请求失败 = it = $it", true)
                    finish()
                } else {
                    LogUtils.i(TAG, "产品列表-获取失败 = it = $it", true)
                    ToastUtils.showToast(getString(R.string.err_network_tips))
                    finish()
                }
            }
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun eventBusMsg(msg: EventMessage) {
        when (msg.action) {
            /*EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        LogUtils.w(TAG, "device connected")
                    }
                    BleCommonAttributes.STATE_CONNECTING -> {
                        LogUtils.w(TAG, "device connecting")
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        LogUtils.w(TAG, "device distonnect")
                    }
                }
            }*/
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    if (isScanning) {
                        scanningDeviceOver()
                        isScanning = !isScanning
                    }
                }
            }
            EventAction.ACTION_QUIT_QR_SCAN_MANUAL_BIND -> {
                searchDeviceMethod()
            }
        }
    }

    //检查产品列表是否为空
    private fun checkProductData() {
        LogUtils.i(TAG, "checkProductData start")
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("检测产品列表是否为空").apply {
            log = "设备产品列表：${GsonUtils.toJson(viewModel.checkProductList())}"
        })
        if (viewModel.checkProductList()) {
            LogUtils.i(TAG, "checkProductData 列表不为空，搜索设备")
            searchDeviceMethod()
        } else {
            LogUtils.i(TAG, "checkProductData 列表为空，请求后台")
            viewModel.getProductList(getProductListTrackingLog)
            dialog = DialogUtils.dialogShowLoad(this)
        }
    }

    //搜索按钮点击事件
    fun searchDeviceMethod() {
        scanHandler?.removeCallbacksAndMessages(null)
        if (isScanning) {
            scanningDeviceOver()
        } else {
            scanningDeviceStart()
        }
        isScanning = !isScanning

        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("搜索列表连接"))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            //跳转扫描二维码
            binding.layoutTitle.layoutRight.id -> {
                if (!AppUtils.isOpenBluetooth()) {
                    ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                    return
                }
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, TrackingLog.getAppTypeTrack("扫码连接"))
                if (!com.blankj.utilcode.util.PermissionUtils.isGranted(*PermissionUtils.PERMISSION_GROUP_CAMERA)) {
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_BIND,
                        TrackingLog.getAppTypeTrack("相机权限未开")/*, "1216", true*/
                    )
                }
                com.jwei.publicone.utils.PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    getString(R.string.permission_camera),
                    com.jwei.publicone.utils.PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    startActivityForResult(Intent(this, ScanCodeActivity::class.java), SCAN_QR_CODE)
                    scanningDeviceOver()
                    scanHandler?.removeCallbacksAndMessages(null)
                }
            }
            //搜索
            binding.layoutScanDevice.id -> {
                LogUtils.i(TAG, "binding isScanning = $isScanning")
                checkProductData()
            }
            //跳转帮助
            binding.tvHelp.id -> {
                startActivity(Intent(this, QAActivity::class.java))
                scanningDeviceOver()
                scanHandler?.removeCallbacksAndMessages(null)
            }
        }
    }

    /**
     * 点击扫描设备 ItemClick
     */
    private fun initDeviceItemClick() {
        adapter.setListener(object : ScanDeviceAdapter.ItemClickListener {
            override fun onItemClick(position: Int) {
                val bean = adapter.getData().get(position)
                scanningDeviceOver()
                if (!AppUtils.isOpenBluetooth()) {
                    ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                    return
                }
                var product: ProductListResponse.Data? = null
                for (j in Global.productList.indices) {
                    if (TextUtils.equals(bean.deviceType, Global.productList[j].deviceType)) {
                        product = Global.productList[j]
                        break
                    }
                }
                if (product == null) {
                    LogUtils.e(TAG, "绑定失败，无设备产品信息！！！！")
                    ToastUtils.showToast(R.string.bind_device_error)
                    return
                }

                //后台产品信息是否支持bt
                val isSupportBt = product.supportBt == "1"
                if (isSupportBt) {
                    if (bean.isSupportHeadset) { //设备支持bt
                        if (!TextUtils.isEmpty(bean.address) && !TextUtils.isEmpty(bean.headsetMac)) {
                            //保存bt mac
                            SpUtils.saveHeadsetMac(bean.address, bean.headsetMac)
                            if (!TextUtils.isEmpty(product.btBleName)) {
                                val hBleName = product.btBleName + "_" + BleUtils.getMacLastStr(bean.headsetMac, 4)
                                //保存bt name
                                SpUtils.saveHeadsetName(bean.address, hBleName)
                            }
                        }
                    }
                }
                //去绑定页面
                searchToBindDeviceActivity(bean, product)
            }
        })
    }

    /**
     * 扫码结果绑定
     */
    fun scanCodeDeviceResult(deviceScanQrCodeBean: DeviceScanQrCodeBean?) {
        if (deviceScanQrCodeBean != null) {
            LogUtils.i(TAG, "deviceScanQrCodeBean =  $deviceScanQrCodeBean", true)
            //DeviceScanQrCodeBean{appDownUrl='', radio='e72415133d083075010206300000d82415133d08', random='474545', name='',
            // mDeviceRadioBroadcastBean=DeviceRadioBroadcastBean{, deviceMac='E7:24:15:13:3D:08,
            // deviceType=30000, deviceVersion=66054, isBind=false, isUserMode=true, isDirectConnection=true,
            // isSupportHeadset=true, isHeadsetBond=false, isHeadsetBroadcast=false, headsetMac='D8:24:15:13:3D:08}}
            if (deviceScanQrCodeBean.getmDeviceRadioBroadcastBean() == null) {
                LogUtils.e(TAG, "绑定失败，扫码信息异常！！！！", true)
                ToastUtils.showToast(R.string.bind_device_error)
                return
            }
            if (!AppUtils.isOpenBluetooth()) {
                ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                return
            }
            var product: ProductListResponse.Data? = null
            for (j in Global.productList.indices) {
                if (TextUtils.equals(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().deviceType.toString(), Global.productList[j].deviceType)) {
                    product = Global.productList[j]
                    break
                }
            }
            if (product == null) {
                LogUtils.e(TAG, "绑定失败，无设备产品信息！！！！", true)
                ToastUtils.showToast(R.string.bind_device_error)
                return
            }
            //后台产品信息是否支持bt
            val isSupportBt = product.supportBt == "1"
            if (isSupportBt) {
                if (deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().isSupportHeadset) { //设备支持bt
                    if (!TextUtils.isEmpty(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().deviceMac) &&
                        !TextUtils.isEmpty(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().headsetMac)
                    ) {
                        //保存bt mac
                        SpUtils.saveHeadsetMac(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().deviceMac, deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().headsetMac);

                        if (!TextUtils.isEmpty(product.btBleName)) {
                            val hBleName = product.btBleName + BleUtils.getMacLastStr(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().headsetMac, 4)
                            //保存bt name
                            SpUtils.saveHeadsetName(deviceScanQrCodeBean.getmDeviceRadioBroadcastBean().deviceMac, hBleName)
                        }
                    }
                }
            }
            //不支持bt通话蓝牙,去绑定页面
            scanCodeToBindDeviceActivity(deviceScanQrCodeBean, product)
        }
    }
    //endregion

    /**
     * 扫描去绑定设备
     */
    fun searchToBindDeviceActivity(bean: ScanDeviceBean, product: ProductListResponse.Data) {
        ControlBleTools.getInstance().disconnect()
        val intent = Intent(this@ScanDeviceActivity, BindDeviceActivity::class.java)
        intent.putExtra(BindDeviceActivity.EXTRA_NAME, bean.name)
        intent.putExtra(BindDeviceActivity.EXTRA_ADDRESS, bean.address)
        intent.putExtra(BindDeviceActivity.EXTRA_LOGO_URL, product.homeLogo)
        intent.putExtra(BindDeviceActivity.EXTRA_DEVICE_TYPE, bean.deviceType)
        intent.putExtra(BindDeviceActivity.EXTRA_IS_SCAN_CODE, false)
        intent.putExtra(BindDeviceActivity.EXTRA_DEVICE_ISBR, bean.isSupportHeadset && product.supportBt == "1")
        startActivity(intent)
    }

    /**
     * 扫码去绑定设备
     */
    fun scanCodeToBindDeviceActivity(bean: DeviceScanQrCodeBean, product: ProductListResponse.Data) {
        ControlBleTools.getInstance().disconnect()
        val intent = Intent(this@ScanDeviceActivity, BindDeviceActivity::class.java)
        intent.putExtra(BindDeviceActivity.EXTRA_NAME, bean.name)
        intent.putExtra(BindDeviceActivity.EXTRA_ADDRESS, bean.getmDeviceRadioBroadcastBean().deviceMac)
        intent.putExtra(BindDeviceActivity.EXTRA_LOGO_URL, product.homeLogo)
        intent.putExtra(BindDeviceActivity.EXTRA_DEVICE_TYPE, bean.getmDeviceRadioBroadcastBean().deviceType.toString())
        intent.putExtra(BindDeviceActivity.EXTRA_CODE, bean.random)
        intent.putExtra(BindDeviceActivity.EXTRA_IS_SCAN_CODE, true)
        intent.putExtra(BindDeviceActivity.EXTRA_DEVICE_ISBR, bean.getmDeviceRadioBroadcastBean().isSupportHeadset && product.supportBt == "1")
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCAN_QR_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val deviceScanQrCodeBean = data?.getParcelableExtra<DeviceScanQrCodeBean>(ScanCodeActivity.SCAN_RESULT)
                scanCodeDeviceResult(deviceScanQrCodeBean)
            }
        }
    }

    /**
     * 关闭等待弹窗
     */
    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) viewModel.stopScanDevice()
    }

    override fun onDestroy() {
        if (isScanning) {
            viewModel.stopScanDevice()
        }
        super.onDestroy()

        scanHandler?.removeCallbacksAndMessages(null)
        AppUtils.unregisterEventBus(this)
    }

}