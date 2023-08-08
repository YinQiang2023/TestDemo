package com.smartwear.xzfit.https.response

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

data class GetUserInfoResponse(
    val userId: Long,
    val nikname: String,
    val height: String,
    val weight: String,
    val birthday: String,
    val sex: Byte,
    val head: String,
    val stepTarget: Int,
    val distanceTarget: Int,
    val consumeTarget: Int,
    val sleepTarget: Int,
    val skinColor: String,
    val unit: String
) {
    override fun toString(): String {
        return "GetUserInfoResponse(userId=$userId, nikname='$nikname', height='$height', weight='$weight', birthday='$birthday', sex=$sex, head='$head', stepTarget=$stepTarget, distanceTarget=$distanceTarget, consumeTarget=$consumeTarget, sleepTarget=$sleepTarget, skinColor='$skinColor', unit='$unit')"
    }
}