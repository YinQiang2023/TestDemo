package com.smartwear.xzfit.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.blankj.utilcode.util.ThreadUtils
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.WelcomeActivityBinding
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.login.LoginActivity
import com.smartwear.xzfit.ui.login.privacy.PrivacyStatementActivity
import com.smartwear.xzfit.ui.region.SelectRegionActivity
import com.smartwear.xzfit.utils.ErrorUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import com.smartwear.xzfit.viewmodel.UserModel

class WelcomeActivity : BaseActivity<WelcomeActivityBinding, UserModel>(WelcomeActivityBinding::inflate, UserModel::class.java) {

    //region 防止点击桌面图标会重新启动应用
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            finish()
            return
        }
        GlobalEventManager.isCanShowFirmwareUpgrade = true
        GlobalEventManager.isCanUpdateAgps = true
        if (SpUtils.getValue(SpUtils.APP_FIRST_START, SpUtils.APP_FIRST_START_DEFAULT) != SpUtils.APP_FIRST_START_DEFAULT) {
            //app启动上报
            viewModel.appStart()
        }
        //app启动
        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("1","1"))
        //每次启动上报网络错误日志，上传成功清除网络错误日志
        ErrorUtils.sendHttpError()
        //每次启动上报异常埋点
        AppTrackingManager.postTrackingDataToServer()
        //每次启动上报用户行为埋点
        AppTrackingManager.postBehaviorTracking()
    }
    //endregion

    /*
    bug :已登录账号，APP一直放至后台，账号被其他手机登录，第二天打开APP,APP卡在启动页面卡死
    override fun initData() {
        super.initData()
        viewModel.isEndWelcomePage.observe(this, Observer {
            if (it) {
                //登录是否失效
                if (Global.IS_LOGIN_CONFLICT) {
                    viewModel.userLoginOut(HttpCommonAttributes.AUTHORIZATION_EXPIRED)
                    return@Observer
                }
                //正常进入
                enterApp()
            }
        })
    }*/

    private fun enterApp() {
        if (SpUtils.getValue(SpUtils.APP_FIRST_START, SpUtils.APP_FIRST_START_DEFAULT) == SpUtils.APP_FIRST_START_DEFAULT) {
            startActivity(Intent(this, PrivacyStatementActivity::class.java))
        } else {
            if (SpUtils.getValue(SpUtils.USER_IS_LOGIN, SpUtils.USER_IS_LOGIN_DEFAULT) == SpUtils.USER_IS_LOGIN_DEFAULT) {
                //未选择地区和服务器
                if (SpUtils.getValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_DEFAULT) == SpUtils.SERVICE_ADDRESS_DEFAULT) {
                    startActivity(Intent(this, SelectRegionActivity::class.java))
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
            }

        }
        //finish()
    }

    override fun onResume() {
        super.onResume()
        //viewModel.countdown(2)  bug :已登录账号，APP一直放至后台，账号被其他手机登录，第二天打开APP,APP卡在启动页面卡死
        downTimerTask = DownTimerTask()
        ThreadUtils.executeByIo(downTimerTask)
    }

    //region 欢迎界面倒计时
    private var downTimerTask: DownTimerTask? = null

    inner class DownTimerTask : ThreadUtils.SimpleTask<Int>() {
        override fun doInBackground(): Int {
            var i = 2
            while (i != 0) {
                i--
                Thread.sleep(1000)
            }
            return i
        }

        override fun onSuccess(result: Int?) {
            //登录是否失效
            if (Global.IS_LOGIN_CONFLICT) {
                viewModel.userLoginOut(HttpCommonAttributes.AUTHORIZATION_EXPIRED)
                return
            }
            //正常进入
            enterApp()
        }
    }
    //endregion


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
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

    override fun onDestroy() {
        super.onDestroy()
        if (downTimerTask != null) {
            ThreadUtils.cancel(downTimerTask)
        }
    }

}
