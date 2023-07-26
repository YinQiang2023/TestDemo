package com.jwei.xzfit.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2021/11/16.
 * 蓝牙状态改变广播接受者
 */
class BluetoothMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != null) {
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                    com.jwei.xzfit.utils.LogUtils.d("BluetoothMonitorReceiver", "state --------> $blueState", true)
                    when (blueState) {
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            LogUtils.d("蓝牙正在打开")
                        }
                        BluetoothAdapter.STATE_ON -> {
                            LogUtils.d("蓝牙已经打开")
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            LogUtils.d("蓝牙正在关闭")
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            LogUtils.d("蓝牙已经关闭")
                        }
                    }
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_BLE_STATUS_CHANGE, blueState))
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    LogUtils.d(context, "蓝牙设备已连接")
                    /*AppUtils.tryBlock {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        LogUtils.d(context, "蓝牙设备信息：" + device?.getName() + ", Address:" + device?.getAddress() + ", BondState:" + device?.getBondState() + ", Type:" + device?.getType())
                    }*/
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    LogUtils.d("蓝牙设备已断开")
                    /*AppUtils.tryBlock {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        LogUtils.d(context, "蓝牙设备信息：" + device?.getName() + ", Address:" + device?.getAddress() + ", BondState:" + device?.getBondState() + ", Type:" + device?.getType())
                    }*/
                }
            }
        }
    }
}