package com.jwei.publicone.ui.sport.livedata

import androidx.lifecycle.MutableLiveData
import com.jwei.publicone.db.model.sport.SportModleInfo
import com.jwei.publicone.ui.sport.bean.SportDataBean
import com.jwei.publicone.utils.manager.DevSportManager

/**
 * Created by Android on 2021/10/11.
 * 运动全局数据
 */
class SportLiveData private constructor() {

    companion object {
        val instance = SingletonHolder.INSTANCE
    }

    private object SingletonHolder {
        val INSTANCE = SportLiveData()
    }

    init {
        //设备运动数据同步回调
        DevSportManager.initDevSportCallBack()
    }

    //APP运动类型
    private val appSportType = MutableLiveData(0)
    fun getAppSportType() = appSportType

    //运动耗时 ms
    private val sportTime = MutableLiveData(0L)
    fun getSportTime(): MutableLiveData<Long> {
        return sportTime
    }

    //运动距离 m
    private val sportDistance = MutableLiveData(0F)
    fun getSportDistance(): MutableLiveData<Float> {
        return sportDistance
    }

    //运动速度 km/h
    private val sportSpeed = MutableLiveData(0F)
    fun getSportSpeed(): MutableLiveData<Float> {
        return sportSpeed
    }

    //配速 0
    private val sportMinkm = MutableLiveData("00'00\"")
    fun getSportMinkm(): MutableLiveData<String> {
        return sportMinkm
    }

    //卡路里 kcal
    private val calories = MutableLiveData(0F)
    fun getCalories(): MutableLiveData<Float> {
        return calories
    }

    //运动中过程数据
    private val sportData = MutableLiveData(mutableListOf<MutableList<SportDataBean>>())
    fun getSportData() = sportData

    //当前运动数据
    private val sportModleInfo = MutableLiveData<SportModleInfo?>(null)
    fun getSportModleInfo(): MutableLiveData<SportModleInfo?> {
        return sportModleInfo
    }

    /**
     * 清除临时数据
     * */
    fun resetTempData() {
        sportTime.value = 0L
        sportDistance.value = 0F
        sportSpeed.value = 0F
        sportMinkm.value = "00'00\""
        calories.value = 0F
        sportData.value = sportData.value!!.apply { clear() }
    }

}