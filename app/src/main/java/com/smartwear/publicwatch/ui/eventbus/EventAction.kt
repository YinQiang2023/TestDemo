package com.smartwear.publicwatch.ui.eventbus

object EventAction {
    //设备连接成功
    const val ACTION_DEVICE_CONNECTED = "ACTION_DEVICE_CONNECTED"

    //设备连接失败
    const val ACTION_DEVICE_CONNECT_FAIL = "ACTION_DEVICE_CONNECT_FAIL"

    //同步数据中状态
    const val ACTION_SYNCING_DATA = "ACTION_SYNCING_DATA"

    //重新同步数据
    const val ACTION_REF_SYNC = "ACTION_REF_SYNC"
    const val Action_DEVICE_OTHER_SYNC = "Action_DEVICE_OTHER_SYNC"

    //未绑定设备
    const val ACTION_NO_DEVICE_BINDING = "ACTION_NO_DEVICE_BINDING"

    //刷新健康页设备图
    const val ACTION_REFRESH_HEALTHY_PAGE_DEVICE_ICON = "ACTION_REFRESH_HEALTHY_PAGE_DEVICE_ICON"

    //刷新已绑定设备列表
    const val ACTION_REF_BIND_DEVICE = "REF_BIND_DEVICE"

    //设备蓝牙状态改变
    const val ACTION_DEVICE_BLE_STATUS_CHANGE = "DEVICE_BLE_STATUS_CHANGE"

    //网络连接上
    const val ACTION_NETWORK_CONNECTED = "ACTION_NETWORK_CONNECTED"

    //网络断连
    const val ACTION_NETWORK_DISCONNECTED = "ACTION_NETWORK_DISCONNECTED"

    //切换用户性别
    const val ACTION_SEX_CHANGE = "ACTION_SEX_CHANGE"

    //蓝牙状态改变
    const val ACTION_BLE_STATUS_CHANGE = "BLE_STATUS_CHANGE"

    //app通知列表权限改变
    const val ACTION_APP_NOTIFY_PERMISSION_CHANGE = "EVENT_APP_NOTIFY_PERMISSION_CHANGE"

    //系统通知列表权限改变
    const val ACTION_SYS_NOTIFY_PERMISSION_CHANGE = "EVENT_SYS_NOTIFY_PERMISSION_CHANGE"

    //手机音量变化
    const val ACTION_VOLUME_CHANGE = "EVENT_VOLUME_CHANGE"

    //未接来电
    const val ACTION_CALLS_MISSED = "EVENT_CALLS_MISSED"

    //短信
    const val ACTION_NEW_SMS = "EVENT_NEW_SMS"

    //设备找手机通知被用户取消
    const val ACTION_STOP_FIND_PHONE = "EVENT_STOP_FIND_PHONE"

    //分享运动数据
    const val ACTION_SHARE_SPORT_DATA = "EVENT_SHARE_SPORT_DATA"

    //设备快捷回复来电
    const val ACTION_REPLY_SEND_SMS = "EVENT_REPLY_SEND_SMS"

    //设备刷新支持产品列表
    const val ACTION_REF_DEVICE_SETTING = "EVENT_REF_DEVICE_SETTING"

    //用户设置地图选项改变
    const val ACTION_MAP_CHANGE = "EVENT_MAP_CHANGE"

    //应用前后台切换改变
    const val ACTION_APP_STATUS_CHANGE = "ACTION_APP_STATUS_CHANGE"

    //定位数据
    const val ACTION_LOCATION = "ACTION_LOCATION"

    //GPS信号变化
    const val ACTION_GPS_SATELLITE_CHANGE = "ACTION_GPS_SATELLITE_CHANGE"

    //设备请求辅助运动无权限
    const val ACTION_DEV_SPORT_NO_PERMISSION = "ACTION_DEV_SPORT_NO_PERMISSION"

    //设备请求辅助运动无权限
    const val ACTION_DEV_SPORT_NO_GPS = "ACTION_DEV_SPORT_NO_GPS"

    //刷新设备电池信息
    const val ACTION_REFRESH_BATTERY_INFO = "ACTION_REFRESH_BATTERY_INFO"

    //同步天气消息
    const val ACTION_SYNC_WEATHER_INFO = "ACTION_SYNC_WEATHER_INFO"

    //刷新健康页面步数，距离，卡路里数据
    const val ACTION_SYNC_DAILY_INFO = "ACTION_SYNC_DAILY_INFO"

    //更新目标设置刷新进度
    const val ACTION_UPDATE_TARGET_INFO = "ACTION_UPDATE_TARGET_INFO"

    //连接设备刷新数据完成 刷新AGPS
    const val ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE = "ACTION_FINISH_UPDATE_FOR_CONNECTED_DEVICE"

    //退出手动扫描自动绑定
    const val ACTION_QUIT_QR_SCAN_MANUAL_BIND = "ACTION_QUIT_SCAN_BIND"

    //发送短信无权限
    const val ACTION_SMS_NOT_PER = "ACTION_SMS_NOT_PER"

    //通话蓝牙配对
    const val ACTION_HEADSET_BOND = "ACTION_HEADSETBOND"

    //通话蓝牙配对失败
    const val ACTION_HEADSETBOND_FAILED = "ACTION_HEADSETBOND_FAILED"

    //响铃模式改变
    const val RINGER_MODE_CHANGED = "RINGER_MODE_CHANGED"

    //系统时间变化
    const val ACTION_TIME_CHANGED = "TIME_CHANGED"

    //绑定引导-触发开启设置通知总开关
    const val ACTION_GUIDE_NOTIFY_SWITCH = "ACTION_GUIDE_NOTIFY_SWITCH"

    //SIFLI DFU STATE 思澈ota状态
    const val ACTION_SIFLI_DFU_STATE = "ACTION_SIFLI_DFU_STATE"

    //SIFLI DFU PRO 思澈ota进度
    const val ACTION_SIFLI_DFU_PROGRESS = "ACTION_SIFLI_DFU_PROGRESS"

    //SIFLI DFU STATE 思澈表盘状态
    const val ACTION_SIFLI_FACE_STATE = "ACTION_SIFLI_FACE_STATE"

    //SIFLI DFU PRO 思澈表盘进度
    const val ACTION_SIFLI_FACE_PROGRESS = "ACTION_SIFLI_FACE_PROGRESS"

    //SIFLI 无可用服务
    const val ACTION_SIFLI_WITHOUT_SERVICE = "ACTION_SIFLI_FACE_PROGRESS"

    //设备请求辅助运动权限允许
    const val ACTION_DEV_SPORT_PERMISSION = "ACTION_DEV_SPORT_PERMISSION"

    //权限被拒绝
    const val ACTION_PERMISSION_DENIED = "ACTION_PERMISSION_DENIED"




}