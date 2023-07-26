package com.jwei.xzfit.ui.device.setting.remind

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSON
import com.zhapp.ble.ControlBleTools
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityRemindSetBinding
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.DeviceModel

/**
 * Created by Android on 2021/10/6.
 * 提醒通知设置 目录
 */
class RemindSetActivity : BaseActivity<ActivityRemindSetBinding, DeviceModel>(
    ActivityRemindSetBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private var mData: LinkedHashMap<String, Int> = LinkedHashMap()

    //产品功能列表
    private val deviceSettingBean by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_remind)
    }


    override fun initData() {
        super.initData()

        val texts = resources.getStringArray(R.array.deviceRemindStrList)
        val imgs = resources.obtainTypedArray(R.array.deviceRemindImgList)
        for (i in texts.indices) {
            mData[texts.get(i)] = imgs.getResourceId(i, 0)
        }
        imgs.recycle()
        binding.llRemindSetList.removeAllViews()
        mData.entries.forEach {
            if (checkload(it.key)) {
                val constraintLayout = LayoutInflater.from(this).inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                tvName.text = it.key
                image.background = ContextCompat.getDrawable(this, it.value)
                image.visibility = View.GONE
                constraintLayout.tag = it.key
                setViewsClickListener(this, constraintLayout)
                binding.llRemindSetList.addView(constraintLayout)
                if (it.key == getString(R.string.device_remind_hand_washing)) {
                    viewLine01.visibility = View.GONE
                } else {
                    viewLine01.visibility = View.VISIBLE
                }
            }

        }
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkload(key: String): Boolean {
        if (deviceSettingBean != null) {
            when (key) {
                getString(R.string.device_remind_sedentary) -> {
                    return deviceSettingBean.reminderRelated.sedentary
                }
                getString(R.string.device_remind_drink_water) -> {
                    return deviceSettingBean.reminderRelated.drink_water
                }
                getString(R.string.device_remind_take_pills) -> {
                    return deviceSettingBean.reminderRelated.reminder_to_take_medicine
                }
                getString(R.string.device_remind_alarm_clock) -> {
                    return deviceSettingBean.reminderRelated.alarm_clock
                }
                getString(R.string.device_remind_hand_washing) -> {
                    return deviceSettingBean.reminderRelated.hand_washing_reminder
                }
                getString(R.string.device_remind_event) -> {
                    return deviceSettingBean.reminderRelated.event_reminder
                }

            }
        }
        return true
    }

    override fun onClick(v: View?) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        when (v?.tag) {
            getString(R.string.device_remind_sedentary) -> {
                startActivity(
                    Intent(this, ReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_SEDENTARY)
                )
            }
            getString(R.string.device_remind_drink_water) -> {
                startActivity(
                    Intent(this, ReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_DRINK)
                )
            }
            getString(R.string.device_remind_take_pills) -> {
                startActivity(
                    Intent(this, ReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_PILLS)
                )
            }
            getString(R.string.device_remind_hand_washing) -> {
                startActivity(
                    Intent(this, ReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_HAND_WASH)
                )
            }
            getString(R.string.device_remind_alarm_clock) -> {
                startActivity(
                    Intent(this, ClockEventReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_CLOCK)
                )
            }
            getString(R.string.device_remind_event) -> {
                startActivity(
                    Intent(this, ClockEventReminderActivity::class.java)
                        .putExtra("type", Global.REMINDER_TYPE_EVENT)
                )
            }
        }
    }
}