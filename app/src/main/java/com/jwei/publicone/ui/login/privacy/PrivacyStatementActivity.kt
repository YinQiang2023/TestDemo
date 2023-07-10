package com.jwei.publicone.ui.login.privacy

import android.content.Intent
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.AppUtils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.PrivacyStatementActivityBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.region.SelectRegionActivity
import com.jwei.publicone.utils.SpUtils
import com.jwei.publicone.utils.SpannableStringTool
import com.jwei.publicone.viewmodel.UserModel

class PrivacyStatementActivity : BaseActivity<PrivacyStatementActivityBinding, UserModel>(PrivacyStatementActivityBinding::inflate, UserModel::class.java) {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        binding.tvContent.setText("\u3000\u3000${getString(R.string.privacy_statement_content)}")
        binding.tvTitle.setOnClickListener { }
        binding.btAgree.setOnClickListener {
            SpUtils.setValue(SpUtils.APP_FIRST_START, "1")
            startActivity(Intent(this@PrivacyStatementActivity, SelectRegionActivity::class.java))
            finish()
        }
        binding.btQuit.setOnClickListener {
//            finish()
//            exitProcess(0)
            val dialog = DialogUtils.dialogShowContentAndTwoBtn(this, getString(R.string.disagree_context),
                getString(R.string.disagree_left_btn), getString(R.string.disagree_right_btn), object : DialogUtils.DialogClickListener {
                    override fun OnOK() {
                        SpUtils.setValue(SpUtils.APP_FIRST_START, "1")
                        startActivity(Intent(this@PrivacyStatementActivity, SelectRegionActivity::class.java))
                        finish()
                    }

                    override fun OnCancel() {
                        AppUtils.exitApp()
                    }

                })
        }

        binding.tvPrivacyAuthority.movementMethod = LinkMovementMethod.getInstance()
        binding.tvPrivacyAuthority.text = SpannableStringTool.get()
            .append(getString(R.string.privacy_statement_1))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
            .append("《").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.user_agreement))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
//            .setUnderline()
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@PrivacyStatementActivity, UseAgreementActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                }
            })
            .append("》").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.sign_of_coordination))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.color_171717))
            .append("《").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .append(getString(R.string.privacy_policy))
            .setFontSize(14f)
            .setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
//            .setUnderline()
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@PrivacyStatementActivity, PrivacyPolicyActivity::class.java))
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = false
                }
            })
            .append("》").setFontSize(14f).setForegroundColor(ContextCompat.getColor(this, R.color.app_index_color))//定制
            .create()
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }
}