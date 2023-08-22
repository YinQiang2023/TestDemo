package com.smartwear.publicwatch.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ThreadUtils
import com.google.gson.reflect.TypeToken
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.bean.NotifyItem
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by Android on 2021/10/8.
 */
class MsgNotifyModel : BaseViewModel() {
    //消息通知设置列表
    val sysInfos: MutableLiveData<MutableList<NotifyItem>> = MutableLiveData(mutableListOf())
    val sysList: MutableList<NotifyItem> = mutableListOf()

    //已安装应用通知设置列表
    val appInfos: MutableLiveData<MutableList<NotifyItem>> = MutableLiveData(mutableListOf())

    //已安装应用
    val appList: MutableList<NotifyItem> = mutableListOf()

    /**
     * 获取通知设置列表
     * */
    fun getNotifyItem(context: Context, hasApp: Boolean = true) {
        sysList.clear()
        val dataJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST)
        if ((!dataJson.isNullOrBlank())) {
            val tempList: MutableList<NotifyItem>? =
                GsonUtils.fromJson(dataJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
            if (tempList != null) {
                val texts =
                    BaseApplication.mContext.resources.getStringArray(R.array.sys_notify_list)

                if (hasApp && tempList.size < texts.size) {
                    tempList.apply {
                        add(
                            NotifyItem(
                                2,
                                title = context.getString(R.string.notify_app_title),
                                isTypeHeader = true,
                                isShowLine = false
                            )
                        )
                        add(
                            NotifyItem(
                                2,
                                title = context.getString(R.string.device_msg_notify_other),
                                isTypeHeader = true,
                                isCanNext = true,
                                isShowLine = true
                            )
                        )
                    }
                }

                for (i in if (hasApp) texts.indices else tempList.indices) {
                    tempList[i].title = texts[i]
                }
                //缓存
                SpUtils.getSPUtilsInstance()
                    .put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(tempList))
                sysList.addAll(tempList)
                //LogUtils.e("mDatas = ${GsonUtils.toJson(mDatas)}")
                sysList.removeAll { it.type == 2 && !it.isTypeHeader }
                sysInfos.postValue(sysList)
                return
            }
        }

        sysInfos.value!!.apply {
            add(
                NotifyItem(
                    1,
                    title = context.getString(R.string.device_msg_notify_switch),
                    isTypeHeader = true
                )
            )
            add(
                NotifyItem(
                    1,
                    title = context.getString(R.string.notify_sys_title),
                    isTypeHeader = false,
                    isShowLine = false
                )
            )
            add(
                NotifyItem(
                    1,
                    packageName = Global.PACKAGE_CALL,
                    title = context.getString(R.string.device_msg_notify_call),
                    imgName = "device_msg_notify_item_call"
                )
            )
            add(
                NotifyItem(
                    1,
                    packageName = Global.PACKAGE_MMS,
                    title = context.getString(R.string.device_msg_notify_sms),
                    imgName = "device_msg_notify_item_sms"
                )
            )
            add(
                NotifyItem(
                    1,
                    packageName = Global.PACKAGE_MISS_CALL,
                    title = context.getString(R.string.device_msg_notify_missed_call),
                    imgName = "device_msg_notify_item_missed_call"
                )
            )
            if (hasApp) {
                add(
                    NotifyItem(
                        2,
                        title = context.getString(R.string.notify_app_title),
                        isTypeHeader = true,
                        isShowLine = false
                    )
                )
                add(
                    NotifyItem(
                        2,
                        title = context.getString(R.string.device_msg_notify_other),
                        isTypeHeader = true,
                        isCanNext = true,
                        isShowLine = true
                    )
                )
            }
        }
        sysInfos.postValue(sysList)
        //缓存
        SpUtils.getSPUtilsInstance()
            .put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(sysInfos.value!!))
    }


    /**
     * 第三方应用屏蔽的应用包名
     * */
    val continuePackageName = arrayListOf<String>(
        //自己
        AppUtils.getAppPackageName(),
        //电话
        "com.oneplus.dialer",
        "com.android.incallui",
        "com.android.phone",
        "com.samsung.android.dialer",
        "com.google.android.dialer",
        //拨号
        "com.android.contacts",
        "cn.nubia.contacts",
        //短信
        "com.android.mms",
        "com.android.mms.service",
        "com.oneplus.mms",
        "com.samsung.android.messaging",
        "cn.nubia.mms",
        "com.google.android.apps.messaging"
    )

    /**
     * 获取已安装应用通知设置列表
     * */
    suspend fun getApps() {
        withContext(Dispatchers.IO) {
            val pm = BaseApplication.mContext.packageManager
            //获取电话包名加入过滤包名列表
            var infoIntent = Intent(Intent.ACTION_DIAL)
            pm.resolveActivity(infoIntent, 0)?.let {
                //Log.e("ACTION_CALL", it.activityInfo.packageName)
                if (it.activityInfo != null) {
                    val pkName = it.activityInfo.packageName
                    if (!TextUtils.isEmpty(pkName)) {
                        if (!continuePackageName.contains(pkName)) {
                            continuePackageName.add(it.activityInfo.packageName)
                        }
                    }
                }
            }
            //获取短信包名加入过滤包名列表
            val uri: Uri = Uri.parse("smsto:10086")
            infoIntent = Intent(Intent.ACTION_SENDTO, uri)
            pm.resolveActivity(infoIntent, 0)?.let {
                //Log.e("ACTION_MMS", it.activityInfo.packageName)
                if (it.activityInfo != null) {
                    val pkName = it.activityInfo.packageName
                    if (!TextUtils.isEmpty(pkName)) {
                        if (!continuePackageName.contains(pkName)) {
                            continuePackageName.add(it.activityInfo.packageName)
                        }
                    }
                }
            }
            //获取所有已安装应用
            val resolveIntent = Intent(Intent.ACTION_MAIN, null)
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val rList: List<ResolveInfo> = pm.queryIntentActivities(resolveIntent, 0)
            for (r in rList) {
                /*LogUtils.d(
                    "    " + r.activityInfo.packageName + "----" + r.loadLabel(pm).toString()
                )*/
                if (r.activityInfo != null && r.activityInfo.packageName != null) {
                    //过滤特殊包名
                    if (continuePackageName.contains(r.activityInfo.packageName)) {
                        continue
                    }
                    appList.add(
                        NotifyItem(
                            2, r.activityInfo.packageName,
                            r.loadLabel(pm).toString() /*, icon = r.loadIcon(pm)*/ //OOM
                        )
                    )
                }
            }
            appInfos.postValue(appList)
        }
    }

    /**
     * 根据包名获取icon
     */
    fun getIconByPageName(packageName: String): Drawable? {
        val pm = BaseApplication.mContext.packageManager
        val resolveIntent = Intent(Intent.ACTION_MAIN, null)
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val rList: List<ResolveInfo> = pm.queryIntentActivities(resolveIntent, 0)
        for (r in rList) {
            if (r.activityInfo != null && r.activityInfo.packageName != null) {
                if (TextUtils.equals(r.activityInfo.packageName, packageName)) {
                    return r.loadIcon(pm)
                }
            }
        }
        return null
    }

    /**
     * 上报通知提示开关状态变化用户行为
     */
    var postMessageRunnable: Runnable? = null
    fun postMessageTraceSave() {
        if (postMessageRunnable == null) {
            postMessageRunnable = Runnable {
                var messageSwitch = "0"
                val log = StringBuilder()
                val notifyJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, "")
                val otherNotifyJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER, "")
                val tempList: MutableList<NotifyItem>? = GsonUtils.fromJson(notifyJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
                val otherList: MutableList<NotifyItem>? = GsonUtils.fromJson(otherNotifyJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
                if (!tempList.isNullOrEmpty()) {
                    log.append("sys:")
                    for (n in tempList) {
                        if (n.isShowLine && !n.isCanNext) {
                            log.append(n.title).append(":").append(if (n.isOpen) "1" else "0").append("#")
                        }
                        if (n.type == 1 && n.isTypeHeader && !n.isCanNext) {
                            //总开关状态
                            messageSwitch = if (n.isOpen) "1" else "0"
                        }
                    }
                }
                if (!otherList.isNullOrEmpty()) {
                    log.append("app:")
                    for (n in otherList) {
                        log.append(n.title).append(":").append(if (n.isOpen) "1" else "0").append("#")
                    }
                }

                traceSave("device_set", "message_switch", messageSwitch, log.toString())

                //app埋点异常日志上报
                /*AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getStartTypeTrack("消息通知"), isStart = true)
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getAppTypeTrack("消息通知开关状态改变").apply {
                    this.log = "消息开关状态：$log"
                })
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getAppTypeTrack("消息通知开关状态改变"), "1710",isEnd = true)*/
            }
        }
        ThreadUtils.getMainHandler().removeCallbacks(postMessageRunnable!!)
        ThreadUtils.runOnUiThreadDelayed(postMessageRunnable, 5000)

        AppTrackingManager.saveOnlyBehaviorTracking("7", "9")
    }
}