package com.smartwear.xzfit.ui.user

import android.app.Dialog
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import com.blankj.utilcode.util.*
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.WriteOffActivityBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.utils.ManageActivity
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.viewmodel.UserModel

/**
 * Created by Android on 2022/2/14.
 */
class WriteOffActivity : BaseActivity<WriteOffActivityBinding, UserModel>(WriteOffActivityBinding::inflate, UserModel::class.java), View.OnClickListener {
    private var dialog: Dialog? = null
    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.write_off)

        setViewsClickListener(
            this, binding.btnNext,
            binding.btnNext2,
            binding.btnConfirm,
            binding.tvVerifyTips,
            tvTitle!!
        )

        binding.chVerifyTips.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.btnNext2.setBackgroundResource(R.drawable.selector_public_button)
            } else {
                binding.btnNext2.setBackgroundResource(R.drawable.login_home_login_grey_btn)
            }
        }

        binding.etPassWord.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (it.toString().trim().isEmpty()) {
                        binding.btnConfirm.setBackgroundResource(R.drawable.login_home_login_grey_btn)
                    } else {
                        binding.btnConfirm.setBackgroundResource(R.drawable.selector_public_button)
                    }
                }
            }

        })

        val accountType = SpUtils.getValue(SpUtils.ACCOUNT_TYPE, "")
        if (accountType == "5") {
            binding.etPassWord.setVisibility(View.GONE)
            binding.appCompatTextView10.setVisibility(View.GONE)
            binding.btnConfirm.setBackgroundResource(R.drawable.selector_public_button)
        } else {
            binding.etPassWord.setVisibility(View.VISIBLE)
            binding.appCompatTextView10.setVisibility(View.VISIBLE)
            binding.btnConfirm.setBackgroundResource(R.drawable.login_home_login_grey_btn)
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                binding.btnNext.id -> {
                    //binding.flipper.showNext()
                    binding.flipper.displayedChild = 1
                }
                binding.tvVerifyTips.id -> {
                    binding.chVerifyTips.isChecked = !binding.chVerifyTips.isChecked
                }
                binding.btnNext2.id -> {
                    if (binding.chVerifyTips.isChecked) {
                        //binding.flipper.showNext()
                        binding.flipper.displayedChild = 2
                    }
                }
                tvTitle!!.id -> {
                    if (!isDisableBack) {
                        finish()
                    }
                }
                binding.btnConfirm.id -> {
                    var psw = binding.etPassWord.text.toString().trim()
                    val accountType = SpUtils.getValue(SpUtils.ACCOUNT_TYPE, "")
                    if (accountType == "5") {
                        psw = "123456"
                    }
                    if (psw.isEmpty()) {
                        return
                    }
                    val tast = NetworkUtils.isAvailableAsync { isAvailable ->
                        if (!isAvailable) {
                            ToastUtils.showShort(getString(R.string.not_network_tips))
                            return@isAvailableAsync
                        }
                        dialog = DialogUtils.dialogShowLoad(this@WriteOffActivity)
                        viewModel.serverLogout(psw)
                    }
                }
            }
        }
    }

    override fun initData() {
        super.initData()
        viewModel.logoutResult.observe(this) { it ->
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        com.smartwear.xzfit.utils.ToastUtils.showToast(getString(R.string.successful_operation_tips))
                        isDisableBack = true
                        ThreadUtils.runOnUiThreadDelayed({
                            viewModel.loginout()
                            SpUtils.putValue(SpUtils.LAST_DEVICE_LOGIN_TIME, "")
                            SpUtils.putValue(SpUtils.USER_NAME, "")
                            ManageActivity.cancelAll()
                            BaseApplication.isShowIngOfflineDialog = false
                            ProcessUtils.killAllBackgroundProcesses()
                            AppUtils.relaunchApp()
                        }, 2000)
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        com.smartwear.xzfit.utils.ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.REQUEST_NOT_REGISTER -> {
                        com.smartwear.xzfit.utils.ToastUtils.showToast(getString(R.string.user_not_registered_tips))
                    }
                    HttpCommonAttributes.REQUEST_PASSWORD_ERROR -> {
                        com.smartwear.xzfit.utils.ToastUtils.showToast(getString(R.string.password_error_tips))
                    }
                    HttpCommonAttributes.SERVER_ERROR -> {
                        com.smartwear.xzfit.utils.ToastUtils.showToast(getString(R.string.err_network_tips))
                    }
                }
            }
        }
    }

    private var isDisableBack = false

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isDisableBack) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog, 1000)
    }
}