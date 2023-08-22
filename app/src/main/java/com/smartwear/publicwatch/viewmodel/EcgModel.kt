package com.smartwear.publicwatch.viewmodel

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.IntentUtils
import com.blankj.utilcode.util.PathUtils
import com.zhapp.ble.bean.OffEcgDataBean
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.db.model.Ecg
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.https.MyRetrofitClient
import com.smartwear.publicwatch.https.Response
import com.smartwear.publicwatch.https.params.LatelyDataBean
import com.smartwear.publicwatch.https.params.UpLoadEcgListBean
import com.smartwear.publicwatch.https.params.getEcgDetailedDataBean
import com.smartwear.publicwatch.https.params.getEcgListByDayBean
import com.smartwear.publicwatch.https.response.EcgLastDataResponse
import com.smartwear.publicwatch.https.response.EcgResponse
import com.smartwear.publicwatch.https.response.EffectiveStandLatelyResponse
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.healthy.ecg.EcgUtils
import com.smartwear.publicwatch.ui.livedata.RefreshHealthyFragment
import com.smartwear.publicwatch.utils.*
import com.zjw.healthdata.EcgDataProcessing
import com.zjw.healthdata.bean.EcgInfo
import org.litepal.LitePal.where
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class EcgModel : BaseViewModel() {
    private val TAG: String = EcgModel::class.java.simpleName
    lateinit var loadDialog: Dialog
    fun showDialog() {
        if (!loadDialog.isShowing) {
            loadDialog.show()
        }
    }

    fun dismissDialog() {
        DialogUtils.dismissDialog(loadDialog, 500)
    }

    //心电处理
    var mEcgDataProcessing: EcgDataProcessing? = EcgDataProcessing()

    /**
     * 获取心电数据
     */
    fun getEcgToInfo(value: Int): EcgInfo {
        val mEcgInfo: EcgInfo
        val isLeft = true;
        val calibration_hr = 70;
        val calibration_sbp = 120;
        val calibration_dbp = 70;
        val user_height = 170;
        val user_weight = 65;
        val user_step = 0;
        mEcgInfo = mEcgDataProcessing!!.getEcgInfo(value, isLeft, calibration_hr, calibration_sbp, calibration_dbp, user_height, user_weight, user_step);
        return mEcgInfo
    }

    /**
     * 获取离线心电数据
     */
    private fun getOffEcgToInfo(info: OffEcgDataBean): EcgInfo {
        mEcgDataProcessing!!.init()
        val mEcgInfo: EcgInfo
        val isLeft = true;
        val calibration_hr = 70;
        val calibration_sbp = 120;
        val calibration_dbp = 70;
        val user_height = 170;
        val user_weight = 65;
        val user_step = 0;
        mEcgInfo =
            mEcgDataProcessing!!.getOffEcgInfo(info.ecgDataValueList, isLeft, calibration_hr, calibration_sbp, calibration_dbp, user_height, user_weight, user_step);
        return mEcgInfo
    }

    fun getEcgInfo(ecgMeasureDate: String, ecgMeasureTime: String, mEcgData: String, mEcgInfo: EcgInfo): Ecg {
        val info = Ecg()
        val userID = SpUtils.getValue(SpUtils.USER_ID, "")
        info.userId = userID
        info.healthMeasuringTime = ecgMeasureTime
        info.initiator = "1"//数据来源， 0：离线 1：APP发起

        info.deviceSensorType = "1"//暂定固定为1，后期需要修改
        info.bpStatus = "1"//暂时不到位不支持，后续要修改

        info.ecgData = mEcgData

        info.heart = mEcgInfo.ecgHR.toString()
        info.systolic = mEcgInfo.ecgSBP.toString()
        info.diastolic = mEcgInfo.ecgDBP.toString()

        if (mEcgInfo.ecgHR >= 100) {
            info.hrvResult = "2"
        } else if (mEcgInfo.ecgHR <= 50) {
            info.hrvResult = "1"
        } else {
            info.hrvResult = "0"
        }

        info.healthIndex = mEcgInfo.healtHeartIndex.toString()
        info.fatigueIndex = mEcgInfo.healthFatigueIndex.toString()
        info.bodyLoad = mEcgInfo.healthLoadIndex.toString()
        info.bodyQuality = mEcgInfo.healthBodyIndex.toString()
        info.cardiacFunction = mEcgInfo.healthFatigueIndex.toString()

        info.isUpLoad = false
        info.timeStamp = System.currentTimeMillis().toString()
        info.deviceType = Global.deviceType
        info.deviceMac = Global.deviceMac
        info.deviceVersion = Global.deviceVersion
        info.date = ecgMeasureDate
        info.appVersion = AppUtils.getAppVersionName()

        return info
    }

    private fun getServiceEcg(data: EcgResponse.Data): Ecg {
        val info = Ecg()
        info.DataId = data.id.toLong()

        info.userId = data.userId
        info.healthMeasuringTime = data.healthMeasuringTime
        info.initiator = data.initiator

        info.deviceSensorType = data.deviceSensorType
        info.bpStatus = data.bpStatus

        info.ecgData = ""

        info.heart = data.heart
        info.systolic = data.systolic
        info.diastolic = data.diastolic

        info.hrvResult = data.hrvResult

        info.healthIndex = data.healthIndex
        info.fatigueIndex = data.fatigueIndex
        info.bodyLoad = data.bodyLoad
        info.bodyQuality = data.bodyQuality
        info.cardiacFunction = data.cardiacFunction

        info.isUpLoad = true
        info.timeStamp = System.currentTimeMillis().toString()
        info.deviceType = ""
        info.deviceMac = ""
        info.deviceVersion = ""
        info.date = TimeUtils.AllTimeToDate(data.healthMeasuringTime)
        info.appVersion = ""

        return info
    }

    //获取最近一次心电数据
    private fun getServiceLastDataEcg(data: EcgLastDataResponse): Ecg {
        val info = Ecg()
        info.DataId = data.id.toLong()

        info.userId = data.userId
        info.healthMeasuringTime = data.healthMeasuringTime
        info.initiator = data.initiator

        info.deviceSensorType = data.deviceSensorType
        info.bpStatus = data.bpStatus

        info.ecgData = ""

        info.heart = data.heart
        info.systolic = data.systolic
        info.diastolic = data.diastolic

        info.hrvResult = data.hrvResult

        info.healthIndex = data.healthIndex
        info.fatigueIndex = data.fatigueIndex
        info.bodyLoad = data.bodyLoad
        info.bodyQuality = data.bodyQuality
        info.cardiacFunction = data.cardiacFunction

        info.isUpLoad = true
        info.timeStamp = System.currentTimeMillis().toString()
        info.deviceType = ""
        info.deviceMac = ""
        info.deviceVersion = ""
        info.date = TimeUtils.AllTimeToDate(data.healthMeasuringTime)
        info.appVersion = ""

        return info
    }


    //保存单条心电数据到数据库
    fun saveEcgData(info: Ecg): Boolean {
        val isSuccess = info.saveUpdate(Ecg::class.java, "userId = ? and healthMeasuringTime = ?", info.userId, info.healthMeasuringTime)
        LogUtils.i(TAG, "saveEcgData() healthMeasuringTime = $info.healthMeasuringTime success = $isSuccess")
        return isSuccess
    }

    //查询指定日期所有数据-排序
    fun queryEcgDataList(date: String): ArrayList<Ecg> {
        val count = 50;
        val userId = SpUtils.getValue(SpUtils.USER_ID, "")
        LogUtils.i(TAG, "queryEcgDataList() date = $date USER_ID = $userId")
        val ecgList: List<Ecg> = where("userId = ? and date = ?", userId, date).limit(count).order("healthMeasuringTime desc").find(Ecg::class.java)
        LogUtils.i(TAG, "queryEcgDataList() ecgList = ${ecgList.size}")
        return ecgList as ArrayList<Ecg>
    }


    private val outLineDataCount = 10

    //查询最近的N条数据，未上传后台的数据
    private fun queryEcgDataListNoUpdate(): ArrayList<Ecg> {
        val userId = SpUtils.getValue(SpUtils.USER_ID, "")
        LogUtils.i(TAG, "queryEcgDataListNoUpdate() USER_ID = $userId")
        val ecgList: List<Ecg> = where("userId = ? and isUpLoad = ?", userId, "0").limit(outLineDataCount).order("healthMeasuringTime desc").find(Ecg::class.java)
        LogUtils.i(TAG, "queryEcgDataListNoUpdate() ecgList = ${ecgList.size}")
        return ecgList as ArrayList<Ecg>
    }

    //根据时间查询，并更新状态
    private fun updateEcgDataListUploadState(healthMeasuringTime: String) {
        val userId = SpUtils.getValue(SpUtils.USER_ID, "")
        var ecg = Ecg()
        ecg.isUpLoad = true
        ecg.updateAll("userId = ? and healthMeasuringTime = ?", userId, healthMeasuringTime)
    }

    //查询当前
    private fun queryEcgLastData(date: String): Ecg? {
        val userId = SpUtils.getValue(SpUtils.USER_ID, "")
        if (userId.isNotEmpty()) {
            val time = "$date 23:59:59"
            LogUtils.i(TAG, "time = $time")
            val ecgList = where("userId = ? and healthMeasuringTime < ?", userId, time).limit(1).order("healthMeasuringTime desc").find(Ecg::class.java, true)
            LogUtils.i(TAG, "ecgList.size = ${ecgList.size}")
            for (i in 0 until ecgList.size) {
                val ecg = ecgList[i];
                LogUtils.i(
                    TAG,
                    "queryEcgLastData() i = $i userId = ${ecg.userId} ecg.time = ${ecg.healthMeasuringTime} isUpLoad = ${ecg.isUpLoad} createDateTime = ${ecg.createDateTime}"
                )
            }
            if (ecgList != null && ecgList.size > 0) {
                val info = ecgList[0]
                LogUtils.i(TAG, "info = $info")
                return info
            }
        }
        return null
    }

    fun uploadEcgListToService() {
        val ecgList = queryEcgDataListNoUpdate()
        LogUtils.i(TAG, "uploadEcgListToService() ecgList = ${ecgList.size}")
        if (ecgList.size > 0) {
            for (i in 0 until ecgList.size) {
                val ecg = ecgList[i];
                LogUtils.i(TAG, "uploadEcgListToService() i = $i ecg.time = ${ecg.healthMeasuringTime} isUpLoad = ${ecg.isUpLoad}", true)
            }
            uploadEcgList(ecgList)
        }
    }

    //上传单条数据
    private val uploadEcg = MutableLiveData("")
    fun uploadEcg(ecg: Ecg) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadEcgListBean()
                    dataList.dataList.add(UpLoadEcgListBean.Utils.getData(userId, ecg))
                    val result = MyRetrofitClient.service.uploadEcgList(
                        JsonUtils.getRequestJson(TAG, dataList, UpLoadEcgListBean::class.java)
                    )
                    LogUtils.i(TAG, "uploadEcg result = $result")
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        updateEcgDataListUploadState(ecg.healthMeasuringTime)
                    }
                    uploadEcg.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getEcgListByDay e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                uploadEcg.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //上传多条数据
    private val uploadEcgList = MutableLiveData("")
    private fun uploadEcgList(list: ArrayList<Ecg>) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val dataList = UpLoadEcgListBean()

                    LogUtils.i(TAG, "uploadEcgList list = ${list.toTypedArray().contentToString()}")

                    for (i in 0 until list.size) {
                        val ecg = list[i];
                        dataList.dataList.add(UpLoadEcgListBean.Utils.getData(userId, ecg))
                    }
                    val result = MyRetrofitClient.service.uploadEcgList(
                        JsonUtils.getRequestJson(TAG, dataList, UpLoadEcgListBean::class.java)
                    )

                    LogUtils.i(TAG, "uploadEcgList result = $result")
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        for (i in 0 until list.size) {
                            val ecg = list[i];
                            updateEcgDataListUploadState(ecg.healthMeasuringTime)
                        }
                    }
                    uploadEcgList.postValue(result.code)
                    userLoginOut(result.code)
                }

            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getEcgListByDay e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                uploadEcgList.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //获取某一天多条数据
    val getEcgListByDay = MutableLiveData("")
    fun getEcgListByDay(date: String) {
        LogUtils.i(TAG, "getEcgListByDay() date = $date")
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val result = MyRetrofitClient.service.getEcgListByDay(JsonUtils.getRequestJson(TAG, getEcgListByDayBean(userId, date), getEcgListByDayBean::class.java))
                LogUtils.i(TAG, "getEcgListByDay result = $result")

                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    val dataList = result.data.list
                    for (info in dataList) {
                        val ecg = getServiceEcg(info)
                        saveEcgData(ecg)
                    }
                }



                getEcgListByDay.postValue(result.code)
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getEcgListByDay e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                getEcgListByDay.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //获取某一个详细数据
    val getEcgDetailedData = MutableLiveData("")
    fun getEcgDetailedData(t: Ecg) {
        LogUtils.i(TAG, "getEcgDetailedData() t.id = $t.id")
        launchUI {
            try {
                val result = MyRetrofitClient.service.getEcgDetailedData(
                    JsonUtils.getRequestJson(
                        TAG,
                        getEcgDetailedDataBean(t.DataId.toString()),
                        getEcgDetailedDataBean::class.java
                    )
                )
                LogUtils.i(TAG, "getEcgDetailedData result = $result")

                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                    t.ecgData = result.data.ecgData
                    t.isUpLoad = true
                    t.updateAll("userId = ? and healthMeasuringTime = ?", t.userId, t.healthMeasuringTime)
                    EcgUtils.cacheEcg = t
                }
                getEcgDetailedData.postValue(result.code)
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getEcgDetailedData e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                getEcgDetailedData.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    fun getEcgLastlyData() {
        LogUtils.i(TAG, "getEcgLastlyData()")
        launchUI {
            try {
                val date = TimeUtils.getDate()
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result = MyRetrofitClient.service.getEcgLastlyData(JsonUtils.getRequestJson(LatelyDataBean(userId, date), LatelyDataBean::class.java))
                    LogUtils.i(TAG, "getEcgLastlyData result = $result")
                    if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) {
                        val info = EcgModel().getServiceLastDataEcg(result.data)
                        EcgModel().saveEcgData(info)
                    }
                    updateHealthFragmentUi()
                    userLoginOut(result.code)
                } else {
                    val result = Response("", "", HttpCommonAttributes.LOGIN_OUT, "", EffectiveStandLatelyResponse())
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "getEcgLastlyData e =$e", true)
            }
        }
    }

    fun updateHealthFragmentUi() {
        val date = TimeUtils.getDate()
        val ecg = EcgModel().queryEcgLastData(date)
        if (ecg != null) {
            LogUtils.i(TAG, "updateHealthFragmentUi ecg = $ecg")
            val index = Global.healthyItemList.indexOfFirst { it.topTitleText == BaseApplication.mContext.getString(R.string.healthy_ecg_title) }
            if (index != -1) {
                LogUtils.i(TAG, "updateHealthFragmentUi 找到ID")
                Global.healthyItemList.get(index).apply {
                    context = ecg.heart
                    bottomText = TimeUtils.timeToStamp(ecg.healthMeasuringTime).toString()
                }
                RefreshHealthyFragment.postValue(true)
            }
        }
    }

    //保存单条心电数据到数据库
    fun saveOffEcgData(info: OffEcgDataBean): Boolean {
        LogUtils.i(TAG, "saveOffEcgData() start()")
        val offEcgInfo = getOffEcgToInfo(info)
        val mEcg = getEcgInfo(TimeUtils.AllTimeToDate(info.measurementTime), info.measurementTime, info.ecgDataStr, offEcgInfo)
        val isSuccess = saveEcgData(mEcg)
        if (isSuccess) {
            LogUtils.i(TAG, "saveOffEcgData() isSuccess = $isSuccess")
            uploadEcg(mEcg) //上传数据
            updateHealthFragmentUi()
        }
        return isSuccess
    }

    fun getNewReportEcgData(inputData: String, maxSize: Int): Array<String> {
        var resultData: Array<String>? = null
        resultData = inputData.split(",".toRegex()).toTypedArray()
        val oldData = inputData.split(",".toRegex()).toTypedArray()
        if (resultData.size > maxSize) {
            for (i in resultData.size - maxSize until resultData.size) {
                oldData.set(i - (resultData.size - maxSize), resultData[i])
            }
            resultData = oldData
        }
        return resultData
    }

    fun getReportValue(data: Int, pos: Int): Int {
        val stage1 = 250
        val stag2 = 285
        val stag3 = 250
        var value = data
        val rowHeight = (EcgUtils.MaxHeight / EcgUtils.LineNumber).toInt()
        if (pos in 0..20) {
            value = stage1
        } else if (pos in 21..50) {
            value = stag2
        } else if (pos in 51..70) {
            value = stag3
        } else if (pos > 70 && pos <= EcgUtils.MaxWidth) {
            value += rowHeight * 2
            if (value > 300) {
                value = 300
            } else if (value < 200) {
                value = 200
            }
        } else if (pos > EcgUtils.MaxWidth && pos <= EcgUtils.MaxWidth * 2) {
            value += rowHeight * 1
            if (value > 200) {
                value = 200
            } else if (value < 100) {
                value = 100
            }
        } else if (pos > EcgUtils.MaxWidth * 2 && pos <= EcgUtils.MaxWidth * 3) {
            if (value > 100) {
                value = 100
            } else if (value < 0) {
                value = 0
            }
        }
        return value
    }

    private val saveShareImgPath = PathUtils.getExternalAppCachePath() + File.separator + "image" + File.separator + "ecg_share.jpg"
    fun shareEcgReportImg(context: Context, photoUri: Bitmap?) {
        try {
            var filePath = saveShareImgPath
            LogUtils.i(TAG, "分享图片保存地址： $filePath")
            FileUtils.createFileByDeleteOldFile(filePath)
            val os: OutputStream = FileOutputStream(filePath)
            photoUri?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            context.startActivity(IntentUtils.getShareImageIntent(File(filePath)))
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showToast(context.getString(R.string.share_failed))
        }
    }
}
