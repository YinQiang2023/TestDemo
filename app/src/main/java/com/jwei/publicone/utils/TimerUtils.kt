package com.jwei.publicone.utils

import com.jwei.publicone.ui.HealthyFragment


object TimerUtils {
    var mTimeCount = 0L
    var isForUser = false
    private const val TIME_DELAY = 60 * 1000
    fun calcTimer(currentTime: Long): Boolean {
        return if (currentTime - mTimeCount > (3 * TIME_DELAY) || isForUser) {
//        return if ((currentTime - mTimeCount > (3 * 10000) || isForUser) && !RealTimeRefreshDataUtils.isInconsistent){
            if (HealthyFragment.viewIsVisible) {
                mTimeCount = currentTime
            }
            isForUser = false
            true
        } else {
            false
        }
    }
}