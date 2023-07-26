package com.jwei.xzfit.viewmodel

import android.content.Context
import android.os.CountDownTimer
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.NetworkUtils
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.db.model.track.TrackingLog
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.https.MyRetrofitClient
import com.jwei.xzfit.https.Response
import com.jwei.xzfit.https.params.*
import com.jwei.xzfit.https.response.*
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.user.bean.TargetBean
import com.jwei.xzfit.ui.user.bean.UserBean
import com.jwei.xzfit.utils.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.RequestBody
import okhttp3.internal.platform.Jdk9Platform.Companion.isAvailable
import retrofit2.HttpException
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File
import java.util.*
import kotlin.coroutines.resume

class UserModel : BaseViewModel() {
    private val TAG: String = UserModel::class.java.simpleName

    val isEndWelcomePage = MutableLiveData(false)

    val countdown = MutableLiveData(0)
    fun countdown(timeSecond: Int) {
        isEndWelcomePage.postValue(false)
        val timer: CountDownTimer = object : CountDownTimer(timeSecond * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdown.postValue(millisUntilFinished.toInt() / 1000)
            }

            override fun onFinish() {
                countdown.postValue(0)
                isEndWelcomePage.postValue(true)
            }
        }
        timer.start()
    }

    //查询是否已注册
    val queryByLoginName = MutableLiveData<Response<QureyLoginAccountResponse>>()
    fun queryByLoginName(name: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(QueryByLoginNameBean(name))
                    }
                }
                //----
                val result = MyRetrofitClient.service.queryByLoginName(JsonUtils.getRequestJson(TAG, QueryByLoginNameBean(name), QueryByLoginNameBean::class.java))
                LogUtils.e(TAG, "queryByLoginName result = $result")
                //----
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_REGISTERED || result.code == HttpCommonAttributes.REQUEST_NOT_REGISTER) "成功" else "失败"
                    }
                }
                //----
                queryByLoginName.postValue(result)
                //----
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "queryByLoginName e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                val result = Response("", "queryByLoginName e =$e", HttpCommonAttributes.REQUEST_FAIL, "", QureyLoginAccountResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }
                queryByLoginName.postValue(result)
            }
        }
    }

    //查询中国地区外网服务器
    val queryChinaByLoginName = MutableLiveData<Response<QureyLoginAccountResponse>>()
    fun queryChinaByLoginName(name: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(QueryByLoginNameBean(name))
                    }
                }
                val result = MyRetrofitClient.chinaService.queryByLoginName(JsonUtils.getRequestJson(TAG, QueryByLoginNameBean(name), QueryByLoginNameBean::class.java))
                LogUtils.e(TAG, "queryByLoginName result = $result")
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_REGISTERED || result.code == HttpCommonAttributes.REQUEST_NOT_REGISTER) "成功" else "失败"
                    }
                }
                queryChinaByLoginName.postValue(result)
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "queryByLoginName e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                val result = Response("", "queryByLoginName e =$e", HttpCommonAttributes.REQUEST_FAIL, "", QureyLoginAccountResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }
                queryChinaByLoginName.postValue(result)
            }
        }
    }

    //注册
    val registerCode = MutableLiveData("")
    fun register(name: String, password: String, type: String = "1", vararg tracks: TrackingLog) {
        launchUI {
            try {
                val bean = RegisterBean()
                bean.loginName = name
                bean.password = password
                bean.accountType = type
                bean.registerTime = TimeUtils.getTime()
                bean.phoneSystemType = 2
                bean.phoneType = AppUtils.getPhoneType()
                bean.phoneSystemVersion = AppUtils.getOsVersion()
                bean.phoneMac = ""
                bean.meid = ""
                bean.registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "")

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(bean)
                    }
                }

                val result = MyRetrofitClient.service.register(JsonUtils.getRequestJson(TAG, bean, RegisterBean::class.java))
                LogUtils.e(TAG, "register result = $result")
                val registerResponse = result.data
                if (registerResponse != null) {
                    SpUtils.setValue(SpUtils.USER_NAME, name)
                    SpUtils.setValue(SpUtils.USER_PASSWORD, password)
                    SpUtils.setValue(SpUtils.ACCOUNT_TYPE, type)

                    SpUtils.setValue(SpUtils.USER_ID, registerResponse.userId.toString())
                    SpUtils.setValue(SpUtils.AUTHORIZATION, registerResponse.authorization)
                    SpUtils.setValue(SpUtils.REGISTER_TIME, registerResponse.registerTime)
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }

                registerCode.postValue(result.code)
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "register e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                val result = Response("", "register e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                registerCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //登录
    val loginCode = MutableLiveData("")
    fun login(name: String, password: String, type: String = "1", vararg tracks: TrackingLog) {
        launchUI {
            try {
                val loginInfo = LoginBean()
                loginInfo.loginName = name
                loginInfo.password = password
                loginInfo.phoneSystemType = 2
                loginInfo.phoneType = AppUtils.getPhoneType()
                loginInfo.phoneSystemVersion = AppUtils.getOsVersion()
                loginInfo.phoneMac = ""
                loginInfo.meid = ""
                loginInfo.registrationArea = SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, "")
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(loginInfo)
                    }
                }
                val result = MyRetrofitClient.service.login(JsonUtils.getRequestJson(TAG, loginInfo, LoginBean::class.java))
                LogUtils.e(TAG, "login result = $result")
                val loginResponse = result.data
                if (loginResponse != null) {
                    SpUtils.setValue(SpUtils.USER_NAME, name)
                    SpUtils.setValue(SpUtils.USER_PASSWORD, password)
                    SpUtils.setValue(SpUtils.ACCOUNT_TYPE, type)

                    SpUtils.setValue(SpUtils.USER_ID, loginResponse.userId.toString())
                    SpUtils.setValue(SpUtils.AUTHORIZATION, loginResponse.authorization)
                    SpUtils.setValue(SpUtils.REGISTER_TIME, loginResponse.registerTime)
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS ||
                            //外服且未注册
                            (result.code == HttpCommonAttributes.REQUEST_NOT_REGISTER && SpUtils.getValue(
                                SpUtils.SERVICE_ADDRESS,
                                SpUtils.SERVICE_ADDRESS_DEFAULT
                            ) == SpUtils.SERVICE_ADDRESS_TO_TYPE1)
                        ) "成功" else "失败"
                    }
                }

                loginCode.postValue(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "login e =$e", true)

                val result = Response("", "register e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }
                loginCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //游客登录
    val ssoLoginCode = MutableLiveData("")
    fun ssoLogin(dataID: String, vararg tracks: TrackingLog) {
        launchUI {
            try {

                val ssologinInfo = SsoLoginBean()
                ssologinInfo.openid = dataID
                ssologinInfo.uid = dataID
//                ssologinInfo.name = ""
//                ssologinInfo.gender = 0
//                ssologinInfo.iconurl = ""
                ssologinInfo.registerTime = TimeUtils.getTime()

                ssologinInfo.phoneSystemType = "2"
                ssologinInfo.phoneType = AppUtils.getPhoneType()
                ssologinInfo.phoneSystemVersion = AppUtils.getOsVersion()
                ssologinInfo.phoneMac = ""
                ssologinInfo.meid = ""
                ssologinInfo.appId = CommonAttributes.APP_ID
                ssologinInfo.accountType = "5"

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serReqJson = AppUtils.toSimpleJsonString(ssologinInfo)
                    }
                }

                val result = MyRetrofitClient.service.ssoLogin(JsonUtils.getRequestJson(TAG, ssologinInfo, SsoLoginBean::class.java))
                LogUtils.e(TAG, "ssoLogin result = $result")
                val loginResponse = result.data
                if (loginResponse != null) {
                    SpUtils.setValue(SpUtils.USER_NAME, "")
                    SpUtils.setValue(SpUtils.USER_PASSWORD, "")
                    SpUtils.setValue(SpUtils.ACCOUNT_TYPE, "5")

                    SpUtils.setValue(SpUtils.USER_ID, loginResponse.userId.toString())
                    SpUtils.setValue(SpUtils.AUTHORIZATION, loginResponse.authorization)
                    SpUtils.setValue(SpUtils.REGISTER_TIME, loginResponse.registerTime)
                }

                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                    }
                }

                ssoLoginCode.postValue(result.code)
            } catch (e: Exception) {
                LogUtils.e(TAG, "ssoLogin e =$e", true)

                val result = Response("", "ssoLogin e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                ssoLoginCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //忘记密码
    val findPassWordCode = MutableLiveData("")
    fun findPassWord(name: String, password: String, code: String) {
        launchUI {
            try {
                val bean = FindPassWordBean()
                bean.loginName = name
                bean.resetPassword = password
                bean.phoneSystemType = 2
                bean.validationCode = code
                bean.phoneType = AppUtils.getPhoneType()
                bean.phoneSystemVersion = AppUtils.getOsVersion()
                bean.phoneMac = ""
                bean.meid = ""
                val result = MyRetrofitClient.service.findPassWord(JsonUtils.getRequestJson(TAG, bean, FindPassWordBean::class.java))
                LogUtils.e(TAG, "findPassWord result = $result")
                findPassWordCode.postValue(result.code)
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "findPassWord e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                findPassWordCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //获取验证码
    val getCode = MutableLiveData("")
    fun getCode(name: String, type: String, reqType: String) {
        launchUI {
            try {
                val bean = GetCodeBean()
                bean.loginName = name
                bean.accountType = type
                bean.reqType = reqType
                val result = MyRetrofitClient.service.getCode(JsonUtils.getRequestJson(TAG, bean, GetCodeBean::class.java))
                LogUtils.e(TAG, "getCode result = $result")
                getCode.postValue(result.code)
            } catch (e: Exception) {

                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getCode e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                getCode.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    var cacheUserInfo: Response<GetUserInfoResponse>? = null

    //获取用户信息
    val getUserInfo = MutableLiveData("")
    fun getUserInfo(vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serReqJson = AppUtils.toSimpleJsonString(NoResponse(userId.toLong()))
                        }
                    }

                    val result = MyRetrofitClient.service.getUserInfo(JsonUtils.getRequestJson(TAG, NoResponse(userId.toLong()), NoResponse::class.java))
                    cacheUserInfo = result
                    LogUtils.e(TAG, "getUserInfo result = $result")
                    if (result.data != null) {
                        UserBean().saveData(result)
                    }

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serResJson = AppUtils.toSimpleJsonString(result)
                            track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                        }
                    }

                    getUserInfo.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "getUserInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                val result = Response("", "getUserInfo e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                getUserInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //上传用户信息-第一录入
    val inputUserInfo = MutableLiveData("")
    fun inputUserInfo(mUserBean: UserBean, mTargetBean: TargetBean, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val bean = inputUserInfoBean()
                    bean.userId = userId.toLong()
                    bean.nikname = mUserBean.nickname
                    bean.height = mUserBean.height
                    bean.weight = mUserBean.weight
                    bean.birthday = mUserBean.birthDate
                    bean.sex = if (mUserBean.sex == "1") 1 else 0

                    bean.unit = mTargetBean.unit
                    bean.stepTarget = mTargetBean.sportTarget.toInt()
                    bean.distanceTarget = mTargetBean.distanceTarget.toInt()
                    bean.consumeTarget = mTargetBean.consumeTarget.toInt()
                    bean.sleepTarget = mTargetBean.sleepTarget.trim().toInt()

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serReqJson = AppUtils.toSimpleJsonString(bean)
                        }
                    }

                    val result = MyRetrofitClient.service.upLoadUserInfo(JsonUtils.getRequestJson(TAG, bean, inputUserInfoBean::class.java))
                    LogUtils.e(TAG, "inputUserInfo result = $result")

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serResJson = AppUtils.toSimpleJsonString(result)
                            track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                        }
                    }

                    inputUserInfo.postValue(result.code)
                    userLoginOut(result.code)
                }

            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "inputUserInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                val result = Response("", "inputUserInfo e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                inputUserInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //上传用户信息-修改个人信息
    val upLoadUserInfo = MutableLiveData("")
    fun upLoadUserInfo(userBean: UserBean) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val bean = UpLoadUserInfoBean()
                    bean.userId = userId.toLong()
                    bean.nikname = userBean.nickname
                    bean.height = userBean.height
                    bean.weight = userBean.weight
                    bean.birthday = userBean.birthDate
                    bean.sex = if (userBean.sex == "1") 1 else 0

//                    bean.unit = userBean.unit
//                    bean.stepTarget = userBean.stepTarget.toInt()
//                    bean.distanceTarget = userBean.distanceTarget.toInt() * 1000
//                    bean.consumeTarget = userBean.consumeTarget.toInt()
//                    bean.sleepTarget = userBean.sleepTarget.trim().toInt()/*calcSleepTime(userBean.sleepTarget)*/

                    val result = MyRetrofitClient.service.upLoadUserInfo(JsonUtils.getRequestJson(TAG, bean, UpLoadUserInfoBean::class.java))
                    LogUtils.e(TAG, "upLoadUserInfo result = $result")
                    upLoadUserInfo.postValue(result.code)
                    userLoginOut(result.code)
                }

            } catch (e: Exception) {

                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "upLoadUserInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                upLoadUserInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }


    //上传头像
    val upLoadAvatar = MutableLiveData("")

    /**
     * 上传头像 TODO 后台上传图片资源有限制检测： 涉黄，暴力等非法图片资源上传后会返回一张默认资源
     * */
    fun upLoadAvatar(data: String, vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serReqJson = AppUtils.toSimpleJsonString(UserAvatarBean(userId.toLong(), headImageData = "img data length: ${data.length}"))
                        }
                    }

                    val result = MyRetrofitClient.service.upLoadUserAvatar(
                        JsonUtils.getRequestJson(
                            TAG,
                            UserAvatarBean(userId.toLong(), headImageData = data),
                            UserAvatarBean::class.java
                        )
                    )

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serResJson = AppUtils.toSimpleJsonString(result)
                            track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                        }
                    }

                    LogUtils.e(TAG, "upLoadAvatar result = $result")
                    upLoadAvatar.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {

                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "upLoadAvatar e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }

                val result = Response("", "upLoadAvatar e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                upLoadAvatar.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    suspend fun lubanHead(context: Context, head: File): File? {
        return suspendCancellableCoroutine {
            Luban.with(context)
                .load(head)
                .ignoreBy(100) //压缩上限 100KB
                .filter { path -> !(TextUtils.isEmpty(path) || path.lowercase(Locale.ENGLISH).endsWith(".gif")) }
                .setTargetDir(Global.LUBAN_CACHE_DIR)
                .setCompressListener(object : OnCompressListener {
                    override fun onStart() {
                    }

                    override fun onSuccess(file: File?) {
                        it.resume(file)
                    }

                    override fun onError(e: Throwable?) {
                        e?.printStackTrace()
                        it.resume(null)
                    }

                }).launch()
        }
    }

    //查询目标
    val queryTargetInfo = MutableLiveData("")
    fun queryTargetInfo(vararg tracks: TrackingLog) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val bean = QueryTargetParam(userId.toLong())

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serReqJson = AppUtils.toSimpleJsonString(bean)
                        }
                    }

                    val result = MyRetrofitClient.service.queryTargetInfo(JsonUtils.getRequestJson(TAG, bean, QueryTargetParam::class.java))
                    LogUtils.e(TAG, "queryTargetInfo result = $result")
                    if (result.data != null) {
                        TargetBean().saveData(result)
                    }

                    if (tracks.isNotEmpty()) {
                        for (track in tracks) {
                            track.serResJson = AppUtils.toSimpleJsonString(result)
                            track.serResult =
                                if (result.code == HttpCommonAttributes.REQUEST_SUCCESS || result.code == HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA) "成功" else "失败"
                        }
                    }

                    queryTargetInfo.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(
                        TAG, "queryTargetInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className
                    )
                }

                val result = Response("", "queryTargetInfo e =$e", HttpCommonAttributes.SERVER_ERROR, "", null)
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }

                queryTargetInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //上传目标
    val uploadTargetInfo = MutableLiveData("")
    fun uploadTargetInfo(mTargetBean: TargetBean) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val bean = TargetSettingResponse()
                    bean.userId = userId.toLong()
                    bean.sportTarget = mTargetBean.sportTarget.toInt()
                    bean.sleepTarget = mTargetBean.sleepTarget.toInt()
                    bean.distanceTarget = mTargetBean.distanceTarget.toInt()
                    bean.consumeTarget = mTargetBean.consumeTarget.toInt()
                    bean.unit = mTargetBean.unit
                    bean.temperature = mTargetBean.temperature
                    val result = MyRetrofitClient.service.uploadTargetInfo(JsonUtils.getRequestJson(TAG, bean, TargetSettingResponse::class.java))
                    LogUtils.e(TAG, "uploadTargetInfo result = $result")
                    uploadTargetInfo.postValue(result.code)
                    userLoginOut(result.code)
                }

            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "uploadTargetInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                uploadTargetInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
            }
        }
    }

    //意见反馈
    val uploadFeedbackInfo = MutableLiveData("")
    fun uploadFeedbackInfo(body: RequestBody) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result = MyRetrofitClient.service.uploadFeedbackInfo(body)
                    LogUtils.e(TAG, "uploadFeedbackInfo result = $result")
                    uploadFeedbackInfo.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "uploadFeedbackInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                }
                uploadFeedbackInfo.postValue(HttpCommonAttributes.REQUEST_FAIL)
            }
        }
    }

//    fun userLoginOut(code : String){
//        if (code == HttpCommonAttributes.LOGIN_OUT){
//            ManageActivity.cancelAll()
//            BaseApplication.mContext.startActivity(Intent(BaseApplication.mContext , LoginActivity::class.java))
//        }
//    }

//    private fun calcSleepTime(time: String): Int {
//        val timeArray = time.trim().split(":")
//        val hour = timeArray[0].toInt()
//        val min = timeArray[1].toInt()
//        return hour * 60 + min
//    }

    val requestAgpsInfo = MutableLiveData<Response<AgpsResponse>>()
    fun requestAgpsInfo(vararg tracks: TrackingLog) {
        launchUI {
            try {
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.startTime = TrackingLog.getNowString()
                        track.serReqJson = ""
                    }
                }
                val result = MyRetrofitClient.service.requestAgpsInfo()
                LogUtils.e(TAG, "requestAgpsInfo result = ${result.data.dataUrl}")
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = if (result.code == HttpCommonAttributes.REQUEST_SUCCESS) "成功" else "失败"
                    }
                }
                userLoginOut(result.code)
                requestAgpsInfo.postValue(result)
            } catch (e: Exception) {
                val msg = StringBuilder()
                if (e is HttpException) {
                    //获取对应statusCode和Message
                    val exception: HttpException = e as HttpException
                    val message = exception.response()?.message()
                    val code = exception.response()?.code()
                    if (code != null && message != null) {
                        msg.append("Http code = $code message: $message\n")
                    }
                }
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "requestAgpsInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                    ErrorUtils.onLogResult("requestAgpsInfo e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className)
                    msg.append(e)
                }
                if (msg.isNotEmpty()) {
                    ErrorUtils.onLogResult("请求后台AGPS数据失败：msg:$msg network isAvailable:" + isAvailable)
                    ErrorUtils.onLogError(ErrorUtils.ERROR_MODE_REQUEST_BACKGROUND_DATA)
                }
                val result = Response("", msg.toString(), HttpCommonAttributes.REQUEST_FAIL, "", AgpsResponse())
                if (tracks.isNotEmpty()) {
                    for (track in tracks) {
                        track.serResJson = AppUtils.toSimpleJsonString(result)
                        track.serResult = "失败"
                    }
                }
                requestAgpsInfo.postValue(result)
            }
        }
    }

    val logoutResult = MutableLiveData("")
    fun serverLogout(psw: String) {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty()) {
                    val result = MyRetrofitClient.service.logout(JsonUtils.getRequestJson(TAG, LogoutBean(userId, psw), LogoutBean::class.java))
                    LogUtils.e(TAG, "serverLogout result = $result")
                    logoutResult.postValue(result.code)
                    userLoginOut(result.code)
                }
            } catch (e: Exception) {
                if (e.stackTrace.isNotEmpty()) {
                    val stackTrace = e.stackTrace[0]
                    LogUtils.e(TAG, "serverLogout e =$e" + "  =" + stackTrace.lineNumber + " calss = " + stackTrace.className, true)
                    getUserInfo.postValue(HttpCommonAttributes.SERVER_ERROR)
                }
            }
        }
    }

    //活跃用户统计
    fun appStart() {
        launchUI {
            try {
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                val startApp = StartAppBean()
                startApp.userId = userId
                startApp.phoneSystemLanguage = Locale.getDefault().language.toString()
                startApp.imei = "0"
                /*if (!DeviceUtils.getAndroidID().isNullOrEmpty()) {
                    startApp.imei = DeviceUtils.getAndroidID()
                }*/
                startApp.phoneType = AppUtils.getPhoneType()
                startApp.phoneSystemVersion = AppUtils.getOsVersion()
                startApp.phoneMac = /*DeviceUtils.getMacAddress()*/"0"
                startApp.appUnix = "${System.currentTimeMillis()}"
                startApp.country = Locale.getDefault().country
                //startApp.province
                startApp.city = SpUtils.getValue(SpUtils.WEATHER_CITY_NAME, "")
                var networkType: String = when (NetworkUtils.getNetworkType()) {
                    NetworkUtils.NetworkType.NETWORK_5G -> "5G"
                    NetworkUtils.NetworkType.NETWORK_4G -> "4G"
                    NetworkUtils.NetworkType.NETWORK_3G -> "3G"
                    NetworkUtils.NetworkType.NETWORK_2G -> "2G"
                    NetworkUtils.NetworkType.NETWORK_NO -> "NO"
                    NetworkUtils.NetworkType.NETWORK_WIFI -> "WIFI"
                    NetworkUtils.NetworkType.NETWORK_ETHERNET -> "ETHERNET"
                    NetworkUtils.NetworkType.NETWORK_UNKNOWN -> "UNKNOWN"
                    else -> "UNKNOWN"
                }
                startApp.internetType = networkType
                startApp.simType = NetworkUtils.getNetworkOperatorName()
                val spGps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")
                if (!TextUtils.isEmpty(spGps) && spGps.contains(",")) {
                    val gps = SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "").trim().split(",")
                    if (gps.isNotEmpty() && gps.size == 2) {
                        startApp.longitude = gps[1]
                        startApp.latitude = gps[0]
                    }
                }
                startApp.ip = /*NetworkUtils.getIPAddress(true)*/"0"
                startApp.phoneSystemArea = Locale.getDefault().getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
                MyRetrofitClient.service.startApp(JsonUtils.getRequestJson(TAG, startApp, StartAppBean::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
