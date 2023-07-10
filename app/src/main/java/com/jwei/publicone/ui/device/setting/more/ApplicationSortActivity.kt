package com.jwei.publicone.ui.device.setting.more

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.Utils
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityApplicationSrortBinding
import com.jwei.publicone.databinding.ItemEditCardBinding
import com.jwei.publicone.databinding.ItemEditCardTitleBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.adapter.ViewHolder
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.ui.device.bean.DevAppItem
import com.jwei.publicone.ui.healthy.drag.DragAdapter
import com.jwei.publicone.ui.healthy.drag.ItemDragCallback
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WidgetBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.https.response.ApplicationListResponse
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.GlideApp
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 * Created by Android on 2021/10/28.
 */
class ApplicationSortActivity : BaseActivity<ActivityApplicationSrortBinding, DeviceModel>(
    ActivityApplicationSrortBinding::inflate, DeviceModel::class.java
), View.OnClickListener, ItemDragCallback.OnDragListener {

    private lateinit var loadDialog: Dialog
    private lateinit var dragCallback: ItemTouchHelper
    private lateinit var widgetList: MutableList<WidgetBean>
    private var devAppItems = mutableListOf<DevAppItem>()
    private var type = 0 //0 应用列表排序  ， 1 直达卡片排序

    override fun setTitleId() = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        type = intent.getIntExtra("type", 0)
        setTvTitle(if (type == 0) R.string.dev_more_set_app_sort else R.string.dev_more_set_card_sort)

        AppUtils.registerEventBus(this)
        loadDialog = DialogUtils.showLoad(this)

        binding.rvEditCard.apply {
            layoutManager = LinearLayoutManager(this@ApplicationSortActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        dragCallback = ItemTouchHelper(
            ItemDragCallback(
                binding.rvEditCard.adapter as DragAdapter<*, *>?,
                devAppItems as List<DevAppItem>, true, this
            )
        )
        dragCallback.attachToRecyclerView(binding.rvEditCard)

        setViewsClickListener(this, binding.btnSave, binding.noNetwork.btnRetry)

        checkNetWork()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun oneventMsg(event: EventMessage) {
        if (event.action == EventAction.ACTION_NETWORK_DISCONNECTED) {
            checkNetWork()
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
                        binding.dataLayout.visibility = View.VISIBLE
                    } else {
                        binding.noNetwork.layoutNoNetWork.visibility = View.VISIBLE
                        binding.dataLayout.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun initData() {
        super.initData()

        loadDialog.show()
        if (type == 0) {
            viewModel.deviceSettingLiveData.getWidgetList().observe(this) { list ->
                if (list == null) return@observe
                LogUtils.d("设备应用列表---------->")
                LogUtils.json(list)
                widgetList = list
                if (!NetworkUtils.isConnected()) {
                    ToastUtils.showToast(getString(R.string.not_network_tips))
                    return@observe
                }
                LogUtils.d("设备当前选中语言id---------->${Global.deviceSelectLanguageId}")
                viewModel.getApplicationList(Global.deviceSelectLanguageId)
            }
        } else {
            viewModel.deviceSettingLiveData.getCardList().observe(this) { list ->
                if (list == null) return@observe
                LogUtils.d("设备卡片列表---------->")
                LogUtils.json(list)
                widgetList = list
                if (!NetworkUtils.isConnected()) {
                    ToastUtils.showToast(getString(R.string.not_network_tips))
                    return@observe
                }
                LogUtils.d("设备当前选中语言id---------->${Global.deviceSelectLanguageId}")
                viewModel.getCardList(Global.deviceSelectLanguageId)
            }
        }


        if (type == 0) {
            ControlBleTools.getInstance().getApplicationList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    DialogUtils.dismissDialog(loadDialog)
                    ToastUtils.showSendCmdStateTips(state)
                }
            })
        } else {
            ControlBleTools.getInstance().getWidgetList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    DialogUtils.dismissDialog(loadDialog)
                    ToastUtils.showSendCmdStateTips(state)
                }
            })
        }

        viewModel.applicationCode.observe(this, {
            if (it.isNullOrEmpty()) return@observe
            if (it == HttpCommonAttributes.REQUEST_FAIL) {
                ToastUtils.showToast(getString(R.string.operation_failed_tips))
            }
        })

        viewModel.cardListCode.observe(this, {
            if (it.isNullOrEmpty()) return@observe
            if (it == HttpCommonAttributes.REQUEST_FAIL) {
                ToastUtils.showToast(getString(R.string.operation_failed_tips))
            }
        })

        viewModel.applicationList.observe(this, MyListObserver())
        viewModel.cardList.observe(this, MyListObserver())
    }

    inner class MyListObserver : Observer<ApplicationListResponse?> {
        override fun onChanged(data: ApplicationListResponse?) {
            if (data == null) return
            if (!data.dataList.isNullOrEmpty()) {
                checkNetWork()
                //{ "dataList": [{ "icoUrl": "http://file.wearheart.cn/upload/icon/20211020/4ee8056eb5e148cda21a1e3a2bb6f53b.png",
                //"languageName": "Workouts", "protocolId": 0 },...]}
                if (::widgetList.isInitialized) {
                    devAppItems.clear()
                    //启用的
                    for (w in widgetList) {
                        data.dataList!!.firstOrNull { it.protocolId == w.functionId && w.isEnable }?.let {
                            val item = DevAppItem()
                            item.protocolId = it.protocolId
                            item.iconUrl = it.icoUrl
                            item.isHide = false
                            item.haveHide = w.haveHide
                            item.centerTextId = it.languageName
                            devAppItems.add(item)
                        }
                    }
                    //隐藏区item
                    val bean = DevAppItem()
                    bean.protocolId = -1
                    bean.centerTextId = BaseApplication.mContext.resources.getString(R.string.edit_card_hide_area)
                    bean.isTitle = true
                    devAppItems.add(bean)
                    //隐藏的
                    for (w in widgetList) {
                        data.dataList!!.firstOrNull { it.protocolId == w.functionId && !w.isEnable }?.let {
                            val item = DevAppItem()
                            item.protocolId = it.protocolId
                            item.iconUrl = it.icoUrl
                            item.isHide = true
                            item.haveHide = w.haveHide
                            item.centerTextId = it.languageName
                            devAppItems.add(item)
                        }
                    }
                    (binding.rvEditCard.adapter as DragAdapter<*, *>?)?.calcHideCount()
                    binding.rvEditCard.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun initAdapter(): DragAdapter<DevAppItem/*DragBean*/, ViewBinding> {
        return object : DragAdapter<DevAppItem/*DragBean*/, ViewBinding>(devAppItems/*Global.editCardList*/) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ViewBinding {
                return if (viewType == 0) {
                    ItemEditCardTitleBinding.inflate(layoutInflater, parent, false)
                } else {
                    ItemEditCardBinding.inflate(layoutInflater, parent, false)
                }
            }

            override fun getItemType(t: DevAppItem/*DragBean*/): Int {
                return if (t.isTitle) 0 else 1
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun convert(
                holder: ViewHolder<ViewBinding>,
                v: ViewBinding,
                t: DevAppItem/*DragBean*/,
                position: Int,
                hiddenCount: Int
            ) {
                if (t.isTitle) {
                    val itemView = v as ItemEditCardTitleBinding
                    itemView.ivItem.text = t.centerTextId
                    itemView.tvHiddenTip.text = getString(R.string.edit_card_tips1_dev_app)
                    itemView.tvHide.text = getString(R.string.edit_card_tips3)
                    if (hiddenCount > 0) {
                        itemView.lyHiddenItem.visibility = View.GONE
                    } else {
                        itemView.lyHiddenItem.visibility = View.VISIBLE
                    }
                } else {
                    val itemView = v as ItemEditCardBinding
                    if (t.leftImg != 0) {
                        itemView.ivItemLeft.setImageResource(t.leftImg)
                    } else if (t.iconUrl.isNotEmpty()) {
                        // 加载网络图
                        GlideApp.with(this@ApplicationSortActivity)
                            .load(t.iconUrl)
                            .error(R.mipmap.device_fragment_set_remind_icon)
                            .into(itemView.ivItemLeft)
                    }

                    itemView.tvItemText.text = t.centerTextId
                    if (t.isHide) {
                        itemView.ivItemLeft.alpha = 0.35f
                        itemView.tvItemText.setTextColor(ContextCompat.getColor(this@ApplicationSortActivity, R.color.color_FFFFFF_35))
                    } else {
                        itemView.ivItemLeft.alpha = 1f
                        itemView.tvItemText.setTextColor(ContextCompat.getColor(this@ApplicationSortActivity, R.color.color_FFFFFF))
                    }

                    itemView.ivItemRight.setOnTouchListener { v, event ->

                        if (t.protocolId != 0 && event.action == MotionEvent.ACTION_DOWN) {
                            dragCallback.startDrag(holder)
                        }
                        false
                    }
                }
            }

            override fun convert(v: ViewBinding, t: DevAppItem/*DragBean*/, position: Int) {
            }
        }
    }

    /**
     * 排序移动完成
     * */
    override fun onComplete(start: Int, end: Int) {
        LogUtils.d("start $start , end $end")
        if (start == -1 || end == -1 || start == end) {
            return
        }
        //移动的item
        val item = devAppItems.get(end)
        // 不可隐藏的item 被隐藏了需要复位
        if (!item.haveHide) {
            if (item.isHide) {
                item.isHide = false
                if (start < end) { // 未隐藏拖动隐藏只有 start < end
                    for (i in start until end) { //i = start -> end-1
                        if (i >= 0 && i < devAppItems.size) {
                            Collections.swap(devAppItems, i, end)
                        }
                    }
                }
                ToastUtils.showToast(StringBuilder().append(getString(R.string.not_supported_hide_tips)).toString())
            }
        }
        //LogUtils.d(GsonUtils.toJson(devAppItems))
        (binding.rvEditCard.adapter as DragAdapter<*, *>?)?.calcHideCount()
        binding.rvEditCard.adapter?.notifyDataSetChanged()

    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnSave.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }

                /*if(mIsNoNetWork){ //无网络重试
                    loadDialog.show()
                    ControlBleTools.getInstance().getWidgetList(object : ParsingStateManager.SendCmdStateListener(lifecycle){
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    })
                    return
                }*/

                LogUtils.d(" ")
                LogUtils.json(devAppItems)
                if (devAppItems.isNullOrEmpty()) {
                    ToastUtils.showToast(R.string.no_data)
                    return
                }
                val list = mutableListOf<WidgetBean>()
                var order = 1
                for (item in devAppItems) {
                    if (item.protocolId != -1) {
                        val widget = WidgetBean()
                        widget.functionId = item.protocolId
                        widget.isEnable = !item.isHide
                        widget.order = order
                        order++
                        list.add(widget)
                    }
                }
                if (list.isNullOrEmpty()) {
                    ToastUtils.showToast(R.string.no_data)
                    return
                }
                loadDialog.show()
                LogUtils.d("保存列表排序")
                LogUtils.d(GsonUtils.toJson(list))
                if (type == 0) {
                    ControlBleTools.getInstance().setApplicationList(list, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                            if (state == SendCmdState.SUCCEED) {
                                ToastUtils.showToast(R.string.save_success)
//                            ControlBleTools.getInstance().getWidgetList(null)
                                finish()
                            }
                        }
                    })
                } else {
                    ControlBleTools.getInstance().setWidgetList(list, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                            if (state == SendCmdState.SUCCEED) {
                                ToastUtils.showToast(R.string.save_success)
//                            ControlBleTools.getInstance().getWidgetList(null)
                                finish()
                            }
                        }
                    })
                }

            }
            binding.noNetwork.btnRetry.id -> {
                loadDialog.show()
                if (type == 0) {
                    ControlBleTools.getInstance().getApplicationList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    })
                } else {
                    ControlBleTools.getInstance().getWidgetList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            DialogUtils.dismissDialog(loadDialog)
                            ToastUtils.showSendCmdStateTips(state)
                        }
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }

}