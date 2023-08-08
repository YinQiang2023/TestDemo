package com.smartwear.xzfit.ui.healthy.ecg

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.TimeUtils
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.zh.ble.wear.protobuf.SportingProtos
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.EcgCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityEcgBinding
import com.smartwear.xzfit.databinding.ItemEcgBinding
import com.smartwear.xzfit.db.model.Ecg
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.ui.adapter.MultiItemCommonAdapter
import com.smartwear.xzfit.utils.DateUtils
import com.smartwear.xzfit.utils.LogUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.EcgModel
import java.util.*

class EcgActivity : BaseActivity<ActivityEcgBinding, EcgModel>(ActivityEcgBinding::inflate, EcgModel::class.java), View.OnClickListener {
    private val TAG: String = EcgActivity::class.java.simpleName
    override fun setTitleId(): Int = binding.title.layoutTitle.id

    //是否手动改变
    private var isManual = true

    //选中的日期
    private var mSelectionDate = ""

    //选中过的日期
    private var mSDates = mutableListOf<String>()

    //当前查询页
    private var pageIndex = 1

    //日期选择间隔，避免两次日历变化导致第一次无效的请求
    private val AVOID_REPEATER_DATE_CHANGE = 200L


    override fun initView() {
        super.initView()
        binding.title.tvCenterTitle.text = getString(R.string.healthy_ecg_title)
        binding.title.layoutRight.visibility = View.VISIBLE
        binding.title.tvRIght.visibility = View.GONE
        binding.title.tvRIght.text = getString(R.string.healthy_ecg_title_right_tx)
        viewModel.loadDialog = DialogUtils.showLoad(this)
        setViewsClickListener(this, binding.lyDate, binding.title.tvRIght, binding.btnStartTest)

        mSelectionDate = TimeUtils.getNowString(com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat(DateUtils.TIME_YYYY_MM_DD))
        binding.tvDate.text = mSelectionDate

        //选择日期刷新
        binding.calendarView.setOnCalendarSelectListener(object :
            CalendarView.OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: Calendar?) {
            }

            override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                val date = DateUtils.FormatDateYYYYMMDD(calendar)
                mSelectionDate = date
                if (isClick && binding.calendarLayout.isExpand) {
                    binding.calendarLayout.shrink()
                    isManual = true
                    rotateArrow()
                }
                calendarSelectHandler.removeCallbacksAndMessages(null)
                calendarSelectHandler.postDelayed(calendarSelectRunnable, AVOID_REPEATER_DATE_CHANGE)

                getData()

            }
        })

        binding.calendarView.setOnViewChangeListener(object : CalendarView.OnViewChangeListener {
            override fun onViewChange(isMonthView: Boolean) {
                if (isManual) {
                    isManual = false
                    return
                }
                rotateArrow()
            }
        })


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@EcgActivity)
            adapter = initAdapter()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.lyDate.id -> {
                isManual = true
                rotateArrow()
                if (binding.calendarLayout.isExpand) {
                    binding.calendarLayout.shrink()
                } else {
                    binding.calendarLayout.expand()
                }
            }

            binding.title.tvRIght.id -> {
                startActivity(Intent(this, EcgCalibrationActivity::class.java))
            }

            //开始测量
            binding.btnStartTest.id -> {
                //判断是否已连接
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }
                requestEcgMeasurement()

            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (EcgUtils.isAutoRefresh) {
            EcgUtils.isAutoRefresh = false
            getData()
        }

        CallBackUtils.ecgCallBack = object : EcgCallBack {
            override fun onEcgCommandSet(command: Int) {
                when (command) {
                    SportingProtos.SEEcgData.SEECGRespond.OK_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 可以正常响应--->进行下一步")
                        viewModel.dismissDialog()
                        startActivity(Intent(this@EcgActivity, EcgMeasureActivity::class.java))
                    }
                    SportingProtos.SEEcgData.SEECGRespond.BUSY_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备正忙，无法正常响应")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(ActivityUtils.getTopActivity(), "", "当前设备正忙，请稍后重试", getString(R.string.dialog_confirm_btn), null)
                    }
                    SportingProtos.SEEcgData.SEECGRespond.CHARGING_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备正忙，设备充电中")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.ota_device_busy_tips), getString(R.string.dialog_confirm_btn), null
                        )
                    }
                    SportingProtos.SEEcgData.SEECGRespond.LOW_BATTERY_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备低电量")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.ota_device_low_power_tips), getString(R.string.dialog_confirm_btn), null
                        )
                    }
//                    SportingProtos.SEEcgData.SEECGRespond.END_MEASUREMENT_DATA_OK_VALUE -> {
//                        LogUtils.i(TAG, "onEcgCommandSet command = ecg结束测量数据正常结束")
//                    }
//                    SportingProtos.SEEcgData.SEECGRespond.END_MEASUREMENT_DATA_ERROR_VALUE -> {
//                        LogUtils.i(TAG, "onEcgCommandSet command = ecg结束测量数据异常结束")
//                    }
                    SportingProtos.SEEcgData.SEECGRespond.UNKNOWN_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 未知")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.send_device_cmd_tip_unknown), getString(R.string.dialog_confirm_btn), null
                        )
                    }
                }
            }

            override fun onEcgRespond(command: Int) {
                when (command) {
//                    SportingProtos.SEEcgData.SEECGCommand.ECG_REQUEST_MEASUREMENT_VALUE -> {
//                        LogUtils.i(TAG, "onEcgRespond command = 请求心电请求")
//                    }
//                    SportingProtos.SEEcgData.SEECGCommand.ECG_START_MEASUREMENT_VALUE -> {
//                        LogUtils.i(TAG, "onEcgRespond command = 开始测量")
//                    }
//                    SportingProtos.SEEcgData.SEECGCommand.ECG_ABNORMAL_END_MEASUREMENT_VALUE -> {
//                        LogUtils.i(TAG, "onEcgRespond command = 提前结束测量")
//                    }
//                    SportingProtos.SEEcgData.SEECGCommand.ECG_END_MEASUREMENT_VALUE -> {
//                        LogUtils.i(TAG, "onEcgRespond command = 停止测量")
//                    }
//                    SportingProtos.SEEcgData.SEECGCommand.ECG_MANUAL_END_MEASUREMENT_VALUE -> {
//                        LogUtils.i(TAG, "onEcgRespond command = 手动结束测量")
//                    }

                }
            }

            override fun onEcgDataSend(ecgData: IntArray) {
//                LogUtils.i(TAG, "onEcgRespond ecgData.len = " + ecgData.size)
//                var result_data: Any = ArrayList<Any?>()
            }
        }
    }

    private fun requestEcgMeasurement() {
        //询问状态
        ControlBleTools.getInstance().requestEcgMeasurement(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                viewModel.dismissDialog()
                ToastUtils.showSendCmdStateTips(state)
            }
        });
        viewModel.showDialog()
    }

    override fun initData() {
        super.initData()
        getData()
        viewModel.uploadEcgListToService()
        viewModel.getEcgListByDay.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                viewModel.dismissDialog()
                loadData(mSelectionDate)
                when (it) {
                    HttpCommonAttributes.REQUEST_REGISTERED -> {
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER,
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                }
            }
        })

        viewModel.getEcgDetailedData.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                viewModel.dismissDialog()
                startActivity(Intent(this@EcgActivity, EcgDetailsActivity::class.java))
                when (it) {
                    HttpCommonAttributes.REQUEST_REGISTERED -> {
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER,
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                }
            }
        })
    }


    private fun initAdapter(): MultiItemCommonAdapter<Ecg, ItemEcgBinding> {
        return object : MultiItemCommonAdapter<Ecg, ItemEcgBinding>(ecgList) {
            override fun getItemType(t: Ecg): Int {
                return 0
            }

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemEcgBinding {
                return ItemEcgBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemEcgBinding, t: Ecg, position: Int) {
                LogUtils.i(TAG, "initAdapter() position = $position")

                v.tvTitle.text = getString(R.string.ecg_details_title)
                v.itemLyEcg.visibility = View.GONE
                v.tvHeartValue.text = t.heart
                v.tvType.text = t.healthIndex
                v.tvTime.text = com.smartwear.xzfit.utils.TimeUtils.AllTimeToTime(t.healthMeasuringTime)


                //在线测量-APP发起
                if (t.initiator.equals("1")) {
                    v.ivType.setBackgroundResource(R.mipmap.sport_scene_phone)
                }
                //离线-设备发起
                else if (t.initiator.equals("0")) {
                    v.ivType.setBackgroundResource(R.mipmap.sport_scene_phone)
                }

                v.root.setOnClickListener {
                    gotoDetail(t)
                }
            }
        }
    }


    /**
     * icon跟随日期布局开闭状态旋转
     * */
    private fun rotateArrow() {
        val degree = if (binding.ivDateArrow.tag == null || binding.ivDateArrow.tag == true) {
            binding.ivDateArrow.tag = false
            -180F
        } else {
            binding.ivDateArrow.tag = true
            0F
        }
        binding.ivDateArrow.animate().setDuration(350).rotation(degree)
    }

    //region 用户选择了日历日期
    private var calendarSelectHandler = Handler(Looper.getMainLooper())
    private var calendarSelectRunnable = Runnable {
        calendarSelect()
    }

    private fun calendarSelect() {
        val calendarData = java.util.Calendar.getInstance()
        calendarData.timeInMillis = DateUtils.getLongTime(mSelectionDate, DateUtils.TIME_YYYY_MM_DD)
        val registerTime = DateUtils.getLongTime(SpUtils.getValue(SpUtils.REGISTER_TIME, "0"), DateUtils.TIME_YYYY_MM_DD)
        LogUtils.i(TAG, "注册日期 ${SpUtils.getValue(SpUtils.REGISTER_TIME, "0")}, 选择日期 $mSelectionDate")
        if (calendarData.timeInMillis < registerTime) {
            ToastUtils.showToast(R.string.history_over_time_tips2)
        }
        if (calendarData.timeInMillis > System.currentTimeMillis()) {
            calendarData.timeInMillis = System.currentTimeMillis()
            ToastUtils.showToast(R.string.history_over_time_tips)
        }

        binding.tvDate.text = mSelectionDate
        //每次生命周期同步过云的日期不再同步
        pageIndex = 1
//        viewModel.querySportRecordData(mSelectionDate, isSync)
    }

    private fun isSync(date: String): Boolean {
        var result = false
        if (!mSDates.contains(date)) {
            result = true
            mSDates.add(date)
        }
        return result
    }

    private fun getData() {
        val isSync = isSync(mSelectionDate)
        LogUtils.i(TAG, "getData() mSelectionDate = $mSelectionDate isSync = $isSync")
        if (isSync) {
            viewModel.showDialog()
            viewModel.getEcgListByDay(mSelectionDate) //获取今日数据
        } else {
            loadData(mSelectionDate)
        }
    }

    var ecgList = ArrayList<Ecg>()

    @SuppressLint("NotifyDataSetChanged")
    private fun loadData(date: String) {
        val ecgSqlList = viewModel.queryEcgDataList(date)
        LogUtils.i(TAG, "loadData() ecgList.size = " + ecgList.size)
        ecgList.clear()
        ecgList.addAll(ecgSqlList)
        LogUtils.i(TAG, "loadData() ecgList.size = " + ecgList.size)

        if (ecgList.size > 0) {
            binding.recyclerView.visibility = View.VISIBLE
            binding.noData.layoutNoData.visibility = View.GONE
        } else {
            binding.recyclerView.visibility = View.GONE
            binding.noData.layoutNoData.visibility = View.VISIBLE
        }
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    /**
     * 进入详情
     * */
    private fun gotoDetail(t: Ecg) {
        EcgUtils.cacheEcg = t
        //心电数据不为空
        if (t.ecgData != "") {
            startActivity(Intent(this@EcgActivity, EcgDetailsActivity::class.java))
        }//后台 Id为空
        else if (t.DataId.toString() == "") {
            startActivity(Intent(this@EcgActivity, EcgDetailsActivity::class.java))
        } else {
            viewModel.showDialog()
            viewModel.getEcgDetailedData(t)
        }
    }

}