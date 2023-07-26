package com.jwei.xzfit.https

/**
 * Created by android
 * on 2021/8/10
 */
object HttpCommonAttributes {


    @kotlin.jvm.JvmField
    var authorization: String = ""

    //URL ---> accessManager.properties

    const val SERVER_ERROR = "-1"           //  	连接服务器异常！
    const val REQUEST_SUCCESS = "0000"           //  	操作成功！
    const val REQUEST_FAIL = "0001"              //  	操作失败！
    const val REQUEST_REGISTER_FAIL = "0002"     //	    注册失败！
    const val REQUEST_REGISTERED = "0003"       //            用户已注册！
    const val REQUEST_NOT_REGISTER = "0004"     //            用户未注册！
    const val REQUEST_PASSWORD_ERROR = "0005"  //         密码错误！
    const val REQUEST_CODE_INVALID = "0008" //已失效，请重新获取！
    const val REQUEST_CODE_ERROR = "0009" //验证码错误！
    const val REQUEST_PARAMS_NULL = "0010"  //         请求参数为空！

    const val REQUEST_SEND_CODE_ERROR = "0006" // 验证码下发失败！
    const val REQUEST_SEND_CODE_FREQUENTLY = "0007"  //           验证码获取频繁！
    const val REQUEST_SEND_CODE_NO_DATA = "0012"  //           没有数据
    const val LOGIN_OUT = "0030"  //           用户下线提醒
    const val DUPLICATE_BINDING = "1000"  //           用户下线提醒
    const val AUTHORIZATION_EXPIRED = "0020"  //           Authorization失效
    const val USER_LOGIN_OUT = "0"  //           用户主动退出登录
    const val USER_NAME_ERR = "0025" //         用户昵称违规


//    0011        请求数据有误！
//    0012        没有数据
//    0013        已申请添加好友！（关注取消双方，作废）
//    0014        好友已申请添加！（关注取消双方，作废）
//    0015        好友被删除（关注取消双方，作废）
//    0016        已经是好友（关注取消双方，作废）
//    0017        已关注好友
//    0018        关注好友已申请
//    0019        未申请好友关注
//    0020        Authorization失效！
//    0021        Authorization不能为空！
//    0022        接口重复请求！
//    0023        服务端处理异常！
}