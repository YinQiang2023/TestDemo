package com.smartwear.xzfit.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools

/**
 * Created by Android on 2021/12/30.
 * 语言切换，重启应用
 */
class LocaleChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            LogUtils.e("LocaleChangeReceiver", "Language change")

            if (ActivityUtils.getTopActivity() == null) {
                relaunchApp()
            }
            ActivityUtils.getTopActivity()?.let { activity ->
                ActivityUtils.addActivityLifecycleCallbacks(object : Utils.ActivityLifecycleCallbacks() {
                    override fun onActivityResumed(activity: Activity) {
                        super.onActivityResumed(activity)
                        relaunchApp()
                    }
                })
            }
        }
    }

    private fun relaunchApp() {
        //清除sdk内部设备信息
        com.smartwear.xzfit.utils.LogUtils.e("sdk release","Language change exitApp")
        ControlBleTools.getInstance().disconnect()
        ControlBleTools.getInstance().release()
        AppUtils.exitApp()
        return
    }
}