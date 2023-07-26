package com.jwei.xzfit.ui.debug.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.Utils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.R
import com.jwei.xzfit.databinding.ItemDialDebugBinding
import com.jwei.xzfit.ui.adapter.SimpleAdapter
import com.jwei.xzfit.ui.device.bean.WatchSystemBean
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.DeviceModel
import kotlinx.android.synthetic.main.dialog_dial.*

/**
 *                 _ooOoo_
 *                o8888888o
 *                88" . "88
 *                (| -_- |)
 *                 O\ = /O
 *             ____/`---'\____
 *           .   ' \\| |// `.
 *            / \\||| : |||// \
 *          / _||||| -:- |||||- \
 *            | | \\\ - /// | |
 *          | \_| ''\---/'' |_/ |
 *           \ .-\__ `-` ___/-. /
 *        ___`. .' /--.--\ `. . __
 *     ."" '< `.___\_<|>_/___.' >'"".
 *    | | : `- \`.;`\ _ /`;.`/ - ` : | |
 *      \ \ `-. \_ __\ /__ _/ .-` / /
 *======`-.____`-.___\_____/___.-`____.-*======
 *                 `=---='
 *
 *         Buddha bless, never BUG!
 */
class DialDialog(context: Context, val lifecycleOwner: LifecycleOwner, var viewModel: DeviceModel) : BottomFullDialog(context) {
    private var myDialList = mutableListOf<WatchSystemBean>()

    override fun setLayout(): Int {
        return R.layout.dialog_dial
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        rvLocalDial.apply {
            adapter = initAdapter()
        }

    }

    fun initAdapter(): SimpleAdapter<WatchSystemBean, ItemDialDebugBinding> {
        return object : SimpleAdapter<WatchSystemBean, ItemDialDebugBinding>(ItemDialDebugBinding::inflate, myDialList) {
            override fun onBindingData(binding: ItemDialDebugBinding?, t: WatchSystemBean, position: Int) {
                binding?.tvTitle?.text = "表盘编号：${t.dialCode}"
                binding?.ivChecked?.visibility = if (t.isCurrent) View.VISIBLE else View.GONE
                binding?.layout?.setOnClickListener {
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(Utils.getApp().getString(R.string.device_no_connection))
                        return@setOnClickListener
                    }
                    ControlBleTools.getInstance().setDeviceWatchFromId(t.dialCode, object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            viewModel.getDialFromDevice()
                        }
                    })
                }
            }
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    fun initData() {
        /**
         * 监听-内置表盘-设备
         */
        viewModel.getDialFromDevice.observe(lifecycleOwner, Observer {
            if (it == null) return@Observer
            myDialList.clear()
            for (i in it.indices) {
                val bean = WatchSystemBean()
                bean.isCurrent = it[i].isCurrent
                bean.isRemove = it[i].isRemove
                bean.dialCode = it[i].id
                myDialList.add(bean)
            }
            rvLocalDial.adapter!!.notifyDataSetChanged()
        })

        viewModel.getDialFromDevice()
    }

}