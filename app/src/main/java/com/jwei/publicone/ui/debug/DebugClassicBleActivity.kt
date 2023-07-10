package com.jwei.publicone.ui.debug

import android.view.View
import androidx.lifecycle.Observer
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ClassicBluetoothStateBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityDebugClassicBleBinding
import com.jwei.publicone.ui.device.DeviceSettingLiveData
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.DeviceModel

/**
 * Created by Android on 2022/4/29.
 */
class DebugClassicBleActivity : BaseActivity<ActivityDebugClassicBleBinding, DeviceModel>(
    ActivityDebugClassicBleBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("经典蓝牙开关设置")
        setViewsClickListener(this, binding.btnGet, binding.btnSet)

        binding.btnSet.callOnClick()
    }

    override fun onClick(v: View) {
        if (v.id == binding.btnGet.id) {
            if(ControlBleTools.getInstance().isConnect){
                ControlBleTools.getInstance().getClassicBluetoothState(object :ParsingStateManager.SendCmdStateListener(this.lifecycle){
                    override fun onState(state: SendCmdState) {
                        if(state == SendCmdState.SUCCEED) {
                            ToastUtils.showToast(getString(R.string.set_success))
                        }
                    }
                })
            }
        }else if(v.id == binding.btnSet.id) {
            if(ControlBleTools.getInstance().isConnect){
                ControlBleTools.getInstance().setClassicBluetoothState(ClassicBluetoothStateBean(binding.cbSwitchButton.isChecked,false),object :ParsingStateManager.SendCmdStateListener(this.lifecycle){
                    override fun onState(state: SendCmdState) {
                        if(state == SendCmdState.SUCCEED) {
                            ToastUtils.showToast(getString(R.string.set_success))
                        }
                    }
                })
            }
        }
    }

    override fun initData() {
        super.initData()
        DeviceSettingLiveData.instance.getClassicBluetoothStateBean().observe(this,object :Observer<ClassicBluetoothStateBean>{
            override fun onChanged(t: ClassicBluetoothStateBean?) {
                if(t!=null){
                    binding.cbSwitchButton.isChecked = t.isSwitch
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}