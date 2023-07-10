package com.jwei.publicone.base

import android.text.TextUtils
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ProcessUtils
import com.zhapp.ble.ControlBleTools
import com.jwei.publicone.R
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.MyRetrofitClient
import com.jwei.publicone.https.params.TraceLogBean
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.ui.user.bean.TargetBean
import com.jwei.publicone.ui.user.bean.UserBean
import com.jwei.publicone.utils.JsonUtils
import com.jwei.publicone.utils.ManageActivity
import com.jwei.publicone.utils.SpUtils
import com.jwei.publicone.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by android
 * on 2021/7/14
 */
open class BaseViewModel : ViewModel(), LifecycleObserver {

    val error by lazy { MutableLiveData<Exception>() }

    fun launchUI(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch {
        block()
//        try {
//            block()
//        } catch (e: Exception){
//            error.value = e
//            Log.e("xxx Exception", e.toString())
//        }
    }


    fun userLoginOut(code: String) {
        if (code == HttpCommonAttributes.LOGIN_OUT || code == HttpCommonAttributes.AUTHORIZATION_EXPIRED
            || code == HttpCommonAttributes.USER_LOGIN_OUT
        ) {

            val loginTime = SpUtils.getValue(SpUtils.LAST_DEVICE_LOGIN_TIME, "")
            val userName = SpUtils.getValue(SpUtils.USER_NAME, "")

            if (code == HttpCommonAttributes.USER_LOGIN_OUT) {
                loginout()
                SpUtils.putValue(SpUtils.LAST_DEVICE_LOGIN_TIME, "")
                SpUtils.putValue(SpUtils.USER_NAME, "")

                ManageActivity.cancelAll()
                /*val intent = Intent(BaseApplication.mContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                BaseApplication.mContext.startActivity(intent)*/
                ProcessUtils.killAllBackgroundProcesses()
                AppUtils.relaunchApp()
                return
            }
            val topActivity = ManageActivity.getTopActivityNotInFinishing()
            if (topActivity != null) {
                Global.IS_LOGIN_CONFLICT = true
                var loginTimeStr = ""
                if (!TextUtils.isEmpty(loginTime)) {
                    loginTimeStr = TimeUtils.date2Str(Date(loginTime.toLong()), TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)
                }
                var tipsContent = if (code == HttpCommonAttributes.LOGIN_OUT) String.format(Locale.ENGLISH, topActivity.getString(R.string.remote_landing), loginTimeStr)
                else topActivity.getString(R.string.login_info_expired)
                val showDialogTwoBtn = DialogUtils.showDialogTwoBtn(topActivity, topActivity.getString(R.string.offline_tips),
                    tipsContent, topActivity.getString(R.string.privacy_statement_quit),
                    topActivity.getString(R.string.repeat_login), object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            loginout()
                            Global.IS_LOGIN_CONFLICT = false
                            SpUtils.putValue(SpUtils.LAST_DEVICE_LOGIN_TIME, "")
                            //SpUtils.putValue(SpUtils.USER_NAME, "")
                            ManageActivity.cancelAll()
                            BaseApplication.isShowIngOfflineDialog = false
                            ProcessUtils.killAllBackgroundProcesses()
                            /*val intent = Intent(BaseApplication.mContext, LoginActivity::class.java)
                            if (!TextUtils.isEmpty(userName))
                                intent.putExtra("intent_basic", userName)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            BaseApplication.mContext.startActivity(intent)*/
                            AppUtils.relaunchApp()
                        }

                        override fun OnCancel() {
                            loginout()
                            Global.IS_LOGIN_CONFLICT = false
                            SpUtils.putValue(SpUtils.LAST_DEVICE_LOGIN_TIME, "")
                            SpUtils.putValue(SpUtils.USER_NAME, "")
                            ManageActivity.cancelAll()
                            BaseApplication.isShowIngOfflineDialog = false
                            ProcessUtils.killAllBackgroundProcesses()
                            AppUtils.relaunchApp()
                        }

                    })
                if (!BaseApplication.isShowIngOfflineDialog) {
                    showDialogTwoBtn.setCancelable(false)
                    showDialogTwoBtn.show()
                    showDialogTwoBtn.setOnDismissListener {
                        BaseApplication.isShowIngOfflineDialog = false
                    }
                    BaseApplication.isShowIngOfflineDialog = true
                }

            }

        }
    }

    fun loginout() {
        SpUtils.setValue(SpUtils.USER_IS_LOGIN, SpUtils.USER_IS_LOGIN_DEFAULT)
        SpUtils.setValue(SpUtils.USER_ID, "")
        SpUtils.setValue(SpUtils.AUTHORIZATION, "")
        SpUtils.setValue(SpUtils.APP_FIRST_START, SpUtils.APP_FIRST_START_DEFAULT)
        SpUtils.setValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "")
        SpUtils.setValue(SpUtils.SERVICE_REGION_AREA_CODE, "")
        SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_DEFAULT)
        UserBean().clearData()
        TargetBean().clearData()
        SpUtils.setValue(SpUtils.USER_LOCAL_DATA, "")
        SpUtils.setValue(SpUtils.USER_INFO_AVATAR_URI, "")
//        SpUtils.setValue(SpUtils.HEALTHY_SHOW_ITEM_LIST, "")
//        SpUtils.setValue(SpUtils.EDIT_CARD_ITEM_LIST, "")
//            SpUtils.getSPUtilsInstance().put(SpUtils.WEATHER_SWITCH, false)
        SpUtils.remove(SpUtils.WEATHER_SWITCH)
        SpUtils.setValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")
        SpUtils.setValue(SpUtils.WEATHER_CITY_NAME, "")
        SpUtils.setValue(SpUtils.DEVICE_NAME, "")
        SpUtils.setValue(SpUtils.DEVICE_MAC, "")
        SpUtils.getSPUtilsInstance().remove(SpUtils.DEVICE_SETTING)
        //清除sdk内部设备信息
        com.jwei.publicone.utils.LogUtils.e("sdk release","user loginout")
        ControlBleTools.getInstance().disconnect()
        ControlBleTools.getInstance().release()
        Global.cleanData()
    }

    /**
     * 用户行为上报
     */
    fun traceSave(page: String, module: String, value: String,msgLog:String? = null) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val traceLogBean = TraceLogBean()
                traceLogBean.userId = userId
                traceLogBean.page = page
                traceLogBean.module = module
                traceLogBean.value = value
                traceLogBean.phoneSystemLanguage = Locale.getDefault().language.toString()
                traceLogBean.imei = "0"
                /*if (!DeviceUtils.getAndroidID().isNullOrEmpty()) {
                    traceLogBean.imei = DeviceUtils.getAndroidID()
                }*/
                traceLogBean.phoneType = com.jwei.publicone.utils.AppUtils.getPhoneType()
                traceLogBean.phoneSystemVersion = com.jwei.publicone.utils.AppUtils.getOsVersion()
                val gps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                if (gps.isNotEmpty() && gps.size == 2) {
                    traceLogBean.longitude = gps[0]
                    traceLogBean.latitude = gps[1]
                }
                traceLogBean.phoneSystemArea = Locale.getDefault().getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
                traceLogBean.errorLog = msgLog
                //val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create()
                MyRetrofitClient.service.traceSave(JsonUtils.getRequestJson(/*gson,*/ "traceSave", traceLogBean, TraceLogBean::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

}
