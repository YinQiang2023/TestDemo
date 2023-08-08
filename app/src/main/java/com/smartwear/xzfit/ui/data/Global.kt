package com.smartwear.xzfit.ui.data

import android.content.res.Resources
import android.util.Log
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.PathUtils
import com.zhapp.ble.bean.PhysiologicalCycleBean
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.db.model.Daily
import com.smartwear.xzfit.https.response.DeviceLanguageListResponse
import com.smartwear.xzfit.https.response.ProductListResponse
import com.smartwear.xzfit.ui.device.bean.DeviceSettingBean
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.healthy.bean.DragBean
import com.smartwear.xzfit.ui.healthy.bean.HealthyItemBean
import com.smartwear.xzfit.ui.livedata.RefreshHealthyFragment
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.utils.SendCmdUtils
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.viewmodel.DailyModel
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.smartwear.xzfit.viewmodel.EcgModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.litepal.LitePal

object Global {
    private val TAG = Global.javaClass.simpleName
    var healthyItemList: MutableList<HealthyItemBean> = mutableListOf()
    var editCardList = mutableListOf<DragBean>()

    private fun getIdentifier(name: String, defType: String): Int {
        val res = BaseApplication.mContext.resources
        return res.getIdentifier(name, defType, BaseApplication.mContext.packageName)
    }

    private fun noSaveDeviceMenuToAddItem() {
        val sex = UserBean().getData().sex
        val resources = BaseApplication.mContext.resources
        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(SpUtils.DEVICE_SETTING, ""),
            DeviceSettingBean::class.java
        )

        //步数
        if (deviceSettingBean!!.dataRelated.step_count) {
            //列表没有，后台已配置，需添加
            val bean = addEditCardItem(resources, "步数")
            editCardList.add(bean)
        }

        //距离
        if (deviceSettingBean!!.dataRelated.distance) {
            val bean = addEditCardItem(resources, "距离")
            editCardList.add(bean)
        }

        //卡路里
        if (deviceSettingBean!!.dataRelated.calories) {
            val bean = addEditCardItem(resources, "卡路里")
            editCardList.add(bean)
        }

        //心率
        if (deviceSettingBean!!.dataRelated.continuous_heart_rate) {
            val bean = addEditCardItem(resources, "心率")
            editCardList.add(bean)
        }

        //运动记录
        val sportIndex = editCardList.indexOfFirst { it.tag == "运动记录" }
        if (sportIndex == -1) {
            val bean = addEditCardItem(resources, "运动记录")
            editCardList.add(bean)
        }

        //睡眠
        val sleepIndex = editCardList.indexOfFirst { it.tag == "睡眠" }
        if (sleepIndex == -1) {
            val bean = addEditCardItem(resources, "睡眠")
            editCardList.add(bean)
        }

        //血氧
        if (deviceSettingBean!!.dataRelated.offline_blood_oxygen) {
            val bean = addEditCardItem(resources, "血氧")
            editCardList.add(bean)
        }

        //生理周期
        if (deviceSettingBean!!.dataRelated.menstrual_cycle && sex == "1") {
            val bean = addEditCardItem(resources, "生理周期")
            editCardList.add(bean)
        }

        //有效站立
        if (deviceSettingBean!!.dataRelated.effective_standing) {
            val bean = addEditCardItem(resources, "有效站立")
            editCardList.add(bean)
        }

        //心电
        if (deviceSettingBean!!.dataRelated.ecg) {
            val bean = addEditCardItem(resources, "心电")
            editCardList.add(bean)
        }

        //连续压力
        if (deviceSettingBean!!.dataRelated.pressure) {
            val bean = addEditCardItem(resources, "连续压力")
            editCardList.add(bean)
        }

        //离线压力
        if (deviceSettingBean!!.dataRelated.offline_pressure) {
            val bean = addEditCardItem(resources, "离线压力")
            editCardList.add(bean)
        }

        val editHideBean = DragBean()
        editHideBean.centerTextId = resources.getString(R.string.edit_card_hide_area)
        editHideBean.isTitle = true
        editCardList.add(editHideBean)

        for (i in editCardList.indices) {
            val data = editCardList[i]
            if (!data.isHide && !data.isTitle) {
                val bean = addHealthyItem(resources, data.tag)
                healthyItemList.add(bean)
            }
        }
    }

    /**
     * 填充数据
     */
    fun fillListData() {
        val temp = JSON.parseArray(SpUtils.getValue(SpUtils.EDIT_CARD_ITEM_LIST, ""), DragBean::class.java)
        healthyItemList.clear()
        editCardList.clear()
        var hasVersionInfo = SpUtils.getValue(SpUtils.DEVICE_SETTING, "").isNotEmpty()
        Log.i(TAG, "fillListData hasVersionInfo = " + hasVersionInfo)
        Log.i(TAG, "fillListData temp.isNullOrEmpty() = $temp")

        if (temp.isNullOrEmpty()) {
            if (hasVersionInfo) {
                noSaveDeviceMenuToAddItem()
            } else {
                noDataFillList()
            }
        } else {
            val resources = BaseApplication.mContext.resources
            val sex = UserBean().getData().sex
            for (i in temp.indices) {
                val bean = if (!temp[i].isTitle) {
                    addEditCardItem(resources, temp[i].tag, temp[i].isHide)
                } else {
                    val editHideBean = DragBean()
                    editHideBean.centerTextId = resources.getString(R.string.edit_card_hide_area)
                    editHideBean.isTitle = true
                    editHideBean
                }
                editCardList.add(bean)
            }


            if (hasVersionInfo) {
                deviceSettingBean = JSON.parseObject(
                    SpUtils.getValue(SpUtils.DEVICE_SETTING, ""),
                    DeviceSettingBean::class.java
                )
                var titleIndex = editCardList.indexOfFirst { it.isTitle }

                //步数
                val stepIndex = editCardList.indexOfFirst { it.tag == "步数" }
                if (stepIndex == -1 && deviceSettingBean!!.dataRelated.step_count) {
                    //列表没有，后台已配置，需添加
                    val bean = addEditCardItem(resources, "步数")
                    editCardList.add(titleIndex, bean)
                } else if (stepIndex != -1 && !deviceSettingBean!!.dataRelated.step_count) {
                    //列表有，后台配置没有，需取消
                    editCardList.removeAt(stepIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //距离
                val distanceIndex = editCardList.indexOfFirst { it.tag == "距离" }
                if (distanceIndex == -1 && deviceSettingBean!!.dataRelated.distance) {
                    val bean = addEditCardItem(resources, "距离")
                    editCardList.add(titleIndex, bean)
                } else if (distanceIndex != -1 && !deviceSettingBean!!.dataRelated.distance) {
                    editCardList.removeAt(distanceIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //卡路里
                val caloriesIndex = editCardList.indexOfFirst { it.tag == "卡路里" }
                if (caloriesIndex == -1 && deviceSettingBean!!.dataRelated.calories) {
                    val bean = addEditCardItem(resources, "卡路里")
                    editCardList.add(titleIndex, bean)
                } else if (caloriesIndex != -1 && !deviceSettingBean!!.dataRelated.calories) {
                    editCardList.removeAt(caloriesIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //心率
                val heartRateIndex = editCardList.indexOfFirst { it.tag == "心率" }
                if (heartRateIndex == -1 && deviceSettingBean!!.dataRelated.continuous_heart_rate) {
                    val bean = addEditCardItem(resources, "心率")
                    editCardList.add(titleIndex, bean)
                } else if (heartRateIndex != -1 && !deviceSettingBean!!.dataRelated.continuous_heart_rate) {
                    editCardList.removeAt(heartRateIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //运动记录
                val sportIndex = editCardList.indexOfFirst { it.tag == "运动记录" }
                if (sportIndex == -1) {
                    val bean = addEditCardItem(resources, "运动记录")
                    editCardList.add(titleIndex, bean)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //睡眠
                val sleepIndex = editCardList.indexOfFirst { it.tag == "睡眠" }
                if (sleepIndex == -1) {
                    val bean = addEditCardItem(resources, "睡眠")
                    editCardList.add(titleIndex, bean)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //血氧
                val bloodOxygenIndex = editCardList.indexOfFirst { it.tag == "血氧" }
                if (bloodOxygenIndex == -1 && deviceSettingBean!!.dataRelated.offline_blood_oxygen) {
                    val bean = addEditCardItem(resources, "血氧")
                    editCardList.add(titleIndex, bean)
                } else if (bloodOxygenIndex != -1 && !deviceSettingBean!!.dataRelated.offline_blood_oxygen) {
                    editCardList.removeAt(bloodOxygenIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //生理周期
                val menstrualCycleIndex = editCardList.indexOfFirst { it.tag == "生理周期" }
                if (menstrualCycleIndex == -1 && deviceSettingBean!!.dataRelated.menstrual_cycle && sex == "1") {
                    val bean = addEditCardItem(resources, "生理周期")
                    editCardList.add(titleIndex, bean)
                } else if (menstrualCycleIndex != -1 && !deviceSettingBean!!.dataRelated.menstrual_cycle) {
                    editCardList.removeAt(menstrualCycleIndex)
                } else if (menstrualCycleIndex != -1 && deviceSettingBean!!.dataRelated.menstrual_cycle && sex != "1") {
                    editCardList.removeAt(menstrualCycleIndex)
                }
                titleIndex = editCardList.indexOfFirst { it.isTitle }

                //有效站立
                val effectiveStandingIndex = editCardList.indexOfFirst { it.tag == "有效站立" }
                if (effectiveStandingIndex == -1 && deviceSettingBean!!.dataRelated.effective_standing) {
                    val bean = addEditCardItem(resources, "有效站立")
                    editCardList.add(titleIndex, bean)
                } else if (effectiveStandingIndex != -1 && !deviceSettingBean!!.dataRelated.effective_standing) {
                    editCardList.removeAt(effectiveStandingIndex)
                }

                //心电
                val ecgIndex = editCardList.indexOfFirst { it.tag == "心电" }
                if (ecgIndex == -1 && deviceSettingBean!!.dataRelated.ecg) {
                    val bean = addEditCardItem(resources, "心电")
                    editCardList.add(titleIndex, bean)
                } else if (ecgIndex != -1 && !deviceSettingBean!!.dataRelated.ecg) {
                    editCardList.removeAt(ecgIndex)
                }

                //连续压力
                val pressureIndex = editCardList.indexOfFirst { it.tag == "连续压力" }
                if (pressureIndex == -1 && deviceSettingBean!!.dataRelated.pressure) {
                    val bean = addEditCardItem(resources, "连续压力")
                    editCardList.add(titleIndex, bean)
                } else if (pressureIndex != -1 && !deviceSettingBean!!.dataRelated.pressure) {
                    editCardList.removeAt(pressureIndex)
                }

                //离线压力
                val offlinePressureIndex = editCardList.indexOfFirst { it.tag == "离线压力" }
                if (offlinePressureIndex == -1 && deviceSettingBean!!.dataRelated.offline_pressure) {
                    val bean = addEditCardItem(resources, "离线压力")
                    editCardList.add(titleIndex, bean)
                } else if (offlinePressureIndex != -1 && !deviceSettingBean!!.dataRelated.offline_pressure) {
                    editCardList.removeAt(offlinePressureIndex)
                }



                for (i in editCardList.indices) {
                    val data = editCardList[i]
                    if (!data.isHide && !data.isTitle) {
                        val bean = addHealthyItem(resources, data.tag)
                        healthyItemList.add(bean)
                    }
                }


            } else {
                for (i in editCardList.size - 1 downTo 0) {
                    val bean = editCardList[i]
                    Log.i(TAG, "bean.tag = $bean.tag")
                    when (bean.tag) {
                        "睡眠", "血氧", "生理周期", "有效站立", "心电", "连续压力", "离线压力" -> {
                            editCardList.remove(bean)
                        }
                    }
                }
                for (i in editCardList.indices) {
                    val data = editCardList[i]
                    Log.i(TAG, "data = $data")
                    if (!data.isHide && !data.isTitle) {
                        val bean = addHealthyItem(resources, data.tag)
                        healthyItemList.add(bean)
                    }
                }
            }
        }
        if (hasVersionInfo) {
            SpUtils.getSPUtilsInstance().remove(SpUtils.EDIT_CARD_ITEM_LIST)
            SpUtils.getSPUtilsInstance().remove(SpUtils.HEALTHY_SHOW_ITEM_LIST)
            SpUtils.setValue(SpUtils.EDIT_CARD_ITEM_LIST, JSON.toJSONString(editCardList))
            SpUtils.setValue(SpUtils.HEALTHY_SHOW_ITEM_LIST, JSON.toJSONString(healthyItemList))
        }
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SEX_CHANGE))
        RefreshHealthyFragment.postValue(true)
        SendCmdUtils.getSportData()
        EcgModel().updateHealthFragmentUi()
    }

    private fun addEditCardItem(
        resources: Resources,
        name: String,
        isHide: Boolean = false
    ): DragBean {
        val bean = DragBean()
        when (name) {
            "步数" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_step)
                bean.leftImg = getIdentifier("edit_card_item_step", "mipmap")
                bean.isHide = isHide
                bean.tag = "步数"
            }

            "距离" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_distance)
                bean.leftImg = getIdentifier("edit_card_item_distance", "mipmap")
                bean.isHide = isHide
                bean.tag = "距离"
            }

            "卡路里" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_calories)
                bean.leftImg = getIdentifier("edit_card_item_calories", "mipmap")
                bean.isHide = isHide
                bean.tag = "卡路里"
            }

            "睡眠" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_sleep)
                bean.leftImg = getIdentifier("edit_card_item_sleep", "mipmap")
                bean.isHide = isHide
                bean.tag = "睡眠"
            }

            "心率" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_heart)
                bean.leftImg = getIdentifier("edit_card_item_hr", "mipmap")
                bean.isHide = isHide
                bean.tag = "心率"
            }

            "血氧" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_blood_oxygen)
                bean.leftImg = getIdentifier("edit_card_item_blood_oxygen", "mipmap")
                bean.isHide = isHide
                bean.tag = "血氧"
            }

            "生理周期" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_women_health)
                bean.leftImg = getIdentifier("edit_card_item_women_health", "mipmap")
                bean.isHide = isHide
                bean.tag = "生理周期"
            }

            "有效站立" -> {
                bean.centerTextId =
                    resources.getString(R.string.healthy_sports_list_effective_stand)
                bean.leftImg = getIdentifier("edit_card_item_effective_stand", "mipmap")
                bean.isHide = isHide
                bean.tag = "有效站立"
            }

            "心电" -> {
                bean.centerTextId =
                    resources.getString(R.string.healthy_ecg_title)
                bean.leftImg = getIdentifier("edit_card_item_ecg", "mipmap")
                bean.isHide = isHide
                bean.tag = "心电"
                //Log.i(TAG, "addEditCardItem() = bean = $bean")
            }

            "连续压力" -> {
                bean.centerTextId =
                    resources.getString(R.string.healthy_pressure_title)
                bean.leftImg = getIdentifier("edit_card_item_pressure", "mipmap")
                bean.isHide = isHide
                bean.tag = "连续压力"
            }

            "离线压力" -> {
                bean.centerTextId =
                    resources.getString(R.string.healthy_pressure_title)
                bean.leftImg = getIdentifier("edit_card_item_pressure", "mipmap")
                bean.isHide = isHide
                bean.tag = "离线压力"
            }

            "运动记录" -> {
                bean.centerTextId = resources.getString(R.string.healthy_sports_list_sport_record)
                bean.leftImg = getIdentifier("edit_card_item_sport_record", "mipmap")
                bean.isHide = isHide
                bean.tag = "运动记录"
            }
        }
        return bean
    }

    private fun addHealthyItem(resources: Resources, name: String): HealthyItemBean {
        val bean = HealthyItemBean()
        when (name) {
            "步数" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_step)
                bean.topTitleImg = getIdentifier("healthy_item_step", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "步数"
            }

            "距离" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_distance)
                bean.topTitleImg = getIdentifier("healthy_item_distance", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "距离"
            }

            "卡路里" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_calories)
                bean.topTitleImg = getIdentifier("healthy_item_calories", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "卡路里"
            }

            "睡眠" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_sleep)
                bean.topTitleImg = getIdentifier("healthy_item_sleep", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "睡眠"
            }

            "心率" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_heart)
                bean.topTitleImg = getIdentifier("healthy_item_hr", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "心率"
            }

            "血氧" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_blood_oxygen)
                bean.topTitleImg = getIdentifier("healthy_item_blood_oxygen", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "血氧"
            }

            "生理周期" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_women_health)
                bean.topTitleImg = getIdentifier("healthy_item_women_health", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "生理周期"
            }

            "有效站立" -> {
                bean.topTitleText =
                    resources.getString(R.string.healthy_sports_list_effective_stand)
                bean.topTitleImg = getIdentifier("healthy_item_effective_stand", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "有效站立"
            }

            "心电" -> {
                bean.topTitleText = resources.getString(R.string.healthy_ecg_title)
                bean.topTitleImg = getIdentifier("healthy_item_ecg", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "心电"
            }

            "连续压力" -> {
                bean.topTitleText =
                    resources.getString(R.string.healthy_pressure_title)
                bean.topTitleImg = getIdentifier("edit_item_pressure", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "连续压力"
            }

            "离线压力" -> {
                bean.topTitleText =
                    resources.getString(R.string.healthy_pressure_title)
                bean.topTitleImg = getIdentifier("edit_item_offline_pressure", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "离线压力"
            }

            "运动记录" -> {
                bean.topTitleText = resources.getString(R.string.healthy_sports_list_sport_record)
                bean.topTitleImg = getIdentifier("healthy_item_sport_record", "mipmap")
                bean.bg = getIdentifier("public_bg", "drawable")
                bean.tag = "运动记录"
            }
        }
        return bean
    }


    fun noDataFillList() {
        healthyItemList.clear()
        editCardList.clear()
        val res = BaseApplication.mContext.resources
        val beanStep = HealthyItemBean()
        beanStep.topTitleText = res.getString(R.string.healthy_sports_list_step)
        beanStep.topTitleImg = res.getIdentifier("healthy_item_step", "mipmap", BaseApplication.mContext.packageName)
        beanStep.bg = res.getIdentifier("public_bg", "drawable", BaseApplication.mContext.packageName)
        beanStep.tag = "步数"
        healthyItemList.add(beanStep)

        val editBeanStep = DragBean()
        editBeanStep.centerTextId = res.getString(R.string.healthy_sports_list_step)
        editBeanStep.leftImg = res.getIdentifier("edit_card_item_step", "mipmap", BaseApplication.mContext.packageName)
        editBeanStep.isHide = false
        editBeanStep.tag = "步数"
        editCardList.add(editBeanStep)

        val beanDistance = HealthyItemBean()
        beanDistance.topTitleText = res.getString(R.string.healthy_sports_list_distance)
        beanDistance.topTitleImg = res.getIdentifier("healthy_item_distance", "mipmap", BaseApplication.mContext.packageName)
        beanDistance.bg = res.getIdentifier("public_bg", "drawable", BaseApplication.mContext.packageName)
        beanDistance.tag = "距离"
        healthyItemList.add(beanDistance)

        val editBeanDistance = DragBean()
        editBeanDistance.centerTextId = res.getString(R.string.healthy_sports_list_distance)
        editBeanDistance.leftImg = res.getIdentifier("edit_card_item_distance", "mipmap", BaseApplication.mContext.packageName)
        editBeanDistance.isHide = false
        editBeanDistance.tag = "距离"
        editCardList.add(editBeanDistance)

        val beanCalories = HealthyItemBean()
        beanCalories.topTitleText = res.getString(R.string.healthy_sports_list_calories)
        beanCalories.topTitleImg = res.getIdentifier("healthy_item_calories", "mipmap", BaseApplication.mContext.packageName)
        beanCalories.bg =
            res.getIdentifier("public_bg", "drawable", BaseApplication.mContext.packageName)
        beanCalories.tag = "卡路里"
        healthyItemList.add(beanCalories)

        val editBeanCalories = DragBean()
        editBeanCalories.centerTextId = res.getString(R.string.healthy_sports_list_calories)
        editBeanCalories.leftImg = res.getIdentifier(
            "edit_card_item_calories",
            "mipmap",
            BaseApplication.mContext.packageName
        )
        editBeanCalories.isHide = false
        editBeanCalories.tag = "卡路里"
        editCardList.add(editBeanCalories)

        val beanHeart = HealthyItemBean()
        beanHeart.topTitleText = res.getString(R.string.healthy_sports_list_heart)
        beanHeart.topTitleImg = res.getIdentifier("healthy_item_hr", "mipmap", BaseApplication.mContext.packageName)
        beanHeart.bg = res.getIdentifier("public_bg", "drawable", BaseApplication.mContext.packageName)
        beanHeart.tag = "心率"
        healthyItemList.add(beanHeart)

        val editBeanHeart = DragBean()
        editBeanHeart.centerTextId = res.getString(R.string.healthy_sports_list_heart)
        editBeanHeart.leftImg =
            res.getIdentifier("edit_card_item_hr", "mipmap", BaseApplication.mContext.packageName)
        editBeanHeart.isHide = false
        editBeanHeart.tag = "心率"
        editCardList.add(editBeanHeart)

        val sportsBean = HealthyItemBean()
        sportsBean.topTitleText = res.getString(R.string.healthy_sports_list_sport_record)
        sportsBean.topTitleImg = res.getIdentifier("healthy_item_sport_record", "mipmap", BaseApplication.mContext.packageName)
        sportsBean.bg = res.getIdentifier("public_bg", "drawable", BaseApplication.mContext.packageName)
        sportsBean.tag = "运动记录"
        healthyItemList.add(sportsBean)

        val sportsEditBean = DragBean()
        sportsEditBean.centerTextId = res.getString(R.string.healthy_sports_list_sport_record)
        sportsEditBean.leftImg = res.getIdentifier("edit_card_item_sport_record", "mipmap", BaseApplication.mContext.packageName)
        sportsEditBean.isHide = false
        sportsEditBean.tag = "运动记录"
        editCardList.add(sportsEditBean)

//            saveMainList()
        val editHideBean = DragBean()
        editHideBean.centerTextId = res.getString(R.string.edit_card_hide_area)
        editHideBean.isTitle = true
        editCardList.add(editHideBean)
//        RefreshHealthyFragment.postValue(true)
//        SendCmdUtils.getSportData()
//        saveMainList()
//        }
    }

    fun saveMainList() {
        SpUtils.getSPUtilsInstance().remove(SpUtils.EDIT_CARD_ITEM_LIST)
        SpUtils.getSPUtilsInstance().remove(SpUtils.HEALTHY_SHOW_ITEM_LIST)
        SpUtils.setValue(SpUtils.EDIT_CARD_ITEM_LIST, JSON.toJSONString(editCardList))
        SpUtils.setValue(SpUtils.HEALTHY_SHOW_ITEM_LIST, JSON.toJSONString(healthyItemList))
    }

    //APP启动时间
    var appStartTimestamp = 0L

    @JvmField
    var deviceType = ""

    @JvmField
    var deviceVersion = ""
    var deviceMac = ""
    var deviceSn = ""
    var deviceCapacity = 0
    var deviceSettingBean: DeviceSettingBean? = DeviceSettingBean()

    //产品设备号列表
    val productList = mutableListOf<ProductListResponse.Data>()
    var physiologicalCycleBean: PhysiologicalCycleBean? = null
    var dialDirection = true //表盘方向

    //生理周期距离安全期
    var womenHealthSumSafetyPeriod = 0

    fun checkDeviceType(deviceType: String): Boolean {
        val index = productList.indexOfFirst { it.deviceType == deviceType }
        return index != -1
    }

    //清除全局数据
    fun cleanData() {
        deviceType = ""
        deviceVersion = ""
        deviceMac = ""
        productList.clear()
    }

    //根据设备号获取图片url
    fun getDeviceImgUrl(deviceType: String): String {
        if (productList.size > 0) {
            for (i in productList.indices) {
                if (deviceType == productList[i].deviceType) {
                    return productList[i].homeLogo
                }
            }
        }
        return ""
    }

    const val REQUEST_DATA_TYPE_WEEK = 1
    const val REQUEST_DATA_TYPE_MONTH = 2
    private var scope = MainScope()

    fun startAppUpData() {
        scope.launch(Dispatchers.Unconfined) {
            val dailyList =
                LitePal.where("isUpLoad = 0 and userId = ?", SpUtils.getValue(SpUtils.USER_ID, ""))
                    .limit(5).find(Daily::class.java)
            if (dailyList.size > 0) {
                DailyModel().upLoadDaily(dailyList)
            }
            scope.cancel()
        }
    }


    //设备语言列表
    var deviceLanguageList: DeviceLanguageListResponse? = null

    //当前设备选中的语言  默认英语
    var deviceSelectLanguageId = 0
        get() {
            field = SpUtils.getSPUtilsInstance().getInt(SpUtils.DEVICE_SELECT_LANGUAGE_ID, 104)
            return field
        }
        set(value) {
            if (value > 0) {
                field = value
                SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_SELECT_LANGUAGE_ID, value)
            }
        }

    /**
     * 获取服务器默认语言列表
     * */
    fun getDevLanguage() {
        DeviceModel().queryLanguageList(0)
    }

    //登录冲突
    var IS_LOGIN_CONFLICT = false

    //是否绑定设备中
    var IS_BIND_DEVICE = false

    //是否启用设备中
    var IS_ENABLE_DEVICE = false

    //是否刷新数据中
    var IS_SYNCING_DATA = false

    //设备连接验证成功
    var IS_DEVICE_VERIFY = false

    //点击防抖动间隔
    const val SINGLE_CLICK_INTERVAL = 600L

    //定位偏移最小值
    const val GPS_OFFSET_DISTANCE_MIN = 5.0

    //定位偏移最大值
    const val GPS_OFFSET_DISTANCE_MAX = 300.0

    //按压进度条最大值
    const val PRESS_SEEKBAR_MAX = 50

    //按压进度条间隔
    const val PRESS_SEEKBAR_INTERVAL = 20L

    //APP运动 数据刷新间隔
    const val SPORT_REF_DATA_INTERVAL = 10 * 1000L

    //设备找手机超时时间
    const val FIND_PHONE_TIMEOUT = 30 * 1000L

    //设备找手机通知标识
    const val FIND_PHONE_NOTIFICATION_TAG = "FIND_PHONE_TAG"

    //来电自定义包名
    const val PACKAGE_CALL = "com.android.zhkj.call"

    //未接来电自定义包名
    const val PACKAGE_MISS_CALL = "com.android.zhkj.miss_call"

    //短信自定义包名
    const val PACKAGE_MMS = "com.android.zhkj.mms"

    //发送拍照广播，和相机库需要保持一致，不可以修改！
    const val TAG_SEND_PHOTO_ACTION = "TAG_SEND_PHOTO_ACTION"

    //相机退出广播
    const val TAG_CLOSE_PHOTO_ACTION = "TAG_CLOSE_PHOTO_ACTION"

    //自定义接受短信广播
    const val ACTION_NEW_SMS = "android.zhkj.action.NEW_SMS"

    //用于定位开关监听
    const val ACTION_PROVIDERS_CHANGED = "android.location.PROVIDERS_CHANGED"

    //自定义未接来电广播
    const val ACTION_CALLS_MISSED = "android.zhkj.action.CALLS_MISSED"

    //Gmail包名
    const val GMAIL_PACK_NAME = "com.google.android.gm"

    //region 运动数据类型
    const val SPORT_SINGLE_DATA_TYPE_TIME = 0x01
    const val SPORT_SINGLE_DATA_TYPE_DISTANCE = 0x02
    const val SPORT_SINGLE_DATA_TYPE_SPEED = 0x03
    const val SPORT_SINGLE_DATA_TYPE_MINKM = 0x04
    const val SPORT_SINGLE_DATA_TYPE_CALORIES = 0x05
    const val SPORT_SINGLE_DATA_TYPE_HEART_RATE = 0x06
    const val SPORT_SINGLE_DATA_TYPE_STEP_RATE = 0x07
    const val SPORT_SINGLE_DATA_TYPE_STEP_NUM = 0x08
    //endregion

    //region 提醒类型
    const val REMINDER_TYPE_SEDENTARY = 0x09
    const val REMINDER_TYPE_DRINK = 0x0a
    const val REMINDER_TYPE_PILLS = 0x0b
    const val REMINDER_TYPE_CLOCK = 0x0c
    const val REMINDER_TYPE_EVENT = 0x0d
    const val REMINDER_TYPE_HAND_WASH = 0x0e
    //endregion


    enum class DateType {
        TODAY, WEEK, MONTH
    }


    const val ONE_TYPE = "ONE_TYPE"
    const val ONE_TEXT_COLOR: Int = R.color.cycle_color_type1_text
    const val ONE_BG_COLOR: Int = R.color.cycle_color_type1_bg
    const val TWO_TYPE = "TWO_TYPE"
    const val TWO_TEXT_COLOR: Int = R.color.cycle_color_type2_text
    const val TWO_BG_COLOR: Int = R.color.cycle_color_type2_bg
    const val THREE_TYPE = "THREE_TYPE"
    const val THREE_TEXT_COLOR: Int = R.color.cycle_color_type3_text
    const val THREE_BG_COLOR: Int = R.color.cycle_color_type3_bg

    const val FOUR_TYPE = "FOUR_TYPE"

    fun getTextColor(type: String?): Int {
        var result = R.color.cycle_color_type0_text
        if (type == null || type == "") {
            return result
        }
        when (type) {
            ONE_TYPE -> {
                result = ONE_TEXT_COLOR
            }

            TWO_TYPE -> {
                result = TWO_TEXT_COLOR
            }

            THREE_TYPE -> {
                result = THREE_TEXT_COLOR
            }
        }
        return result
    }

    fun getBgColor(type: String?): Int {
        var result = R.color.transparent
        if (type == null || type == "") {
            return result
        }
        when (type) {
            ONE_TYPE -> {
                result = ONE_BG_COLOR
            }

            TWO_TYPE -> {
//                result = TWO_BG_COLOR
            }

            THREE_TYPE -> {
//                result = THREE_BG_COLOR
            }
        }
        return result
    }

    // 鲁班压缩 / 拍照 / 相册选取/ 裁剪 缓存路径
    val LUBAN_CACHE_DIR: String = PathUtils.getAppDataPathExternalFirst() + "/cache/imgCache/luban"

    //设备产品图路径
    val DEVICE_ICON_PATH: String = PathUtils.getAppDataPathExternalFirst() + "/cache/device_img.jpg"

    //region 后台返回设备固件平台分类
    /**
     * Nordic
     */
    @JvmField
    val FIRMWARE_PLATFORM_NORDIC = "0"
    /**
     * Realtek
     */
    @JvmField
    val FIRMWARE_PLATFORM_REALTEK = "1"
    /**
     * Dialog
     */
    @JvmField
    val FIRMWARE_PLATFORM_DIALOG = "2"
    /**
     * 炬芯
     */
    @JvmField
    val FIRMWARE_PLATFORM_JUXING = "3"
    /**
     * 思澈
     */
    @JvmField
    val FIRMWARE_PLATFORM_SIFLI = "4"
    /**
     * 杰理
     */
    @JvmField
    val FIRMWARE_PLATFORM_JIELI = "5"
    //endregion
}