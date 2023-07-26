package com.jwei.xzfit.db.model.track

import com.blankj.utilcode.util.TimeUtils
import java.util.*

/**
 * Created by Android on 2023/4/8.
 * 非数据库对象！！！ 数据库AppTrackingLog$errorLog内容对象
 * App异常埋点日志内容
 */
class TrackingLog {
    /*模型-开始
    时间戳

    模型-APP
    关联开始模型时间戳
    开始时间戳
    结束时间戳
    结果
    操作步骤

    模型-设备
    关联开始模型时间戳
    开始时间戳
    结束时间戳
    结果
    操作步骤
    接口名称

    模型-后台
    关联开始模型时间戳
    开始时间戳
    结束时间戳
    结果
    操作步骤
    接口名称
    请求json
    回调json

    模型-结束
    时间戳
    结果*/

    /**
     * 模型 - 必传 ： 开始，APP，设备，后台，结束
     */
    var type: String = ""

    /**
     * 步骤名称 - 必传
     */
    var step: String = ""

    /**
     * 开始时间
     */
    var startTime: String? = null

    /**
     * 日志内容 - 必传 可分行
     */
    var log: String? = null

    /**
     * 结束时间
     */
    var endTime: String? = null

    //region 设备
    /**
     * 设备方法名称
     */
    var devFunName: String? = null

    /**
     * 请求 ProtoBuf ID
     */
    var devReqProId: String? = null

    /**
     * 返回 ProtoBuf ID
     */
    var devResProId: String? = null

    /**
     * 设备回调结果
     */
    var devResult: String? = null
    //endregion

    //region 后台
    /**
     * 接口名称
     */
    var serName: String? = null

    /**
     * 请求接口url 粗略 eg：ffit/user/register
     */
    var serUrl: String? = null

    /**
     * 请求json
     */
    var serReqJson: String? = null

    /**
     * 回调json
     */
    var serResJson: String? = null

    /**
     * 请求结果
     */
    var serResult: String? = null

    /**
     * 状态码
     */
    var code:String? = null
    //endregion

    companion object TYPEINSTANCT {

        /**
         * 获取开始类型日志对象
         * @param name 事件名称
         */
        @JvmStatic
        fun getStartTypeTrack(stepName: String): TrackingLog {
            return TrackingLog().apply {
                type = "开始"
                step = "开始$stepName"
                startTime = getNowString()
            }
        }

        /**
         * 获取结束类型日志对象
         * @param name 事件名称
         */
        @JvmStatic
        fun getEndTypeTrack(stepName: String): TrackingLog {
            return TrackingLog().apply {
                type = "结束"
                step = "结束$stepName"
                startTime = getNowString()
            }
        }

        /**
         * 获取App类型日志对象
         */
        @JvmStatic
        fun getAppTypeTrack(stepName: String): TrackingLog {
            return TrackingLog().apply {
                type = "App"
                step = stepName
                startTime = getNowString()
            }
        }

        /**
         * 获取设备类型日志对象
         * @param stepName
         * @param devFuncationName
         * @param devRequestProtobuf
         */
        @JvmStatic
        fun getDevTyepTrack(stepName: String, devFuncationName: String, devRequestProtobuf: String,devResultProtobuf:String=""): TrackingLog {
            return TrackingLog().apply {
                type = "设备"
                step = stepName
                this.devFunName = devFuncationName
                this.devReqProId = devRequestProtobuf
                this.devResProId = devResultProtobuf.ifEmpty { devRequestProtobuf }
                startTime = getNowString()
            }
        }

        /**
         * 获取后台类型日志对象
         * @param stepName
         * @param serInterfaceName
         * @param serRequestUrl
         * @param serRequestJson
         */
        @JvmStatic
        fun getSerTypeTrack(stepName: String, serInterfaceName: String, serRequestUrl: String): TrackingLog {
            return TrackingLog().apply {
                type = "后台"
                step = stepName
                this.serName = serInterfaceName
                this.serUrl = serRequestUrl
                startTime = getNowString()
            }
        }

        /**
         * 获取当前时间
         */
        @JvmStatic
        fun getNowString(): String = TimeUtils.getNowString(com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS"))
    }

    override fun toString(): String {
        return "TrackingLog(type='$type', step='$step', startTime=$startTime, log=$log, endTime=$endTime, devFunName=$devFunName, devReqProId=$devReqProId, devResProId=$devResProId, devResult=$devResult, serName=$serName, serUrl=$serUrl, serReqJson=$serReqJson, serResJson=$serResJson, serResult=$serResult, code=$code)"
    }

}