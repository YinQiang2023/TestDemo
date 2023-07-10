package com.jwei.publicone.ui.healthy.ecg

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.zh.ble.wear.protobuf.SportingProtos
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.EcgCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityEcgMeasureBinding
import com.jwei.publicone.db.model.Ecg
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.utils.LogUtils
import com.jwei.publicone.utils.TimeUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.EcgModel
import com.zjw.healthdata.bean.EcgInfo

/**
 * Created by Android on 2022/3/24.
 */
class EcgMeasureActivity : BaseActivity<ActivityEcgMeasureBinding, EcgModel>(ActivityEcgMeasureBinding::inflate, EcgModel::class.java), View.OnClickListener {
    private val TAG: String = EcgMeasureActivity::class.java.simpleName
    override fun setTitleId(): Int = binding.title.layoutTitle.id

    //是否测量测量中
    private var isMeasureIng = false


    override fun initView() {
        super.initView()
        binding.title.tvTitle.text = getString(R.string.measure_title)
        binding.title.layoutRight.visibility = View.VISIBLE
        binding.title.ivRightIcon.visibility = View.VISIBLE
        binding.title.ivRightIcon.setImageResource(R.mipmap.icon_close)
        setViewsClickListener(this, binding.title.tvTitle, binding.title.layoutRight, binding.btnRestart)

        initHandler();
        viewModel.loadDialog = DialogUtils.showLoad(this)
        viewModel.mEcgDataProcessing!!.init()
        EcgUtils.initEcgView(binding.ecgMeasureEcgview)
        updateUi(1);
    }

    override fun onClick(v: View) {
        when (v.id) {
            //结束测量
            binding.title.tvTitle.id -> {
                quitActivity();
            }

            //结束测量
            binding.title.layoutRight.id -> {
                quitActivity();
            }

            //重新测量
            binding.btnRestart.id -> {
                //询问状态
                ControlBleTools.getInstance().requestEcgMeasurement(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        viewModel.dismissDialog()
                        ToastUtils.showSendCmdStateTips(state)
                    }
                });
                viewModel.showDialog()
            }
        }
    }

    override fun initData() {
        super.initData()
    }

    override fun onResume() {
        super.onResume()
        CallBackUtils.ecgCallBack = object : EcgCallBack {
            override fun onEcgCommandSet(command: Int) {
                when (command) {
                    SportingProtos.SEEcgData.SEECGRespond.OK_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 可以正常响应--->进行下一步")
                        viewModel.dismissDialog()
                        updateUi(1);
                    }
                    SportingProtos.SEEcgData.SEECGRespond.BUSY_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备正忙，无法正常响应")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.ota_device_busy_tips),
                            getString(R.string.dialog_confirm_btn), null
                        )
                    }
                    SportingProtos.SEEcgData.SEECGRespond.CHARGING_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备正忙，设备充电中")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.device_battery_charge_state),
                            getString(R.string.dialog_confirm_btn), null
                        )
                    }
                    SportingProtos.SEEcgData.SEECGRespond.LOW_BATTERY_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 设备低电量")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.ota_device_low_power_tips),
                            getString(R.string.dialog_confirm_btn), null
                        )
                    }
                    SportingProtos.SEEcgData.SEECGRespond.END_MEASUREMENT_DATA_OK_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = ecg结束测量数据正常结束")

                    }
                    SportingProtos.SEEcgData.SEECGRespond.END_MEASUREMENT_DATA_ERROR_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = ecg结束测量数据异常结束")
                        updateUi(3);
                    }
                    SportingProtos.SEEcgData.SEECGRespond.UNKNOWN_VALUE -> {
                        LogUtils.i(TAG, "onEcgCommandSet command = 未知")
                        viewModel.dismissDialog()
                        DialogUtils.showDialogTitleAndOneButton(
                            ActivityUtils.getTopActivity(), "", getString(R.string.send_device_cmd_tip_unknown),
                            getString(R.string.dialog_confirm_btn), null
                        )
                    }
                }
            }

            override fun onEcgRespond(command: Int) {
                when (command) {
                    SportingProtos.SEEcgData.SEECGCommand.ECG_REQUEST_MEASUREMENT_VALUE -> {
                        LogUtils.i(TAG, "onEcgRespond command = 请求心电请求")
                    }
                    SportingProtos.SEEcgData.SEECGCommand.ECG_START_MEASUREMENT_VALUE -> {
                        LogUtils.i(TAG, "onEcgRespond command = 开始测量")
                        updateUi(2);
                    }
                    SportingProtos.SEEcgData.SEECGCommand.ECG_ABNORMAL_END_MEASUREMENT_VALUE -> {
                        LogUtils.i(TAG, "onEcgRespond command = 提前结束测量")
                        updateUi(3);
                    }
                    SportingProtos.SEEcgData.SEECGCommand.ECG_END_MEASUREMENT_VALUE -> {
                        LogUtils.i(TAG, "onEcgRespond command = 停止测量--正常结束")
                        if (EcgUtils.checkHrValue(mEcgInfo.ecgHR)) {
                            updateUi(4);
                        } else {
                            updateUi(3);
                        }
                    }
                    SportingProtos.SEEcgData.SEECGCommand.ECG_MANUAL_END_MEASUREMENT_VALUE -> {
                        LogUtils.i(TAG, "onEcgRespond command = 手动结束测量")
//                        updateUi(3);
                        finish()
                    }
                }
            }

            override fun onEcgDataSend(ecgData: IntArray) {
//                LogUtils.i(TAG, "onEcgRespond ecgData.len = " + ecgData.size)
                handleEcgData(ecgData);
            }
        }
    }

    fun updateUi(type: Int) {
        when (type) {
            //准备测量
            1 -> {
                isMeasureIng = false;
                binding.llBottomViw1.visibility = View.VISIBLE;
                binding.llBottomViw2.visibility = View.VISIBLE;
                binding.llBottomViw3.visibility = View.VISIBLE;

                binding.tvBottomText1.visibility = View.VISIBLE;
                binding.tvBottomText2.visibility = View.VISIBLE;

                binding.tvBottomText1.setText(getText(R.string.measure_tip_prepare))
                binding.tvBottomText2.setText(getText(R.string.measure_tip_long_press_start))

                binding.ivEcgTip.visibility = View.VISIBLE;
                binding.btnRestart.visibility = View.GONE;

            }

            //正在测量中
            2 -> {
                isMeasureIng = true;
                binding.llBottomViw1.visibility = View.VISIBLE;
                binding.llBottomViw2.visibility = View.VISIBLE;
                binding.llBottomViw3.visibility = View.VISIBLE;

                binding.tvBottomText1.visibility = View.GONE;
                binding.tvBottomText2.visibility = View.VISIBLE;
                binding.tvBottomText1.setText("")
                binding.tvBottomText2.setText(getText(R.string.measure_tip_1))

                binding.ivEcgTip.visibility = View.GONE;
                binding.btnRestart.visibility = View.GONE;
                initEcg()

            }

            //测量失败
            3 -> {
                isMeasureIng = false;
                binding.llBottomViw1.visibility = View.VISIBLE;
                binding.llBottomViw2.visibility = View.GONE;
                binding.llBottomViw3.visibility = View.VISIBLE;

                binding.tvBottomText1.visibility = View.VISIBLE;
                binding.tvBottomText2.visibility = View.VISIBLE;
                binding.tvBottomText1.setText(getText(R.string.measure_tip_fail))
                binding.tvBottomText2.setText(getText(R.string.measure_tip_fail_info))

                binding.ivEcgTip.visibility = View.GONE;
                binding.btnRestart.visibility = View.VISIBLE;
            }

            //结束
            4 -> {
                isMeasureIng = false;
                binding.llBottomViw1.visibility = View.VISIBLE;
                binding.llBottomViw2.visibility = View.GONE;
                binding.llBottomViw3.visibility = View.VISIBLE;

                binding.tvBottomText1.visibility = View.VISIBLE;
                binding.tvBottomText2.visibility = View.GONE;
                binding.tvBottomText1.setText(getText(R.string.measure_tip_analysis_datao))
                binding.tvBottomText2.setText("")

                binding.ivEcgTip.visibility = View.GONE;
                binding.btnRestart.visibility = View.GONE;
                measureSuccess()
            }
        }
    }

    var ecgMeasureDate = ""
    var ecgMeasureTime = ""
    var mEcgDataBuffer = StringBuffer()
    private fun initEcg() {
        ecgMeasureDate = TimeUtils.getDate()
        ecgMeasureTime = TimeUtils.getTime()
        mEcgDataBuffer = StringBuffer()
    }


    private lateinit var mEcgInfo: EcgInfo
    fun handleEcgData(ecgData: IntArray) {
        for (i in ecgData.indices) {
            mEcgDataBuffer.append(ecgData.get(i).toString() + ",")
            mEcgInfo = viewModel.getEcgToInfo(ecgData.get(i))
            val ecgValue = mEcgInfo.ecgData
            val hrValue = mEcgInfo.ecgHR
            updateHrData(hrValue);
            sendEcgDate(EcgUtils.getEcgDrawValue(ecgValue))
        }
    }

    private fun updateHrData(value: Int) {
        if (value > 0) {
            binding.tvHrValue.setText(value.toString())
        } else {
            binding.tvHrValue.setText(getText(R.string.no_data_sign))
        }
    }

    private fun measureSuccess() {
        LogUtils.i(TAG, "measureSuccess()")
        LogUtils.i(TAG, "measureSuccess() ecgMeasureTime = $ecgMeasureDate")
        LogUtils.i(TAG, "measureSuccess() ecgMeasureTime = $ecgMeasureTime")
        LogUtils.i(TAG, "measureSuccess() mEcgDataBuffer.len = ${mEcgDataBuffer.length}")
        LogUtils.i(TAG, "measureSuccess() mEcgDataBuffer = $mEcgDataBuffer")
        LogUtils.i(TAG, "measureSuccess() mEcgInfo = $mEcgInfo")
        val mEcg = viewModel.getEcgInfo(ecgMeasureDate, ecgMeasureTime, mEcgDataBuffer.toString(), mEcgInfo)
        val isSuccess: Boolean = viewModel.saveEcgData(mEcg)
        if (isSuccess) {
            LogUtils.i(TAG, "measureSuccess() isSuccess = $isSuccess")
            viewModel.uploadEcg(mEcg) //上传数据
            viewModel.updateHealthFragmentUi()
            Handler().postDelayed({
                gotoDetail(mEcg)
            }, 2000)
        }
    }


    private var mHandler: Handler? = null
    private fun initHandler() {
        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                // TODO Auto-generated method stub
                when (msg.what) {
                    MSG_DATA_ECG -> {
                        val ecg_data = msg.arg2
                        if (ecg_data > 0.0000001f) {
//                            LogUtils.i(TAG, "handler_init ecg_data = $ecg_data")
                            binding.ecgMeasureEcgview.setLinePoint(ecg_data.toFloat())
                        }
                    }
                    else -> {}
                }
                super.handleMessage(msg)
            }
        }
    }

    var MSG_DATA_ECG = 0x11
    private fun sendEcgDate(inputValue: Double) {
        val ecgValue: Double = inputValue
        val message = Message()
        message.what = MSG_DATA_ECG
        message.arg2 = ecgValue.toInt()
        mHandler!!.sendMessage(message)
    }


    private fun quitActivity() {
        LogUtils.i(TAG, "quitActivity()")
        if (!isMeasureIng) {
            finish()
            return
        }
        DialogUtils.showDialogTwoBtn(
            this,
            null,
            getString(R.string.end_measure_tip),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    requestEcgMeasurement()
                }

                override fun OnCancel() {
                }
            }).show()
    }

    private fun requestEcgMeasurement() {
        //询问状态
        ControlBleTools.getInstance().stopEcgMeasurement(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                viewModel.dismissDialog()
                ToastUtils.showSendCmdStateTips(state)
                finish()
            }
        });
        viewModel.showDialog()
    }

    /**
     * 进入详情
     * */
    private fun gotoDetail(t: Ecg) {
        EcgUtils.isAutoRefresh = true
        EcgUtils.cacheEcg = t
        startActivity(Intent(this@EcgMeasureActivity, EcgDetailsActivity::class.java))
        finish()
    }

}