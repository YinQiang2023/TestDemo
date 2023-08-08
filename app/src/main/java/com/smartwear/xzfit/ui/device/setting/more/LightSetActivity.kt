package com.smartwear.xzfit.ui.device.setting.more

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.KeyEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityLightSetBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.view.wheelview.NumberPicker
import com.smartwear.xzfit.view.wheelview.contract.WheelFormatter
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ScreenSettingBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.ui.device.bean.DeviceSettingBean
import com.smartwear.xzfit.utils.SpUtils
import java.lang.StringBuilder

/**
 * Created by Android on 2021/10/28.
 */
class LightSetActivity : BaseActivity<ActivityLightSetBinding, DeviceModel>(
    ActivityLightSetBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    //等待loading
    private lateinit var loadDialog: Dialog

    //产品功能列表
    private val deviceSettingBean: DeviceSettingBean? by lazy {
        JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    //是否未提交修改
    private var isUnCommit = false

    private var mLevel = 0
    private var mTime = 0

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.dev_more_set_lum)

        loadDialog = DialogUtils.showLoad(this)

        if (deviceSettingBean != null) {
            if (!deviceSettingBean!!.settingsRelated.bright_adjustment) {
                binding.llLevel.visibility = View.GONE
                binding.line1.visibility = View.GONE
            }
            if (!deviceSettingBean!!.settingsRelated.bright_screen_time) {
                binding.llTime.visibility = View.GONE
                binding.line2.visibility = View.GONE
            }
            if (!deviceSettingBean!!.settingsRelated.double_click_to_brighten_the_screen) {
                binding.llDoubleClick.visibility = View.GONE
            }
        }

        setViewsClickListener(
            this,
            tvTitle!!,
            binding.llLevel,
            binding.llTime,
            binding.btnSave
        )

        binding.mNormallySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            isUnCommit = true
            binding.llTime.isEnabled = !isChecked
            //binding.v3.setTextColor(if(isChecked) ContextCompat.getColor(this,R.color.color_FFFFFF_20) else ContextCompat.getColor(this,R.color.color_FFFFFF))
            binding.tvTime.setTextColor(if (isChecked) ContextCompat.getColor(this, R.color.color_FFFFFF_20) else ContextCompat.getColor(this, R.color.color_FFFFFF))
        }
        binding.mDoubleSwitch.setOnCheckedChangeListener { buttonView, isChecked -> isUnCommit = true }
    }

    @SuppressLint("SetTextI18n")
    override fun initData() {
        super.initData()
        viewModel.deviceSettingLiveData.getScreenSetting().observe(this) { bean ->
            LogUtils.d("屏幕设置 ->${GsonUtils.toJson(bean)}")
            mLevel = bean.level
            binding.tvLevel.text = "${bean.level}"
            mTime = bean.duration
            binding.tvTime.text = "${bean.duration} ${getString(R.string.unit_secs)}"
            binding.mDoubleSwitch.isChecked = bean.doubleClick
            binding.mNormallySwitch.isChecked = bean.isSwitch
            isUnCommit = false
        }
        loadDialog.show()
        ControlBleTools.getInstance().getScreenSetting(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state) {
                    //获取超时直接关闭页面
                    finish()
                }
            }
        })
    }

    //region 选择时长
    private fun showTimeDialog(time: Int) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            //item.toInt()
            mTime = item.toInt()
            binding.tvTime.text = "$mTime ${getString(R.string.unit_secs)}"
            isUnCommit = true

        }
        picker.setRangeStep(1, 4, 5)
        picker.setDefaultValue(time)
        picker.setFormatter(WheelFormatter { item ->
            return@WheelFormatter "${item} ${getString(R.string.unit_secs)}"
        })
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }
    //endregion

    //region 选择等级
    private fun showLevelDialog(level: Int) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            //item.toInt()
            mLevel = item.toInt()
            binding.tvLevel.text = "$mLevel"
            isUnCommit = true
        }
        picker.setRangeStep(1, 5, 1)
        picker.setDefaultValue(level)
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }
    //endregion


    //region 未提交提示
    private fun showUnCommitDialog() {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.save_nu_commit_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    sendLight()
                }

                override fun OnCancel() {
                    finish()
                }
            })
        dialog.show()
    }
    //endregion

    //region 设置亮度
    /**
     * @param isShowPowerSaving 是否检测省电模式 默认true
     */
    private fun sendLight(isShowPowerSaving: Boolean = true) {
        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        //功能启用 值为0
        if (deviceSettingBean != null && deviceSettingBean!!.settingsRelated.bright_adjustment && mLevel == 0) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.brightness_level)).toString()
            )
            return
        }
        //功能启用 常亮未开 值为0
        if (deviceSettingBean != null && deviceSettingBean!!.settingsRelated.bright_screen_time &&
            !binding.mNormallySwitch.isChecked &&
            mTime == 0
        ) {
            ToastUtils.showToast(
                StringBuilder().append(getString(R.string.user_info_please_choose))
                    .append(getString(R.string.bright_screen_time)).toString()
            )
            return
        }
        if (isShowPowerSaving &&
            SpUtils.getSPUtilsInstance().getBoolean(SpUtils.DEVICE_POWER_SAVING) &&
            (mLevel > 1 || binding.mNormallySwitch.isChecked)
        ) {
            showClosePowerSavingDialog()
            return
        }
        val bean = ScreenSettingBean()
        bean.isSwitch = binding.mNormallySwitch.isChecked
        bean.level = mLevel
        bean.duration = mTime
        bean.doubleClick = binding.mDoubleSwitch.isChecked
        loadDialog.show()
        LogUtils.d("屏幕设置发送 ->${GsonUtils.toJson(bean)}")
        ControlBleTools.getInstance().setScreenSetting(bean, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                if (state == SendCmdState.SUCCEED) {
                    isUnCommit = false
                    ToastUtils.showToast(R.string.save_success)
                    finish()
                }
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    /**
     * 关闭息屏显示dialgo
     * */
    private fun showClosePowerSavingDialog() {
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.close_power_saving),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    loadDialog.show()
                    ControlBleTools.getInstance().setPowerSaving(false,
                        object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                            override fun onState(state: SendCmdState) {
                                DialogUtils.dismissDialog(loadDialog)
                                ToastUtils.showSendCmdStateTips(state)
                                ControlBleTools.getInstance().getPowerSaving(null)
                                sendLight(false)
                            }
                        })
                }

                override fun OnCancel() {

                }
            }).show()

    }
    //endregion


    override fun onClick(v: View) {
        when (v.id) {
            tvTitle?.id -> {
                /*if (isUnCommit) {
                    showUnCommitDialog()
                    return
                }*/
                finish()
            }
            binding.llLevel.id -> {
                showLevelDialog(mLevel)
            }
            binding.llTime.id -> {
                showTimeDialog(mTime)
            }
            binding.btnSave.id -> {
                sendLight()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        /*if (keyCode == KeyEvent.KEYCODE_BACK && isUnCommit) {
            showUnCommitDialog()
            return false
        }*/
        return super.onKeyDown(keyCode, event)
    }

}