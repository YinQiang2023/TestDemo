package com.smartwear.publicwatch.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.ThemeManager
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.utils.NotificationUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.DialogPerExplainBinding
import com.smartwear.publicwatch.databinding.HomeActivityBinding
import com.smartwear.publicwatch.db.model.track.TrackingLog
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.dialog.customdialog.CustomDialog
import com.smartwear.publicwatch.service.LocationService
import com.smartwear.publicwatch.service.MyNotificationsService
import com.smartwear.publicwatch.ui.adapter.FragmentAdapter
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.user.AppUpdateManager
import com.smartwear.publicwatch.ui.user.UpdateInfoService
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.PermissionUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import com.smartwear.publicwatch.utils.manager.GoogleFitManager
import com.smartwear.publicwatch.viewmodel.DeviceModel
import com.smartwear.publicwatch.viewmodel.UserModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 首页
 * 初次启动事件流程 1.权限说明弹窗 2.通知使用权限 3.蓝牙是否开启 4.连接设备 5.app升级
 */
class HomeActivity : BaseActivity<HomeActivityBinding, UserModel>(
    HomeActivityBinding::inflate,
    UserModel::class.java
) {
    private var TAG = HomeActivity::class.java.simpleName

    companion object ACTIVITY_REQUEST_CODE {
        //通知发送允许权限
        const val SET_NOTIFICATION_REQUEST_CODE = 0x01

        //健康页面卡片排序
        const val EDIT_CARD_ITEM_REQUEST_CODE = 0x02

        //设备模块解绑
        const val UNBIND_REQUEST_CODE = 0x03

        //设备模块解绑
        const val NOTIFICATION_ENABLE_REQUEST_CODE = 0x04
    }

    private val fragmentList = ArrayList<Fragment>()

    //首页是否已经首次连接设备
    private var isFirstConnect = false

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public fun onEventMessage(msg: EventMessage) {
//        when (msg.action) {
//
//        }
//    }

    override fun initView() {
        super.initView()
        fragmentList.add(HealthyFragment())
        fragmentList.add(NewSportFragment()/*SportFragment()*/)
        fragmentList.add(DeviceFragment())
        fragmentList.add(MineFragment())
        val fm: FragmentManager = supportFragmentManager
        val fa = FragmentAdapter(fm, fragmentList)

        binding.viewPager.apply {
            adapter = fa
            isNoScroll = true
            offscreenPageLimit = 4
        }
        binding.viewPager.currentItem = 0


        binding.radioButtonGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButton1 -> {
                    updateUi(0)
                }

                R.id.radioButton2 -> {
                    updateUi(1)
                }

                R.id.radioButton3 -> {
                    updateUi(2)
                }

                R.id.radioButton4 -> {
                    updateUi(3)
                }
            }
        })

    }

    override fun initData() {
        super.initData()
        AppUtils.registerEventBus(this)
        //获取设备语言
        Global.getDevLanguage()
        //获取设备产品列表
        DeviceModel().getProductList()
//        GlobalVariable.getUserInfo()
        Global.fillListData()
        UserModel().getUserInfo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //初始化蓝牙服务
        BaseApplication.application.initControlBleTools(BaseApplication.mContext)
        //DeviceFragment initData refreshDeviceView connect 已经执行连接已绑定设备
        //初始化数据授权
        BaseApplication.initDataReduction()
        //权限说明弹窗
        if (!SpUtils.getSPUtilsInstance().getBoolean(SpUtils.FIRST_MAIN_PERMISSION_EXPLAIN, false)) {
            showPermissionExplain()
            return
        }
        //通知使用权限
        areNotificationsEnabled()
    }

    override fun onResume() {
        super.onResume()
        //isCurrentActivity = true
        if (HealthyFragment.viewIsVisible || DeviceFragment.viewIsVisible) {
            RealTimeRefreshDataUtils.openRealTime()
        }
    }

    /**
     * 首次进入主页权限说明弹窗
     */
    private fun showPermissionExplain() {
        val dialogBinding = DialogPerExplainBinding.inflate(layoutInflater)
        val perExplainDialog = CustomDialog.builder(this)
            .setContentView(dialogBinding.root)
            .setHeight((ScreenUtils.getScreenHeight() * 0.7).toInt())
            .setCancelable(false)
            .build()
        perExplainDialog.show()
        ClickUtils.applySingleDebouncing(dialogBinding.btnGotIt) {
            SpUtils.getSPUtilsInstance().put(SpUtils.FIRST_MAIN_PERMISSION_EXPLAIN, true)
            perExplainDialog.dismiss()
            //通知使用权限
            areNotificationsEnabled()
        }
    }

    /**
     * 通知使用权
     */
    fun areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { //Android 13 通知使用权运动时权限申请
            if (!PermissionUtils.checkPermissions(*PermissionUtils.PERMISSION_NOTIF13)) {
                PermissionUtils.checkRequestPermissions(
                    lifecycle,
                    getString(R.string.notification_13_permission_hint),
                    PermissionUtils.PERMISSION_NOTIF13
                ) {
                    //通知使用权限
                    areNotificationsEnabled()

                    if (NotificationUtils.areNotificationsEnabled(this)) {
                        //重启蓝牙服务重启app通知
                        if (AppUtils.isOpenBluetooth()) {
                            com.smartwear.publicwatch.utils.LogUtils.e("sdk release", "Notification permission authorization")
                            ControlBleTools.getInstance().release()
                            BaseApplication.application.initControlBleTools(BaseApplication.mContext)
                        }
                        //重启定位服务
                        LocationService.initLocationService(BaseApplication.mContext)
                    }

                }
                return
            }
        }
        if (!NotificationUtils.areNotificationsEnabled(this)) {
            showSetNotificationDialog()
        } else {
            //通知访问权限
            if (MyNotificationsService.checkNotificationIsEnable(this, NOTIFICATION_ENABLE_REQUEST_CODE)) {
                firstConnect()
            }
        }
    }

    /**
     * 通知使用权设置提示弹窗
     */
    private fun showSetNotificationDialog() {
        LogUtils.d("showSetNotificationDialog")
        val msg = StringBuilder()
        msg.append(getString(R.string.notification_permission_hint))
        DialogUtils.showDialogTitleAndOneButton(
            this,
            getString(R.string.apply_permission),
            msg.toString(),
            getString(R.string.know),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    NotificationUtils.goToSetNotification(
                        this@HomeActivity,
                        SET_NOTIFICATION_REQUEST_CODE
                    )
                }

                override fun OnCancel() {
                    NotificationUtils.goToSetNotification(
                        this@HomeActivity,
                        SET_NOTIFICATION_REQUEST_CODE
                    )
                }
            })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMsg(event: EventMessage) {
        when (event.action) {
            //权限被拒绝
            EventAction.ACTION_PERMISSION_DENIED -> {
                val deniedBean = event.obj as PermissionUtils.DeniedBean
                if (!deniedBean.deniedForever.contains(Manifest.permission.POST_NOTIFICATIONS) &&
                    deniedBean.denied.contains(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    //通知权限拒绝且可以允许申请
                    showSetNotificationDialog()
                }
            }
        }
    }

    /**
     * 检测apk版本升级
     */
    fun appCheckUpdate() {
        NetworkUtils.isAvailableAsync(object : Utils.Consumer<Boolean> {
            override fun accept(t: Boolean?) {
                t?.let {
                    if (t) {
                        AppUpdateManager.checkUpdate(this@HomeActivity, false)
                    }
                }
            }
        })
    }


    fun firstConnect() {
        //权限说明弹窗，通知访问权完成，如果已经绑定设备，尝试申请Android12权限并连接设备
        if (!isFirstConnect) {
            if (SpUtils.getSPUtilsInstance().getBoolean(SpUtils.FIRST_MAIN_PERMISSION_EXPLAIN, false)) {
                if (!TextUtils.isEmpty(SpUtils.getValue(SpUtils.DEVICE_NAME, "")) && !TextUtils.isEmpty(SpUtils.getValue(SpUtils.DEVICE_MAC, ""))) {
                    isFirstConnect = true
                    if (AppUtils.isOpenBluetooth()) {
                        if (!ControlBleTools.getInstance().isConnect) {
                            SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                            //检测app升级
                            appCheckUpdate()
                        }
                    } else {
                        /*AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getStartTypeTrack("重连"), isStart = true)
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("发起连接").apply {
                            log = "connect() name:${SpUtils.getValue(SpUtils.DEVICE_NAME, "")},address:${SpUtils.getValue(SpUtils.DEVICE_MAC, "")}"
                        })
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_RECONNECT, TrackingLog.getAppTypeTrack("系统蓝牙开关被关闭"), "1311", true)*/
                        //蓝牙未开启， 蓝牙开启后由 EventAction.ACTION_BLE_STATUS_CHANGE -> DeviceFragment 处响应并触发连接
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PermissionUtils.checkRequestPermissions(
                                null,
                                BaseApplication.mContext.getString(R.string.permission_bluetooth),
                                PermissionUtils.PERMISSION_BLE12
                            ) {
                                AppUtils.enableBluetooth(this, 0)
                                //检测app升级
                                appCheckUpdate()
                            }
                        } else {
                            AppUtils.enableBluetooth(this, 0)
                            //检测app升级
                            appCheckUpdate()
                        }
                    }

                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        RealTimeRefreshDataUtils.closeRealTime()
        //isCurrentActivity = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GoogleFitManager.resultRequestPermissions(requestCode, resultCode, data)
        if (requestCode == SET_NOTIFICATION_REQUEST_CODE) { // resultCode = 0 , data = null
            if (NotificationUtils.areNotificationsEnabled(this)) {
                //重启蓝牙服务重启app通知
                if (AppUtils.isOpenBluetooth()) {
                    com.smartwear.publicwatch.utils.LogUtils.e("sdk release", "Notification permission authorization")
                    ControlBleTools.getInstance().release()
                    BaseApplication.application.initControlBleTools(BaseApplication.mContext)
                }
                //重启定位服务
                LocationService.initLocationService(BaseApplication.mContext)
            }
            //获取 访问通知权限
            if (MyNotificationsService.checkNotificationIsEnable(this, NOTIFICATION_ENABLE_REQUEST_CODE)) {
                firstConnect()
            }
        } else if (requestCode == NOTIFICATION_ENABLE_REQUEST_CODE) {
            firstConnect()
        } else if (requestCode == UpdateInfoService.GET_UNKNOWN_APP_SOURCES) {
            AppUpdateManager.updateInfoService?.handleActivityResult(requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        AppUpdateManager.updateInfoService?.handlePermissionsResult(requestCode, grantResults)
    }

    private fun updateUi(item: Int) {
        binding.viewPager.currentItem = item

        binding.bottomMenu.bottomMenuImg1.setBackgroundResource(R.mipmap.menu_healthy_unselected)
        binding.bottomMenu.bottomMenuImg2.setBackgroundResource(R.mipmap.menu_sport_unselected)
        binding.bottomMenu.bottomMenuImg3.setBackgroundResource(R.mipmap.menu_device_unselected)
        binding.bottomMenu.bottomMenuImg4.setBackgroundResource(R.mipmap.menu_me_unselected)
        binding.bottomMenu.bottomMenuText1.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.bottom_color_off
            )
        )
        binding.bottomMenu.bottomMenuText2.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.bottom_color_off
            )
        )
        binding.bottomMenu.bottomMenuText3.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.bottom_color_off
            )
        )
        binding.bottomMenu.bottomMenuText4.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.bottom_color_off
            )
        )
        when (item) {
            0 -> {
                binding.bottomMenu.bottomMenuImg1.setBackgroundResource(R.mipmap.menu_healthy_selected)
                binding.bottomMenu.bottomMenuText1.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.bottom_color_on
                    )
                )
            }

            1 -> {
                binding.bottomMenu.bottomMenuImg2.setBackgroundResource(R.mipmap.menu_sport_selected)
                binding.bottomMenu.bottomMenuText2.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.bottom_color_on
                    )
                )
            }

            2 -> {
                binding.bottomMenu.bottomMenuImg3.setBackgroundResource(R.mipmap.menu_device_selected)
                binding.bottomMenu.bottomMenuText3.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.bottom_color_on
                    )
                )
            }

            3 -> {
                binding.bottomMenu.bottomMenuImg4.setBackgroundResource(R.mipmap.menu_me_selected)
                binding.bottomMenu.bottomMenuText4.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.bottom_color_on
                    )
                )
            }
        }
    }

    //region 双击返回退出
    private var mPressedTime = 0L
    override fun onBackPress() {
        /*var mNowTime = System.currentTimeMillis()
        if (mNowTime - mPressedTime > 2000) {
            ToastUtils.showToast(R.string.exit_app_tips)
            mPressedTime = mNowTime
        } else {
            finish()
            ActivityUtils.startHomeActivity()
        }*/
        try {
            //android.content.ActivityNotFoundException
            //No Activity found to handle Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] flg=0x10000000 }
            //java.lang.SecurityException
            //Permission Denial: starting Intent { act=android.intent.action.MAIN flg=0x10400000 cmp=com.huawei.android.launcher/.powersavemode.PowerSaveModeLauncher } from ProcessRecord{53f8c84 13883:com.zhapp.infowear/u0a143} (pid=13883, uid=10143) not exported from uid 10063
            ActivityUtils.startHomeActivity()
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            exitApp()
        } catch (e: SecurityException) {
            e.printStackTrace()
            exitApp()
        }
    }

    fun exitApp() {
        var mNowTime = System.currentTimeMillis()
        if (mNowTime - mPressedTime > 2000) {
            ToastUtils.showToast(R.string.exit_app_tips)
            mPressedTime = mNowTime
        } else {
            com.blankj.utilcode.util.AppUtils.exitApp()
        }
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
        //手动杀死app / 退出登录 / 重新登录
        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("1", "2"))
//        //清除sdk内部设备信息
//        ControlBleTools.getInstance().disconnect()
//        ControlBleTools.getInstance().release()
        if (CallBackUtils.uploadBigDataListener != null) {
            //处理三星手机重启应用进程不死导致继续断点续传
            CallBackUtils.uploadBigDataListener.onTimeout()
            ThemeManager.getInstance().protoHandler.removeCallbacksAndMessages(null)
            ThemeManager.getInstance().isSendProto4 = false
            ThemeManager.getInstance().isResumable = false
        }
    }
}