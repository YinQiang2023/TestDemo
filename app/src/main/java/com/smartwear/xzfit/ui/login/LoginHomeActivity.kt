package com.smartwear.xzfit.ui.login

import android.content.Intent
import android.view.View
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityLoginHomeBinding
import com.smartwear.xzfit.ui.login.register.RegisterActivity
import com.smartwear.xzfit.viewmodel.UserModel

class LoginHomeActivity : BaseActivity<ActivityLoginHomeBinding, UserModel>
    (ActivityLoginHomeBinding::inflate, UserModel::class.java), View.OnClickListener {

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        binding.btnLogin.setOnClickListener(this)
        binding.btnRegister.setOnClickListener(this)
        setViewsClickListener(this, binding.btnLogin, binding.btnRegister)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnLogin.id -> {
                startActivity(Intent(this@LoginHomeActivity, LoginActivity::class.java))
            }
            binding.btnRegister.id -> {
                startActivity(Intent(this@LoginHomeActivity, RegisterActivity::class.java))
            }
        }
    }
}