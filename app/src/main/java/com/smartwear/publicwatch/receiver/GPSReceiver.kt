package com.smartwear.publicwatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.service.LocationService
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.ToastUtils

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