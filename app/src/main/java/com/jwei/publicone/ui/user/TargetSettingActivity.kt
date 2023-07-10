package com.jwei.publicone.ui.user

import android.app.Dialog
import android.text.TextUtils
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.ui.adapter.CommonAdapter
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import androidx.lifecycle.Observer
import com.alibaba.fastjson.JSON
import com.jwei.publicone.databinding.*
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.ui.user.utils.UnitConverUtils
import com.jwei.publicone.view.wheelview.NumberPicker
import com.jwei.publicone.view.wheelview.SleepPicker
import com.jwei.publicone.ui.device.bean.DeviceSettingBean
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.ui.user.bean.TargetBean
import com.jwei.publicone.utils.*
import com.jwei.publicone.viewmodel.UserModel
import org.greenrobot.eventbus.EventBus


class TargetSettingActivity : BaseActivity<ActivityTargetSettingBinding, UserModel>(ActivityTargetSettingBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = TargetSettingActivity::class.java.simpleName
    private lateinit var mTargetBean: TargetBean
    private val list: MutableList<MutableMap<String, *>> = ArrayList()

    private var dialog: Dialog? = null

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_fragment_set_goal)
        setViewsClickListener(
            this, binding.btnFinish,
            tvTitle!!
        )
        initRv()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            tvTitle!!.id -> {
                finish()
            }
            binding.btnFinish.id -> {
                clickFinishBtn()
            }
        }
    }


    //产品功能列表
    private val deviceSettingBean by lazy {
        JSON.parseObject(SpUtils.getValue(SpUtils.DEVICE_SETTING, ""), DeviceSettingBean::class.java)
    }


    override fun initData() {
        viewModel.uploadTargetInfo.observe(this, Observer {
            Log.i(TAG, "initData() uploadTargetInfo it = ")
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        TargetBean().saveData(mTargetBean)
                        SendCmdUtils.setUserInformation()
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_UPDATE_TARGET_INFO))
                        finish()
//                        ToastUtils.showToast("同步个人数据成功")
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                }
            }
        })
    }


    private fun initRv() {
        fillData()
        restoreData()
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@TargetSettingActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.targetSettingNameList)
        var imgs = resources.obtainTypedArray(R.array.targetSettingImgList)

        for (index in texts.indices) {
            if (checkLoad(texts[index].toString())) {
                val map: MutableMap<String, Any> = HashMap()
                map["content"] = texts[index]
                map["img"] = imgs.getResourceId(index, 0)
                map["value"] = ""
                map["unit"] = ""
                list.add(map)
            }
        }
        imgs.recycle()
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkLoad(s: String?): Boolean {
        if (deviceSettingBean != null) {
            when (s) {
                getString(R.string.healthy_sports_list_step) -> {
                    return deviceSettingBean.settingsRelated.step_goal
                }
                getString(R.string.healthy_sports_list_distance) -> {
                    return deviceSettingBean.settingsRelated.distance_target
                }
                getString(R.string.healthy_sports_list_calories) -> {
                    return deviceSettingBean.settingsRelated.calorie_goal
                }
                getString(R.string.sleep_title) -> {
                    return deviceSettingBean.settingsRelated.sleep_goal
                }
            }
        }
        return true
    }

    private fun restoreData() {
        mTargetBean = TargetBean().getData()
        Log.i(TAG, "restoreData() mTargetBean = " + mTargetBean)
        if (mTargetBean != null) {
            list.clear()
            val texts = resources.getStringArray(R.array.targetSettingNameList)
            var imgs = resources.obtainTypedArray(R.array.targetSettingImgList)
            for (index in texts.indices) {
                val map: MutableMap<String, Any> = HashMap()
                if (checkLoad(texts[index].toString())) {
                    map["content"] = texts[index]
                    map["img"] = imgs.getResourceId(index, 0)
                    when (texts[index].toString()) {
                        getString(R.string.healthy_sports_list_step) -> {
                            //模拟数据
//                            upLoadBean.sportTarget = 50000;
                            Log.i(TAG, "createStepGoalDialog upLoadBean.sportTarget 1 = " + mTargetBean.sportTarget)
                            if (mTargetBean.sportTarget.toInt() < Constant.STEP_TARGET_MIN_VALUE) {
                                mTargetBean.sportTarget = Constant.STEP_TARGET_MIN_VALUE.toString();
                            } else if (mTargetBean.sportTarget.toInt() > Constant.STEP_TARGET_MAX_VALUE) {
                                mTargetBean.sportTarget = Constant.STEP_TARGET_MAX_VALUE.toString();
                            }
                            Log.i(TAG, "createStepGoalDialog upLoadBean.sportTarget 2 = " + mTargetBean.sportTarget)
                            map["value"] = mTargetBean.sportTarget.toString()
                            if (mTargetBean.sportTarget.trim().toInt() > 1) {
                                map["unit"] = getString(R.string.unit_steps)
                            } else {
                                map["unit"] = getString(R.string.unit_step)
                            }
                        }
                        getString(R.string.healthy_sports_list_distance) -> {
                            map["value"] = getDistanceByUnit(mTargetBean.distanceTarget.toString(), false)[1]
                            map["unit"] = getDistanceByUnit(mTargetBean.distanceTarget.toString(), true)[0]
                        }
                        getString(R.string.healthy_sports_list_calories) -> {
                            map["value"] = mTargetBean.consumeTarget.toString()
                            map["unit"] = getString(R.string.unit_calories)
                        }
                        getString(R.string.sleep_title) -> {
                            map["value"] = TimeUtils.getHoursAndMinutes(mTargetBean.sleepTarget.toInt(), this)
                            map["unit"] = ""
                        }
                    }
                    list.add(map)
                }
            }
            imgs.recycle()
        }
    }

    private fun getDistanceByUnit(height: String, needSwitch: Boolean): Array<String> {
        val valueStr: Array<String> = arrayOf("", "")
        var unit = ""
        var temp = ""
        if (mTargetBean.unit == "0" || !needSwitch) {
            var sum = 0
            sum = height.trim().toInt()
            temp = "$sum"
            unit = getString(R.string.unit_distance_0)
        } else {
            temp = UnitConverUtils.kmToMi(height)
            unit = getString(R.string.unit_distance_1)
        }
        valueStr[0] = unit
        valueStr[1] = temp
        return valueStr
    }

    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemUserInfoBinding {
                return ItemUserInfoBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemUserInfoBinding, t: MutableMap<String, *>, position: Int) {
                v.tvItemLeft.text = "${t["content"]}"
                v.ivItemLeft.setImageResource(t["img"] as Int)
                ("${t["value"]}" + " " + "${t["unit"]}").also { v.tvItemRight.text = it }
                v.ivItemLeft.visibility = View.GONE

                if (position == (list.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }

                v.cslItemUserInfoParent.setOnClickListener {
                    try {
                        when (v.tvItemLeft.text.toString()) {
                            getString(R.string.healthy_sports_list_step) -> {
                                createStepGoalDialog(position)
                            }
                            getString(R.string.healthy_sports_list_distance) -> {
                                createDistanceTargetDialog(position)
                            }
                            getString(R.string.healthy_sports_list_calories) -> {
                                createCaloriesTargetDialog(position)
                            }
                            getString(R.string.sleep_title) -> {
                                createSleepTargetDialog(position)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun resetRvData(rightStr: String, position: Int, unit: String) {
        val map: MutableMap<String, Any> = HashMap()
        map["content"] = list[position]["content"] as String
        map["value"] = rightStr
        map["unit"] = list[position]["unit"] as String
        map["img"] = list[position]["img"] as Int
        list[position] = map
        binding.rvList.adapter?.notifyItemChanged(position)
    }

    private fun createStepGoalDialog(position: Int) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.sportTarget = item.toString()
            resetRvData(mTargetBean.sportTarget.toString(), position, getString(R.string.unit_step))
        }
        picker.setRangeStep(
            Constant.STEP_TARGET_MIN_VALUE / Constant.STEP_TARGET_SCALE_VALUE,
            Constant.STEP_TARGET_MAX_VALUE / Constant.STEP_TARGET_SCALE_VALUE,
            Constant.STEP_TARGET_SCALE_VALUE
        )



        if (mTargetBean.sportTarget.toInt() != 0) {
            picker.setDefaultValue(mTargetBean.sportTarget)
        } else {
            picker.setDefaultValue(Constant.STEP_TARGET_DEFAULT_VALUE)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun createDistanceTargetDialog(positions: Int) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.distanceTarget = item.toString()
            //公制
            if (mTargetBean.unit.trim().toInt() == 0) {
                resetRvData(item.toString(), positions, getString(R.string.unit_distance_0))
            }
            //英制
            else {
                resetRvData(item.toString(), positions, getString(R.string.unit_distance_1))
            }
        }
        picker.setRange(Constant.DISTANCE_TARGET_MIN_VALUE, Constant.DISTANCE_TARGET_MAX_VALUE, 1)
        Log.i(TAG, "createDistanceTargetDialog upLoadBean.distanceTarget 0 = " + mTargetBean.distanceTarget)
        if (mTargetBean.distanceTarget.toInt() != 0) {
            Log.i(TAG, "createDistanceTargetDialog upLoadBean.distanceTarget 1 = " + mTargetBean.distanceTarget)
            picker.setDefaultValue(mTargetBean.distanceTarget.toString())
        } else {
            Log.i(TAG, "createDistanceTargetDialog upLoadBean.distanceTarget 2 = " + mTargetBean.distanceTarget)
            picker.setDefaultValue(Constant.DISTANCE_TARGET_DEFAULT_VALUE)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun createCaloriesTargetDialog(position: Int) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.consumeTarget = item.toString()
            resetRvData(mTargetBean.consumeTarget.toString(), position, getString(R.string.unit_calories))
        }
        picker.setRangeStep(
            Constant.CALORIE_TARGET_MIN_VALUE / Constant.CALORIE_TARGET_SCALE_VALUE,
            Constant.CALORIE_TARGET_MAX_VALUE / Constant.CALORIE_TARGET_SCALE_VALUE,
            Constant.CALORIE_TARGET_SCALE_VALUE
        )
        if (mTargetBean.consumeTarget.toInt() != 0) {
            picker.setDefaultValue(mTargetBean.consumeTarget)
        } else {
            picker.setDefaultValue(Constant.CALORIE_TARGET_DEFAULT_VALUE)
        }

        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun createSleepTargetDialog(position: Int) {
        val sleepList = resources.getStringArray(R.array.sleepTarget)
        val sleepUnitList = resources.getStringArray(R.array.unit_sleep)
        val picker = SleepPicker(this)
        picker.setData(sleepList.toMutableList(), sleepUnitList.toMutableList())
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setOnOptionPickedListener { item ->
            mTargetBean.sleepTarget = TimeUtils.getMinutesByTimeForStyle(item)
            resetRvData(TimeUtils.getHoursAndMinutes(mTargetBean.sleepTarget.trim().toInt(), this), position, "")
        }
        if (!TextUtils.isEmpty(mTargetBean.sleepTarget) && mTargetBean.sleepTarget.trim().toInt() != 0) {
            val timeForStyleByMinutes = TimeUtils.getTimeForStyleByMinutes(mTargetBean.sleepTarget.trim().toInt())
            picker.setDefaultValue(timeForStyleByMinutes[0], timeForStyleByMinutes[1])
        } else {
            picker.setDefaultValue("08", "00")
        }
        picker.show()
    }

    private fun clickFinishBtn() {
        if (mTargetBean != null) {
            dialog = DialogUtils.dialogShowLoad(this)
            viewModel.uploadTargetInfo(mTargetBean)
        }
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

}