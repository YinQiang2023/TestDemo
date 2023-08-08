package com.smartwear.xzfit.utils

import android.app.Activity
import com.blankj.utilcode.util.ActivityUtils

object ManageActivity {
    fun cancelAll() {
        ActivityUtils.finishAllActivities()
    }

    fun removeActivity(cls: Class<out Activity?>) {
        ActivityUtils.finishActivity(cls)
    }

    /**
     * 获取不在finish的activity
     * @return
     */
    fun getTopActivityNotInFinishing(): Activity? {
        return ActivityUtils.getTopActivity()
    }

}