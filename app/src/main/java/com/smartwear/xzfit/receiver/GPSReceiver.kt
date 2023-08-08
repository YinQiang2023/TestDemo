package com.smartwear.xzfit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.service.LocationService
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.ToastUtils

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