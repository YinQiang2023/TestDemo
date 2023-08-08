package com.smartwear.xzfit.db.model

class Sleep : BaseData() {
    var userId: String = ""
    var startSleepTimestamp = "" //总的睡眠起始时间：入睡时间
    var endSleepTimestamp = "" //总的睡眠结束时间：起床时间
    var sleepDuration = "" //总的睡眠持续时间：睡眠时长
    var sleepScore = "" //睡眠分数
    var awakeTime = "" //总的清醒时长：
    var awakeTimePercentage = ""//总的清醒时长百分比：
    var lightSleepTime = "" //总的浅睡时长：
    var lightSleepTimePercentage = "" //总的浅睡时长百分比：
    var deepSleepTime = "" //总的深睡时长：
    var deepSleepTimePercentage = "" //总的深睡时长百分比：
    var rapidEyeMovementTime = "" //总的快速眼动时长：
    var rapidEyeMovementTimePercentage = "" //总的快速眼动时长百分比：
    var sleepDistributionDataList = "" //各个睡眠段落
}