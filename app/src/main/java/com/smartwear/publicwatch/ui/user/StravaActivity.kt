package com.smartwear.publicwatch.ui.user

import android.app.Dialog
import android.content.Intent
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.NetworkUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.ActivityStravaBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.utils.LogUtils
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.utils.manager.StravaManager

/**
 * Created by Android on 2022/5/14.
 */
class StravaActivity : BaseActivity<ActivityStravaBinding, BaseViewModel>(
    ActivityStravaBinding::inflate,
    BaseViewModel::class.java
) {
    private val TAG = StravaActivity::class.java.simpleName

    private lateinit var loadDialog: Dialog

    private val LOGIN_CODE = 0x001

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(getString(R.string.strava))

        binding.tvDescribe.text = getString(R.string.strava_explanation)
        binding.mSwitch.isChecked = SpUtils.getStravaSwitch()
        binding.tvStatus.text = getString(if (SpUtils.getStravaSwitch()) R.string.authorized else R.string.unauthorized)
        binding.tvStatus.setTextColor(
            ContextCompat.getColor(
                this@StravaActivity,
                if (SpUtils.getStravaSwitch()) R.color.app_index_color else R.color.color_878787
            )
        )

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
                SpUtils.setStravaSwitch(binding.mSwitch.isChecked)
                if (binding.mSwitch.isChecked) {
                    binding.mSwitch.isChecked = !binding.mSwitch.isChecked
                    //开始授权
                    StravaManager.authorizationStrava(this, object : StravaManager.StravaAuthListener {
                        override fun onAccessSucceeded() {
                            DialogUtils.dismissDialog(loadDialog)
                            SpUtils.setStravaSwitch(true)
                            binding.mSwitch.isChecked = true
                            LogUtils.d(TAG, "Strava 授权登录成功", true)
                            binding.tvStatus.setTextColor(
                                ContextCompat.getColor(
                                    this@StravaActivity,
                                    if (SpUtils.getStravaSwitch()) R.color.app_index_color else R.color.color_878787
                                )
                            )
                            binding.tvStatus.text = getString(if (SpUtils.getStravaSwitch()) R.string.authorized else R.string.unauthorized)
                            viewModel.traceSave("device_set", "strava_switch", "1")
                        }

                        override fun onFailure(msg: String) {
                            DialogUtils.dismissDialog(loadDialog)
                            SpUtils.setStravaSwitch(false)
                            binding.mSwitch.isChecked = false
                            binding.tvStatus.setTextColor(
                                ContextCompat.getColor(
                                    this@StravaActivity,
                                    if (SpUtils.getStravaSwitch()) R.color.app_index_color else R.color.color_878787
                                )
                            )
                            binding.tvStatus.text = getString(if (SpUtils.getStravaSwitch()) R.string.authorized else R.string.unauthorized)
                            ToastUtils.showToast(R.string.authorization_failed)
                            LogUtils.e(TAG, "Strava 授权登录失败：$msg", true)
                            viewModel.traceSave("device_set", "strava_switch", "0")
                        }
                    })
                } else {
                    //关闭授权
                    SpUtils.setStravaSwitch(false)
                    binding.tvStatus.setTextColor(ContextCompat.getColor(this@StravaActivity, if (SpUtils.getStravaSwitch()) R.color.app_index_color else R.color.color_878787))
                    binding.tvStatus.text = getString(if (SpUtils.getStravaSwitch()) R.string.authorized else R.string.unauthorized)
                    StravaManager.deauthorizeStrava(object : StravaManager.StravaAuthListener {
                        override fun onAccessSucceeded() {
                            LogUtils.e(TAG, "Strava 取消授权", true)
                            DialogUtils.dismissDialog(loadDialog)
                            viewModel.traceSave("device_set", "strava_switch", "0")
                        }

                        override fun onFailure(msg: String) {
                            //取消授权永远成功
                            DialogUtils.dismissDialog(loadDialog)
                        }
                    })
                }

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        StravaManager.resultRequestPermissions(requestCode, resultCode, data)
    }
}