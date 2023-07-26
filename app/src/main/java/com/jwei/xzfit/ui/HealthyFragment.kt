package com.jwei.xzfit.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.TimeUtils
import com.zhapp.ble.BleCommonAttributes
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.databinding.FragmentHealthyBinding
import com.jwei.xzfit.databinding.ItemHealthyFragmentBinding
import com.jwei.xzfit.ui.adapter.MultiItemCommonAdapter
import com.jwei.xzfit.ui.healthy.bean.HealthyItemBean
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.ui.healthy.EditCardActivity
import com.jwei.xzfit.ui.healthy.history.*
import com.jwei.xzfit.ui.healthy.history.womenhealth.WomenHealthActivity
import com.jwei.xzfit.ui.livedata.RefreshHealthyFragment
import com.jwei.xzfit.ui.sport.SportRecordActivity
import com.jwei.xzfit.utils.*
import com.jwei.xzfit.viewmodel.DailyModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.*
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.FitnessDataCallBack
import com.zhapp.ble.callback.PhysiologicalCycleCallBack
import com.zhapp.ble.callback.SportParsingProgressCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.parsing.SportParsing
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.expansion.postDelay
import com.jwei.xzfit.ui.device.weather.utils.WeatherManagerUtils
import com.jwei.xzfit.ui.healthy.history.StressActivity
import com.jwei.xzfit.ui.healthy.bean.SyncDailyInfoBean
import com.jwei.xzfit.ui.healthy.ecg.EcgActivity
import com.jwei.xzfit.ui.healthy.history.womenhealth.WomenPeriodSettingActivity
import com.jwei.xzfit.ui.livedata.RefreshBatteryState
import com.jwei.xzfit.ui.livedata.RefreshHealthyMainData
import com.jwei.xzfit.ui.livedata.bean.BatteryBean
import com.jwei.xzfit.ui.user.bean.TargetBean
import com.jwei.xzfit.ui.user.utils.UnitConverUtils
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.LogUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.utils.manager.GoogleFitManager
import com.jwei.xzfit.viewmodel.EcgModel
import com.jwei.xzfit.viewmodel.SportModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.*


class HealthyFragment : BaseFragment<FragmentHealthyBinding, DailyModel>(FragmentHealthyBinding::inflate, DailyModel::class.java), View.OnClickListener {
    val TAG = HealthyFragment::class.java.simpleName
    private lateinit var mTargetBean: TargetBean
    private var mTotalDistance: String? = null//记录总距离
    var mTimerHandler = Handler(Looper.getMainLooper())
    private var refreshOnlineCount = 0
    private var isDeviceConnectSync = false

    private var myFitnessDataCallBack: MyFitnessDataCallBack? = null

    private var mySportParsingProgressCallBack: MySportParsingProgressCallBack? = null

    //数据同步中
    private var isDeviceSyncing = false

    companion object {
        var viewIsVisible = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            //设备已连接
            EventAction.ACTION_DEVICE_CONNECTED -> {
                LogUtils.e(TAG, "------ACTION_DEVICE_CONNECTED----------")
                //开启实时健康数据上报
                RealTimeRefreshDataUtils.openRealTime()
                //刷新设备语言
                SendCmdUtils.getDeviceLanguage()
                //更新界面
                binding.tvBluetoothState.visibility = View.VISIBLE
                binding.ivBluetoothState.visibility = View.GONE
                binding.ivBluetoothConnectState.visibility = View.VISIBLE
                binding.ivBluetoothConnectState.setImageResource(R.mipmap.un_bind_success)
                binding.tvBluetoothState.text = getString(R.string.device_connected)

                binding.lySyncing.visibility = View.GONE
                binding.layoutBattery.visibility = View.VISIBLE

                TimerUtils.isForUser = true
                isDeviceConnectSync = true

                binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
                SendCmdUtils.setUserInformation()
                autoRefresh()
            }
            //设备连接失败
            EventAction.ACTION_DEVICE_CONNECT_FAIL -> {
                binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "--")
                binding.roundView1.setProgress(0.0f)
                binding.roundView2.setProgress(0.0f)
                binding.roundView3.setProgress(0.0f)

                binding.ivBluetoothState.visibility = View.GONE
                binding.ivBluetoothConnectState.visibility = View.VISIBLE
                binding.layoutBattery.visibility = View.GONE
                binding.ivBluetoothConnectState.setImageResource(R.mipmap.icon_ref_connect)
                binding.tvBluetoothState.text = getString(R.string.device_connected_failed)
            }
            //未绑定设备
            EventAction.ACTION_NO_DEVICE_BINDING -> {
                binding.ivBluetoothState.visibility = View.GONE
                binding.ivBluetoothConnectState.visibility = View.GONE

                binding.ivNoDeviceBind.visibility = View.VISIBLE
                binding.lySyncing.visibility = View.VISIBLE
                binding.layoutBattery.visibility = View.GONE
                binding.tvCloseBt.visibility = View.GONE
                binding.tvBluetoothState.visibility = View.VISIBLE

                binding.tvDeviceName.text = ""
                binding.tvBluetoothState.text = getString(R.string.healthy_no_device_binding_tips)
                GlideApp.with(requireActivity()).load(R.mipmap.device_no_bind_right_img).into(binding.ivDeviceIcon)
            }
            //蓝牙状态改变
            EventAction.ACTION_BLE_STATUS_CHANGE -> {
                //开关打开
                if (msg.arg == BluetoothAdapter.STATE_ON) {
                    binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
                }
                //开关关闭
                else if (msg.arg == BluetoothAdapter.STATE_OFF) {
                    binding.ivBluetoothState.visibility = View.GONE
                    binding.tvBluetoothState.visibility = View.GONE
                    binding.ivNoDeviceBind.visibility = View.GONE
                    binding.ivBluetoothConnectState.visibility = View.GONE
                    binding.lySyncing.visibility = View.VISIBLE
                    binding.layoutBattery.visibility = View.GONE
                    binding.tvCloseBt.visibility = View.VISIBLE
                    binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")

                }
            }
            //修改性别
            EventAction.ACTION_SEX_CHANGE, EventAction.ACTION_NETWORK_CONNECTED, EventAction.ACTION_REF_SYNC -> {
                //是否同步中
                if (ControlBleTools.getInstance().isConnect && Global.IS_SYNCING_DATA) {
                    //设备同步中不重复获取日常和运动
                    return
                }
                TimerUtils.isForUser = true
                autoRefresh()
            }
            //刷新健康页设备图
            EventAction.ACTION_REFRESH_HEALTHY_PAGE_DEVICE_ICON -> {
                for (i in DeviceManager.dataList.indices) {
                    if (DeviceManager.dataList[i].deviceStatus == 1) {
                        for (j in Global.productList.indices) {
                            if (TextUtils.equals(DeviceManager.dataList[i].deviceType.toString(), Global.productList[j].deviceType)) {
                                val logo = Global.productList[j].homeLogo
                                if (TextUtils.isEmpty(logo)) {
                                    GlideApp.with(requireActivity()).load(R.mipmap.device_no_bind_right_img).into(binding.ivDeviceIcon)
                                } else {
                                    LogUtils.d("saveDeviceIcon", "saveDeviceIcon $logo")
                                    viewmodel.saveDeviceIcon(logo)
                                    GlideApp.with(requireActivity()).load(logo).into(binding.ivDeviceIcon)
                                }
                                binding.ivNoDeviceBind.visibility = View.GONE
                                binding.tvDeviceName.text = DeviceManager.dataList[i].deviceName
                                break
                            }
                        }
                        break
                    }
                }
            }
            //同步天气消息
            EventAction.ACTION_SYNC_WEATHER_INFO -> {
                if ((msg.obj as Boolean)) {
                    binding.lyRefresh.tag = SNYC_SUCCESS
                    mTimerHandler.removeCallbacksAndMessages(null)
                    mTimerHandler.post(refreshTimeRunnable)
                } else {
                    binding.lyRefresh.tag = SNYC_FAIL
                    mTimerHandler.removeCallbacksAndMessages(null)
                    mTimerHandler.post(refreshTimeRunnable)
                }
            }
            //刷新健康页面步数，距离，卡路里数据
            EventAction.ACTION_SYNC_DAILY_INFO -> {
                mTargetBean = TargetBean().getData()
                val info = msg.obj as SyncDailyInfoBean
                binding.tvStepCount.text = info.steps
                binding.tvCalories.text = info.calories
                mTotalDistance = info.distance
                binding.tvDistance.text = UnitConverUtils.showDistanceKmStyle(info.distance)
                binding.tvDistanceUnit.text = context?.let { it1 -> UnitConverUtils.showDistanceUnitStyle(it1) }
                if (!TextUtils.isEmpty(info.steps)) {
                    if (info.steps.trim().toInt() <= 0) {
                        binding.roundView1.setProgress(0.0f)
                    } else {
                        binding.roundView1.setProgress(info.steps.trim().toFloat() / mTargetBean.sportTarget.trim().toFloat())
                    }
                }
                if (!TextUtils.isEmpty(info.calories)) {
                    if (info.calories.trim().toInt() <= 0) {
                        binding.roundView2.setProgress(0.0f)
                    } else {
                        binding.roundView2.setProgress(info.calories.trim().toFloat() / mTargetBean.consumeTarget.trim().toFloat())
                    }
                }
                if (!TextUtils.isEmpty(info.distance)) {
                    if (info.distance.trim().toFloat() <= 0.0f) {
                        binding.roundView3.setProgress(0.0f)
                    } else {
                        binding.roundView3.setProgress(info.distance.trim().toFloat() / mTargetBean.getDistanceTargetMi().toInt())
                    }
                }
            }
            //更新目标设置刷新进度
            EventAction.ACTION_UPDATE_TARGET_INFO -> {
                mTargetBean = TargetBean().getData()
                val stepPosition = Global.healthyItemList.indexOfFirst {
                    it.topTitleText == BaseApplication.mContext.getString(
                        R.string.healthy_sports_list_step
                    )
                }
                val caloriesPosition = Global.healthyItemList.indexOfFirst {
                    it.topTitleText == BaseApplication.mContext.getString(
                        R.string.healthy_sports_list_calories
                    )
                }
                val distancePosition = Global.healthyItemList.indexOfFirst {
                    it.topTitleText == BaseApplication.mContext.getString(
                        R.string.healthy_sports_list_distance
                    )
                }
                val step = Global.healthyItemList[stepPosition].context
                val calories = Global.healthyItemList[caloriesPosition].context
                val distance = Global.healthyItemList[distancePosition].context

                if (!TextUtils.isEmpty(step)) {
                    if (step.trim().toInt() <= 0) {
                        binding.roundView1.setProgress(0.0f)
                    } else {
                        binding.roundView1.setProgress(step.trim().toFloat() / mTargetBean.sportTarget.trim().toFloat())
                    }
                }
                if (!TextUtils.isEmpty(calories)) {
                    if (calories.trim().toInt() <= 0) {
                        binding.roundView2.setProgress(0.0f)
                    } else {
                        binding.roundView2.setProgress(calories.trim().toFloat() / mTargetBean.consumeTarget.trim().toFloat())
                    }
                }
                if (!TextUtils.isEmpty(distance)) {
                    if (distance.trim().toFloat() <= 0.0f) {
                        binding.roundView3.setProgress(0.0f)
                    } else {
                        binding.roundView3.setProgress(distance.trim().toFloat() / mTargetBean.getDistanceTargetMi().toInt())
                    }
                }
            }
            //设备蓝牙状态改变
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                when (msg.arg) {
                    BleCommonAttributes.STATE_CONNECTING -> {
                        Log.w(TAG, "device connecting")
                        connectingView()
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        Log.w(TAG, "device disconnect")
                        if (AppUtils.isOpenBluetooth()) {
                            connectingView()
                        }
                        if (isDeviceSyncing) {
                            isDeviceSyncing = false
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("同步过程中蓝牙断开"), "1427", true)
                        }
                        //binding.lyRefresh.tag = SNYC_SUCCESS
                        mTimerHandler.removeCallbacksAndMessages(null)
                        mTimerHandler.post(refreshTimeRunnable)
                    }
                    BleCommonAttributes.STATE_TIME_OUT -> {
                        LogUtils.w(TAG, "device connect timeout")
                        //由DeviceFragment 重连设备
                    }
                }
            }
            //刷新设备电池信息
            EventAction.ACTION_REFRESH_BATTERY_INFO -> {
                val batteryInfo = msg.obj as RealTimeBean.DeviceBatteryInfo
                refreshBatteryInfo(batteryInfo.capacity.trim().toInt(), batteryInfo.chargeStatus.trim().toInt())
            }
        }
    }

    /**
     * 显示连接中状态
     */
    private fun connectingView() {
        binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
        binding.lySyncing.visibility = View.VISIBLE
        binding.ivBluetoothState.visibility = View.VISIBLE
        binding.tvBluetoothState.visibility = View.VISIBLE
        binding.tvBluetoothState.text = resources.getString(R.string.device_connecting)
        val animation = binding.ivBluetoothState.background as AnimationDrawable
        binding.ivBluetoothState.setImageDrawable(animation)
        animation.start()

        binding.ivBluetoothConnectState.visibility = View.GONE
        binding.ivNoDeviceBind.visibility = View.GONE
        binding.tvCloseBt.visibility = View.GONE
        binding.layoutBattery.visibility = View.GONE
    }

    /**
     * 刷新电池信息
     */
    private fun refreshBatteryInfo(capacity: Int, chargeStatus: Int) {
        binding.battery.setPre(capacity / 100f, chargeStatus == 1)
        if (chargeStatus == 1) {
            binding.tvPower.text = getString(R.string.device_battery_charge_state)
        } else {
            binding.tvPower.text = "$capacity${getString(R.string.healthy_sports_item_percent_sign)}"
        }
        RefreshBatteryState.postValue(BatteryBean(capacity, chargeStatus))
    }

    override fun setTitleId(): Int {
        return binding.topTitle.id
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        mTimerHandler.removeCallbacksAndMessages(null)
        myFitnessDataCallBack = null
        mySportParsingProgressCallBack = null
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            viewIsVisible = true
            RealTimeRefreshDataUtils.openRealTime()
        } else {
            viewIsVisible = false
            RealTimeRefreshDataUtils.closeRealTime()
        }
    }

    override fun initView() {
        super.initView()
        EventBus.getDefault().register(this)

        binding.rvHealthyFragment.apply {
            layoutManager = GridLayoutManager(this.context, 2)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
//        binding.ivEditCard.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG) //下划线
        setViewsClickListener(this, binding.ivEditCard, binding.lySyncingState, binding.lySyncing)

        binding.lyRefresh.setOnRefreshListener {
            LogUtils.e(TAG, "下拉刷新")
            TimerUtils.isForUser = true
            RealTimeRefreshDataUtils.isRefreshing = false
            autoRefresh()
        }
        binding.lyRefresh.setEnableLoadMore(false)
        binding.tvDistanceUnit.text = context?.let { it1 -> UnitConverUtils.showDistanceUnitStyle(it1) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionUtils.isGranted(*com.jwei.xzfit.utils.PermissionUtils.PERMISSION_BLE12)) {
                binding.ivBluetoothState.visibility = View.GONE
            }
        }
        //蓝牙未开启
        if (!AppUtils.isOpenBluetooth()) {
            binding.ivBluetoothState.visibility = View.GONE
            binding.tvBluetoothState.visibility = View.GONE
            binding.ivNoDeviceBind.visibility = View.GONE
            binding.ivBluetoothConnectState.visibility = View.GONE
            binding.lySyncing.visibility = View.VISIBLE
            binding.layoutBattery.visibility = View.GONE
            ControlBleTools.getInstance().disconnect()
            binding.tvCloseBt.visibility = View.VISIBLE
        }
        //蓝牙已开启
        else {
            connectingView()
            //网络未连接
            if (!NetworkUtils.isConnected()) {
                binding.tvBluetoothState.visibility = View.VISIBLE
                binding.ivBluetoothState.visibility = View.GONE
                binding.lySyncing.visibility = View.GONE
                binding.layoutBattery.visibility = View.GONE
                binding.battery.setPre(1f, false)
                binding.tvPower.text = "100%"
                binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
            }
            if (SpUtils.getValue(SpUtils.DEVICE_NAME, "").isEmpty()) {
                binding.lySyncing.visibility = View.GONE
            }
        }

        binding.tvDeviceName.text = SpUtils.getValue(SpUtils.DEVICE_NAME, "")
        //先加载缓存路径下的图片
        if (SpUtils.getValue(SpUtils.DEVICE_NAME, "").isNotEmpty()) {
            GlideApp.with(requireActivity())
                .load(FileUtils.getFileByPath(Global.DEVICE_ICON_PATH))
                .error(R.mipmap.device_no_bind_right_img)
                .into(binding.ivDeviceIcon)
        }
        //查询服务器最近一条运动记录
        SportModel().querySportRecordData("", true, 1, "1")
        //查询最近一次ecg数据
        if (Global.deviceSettingBean!!.dataRelated.ecg) {
            EcgModel().getEcgLastlyData()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun autoRefresh() {
        binding.lyRefresh.setEnableRefresh(false)
        binding.lyRefresh.complete()
        binding.lySyncingState.visibility = View.VISIBLE
        binding.tvSync.text = getString(R.string.healthy_sports_sync_tips)
        val animationDrawable = binding.ivSync.background as AnimationDrawable
        binding.ivSyncState.setImageDrawable(animationDrawable)
        animationDrawable.start()
        onRefreshAction(userVisibleHint)
        Global.IS_SYNCING_DATA = true
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYNCING_DATA, true, -1))
    }

    private val SNYC_SUCCESS = 0
    private val SNYC_FAIL = 1
    private val refreshTimeRunnable = Runnable {
        try {
            if (binding.lyRefresh.tag != null) {
                when (binding.lyRefresh.tag) {
                    SNYC_SUCCESS -> {
                        com.blankj.utilcode.util.LogUtils.d("同步成功")
                        if (ControlBleTools.getInstance().isConnect) {
                            binding.ivSyncState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.un_bind_success))
                            binding.tvSync.text = getString(R.string.sync_success_tips)
                            postDelay(1000) {
                                binding.lySyncingState.visibility = View.GONE
                            }
                            //上传未上传的日常数据至服务器
                            viewmodel.upLoadFitnessData()
                        } else {
                            //同步服务器数据不需要同步结果
                            binding.lySyncingState.visibility = View.GONE
                        }
                        if (isDeviceConnectSync) {
                            isDeviceConnectSync = false
                            EventBus.getDefault().post(EventMessage(EventAction.ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE))
                        }
                        //上报一次用户行为
                        AppTrackingManager.postBehaviorTracking()
                        ErrorUtils.clearErrorSync()
                    }
                    //同步失败
                    else -> {
                        com.blankj.utilcode.util.LogUtils.d("同步失败")
                        ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                        if (ControlBleTools.getInstance().isConnect) {
                            binding.ivSyncState.setImageDrawable(ContextCompat.getDrawable(BaseApplication.mContext, R.mipmap.icon_ref_connect))
                            binding.tvSync.text = getString(R.string.sync_fail_tips)
                        } else {
                            //同步服务器数据不需要同步结果
                            binding.lySyncingState.visibility = View.GONE
                        }
                    }
                }
            }
            RealTimeRefreshDataUtils.isRefreshing = false
            binding.lyRefresh.setEnableRefresh(true)
            Global.IS_SYNCING_DATA = false
            isDeviceSyncing = false
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYNCING_DATA, false, binding.lyRefresh.tag as Int))
            binding.lyRefresh.complete()
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun onRefreshAction(isUserVisibleHint: Boolean = false) {
        if (TimerUtils.calcTimer(System.currentTimeMillis())) {
            if (ControlBleTools.getInstance().isConnect) {
                if (!RealTimeRefreshDataUtils.isRefreshing) {
                    if (isUserVisibleHint) { //页面可见时才记录同步断连异常
                        isDeviceSyncing = true
                    }
                    RealTimeRefreshDataUtils.openRealTime()
                    SendCmdUtils.refreshSend()
                }
            } else {
                refreshOnlineCount = 0
                viewmodel.refreshHealthyFromOnline()
            }
            SendCmdUtils.getSportData()
            //暂时注释代码
//            if (Global.deviceSettingBean!!.dataRelated.ecg) {
//                EcgModel().updateHealthFragmentUi()
//            }
        } else {
            ThreadUtils.runOnUiThreadDelayed({
                binding.lyRefresh.tag = SNYC_SUCCESS
                mTimerHandler.removeCallbacksAndMessages(null)
                mTimerHandler.post(refreshTimeRunnable!!)
            }, 3000)
        }
        startTimerDelayed()
    }

    private val timerCount = 11 * 1000L
    private fun startTimerDelayed() {
        mTimerHandler.postDelayed(refreshTimeRunnable, 3 * timerCount)
    }

    private fun startTimer() {
        mTimerHandler.removeCallbacksAndMessages(null)
        mTimerHandler.post(refreshTimeRunnable)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initData() {
        super.initData()
        myFitnessDataCallBack = MyFitnessDataCallBack(this)
        CallBackUtils.fitnessDataCallBack = myFitnessDataCallBack
        mySportParsingProgressCallBack = MySportParsingProgressCallBack(this)
        CallBackUtils.sportParsingProgressCallBack = mySportParsingProgressCallBack
        observe()
    }

    class MyFitnessDataCallBack(fragment: HealthyFragment) : FitnessDataCallBack {
        private var wrFragment: WeakReference<HealthyFragment>? = null
        private var TAG = "HealthyFragment"
        private var trackingLog = TrackingLog.getAppTypeTrack("日常数据进度")

        init {
            wrFragment = WeakReference(fragment)
            if (wrFragment?.get() == null) {
                LogUtils.e(TAG, "HealthyFragment is Null")
            }
        }


        override fun onProgress(progress: Int, total: Int) {
            LogUtils.e(TAG, "daily data onProgress  progress = $progress  total = $total")
            ErrorUtils.onLogResult("daily data onProgress  progress = $progress  total = $total")
            if (progress == 0) {
                trackingLog.startTime = TrackingLog.getNowString()
                trackingLog.log = "daily data onProgress  progress = $progress  total = $total"
            } else if (progress == total) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, trackingLog)
            } else {
                trackingLog.log += "\ndaily data onProgress  progress = $progress  total = $total"
            }


            wrFragment?.get()?.apply {
                if (progress < total) {
                    mTimerHandler.removeCallbacksAndMessages(null)
                    binding.lyRefresh.tag = SNYC_FAIL
                    startTimerDelayed()
                } else if (progress == total) {

                    LogUtils.e(TAG, "getFitnessSportIdsData")
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getStartTypeTrack("同步运动"), isStart = true)
                    val trackingLog = TrackingLog.getDevTyepTrack("轮询ID列表请求运动数据", "获取设备运动数据ids", "GET_FITNESS_SPORT_ID_LIST")
                    //获取运动数据
                    ControlBleTools.getInstance().getFitnessSportIdsData(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                        override fun onState(state: SendCmdState) {
                            trackingLog.endTime = TrackingLog.getNowString()
                            trackingLog.log = "state : $state"
                            if (state != SendCmdState.SUCCEED && ControlBleTools.getInstance().isConnect) { //失败直接给健康数据处理
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog.apply {
                                    log += "\n 获取运动数据超时/失败"
                                }, "1512", isEnd = true)
                                if (!SportParsing.getInstance().isSyncingSportData) { //不在同步运动中
                                    if (!WeatherManagerUtils.syncWeather()) {
                                        //天气开关没有开启时操作
                                        binding.lyRefresh.tag = SNYC_SUCCESS
                                        mTimerHandler.removeCallbacksAndMessages(null)
                                        mTimerHandler.post(refreshTimeRunnable!!)
                                    }
                                }
                            } else {
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, trackingLog)
                            }

                            isDeviceSyncing = false
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getEndTypeTrack("同步数据"), isEnd = true)
                        }
                    })
                }
            }

        }

        override fun onDailyData(bean: DailyBean) {
            LogUtils.e(TAG, "onDailyData  ${bean.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onDailyData  ${bean.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveDailyData(bean)
            //同步Google fit
            GoogleFitManager.postGooglefitData(bean)
        }

        override fun onSleepData(data: SleepBean) {
            LogUtils.e(TAG, "onSleepData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onSleepData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveSleepData(data)
        }

        override fun onContinuousHeartRateData(data: ContinuousHeartRateBean) {
            LogUtils.e(TAG, "onContinuousHeartRateData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onContinuousHeartRateData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveHeartRateData(data)
        }

        override fun onOfflineHeartRateData(data: OfflineHeartRateBean) {
            LogUtils.e(TAG, "onOfflineHeartRateData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onOfflineHeartRateData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveSingleHeartRateData(data)
        }

        override fun onContinuousBloodOxygenData(data: ContinuousBloodOxygenBean) {
            LogUtils.e(TAG, "onContinuousBloodOxygenData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onContinuousBloodOxygenData  ${data.toString()}"
            })
        }

        override fun onOfflineBloodOxygenData(data: OfflineBloodOxygenBean) {
            LogUtils.e(TAG, "onOfflineBloodOxygenData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onOfflineBloodOxygenData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveSingleBloodOxygenData(data)
        }

        override fun onContinuousPressureData(data: ContinuousPressureBean?) {
            LogUtils.e(TAG, "onContinuousPressureData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onContinuousPressureData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.savePressureData(data)
        }

        override fun onOfflinePressureData(data: OfflinePressureDataBean?) {
            LogUtils.e(TAG, "onOfflinePressureData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onOfflinePressureData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveSinglePressureData(data)
        }

        override fun onContinuousTemperatureData(data: ContinuousTemperatureBean?) {
            LogUtils.e(TAG, "onContinuousTemperatureData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onContinuousTemperatureData  ${data.toString()}"
            })
        }

        override fun onOfflineTemperatureData(data: OfflineTemperatureDataBean?) {
            LogUtils.e(TAG, "onOfflineTemperatureData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onOfflineTemperatureData  ${data.toString()}"
            })
        }

        override fun onEffectiveStandingData(data: EffectiveStandingBean) {
            LogUtils.e(TAG, "onEffectiveStandingData  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onEffectiveStandingData  ${data.toString()}"
            })
            wrFragment?.get()?.viewmodel?.saveEffectiveStandData(data)
        }

        override fun onActivityDurationData(data: ActivityDurationBean?) {
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onActivityDurationData  ${data.toString()}"
            })
        }

        override fun onOffEcgData(data: OffEcgDataBean?) {
            LogUtils.e(TAG, "onOffEcgDataBean  ${data.toString()}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onOffEcgDataBean  ${data.toString()}"
            })
            if (data != null) {
                try {
                    var isSuccess = EcgModel().saveOffEcgData(data)
                    LogUtils.i(TAG, "onOffEcgDataBean isSuccess = $isSuccess")
                } catch (e: Exception) {
                    LogUtils.e(TAG, "onOffEcgDataBean Exception")
                }
            }
        }

        override fun onExaminationData(data: ExaminationBean?) {
            LogUtils.e(TAG, "onExaminationDataBeans  ${GsonUtils.toJson(data)}")
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_FITNESS, TrackingLog.getAppTypeTrack("日常数据记录").apply {
                log = "onExaminationDataBeans  ${data.toString()}"
            })
        }

        override fun onRingTodayActiveTypeData(bean: TodayActiveTypeData?) {
        }

        override fun onRingOverallDayMovementData(bean: OverallDayMovementData?) {
        }

        override fun onRingTodayRespiratoryRateData(bean: TodayRespiratoryRateData?) {
        }

        override fun onRingHealthScore(bean: RingHealthScoreBean?) {
        }

        override fun onRingSleepResult(bean: RingSleepResultBean?) {
        }

        override fun onRingSleepNAP(list: MutableList<RingSleepNapBean>?) {
        }
    }

    class MySportParsingProgressCallBack(fragment: HealthyFragment) : SportParsingProgressCallBack {
        private var wrFragment: WeakReference<HealthyFragment>? = null
        private var TAG = "HealthyFragment"

        init {
            wrFragment = WeakReference(fragment)
            if (wrFragment?.get() == null) {
                LogUtils.e(TAG, "HealthyFragment is Null")
            }
        }

        override fun onProgress(progress: Int, total: Int) {
            LogUtils.e(TAG, "sport data onProgress  progress = $progress  total = $total")
            ErrorUtils.onLogResult("sport data onProgress  progress = $progress  total = $total")
            wrFragment?.get()?.apply {
                if (progress < total) {
                    mTimerHandler.removeCallbacksAndMessages(null)
                    binding.lyRefresh.tag = SNYC_FAIL
                    startTimerDelayed()
                } else if (progress == total) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_SYNC_SPORT, TrackingLog.getEndTypeTrack("同步运动").apply {
                        log = "sport data total = $total"
                    }, isEnd = true)
                    if (!WeatherManagerUtils.syncWeather()) {
                        //天气开关没有开启时操作
                        binding.lyRefresh.tag = SNYC_SUCCESS
                        mTimerHandler.removeCallbacksAndMessages(null)
                        mTimerHandler.post(refreshTimeRunnable!!)
                    }
                } else {
                    ErrorUtils.onLogResult("sport data onProgress progress > total")
                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_SYNC_TIME_OUT)
                    mTimerHandler.removeCallbacksAndMessages(null)
                    binding.lyRefresh.tag = SNYC_FAIL
                    mTimerHandler.post(refreshTimeRunnable!!)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() {
        //刷新数据
        RefreshHealthyFragment.observe(this, Observer {
            LogUtils.i(TAG, "RefreshHealthyFragment")
            if (it != null) {
                if (it) {
                    binding.rvHealthyFragment.adapter?.notifyDataSetChanged()
                    switchDistanceStyle()
                }
            }
        })

        RefreshHealthyMainData.observe(this, Observer {
            LogUtils.i(TAG, "RefreshHealthyMainData")
            if (it != null) {
                if (it) {
                    val stepPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_step
                        )
                    }
                    val caloriesPosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_calories
                        )
                    }
                    val distancePosition = Global.healthyItemList.indexOfFirst {
                        it.topTitleText == BaseApplication.mContext.getString(
                            R.string.healthy_sports_list_distance
                        )
                    }

                    val step = if (stepPosition != -1) {
                        Global.healthyItemList[stepPosition].context
                    } else {
                        "0"
                    }
                    val calories = if (caloriesPosition != -1) {
                        Global.healthyItemList[caloriesPosition].context
                    } else {
                        "0"
                    }
                    val distance = if (distancePosition != -1) {
                        Global.healthyItemList[distancePosition].context
                    } else {
                        "0"
                    }

                    binding.rvHealthyFragment.adapter?.notifyDataSetChanged()
                    //如果设备未连接，圆环处使用服务器数据
                    if (!ControlBleTools.getInstance().isConnect) {
                        val syncDailyBean = SyncDailyInfoBean()
                        syncDailyBean.steps = step
                        syncDailyBean.calories = calories
                        syncDailyBean.distance = distance
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYNC_DAILY_INFO, syncDailyBean))
                    }
                }

                if (RealTimeRefreshDataUtils.isRefreshing) {
                    autoRefresh()
                }
                //连接设备时，同步结果由设备同步数据决定; 否则由同步网络结果决定
                if (!ControlBleTools.getInstance().isConnect) {
                    binding.lyRefresh.tag = if (it) SNYC_SUCCESS else SNYC_FAIL
                    mTimerHandler.removeCallbacksAndMessages(null)
                    mTimerHandler.post(refreshTimeRunnable!!)
                }
            }
        })
    }

    //设置公英制之后，属性距离单位
    private fun switchDistanceStyle() {
        if (!TextUtils.isEmpty(mTotalDistance) && mTotalDistance?.toFloat()!! > 0)
            binding.tvDistance.text = UnitConverUtils.showDistanceKmStyle(mTotalDistance!!)
        binding.tvDistanceUnit.text = context?.let { it1 -> UnitConverUtils.showDistanceUnitStyle(it1) }
    }

    private fun initAdapter(): MultiItemCommonAdapter<HealthyItemBean, ItemHealthyFragmentBinding> {
        return object :
            MultiItemCommonAdapter<HealthyItemBean, ItemHealthyFragmentBinding>(Global.healthyItemList) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemHealthyFragmentBinding {
                return ItemHealthyFragmentBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("SetTextI18n")
            override fun convert(v: ItemHealthyFragmentBinding, t: HealthyItemBean, position: Int) {
                v.itemLy.setBackgroundResource(t.bg)
                v.ivivHealthyItemTop.setImageResource(t.topTitleImg)
                v.tvHealthyItemTop.text = t.topTitleText

                when (t.topTitleText) {
                    //步数
                    getString(R.string.healthy_sports_list_step) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.VISIBLE
                            t.context = "0"
                            v.healthyItemCenter.text = dealItemString(t.context, value2 = " ", value4 = getString(R.string.unit_step))
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)

                        } else {
                            if (t.context.trim().toInt() <= 0) {
                                v.healthyItemCenter.text = dealItemString(t.context, value2 = " ", value4 = getString(R.string.unit_step))
                            } else {
                                v.healthyItemCenter.text = dealItemString(t.context, value2 = " ", value4 = getString(R.string.unit_steps))
                            }
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)
                        }
                    }
                    //距离
                    getString(R.string.healthy_sports_list_distance) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim().toFloat() == 0.00f) {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.VISIBLE
                            t.context = "0.00"
                            v.healthyItemCenter.text = context?.let { UnitConverUtils.showDistanceUnitStyle(it) }?.let {
                                dealItemString(t.context, value2 = " ", value4 = it)
                            }
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)
                        } else {
                            v.healthyItemCenter.text = context?.let { UnitConverUtils.showDistanceUnitStyle(it) }?.let {
                                dealItemString(UnitConverUtils.showDistanceKmStyle(t.context), value2 = " ", value4 = it)
                            }
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)
                        }

                    }
                    //卡路里
                    getString(R.string.healthy_sports_list_calories) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.VISIBLE
                            t.context = "0"
                            v.healthyItemCenter.text = dealItemString(t.context, value2 = " ", value4 = getString(R.string.unit_calories))
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)
                        } else {
                            v.healthyItemCenter.text = dealItemString(t.context, value2 = " ", value4 = getString(R.string.unit_calories))
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sports_today)
                        }
                    }
                    //睡眠
                    getString(R.string.healthy_sports_list_sleep) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString(
                                "${t.context.trim().toInt() / 60}", getString(R.string.hours_text),
                                "${t.context.trim().toInt() % 60}", getString(R.string.minutes_text)
                            )
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_sleep_duration)
                        }
                    }
                    //心率
                    getString(R.string.healthy_sports_list_heart) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString("${t.context}", value2 = " ", value4 = getString(R.string.hr_unit_bpm))
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_last_time)
                        }
                    }
                    //血氧
                    getString(R.string.healthy_sports_list_blood_oxygen) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString("${t.context}", getString(R.string.healthy_sports_item_percent_sign))
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_last_time)
                        }

                    }
                    //运动记录
                    getString(R.string.healthy_sports_list_sport_record) -> {
                        if (!isNoData(v.healthyItemBottom, t.context)) {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemSportType.visibility = View.GONE
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text =
                                dealItemString(t.context, "  ", "", getString(R.string.unit_calories))
                            v.healthyItemBottom.text = SpannableStringTool.get()
                                .append(TimeUtils.millis2String(t.bottomText.toLong(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("MM/dd")))
                                .setFontSize(12f)
                                .setForegroundColor(Color.WHITE)
                                .append("   ")
                                .append(TimeUtils.millis2String(t.bottomText.toLong(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("HH:mm")))
                                .setFontSize(12f)
                                .setForegroundColor(Color.WHITE)
                                .create()
                            v.healthyItemSportType.visibility = View.VISIBLE
                            v.healthyItemSportType.text = t.subTitleText
                        }
                    }
                    //女性健康
                    getString(R.string.healthy_sports_list_women_health) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString(t.context, "")
                            v.healthyItemBottom.text = "${t.bottomText}${t.bottomText2.trim()}${getString(R.string.women_health_day_text1)}"

                        }
                    }
                    //有效站立
                    getString(R.string.healthy_sports_list_effective_stand) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString(t.context, "")
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_last_time)
                        }
                    }
                    //压力
                    getString(R.string.healthy_pressure_title) -> {
                        v.healthyItemSportType.visibility = View.GONE
                        if (!isNoData(v.healthyItemBottom, t.context) || t.context.trim() == "0") {
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        } else {
                            v.healthyItemCenter.text = dealItemString(t.context, "")
                            v.healthyItemBottom.text = getString(R.string.healthy_sports_item_last_time)
                        }
                    }
                    //心电
                    getString(R.string.healthy_ecg_title) -> {
                        LogUtils.i(TAG, "刷新心电数据 = t.context = ${t.context}  t.bottomText = ${t.bottomText}")
                        v.healthyItemSportType.visibility = View.GONE
                        //有数据
                        if (t.context.isNotEmpty()) {
                            v.healthyItemBottom.visibility = View.VISIBLE
                            v.healthyItemCenter.visibility = View.VISIBLE
                            LogUtils.i(TAG, "刷新心电数据 = 有数据 = t.context = ${t.context}  t.bottomText = ${t.bottomText}")
                            v.healthyItemCenter.text = dealItemString(t.context, "  ", "", getString(R.string.hr_unit_bpm))
                            v.healthyItemBottom.text = SpannableStringTool.get()
                                .append(TimeUtils.millis2String(t.bottomText.toLong(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("MM/dd")))
                                .setFontSize(12f)
                                .setForegroundColor(Color.WHITE)
                                .append("   ")
                                .append(TimeUtils.millis2String(t.bottomText.toLong(), com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("HH:mm")))
                                .setFontSize(12f)
                                .setForegroundColor(Color.WHITE)
                                .create()
                        }
                        //无数据
                        else {
                            LogUtils.i(TAG, "刷新心电数据 = 无数据")
                            v.healthyItemCenter.visibility = View.VISIBLE
                            v.healthyItemCenter.text = "${getString(R.string.healthy_sports_no_string_tips)}${getString(R.string.healthy_sports_data_string_tips)}"
                            v.healthyItemBottom.visibility = View.GONE
                        }
                    }
                }

                setViewsClickListener({ itemClick(t.topTitleText) }, v.itemLy)
            }

            override fun getItemType(t: HealthyItemBean): Int {
                return 0
            }

        }
    }

    private fun dealItemString(
        value1: String,
        value2: String = "",
        value3: String = "",
        value4: String = ""
    ): SpannableStringBuilder {
        val context = this.context ?: return SpannableStringTool.get().append("").create()
        return SpannableStringTool.get()
            .append(value1)
            .setFontSize(26f)
            .setForegroundColor(ContextCompat.getColor(context, R.color.color_171717))
            .append(value2)
            .setFontSize(16f)
            .setForegroundColor(ContextCompat.getColor(context, R.color.color_878787))
            .append(value3)
            .setFontSize(26f)
            .setForegroundColor(ContextCompat.getColor(context, R.color.color_171717))
            .append(value4)
            .setFontSize(16f)
            .setForegroundColor(ContextCompat.getColor(context, R.color.color_878787))
            .create()
    }

    private fun isNoData(view: View, data: String): Boolean {
        return if (data.isNullOrEmpty()) {
            view.visibility = View.GONE
            false
        } else {
            view.visibility = View.VISIBLE
            true
        }
    }

    private fun itemClick(name: String) {
        when (name) {
            //步数
            getString(R.string.healthy_sports_list_step) -> {
                activity?.startActivity(
                    Intent(activity, DailyDataActivity::class.java)
                        .putExtra(DailyModel().dataTypeTag, DailyDataActivity.DailyDataType.STEPS)
                        .putExtra(DailyModel().currentStepTag, RealTimeRefreshDataUtils.realTimeStep)
                        .putExtra(DailyModel().currentCaloriesTag, RealTimeRefreshDataUtils.realTimeCalories)
                        .putExtra(DailyModel().currentDistanceTag, RealTimeRefreshDataUtils.realTimeDistance)
                )
            }
            //距离
            getString(R.string.healthy_sports_list_distance) -> {
                activity?.startActivity(
                    Intent(activity, DailyDataActivity::class.java)
                        .putExtra(DailyModel().dataTypeTag, DailyDataActivity.DailyDataType.DISTANCE)
                        .putExtra(DailyModel().currentStepTag, RealTimeRefreshDataUtils.realTimeStep)
                        .putExtra(DailyModel().currentCaloriesTag, RealTimeRefreshDataUtils.realTimeCalories)
                        .putExtra(DailyModel().currentDistanceTag, RealTimeRefreshDataUtils.realTimeDistance)
                )
            }
            //卡路里
            getString(R.string.healthy_sports_list_calories) -> {
                activity?.startActivity(
                    Intent(activity, DailyDataActivity::class.java)
                        .putExtra(DailyModel().dataTypeTag, DailyDataActivity.DailyDataType.CALORIES)
                        .putExtra(DailyModel().currentStepTag, RealTimeRefreshDataUtils.realTimeStep)
                        .putExtra(DailyModel().currentCaloriesTag, RealTimeRefreshDataUtils.realTimeCalories)
                        .putExtra(DailyModel().currentDistanceTag, RealTimeRefreshDataUtils.realTimeDistance)
                )
            }
            //睡眠
            getString(R.string.healthy_sports_list_sleep) -> {
                activity?.startActivity(Intent(activity, SleepHistoryActivity::class.java))
            }
            //心率
            getString(R.string.healthy_sports_list_heart) -> {
                activity?.startActivity(Intent(activity, HeartRateActivity::class.java))
            }
            //血氧
            getString(R.string.healthy_sports_list_blood_oxygen) -> {
                activity?.startActivity(Intent(activity, BloodOxygenActivity::class.java))
            }
            //运动记录
            getString(R.string.healthy_sports_list_sport_record) -> {
                activity?.startActivity(Intent(activity, SportRecordActivity::class.java))
            }
            //女性健康
            getString(R.string.healthy_sports_list_women_health) -> {
                if (ControlBleTools.getInstance().isConnect) {
                    val dialog = DialogUtils.showLoad(activity)
                    dialog.show()
                    CallBackUtils.physiologicalCycleCallBack = MyPhysiologicalCycleCallBack(this, dialog)
                    ControlBleTools.getInstance().getPhysiologicalCycle(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                        override fun onState(state: SendCmdState) {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    })
                } else {
                    ToastUtils.showToast(R.string.device_no_connection)
                }
            }
            //有效站立
            getString(R.string.healthy_sports_list_effective_stand) -> {
                activity?.startActivity(Intent(activity, EffectiveStandActivity::class.java))
            }
            //心电
            getString(R.string.healthy_ecg_title) -> {
                activity?.startActivity(Intent(activity, EcgActivity::class.java))
            }
            //连续压力
            getString(R.string.healthy_pressure_title) -> {
                if (Global.deviceSettingBean!!.dataRelated.offline_pressure) {
                    activity?.startActivity(Intent(activity, OfflineStressActivity::class.java))
                } else if (Global.deviceSettingBean!!.dataRelated.pressure) {
                    activity?.startActivity(Intent(activity, StressActivity::class.java))
                }
            }
        }
    }

    class MyPhysiologicalCycleCallBack(fragment: HealthyFragment, dialog: Dialog) : PhysiologicalCycleCallBack {
        private var wrFragment: WeakReference<HealthyFragment>? = null
        private var wrDialog: WeakReference<Dialog>? = null

        init {
            wrFragment = WeakReference(fragment)
            wrDialog = WeakReference(dialog)
        }

        override fun onPhysiologicalCycleResult(bean: PhysiologicalCycleBean) {
            wrFragment?.get()?.let { fragment ->
                if (wrDialog?.get()?.isShowing == true) {
                    wrDialog?.get()?.dismiss()
                }
                Global.physiologicalCycleBean = bean
                com.blankj.utilcode.util.LogUtils.d("PhysiologicalCycleBean -->" + Global.physiologicalCycleBean)
                LogUtils.e("PhysiologicalCycleBean -->", " " + Global.physiologicalCycleBean)
                if (bean.preset) {
                    fragment.activity?.startActivity(Intent(fragment.activity, WomenHealthActivity::class.java))
                } else {
                    fragment.activity?.startActivity(
                        Intent(fragment.activity, WomenPeriodSettingActivity::class.java)
                            .putExtra("type", 1)
                    )
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            //卡片排序
            R.id.ivEditCard -> {
                startActivityForResult(
                    Intent(activity, EditCardActivity::class.java),
                    HomeActivity.EDIT_CARD_ITEM_REQUEST_CODE
                )
            }
            //设备连接诶失败
            R.id.lySyncing -> {
                if (binding.tvBluetoothState.text.toString().trim() == getString(R.string.device_connected_failed) ||
                    binding.tvBluetoothState.text.toString().trim() == getString(R.string.device_no_connection)
                ) {
                    ControlBleTools.getInstance().disconnect()
                    SendCmdUtils.connectDevice(SpUtils.getValue(SpUtils.DEVICE_NAME, ""), SpUtils.getValue(SpUtils.DEVICE_MAC, ""))
                }
            }
            R.id.lySyncingState -> {
                TimerUtils.isForUser = true
                autoRefresh()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != AppCompatActivity.RESULT_OK) return
        when (requestCode) {
            HomeActivity.EDIT_CARD_ITEM_REQUEST_CODE -> {
                binding.rvHealthyFragment.adapter?.notifyDataSetChanged()
            }
        }
    }
}