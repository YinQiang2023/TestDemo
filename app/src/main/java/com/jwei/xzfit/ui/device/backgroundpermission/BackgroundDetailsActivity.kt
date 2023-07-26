package com.jwei.xzfit.ui.device.backgroundpermission

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ConvertUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.BackgroundDetailsActivityBinding
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.GlideApp
import com.jwei.xzfit.viewmodel.DeviceModel

/**
 * Created by Android on 2022/1/10.
 */
class BackgroundDetailsActivity : BaseActivity<BackgroundDetailsActivityBinding, DeviceModel>(
    BackgroundDetailsActivityBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private var module: String? = null
    private var phoneType = 4

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.running_permission_title)
        module = intent.getStringExtra("module")
        phoneType = intent.getIntExtra("phoneType", 4)
        if (module.isNullOrEmpty()) {
            finish()
        }
        if (module == "7") {
            binding.ivPicture.visibility = View.GONE
        }
        if (module == "4") {
            binding.btnToSet.visibility = View.GONE
        }
        setModuleName()
        setViewByPhoneType()
        setViewsClickListener(this, binding.btnToSet, binding.btnToSet2)
    }

    override fun initData() {
        super.initData()
        viewModel.kaImgs.observe(this) { ims ->
            if (ims.isNullOrEmpty()) {
                binding.ivPicture.visibility = View.GONE
                return@observe
            }
            AppUtils.tryBlock {
                GlideApp.with(this).load(ims[0]).into(binding.ivPicture)
                if (ims.size == 2) {
                    GlideApp.with(this).load(ims[1]).into(binding.ivPicture2)
                }
            }
        }
    }

    /**
     * 设置保活方式名称
     * */
    private fun setModuleName() {
        binding.tvModule.text = when (module!!) {
            "1" -> {
                getString(R.string.running_permission_power_saving)
            }
            "2" -> {
                getString(R.string.running_permission_unlimited)
            }
            "3" -> {
                getString(R.string.running_permission_boot_completed)
            }
            "4" -> {
                getString(R.string.running_permission_locking)
            }
            "5" -> {
                getString(R.string.running_permission_power_management)
            }
            "6" -> {
                getString(R.string.running_permission_power_consumption)
            }
            else -> {
                getString(R.string.running_permission_other_set)
            }
        }
    }

    /**
     * 根据手机型号，保活方式设置说明
     * */
    private fun setViewByPhoneType() {
        // phoneType: 0 xiaomi  1 huawei 2 oppo 3 vivo 4 其它
        // module   : 1 关闭省电模式 2 选择后台“无限制” 3 允许应用自启动 4 后台进程锁定  5电源管理 6关闭耗电保护
        when (phoneType) {
            0 -> { //xiaomi
                when (module!!) {
                    "1" -> {
                        setExplainViews(
                            getString(R.string.power_saving1),
                            getString(R.string.power_saving2),
                            getString(R.string.power_saving3)
                        )
                    }
                    "2" -> {
                        setExplainViews(
                            getString(R.string.unlimited1),
                            getString(R.string.unlimited2),
                            getString(R.string.unlimited3)
                        )
                    }
                    "3" -> {
                        setExplainViews(
                            getString(R.string.boot_completed_xiaomi1),
                            getString(R.string.boot_completed_xiaomi2),
                            getString(R.string.boot_completed_xiaomi3)
                        )
                    }
                    "4" -> {
                        setExplainViews(
                            getString(R.string.Locking_xiaomi1),
                            getString(R.string.Locking_xiaomi2)
                        )
                    }
                }
            }
            1 -> { //huawei
                when (module!!) {
                    "3" -> {
                        setExplainViews(
                            getString(R.string.boot_completed_huawei1),
                            getString(R.string.boot_completed_huawei2),
                        )

                        //鸿蒙os应用自启用
                        binding.llExplain2.visibility = View.VISIBLE
                        binding.ivPicture2.visibility = View.VISIBLE
                        binding.btnToSet2.visibility = View.VISIBLE
                        setExplainViews2(
                            getString(R.string.boot_completed_huawei3),
                            getString(R.string.boot_completed_huawei4),
                            getString(R.string.boot_completed_huawei5),
                            getString(R.string.boot_completed_huawei6)
                        )
                    }
                    "4" -> {
                        setExplainViews(
                            getString(R.string.Locking_huawei1),
                            getString(R.string.Locking_huawei2)
                        )
                    }
                    "5" -> {
                        setExplainViews(
                            getString(R.string.powermanagement1),
                            getString(R.string.powermanagement2),
                            getString(R.string.powermanagement3),
                            getString(R.string.powermanagement4)
                        )
                    }
                }
            }
            2 -> { //oppo
                when (module!!) {
                    "3" -> {
                        setExplainViews(
                            getString(R.string.boot_completed_oppo1),
                            getString(R.string.boot_completed_oppo2),
                            getString(R.string.boot_completed_oppo3)
                        )
                    }
                    "4" -> {
                        setExplainViews(
                            getString(R.string.Locking_oppo1),
                            getString(R.string.Locking_oppo2)
                        )
                    }
                    "6" -> {
                        setExplainViews(
                            getString(R.string.power_consumption_oppo1),
                            getString(R.string.power_consumption_oppo2),
                            getString(R.string.power_consumption_oppo3),
                            getString(R.string.power_consumption_oppo4)
                        )
                    }
                }
            }
            3 -> { //vivo
                when (module!!) {
                    "3" -> {
                        setExplainViews(
                            getString(R.string.boot_completed_vivo1),
                            getString(R.string.boot_completed_vivo2),
                            getString(R.string.boot_completed_vivo3),
                            getString(R.string.boot_completed_vivo4)
                        )
                    }
                    "4" -> {
                        setExplainViews(
                            getString(R.string.Locking_vivo1),
                            getString(R.string.Locking_vivo2)
                        )
                    }
                    "6" -> {
                        setExplainViews(
                            getString(R.string.power_consumption_vivo1),
                            getString(R.string.power_consumption_vivo2),
                            getString(R.string.power_consumption_vivo3)
                        )
                    }
                }
            }
            4 -> { //oneplus
                when (module!!) {
                    "5" -> {
                        setExplainViews(
                            getString(R.string.power_consumption_one_plus1),
                            getString(R.string.power_consumption_one_plus2),
                            getString(R.string.power_consumption_one_plus3)
                        )
                    }
                    "4" -> {
                        setExplainViews(
                            getString(R.string.locking_one_plus1),
                            getString(R.string.locking_one_plus2)
                        )
                    }
                }
            }
            5 -> { //samsung
                when (module!!) {
                    "4" -> {
                        setExplainViews(
                            getString(R.string.locking_samsung1),
                            getString(R.string.locking_samsung2)
                        )
                    }
                    "5" -> {
                        setExplainViews(
                            getString(R.string.power_consumption_samsung1),
                            getString(R.string.power_consumption_samsung2)
                        )
                    }

                }
            }
            6 -> { //motorola
                when (module!!) {
                    "5" -> {
                        setExplainViews(
                            getString(R.string.power_consumption_motorola1),
                            getString(R.string.power_consumption_motorola2)
                        )
                    }
                }
            }
            7 -> {
                setExplainViews(
                    getString(R.string.phone_other1),
                    getString(R.string.phone_other2),
                    getString(R.string.phone_other3)
                )
            }
        }
        if (module != "7") {
            viewModel.getBPDImageByPhoneType(phoneType + 1, module!!)
        }
    }

    /**
     * 设置说明
     * */
    private fun setExplainViews(vararg explain: String) {
        binding.llExplain.removeAllViews()
        var isSet = false
        if (explain.isNotEmpty()) {
            explain.forEach { e ->
                val tv = AppCompatTextView(this)
                tv.text = e
                tv.setTextColor(ContextCompat.getColor(this, R.color.color_878787))
                tv.textSize = 14f
                tv.setPadding(0, if (isSet) 0 else ConvertUtils.dp2px(12f), 0, 0)
                isSet = true
                binding.llExplain.addView(tv)
            }
        }
    }

    /**
     * 设置说明
     * 鸿蒙os应用自启用
     * */
    private fun setExplainViews2(vararg explain: String) {
        binding.llExplain2.removeAllViews()
        if (explain.isNotEmpty()) {
            explain.forEach { e ->
                val tv = AppCompatTextView(this)
                tv.text = e
                tv.setTextColor(ContextCompat.getColor(this, R.color.color_878787))
                tv.textSize = 14f
                binding.llExplain2.addView(tv)
            }
        }
    }

    /**
     * 点击去设置
     * */
    private fun goTiSetting() {
        // phoneType: 0 xiaomi  1 huawei 2 oppo 3 vivo 4 oneplus 5 samsung 6 motorola 7 其它
        // module   : 1 关闭省电模式 2 选择后台“无限制” 3 允许应用自启动 4 后台进程锁定  5电源管理 6关闭耗电保护
        val mtype = Build.BRAND // 手机品牌
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        var componentName: ComponentName? = null
        try {
            when (module!!) {
                "1" -> {
                    when (phoneType) {
                        0 -> { //xiaomi
                            componentName = ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerMainActivity")
                            intent.component = componentName
                            startActivity(intent)
                        }
                        else -> defSetting()
                    }
                }
                "2" -> {
                    when (phoneType) {
                        0 -> { //xiaomi
                            componentName = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                            intent.putExtra(
                                "package_label",
                                applicationContext.packageManager.getApplicationLabel(applicationContext.applicationInfo).toString()
                            )
                            intent.putExtra("package_name", applicationContext.packageName)
                            intent.component = componentName
                            startActivity(intent)
                        }
                        else -> defSetting()
                    }
                }
                "3" -> {
                    when (phoneType) {
                        0 -> { //xiaomi
                            componentName = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                        }
                        1 -> { //huawei
                            componentName = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.mainscreen.MainScreenActivity")
                        }
                        2 -> { //oppo
                            componentName = ComponentName("com.coloros.safecenter", "com.coloros.privacypermissionsentry.PermissionTopActivity")
                        }
                        3 -> { //vivo
                            componentName =
                                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                        }
                    }
                    if (mtype.startsWith("ZTE")) {
                        componentName = ComponentName("com.zte.heartyservice", "com.zte.heartyservice.autorun.AppAutoRunManager")
                    } else if (mtype.startsWith("F")) {
                        componentName = ComponentName("com.gionee.softmanager", "com.gionee.softmanager.oneclean.AutoStartMrgActivity")
                    }
                    intent.component = componentName
                    startActivity(intent)
                }
                /*"4" -> {
                    when (phoneType) {
                        0 -> { //xiaomi
                        }
                        1 -> { //huawei
                        }
                        2 -> { //oppo
                        }
                        3 -> { //vivo
                        }
                        else-> defSetting()
                    }
                }*/
                "5" -> {
                    when (phoneType) {
                        1 -> { //huawei
                            componentName = ComponentName("com.android.settings", "com.android.settings.Settings\$HighPowerApplicationsActivity")
                            intent.component = componentName
                            startActivity(intent)
                        }
                        4 ->{ //oneplus
                            com.blankj.utilcode.util.AppUtils.launchAppDetailsSettings()
                        }
                        5 ->{ //samsung
                            com.blankj.utilcode.util.AppUtils.launchAppDetailsSettings()
                        }
                        6 ->{ //motorola
                            com.blankj.utilcode.util.AppUtils.launchAppDetailsSettings()
                        }
                    }
                }
                "6" -> {
                    when (phoneType) {
                        2 -> { //oppo
                            componentName = ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
                        }
                        3 -> { //vivo
                            componentName = ComponentName("com.iqoo.powersaving", "com.iqoo.powersaving.PowerSavingManagerActivity")
                        }
                    }
                    intent.component = componentName
                    startActivity(intent)
                }
                else -> defSetting()
            }
        } catch (e: Exception) { //抛出异常就直接打开设置页面
            e.printStackTrace()
            defSetting()
        }
    }

    /**
     * 鸿蒙os应用自启用
     */
    private fun goTiSetting2() {
        val mtype = Build.BRAND // 手机品牌
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        var componentName: ComponentName? = null
        try {
            if (TextUtils.equals(module, "3") && phoneType == 1) {
                /*componentName = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                intent.component = componentName
                if(intent.resolveActivity(packageManager) == null){*/
                componentName = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                intent.component = componentName
                //}
                startActivity(intent)
            }
        } catch (e: Exception) { //抛出异常就直接打开设置页面
            e.printStackTrace()
            defSetting()
        }
    }

    private fun defSetting() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnToSet.id -> {
                goTiSetting()
            }
            binding.btnToSet2.id -> {
                goTiSetting2()
            }
        }
    }


}