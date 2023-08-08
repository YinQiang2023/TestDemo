package com.smartwear.xzfit.ui.healthy.history.womenhealth

import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseViewModel
import com.smartwear.xzfit.databinding.ActivityWomenPeriodSettingBinding
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.view.wheelview.BirthdayPicker
import com.smartwear.xzfit.view.wheelview.NumberPicker
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.PhysiologicalCycleBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.PhysiologicalCycleCallBack
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.view.wheelview.entity.DateEntity
import java.lang.ref.WeakReference
import java.util.*

class WomenPeriodSettingActivity : BaseActivity<ActivityWomenPeriodSettingBinding, BaseViewModel>(
    ActivityWomenPeriodSettingBinding::inflate,
    BaseViewModel::class.java
), View.OnClickListener {

    private var type = 0
    private lateinit var physiologicalCycleBean: PhysiologicalCycleBean

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.layoutMenstrualCycleLength.id -> {
                showMenstrualCycleLengthDialog()
            }
            binding.layoutPeriodLength.id -> {
                showPeriodLengthDialog()
            }
            binding.layoutCycleTime.id -> {
                showCycleTimeDialog()
            }
            binding.tvSave.id -> {
                save()
            }
        }
    }

    override fun initView() {
        super.initView()
        binding.title.tvCenterTitle.text = getString(R.string.women_period_title)
        type = intent.getIntExtra("type", 0)
        setViewsClickListener(
            this, binding.layoutMenstrualCycleLength, binding.layoutPeriodLength,
            binding.layoutCycleTime, binding.tvSave
        )
//        physiologicalCycleBean = JSON.parseObject(SpUtils.getValue(SpUtils.WOMEN_HEALTH_DATA_FROM_DEVICE,"") , PhysiologicalCycleBean::class.java)
        if (Global.physiologicalCycleBean == null) {
            finish()
            return
        }
        physiologicalCycleBean = Global.physiologicalCycleBean!!
    }

    override fun initData() {
        super.initData()
        binding.tvMenstrualCycleLength.text = if (Global.physiologicalCycleBean?.physiologicalCycleDay == 0) {
            "5"
        } else {
            "${Global.physiologicalCycleBean?.physiologicalCycleDay}"
        }
        binding.tvPeriodLength.text = if (Global.physiologicalCycleBean?.totalCycleDay == 0) {
            "28"
        } else {
            "${Global.physiologicalCycleBean?.totalCycleDay}"
        }
        val date = Global.physiologicalCycleBean?.physiologicalStartDate
        val dateStr = "${date?.year}-${date?.month}-${date?.day}"
        binding.tvCycleTime.text = dateStr
//        binding.tvCycleTime.text = if (SpUtils.getValue(SpUtils.WOMEN_HEALTH_CYCLE_TIME , "").isNullOrEmpty()){
//            DateUtils.getStringDate(System.currentTimeMillis() , DateUtils.TIME_YYYY_MM_DD)
//        }else{
//            SpUtils.getValue(SpUtils.WOMEN_HEALTH_CYCLE_TIME , "")
//        }
    }

    private fun showMenstrualCycleLengthDialog() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            binding.tvMenstrualCycleLength.text = item.toString()
        }
        picker.setRange(1, 60, 1)
        if (!TextUtils.isEmpty(binding.tvMenstrualCycleLength.text.toString().trim())) {
            picker.setDefaultValue(binding.tvMenstrualCycleLength.text.toString().trim().toInt())
        } else {
            picker.setDefaultValue(1)
        }

        picker.wheelLayout.setCyclicEnabled(true)
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        //picker.okView.setTextColor(ContextCompat.getColor(this ,R.color.color_E96192))
        picker.show()
    }

    private fun showPeriodLengthDialog() {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            binding.tvPeriodLength.text = item.toString()
        }
        picker.setRange(7, 100, 1)
        if (!TextUtils.isEmpty(binding.tvPeriodLength.text.toString().trim())) {
            picker.setDefaultValue(binding.tvPeriodLength.text.toString().trim().toInt())
        } else {
            picker.setDefaultValue(93 / 2)
        }
        picker.wheelLayout.setCyclicEnabled(true)
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        //picker.okView.setTextColor(ContextCompat.getColor(this ,R.color.color_E96192))
        picker.show()
    }

    private fun showCycleTimeDialog() {
        val picker = BirthdayPicker(this)
        picker.setOnDatePickedListener { year, month, day ->
            binding.tvCycleTime.text = "$year-$month-$day"
        }
        picker.setDefaultValue(1991, 11, 11)
        val array = binding.tvCycleTime.text.toString().trim().split("-")
        if (array.size >= 3) {
            picker.setDefaultValue(array[0].trim().toInt(), array[1].trim().toInt(), array[2].trim().toInt())
        } else {
            picker.setDefaultValue(1991, 11, 11)
        }
        val calendar = Calendar.getInstance()
        val nowY = calendar.get(Calendar.YEAR)
        val nowM = calendar.get(Calendar.MONTH) + 1
        val nowD = calendar.get(Calendar.DAY_OF_MONTH)
        picker.wheelLayout.setRange(DateEntity.target(1970, 1, 1), DateEntity.target(nowY, nowM, nowD))
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun save() {
        if (TextUtils.isEmpty(binding.tvMenstrualCycleLength.text.toString().trim()) &&
            TextUtils.isEmpty(binding.tvPeriodLength.text.toString().trim()) &&
            TextUtils.isEmpty(binding.tvCycleTime.text.toString().trim())
        )
            ToastUtils.showToast(R.string.women_health_save_tips)

        val dialog = DialogUtils.showLoad(this)
        dialog.show()
        val array = binding.tvCycleTime.text.toString().trim().split("-")
        val dataBean = PhysiologicalCycleBean.DateBean()
        if (array.size >= 3) {
            dataBean.year = array[0].trim().toInt()
            dataBean.month = array[1].trim().toInt()
            dataBean.day = array[2].trim().toInt()
        } else {
            ToastUtils.showToast(R.string.women_health_save_tips1)
        }
        Global.physiologicalCycleBean?.physiologicalCycleDay = binding.tvPeriodLength.text.toString().trim().toInt()
        Global.physiologicalCycleBean?.totalCycleDay = binding.tvMenstrualCycleLength.text.toString().trim().toInt()
        Global.physiologicalCycleBean?.physiologicalStartDate = dataBean
        Global.physiologicalCycleBean?.preset = true

        val bean = PhysiologicalCycleBean()
        bean.remindSwitch = Global.physiologicalCycleBean?.remindSwitch!!
        bean.advanceDay = Global.physiologicalCycleBean?.advanceDay!!
        bean.totalCycleDay = binding.tvPeriodLength.text.toString().trim().toInt()
        bean.physiologicalCycleDay = binding.tvMenstrualCycleLength.text.toString().trim().toInt()
        bean.preset = true
        bean.physiologicalStartDate = Global.physiologicalCycleBean?.physiologicalStartDate
        LogUtils.d("set PhysiologicalCycleBean -->$bean")
        ControlBleTools.getInstance().setPhysiologicalCycle(bean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
            }
        })
        CalcCycleDataUtils.loadCycleData()

        CallBackUtils.physiologicalCycleCallBack = MyPhysiologicalCycleCallBack(this, dialog)
        ControlBleTools.getInstance().getPhysiologicalCycle(object :ParsingStateManager.SendCmdStateListener(this.lifecycle){
            override fun onState(state: SendCmdState) {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    class MyPhysiologicalCycleCallBack(activity: WomenPeriodSettingActivity, dialog: Dialog) : PhysiologicalCycleCallBack {
        private var wrActivity: WeakReference<WomenPeriodSettingActivity>? = null
        private var wrDialog: WeakReference<Dialog>? = null

        init {
            wrActivity = WeakReference(activity)
            wrDialog = WeakReference(dialog)
        }

        override fun onPhysiologicalCycleResult(bean: PhysiologicalCycleBean) {
            wrActivity?.get()?.let { activity ->
                if (wrDialog?.get()?.isShowing == true) {
                    wrDialog?.get()?.dismiss()
                }
                Global.physiologicalCycleBean = bean
                if (activity.type == 0) {
                    activity.finish()
                } else {
                    activity.startActivity(Intent(activity, WomenHealthActivity::class.java))
                    activity.finish()
                }
            }
        }

    }
}