package com.jwei.publicone.ui.login.register

import android.app.Dialog
import android.content.Intent
import android.text.InputFilter
import android.text.InputType
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityRegisterBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.ui.login.LoginActivity
import com.jwei.publicone.ui.login.privacy.PrivacyPolicyActivity
import com.jwei.publicone.ui.login.privacy.UseAgreementActivity
import com.jwei.publicone.ui.user.UserInfoActivity
import com.jwei.publicone.utils.*
import com.jwei.publicone.viewmodel.UserModel
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.blankj.utilcode.util.NetworkUtils
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.db.model.track.TrackingLog
import com.jwei.publicone.ui.region.RegionBean
import com.jwei.publicone.ui.region.RegionSettingActivity
import com.jwei.publicone.utils.manager.AppTrackingManager


class RegisterActivity : BaseActivity<ActivityRegisterBinding, UserModel>(ActivityRegisterBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = RegisterActivity::class.java.simpleName
    private var dialog: Dialog? = null
    private val filter = InputFilter { source, start, end, dest, dstart, dend ->
        if (source != null && !RegexUtils.inputFilterStr("" + source)) {
            ""
        } else null
    }

    private lateinit var registerForActivityResult: ActivityResultLauncher<Intent>

    //注册查询是否已注册异常日志埋点
    private val queryByLoginNameTrackingLog by lazy { TrackingLog.getSerTypeTrack("查询是否已注册", "判断该账号是否已注册", "ffit/user/queryByLoginName") }

    //注册日志埋点
    private val registerTrackingLog by lazy { TrackingLog.getSerTypeTrack("注册", "用户注册", "ffit/user/register") }

    override fun initView() {
        super.initView()
        initPrivacyAuthority()
        setViewsClickListener(
            this, binding.chHidePsw,
            binding.chConfirmHidePsw,
            binding.btnRegisterAndLogin,
            binding.chbPrivacyAuthority,
            binding.tvSelectLocale,
            binding.ivSelectLocale
        )
        observeData()

        binding.chbPrivacyAuthority.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                LogUtils.e(TAG, "chbPrivacyAuthority true")
                binding.btnRegisterAndLogin.setBackgroundResource(R.drawable.selector_public_button)
            } else {
                LogUtils.e(TAG, "chbPrivacyAuthority false")
                binding.btnRegisterAndLogin.setBackgroundResource(R.drawable.login_home_login_grey_btn)
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

        binding.chConfirmHidePsw.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.etConfirmPassWord.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                binding.etConfirmPassWord.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            }
            ViewUtils.setEditTextSelection(binding.etConfirmPassWord)
        }


        val array = arrayOf(filter, InputFilter.LengthFilter(20))
        binding.etPassWord.filters = array
        binding.etConfirmPassWord.filters = array
        //过滤emoji表情
        //val filters = arrayOf<InputFilter>(ProhibitEmojiUtils.inputFilterProhibitEmoji(50))
        binding.etPhoneOrEmail.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(50)
    }

    override fun initData() {
        super.initData()
        binding.tvSelectLocale.text = RegionSettingActivity.getRegionName(
            SpUtils.getValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, ""),
            SpUtils.getValue(SpUtils.SERVICE_REGION_AREA_CODE, "")
        )
        registerForActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                val mRegion = it.data?.getSerializableExtra(RegionSettingActivity.KEY_REGION) as RegionBean?
                if (mRegion != null) {
                    val name = RegionSettingActivity.getRegionName(mRegion.countryIsoCode, mRegion.areaCode)
                    binding.tvSelectLocale.text = name

                    SpUtils.setValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, mRegion.countryIsoCode)
                    SpUtils.setValue(SpUtils.SERVICE_REGION_AREA_CODE, mRegion.areaCode)
                    if (RegionSettingActivity.isChinaServiceUrl(mRegion.countryIsoCode, mRegion.areaCode)) {
                        SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE1)
                    } else {
                        SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE2)
                    }
                } else {
                    ToastUtils.showToast(R.string.select_region_no_select)
                }
                if (LocaleUtils.getSelectLocalLanguage()) {
                    binding.etPhoneOrEmail.hint = getString(R.string.register_account_tips)
                } else {
                    binding.etPhoneOrEmail.hint = getString(R.string.register_account_email_tips)
                }
            }
        }
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
            binding.chConfirmHidePsw.id -> {
                if (binding.chConfirmHidePsw.isChecked) {
                    binding.etConfirmPassWord.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    binding.etConfirmPassWord.inputType = (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                }
                ViewUtils.setEditTextSelection(binding.etConfirmPassWord)
            }
            binding.chbPrivacyAuthority.id -> {
                if (binding.chbPrivacyAuthority.isChecked) {
                    binding.btnRegisterAndLogin.setBackgroundResource(R.drawable.selector_public_button)
                } else {
                    binding.btnRegisterAndLogin.setBackgroundResource(R.drawable.login_home_login_grey_btn)
                }
            }
            binding.btnRegisterAndLogin.id -> {
                if (binding.chbPrivacyAuthority.isChecked) {
                    clickLoginBtn()
                } else {
                    ToastUtils.showToast(getString(R.string.select_privacy_authority_tips))
                }
            }
            binding.tvSelectLocale.id, binding.ivSelectLocale.id -> {
                registerForActivityResult.launch(Intent(this, RegionSettingActivity::class.java))
            }
        }
    }

    private fun observeData() {
        //检查是否已注册
        viewModel.queryByLoginName.observe(this, Observer {
            if (it != null) {
                com.blankj.utilcode.util.LogUtils.i(TAG, "queryByLoginName = $it")
                dismissDialog()

                queryByLoginNameTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                    if (it.code == HttpCommonAttributes.REQUEST_NOT_REGISTER) {
                        log = "用户未注册"
                    } else if (it.code == HttpCommonAttributes.REQUEST_REGISTERED) {
                        log = "用户已注册"
                    }
                }

                if (it.code != HttpCommonAttributes.REQUEST_NOT_REGISTER) {
                    if (it.code == HttpCommonAttributes.REQUEST_REGISTERED) {
                        //已注册
                        AppTrackingManager.trackingModule(
                            AppTrackingManager.MODULE_REGISTER,
                            queryByLoginNameTrackingLog.apply {
                                log += "\n用户已注册"
                            }, "1014", true
                        )
                    } else {
                        //查询失败
                        AppTrackingManager.trackingModule(
                            AppTrackingManager.MODULE_REGISTER,
                            queryByLoginNameTrackingLog.apply {
                                log += "\n查询是否已注册失败"
                            }, "1013", true
                        )
                    }
                }

                when (it.code) {
                    HttpCommonAttributes.REQUEST_REGISTERED -> {
                        LogUtils.i(TAG, "queryByLoginName = 用戶已注册")
                        dialog = null
                        dialog = DialogUtils.dialogShowContentAndTwoBtn(this, getString(R.string.register_dialog_query_by_login_name_center_tips),
                            getString(R.string.register_dialog_query_by_login_name_left_btn),
                            getString(R.string.register_dialog_query_by_login_name_right_btn),
                            object : DialogUtils.DialogClickListener {
                                override fun OnOK() {
                                    ManageActivity.removeActivity(LoginActivity::class.java)
                                    SpUtils.setValue(SpUtils.USER_NAME, binding.etPhoneOrEmail.text.toString().trim())
                                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                    intent.putExtra(INTENT_BASIC, binding.etPhoneOrEmail.text.toString().trim())
                                    startActivity(intent)
                                    finish()
                                }

                                override fun OnCancel() {}
                            })
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                        LogUtils.i(TAG, "queryByLoginName = 用户未注册")
                        viewModel.register(binding.etPhoneOrEmail.text.toString().trim(), binding.etPassWord.text.toString().trim(), type, registerTrackingLog)
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                }
            }
        })

        //注册
        viewModel.registerCode.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                registerTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                }
                var errorLog = ""
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        com.blankj.utilcode.util.LogUtils.i(TAG, "registerCode = 注册成功")
                        ToastUtils.showToast(getString(R.string.user_register_success_tips))
                        startActivity(Intent(this, UserInfoActivity::class.java))
                        finish()
                        ManageActivity.cancelAll()
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("11", "34").apply {
                            functionStatus = "1"
                        })
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        errorLog = getString(R.string.operation_failed_tips)
                        ToastUtils.showToast(errorLog)
                    }
                    HttpCommonAttributes.REQUEST_REGISTER_FAIL -> {
                        errorLog = getString(R.string.registration_failed_tips)
                        ToastUtils.showToast(errorLog)
                    }
                    HttpCommonAttributes.REQUEST_REGISTERED -> {
                        errorLog = getString(R.string.user_already_registered_tips)
                        ToastUtils.showToast(errorLog)
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        errorLog = getString(R.string.server_exception_tips)
                        ToastUtils.showToast(errorLog)
                    }
                }
                if (it != HttpCommonAttributes.REQUEST_SUCCESS) {
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_REGISTER,
                        registerTrackingLog.apply {
                            log +="\n注册失败"
                        }, "1015", true
                    )
                } else {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, registerTrackingLog)
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, TrackingLog.getEndTypeTrack("注册"), isEnd = true)
                }
            }
        })
    }

    private fun initPrivacyAuthority() {
        binding.tvPrivacyAuthority.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyAuthority.text =
            SpannableStringTool.get()
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
                        startActivity(Intent(this@RegisterActivity, UseAgreementActivity::class.java))
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
                        startActivity(Intent(this@RegisterActivity, PrivacyPolicyActivity::class.java))
                    }

                    override fun updateDrawState(ds: TextPaint) {
//                        ds.isUnderlineText = false
                    }
                })
                .append("》").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))
                .create()
    }

    var type = "1"
    private fun clickLoginBtn() {
        if (binding.etPhoneOrEmail.text.toString().trim().isEmpty()) {
            if (AppUtils.isZh(BaseApplication.mContext)) {
                ToastUtils.showToast(R.string.register_account_tips)
            } else {
                ToastUtils.showToast(R.string.register_account_email_tips)
            }
            return
        }
        if (binding.etPassWord.text.toString().trim().isEmpty()) {
            ToastUtils.showToast(R.string.register_psw_tips)
            return
        }
        if (binding.etConfirmPassWord.text.toString().trim().isEmpty()) {
            ToastUtils.showToast(getString(R.string.regex_psw_not_equal_tips))
            return
        }
        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, TrackingLog.getStartTypeTrack("注册"), isStart = true)
        if (!checkPhoneOrEmail()) {
            return
        }
        if (!checkPassword()) {
            return
        }
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showToast(getString(R.string.not_network_tips))
            return
        }
        dialog = DialogUtils.dialogShowLoad(this)


        viewModel.queryByLoginName(binding.etPhoneOrEmail.text.toString().trim(), queryByLoginNameTrackingLog)
    }


    private fun checkPhoneOrEmail(): Boolean {
        if (!binding.etPhoneOrEmail.text.isNullOrEmpty()) {
            val trackingLog = TrackingLog.getAppTypeTrack("检测邮箱或手机号").apply {
                log = "输入的邮箱或手机号：${binding.etPhoneOrEmail.text.toString().trim()}"
            }
            if (LocaleUtils.getSelectLocalLanguage()) {
                if (!RegexUtils.isMobileNO(binding.etPhoneOrEmail.text.toString().trim())) {
                    if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                        ToastUtils.showToast(getString(R.string.regex_phone_number_and_email_tips))
                        LogUtils.e(TAG, "邮箱或手机号格式错误：${binding.etPhoneOrEmail.text.toString().trim()}")
                        ErrorUtils.sendEmailError(binding.etPhoneOrEmail.text.toString().trim(), ErrorUtils.EMAIL_OR_PHONE_ERROR)
                        AppTrackingManager.trackingModule(
                            AppTrackingManager.MODULE_REGISTER,
                            trackingLog.apply {
                                log += "\n邮箱或手机号格式错误:${binding.etPhoneOrEmail.text.toString().trim()}"
                            }, "1010", true
                        )
                    } else {
                        type = "2"
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, trackingLog)
                        return true
                    }
                } else {
                    type = "1"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, trackingLog)
                    return true
                }
            } else {
                if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                    ToastUtils.showToast(getString(R.string.regex_email_tips))
                    LogUtils.e(TAG, "邮箱格式错误：${binding.etPhoneOrEmail.text.toString().trim()}")
                    ErrorUtils.sendEmailError(binding.etPhoneOrEmail.text.toString().trim(), ErrorUtils.EMAIL_ERROR)
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_REGISTER,
                        trackingLog.apply {
                            log += "\n邮箱格式错误:${binding.etPhoneOrEmail.text.toString().trim()}"
                        }, "1010", true
                    )
                } else {
                    type = "2"
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, trackingLog)
                    return true
                }
            }
        } else {
            ToastUtils.showToast(getString(R.string.user_name_null_tips))
        }
        return false
    }

    private fun checkPassword(): Boolean {
        if (!binding.etPassWord.text.isNullOrEmpty() && !binding.etConfirmPassWord.text.isNullOrEmpty()) {
            val trackingLog = TrackingLog.getAppTypeTrack("检测密码").apply {
                log = "输入的密码1：${binding.etPassWord.text.toString().trim()}\n密码2：${binding.etConfirmPassWord.text.toString().trim()}"
            }

            if (RegexUtils.passwordLimit(binding.etPassWord.text.toString().trim()) &&
                RegexUtils.passwordLimit(binding.etConfirmPassWord.text.toString().trim())
            ) {
                if (binding.etPassWord.text.toString().trim() == binding.etConfirmPassWord.text.toString().trim()) {
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_REGISTER, trackingLog)
                    return true
                } else {
                    ToastUtils.showToast(getString(R.string.regex_psw_not_equal_tips))
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_REGISTER,
                        trackingLog.apply {
                            log += "密码不一致 密码1：${binding.etPassWord.text.toString().trim()}\n" +
                                    "密码2：${binding.etConfirmPassWord.text.toString().trim()}\n"
                        }, "1011", true
                    )
                }
            } else {
                ToastUtils.showToast(getString(R.string.regex_psw_less_tips))
                AppTrackingManager.trackingModule(
                    AppTrackingManager.MODULE_REGISTER,
                    trackingLog.apply {
                        log += "\n密码格式错误 密码1：${binding.etPassWord.text.toString().trim()}\n" +
                                "密码2：${binding.etConfirmPassWord.text.toString().trim()}"
                    }, "1012", true
                )
            }
        } else if (binding.etPassWord.text.isNullOrEmpty()) {
            ToastUtils.showToast(getString(R.string.password_null_tips))
        } else if (binding.etConfirmPassWord.text.isNullOrEmpty()) {
            ToastUtils.showToast(getString(R.string.confirm_password_null_tips))
        }
        return false
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

}