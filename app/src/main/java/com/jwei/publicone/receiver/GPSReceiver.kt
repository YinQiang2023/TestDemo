package com.jwei.publicone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.service.LocationService
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.ToastUtils

/**
 * Created by Android on 2021/11/19.
 * GPS开关广播接受者
 */
class GPSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Global.ACTION_PROVIDERS_CHANGED) {
            if (!AppUtils.isGPSOpen(BaseApplication.mContext)) {
                LocationService.binder?.service?.isLocationDoing?.let {
                    if(it){
                        ToastUtils.showToast(R.string.gps_close_tips)
                    }
                }
            }
        }
    }
}