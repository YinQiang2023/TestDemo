package com.smartwear.xzfit.ui.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.blankj.utilcode.util.NetworkUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.https.MyRetrofitClient
import com.smartwear.xzfit.https.Response
import com.smartwear.xzfit.https.params.CheckAppVersionParam
import com.smartwear.xzfit.https.response.AppVersionResponse
import com.smartwear.xzfit.ui.view.AppUpdateDialog
import com.smartwear.xzfit.utils.*
import retrofit2.Call
import retrofit2.Callback
import java.lang.Exception
import kotlin.math.abs

object AppUpdateManager {
    private var lastTime: Long = -1
    private var appUpdateDialog: AppUpdateDialog? = null
    var updateInfoService: UpdateInfoService? = null
    private val TAG: String = AppUpdateManager::class.java.simpleName
    const val FLAG_NEED_UPDATE = 0x01
    const val FLAG_NO_UPDATE = 0x02
    const val FLAG_UPDATING = 0x03
    const val FLAG_DOWN_FAIL = 0x04

    /**
     * 检测app升级
     * @param context
     * @param showNoUpdateDialog 是否无升级显示弹窗提示
     */
    fun checkUpdate(context: Context, showNoUpdateDialog: Boolean/*,callbac: IsNeedUpDateCallBack?=null*/) {
        updateInfoService = UpdateInfoService(context)
        updateInfoService?.setListener(object : UpdateInfoService.UpdateListener {
            override fun onStart() {
                LogUtils.i(TAG, "updateInfoService onStart ", true)
                if (appUpdateDialog != null) {
                    appUpdateDialog?.updateUI(FLAG_UPDATING, 0, 0f, 0f)
                }
                lastTime = -1
            }

            override fun onProgress(progress: Int, process: Float, length: Float) {
                //com.blankj.utilcode.util.LogUtils.i(TAG, "updateInfoService onProgress $progress" + " thread " + Thread.currentThread().name)
                if (appUpdateDialog != null && (lastTime == -1L
                            || abs(System.currentTimeMillis() - lastTime) >= 200
                            || process >= 99)
                ) {
                    lastTime = System.currentTimeMillis()
                    appUpdateDialog?.updateUI(FLAG_UPDATING, progress, process, length)
                }
            }

            override fun onSuccess() {
                LogUtils.i(TAG, "updateInfoService onSuccess ")
                if (appUpdateDialog != null && appUpdateDialog!!.isShowing) {
                    appUpdateDialog?.dismiss()
                }
                lastTime = -1
            }

            override fun onFailed(errorCode: Int) {
                LogUtils.i(TAG, "updateInfoService onFailed ", true)
                if (appUpdateDialog != null) {
                    appUpdateDialog?.updateUI(FLAG_DOWN_FAIL, 0, 0f, 0f)
                }
                lastTime = -1
            }

        })

        val bean = CheckAppVersionParam(BaseApplication.mContext.getString(R.string.main_app_name))
        val checkAppVersionCall = MyRetrofitClient.service.checkAppVersion(
            JsonUtils.getRequestJson(
                bean,
                CheckAppVersionParam::class.java
            )
        )
        checkAppVersionCall.enqueue(object : Callback<Response<AppVersionResponse>> {
            override fun onResponse(call: Call<Response<AppVersionResponse>>, response: retrofit2.Response<Response<AppVersionResponse>>) {
                AppUtils.tryBlock {
                    val result = response.body()
                    result?.let { result ->
                        val data = result.data
                        LogUtils.e(TAG, "CheckAppVersionParam result = $result")
                        if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                            if (data.isAppUpdate) {
                                LogUtils.i(TAG, "请求接口-获取APP版本号 需要升级")
                                updateAppVersionUi(context, FLAG_NEED_UPDATE, data.appDownloadUrl, true)
                            } else {
                                LogUtils.i(TAG, "请求接口-获取APP版本号 不需要升级")
                                updateAppVersionUi(context, FLAG_NO_UPDATE, data.appDownloadUrl, showNoUpdateDialog)
                            }
                            return@let
                        }
                        LogUtils.i(TAG, "请求接口-获取APP版本号 不需要升级")
                        updateAppVersionUi(context, FLAG_NO_UPDATE, "", showNoUpdateDialog)
                    }
                }
            }

            override fun onFailure(call: Call<Response<AppVersionResponse>>, t: Throwable) {
                val stackTrace = t.stackTrace[0]
                LogUtils.e(
                    TAG,
                    "CheckAppVersionParam e =$t" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className
                )
            }
        })
    }

    private fun updateAppVersionUi(context: Context, flag: Int, appDownLoadUrl: String, showDialog: Boolean) {
        val cancelText = context.getString(R.string.dialog_cancel_btn)
        var okText = ""
        var tittle = ""
        var content = ""
        if (flag == FLAG_NEED_UPDATE) {
            tittle = context.getString(R.string.find_new_app_version)
            content = context.getString(R.string.find_new_app_version_tips)
            okText = context.getString(R.string.upgrade_immediately)
        } else {
            tittle = context.getString(R.string.dialog_title_tips)
            content = context.getString(R.string.current_app_version_is_latest)
            okText = context.getString(R.string.dialog_confirm_btn)
        }
        DialogUtils.getDialogActivity(context)?.let {
            if (!it.isFinishing && !it.isDestroyed) {
                if (appUpdateDialog != null && appUpdateDialog!!.isShowing) {
                    appUpdateDialog?.dismiss()
                }
                appUpdateDialog = AppUpdateDialog(it)
                appUpdateDialog?.cancelText = cancelText
                appUpdateDialog?.okText = okText
                appUpdateDialog?.tittle = tittle
                appUpdateDialog?.content = content
                appUpdateDialog?.flag = flag
                appUpdateDialog?.setCallback(object : AppUpdateDialog.AppUpdateCallback {
                    override fun onConfirm(flag: Int) {
                        when (flag) {
                            FLAG_NEED_UPDATE -> gotoBrowser(context, appDownLoadUrl)
                            FLAG_NO_UPDATE -> {}
                            FLAG_UPDATING -> {}
                            else -> gotoBrowser(context, appDownLoadUrl)
                        }
                    }

                    override fun onCancel() {}

                })

                if (appUpdateDialog?.isShowing == false && showDialog) {
                    appUpdateDialog?.show()
                }
            }
        }
    }

    private fun goToDownloadApp(context: Context, appDownLoadUrl: String) {
        /*if (PermissionUtils.lacksPermissions(*PermissionUtils.permissionStorage)) {
            PermissionsActivity.startActivityForResult(
                context as Activity,
                0,
                *PermissionUtils.permissionStorage
            )
            return
        }
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            updateInfoService?.downLoadFile(appDownLoadUrl)
        } else {
            appUpdateDialog?.dismiss()
            ToastUtils.showToast(R.string.sd_card)
        }*/
        PermissionUtils.checkRequestPermissions(
            null,
            BaseApplication.mContext.getString(R.string.permission_sdcard),
            PermissionUtils.PERMISSION_GROUP_SDCARD
        ) {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                NetworkUtils.isAvailableAsync { isAvailable ->
                    if (!isAvailable) {
                        ToastUtils.showToast(context.getString(R.string.not_network_tips))
                        if (appUpdateDialog != null && appUpdateDialog!!.isShowing) {
                            appUpdateDialog?.dismiss()
                        }
                    } else {
                        updateInfoService?.downLoadFile(appDownLoadUrl)
                    }
                }
            } else {
                if (appUpdateDialog != null && appUpdateDialog!!.isShowing) {
                    appUpdateDialog?.dismiss()
                }
                ToastUtils.showToast(R.string.sd_card)
            }
        }
    }

    private fun gotoBrowser(context: Context, url: String) {
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        val contentUrl: Uri = Uri.parse(url)
        intent.data = contentUrl
        context.startActivity(intent)
    }

    private fun gotoGooglePlay(context: Context) {
        val playPackage = "com.android.vending"
        try {
            val currentPackageName = context.packageName
            if (currentPackageName != null) {
                val currentPackageUri = Uri.parse("market://details?id=" + context.packageName)
                val intent = Intent(Intent.ACTION_VIEW, currentPackageUri)
                intent.setPackage(playPackage)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val currentPackageUri = Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
            val intent = Intent(Intent.ACTION_VIEW, currentPackageUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun onDestroy() {
        try {
            if (updateInfoService != null) updateInfoService?.onDestroy()
            if (appUpdateDialog != null && appUpdateDialog!!.isShowing) {
                appUpdateDialog?.dismiss()
                appUpdateDialog?.onDestroy()
            }
            updateInfoService = null
            appUpdateDialog = null
            lastTime = -1
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /*interface IsNeedUpDateCallBack{
        fun isNeedUpDate(isneed:Boolean)
    }*/

}