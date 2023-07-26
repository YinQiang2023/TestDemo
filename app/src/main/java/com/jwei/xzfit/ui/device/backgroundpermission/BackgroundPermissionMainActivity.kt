package com.jwei.xzfit.ui.device.backgroundpermission

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.BackgroundPermissionMainActivityBinding
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.manager.AppTrackingManager
import com.jwei.xzfit.view.MySwitchCompat
import com.jwei.xzfit.view.wheelview.OptionPicker
import com.jwei.xzfit.view.wheelview.contract.TextProvider
import com.jwei.xzfit.viewmodel.DeviceModel
import java.io.Serializable
import java.util.*

/**
 * Created by Android on 2022/1/10.
 */
class BackgroundPermissionMainActivity : BaseActivity<BackgroundPermissionMainActivityBinding, DeviceModel>(
    BackgroundPermissionMainActivityBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private val inflater: LayoutInflater by lazy { LayoutInflater.from(this) }

    //当前手机类型
    private var curValue: Int = 0

    //手机类型集合
    private val mModelData: MutableList<BackgroundPermissionMainActivity.Bean> by lazy { mutableListOf() }

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.running_permission_title)

        initRomValue()

        setViewsClickListener(this, binding.layoutPersonInfo)
    }

    @SuppressLint("BatteryLife")
    override fun initData() {
        super.initData()


        SpUtils.setValue(SpUtils.POWER_OPTIMIZATIONS, if (AppUtils.isIgnoringBatteryOptimizations()) "1" else "0")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent()
                val packageName: String = getPackageName()
                val pm: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LogUtils.d("后台保活状态：" + AppUtils.isIgnoringBatteryOptimizations())
        val oldValue = SpUtils.getValue(SpUtils.POWER_OPTIMIZATIONS, "0")
        if (TextUtils.equals(oldValue, "0") && AppUtils.isIgnoringBatteryOptimizations()) {
            AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("12", "39").apply {
                functionStatus = "1"
                functionSwitchStatus = "1"
            })
            SpUtils.setValue(SpUtils.POWER_OPTIMIZATIONS, "1")
        } else if (TextUtils.equals(oldValue, "1") && !AppUtils.isIgnoringBatteryOptimizations()) {
            AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("12", "39").apply {
                functionStatus = "1"
                functionSwitchStatus = "0"
            })
            SpUtils.setValue(SpUtils.POWER_OPTIMIZATIONS, "0")
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.layoutPersonInfo.id -> {
                createTimingModeDialog(curValue)
            }
        }
    }

    //获取当前手机厂商值
    private fun initRomValue() {
        mModelData.add(Bean(0, getString(R.string.running_permission_xiaomi)))
        mModelData.add(Bean(1, getString(R.string.running_permission_huawei)))
        mModelData.add(Bean(2, getString(R.string.running_permission_oppo)))
        mModelData.add(Bean(3, getString(R.string.running_permission_vivo)))
        mModelData.add(Bean(4, getString(R.string.running_permission_one_plus)))
        mModelData.add(Bean(5, getString(R.string.running_permission_samsung)))
        mModelData.add(Bean(6, getString(R.string.running_permission_motorola)))
        mModelData.add(Bean(7, getString(R.string.running_permission_other_phone)))

        var mtype = Build.BRAND
        LogUtils.d("Build.BRAND -- " + Build.BRAND)
        mtype = mtype.lowercase(Locale.ENGLISH)
        curValue = if (mtype.startsWith("redmi") || mtype.startsWith("mi") || mtype.startsWith("xiaomi") || mtype.startsWith("poco")) {
            0
        } else if (mtype.startsWith("huawei") || mtype.startsWith("honor")) {
            1
        } else if (mtype.startsWith("oppo")) {
            2
        } else if (mtype.startsWith("vivo")) {
            3
        } else if (mtype.startsWith("oneplus")) {
            4
        } else if (mtype.startsWith("samsung")) {
            5
        } else if (mtype.startsWith("motorola")) {
            6
        } else if (mtype.startsWith("zte")) {
            7
        } else if (mtype.startsWith("f")) {
            7
        } else {
            7
        }
        setViewByType(curValue)
    }

    private fun createTimingModeDialog(defaultValue: Int) {
        val defaultPosition = mModelData.indexOfFirst { it.value == defaultValue }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            curValue = position
            setViewByType(curValue)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(mModelData)
        picker.setDefaultPosition(defaultPosition)
        picker.wheelLayout.setCyclicEnabled(true)
        picker.show()
    }

    /**
     * 根据厂商显示布局
     * */
    private fun setViewByType(value: Int) {
        mModelData.firstOrNull { it.value == value }?.let {
            binding.tvPhoneType.text = it.text
            binding.tvPhoneType2.text = it.text
        }
        binding.layoutPhone.removeAllViews()

        // phoneType: 0 xiaomi  1 huawei 2 oppo 3 vivo 4 其它
        when (value) {
            0 -> { //xiaomi
                addSettingItem(
                    getString(R.string.running_permission_power_saving),
                    getString(R.string.running_permission_unlimited),
                    getString(R.string.running_permission_boot_completed),
                    getString(R.string.running_permission_locking)
                )
            }
            1 -> { //huawei
                addSettingItem(
                    getString(R.string.running_permission_boot_completed),
                    getString(R.string.running_permission_locking),
                    getString(R.string.running_permission_power_management)
                )
            }
            2 -> { //oppo
                addSettingItem(
                    getString(R.string.running_permission_boot_completed),
                    getString(R.string.running_permission_locking),
                    getString(R.string.running_permission_power_consumption)
                )
            }
            3 -> { //vivo
                addSettingItem(
                    getString(R.string.running_permission_boot_completed),
                    getString(R.string.running_permission_locking),
                    getString(R.string.running_permission_power_consumption)
                )
            }
            4 -> { //oneplus
                addSettingItem(
                    getString(R.string.running_permission_locking),
                    getString(R.string.running_permission_power_management)
                )
            }
            5 -> { //samsung
                addSettingItem(
                    getString(R.string.running_permission_locking),
                    getString(R.string.running_permission_power_management)
                )
            }
            6 -> { //motorola
                addSettingItem(
                    getString(R.string.running_permission_power_management)
                )
            }
            7 -> { //其它
                addSettingItem(
                    getString(R.string.running_permission_other_set)
                )
            }
        }

    }

    private fun addSettingItem(vararg items: String) {
        for (i in items.indices) {
            val constraintLayout = inflater.inflate(R.layout.device_set_item, null)
            constraintLayout.findViewById<ImageView>(R.id.icon).visibility = View.GONE
            constraintLayout.findViewById<MySwitchCompat>(R.id.mSwitchCompat).visibility = View.GONE
            if (i == items.size - 1) constraintLayout.findViewById<View>(R.id.viewLine01).visibility = View.GONE
            val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
            tvName.text = items.get(i)
            constraintLayout.setPadding(ConvertUtils.dp2px(12f), 0, ConvertUtils.dp2px(12f), 0)
            tvName.setPadding(0, 0, ConvertUtils.dp2px(12f), 0)
            binding.layoutPhone.addView(constraintLayout)
            setViewsClickListener({
                // phoneType: 0 xiaomi  1 huawei 2 oppo 3 vivo 4 其它
                // module   : 1 关闭省电模式 2 选择后台“无限制” 3 允许应用自启动 4 后台进程锁定  5电源管理 6关闭耗电保护
                val intent = Intent(this, BackgroundDetailsActivity::class.java)
                intent.putExtra("phoneType", curValue)
                when (items[i]) {
                    getString(R.string.running_permission_power_saving) -> {
                        intent.putExtra("module", "1")
                    }
                    getString(R.string.running_permission_unlimited) -> {
                        intent.putExtra("module", "2")
                    }
                    getString(R.string.running_permission_boot_completed) -> {
                        intent.putExtra("module", "3")
                    }
                    getString(R.string.running_permission_locking) -> {
                        intent.putExtra("module", "4")
                    }
                    getString(R.string.running_permission_power_management) -> {
                        intent.putExtra("module", "5")
                    }
                    getString(R.string.running_permission_power_consumption) -> {
                        intent.putExtra("module", "6")
                    }
                    getString(R.string.running_permission_other_set) -> {
                        intent.putExtra("module", "7")
                    }
                }
                startActivity(intent)
            }, constraintLayout)
        }
    }


    class Bean(var value: Int, var text: String) : Serializable, TextProvider {
        override fun provideText(): String {
            return text
        }
    }

}