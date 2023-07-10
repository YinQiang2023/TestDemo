package com.jwei.publicone.ui.login

import android.app.Dialog
import android.text.*
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.NetworkUtils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.databinding.FindPassWordActivityBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.utils.*
import com.jwei.publicone.viewmodel.UserModel

class FindPassWordActivity : BaseActivity<FindPassWordActivityBinding, UserModel>(FindPassWordActivityBinding::inflate, UserModel::class.java), View.OnClickListener,
    TextWatcher {

    //是否获取过验证码
    private var isGetCode = false

    private val filter = InputFilter { source, start, end, dest, dstart, dend ->
        if (source != null && !RegexUtils.inputFilterStr("" + source)) {
            ""
        } else null
    }

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.layoutTitle.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setViewsClickListener(this, binding.btOk, binding.btCode, binding.chHidePsw, binding.chConfirmHidePsw)
        val array = arrayOf(filter, InputFilter.LengthFilter(20))
        binding.etPassWord.filters = array
        binding.etConfirmPassWord.filters = array
        if (LocaleUtils.getSelectLocalLanguage()) {
            binding.etPhoneOrEmail.hint = getString(R.string.register_account_tips)
        } else {
            binding.etPhoneOrEmail.hint = getString(R.string.register_account_email_tips)
        }
        setBtCodeEnable()
        binding.etPhoneOrEmail.addTextChangedListener(this)

        //过滤emoji表情
        val filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(50)
        binding.etPhoneOrEmail.filters = filters
        binding.etCode.filters = filters

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


//        binding.btOk.setBackgroundResource(R.drawable.login_home_login_grey_btn)
    }

    private fun setBtCodeEnable() {
        val empty = TextUtils.isEmpty(binding.etPhoneOrEmail.text.toString().trim())
        binding.btCode.alpha = if (empty)
            0.5f else 1.0f
        binding.btCode.isClickable = !empty
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btOk.id -> {
                clickOk()
            }
            binding.btCode.id -> {
                clickOkToGetVerificationCode()
            }
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
        }
    }

    override fun initData() {
        super.initData()

        viewModel.queryByLoginName.observe(this, Observer {
            if (it != null) {
                dismissDialog()
                when (it.code) {
                    HttpCommonAttributes.REQUEST_REGISTERED -> {
                        getCode()
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER,
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                }
            }
        })

        viewModel.countdown.observe(this, Observer {
            if (it == 0) {
                binding.btCode.isEnabled = true
//                if(isGetCode){
//                    binding.btCode.text = resources.getString(R.string.again_get_code)
//                }else {
                binding.btCode.text = resources.getString(R.string.find_password_get_code)
//                }
                binding.btCode.setTextColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            } else {
                binding.btCode.setTextColor(ContextCompat.getColor(this, R.color.color_878787))
                binding.btCode.isEnabled = false
                binding.btCode.text = "$it s"
            }
        })

        viewModel.getCode.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        ToastUtils.showToast(getString(R.string.successful_operation_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_ERROR -> {
                        ToastUtils.showToast(getString(R.string.send_code_error))
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_FREQUENTLY -> {
                        ToastUtils.showToast(getString(R.string.send_code_frequently))
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        ToastUtils.showToast(getString(R.string.not_network_tips))

                    }
                }
            }
        })

        viewModel.findPassWordCode.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        ToastUtils.showToast(getString(R.string.find_password_modify_password_success))
                        finish()
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                        ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                    HttpCommonAttributes.REQUEST_CODE_INVALID -> {
                        //ToastUtils.showToast(getString(R.string.send_code_invalid))
                        ToastUtils.showToast(getString(R.string.send_code_mistake))
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        ToastUtils.showToast(getString(R.string.send_code_mistake))
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        ToastUtils.showToast(getString(R.string.not_network_tips))

                    }
                }
            }
        })


    }

    private fun getCode() {
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showToast(getString(R.string.not_network_tips))
            return
        }
        viewModel.getCode(binding.etPhoneOrEmail.text.toString().trim(), type, "2")
        if (type == "1") {
            viewModel.countdown(60)
        } else viewModel.countdown(60)
    }

    private fun clickOk() {
        if (binding.etPhoneOrEmail.text.toString().trim().isEmpty()) {
            if (AppUtils.isZh(BaseApplication.mContext)) {
                ToastUtils.showToast(R.string.register_account_tips)
            } else {
                ToastUtils.showToast(R.string.register_account_email_tips)
            }
            return
        }
        if (!checkPhoneOrEmail()) {
            return
        }
        if (!checkCode()) {
            return
        }
        if (!checkPassword()) {
            return
        }
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showToast(getString(R.string.not_network_tips))
            return
        }
        viewModel.findPassWord(binding.etPhoneOrEmail.text.toString().trim(), binding.etPassWord.text.toString().trim(), binding.etCode.text.toString().trim())
        dialog = DialogUtils.dialogShowLoad(this)
    }

    private fun clickOkToGetVerificationCode() {
        if (binding.etPhoneOrEmail.text.toString().trim().isEmpty()) {
            if (AppUtils.isZh(BaseApplication.mContext)) {
                ToastUtils.showToast(R.string.register_account_tips)
            } else {
                ToastUtils.showToast(R.string.register_account_email_tips)
            }
            return
        }
        if (!checkPhoneOrEmail()) {
            return
        }
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showToast(getString(R.string.not_network_tips))
            return
        }
        isGetCode = true
        if (dialog == null)
            dialog = DialogUtils.dialogShowLoad(this)
        else dialog?.show()
        viewModel.queryByLoginName(binding.etPhoneOrEmail.text.toString().trim())
    }

    private fun checkCode(): Boolean {
        if (TextUtils.isEmpty(binding.etCode.text)) {
            ToastUtils.showToast(getString(R.string.find_password_code))
            return false
        }
        return true
    }

    var type = "1"
    private fun checkPhoneOrEmail(): Boolean {
        if (!binding.etPhoneOrEmail.text.isNullOrEmpty()) {
            if (LocaleUtils.getSelectLocalLanguage()) {
                if (!RegexUtils.isMobileNO(binding.etPhoneOrEmail.text.toString().trim())) {
                    if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                        ToastUtils.showToast(getString(R.string.regex_phone_number_and_email_tips))
                    } else {
                        type = "2"
                        return true
                    }
                } else {
                    type = "1"
                    return true
                }
            } else {
                if (!RegexUtils.isEmail(binding.etPhoneOrEmail.text.toString().trim())) {
                    ToastUtils.showToast(getString(R.string.regex_email_tips))
                } else {
                    type = "2"
                    return true
                }
            }

        } else {
            ToastUtils.showToast(getString(R.string.regex_phone_number_and_email_tips))
        }
        return false
    }

    private fun checkPassword(): Boolean {
        if (!binding.etPassWord.text.isNullOrEmpty() && !binding.etConfirmPassWord.text.isNullOrEmpty()) {
            if (RegexUtils.passwordLimit(binding.etPassWord.text.toString().trim()) &&
                RegexUtils.passwordLimit(binding.etConfirmPassWord.text.toString().trim())
            ) {
                if (binding.etPassWord.text.toString().trim() == binding.etConfirmPassWord.text.toString().trim()) {
                    return true
                } else {
                    ToastUtils.showToast(getString(R.string.regex_psw_not_equal_tips))
                }
            } else {
                ToastUtils.showToast(getString(R.string.regex_psw_less_tips))
            }
        } else if (binding.etPassWord.text.isNullOrEmpty()) {
            //ToastUtils.showToast(getString(R.string.password_null_tips))
            ToastUtils.showToast(getString(R.string.register_psw_tips))
        } else if (binding.etConfirmPassWord.text.isNullOrEmpty()) {
            //ToastUtils.showToast(getString(R.string.confirm_password_null_tips))
            ToastUtils.showToast(getString(R.string.regex_psw_not_equal_tips))
        }
        return false
    }

    private var dialog: Dialog? = null
    private fun dismissDialog() {
//        if (dialog != null && dialog!!.isShowing) {
//            dialog?.dismiss()
//        }
        DialogUtils.dismissDialog(dialog)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
//        if (binding.etPhoneOrEmail.text.toString().isNotEmpty()) {
        viewModel.countdown.value?.let {
            if (it == 0) {
                setBtCodeEnable()
            }
        }
//        }

//        if (binding.etPhoneOrEmail.text.toString().isNotEmpty() && binding.etCode.text.toString().isNotEmpty() &&
//                binding.etPassWord.text.toString().isNotEmpty() && binding.etConfirmPassWord.text.toString().isNotEmpty()) {
//            binding.btOk.setBackgroundResource(R.drawable.selector_public_button)
//        } else {
//            binding.btOk.setBackgroundResource(R.drawable.login_home_login_grey_btn)
//        }
    }
}