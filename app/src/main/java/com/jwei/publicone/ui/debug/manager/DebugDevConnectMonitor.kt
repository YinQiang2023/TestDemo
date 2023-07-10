package com.jwei.publicone.ui.debug.manager

import com.blankj.utilcode.util.*
import com.zhapp.ble.BleCommonAttributes
import java.util.*

/**
 * Created by Android on 2021/12/24.
 * 监听设备连接次数
 */
object DebugDevConnectMonitor {
    private const val MONITOR_SWITCH = "MONITOR_SWITCH"
    private const val MONITOR_FILE_NAME = "DEBUG_CON_MONITOR_FILE_NAME"
    private const val MONITOR_COUNT = "DEBUG_CON_MONITOR_COUNT"

    //日志文件目录
    private val mFileDir = PathUtils.getAppDataPathExternalFirst() + "/files/log/"

    //当前监听日志文件名
    private var mMonitorFileName: String = ""
        get() {
            return SPUtils.getInstance().getString(MONITOR_FILE_NAME, "")
        }
        set(value) {
            SPUtils.getInstance().put(MONITOR_FILE_NAME, value)
            field = value
        }

    //设备重连次数
    private var mMonitorCount: Int = 0
        get() {
            return SPUtils.getInstance().getInt(MONITOR_COUNT, 0)
        }
        set(value) {
            SPUtils.getInstance().put(MONITOR_COUNT, value)
            field = value
        }

    //设备重连次数监听开关
    private var mMonitorSwitch: Boolean = false
        get() {
            return SPUtils.getInstance().getBoolean(MONITOR_SWITCH, false)
        }
        set(value) {
            SPUtils.getInstance().put(MONITOR_SWITCH, value)
            field = value
        }

    /**
     * 重置监听
     */
    fun resetMonitor() {
        mMonitorFileName = mFileDir + createFileName()
        mMonitorCount = 0
        mMonitorSwitch = true
        FileUtils.createOrExistsFile(mMonitorFileName)
    }

    fun devConnectStateChange(state: Int) {
        if (getSwitch()) {
            if (mMonitorFileName.isNotEmpty()) {
                val stringBuilder = StringBuilder()
                stringBuilder.append(TimeUtils.getNowString(com.jwei.publicone.utils.TimeUtils.getSafeDateFormat(com.jwei.publicone.utils.TimeUtils.DATEFORMAT_COMM)))
                when (state) {
                    BleCommonAttributes.STATE_CONNECTED -> {
                        stringBuilder.append(" ---> 设备连接上\n")
                        mMonitorCount++
                    }
                    BleCommonAttributes.STATE_CONNECTING -> {
                        stringBuilder.append(" ---> 设备连接中\n")
                    }
                    BleCommonAttributes.STATE_DISCONNECTED -> {
                        stringBuilder.append(" ---> 设备断开连接\n")
                    }
                    BleCommonAttributes.STATE_TIME_OUT -> {
                        stringBuilder.append(" ---> 设备连接超时\n")
                    }
                }
                FileIOUtils.writeFileFromString(mMonitorFileName, stringBuilder.toString(), true)
            }
        }
    }


    /**
     * 获取监听开关
     */
    fun getSwitch(): Boolean {
        return mMonitorSwitch
    }

    /**
     * 获取当前日志文件昵称
     */
    fun getCurFileName(): String {
        return mMonitorFileName
    }

    /**
     * 获取设备连接次数
     */
    fun getConnectCount(): Int {
        return mMonitorCount
    }

    /**
     * 创建文件名
     */
    private fun createFileName(): String {
        return "Dev_Connect_${TimeUtils.date2String(Date(), com.jwei.publicone.utils.TimeUtils.getSafeDateFormat("yyyy_MM_dd_HH_mm_ss"))}.txt"
    }

}