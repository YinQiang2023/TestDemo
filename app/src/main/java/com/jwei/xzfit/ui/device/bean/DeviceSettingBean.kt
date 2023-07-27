package com.jwei.xzfit.ui.device.bean

class DeviceSettingBean {
    var pushLengthLimit: String = ""
    var bluetoothName: String = ""
    var deviceVersion: String = ""
    var deviceType: String = ""
    var reminderRelated = ReminderRelatedData() //提醒相关
    var settingsRelated = SettingsRelatedData()//设置相关
    var functionRelated = FunctionRelatedData()//功能相关
    var dataRelated = DataRelatedData() //数据相关

    class ReminderRelatedData {
        var notification = false//通知提醒
        var incoming_call_rejection = false//	来电拒接(android)
        var alarm_clock = false //	闹钟提醒
        var sedentary = false//	久坐提醒
        var drink_water = false //	喝水提醒
        var reminder_to_take_medicine = false //	吃药提醒
        var hand_washing_reminder = false  //洗手提醒
        var meeting = false//	会议提醒
        var event_reminder = false//	事件提醒
        var quick_reply = false //快捷回复
        var heart_rate_warning = false //心率预警
        override fun toString(): String {
            return "ReminderRelatedData(notification=$notification, incoming_call_rejection=$incoming_call_rejection, alarm_clock=$alarm_clock, sedentary=$sedentary, drink_water=$drink_water, reminder_to_take_medicine=$reminder_to_take_medicine, hand_washing_reminder=$hand_washing_reminder, meeting=$meeting, event_reminder=$event_reminder, quick_reply=$quick_reply, heart_rate_warning=$heart_rate_warning)"
        }
    }

    class SettingsRelatedData {
        var step_goal = false//	步数目标设置
        var calorie_goal = false//	卡路里目标设置
        var distance_target = false //	距离目标设置
        var sleep_goal = false //	睡眠目标设置
        var wearing_method = false //	佩戴方式设置
        var language = false //	语言设置
        var raise_your_wrist_to_brighten_the_screen = false //	抬腕亮屏开关设置
        var continuous_heart_rate_switch = false //	连续心率开关设置
        var continuous_blood_oxygen_switch = false //	连续血氧开关设置
        var continuous_body_temperature_switch = false//	连续体温开关设置
        var sleep_rapid_eye_movement_switch = false//	睡眠快速眼动开关设置
        var do_not_disturb = false//	勿扰设置
        var bright_adjustment = false //	亮度调节
        var vibration_adjustment = false //	震动调节
        var off_screen_display = false //	熄屏显示
        var bright_screen_time = false//	亮屏时长
        var cover_the_screen_off = false //	覆盖熄屏
        var double_click_to_brighten_the_screen = false //	双击亮屏
        var notification_does_not_turn_on = false //	通知不亮屏
        var application_list_sorting = false //设备应用列表排序
        var card_sort_list = false      //卡片排序
        var GoogleFit = false
        var Strava = false
        var sleep_mode_settings = false         //睡眠模式设置   定制 - InfoWear未使用
        var multi_sport_sorting = false         //多运动排序
        var device_disconnection_log = false    //设备断连日志
        var world_clock = false                 //世界时钟
        var familiar_with_not_turning_on_the_screen = false // 通知不亮屏幕
        var dev_trace_switch = false            //设备埋点日志数据
        var dial_installation_completed = false //表盘安装完成
        override fun toString(): String {
            return "SettingsRelatedData(step_goal=$step_goal, calorie_goal=$calorie_goal, distance_target=$distance_target, sleep_goal=$sleep_goal, wearing_method=$wearing_method, language=$language, raise_your_wrist_to_brighten_the_screen=$raise_your_wrist_to_brighten_the_screen, continuous_heart_rate_switch=$continuous_heart_rate_switch, continuous_blood_oxygen_switch=$continuous_blood_oxygen_switch, continuous_body_temperature_switch=$continuous_body_temperature_switch, sleep_rapid_eye_movement_switch=$sleep_rapid_eye_movement_switch, do_not_disturb=$do_not_disturb, bright_adjustment=$bright_adjustment, vibration_adjustment=$vibration_adjustment, off_screen_display=$off_screen_display, bright_screen_time=$bright_screen_time, cover_the_screen_off=$cover_the_screen_off, double_click_to_brighten_the_screen=$double_click_to_brighten_the_screen, notification_does_not_turn_on=$notification_does_not_turn_on, application_list_sorting=$application_list_sorting, card_sort_list=$card_sort_list, GoogleFit=$GoogleFit, Strava=$Strava, sleep_mode_settings=$sleep_mode_settings, multi_sport_sorting=$multi_sport_sorting, device_disconnection_log=$device_disconnection_log, world_clock=$world_clock, familiar_with_not_turning_on_the_screen=$familiar_with_not_turning_on_the_screen, dev_trace_switch=$dev_trace_switch, dial_installation_completed=$dial_installation_completed)"
        }


    }

    class FunctionRelatedData {
        var find_phone = false//	找手机功能
        var find_device = false//	找设备功能
        var shake_and_shake_to_take_pictures = false //	摇一摇拍照功能
        var music_control = false //	音乐控制功能(android)
        var dial = false//	表盘功能
        var contacts = false//	常用联系人
        var altitude = false //海拔（海平面大气压）
        var weather = false//	天气功能
        var secondary_screen_movement = false //	副屏运动功能
        var auxiliary_exercise = false //	辅助运动功能
        var AGPS = false //	AGPS功能
        var power_saving_mode = false //省电模式
        var binding_language = false //绑定语言
        override fun toString(): String {
            return "FunctionRelatedData(find_phone=$find_phone, find_device=$find_device, shake_and_shake_to_take_pictures=$shake_and_shake_to_take_pictures, music_control=$music_control, dial=$dial, contacts=$contacts, altitude=$altitude, weather=$weather, secondary_screen_movement=$secondary_screen_movement, auxiliary_exercise=$auxiliary_exercise, AGPS=$AGPS, power_saving_mode=$power_saving_mode, binding_language=$binding_language)"
        }

    }

    class DataRelatedData {
        var step_count = false //	步数数据
        var calories = false //	卡路里数据
        var distance = false//	距离数据
        var continuous_heart_rate = false //	连续心率数据
        var offline_heart_rate = false//	离线心率数据
        var continuous_body_temperature = false//	连续体温数据
        var offline_body_temperature = false //	离线体温数据
        var offline_blood_oxygen = false//	离线血氧数据
        var menstrual_cycle = false//	生理周期数据
        var effective_standing = false//	有效站立
        var ecg = false//	心电
        var blood_pressure = false//	血压
        var continuous_blood_oxygen = false//	连续血氧
        var pressure = false       //	连续压力 数据
        var continuous_pressure = false         //连续压力开关设置
        var offline_pressure = false //离线压力 数据
        override fun toString(): String {
            return "DataRelatedData(step_count=$step_count, calories=$calories, distance=$distance, continuous_heart_rate=$continuous_heart_rate, offline_heart_rate=$offline_heart_rate, continuous_body_temperature=$continuous_body_temperature, offline_body_temperature=$offline_body_temperature, offline_blood_oxygen=$offline_blood_oxygen, menstrual_cycle=$menstrual_cycle, effective_standing=$effective_standing, ecg=$ecg, blood_pressure=$blood_pressure, continuous_blood_oxygen=$continuous_blood_oxygen, pressure=$pressure, continuous_pressure=$continuous_pressure, offline_pressure=$offline_pressure)"
        }
    }

    override fun toString(): String {
        return "DeviceSettingBean(pushLengthLimit='$pushLengthLimit', bluetoothName='$bluetoothName', deviceVersion='$deviceVersion', deviceType='$deviceType', reminderRelated=$reminderRelated, settingsRelated=$settingsRelated, functionRelated=$functionRelated, dataRelated=$dataRelated)"
    }

}