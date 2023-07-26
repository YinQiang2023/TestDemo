package com.jwei.xzfit.utils

import android.Manifest
import android.database.Cursor
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.RomUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.receiver.SmsContentObserver
import com.jwei.xzfit.service.MyNotificationsService
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

/**
 * Created by android
 * on 2021/7/16
 */
object PermissionUtils : LifecycleObserver {
    const val TAG = "Permission"

    //region 权限组
    //Android 12 蓝牙权限
    @RequiresApi(Build.VERSION_CODES.S)
    val PERMISSION_BLE12 = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    //SDCard
    val PERMISSION_GROUP_SDCARD = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    //定位
    val PERMISSION_GROUP_LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
        /*,Manifest.permission.ACCESS_BACKGROUND_LOCATION*/
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    val PERMISSION_10_LOCATION = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    //相机
    val PERMISSION_CAMERA = arrayOf(
        Manifest.permission.CAMERA
    )

    //相机组
    val PERMISSION_GROUP_CAMERA = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    //监听来电、接电话挂电话、读取联系人
    val PERMISSIONS_MAIL_AND_PHONE_LIST = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CALL_LOG
        )//Manifest.permission.ANSWER_PHONE_CALLS, 26以下申请报错
    }
    //endregion

    //region 系统设置授权监听
    private val blockMap = hashMapOf<Lifecycle, () -> Unit>()
    private val perMap = hashMapOf<Lifecycle, Array<String>>()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun lifeResume() {
        perMap.keys.forEach { lifecycle ->
            val pers = perMap.get(lifecycle)
            if (pers != null && pers.isNotEmpty()) {
                LogUtils.d(TAG, "lifeResume - ${pers.contentToString()}")
                if (checkPermissions(*pers)) {
                    blockMap.get(lifecycle)!!.invoke()
                }
            }
        }
        perMap.clear()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun lifeDestroy() {
        LogUtils.d(TAG, "lifeDestroy")
        if (!blockMap.isNullOrEmpty()) {
            blockMap.keys.forEach { lifecycle ->
                lifecycle.removeObserver(this)
            }
        }
        blockMap.clear()
        perMap.clear()
    }
    //endregion

    //永久拒绝的权限
    private var mDeniedForever: MutableList<String> = mutableListOf()

    /**
     * 检测权限是否授权
     */
    fun checkPermissions(vararg permissions: String): Boolean {
        LogUtils.d(TAG, "检测权限是否授权 - ${permissions.toList().toTypedArray().contentToString()}")
        var isGranted = PermissionUtils.isGranted(*permissions)
        LogUtils.d(TAG, "是否授权 - $isGranted")
        return isGranted
    }

    /**
     * 检测并申请权限
     * @param lifecycle 授权权限页面生命周期对象，可为null
     * @param block()  申请权限同意|权限已经授权 后 执行的方法块
     * @param tips dialog 提示申请的权限
     * @param permissions 权限组
     * @return 权限是否同意
     */
    fun checkRequestPermissions(lifecycle: Lifecycle? = null, perName: String, permissions: Array<String>?, block: () -> Unit): Boolean {
        if (permissions == null || permissions.isEmpty()) {
            return false
        }

        if (!checkPermissions(*permissions)) {
            //永远拒绝的权限直接弹永久拒绝提示dialog
            if (mDeniedForever.containsAll(permissions.toList())) {
                showPermissionMissDialog(lifecycle, block, perName, permissions)
                return false
            }
            if (!ActivityUtils.getTopActivity().isFinishing && !ActivityUtils.getTopActivity().isDestroyed) {
                DialogUtils.showDialogTwoBtn(
                    ActivityUtils.getTopActivity(),
                    /*BaseApplication.mContext.getString(R.string.apply_permission)*/null,
                    String.format(Locale.ENGLISH, BaseApplication.mContext.getString(R.string.miss_permission_hint), perName),
                    BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
                    BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            LogUtils.d(TAG, "申请权限")
                            requestPermissions(lifecycle, perName, permissions, block)
                        }

                        override fun OnCancel() {}
                    }
                ).show()
            }
            return false
        }
        permissions.forEach {
            if (mDeniedForever.contains(it)) {
                mDeniedForever.remove(it)
            }
        }
        block()
        return true
    }

    /**
     * 申请权限
     */
    fun requestPermissions(lifecycle: Lifecycle? = null, perName: String, permissions: Array<String>, block: () -> Unit) {
        PermissionUtils.permission(*permissions)
            .callback(object : PermissionUtils.FullCallback {
                override fun onGranted(granted: MutableList<String>) {
                    LogUtils.d(TAG, "申请权限同意 - $granted")
                    granted.forEach {
                        if (mDeniedForever.contains(it)) {
                            mDeniedForever.remove(it)
                        }
                    }
                    if (checkPermissions(*permissions)) {
                        block()

                        //MIUI READ_SMS 问题
                        if (RomUtils.isXiaomi()) {
                            if (permissions.contains(Manifest.permission.READ_SMS)) {
                                fixBugMiUiSMS()
                            }
                        }
                    }
                }

                override fun onDenied(deniedForever: MutableList<String>, denied: MutableList<String>) {
                    LogUtils.d(TAG, "申请权限永远拒绝 - $deniedForever,\n拒绝 = $denied")
                    if (deniedForever.size > 0) {
                        mDeniedForever.addAll(deniedForever)
                        showPermissionMissDialog(lifecycle, block, perName, permissions)
                    }
                }
            }).request()
    }

    /**
     * 权限被永久拒绝dialog提示
     * */
    private fun showPermissionMissDialog(lifecycle: Lifecycle? = null, block: () -> Unit, perName: String, permissions: Array<String>) {
        if (!ActivityUtils.getTopActivity().isFinishing && !ActivityUtils.getTopActivity().isDestroyed) {
            val msg = StringBuilder()
            msg.append(String.format(Locale.ENGLISH, BaseApplication.mContext.getString(R.string.denied_forever_permission_hint), perName))
            DialogUtils.showDialogTwoBtn(
                ActivityUtils.getTopActivity(),
                BaseApplication.mContext.getString(R.string.apply_permission)/*null*/,
                msg.toString(),
                BaseApplication.mContext.getString(R.string.know),
                BaseApplication.mContext.getString(R.string.manual_binding),
                object : DialogUtils.DialogClickListener {
                    override fun OnOK() {
                        PermissionUtils.launchAppDetailsSettings()
                        //监听页面生命周期
                        lifecycle?.let {
                            it.addObserver(this@PermissionUtils)
                            blockMap[it] = block
                            perMap.put(it, permissions)
                        }
                    }

                    override fun OnCancel() {}
                }).show()
        }
    }

    private fun fixBugMiUiSMS() {
        try {
            val cursor: Cursor? = BaseApplication.mContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI, SmsContentObserver.PROJECT,
                null,
                null,
                "_id desc"
            )
            cursor?.moveToFirst()
            MyNotificationsService.setIsRegisterContentObserverSms(false)
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE))
        } catch (e: Exception) {
            Log.w(TAG, "fixBugMiUiSMS Exception")
            e.printStackTrace()
        }
    }
}