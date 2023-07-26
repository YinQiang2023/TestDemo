package com.jwei.xzfit.ui.user.bean

import com.blankj.utilcode.util.GsonUtils
import com.jwei.xzfit.https.Response
import com.jwei.xzfit.https.response.GetUserInfoResponse
import com.jwei.xzfit.ui.user.utils.UnitConverUtils
import com.jwei.xzfit.utils.Constant
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.TextStringUtils

//userId	Long	用户id
//nikname	String	昵称
//height	String	身高
//weight	String	体重
//birthday	String	出生日期
//sex	Byte	性别（0：男 1：女）
//head	String	头像url
//stepTarget	Integer	步数目标（0-99999）单位步
//distanceTarget	Integer	距离目标
//consumeTarget	Integer	消耗目标
//sleepTarget	Integer	睡眠目标
//skinColor	String	肤色
//unit	String	单位

data class UserBean(
    var nickname: String = "",
    var height: String = "",
    var britishHeight: String = "",
    var weight: String = "",
    var britishWeight: String = "",
    var birthDate: String = "",
    var sex: String = "",
    var head: String = "",

//        var stepTarget: String = "",
//        var distanceTarget: String = "",
//        var consumeTarget: String = "",
//        var sleepTarget: String = "",
//        var skinColor: String = "",
//        var unit: String = ""
) {
    fun saveData(result: Response<GetUserInfoResponse>) {
        val userBean = UserBean(
            nickname = if (!TextStringUtils.isNull(result.data.nikname)) result.data.nikname else " ",
            height = if (!TextStringUtils.isNull(result.data.height)) result.data.height else Constant.HEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
            weight = if (!TextStringUtils.isNull(result.data.weight)) result.data.weight else Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString(),
            birthDate = if (!TextStringUtils.isNull(result.data.birthday)) result.data.birthday else Constant.BIRTHDAY_DEFAULT_VALUE,
            sex = if (!TextStringUtils.isNull(result.data.sex.toString())) result.data.sex.toString() else "0",
            head = if (!TextStringUtils.isNull(result.data.head)) result.data.head else "",
//                    ,
//                    unit = if (!TextStringUtils.isNull(result.data.unit)) result.data.unit else "0",
//                    stepTarget = if (!TextStringUtils.isNull(result.data.stepTarget.toString())) result.data.stepTarget.toString() else Constant.STEP_TARGET_DEFAULT_VALUE.toString(),
//                    distanceTarget = if (!TextStringUtils.isNull(result.data.distanceTarget.toString())) result.data.distanceTarget.toString() else Constant.CALORIE_TARGET_DEFAULT_VALUE.toString(),
//                    consumeTarget = if (!TextStringUtils.isNull(result.data.consumeTarget.toString())) result.data.consumeTarget.toString() else Constant.DISTANCE_TARGET_DEFAULT_VALUE.toString(),
//                    sleepTarget =if (!TextStringUtils.isNull(result.data.sleepTarget.toString())) result.data.sleepTarget.toString() else Constant.SLEEP_TARGET_DEFAULT_VALUE.toString(),
//                    birthday = if (!TextStringUtils.isNull(result.data.birthday)) result.data.birthday else Constant.BIRTHDAY_DEFAULT_VALUE,
//                    skinColor = ""
        )
        userBean.britishHeight = if (SpUtils.getBritishHeight().isNotEmpty()) {
            SpUtils.getBritishHeight()
        } else {
            UnitConverUtils.cmToInchString(userBean.height)
        }
        userBean.britishWeight = if (SpUtils.getBritishWeight().isNotEmpty()) {
            SpUtils.getBritishWeight()
        } else {
            UnitConverUtils.kGToLbString(userBean.weight)
        }
        saveData(userBean);
    }

//    fun inputData(user_bean: UserBean) {
//        SpUtils.setNickname(user_bean.nickname)
//        SpUtils.setHeight(user_bean.height)
//        SpUtils.setWeight(user_bean.weight)
//        SpUtils.setBirthday(user_bean.birthDate)
//        SpUtils.setSex(user_bean.sex)
//        SpUtils.setHead(user_bean.head)
//
//        SpUtils.setSportTarget(user_bean.stepTarget)
//        SpUtils.setSleepTarget(user_bean.sleepTarget)
//        SpUtils.setConsumeTarget(user_bean.consumeTarget)
//        SpUtils.setDistanceTarget(user_bean.distanceTarget)
//        SpUtils.setUnit(user_bean.unit)
//    }

    fun saveData(user_bean: UserBean) {
        com.blankj.utilcode.util.LogUtils.d("save user -->${GsonUtils.toJson(user_bean)}")
        SpUtils.setNickname(user_bean.nickname)
        SpUtils.setHeight(user_bean.height)
        SpUtils.setBritishHeight(user_bean.britishHeight)
        SpUtils.setWeight(user_bean.weight)
        SpUtils.setBritishWeight(user_bean.britishWeight)
        SpUtils.setBirthday(user_bean.birthDate)
        SpUtils.setSex(if (user_bean.sex == "1") "1" else "0")
        SpUtils.setHead(user_bean.head)
    }

    fun getData(): UserBean {
        return UserBean(
            nickname = SpUtils.getNickname(),
            height = SpUtils.getHeight(),
            britishHeight = SpUtils.getBritishHeight(),
            weight = SpUtils.getWeight(),
            britishWeight = SpUtils.getBritishWeight(),
            birthDate = SpUtils.getBirthday(),
            sex = SpUtils.getSex(),
            head = SpUtils.getHead(),
//                    ,
//                    unit = if (!TextStringUtils.isNull(result.data.unit)) result.data.unit else "0",
//                    stepTarget = if (!TextStringUtils.isNull(result.data.stepTarget.toString())) result.data.stepTarget.toString() else Constant.STEP_TARGET_DEFAULT_VALUE.toString(),
//                    distanceTarget = if (!TextStringUtils.isNull(result.data.distanceTarget.toString())) result.data.distanceTarget.toString() else Constant.CALORIE_TARGET_DEFAULT_VALUE.toString(),
//                    consumeTarget = if (!TextStringUtils.isNull(result.data.consumeTarget.toString())) result.data.consumeTarget.toString() else Constant.DISTANCE_TARGET_DEFAULT_VALUE.toString(),
//                    sleepTarget =if (!TextStringUtils.isNull(result.data.sleepTarget.toString())) result.data.sleepTarget.toString() else Constant.SLEEP_TARGET_DEFAULT_VALUE.toString(),
//                    birthday = if (!TextStringUtils.isNull(result.data.birthday)) result.data.birthday else Constant.BIRTHDAY_DEFAULT_VALUE,
//                    skinColor = ""
        )
    }

    fun clearData() {
        SpUtils.setNickname("")
        SpUtils.setHeight("")
        SpUtils.setBritishHeight("")
        SpUtils.setWeight("")
        SpUtils.setBritishWeight("")
        SpUtils.setBirthday("")
        SpUtils.setSex("")
        SpUtils.setHead("")
//        SpUtils.setSportTarget("")
//        SpUtils.setSleepTarget("")
//        SpUtils.setConsumeTarget("")
//        SpUtils.setDistanceTarget("")
//        SpUtils.setUnit("")
    }
}

