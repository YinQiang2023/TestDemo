package com.smartwear.publicwatch.utils

import android.annotation.SuppressLint
import com.smartwear.publicwatch.base.BaseApplication
import android.widget.Toast
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.smartwear.publicwatch.R
import com.zhapp.ble.parsing.SendCmdState

@SuppressLint("StaticFieldLeak")
object ToastUtils {
    private val mContext = BaseApplication.mContext
    private var mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
    @JvmStatic
    fun showToast(resId: Int) {
        ThreadUtils.runOnUiThread {
            showToast(mContext.getText(resId), Toast.LENGTH_SHORT)
        }
    }

    fun showToast(resId: Int, duration: Int) {
        ThreadUtils.runOnUiThread {
            showToast(mContext.getText(resId), duration)
        }
    }

    @JvmStatic
    fun showToast(msg: String) {
        ThreadUtils.runOnUiThread {
            showToast(msg, Toast.LENGTH_SHORT)
        }
    }

    fun showToast(strCharSequence: CharSequence, duration: Int) {
        //后台不弹toast
        if(!AppUtils.isAppForeground()) return
        if(duration ==  Toast.LENGTH_SHORT) {
            com.blankj.utilcode.util.ToastUtils.showShort(strCharSequence)
        }else{
            com.blankj.utilcode.util.ToastUtils.showLong(strCharSequence)
        }
        /*if (mToast == null) {
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        } else {
            mToast!!.cancel()
            mToast = null
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        }
        mToast.duration = duration
        mToast.setText(strCharSequence)
        mToast.show()*/
    }

    /*fun showToast(strCharSequence: CharSequence, duration: Int, gravity: Int) {
        if (mToast == null) {
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        } else {
            mToast!!.cancel()
            mToast = null
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        }
        mToast.duration = duration
        mToast.setText(strCharSequence)
        mToast.setGravity(gravity, 0, 0)
        mToast.show()
    }

    fun showToast(strCharSequence: CharSequence?, duration: Int, gravity: Int, x: Int, y: Int) {
        if (mToast == null) {
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        } else {
            mToast!!.cancel()
            mToast = null
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        }
        mToast.duration = duration
        mToast.setText(strCharSequence)
        mToast.setGravity(gravity, x, y)
        mToast.show()
    }

    fun showToast(resId: Int, duration: Int, gravity: Int, x: Int, y: Int) {
        if (mToast == null) {
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        } else {
            mToast!!.cancel()
            mToast = null
            mToast = Toast.makeText(BaseApplication.mContext, "", Toast.LENGTH_LONG)
        }
        mToast.duration = duration
        mToast.setText(mContext.getText(resId))
        mToast.setGravity(gravity, x, y)
        mToast.show()
    }

    fun showToast(resId: Int, duration: Int, gravity: Int) {
        showToast(mContext.getText(resId), duration, gravity)
    }*/

    /**
     * 显示发送指令提示
     * */
    fun showSendCmdStateTips(state: SendCmdState, timeout: (() -> Unit)? = null) {
        when (state) {
            //SendCmdState.SUCCEED->{} //不提示显示数据
            SendCmdState.NOT_SUPPORT -> showToast(R.string.send_device_cmd_tip_not_support)
            SendCmdState.DEPENDENCY_NOT_READY -> showToast(R.string.send_device_cmd_tip_not_ready)
            SendCmdState.FAILED -> showToast(R.string.send_device_cmd_tip_failed)
            SendCmdState.PARAM_ERROR -> showToast(R.string.send_device_cmd_tip_param_err)
            SendCmdState.UNKNOWN -> showToast(R.string.send_device_cmd_tip_unknown)
            SendCmdState.TIMEOUT -> {
                LogUtils.e("----------------指令超时----------------")
                showToast(R.string.send_device_cmd_tip_timeout)
                if (timeout != null) {
                    //调用超时处理
                    timeout()
                }
            }
            else -> {}
        }
    }

    fun cancel() {
        mToast?.cancel()
        mToast = null
        com.blankj.utilcode.util.ToastUtils.cancel()
    }
}