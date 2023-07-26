package com.jwei.xzfit.ui.user

import android.app.Dialog
import android.content.Intent
import com.blankj.utilcode.util.NetworkUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseApplication
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.ActivityGooglefitBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.LogUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.utils.manager.GoogleFitManager

/**
 * Created by Android on 2022/3/16.
 */
class GoogleFitActivity : BaseActivity<ActivityGooglefitBinding, BaseViewModel>(
    ActivityGooglefitBinding::inflate, BaseViewModel::class.java
) {

    private val TAG = GoogleFitActivity::class.java.simpleName

    private lateinit var loadDialog: Dialog

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.google_fit))

        binding.tvDescribe.text = getString(R.string.google_fit_explanation)
        binding.mSwitch.isChecked = SpUtils.getGooglefitSwitch()

        binding.mSwitch.setOnClickListener {
            if (!::loadDialog.isInitialized) {
                loadDialog = DialogUtils.dialogShowLoad(this)
            } else {
                loadDialog.show()
            }
            NetworkUtils.isAvailableAsync { isAvailable ->
                if (!isAvailable) {
                    binding.mSwitch.isChecked = !binding.mSwitch.isChecked
                    ToastUtils.showToast(R.string.not_network_tips)
                    DialogUtils.dismissDialog(loadDialog)
                    return@isAvailableAsync
                }
                if (binding.mSwitch.isChecked) {
                    binding.mSwitch.isChecked = !binding.mSwitch.isChecked
                    if (!AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                        SpUtils.setGooglefitSwitch(false)
                        binding.mSwitch.isChecked = false
                        ToastUtils.showToast(R.string.phone_no_google_service)
                        DialogUtils.dismissDialog(loadDialog)
                        return@isAvailableAsync
                    }
                    //google fit 登录授权
                    GoogleFitManager.authorizationGoogleFit(this, object : GoogleFitManager.GoogleFitAuthListener {
                        override fun onAccessSucceeded() {
                            DialogUtils.dismissDialog(loadDialog)
                            SpUtils.setGooglefitSwitch(true)
                            binding.mSwitch.isChecked = true
                            LogUtils.d(TAG, "Google Fit 授权成功", true)
                            viewModel.traceSave("device_set", "googlefit__switch", "1")
                        }

                        override fun onFailure(msg: String) {
                            DialogUtils.dismissDialog(loadDialog)
                            SpUtils.setGooglefitSwitch(false)
                            binding.mSwitch.isChecked = false
                            ToastUtils.showToast(R.string.authorization_failed)
                            LogUtils.e(TAG, "Google Fit 授权失败：$msg", true)
                            viewModel.traceSave("device_set", "googlefit__switch", "0")
                        }
                    })
                } else {
                    //取消Google Fit授权
                    SpUtils.setGooglefitSwitch(false)
                    GoogleFitManager.deauthorizeGoogleFit(this, object : GoogleFitManager.GoogleFitAuthListener {
                        override fun onAccessSucceeded() {
                            LogUtils.e(TAG, "Google Fit 取消授权", true)
                            DialogUtils.dismissDialog(loadDialog)
                            viewModel.traceSave("device_set", "googlefit__switch", "0")
                        }

                        override fun onFailure(msg: String) {
                            DialogUtils.dismissDialog(loadDialog)
                        }
                    })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        GoogleFitManager.resultRequestPermissions(requestCode, resultCode, data)
    }
}