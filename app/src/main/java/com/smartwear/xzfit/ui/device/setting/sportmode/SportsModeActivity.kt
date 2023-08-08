package com.smartwear.xzfit.ui.device.setting.sportmode

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ClickUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ThreadUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WidgetBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivitySportsModeBinding
import com.smartwear.xzfit.databinding.ItemEditCardBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.device.bean.SportModeItem
import com.smartwear.xzfit.ui.healthy.drag.DragAdapter
import com.smartwear.xzfit.ui.healthy.drag.ItemDragCallback
import com.smartwear.xzfit.utils.SportTypeUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.smartwear.xzfit.ui.adapter.ViewHolder
import com.smartwear.xzfit.utils.GlideApp
import com.smartwear.xzfit.utils.ToastUtils
import java.io.Serializable

/**
 * Created by Android on 2022/8/23.
 */
class SportsModeActivity : BaseActivity<ActivitySportsModeBinding, DeviceModel>
    (ActivitySportsModeBinding::inflate, DeviceModel::class.java), View.OnClickListener, ItemDragCallback.OnDragListener {

    private val loadDialog: Dialog by lazy { DialogUtils.showLoad(this, false) }
    private lateinit var dragCallback: ItemTouchHelper
    private var devSportItems = mutableListOf<SportModeItem>()
    private var devSportData = mutableListOf<WidgetBean>()

    companion object {
        //添加运动请求码
        const val ADD_REQUEST_CDEO = 1003
        const val KEY_LIST_DATA = "sport_data"
    }

    override fun setTitleId() = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.sports_mode)
        setRightIconOrTitle(rightText = getString(R.string.device_fragment_add), clickListener = this)

        binding.recyclerSport.apply {
            layoutManager = LinearLayoutManager(this@SportsModeActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        dragCallback = ItemTouchHelper(
            ItemDragCallback(
                binding.recyclerSport.adapter as DragAdapter<*, *>?,
                devSportItems as List<SportModeItem>, true, this
            )
        )
        dragCallback.attachToRecyclerView(binding.recyclerSport)
        setViewsClickListener(this, binding.btnSave)
    }

    private fun initAdapter(): DragAdapter<SportModeItem, ViewBinding> {
        return object : DragAdapter<SportModeItem, ViewBinding>(devSportItems) {

            override fun getItemType(t: SportModeItem) = 0

            override fun createBinding(parent: ViewGroup?, viewType: Int): ViewBinding {
                return ItemEditCardBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ViewBinding, t: SportModeItem, position: Int) {}

            @SuppressLint("ClickableViewAccessibility")
            override fun convert(holder: ViewHolder<ViewBinding>, v: ViewBinding, t: SportModeItem, position: Int, hiddenCount: Int) {
                val itemView = v as ItemEditCardBinding

                GlideApp.with(this@SportsModeActivity)
                    .load(R.mipmap.icon_sport_del)
                    .into(itemView.ivItemLeft)

                itemView.tvItemText.text = SportTypeUtils.getSportTypeName(2, t.protocolId.toString())

                ClickUtils.applySingleDebouncing(itemView.ivItemLeft, object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        if (devSportItems.size == 1) {
                            ToastUtils.showToast(getString(R.string.min_sport_tips))
                            return
                        }
                        devSportItems.removeAt(position)
                        devSportData.firstOrNull { it.functionId == t.protocolId }?.isEnable = false
                        notifyDataSetChanged()
                        binding.tvMax.text = "${getString(R.string.edit_card_display_area)}（${devSportItems.size}/10）"
                        tvRIght?.setTextColor(ContextCompat.getColor(this@SportsModeActivity, if (devSportItems.size == 10) R.color.color_FFFFFF_50 else R.color.color_FFFFFF))
                    }
                })

                itemView.ivItemRight.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        if (devSportItems.size > 1) {
                            dragCallback.startDrag(holder)
                        }
                    }
                    false
                }
            }
        }
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getSportList().observe(this) {
            if (it == null) return@observe
            LogUtils.d("运动列表：" + GsonUtils.toJson(it))
            //{"functionId":2,"haveHide":true,"isEnable":true,"order":0,"sortable":true},
            //{"functionId":25,"haveHide":true,"isEnable":true,"order":10,"sortable":true} ...
            devSportData = it
            refUiByData()
        }

        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }

        loadDialog.show()
        ControlBleTools.getInstance().getSportWidgetSortList(object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                DialogUtils.dismissDialog(loadDialog)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun refUiByData() {
        if (devSportData.size > 0) {
            devSportItems.clear()
            for (devSport in devSportData) {
                if (devSport.isEnable) { //已经启用的
                    if (devSportItems.size < 10) {
                        val item = SportModeItem()
                        item.protocolId = devSport.functionId
                        devSportItems.add(item)
                    } else {
                        //最多只允许10个,其它置位false
                        devSport.isEnable = false
                    }
                }
            }
        }
        binding.recyclerSport.adapter?.notifyDataSetChanged()
        binding.tvMax.text = "${getString(R.string.edit_card_display_area)}（${devSportItems.size}/10）"
        tvRIght?.setTextColor(ContextCompat.getColor(this, if (devSportItems.size == 10) R.color.color_FFFFFF_50 else R.color.color_FFFFFF))
    }


    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            tvRIght?.id -> {
                if (devSportItems.size == 10) {
                    ToastUtils.showToast(R.string.max_sport_tips)
                    return
                }
                // 进入添加
                val intent = Intent(this, AddSportModeActivity::class.java)
                intent.putExtra(KEY_LIST_DATA, devSportData as Serializable)
                startActivityForResult(intent, ADD_REQUEST_CDEO)
            }
            binding.btnSave.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(R.string.device_no_connection)
                    return
                }

                val newData = mutableListOf<WidgetBean>()
                for (i in 0 until devSportItems.size) {
                    val item = WidgetBean()
                    //{"functionId":2,"haveHide":true,"isEnable":true,"order":0,"sortable":true},
                    item.functionId = devSportItems.get(i).protocolId
                    item.haveHide = true
                    item.isEnable = true
                    item.sortable = true
                    item.order = newData.size // 0....size
                    newData.add(item)
                }
                for (dev in devSportData) {
                    var isCanAdd = true
                    for (n in newData) {
                        if (n.functionId == dev.functionId) {
                            isCanAdd = false
                            break
                        }
                    }
                    if (isCanAdd) {
                        dev.order = newData.size
                        dev.isEnable = false
                        newData.add(dev)
                    }
                }

                loadDialog.show()
                LogUtils.d("设置运动排序：${GsonUtils.toJson(newData)}")
                ControlBleTools.getInstance().setSportWidgetSortList(newData, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
                    override fun onState(state: SendCmdState) {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                        if (state == SendCmdState.SUCCEED) {
                            ToastUtils.showToast(R.string.save_success)
                            finish()
                        }
                    }
                })
            }
        }
    }

    override fun onComplete(start: Int, end: Int) {
        if (start == -1 || end == -1 || start == end) {
            return
        }
        ThreadUtils.runOnUiThread { binding.recyclerSport.adapter?.notifyDataSetChanged() }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_REQUEST_CDEO) {
            if (resultCode == Activity.RESULT_OK) {
                val resultList = data?.getSerializableExtra(AddSportModeActivity.RESULT_DATA) as MutableList<WidgetBean>?
                if (resultList != null) {
                    LogUtils.d("新增后返回运动排序：${GsonUtils.toJson(resultList)}")
                    devSportData = resultList
                    refUiByData()
                }
            }
        }
    }
}