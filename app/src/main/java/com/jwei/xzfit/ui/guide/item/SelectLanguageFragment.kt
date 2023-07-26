package com.jwei.xzfit.ui.guide.item

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
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
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.databinding.FragmentSelectLanguageBinding
import com.jwei.xzfit.databinding.ItemLanguageBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.LanguageItem
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.DeviceModel

@SuppressLint("NotifyDataSetChanged")
class SelectLanguageFragment : BaseFragment<FragmentSelectLanguageBinding, DeviceModel>(
    FragmentSelectLanguageBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private lateinit var loadDialog: Dialog
    private lateinit var languageBean: LanguageListBean
    private val datas: MutableList<LanguageItem> = ArrayList()

    companion object {
        val instance: SelectLanguageFragment by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SelectLanguageFragment()
        }
    }


    override fun initView() {
        super.initView()
        setViewsClickListener(this, binding.btnRetry)
        binding.rvLanguage.visibility = View.VISIBLE
        binding.layoutRetryHttp.visibility = View.GONE

        loadDialog = DialogUtils.showLoad(requireActivity())
        loadDialog.setCanceledOnTouchOutside(false)
        loadDialog.setCancelable(false)

        binding.rvLanguage.apply {
            adapter = initAdapter()
            layoutManager = LinearLayoutManager(requireActivity())
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
    }

    override fun initData() {
        super.initData()
        viewmodel.deviceSettingLiveData.getLanguageList().observe(this) { bean ->
            if (bean == null) return@observe
            languageBean = bean
            LogUtils.e("获取设备语言 ---- >")
            NetworkUtils.isAvailableAsync { isAvailable ->
                if (isAvailable) {
                    chanceUI(true)
                    viewmodel.queryLanguageList(languageBean.selectLanguageId)
                } else {
                    chanceUI(false)
                    ToastUtils.showToast(getString(R.string.not_network_tips))
                    DialogUtils.dismissDialog(loadDialog)
                }
            }
        }

        viewmodel.devLanguageList.observe(this) { bean ->
            DialogUtils.dismissDialog(loadDialog)
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
                        binding.rvLanguage.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        viewmodel.devLanguageCode.observe(this) { code ->
            DialogUtils.dismissDialog(loadDialog)
            if (code.isNullOrEmpty()) return@observe
            when (code) {
                HttpCommonAttributes.REQUEST_FAIL -> {
                    ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    chanceUI(false)
                }
            }
        }

        loadDialog.show()
        ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnRetry.id -> {
                loadDialog.show()
                ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        ToastUtils.showSendCmdStateTips(state)
                    }
                })
            }
        }
    }

    private fun chanceUI(status: Boolean) {
        if (status) {
            binding.rvLanguage.visibility = View.VISIBLE
            binding.layoutRetryHttp.visibility = View.GONE
        } else {
            binding.rvLanguage.visibility = View.GONE
            binding.layoutRetryHttp.visibility = View.VISIBLE
        }
    }

    private fun initAdapter(): CommonAdapter<LanguageItem, ItemLanguageBinding> {
        return object : CommonAdapter<LanguageItem, ItemLanguageBinding>(datas) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemLanguageBinding {
                return ItemLanguageBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemLanguageBinding, t: LanguageItem, position: Int) {
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
                        loadDialog.show()
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
                                            DialogUtils.dismissDialog(loadDialog)
                                            ToastUtils.showSendCmdStateTips(state)
                                        }
                                    })
                                } else {
                                    DialogUtils.dismissDialog(loadDialog)
                                }
                            }
                        })
                    }
                }
            }
        }
    }

}