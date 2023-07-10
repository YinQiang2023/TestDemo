package com.jwei.publicone.ui.login

import android.app.Dialog
import android.content.Intent
import android.text.InputFilter
import android.text.InputType
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.NetworkUtils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.databinding.LoginActivityBinding
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.Response
import com.jwei.publicone.https.response.GetUserInfoResponse
import com.jwei.publicone.ui.GlobalEventManager
import com.jwei.publicone.ui.HomeActivity
import com.jwei.publicone.ui.login.privacy.PrivacyPolicyActivity
import com.jwei.publicone.ui.login.privacy.UseAgreementActivity
import com.jwei.publicone.ui.login.register.RegisterActivity
import com.jwei.publicone.ui.user.UserInfoActivity
import com.jwei.publicone.ui.user.bean.TargetBean
import com.jwei.publicone.utils.*
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.viewmodel.DeviceModel
import com.jwei.publicone.viewmodel.UserModel

class LoginActivity : BaseActivity<LoginActivityBinding, UserModel>(LoginActivityBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = LoginActivity::class.java.simpleName
    private var dialog: Dialog? = null
    private var isSsoLogin: Boolean? = false
    private val filter = InputFilter { source, start, end, dest, dstart, dend ->
        if (source != null && !RegexUtils.inputFilterStr("" + source)) {
            ""
        } else null
    }

    //登录
    private val loginTrackingLog by lazy { TrackingLog.getSerTypeTrack("登录", "用户登录", "ffit/user/login") }

    //游客登录
    private val ssologinTrackingLog by lazy { TrackingLog.getSerTypeTrack("游客登录", "第三方登录接口", "ffit/thirdParty/ssoLogin") }

    //查询中服是否已注册
    private val queryByLoginNameTrackingLog by lazy { TrackingLog.getSerTypeTrack("外服未注册查询中服是否注册", "判断该账号是否已注册", "ffit/user/queryByLoginName") }

    //获取用户信息
    private val getUserInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("获取用户信息", "根据用户ID查询用户基本信息", "ffit/userInfo/getUserInfo") }

    //获取目标数据
    private val queryTargetInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("获取目标数据", "查询目标设置信息", "ffit/calibration/queryByUserID") }

    override fun initView() {
        super.initView()
        val inputPhoneOrName = SpUtils.getValue(SpUtils.USER_NAME, "")
        if (!TextUtils.isEmpty(inputPhoneOrName)) {
            binding.etPhoneOrEmail.setText(inputPhoneOrName.trim())
            ViewUtils.setEditTextSelection(binding.etPhoneOrEmail)
        }
        initPrivacyAuthority()
        setViewsClickListener(
            this, binding.chHidePsw, binding.chbPrivacyAuthority,
            binding.ivRegister, binding.ivForgetPassword, binding.btnLogin, binding.tvGuestLogin
        )

        binding.layoutTitle.tvTitle.visibility = View.GONE

        binding.chbPrivacyAuthority.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                LogUtils.e(TAG, "chbPrivacyAuthority true")
                binding.btnLogin.setBackgroundResource(R.drawable.selector_public_button)
            } else {
                LogUtils.e(TAG, "chbPrivacyAuthority false")
                binding.btnLogin.setBackgroundResource(R.drawable.login_home_login_grey_btn)
            }
        }

        binding.chHidePsw.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.etPassWord.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.etPassWord.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            }
            ViewUtils.setEditTextSelection(binding.etPassWord)
        }

        val array = arrayOf(filter, InputFilter.LengthFilter(20))
        binding.etPassWord.filters = array
        //过滤emoji表情
        /*val filters = arrayOf<InputFilter>(
            ProhibitEmojiUtils.inputFilterProhibitEmoji(50) *//*,
                SysUtils.getInputFilterProhibitSP()*//*
        )*/
        binding.etPhoneOrEmail.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(50)
    }

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.layoutTitle.layoutTitle.id
    }

    override fun onResume() {
        super.onResume()
        if (LocaleUtils.getSelectLocalLanguage()) {
            binding.etPhoneOrEmail.hint = getString(R.string.register_account_tips)
        } else {
            binding.etPhoneOrEmail.hint = getString(R.string.register_account_email_tips)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.chHidePsw.id -> {
                if (binding.chHidePsw.isChecked) {
                    binding.etPassWord.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    binding.etPassWord.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                }
                ViewUtils.setEditTextSelection(binding.etPassWord)
            }
            binding.ivRegister.id -> {
                startActivity(Intent(this, RegisterActivity::class.java))
            }
            binding.ivForgetPassword.id -> {
                startActivity(Intent(this, FindPassWordActivity::class.java))
            }

            binding.btnLogin.id -> {
                if (binding.chbPrivacyAuthority.isChecked) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getStartTypeTrack("登录"), isStart = true)
                    clickLoginBtn()
                } else {
                    ToastUtils.showToast(getString(R.string.select_privacy_authority_tips))
                }
            }
            binding.tvGuestLogin.id -> {
                if (binding.chbPrivacyAuthority.isChecked) {
                    try {
                        var uuid: String = SpUtils.getValue(SpUtils.TOURIST_UUID, "")
                        if (uuid.length == 0) {
                            val a = (Math.random() * 7000000).toInt() + 1000000//a是已经生成的随机数
                            uuid = System.currentTimeMillis().toString() + a.toString()
                            SpUtils.setValue(SpUtils.TOURIST_UUID, uuid)
                        }
                        viewModel.ssoLogin(uuid, ssologinTrackingLog)
                        dialog = DialogUtils.dialogShowLoad(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    ToastUtils.showToast(getString(R.string.select_privacy_authority_tips))
                }
            }
        }
    }

    override fun initData() {
        viewModel.error.observe(this) {
            Log.e(TAG, "it = $it")
        }

        viewModel.loginCode.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                Log.i(TAG, "loginCode = it = $it")


                if (it != HttpCommonAttributes.REQUEST_SUCCESS &&
                    it != HttpCommonAttributes.REQUEST_NOT_REGISTER &&
                    it != HttpCommonAttributes.REQUEST_PASSWORD_ERROR) {
                    dismissDialog()
                    binding.btnLogin.isEnabled = true

                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        loginTrackingLog.apply {
                            log += "\n登录失败"
                        }, "1113", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, loginTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                        if (it == HttpCommonAttributes.REQUEST_SUCCESS) {
                            log += "\n登录成功"
                        }
                    })
                }

                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        Log.i(TAG, "loginCode = 成功")
//                        ToastUtils.showToast(getString(R.string.successful_login_tips))
                        //dialog?.show()
                        //获取用户信息
                        isSsoLogin = false
                        viewModel.getUserInfo(getUserInfoTrackingLog)
                        DeviceModel().getProductList()
                        GlobalEventManager.isCanShowFirmwareUpgrade = true
                        GlobalEventManager.isCanUpdateAgps = true
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("11", "33").apply {
                            functionStatus = "1"
                        })
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                        //用户选择国内服务器，请求保持不变
                        if (SpUtils.getValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_DEFAULT) == SpUtils.SERVICE_ADDRESS_TO_TYPE1) {
                            ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                            dismissDialog()
                            binding.btnLogin.isEnabled = true
                            AppTrackingManager.trackingModule(
                                AppTrackingManager.MODULE_LOGIN,
                                TrackingLog.getAppTypeTrack("未注册")/*, "1118", true*/
                            )
                        } else {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getAppTypeTrack("未注册").apply {
                                log = "国外服务器，执行查询中国服务器是否存在旧用户数据"
                            })
                            //欧洲用户登录-->用户不存在-->中国区查询这个账号是否注册（服务器下发用户注册区域，老用户没有注册区域，新用户肯定有）-->如果是老用户，切中国区登录；新用户提示用户不存在，引导用户注册
                            viewModel.queryChinaByLoginName(binding.etPhoneOrEmail.text.toString().trim(), queryByLoginNameTrackingLog)
                        }

                    }
                    HttpCommonAttributes.REQUEST_PASSWORD_ERROR -> {
                        //ToastUtils.showToast(getString(R.string.password_error_tips))
                        ToastUtils.showToast(getString(R.string.login_err_tips))
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        //ToastUtils.showToast(getString(R.string.server_exception_tips))
                        ToastUtils.showToast(getString(R.string.not_network_tips))
                    }
                }
            }
        })

        viewModel.queryChinaByLoginName.observe(this, Observer {
            dismissDialog()
            binding.btnLogin.isEnabled = true

            if (it.code != HttpCommonAttributes.REQUEST_NOT_REGISTER && it.code != HttpCommonAttributes.REQUEST_REGISTERED) {
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_LOGIN,
                    queryByLoginNameTrackingLog.apply {
                        log += "\n查询是否已注册失败"
                    }, "1119", true
                )
            } else {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, queryByLoginNameTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                    if (it.code == HttpCommonAttributes.REQUEST_REGISTERED) {
                        log = "用户已注册"
                    }
                })
            }

            when (it.code) {
                HttpCommonAttributes.REQUEST_REGISTERED -> {
                    //（服务器下发用户注册区域，老用户没有注册区域，新用户肯定有）-->如果是老用户，切中国区登录；新用户提示用户不存在，引导用户注册
                    if (it.data != null && it.data.registrationArea.isNullOrEmpty()) {
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getAppTypeTrack("登录"))
                        loginTrackingLog.startTime = TrackingLog.getNowString()
                        loginTrackingLog.log = "外服登录无账号，中服存在老用户信息，切中国区登录"
                        SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE1)
                        clickLoginBtn()
                    } else {
                        //用户未注册
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))

                        AppTrackingManager.trackingModule(
                            AppTrackingManager.MODULE_LOGIN,
                            TrackingLog.getAppTypeTrack("中服已注册是新用户，提示未注册")/*, "1120", true*/
                        )
                    }
                    /*SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE1)
                    clickLoginBtn()*/
                }
                HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                    //用户未注册
                    ToastUtils.showToast(getString(R.string.user_not_registered_tips))

                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        TrackingLog.getAppTypeTrack("用户未注册")/*, "1118", true*/
                    )
                }
                else -> {
                    //ToastUtils.showToast(getString(R.string.server_exception_tips))
                    //用户未注册
                    ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                }
            }
        })

        viewModel.ssoLoginCode.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                Log.i(TAG, "ssoLoginCode = it = $it")



                if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                    dismissDialog()

                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        ssologinTrackingLog.apply {
                            log += "\n游客登录失败"
                        }, "1111", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, ssologinTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                    })
                }
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        Log.i(TAG, "ssoLoginCode = 成功")
//                        ToastUtils.showToast(getString(R.string.successful_login_tips))
                        //dialog?.show()
                        //获取用户信息
                        isSsoLogin = true
                        viewModel.getUserInfo()
                        DeviceModel().getProductList()
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("11", "35").apply {
                            functionStatus = "1"
                        })
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                    HttpCommonAttributes.REQUEST_PASSWORD_ERROR -> {
                        //ToastUtils.showToast(getString(R.string.password_error_tips))
                        ToastUtils.showToast(getString(R.string.login_err_tips))
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        //ToastUtils.showToast(getString(R.string.server_exception_tips))
                        ToastUtils.showToast(getString(R.string.not_network_tips))
                    }
                }
            }
        })

        viewModel.getUserInfo.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                Log.i(TAG, "getUserInfo = it = $it")

                if (it != HttpCommonAttributes.REQUEST_SUCCESS && it != HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA) {
                    dismissDialog()
                    binding.btnLogin.isEnabled = true

                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        getUserInfoTrackingLog.apply {
                            log += "\n获取用户信息失败"
                        }, "1114", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, getUserInfoTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                    })
                }
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        Log.i(TAG, "getUserInfo = 成功")
                        //查询目标数据
                        //dialog?.show()
                        if (viewModel.cacheUserInfo != null) {
                            val result: Response<GetUserInfoResponse>? = viewModel.cacheUserInfo
                            if (result!!.data != null) {
                                val height = if (!TextStringUtils.isNull(result.data.height)) result.data.height else ""
                                if (height.isEmpty()) {
                                    startActivity(Intent(this, UserInfoActivity::class.java))
                                    finish()
                                    ManageActivity.cancelAll()

                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getEndTypeTrack("登录"), isEnd = true)
                                } else {
                                    viewModel.queryTargetInfo(queryTargetInfoTrackingLog)
                                }
                            } else {
                                viewModel.queryTargetInfo(queryTargetInfoTrackingLog)
                            }
                        } else {
                            viewModel.queryTargetInfo(queryTargetInfoTrackingLog)
                        }
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        startActivity(Intent(this, UserInfoActivity::class.java))
                        finish()
                        ManageActivity.cancelAll()
                    }
                }
            }
        })

        viewModel.queryTargetInfo.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                Log.i(TAG, "queryTargetInfo = it = $it")



                if (it != HttpCommonAttributes.REQUEST_SUCCESS && it != HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA) {
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        queryTargetInfoTrackingLog.apply {
                            log += "\n获取目标数据失败"
                        }, "1115", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, queryTargetInfoTrackingLog.apply {
                        endTime = TrackingLog.getNowString()
                    })
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getEndTypeTrack("登录"), isEnd = true)
                }

                dismissDialog()
                binding.btnLogin.isEnabled = true
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        //跳转到主页
                        SpUtils.setValue(SpUtils.USER_IS_LOGIN, "1")
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                        ManageActivity.cancelAll()
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        TargetBean().saveNullData()
                        //跳转到主页
                        SpUtils.setValue(SpUtils.USER_IS_LOGIN, "1")
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                        ManageActivity.cancelAll()
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                }
            }
        })
    }

    private fun initPrivacyAuthority() {
        binding.tvPrivacyAuthority.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyAuthority.text = SpannableStringTool.get()
            .append(getString(R.string.privacy_statement_1))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
            .append("《").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.user_agreement))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
//            .setUnderline()
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@LoginActivity, UseAgreementActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
//                        ds.isUnderlineText = false
                }
            })
            .append("》").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.sign_of_coordination))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.color_878787))
            .append("《").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.privacy_policy))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
//            .setUnderline()
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@LoginActivity, PrivacyPolicyActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
//                        ds.isUnderlineText = false
                }
            })
            .append("》").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .create()
    }

    private fun clickLoginBtn() {
        binding.btnLogin.isEnabled = false
        NetworkUtils.isAvailableAsync { isAvailable ->
            if (!isAvailable) {
                ToastUtils.showToast(getString(R.string.not_network_tips))
                binding.btnLogin.isEnabled = true
                return@isAvailableAsync
            }
            if (binding.etPhoneOrEmail.text.toString().trim().isEmpty()) {
                if (AppUtils.isZh(BaseApplication.mContext)) {
                    ToastUtils.showToast(R.string.register_account_tips)
                } else {
                    ToastUtils.showToast(R.string.register_account_email_tips)
                }
                binding.btnLogin.isEnabled = true
                return@isAvailableAsync
            }
            if (!checkUserName()) {
                binding.btnLogin.isEnabled = true
                return@isAvailableAsync
            }
            if (!checkPassword()) {
                binding.btnLogin.isEnabled = true
                return@isAvailableAsync
            }
            viewModel.login(binding.etPhoneOrEmail.text.toString().trim(), binding.etPassWord.text.toString().trim(), type, loginTrackingLog)
            dialog = DialogUtils.dialogShowLoad(this)
        }
    }

    var type = "1"
    private fun checkUserName(): Boolean {
        if (!binding.etPhoneOrEmail.text.isNullOrEmpty()) {
            val trackingLog = TrackingLog.getAppTypeTrack("检测邮箱或手机号").apply {
                log = "输入的邮箱或手机号：${binding.etPhoneOrEmail.text.toString().trim()}"
            }
            if (LocaleUtils.getSelectLocalLanguage()) {
                if (!RegexUtils.isMobileNO(binding.etPhoneOrEmail.text.toString().trim())) {
                    if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                        ToastUtils.showToast(getString(R.string.regex_phone_number_and_email_tips))
                        //ToastUtils.showToast(getString(R.string.login_err_tips))
                        AppTrackingManager.trackingModule(
                            AppTrackingManager.MODULE_LOGIN,
                            trackingLog.apply {
                                log += "\n错误邮箱或手机号:${binding.etPhoneOrEmail.text.toString().trim()}\n邮箱或手机号格式错误"
                            }, "1110", true
                        )
                    } else {
                        type = "2"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, trackingLog)
                        return true
                    }
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, trackingLog)
                    type = "1"
                    return true
                }
            } else {
                if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                    ToastUtils.showToast(getString(R.string.regex_email_tips))
//                    ToastUtils.showToast(getString(R.string.login_err_tips))
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        trackingLog.apply {
                            log += "\n错误邮箱或手机号:${binding.etPhoneOrEmail.text.toString().trim()}\n邮箱格式错误"
                        }, "1110", true
                    )
                } else {
                    type = "2"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, trackingLog)
                    return true
                }
            }
        } else {
            //ToastUtils.showToast(getString(R.string.user_name_null_tips))
            ToastUtils.showToast(getString(R.string.login_err_tips))
        }
        return false
    }

    private fun checkPassword(): Boolean {
        val trackingLog = TrackingLog.getAppTypeTrack("检测密码").apply {
            log = "输入的密码：${binding.etPassWord.text.toString().trim()}}"
        }
        if (!binding.etPassWord.text.isNullOrEmpty()) {
            if (RegexUtils.passwordLimit(binding.etPassWord.text.toString().trim())) {
                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, trackingLog)
                return true
            } else {
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_LOGIN,
                    trackingLog.apply {
                        log += "\n错误格式的密码：${binding.etPassWord.text.toString().trim()}}"
                    }, "1112", true
                )
            }
            ToastUtils.showToast(getString(R.string.regex_psw_less_tips))
        } else {
            ToastUtils.showToast(getString(R.string.register_psw_tips))
//            ToastUtils.showToast(getString(R.string.password_null_tips))
//            ToastUtils.showToast(getString(R.string.login_err_tips))
        }
        return false
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

    override fun onBackPress() {
        //super.onBackPress()
        com.blankj.utilcode.util.AppUtils.exitApp()
    }
}