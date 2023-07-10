package com.jwei.publicone.ui.device.setting.remind

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.*
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityClockEventBinding
import com.jwei.publicone.databinding.ItemReminderClockBinding
import com.jwei.publicone.databinding.ItemReminderEventBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.data.Global
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ClockInfoBean
import com.zhapp.ble.bean.EventInfoBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.utils.manager.AppTrackingManager

/**
 * Created by Android on 2021/10/25.
 * 闹钟事件提醒
 */
class ClockEventReminderActivity : BaseActivity<ActivityClockEventBinding, DeviceModel>(
    ActivityClockEventBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    //提醒类型
    private var type = 0

    //等待loading
    private lateinit var loadDialog: Dialog

    //闹钟数据
    private var clocks: MutableList<ClockInfoBean> = mutableListOf()
    private var maxClocks = 0

    private var events: MutableList<EventInfoBean> = mutableListOf()
    private var maxEvents = 0

    //region initView

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()

        type = intent.getIntExtra("type", Global.REMINDER_TYPE_SEDENTARY)
        loadDialog = DialogUtils.showLoad(this)

        setTvTitle(
            when (type) {
                Global.REMINDER_TYPE_EVENT -> R.string.device_remind_event
                Global.REMINDER_TYPE_CLOCK -> R.string.device_remind_alarm_clock
                else -> R.string.device_remind_alarm_clock
            }
        )


        binding.tvHint.text = when (type) {
            Global.REMINDER_TYPE_EVENT -> getString(R.string.open_remind_event_tip)
            Global.REMINDER_TYPE_CLOCK -> getString(R.string.open_remind_alarm_tip)
            else -> getString(R.string.open_remind_alarm_tip)
        }

        binding.tvDelTip.text =
            when (type) {
                Global.REMINDER_TYPE_EVENT -> {
                    getString(R.string.delete_list_event)
                }
                Global.REMINDER_TYPE_CLOCK -> {
                    getString(R.string.delete_list_tips_colck)
                }
                else -> {
                    getString(R.string.delete_list_tips_colck)
                }
            }


        setRightIconOrTitle(R.mipmap.icon_add, clickListener = this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ClockEventReminderActivity)
            setHasFixedSize(true)
            adapter =
                if (type == Global.REMINDER_TYPE_CLOCK) initClockAdapter() else initEventAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = ConvertUtils.dp2px(10F)
                }
            })
        }
    }
    //endregion

    //region 闹钟adapter
    private fun initClockAdapter(): CommonAdapter<ClockInfoBean, ItemReminderClockBinding> {
        return object : CommonAdapter<ClockInfoBean, ItemReminderClockBinding>(clocks) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemReminderClockBinding {
                return ItemReminderClockBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemReminderClockBinding, t: ClockInfoBean, position: Int) {
                v.tvName.text = t.data.clockName
                v.mSwitch.isChecked = t.data.isEnable

                v.tvTime.text =
                    StringBuilder().append(if (t.data.time.hour < 10) "0${t.data.time.hour}" else t.data.time.hour)
                        .append(":")
                        .append(if (t.data.time.minuter < 10) "0${t.data.time.minuter}" else t.data.time.minuter)

                when (t.data.weekDays) {
                    127 -> {  // 01111111 = 127
                        v.tvWeekDay.text = getString(R.string.everyday)
                    }
                    0 -> {   // 00000000
                        v.tvWeekDay.text = getString(R.string.once_only)
                    }
                    else -> {
                        val weakDayBuilder = StringBuilder()
                        if (t.data.isMonday) weakDayBuilder.append(getString(R.string.week_easy_1))
                        if (t.data.isTuesday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_2))
                        }
                        if (t.data.isWednesday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_3))
                        }
                        if (t.data.isThursday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_4))
                        }
                        if (t.data.isFriday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_5))
                        }
                        if (t.data.isSaturday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_6))
                        }
                        if (t.data.isSunday) {
                            if (weakDayBuilder.isNotEmpty()) weakDayBuilder.append("、")
                            weakDayBuilder.append(getString(R.string.week_easy_7))
                        }
                        v.tvWeekDay.text = weakDayBuilder
                    }
                }

                v.mSwitch.setOnClickListener {
                    v.mSwitch.isChecked = !v.mSwitch.isChecked

                    val tempList: MutableList<ClockInfoBean> = mutableListOf()
                    tempList.addAll(clocks)
                    if (position < tempList.size) {
                        tempList.get(position).data.isEnable = !v.mSwitch.isChecked
                        sendNewClocks(tempList)
                    }
                }

                v.root.setOnClickListener {
                    startActivityForResult(Intent(this@ClockEventReminderActivity, AddAlarmClockActivity::class.java).putExtra("data", t), type)
                }

                v.root.setOnLongClickListener {
                    showDelDialog(position, t.data.clockName)
                    return@setOnLongClickListener true
                }

                v.viewLine01.visibility = if (position == (clocks.size - 1)) View.VISIBLE else View.GONE
            }
        }
    }
    //endregion

    //region 事件adapter
    private fun initEventAdapter(): CommonAdapter<EventInfoBean, ItemReminderEventBinding> {
        return object : CommonAdapter<EventInfoBean, ItemReminderEventBinding>(events) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemReminderEventBinding {
                return ItemReminderEventBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemReminderEventBinding, t: EventInfoBean, position: Int) {
                v.tvContent.text = t.description
                v.tvDate.text = StringBuilder().append(t.time.year).append("-").append(t.time.month)
                    .append("-").append(t.time.day)
                v.tvTime.text =
                    StringBuilder().append(if (t.time.hour < 10) "0${t.time.hour}" else t.time.hour)
                        .append(":")
                        .append(if (t.time.minute < 10) "0${t.time.minute}" else t.time.minute)

                v.root.setOnClickListener {
                    startActivityForResult(
                        Intent(
                            this@ClockEventReminderActivity,
                            AddEventActivity::class.java
                        ).putExtra("data", t).putExtra("index", position), type
                    )
                }

                v.root.setOnLongClickListener {
                    showDelDialog(position, t.description)
                    return@setOnLongClickListener true
                }

            }
        }
    }
    //endregion

    //region initData
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun initData() {
        super.initData()
        if (type == Global.REMINDER_TYPE_CLOCK) {
            viewModel.deviceSettingLiveData.getClockInfo().observe(this) { list ->
                if (list == null) return@observe
                LogUtils.d("闹钟提醒 —>${list.size}")
                LogUtils.json(list)
                clocks.clear()
                clocks.addAll(list)
                //clocks.reverse() //倒置
                binding.tvHint.visibility = if (clocks.size == 0) View.VISIBLE else View.GONE
                binding.dataLayout.visibility = if (clocks.size > 0) View.VISIBLE else View.GONE
                binding.tvAddedNum.text = getString(R.string.added) + clocks.size + "/" + maxClocks
                binding.recyclerView.adapter?.notifyDataSetChanged()

                if (::delDialog.isInitialized && delDialog.isShowing) {
                    delDialog.dismiss()
                }
            }
            viewModel.deviceSettingLiveData.getClockMax().observe(this) { max ->
                maxClocks = max
            }
            loadDialog.show()
            ControlBleTools.getInstance().getClockInfoList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    DialogUtils.dismissDialog(loadDialog)
                    ToastUtils.showSendCmdStateTips(state) {
                        //获取超时直接关闭页面
                        finish()
                    }
                }
            })

        } else {
            viewModel.deviceSettingLiveData.getEventInfos().observe(this) { list ->
                if (list == null) return@observe
                LogUtils.d("事件提醒 —>${GsonUtils.toJson(list)}")
                events.clear()
                events.addAll(list)
                binding.tvHint.visibility = if (events.size == 0) View.VISIBLE else View.GONE
                binding.dataLayout.visibility = if (events.size > 0) View.VISIBLE else View.GONE
                binding.tvAddedNum.text = getString(R.string.added) + events.size + "/" + maxEvents
                binding.recyclerView.adapter?.notifyDataSetChanged()

                if (::delDialog.isInitialized && delDialog.isShowing) {
                    delDialog.dismiss()
                }
            }
            viewModel.deviceSettingLiveData.getEventMax().observe(this) { max ->
                maxEvents = max
            }
            loadDialog.show()
            ControlBleTools.getInstance().getEventInfoList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    DialogUtils.dismissDialog(loadDialog)
                    ToastUtils.showSendCmdStateTips(state) {
                        //获取超时直接关闭页面
                        finish()
                    }
                }
            })
        }
    }
    //endregion

    //region 弹窗提示

    /**
     * 删除提示
     * */
    private lateinit var delDialog: Dialog
    fun showDelDialog(index: Int, name: String? = null) {
        delDialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            if (type == Global.REMINDER_TYPE_CLOCK) {
                getString(R.string.delete_list_tips_colck_tips)
            } else {
                getString(R.string.delete_list_event_tips)
            },
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun OnOK() {
                    delDialog.dismiss()
                    if (type == Global.REMINDER_TYPE_CLOCK) {
                        val tempList: MutableList<ClockInfoBean> = mutableListOf()
                        tempList.addAll(clocks)
                        if (index < tempList.size) {
                            tempList.removeAt(index)
                            sendNewClocks(tempList)
                        }
                    } else if (type == Global.REMINDER_TYPE_EVENT) { //事件
                        val tempList: MutableList<EventInfoBean> = mutableListOf()
                        tempList.addAll(events)
                        if (index < tempList.size) {
                            tempList.removeAt(index)
                            sendNewEvents(tempList)
                        }
                    }
                }

                override fun OnCancel() {}
            })
        delDialog.show()
    }

    /**
     * 提示已经最大
     * */
    var maxDialog: Dialog? = null
    private fun showMaxHint() {
        maxDialog = DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            if (type == Global.REMINDER_TYPE_CLOCK) getString(R.string.add_alarm_max_tips) else getString(R.string.add_event_max_tips),
            getString(R.string.dialog_confirm_btn),
            null
        )
        maxDialog?.show()
        maxDialog?.setCanceledOnTouchOutside(true)
    }
    //endregion

    //region 设置新闹钟数据
    @SuppressLint("NotifyDataSetChanged")
    private fun sendNewClocks(tempList: MutableList<ClockInfoBean>) {
        //重新按下标赋值id
        for (i in 0 until tempList.size) {
            tempList.get(i).id = i
        }
        LogUtils.d("闹钟提醒设置 templist == ${tempList.size}")
        LogUtils.json(tempList)
        loadDialog.show()
        ControlBleTools.getInstance().setClockInfoList(tempList, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> {
                        ControlBleTools.getInstance().getClockInfoList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(p0: SendCmdState?) {
                                DialogUtils.dismissDialog(loadDialog)
                            }
                        })
                    }
                    else -> {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                    }
                }
            }
        })
    }
    //endregion

    //region 设置新闹钟数据
    private fun sendNewEvents(tempList: MutableList<EventInfoBean>) {
        LogUtils.d("事件提醒设置 templist == " + GsonUtils.toJson(tempList))
        loadDialog.show()
        ControlBleTools.getInstance().setEventInfoList(tempList, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> {
                        ControlBleTools.getInstance().getEventInfoList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(p0: SendCmdState?) {
                                DialogUtils.dismissDialog(loadDialog)
                            }
                        })
                    }
                    else -> {
                        DialogUtils.dismissDialog(loadDialog)
                        ToastUtils.showSendCmdStateTips(state)
                    }
                }
            }
        })
    }
    //endregion

    //region click

    override fun onClick(v: View) {
        when (v.id) {
            ivRightIcon?.id -> {
                if (type == Global.REMINDER_TYPE_CLOCK) {
                    if (clocks.size == maxClocks && maxClocks != 0) {
                        showMaxHint()
                        return
                    }
                    val intent = Intent(this, AddAlarmClockActivity::class.java)
                    //intent.putExtra("index",clocks.size+1) //下一个闹钟名： 闹钟 数量+1
                    startActivityForResult(
                        intent, type
                    )
                } else {
                    if (events.size == maxEvents && maxEvents != 0) {
                        showMaxHint()
                        return
                    }
                    startActivityForResult(
                        Intent(this, AddEventActivity::class.java), type
                    )
                }
            }
        }
    }
    //endregion

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Global.REMINDER_TYPE_CLOCK) { //保存新的闹钟
                val clockInfo = data?.getSerializableExtra("data") as ClockInfoBean?
                if (clockInfo != null) {
                    val tempList: MutableList<ClockInfoBean> = mutableListOf()
                    tempList.addAll(clocks)
                    tempList.findLast { it.id == clockInfo.id }?.let {
                        tempList.remove(it)
                    }
                    if (clockInfo.id == -1 || clockInfo.id >= tempList.size) {
                        tempList.add(clockInfo)
                    } else {
                        tempList.add(clockInfo.id, clockInfo)
                    }
                    sendNewClocks(tempList)
                    AppTrackingManager.saveOnlyBehaviorTracking("6", "6")
                }
            } else if (requestCode == Global.REMINDER_TYPE_EVENT) {
                val eventInfo = data?.getSerializableExtra("data") as EventInfoBean?
                val index = data?.getIntExtra("index", 0)
                if (eventInfo != null) {
                    val tempList: MutableList<EventInfoBean> = mutableListOf()
                    tempList.addAll(events)
                    if (index != null && index != -1) {
                        tempList.removeAt(index)
                        tempList.add(index, eventInfo)
                    } else {
                        tempList.add(eventInfo)
                    }
                    sendNewEvents(tempList)
                    AppTrackingManager.saveOnlyBehaviorTracking("6", "2")
                }
            }
        }
    }


}