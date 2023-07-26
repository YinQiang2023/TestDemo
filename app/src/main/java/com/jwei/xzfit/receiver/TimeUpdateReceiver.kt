package com.jwei.xzfit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2023/2/23.
 */
class TimeUpdateReceiver :BroadcastReceiver(){
    private val TAG = TimeUpdateReceiver::class.java.simpleName

    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null) return
        val action = intent.action
        if (action == null || action.isEmpty()) return
        if (action == Intent.ACTION_TIME_TICK) { //系统每1分钟发送一次广播
            //LogUtils.d("系统每分钟时间变化：${TimeUtils.getNowString()}")
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_TIME_CHANGED))
        } else if (action == Intent.ACTION_TIME_CHANGED) {
            //系统手动更改时间发送广播
            //LogUtils.d("系统时间变化：${TimeUtils.getNowString()}")
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_TIME_CHANGED))
        }
    }
}