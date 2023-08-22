package com.smartwear.publicwatch.ui.debug.dialog

import android.content.Context
import android.os.Bundle
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.utils.DeviceManager
import kotlinx.android.synthetic.main.dialog_device_info.*

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
class DeviceInfoDialog(context: Context) : BottomFullDialog(context) {
    override fun setLayout(): Int {
        return R.layout.dialog_device_info
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        for (i in DeviceManager.dataList.indices) {
            if (DeviceManager.dataList[i].deviceStatus == 1) {
                tv_name.text = DeviceManager.dataList[i].deviceName
                tv_Sn.text = DeviceManager.dataList[i].deviceSn
                tv_Mac.text = DeviceManager.dataList[i].deviceMac
                tv_Version.text = DeviceManager.dataList[i].deviceVersion
            }
        }
    }
}