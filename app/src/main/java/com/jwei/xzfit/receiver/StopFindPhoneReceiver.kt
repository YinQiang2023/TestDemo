package com.jwei.xzfit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2021/10/27.
 */
class StopFindPhoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        LogUtils.e("收到找手机通知被关闭广播")
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_STOP_FIND_PHONE))
    }
}