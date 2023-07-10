package com.jwei.publicone.ui.device.setting.sleep

import android.app.Dialog
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivitySleepSettingBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.viewmodel.DeviceModel

class SleepSettingActivity : BaseActivity<ActivitySleepSettingBinding, DeviceModel>(ActivitySleepSettingBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG: String = SleepSettingActivity::class.java.simpleName

    //等待loading
    private lateinit var loadDialog: Dialog

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSave.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }
                loadDialog.show()
                LogUtils.d("快速眼动设置 ->${binding.chbSwitchButton.isChecked}")
                ControlBleTools.getInstance().setRapidEyeMovement(binding.chbSwitchButton.isChecked, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                        if(state == SendCmdState.SUCCEED){
                            ToastUtils.showToast(R.string.save_success)
                            finish()
                            AppTrackingManager.saveOnlyBehaviorTracking("7","10")
                        }
                    }
                })
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_fragment_set_sleep)
        loadDialog = DialogUtils.showLoad(this)

        setViewsClickListener(this, binding.btnSave)
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getRapidEyeMovement().observe(this, {
            if (it == null) return@observe
            LogUtils.d("快速眼动 ->$it")
            binding.chbSwitchButton.isChecked = it
        })

        loadDialog.show()
        ControlBleTools.getInstance().getRapidEyeMovement(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    finish()
                }
            }
        })
    }

}