package com.smartwear.xzfit.ui.device.setting.worldclock

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WorldClockBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityWorldClockBinding
import com.smartwear.xzfit.databinding.ItemWorldClockBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.adapter.ViewHolder
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.device.bean.WorldClockItem
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.healthy.drag.DragAdapter
import com.smartwear.xzfit.ui.healthy.drag.ItemDragCallback
import com.smartwear.xzfit.utils.GlideApp
import com.smartwear.xzfit.utils.TimeUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.Serializable
import java.util.*

/**
 * Created by Android on 2022/9/26.
 */
class WorldClockActivity : BaseActivity<ActivityWorldClockBinding, DeviceModel>
    (ActivityWorldClockBinding::inflate, DeviceModel::class.java), View.OnClickListener, ItemDragCallback.OnDragListener {

    private val loadDialog: Dialog by lazy { DialogUtils.showLoad(this, false) }
    private lateinit var dragCallback: ItemTouchHelper
    private var worldClockItems = mutableListOf<WorldClockItem>()
    private var devWorldClockData = mutableListOf<WorldClockBean>()
    private lateinit var itemDragCallback: ItemDragCallback

    companion object {
        //添加运动请求码
        const val ADD_REQUEST_CDEO = 1004
        const val KEY_LIST_DATA = "dev_data"
    }

    //是否编辑中
    private var isEditing = false

    //是否修改了
    private var isEdited = false


    override fun setTitleId() = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_world_clock)
        EventBus.getDefault().register(this)
        binding.tvMax.visibility = View.GONE
        binding.recyclerClock.visibility = View.GONE

        binding.recyclerClock.apply {
            layoutManager = LinearLayoutManager(this@WorldClockActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        itemDragCallback = ItemDragCallback(
            binding.recyclerClock.adapter as DragAdapter<*, *>?,
            worldClockItems as List<WorldClockItem>, true, this
        )
        itemDragCallback.setCanMove(isEditing)
        dragCallback = ItemTouchHelper(
            itemDragCallback
        )
        dragCallback.attachToRecyclerView(binding.recyclerClock)
        setViewsClickListener(this, binding.btnAdd, tvTitle!!)
    }

    private fun initAdapter(): DragAdapter<WorldClockItem, ViewBinding> {
        return object : DragAdapter<WorldClockItem, ViewBinding>(worldClockItems) {

            override fun getItemType(t: WorldClockItem) = 0

            override fun createBinding(parent: ViewGroup?, viewType: Int): ViewBinding {
                return ItemWorldClockBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ViewBinding, t: WorldClockItem, position: Int) {}

            @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
            override fun convert(holder: ViewHolder<ViewBinding>, v: ViewBinding, t: WorldClockItem, position: Int, hiddenCount: Int) {
                val itemView = v as ItemWorldClockBinding

                if (isEditing) {
                    itemView.ivItemLeft.visibility = View.VISIBLE
                    GlideApp.with(this@WorldClockActivity)
                        .load(R.mipmap.icon_sport_del)
                        .into(itemView.ivItemLeft)
                    itemView.ivItemRight.visibility = View.VISIBLE
                } else {
                    itemView.ivItemLeft.visibility = View.GONE
                    itemView.ivItemRight.visibility = View.GONE
                }

                val name = viewModel.getWorldClockNameById(cityId = t.cityId)
                itemView.tvItemText.text = name.ifEmpty { t.cityName }
                itemView.tvTodayTime.text = getString(R.string.today) + " " + TimeUtils.getHoursAndMinutes2(
                    t.offset * 15 - TimeZone.getDefault()
                        .getOffset(TimeUtils.getGreenDate().getTime()) / (60 * 1000), this@WorldClockActivity
                )
                //时间
                val calendar = Calendar.getInstance()
                calendar.time = TimeUtils.getGreenDate()
                calendar.add(Calendar.MINUTE, t.offset * 15)
                itemView.tvNowTime.text = com.blankj.utilcode.util.TimeUtils.millis2String(calendar.time.time, TimeUtils.getSafeDateFormat("HH:mm"))

                itemView.ivItemLeft.setOnClickListener {
                    notifyItemRemoved(position)
                    worldClockItems.removeAt(position)
                    devWorldClockData.removeAt(position)
                    isEdited = true
                    ThreadUtils.runOnUiThreadDelayed({
                        refUiByData()
                    }, 500)
                }

                itemView.ivItemRight.setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        if (worldClockItems.size > 0) {
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
        viewModel.deviceSettingLiveData.getWorldClock().observe(this) { list ->
            if (list == null) return@observe
            devWorldClockData.clear()
            devWorldClockData.addAll(list)
            LogUtils.d("世界时钟：" + GsonUtils.toJson(devWorldClockData))
            //{"functionId":2,"haveHide":true,"isEnable":true,"order":0,"sortable":true},
            refUiByData()
        }

        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }

        loadDialog.show()
        //获取设备世界时钟
        ControlBleTools.getInstance().getWorldClockList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState?) {
                DialogUtils.dismissDialog(loadDialog)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_REQUEST_CDEO) {
            if (resultCode == Activity.RESULT_OK) {
                val bean = data?.getSerializableExtra(AddWorldClockActivity.RESULT_DATA) as WorldClockBean?
                if (bean != null) {
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return
                    }
                    isEdited = true
                    LogUtils.d("增加：$bean")
                    devWorldClockData.add(bean)
                    refUiByData()
                    saveData()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun refUiByData() {
        worldClockItems.clear()
        for (bean in devWorldClockData) {
            val item = WorldClockItem()
            item.cityId = bean.cityId
            val name = viewModel.getWorldClockNameById(cityId = bean.cityId)
            item.cityName = if (name.isNotEmpty()) name else bean.cityName
            item.offset = bean.offset
            if (worldClockItems.size < 5) {
                worldClockItems.add(item)
                LogUtils.d("item:" + GsonUtils.toJson(item))
            } else {
                break
            }
        }
        itemDragCallback.setCanMove(isEditing)
        binding.recyclerClock.adapter?.notifyDataSetChanged()
        binding.tvMax.text = "${getString(R.string.device_set_world_clock)}（${worldClockItems.size}/5）"
        if (worldClockItems.size > 0) {
            binding.tvMax.visibility = View.VISIBLE
            binding.recyclerClock.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.GONE
            setRightIconOrTitle(rightText = if (isEditing) getString(R.string.save) else getString(R.string.edit), clickListener = this)
        } else {
            binding.tvMax.visibility = View.GONE
            binding.recyclerClock.visibility = View.GONE
            binding.tvNoData.visibility = View.VISIBLE
            setRightIconOrTitle(rightText = if (isEditing) getString(R.string.save) else "", clickListener = this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_TIME_CHANGED -> {
                refUiByData()
            }
        }
    }

    override fun onClick(v: View?) {
        if (v == null) return
        when (v.id) {
            tvTitle?.id -> {
                if (isEdited) {
                    showUnCommitDialog()
                    return
                }
                finish()
            }
            tvRIght?.id -> {
                if (isEditing) {
                    saveData()
                } else {
                    isEditing = true
                    itemDragCallback.setCanMove(isEditing)
                    setRightIconOrTitle(rightText = if (isEditing) getString(R.string.save) else getString(R.string.edit), clickListener = this)
                    binding.recyclerClock.adapter?.notifyDataSetChanged()
                }
            }
            binding.btnAdd.id -> {
                if (worldClockItems.size >= 5) {
                    ToastUtils.showToast(getString(R.string.add_world_clock_max_tips))
                    return
                }
                val intent = Intent(this, AddWorldClockActivity::class.java)
                intent.putExtra(KEY_LIST_DATA, devWorldClockData as Serializable)
                startActivityForResult(intent, ADD_REQUEST_CDEO)
            }
        }
    }

    override fun onBackPress() {
        if (isEdited) {
            showUnCommitDialog()
            return
        }
        super.onBackPress()
    }

    //region 未提交提示
    private fun showUnCommitDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.dialog_title_tips),
            getString(R.string.save_nu_commit_tips_2),
            getString(R.string.permission_quit),
            getString(R.string.save),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    saveData()
                    finish()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    private fun saveData() {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        loadDialog.show()
        viewModel.postWorldClockToDevice(worldClockItems, Global.deviceSelectLanguageId, object : ParsingStateManager.SendCmdStateListener(this.lifecycle) {
            override fun onState(state: SendCmdState?) {
                DialogUtils.dismissDialog(loadDialog)
                state?.let { ToastUtils.showSendCmdStateTips(it) }
                if (state != null && state == SendCmdState.SUCCEED) {
                    isEdited = false
                }
                isEditing = false
                itemDragCallback.setCanMove(isEditing)
                setRightIconOrTitle(rightText = if (isEditing) getString(R.string.save) else getString(R.string.edit), clickListener = this@WorldClockActivity)
                binding.recyclerClock.adapter?.notifyDataSetChanged()
            }
        })
    }


    override fun onComplete(start: Int, end: Int) {
        if (start == -1 || end == -1 || start == end) {
            return
        }
        isEditing = true
        isEdited = true
        itemDragCallback.setCanMove(isEditing)
        setRightIconOrTitle(rightText = if (isEditing) getString(R.string.save) else getString(R.string.edit), clickListener = this)
        binding.recyclerClock.adapter?.notifyDataSetChanged()

    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

}