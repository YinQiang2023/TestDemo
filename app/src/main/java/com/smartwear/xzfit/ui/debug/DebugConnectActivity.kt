package com.smartwear.xzfit.ui.debug

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityDebugConnectBinding
import com.smartwear.xzfit.ui.debug.manager.DebugDevConnectMonitor
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.PermissionUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Android on 2021/12/24.
 */
@SuppressLint("SetTextI18n")
class DebugConnectActivity : BaseActivity<ActivityDebugConnectBinding, DeviceModel>(
    ActivityDebugConnectBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("设备连接测试")
        updateUi()
        setViewsClickListener(this, binding.btnReset)

    }

    override fun onClick(v: View) {
        if (v.id == binding.btnReset.id) {
            DebugDevConnectMonitor.resetMonitor()
            updateUi()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMsg(event: EventMessage) {
        when (event.action) {
            EventAction.ACTION_DEVICE_BLE_STATUS_CHANGE -> {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(updateUiRunnable, 1000)
            }
        }
    }

    //region 更新界面
    private val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    private val updateUiRunnable = Runnable {
        updateUi()
    }

    private fun updateUi() {
        DebugDevConnectMonitor.getCurFileName().let {
            if (it.isNotEmpty()) {
                binding.tvFileName.text = "文件名：$it"
            }
        }
        binding.tvNum.text = "连接次数： ${DebugDevConnectMonitor.getConnectCount()}"
    }
    //endregion


    override fun initData() {
        super.initData()
        AppUtils.registerEventBus(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }

}