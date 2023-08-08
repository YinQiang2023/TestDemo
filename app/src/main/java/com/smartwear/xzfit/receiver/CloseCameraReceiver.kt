package com.smartwear.xzfit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.ui.data.Global
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState

/**
 * Created by Android on 2021/10/27.
 * 关闭相机广播接受者
 */
class CloseCameraReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Global.TAG_CLOSE_PHOTO_ACTION) {
            LogUtils.d("关闭相机")
            ControlBleTools.getInstance().sendPhonePhotogragh(1, object : ParsingStateManager.SendCmdStateListener(null) {
                override fun onState(state: SendCmdState) {
                    LogUtils.d("关闭相机：$state")
                }
            })
        }
    }
}