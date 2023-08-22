package com.smartwear.publicwatch.ui.user

import android.content.Intent
import android.text.TextUtils
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.NetworkUtils
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.ui.adapter.CommonAdapter
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.*
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.ui.debug.DebugFeedbackActivity
import com.smartwear.publicwatch.ui.login.privacy.PrivacyPolicyActivity
import com.smartwear.publicwatch.ui.login.privacy.UseAgreementActivity
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.ToastUtils


class AboutActivity : BaseActivity<ActivityAboutBinding, BaseViewModel>(ActivityAboutBinding::inflate, BaseViewModel::class.java), View.OnClickListener {
    private val list: MutableList<MutableMap<String, *>> = ArrayList()
    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.about)
        setViewsClickListener(
            this, binding.btnFinish,
            tvTitle!!
        )
        initRv()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            tvTitle!!.id -> {
                finish()
            }
            binding.btnFinish.id -> {

                val showDialogTwoBtn = DialogUtils.showDialogTwoBtn(
                    this, /*getString(R.string.dialog_title_tips)*/
                    null,
                    getString(R.string.login_out_tips),
                    getString(R.string.dialog_cancel_btn),
                    getString(R.string.dialog_confirm_btn),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            viewModel.userLoginOut(HttpCommonAttributes.USER_LOGIN_OUT)
                        }

                        override fun OnCancel() {
                        }

                    })
                if (!isFinishing && !isDestroyed)
                    showDialogTwoBtn.show()
            }
        }
    }


    override fun initData() {
        AppUpdateManager.checkUpdate(this, false)
    }


    private fun initRv() {
        fillData()
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@AboutActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.aboutNameList)
        for (index in texts.indices) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = texts[index]
            map["right"] = if (index == 2) {
                val builder = StringBuilder().append(AppUtils.getAppVersionName())
                if (AppUtils.isBetaApp()) {
                    builder.append("_Beta")
                }
                builder.toString()
            } else {
                ""
            }
            map["hasNext"] = index != 2
            list.add(map)
        }
        if (AppUtils.isBetaApp()) {
            list.add(mutableMapOf<String, String>().apply {
                put("content", "问题反馈")
                put("right", "")
            })
        }
    }


    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemPersonalInfoBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemPersonalInfoBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemPersonalInfoBinding {
                return ItemPersonalInfoBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemPersonalInfoBinding, t: MutableMap<String, *>, position: Int) {

                v.tvItemLeft.text = "${t["content"]}"
                v.tvItemRight.text = "${t["right"]}"
                if (TextUtils.equals(t["content"].toString(), getString(R.string.current_app_version))) {
                    v.tvItemRight.setCompoundDrawables(null, null, null, null)
                }

                if (position == (list.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }

                v.cslItemPersonalInfoParent.setOnClickListener {
                    try {
                        when (v.tvItemLeft.text.toString().trim()) {
                            getString(R.string.privacy_policy) -> {
                                NetworkUtils.isAvailableAsync { isAvailable ->
                                    if (!isAvailable) {
                                        ToastUtils.showToast(R.string.not_network_tips)
                                        return@isAvailableAsync
                                    }
                                    startActivity(Intent(this@AboutActivity, PrivacyPolicyActivity::class.java))
                                }
                            }
                            getString(R.string.user_agreement) -> {
                                NetworkUtils.isAvailableAsync { isAvailable ->
                                    if (!isAvailable) {
                                        ToastUtils.showToast(R.string.not_network_tips)
                                        return@isAvailableAsync
                                    }
                                    startActivity(Intent(this@AboutActivity, UseAgreementActivity::class.java))
                                }
                            }
                            getString(R.string.current_app_version) -> {
                                NetworkUtils.isAvailableAsync { isAvailable ->
                                    if (!isAvailable) {
                                        ToastUtils.showToast(R.string.not_network_tips)
                                        return@isAvailableAsync
                                    }
                                    AppUpdateManager.checkUpdate(this@AboutActivity, true)
                                }
                            }
                        }
                        /*if (AppUtils.isBetaApp()) {
                            if (TextUtils.equals("问题反馈", v.tvItemLeft.text.toString().trim())) {
                                startActivity(Intent(this@AboutActivity, DebugFeedbackActivity::class.java))
                            }
                        }*/
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        AppUpdateManager.updateInfoService?.handleActivityResult(requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        AppUpdateManager.updateInfoService?.handlePermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUpdateManager.onDestroy()
    }

}