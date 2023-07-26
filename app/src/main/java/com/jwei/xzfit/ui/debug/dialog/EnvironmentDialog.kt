package com.jwei.xzfit.ui.debug.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import com.zhapp.ble.ControlBleTools
import com.jwei.xzfit.R
import kotlinx.android.synthetic.main.dialog_environment.*
import kotlinx.android.synthetic.main.dialog_language.*

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
class EnvironmentDialog(context: Context) : BottomFullDialog(context) {

    override fun setLayout(): Int {
        return R.layout.dialog_environment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cl_test.setOnClickListener {
            ivChecked.visibility = View.VISIBLE
            ivUserChecked.visibility = View.GONE
            ControlBleTools.getInstance().setTestMode()
        }

        cl_user.setOnClickListener {
            ivUserChecked.visibility = View.VISIBLE
            ivChecked.visibility = View.GONE
            ControlBleTools.getInstance().setUserMode()
        }
    }


}