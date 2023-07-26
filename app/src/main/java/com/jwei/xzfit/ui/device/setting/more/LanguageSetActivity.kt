package com.jwei.xzfit.ui.device.setting.more

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.*
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityLanguageSetBinding
import com.jwei.xzfit.databinding.ItemLanguageBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.device.bean.LanguageItem
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.xzfit.https.HttpCommonAttributes
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean
import com.jwei.xzfit.ui.device.bean.WorldClockItem
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.SpUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Android on 2021/10/28.
 */
class LanguageSetActivity : BaseActivity<ActivityLanguageSetBinding, DeviceModel>(ActivityLanguageSetBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    companion object {
        const val IS_BIND_KEY = "isBind"
        const val BIND_DEVICE_MAC = "mac"
    }

    private lateinit var loadDialog: Dialog
    private lateinit var devLanguageBean: LanguageListBean
    private var datas = mutableListOf<LanguageItem>()
    private var isBind = false
    private var bindDeviceMac: String? = null

    override fun setTitleId() = binding.title.layoutTitle.id
    override fun initView() {
        super.initView()

        isBind = intent.getBooleanExtra(IS_BIND_KEY, false)
        bindDeviceMac = intent.getStringExtra(BIND_DEVICE_MAC)
        setTvTitle(R.string.dev_more_set_language_sel)
        if (isBind) {
            binding.btConfirm.visibility = View.VISIBLE
            tvTitle?.setCompoundDrawables(null, null, null, null)
            tvTitle?.setOnClickListener {  }
            //检测bt配对
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_HEADSET_BOND, bindDeviceMac))
        }

        loadDialog = DialogUtils.dialogShowLoad(this)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@LanguageSetActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
//            addItemDecoration(object : RecyclerView.ItemDecoration() {
//                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//                    val position: Int = parent.getChildAdapterPosition(view)
//                    if (position != 0) {
//                        outRect.top = ConvertUtils.dp2px(20F)
//                    }
//                    outRect.left = ConvertUtils.dp2px(25F)
//                    outRect.right = ConvertUtils.dp2px(25F)
//                }
//            })
        }

        setViewsClickListener(this, binding.noNetwork.btnRetry, binding.btConfirm)

        checkNetWork()
    }

    override fun initData() {
        super.initData()
        AppUtils.registerEventBus(this)
        viewModel.deviceSettingLiveData.getLanguageList().observe(this) { bean ->
            //DialogUtils.dismissDialog(loadDialog) viewModel.devLanguageCode 里面关闭
            if (bean == null) return@observe
            //{"defaultLanguageId":104,"languageList":[104,113,112],"selectLanguageId":104}
            devLanguageBean = bean
            LogUtils.e("获取语言 ---- >")
            LogUtils.json(devLanguageBean)
            NetworkUtils.isAvailableAsync { isAvailable ->
                if (isAvailable) {
                    viewModel.queryLanguageList(devLanguageBean.selectLanguageId)
                } else {
                    ToastUtils.showToast(getString(R.string.not_network_tips))
                    DialogUtils.dismissDialog(loadDialog)
                }
            }
        }

        viewModel.devLanguageList.observe(this) { serLanguagBean ->
            if (serLanguagBean == null) return@observe
            LogUtils.e("获取服务器语言 ---- >")
            LogUtils.json(serLanguagBean)
            //{"dataList":[{"chooseLanguageName":"阿尔巴尼亚语","languageCode":1,"languageName":"Shqiptare"}...]}
            if (!serLanguagBean.dataList.isNullOrEmpty()) {
                checkNetWork()
                if (::devLanguageBean.isInitialized) {
                    if (!devLanguageBean.languageList.isNullOrEmpty()) {
                        datas.clear()
                        for (languageId in devLanguageBean.languageList) {
                            serLanguagBean.dataList!!.firstOrNull { it.languageCode == languageId }?.apply {
                                val languageItem = LanguageItem()
                                languageItem.languageId = languageId
                                languageItem.isSelect = languageId == devLanguageBean.selectLanguageId
                                languageItem.isDef = languageId == devLanguageBean.defaultLanguageId
                                languageItem.title = languageName
                                languageItem.subTitle = chooseLanguageName
                                datas.add(languageItem)
                            }
                        }
                        binding.recycler.adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
        viewModel.devLanguageCode.observe(this) { code ->
            DialogUtils.dismissDialog(loadDialog)
            if (code.isNullOrEmpty()) return@observe
            when (code) {
                HttpCommonAttributes.REQUEST_FAIL -> {
                    ToastUtils.showToast(getString(R.string.operation_failed_tips))
                }
            }
        }

        //region 修改语言后刷新世界时钟
        viewModel.deviceSettingLiveData.getWorldClock().observe(this) { list ->
            if (list == null) return@observe
            val itemList = mutableListOf<WorldClockItem>()
            for (bean in list) {
                val item = WorldClockItem()
                item.cityId = bean.cityId
                val name = viewModel.getWorldClockNameById(cityId = bean.cityId)
                item.cityName = if (name.isNotEmpty()) name else bean.cityName
                item.offset = bean.offset
                if (itemList.size < 5) {
                    itemList.add(item)
                } else {
                    break
                }
            }
            viewModel.postWorldClockToDevice(itemList, Global.deviceSelectLanguageId, null)
        }

        //endregion

        loadDialog.show()
        ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    private fun initAdapter(): CommonAdapter<LanguageItem, ItemLanguageBinding> {
        return object : CommonAdapter<LanguageItem, ItemLanguageBinding>(datas) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemLanguageBinding {
                return ItemLanguageBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun convert(v: ItemLanguageBinding, t: LanguageItem, position: Int) {
                v.tvTitle.text = t.title
                v.tvSubTitle.text = t.subTitle
                v.ivChecked.visibility = if (t.isSelect) View.VISIBLE else View.GONE
                if (position == (mData.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }
                v.layout.setOnClickListener {
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
                                    // 刷新世界时钟
                                    //产品功能列表 判断是否支持世界时钟
                                    val deviceSettingBean = JSON.parseObject(SpUtils.getValue(SpUtils.DEVICE_SETTING, ""), DeviceSettingBean::class.java)
                                    if (deviceSettingBean != null && deviceSettingBean.settingsRelated != null && deviceSettingBean.settingsRelated.world_clock) {
                                        ControlBleTools.getInstance().getWorldClockList(null)
                                    }
                                    //再次查询语言列表
                                    ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                                        override fun onState(state: SendCmdState) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun oneventMsg(event: EventMessage) {
        if (event.action == EventAction.ACTION_NETWORK_DISCONNECTED) {
            //checkNetWork()
        }
    }

    /**
     * 检测网络
     */
    private fun checkNetWork() {
        NetworkUtils.isAvailableAsync(object : Utils.Consumer<Boolean> {
            override fun accept(isAvailable: Boolean?) {
                isAvailable?.let {
                    if (isAvailable) {
                        binding.noNetwork.layoutNoNetWork.visibility = View.GONE
                        binding.recycler.visibility = View.VISIBLE
                    } else {
                        binding.noNetwork.layoutNoNetWork.visibility = View.VISIBLE
                        binding.recycler.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.noNetwork.btnRetry.id -> {
                loadDialog.show()
                ControlBleTools.getInstance().getLanguageList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                    override fun onState(state: SendCmdState) {
                        ToastUtils.showSendCmdStateTips(state)
                    }
                })
            }
            binding.btConfirm.id -> {
                NetworkUtils.isAvailableAsync { isAvailable ->
                    if (isAvailable) {
                        setResult(RESULT_OK, intent)
                        finish()
                    } else {
                        ToastUtils.showToast(getString(R.string.not_network_tips))
                    }
                }

            }
        }
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isBind) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }

}