package com.smartwear.publicwatch.utils.manager

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.os.PowerManager
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.utils.AppUtils

/**
 * Created by Android on 2021/11/29.
 * 保持屏幕不锁屏管理类
 */
@SuppressLint("InvalidWakeLockTag")
class WakeLockManager private constructor() : LifecycleObserver {

    private var mPowerManager: PowerManager? = null

    private var mWakeLock: PowerManager.WakeLock? = null

    private var mLifecycle: Lifecycle? = null

    companion object {
        @JvmStatic
        val instance = SingletonHolder.INSTANCE
    }

    private object SingletonHolder {
        val INSTANCE = WakeLockManager()
    }

    init {
        mPowerManager = BaseApplication.mContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
    }

    /**
     * 是否屏幕是否可交互（未锁屏）
     * @return true 未锁屏  false 锁屏
     */
    fun isScreenOn(): Boolean {
        return mPowerManager?.isInteractive ?: false
    }

    /**
     * 保持不锁屏
     * @param lifecycle 生命周期 不能是主页！！！
     * */
    fun keepUnLock(lifecycle: Lifecycle? = null) {
        AppUtils.tryBlock {
            mLifecycle = lifecycle
            mLifecycle?.addObserver(this)
            if (isScreenOn()) {
                mWakeLock = mPowerManager?.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, this::class.java.simpleName)
                //保持不锁屏
                mWakeLock?.acquire(24 * 60 * 60 * 1000L)
            }
        }
    }

    /**
     * 生命周期结束-释放锁屏
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onLifecycleDestroy() {
        AppUtils.tryBlock {
            mWakeLock?.release()
            mWakeLock = null
            mLifecycle?.removeObserver(this)
            mLifecycle = null
        }
    }

    //region 申请设备电源锁 保持后台程序运行
    private var mCPUWakeLock: PowerManager.WakeLock? = null

    /**
     * 申请设备CUP锁 保持后台程序运行
     */
    @SuppressLint("WakelockTimeout")
    fun acquireCPUWakeLock() {
        AppUtils.tryBlock {
            //PARTIAL_WAKE_LOCK 仅仅确保CPU运行，至于屏幕是否常亮，键盘灯都不做保障
            //ON_AFTER_RELEASE 该锁释放后，会持续保持屏幕状态一段时间。
            mCPUWakeLock = mPowerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE, this::class.java.simpleName)
            mCPUWakeLock?.acquire(2 * 24 * 60 * 60 * 1000L)
        }
    }

    public fun releaseCPULock() {
        AppUtils.tryBlock {
            if (mCPUWakeLock!=null && mCPUWakeLock!!.isHeld){
                mCPUWakeLock!!.release()
                mCPUWakeLock = null
            }
        }
    }
    //endregion

}