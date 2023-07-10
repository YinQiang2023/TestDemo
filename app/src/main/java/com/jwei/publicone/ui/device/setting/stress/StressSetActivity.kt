package com.jwei.publicone.ui.device.setting.stress

import android.app.Dialog
import android.view.View
import android.widget.CompoundButton
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.PressureModeBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityStressSetBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.DeviceModel

/**
 * Created by Android on 2022/10/21.
 */
class StressSetActivity : BaseActivity<ActivityStressSetBinding, DeviceModel>(ActivityStressSetBinding::inflate, DeviceModel::class.java),
    View.OnClickListener,
    CompoundButton.OnCheckedChangeListener {
    private val TAG: String = StressSetActivity::class.java.simpleName

    //等待loading
    private lateinit var loadDialog: Dialog

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_pressure)
        loadDialog = DialogUtils.showLoad(this)
        setViewsClickListener(this,binding.switchReminder,binding.switchDetection,binding.btnSave)
        binding.switchDetection.setOnCheckedChangeListener(this)
    }

    override fun initData() {
        super.initData()
        viewModel.deviceSettingLiveData.getPressureMode().observe(this){
            if(it == null) return@observe
            binding.switchDetection.isChecked = it.pressureMode
            binding.switchReminder.isChecked = it.relaxationReminder
            refUI()
        }
        loadDialog.show()
        ControlBleTools.getInstance().getPressureMode(object :ParsingStateManager.SendCmdStateListener(this.lifecycle){
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
            }
        })
    }

    private fun refUI() {
        binding.tvRelaxReminder.isEnabled = binding.switchDetection.isChecked
        binding.tvRelaxReminderHint.isEnabled = binding.switchDetection.isChecked
        binding.switchReminder.isEnabled = binding.switchDetection.isChecked
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when(buttonView?.id){
            binding.switchDetection.id->{
                if(!binding.switchDetection.isChecked){
                    binding.switchReminder.isChecked = false
                }
                refUI()
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            binding.btnSave.id->{
                saveData()
            }
        }
    }

    private fun saveData() {
        val pressureMode = PressureModeBean()
        pressureMode.pressureMode = binding.switchDetection.isChecked
        pressureMode.relaxationReminder = binding.switchReminder.isChecked
        LogUtils.d("心率检测设置 ->${GsonUtils.toJson(pressureMode)}")
        loadDialog.show()
        ControlBleTools.getInstance()
            .setPressureMode(pressureMode, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    DialogUtils.dismissDialog(loadDialog)
                    if (state == SendCmdState.SUCCEED) {
                        ToastUtils.showToast(R.string.save_success)
                        finish()
                        return
                    }
                    ToastUtils.showSendCmdStateTips(state)
                }
            })
    }

}