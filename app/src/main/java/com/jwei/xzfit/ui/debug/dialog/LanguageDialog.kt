package com.jwei.xzfit.ui.debug.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.R
import com.jwei.xzfit.databinding.ItemLanguageDebugBinding
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.LanguageItem
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.DeviceModel
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
class LanguageDialog(context: Context,val lifecycleOwner: LifecycleOwner,val lifecycle: Lifecycle,var viewmodel:DeviceModel) : BottomFullDialog(context) {
    private val datas: MutableList<LanguageItem> = ArrayList()
    private lateinit var languageBean: LanguageListBean

    override fun setLayout(): Int {
        return R.layout.dialog_language
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rv_language.apply {
            adapter = initAdapter()
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val position: Int = parent.getChildAdapterPosition(view)
                    if (position != 0) {
                        outRect.top = ConvertUtils.dp2px(15F)
                    }
                    outRect.left = ConvertUtils.dp2px(16F)
                    outRect.right = ConvertUtils.dp2px(16F)
                }
            })
        }

        initData()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun initData() {
        viewmodel.deviceSettingLiveData.getLanguageList().observe(lifecycleOwner) { bean ->
            if (bean == null) return@observe
            languageBean = bean
            LogUtils.e("获取设备语言 ---- >")
            NetworkUtils.isAvailableAsync { isAvailable ->
                if (isAvailable) {
                    viewmodel.queryLanguageList(languageBean.selectLanguageId)
                }
            }
        }
        viewmodel.devLanguageList.observe(lifecycleOwner) { bean ->
            if (bean == null) return@observe
            LogUtils.e("获取服务器语言 ---- >")
            if (bean.dataList?.isNotEmpty() == true) {
                if (::languageBean.isInitialized) {
                    if (languageBean.languageList.isNotEmpty()) {
                        datas.clear()
                        for (languageId in languageBean.languageList) {
                            bean.dataList
                                ?.firstOrNull {
                                    it.languageCode == languageId
                                }?.apply {
                                    val languageItem = LanguageItem()
                                    languageItem.languageId = languageId
                                    languageItem.isSelect = languageId == languageBean.selectLanguageId
                                    languageItem.isDef = languageId == languageBean.defaultLanguageId
                                    languageItem.title = languageName
                                    languageItem.subTitle = chooseLanguageName
                                    datas.add(languageItem)
                                }
                        }
                        rv_language.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        viewmodel.devLanguageCode.observe(lifecycleOwner) { code ->
            if (code.isNullOrEmpty()) return@observe
            when (code) {
                HttpCommonAttributes.REQUEST_FAIL -> {
                }
            }
        }

        ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    private fun initAdapter(): CommonAdapter<LanguageItem, ItemLanguageDebugBinding> {
        return object : CommonAdapter<LanguageItem, ItemLanguageDebugBinding>(datas) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemLanguageDebugBinding {
                return ItemLanguageDebugBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemLanguageDebugBinding, t: LanguageItem, position: Int) {
                v.tvTitle.text = t.title
                v.tvSubTitle.text = t.subTitle
                v.ivChecked.visibility = if (t.isSelect) View.VISIBLE else View.GONE
                v.layout.setOnClickListener {
                    if (!AppUtils.isOpenBluetooth()) {
                        ToastUtils.showToast(R.string.dialog_context_open_bluetooth)
                        return@setOnClickListener
                    }
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return@setOnClickListener
                    }
                    if (t.languageId != -1) {
                        LogUtils.e("设置设备语言id ---- >${t.languageId}")
                        ControlBleTools.getInstance().setLanguage(t.languageId, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(state: SendCmdState) {
                                ToastUtils.showSendCmdStateTips(state)
                                if (state == SendCmdState.SUCCEED) {
                                    //本地记录
                                    Global.deviceSelectLanguageId = t.languageId
                                    //再次查询列表
                                    ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                        override fun onState(state: SendCmdState) {
                                            ToastUtils.showSendCmdStateTips(state)
                                        }
                                    })
                                } else {

                                }
                            }
                        })
                    }
                }
            }
        }
    }

}