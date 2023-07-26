package com.jwei.xzfit.service;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Telephony;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.PhoneUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.VolumeUtils;
import com.google.gson.reflect.TypeToken;
import com.zh.ble.wear.protobuf.MusicProtos;
import com.zhapp.ble.bean.MusicInfoBean;
import com.zhapp.ble.parsing.ParsingStateManager;
import com.zhapp.ble.parsing.SendCmdState;
import com.jwei.xzfit.R;
import com.jwei.xzfit.base.BaseApplication;
import com.jwei.xzfit.db.model.track.TrackingLog;
import com.jwei.xzfit.dialog.DialogUtils;
import com.jwei.xzfit.receiver.MissedCallContentObserver;
import com.jwei.xzfit.receiver.PhoneReceiver;
import com.jwei.xzfit.receiver.SmsContentObserver;
import com.jwei.xzfit.ui.data.Global;
import com.jwei.xzfit.ui.device.bean.DeviceSettingBean;
import com.jwei.xzfit.ui.device.bean.NotifyItem;
import com.jwei.xzfit.ui.device.bean.PhoneDtoModel;
import com.jwei.xzfit.ui.eventbus.EventAction;
import com.jwei.xzfit.ui.eventbus.EventMessage;
import com.jwei.xzfit.utils.LogUtils;
import com.jwei.xzfit.utils.PhoneUtil;
import com.jwei.xzfit.utils.SpUtils;
import com.jwei.xzfit.utils.manager.AppTrackingManager;
import com.jwei.xzfit.utils.manager.MicroManager;
import com.jwei.xzfit.utils.manager.MusicSyncManager;
import com.zhapp.ble.ControlBleTools;
import com.zhapp.ble.callback.CallBackUtils;
import com.zhapp.ble.callback.CallStateCallBack;
import com.zhapp.ble.callback.MusicCallBack;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 接收系统通知/发送通知/音乐同步/来电监听/设备找手机通知/设备快捷回复（发短信）
 * 问题记录： 红米 K30 MyNotificationsService 没有正常启动，卸载重置后正常，现象为无法打开手机消息通知总开关
 */
public class MyNotificationsService extends NotificationListenerService {
    private final String TAG = MyNotificationsService.class.getSimpleName();

    public static Context context;

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    //产品功能列表
    private DeviceSettingBean mDeviceSettingBean = null;

    //允许通知列表
    private ArrayList<NotifyItem> appNotifyItems;
    //系统通知列表
    private ArrayList<NotifyItem> sysNotifyItems;

    private static NotificationManager notificationManager;

    //系统来电短信相关广播接受者
    private PhoneReceiver mPhoneReceiver;
    //是否注册来电短信内容
    private static boolean isRegisterContentObserverCall = false;
    private static boolean isRegisterContentObserverSms = false;

    private long OFFHOOK_LAST_TIME = 0;
    private long CALL_STATE_RINGING_LAST_TIME = 0;
    private long FindPhoneTime = 0;

    //120s之前的未接来电才有效
    private final static long MISS_CALL_INTERVAL = 2 * 60 * 1000L;
    //10s之前的短信才有效
    private final static long SMS_INTERVAL = 10 * 1000L;
    //上一次发送给设备的短信时间
    private static long mLastSmsDate = 0L;

    public MyNotificationsService() {
    }

    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        com.blankj.utilcode.util.LogUtils.d(TAG, "onCreate()");
        context = this;
        //重新开启NotificationMonitor
        toggleNotificationListenerService();
        //eventbus
        com.jwei.xzfit.utils.AppUtils.registerEventBus(this);
        //notificationManager
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //初始化通知开关列表
        initNotifyItems();
        //过滤通知
        initSkipNotify();
        //注册广播接受者
        registerReceiver();
        //刷新权限列表
        refNotifyEvent(new EventMessage(EventAction.ACTION_APP_NOTIFY_PERMISSION_CHANGE));
        //刷新产品列表
        initDeviceSetting();
        //设备处理电话相关
        initCallStateCallBack();
        //注册来电短信观察者
        initContentObserver();
        //音乐回调
        initMusicCallBack();
    }

    /**
     * 系统NLS通知服务是否可用
     * adb shell dumpsys notification
     * @return
     */
    private boolean isNotificationListenerServiceEnable(){
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(BaseApplication.application);
        if(packageNames.contains(BaseApplication.application.getPackageName())){
            return true;
        }
        return false;
    }

    //重新开启NotificationMonitor
    private void toggleNotificationListenerService() {
        ComponentName thisComponent = new ComponentName(this, MyNotificationsService.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    //产品列表
    private void initDeviceSetting() {
        mDeviceSettingBean = JSON.parseObject(
                SpUtils.INSTANCE.getValue(
                        SpUtils.DEVICE_SETTING,
                        ""
                ), DeviceSettingBean.class
        );
    }

    //通知开关列表
    private void initNotifyItems() {
        //获取APP通知列表
        String dataJson = SpUtils.INSTANCE.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER);
        if (!TextUtils.isEmpty(dataJson)) {
            appNotifyItems = GsonUtils.fromJson(dataJson, new TypeToken<ArrayList<NotifyItem>>() {
            }.getType());
        }
        //获取系统通知列表
        String sysDataJson = SpUtils.INSTANCE.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST);
        if (!TextUtils.isEmpty(sysDataJson)) {
            sysNotifyItems = GsonUtils.fromJson(sysDataJson, new TypeToken<ArrayList<NotifyItem>>() {
            }.getType());
        }
    }

    //初始化未接来电，短信观察者
    private void initContentObserver() {
        //未接来电
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            if (!isRegisterContentObserverCall) {
                LogUtils.d(TAG, "isRegisterContentObserverCall");
                isRegisterContentObserverCall = true;
                MissedCallContentObserver mMissedCallContentObserver = new MissedCallContentObserver(this.getApplicationContext(), new Handler());
                getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, false, mMissedCallContentObserver);
            }
        }

        //短信
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            if (!isRegisterContentObserverSms) {
                LogUtils.d(TAG, "isRegisterContentObserverSms");
                isRegisterContentObserverSms = true;
                SmsContentObserver mSmsContentObserver = new SmsContentObserver(this.getApplicationContext(), new Handler());
                getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, mSmsContentObserver);
            }
        }
    }

    /**
     * 注册广播
     */
    private void registerReceiver() {
        if (mPhoneReceiver == null) {
            com.blankj.utilcode.util.LogUtils.d(TAG, "registerPhoneReceiver");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PHONE_STATE");
            intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
            //音量变化
            intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
            //静音变化
            intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
            //短信
            intentFilter.addAction(Global.ACTION_NEW_SMS);
            //intentFilter.setPriority(Integer.MAX_VALUE);
            mPhoneReceiver = new PhoneReceiver(new PhoneReceiver.OnPhoneListener() {
                @Override
                public void onCallState(int state, String incomingNumber) {
                    LogUtils.d(TAG, "PhoneReceiver state = " + state + " incomingNumber = " + incomingNumber);
                    handlePhoneState(state, incomingNumber);
                }
            });
            registerReceiver(mPhoneReceiver, intentFilter);
        }
    }

    /**
     * 解注广播
     */
    private void unRegisterReceiver() {
        if (mPhoneReceiver != null) {
            unregisterReceiver(mPhoneReceiver);
        }
    }

    /**
     * EventBus
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refNotifyEvent(EventMessage event) {
        if (event == null) return;
        if (event.getAction().equals(EventAction.ACTION_TIME_CHANGED)) {
            //系统时间变化
            boolean isEnable = isNotificationListenerServiceEnable();
            //com.blankj.utilcode.util.LogUtils.d("通知监听服务NLS是否可用：" + isEnable);
            if(!isEnable){
                toggleNotificationListenerService();
            }
        }
        if (event.getAction().equals(EventAction.ACTION_REF_DEVICE_SETTING)) {
            initDeviceSetting();
        }
        if (event.getAction().equals(EventAction.ACTION_APP_NOTIFY_PERMISSION_CHANGE)) {
            //获取APP通知列表
            initNotifyItems();
        } else if (event.getAction().equals(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE)) {
            //获取系统通知列表
            initNotifyItems();
            //初始化内容监听者
            initContentObserver();
        } else if (event.getAction().equals(EventAction.ACTION_VOLUME_CHANGE)) {
            //LogUtils.d(TAG,"音乐 音量变化");
            int volume = VolumeUtils.getVolume(AudioManager.STREAM_MUSIC);
            com.blankj.utilcode.util.LogUtils.d("音量：" + volume);
            if (volume != MusicSyncManager.getInstance().getmCurrent()) {
                MusicSyncManager.getInstance().sendMusicData();
            }
        } else if (event.getAction().equals(EventAction.RINGER_MODE_CHANGED)) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            final int ringerMode = audioManager.getRingerMode();
            switch (ringerMode) {
                case AudioManager.RINGER_MODE_NORMAL:
                    //normal
                    com.blankj.utilcode.util.LogUtils.d("铃声");
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    //vibrate
                    com.blankj.utilcode.util.LogUtils.d("震动");
                    break;
                case AudioManager.RINGER_MODE_SILENT:
                    //silent
                    com.blankj.utilcode.util.LogUtils.d("静音");
                    break;
            }

        } else if (event.getAction().equals(EventAction.ACTION_CALLS_MISSED)) {

            if (event.getObj() instanceof PhoneDtoModel) {
                PhoneDtoModel phoneDtoModel = (PhoneDtoModel) event.getObj();
                String name = phoneDtoModel.getName();
                String number = phoneDtoModel.getTelPhone();
                long date = phoneDtoModel.getDate();
                if (checkPNisAllowNotify(Global.PACKAGE_MISS_CALL)) {
                    LogUtils.d(TAG, "未接来电--------->" + name + "," + number + "," + TimeUtils.millis2String(date, com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat(com.jwei.xzfit.utils.TimeUtils.DATEFORMAT_COMM)), true);

                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getStartTypeTrack("消息通知"), "", false, true);
                    TrackingLog log = TrackingLog.getAppTypeTrack("未接来电");
                    log.setLog("未接来电--------->" + name.length() + "," + number.length() + "," + TimeUtils.millis2String(date, com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat(com.jwei.xzfit.utils.TimeUtils.DATEFORMAT_COMM)) + " ; isconnect:" + ControlBleTools.getInstance().isConnect());
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, log, "", false, false);
                    if (Math.abs(System.currentTimeMillis() - date) > MISS_CALL_INTERVAL) {
                        com.jwei.xzfit.utils.LogUtils.d(TAG, "无效未接来电 > 120s", true);
                        if (ControlBleTools.getInstance().isConnect()) {
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getAppTypeTrack("重复异常拦截"), "", false, false);
                            TrackingLog errorLog = TrackingLog.getAppTypeTrack("未接来电120s前拦截");
                            errorLog.setLog("无效未接来电 > 120s:当前时间戳：" + System.currentTimeMillis() + ", 通知时间戳：" + date);
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, errorLog, "1713", true, false);
                        }
                        return;
                    }
                    if (ControlBleTools.getInstance().isConnect()) {
                        TrackingLog trackingLog = TrackingLog.getDevTyepTrack("发送通知至设备", "发送系统通知", "SEND_SYSTEM_NOTIFICATION", "");
                        trackingLog.setLog("number:" + (TextUtils.isEmpty(number) ? "" : number).length() + ",name:" + (TextUtils.isEmpty(name) ? "" : name).length());
                        ControlBleTools.getInstance().sendSystemNotification(1,
                                TextUtils.isEmpty(number) ? "" : number,
                                TextUtils.isEmpty(name) ? "" : name,
                                "", new ParsingStateManager.SendCmdStateListener() {
                                    @Override
                                    public void onState(SendCmdState state) {
                                        trackingLog.setEndTime(TrackingLog.getNowString());
                                        trackingLog.setDevResult("state : " + state);
                                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "", false, false);
                                        if (state == SendCmdState.SUCCEED) {
                                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getEndTypeTrack("消息通知"), "", true, false);
                                        } else {
                                            if (ControlBleTools.getInstance().isConnect())
                                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getAppTypeTrack("发送手机消息至设备失败或超时"), "1715", true, false);
                                        }
                                    }
                                });
                    }
                }
            }
        } else if (event.getAction().equals(EventAction.ACTION_NEW_SMS)) {
            com.blankj.utilcode.util.LogUtils.d(TAG, "ACTION_NEW_SMS");
            if (event.getObj() instanceof PhoneDtoModel) {
                PhoneDtoModel phoneDtoModel = (PhoneDtoModel) event.getObj();
                String name = phoneDtoModel.getName();
                String number = phoneDtoModel.getTelPhone();
                String context = phoneDtoModel.getSmsContext();
                long date = phoneDtoModel.getDate();
                if (checkPNisAllowNotify(Global.PACKAGE_MMS)) {
                    LogUtils.d(TAG, "发送短信通知--------->" + name + "," + number + "," + context, true);

                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getStartTypeTrack("消息通知"), "", false, true);
                    TrackingLog log = TrackingLog.getAppTypeTrack("短信通知");
                    log.setLog("短信通知--------->" + name.length() + ",number:" + number.length() + ",context:" + context.length() + ",date:" + date +
                            "; now:" + System.currentTimeMillis() + "; isConnect:" + ControlBleTools.getInstance().isConnect());
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, log, "", false, false);
                    if (Math.abs(System.currentTimeMillis() - date) > SMS_INTERVAL) {
                        LogUtils.d(TAG, "无效短信 - 与当前时间相差 10s", true);
                        if (ControlBleTools.getInstance().isConnect()) {
                            //通知拦截异常埋点上报
                            TrackingLog trackingLog = TrackingLog.getAppTypeTrack("短信消息10s前拦截");
                            trackingLog.setLog("无效短信 - 与当前时间相差 10s: last:" + mLastSmsDate + ", now：" + date);
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "1712", true, false);
                        }
                        return;
                    }
                    if (date != 0) {
                        if (mLastSmsDate == date) {
                            LogUtils.d(TAG, "无效短信 - 时间重复");
                            if (ControlBleTools.getInstance().isConnect()) {
                                //通知拦截异常埋点上报
                                TrackingLog trackingLog = TrackingLog.getAppTypeTrack("短信时间重复拦截");
                                trackingLog.setLog("重复异常拦截 无效短信 - 时间重复: last:" + mLastSmsDate + ", now：" + date);
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "1718", true, false);
                            }
                            return;
                        }
                        mLastSmsDate = date;
                    }
                    //发送短信通知
                    if (ControlBleTools.getInstance().isConnect()) {
                        TrackingLog trackingLog = TrackingLog.getDevTyepTrack("发送通知至设备", "发送系统通知", "SEND_SYSTEM_NOTIFICATION", "");
                        trackingLog.setLog("发送短信通知--------->name:" + name.length() + ",number:" + number.length() + ",context:" + context.length());
                        ControlBleTools.getInstance().sendSystemNotification(2,
                                TextUtils.isEmpty(number) ? "" : number,
                                TextUtils.isEmpty(name) ? "" : name,
                                TextUtils.isEmpty(context) ? "" : context,
                                new ParsingStateManager.SendCmdStateListener() {
                                    @Override
                                    public void onState(SendCmdState state) {
                                        trackingLog.setEndTime(TrackingLog.getNowString());
                                        trackingLog.setDevResult("state : " + state);
                                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "", false, false);
                                        if (state == SendCmdState.SUCCEED) {
                                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getEndTypeTrack("消息通知"), "", true, false);
                                        } else {
                                            if (ControlBleTools.getInstance().isConnect())
                                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getAppTypeTrack("发送手机消息至设备失败或超时"), "1715", true, false);
                                        }
                                    }
                                });
                    }
                }
            }
        } else if (event.getAction().equals(EventAction.ACTION_REPLY_SEND_SMS)) {
            PhoneUtil.endCall(BaseApplication.mContext);
            if (event.getObj() instanceof PhoneDtoModel) {
                PhoneDtoModel phoneDtoModel = (PhoneDtoModel) event.getObj();
                String number = phoneDtoModel.getTelPhone();
                String context = phoneDtoModel.getSmsContext();
                LogUtils.d(TAG, "设备来电快捷回复发送短信--------->" + number + "," + context, true);
                //发送短信
                if (TextUtils.isEmpty(number) || TextUtils.isEmpty(context)) {
                    //快捷回复未空
                    LogUtils.d(TAG, "快捷回复失败--->内容或者号码为空", true);
                    return;
                }
                if (!PhoneUtils.isSimCardReady()) {
                    LogUtils.d(TAG, "快捷回复失败--->SIM卡未准备好", true);
                    return;
                }
                try {
                    if (PermissionUtils.isGranted(Manifest.permission.SEND_SMS)) {
                        SmsManager manager = SmsManager.getDefault();
                        ArrayList<String> strings = manager.divideMessage(context);
                        for (int i = 0; i < strings.size(); i++) {
                            manager.sendTextMessage(number, null, context, null, null);
                        }
                    } else {
                        //没权限
                        LogUtils.d(TAG, "快捷回复失败--->没发送短信权限", true);
                        EventBus.getDefault().post(new EventMessage(EventAction.ACTION_SMS_NOT_PER, phoneDtoModel));
                    }
                } catch (Exception e) {
                    //发送异常
                    LogUtils.d(TAG, "快捷回复失败--->异常", true);
                    e.printStackTrace();
                }
            }
        } else if (event.getAction().equals(EventAction.ACTION_STOP_FIND_PHONE)) {
            LogUtils.d(TAG, "用户取消找手机通知");
            MicroManager.INSTANCE.findPhone(1);

        }
    }

    //region 设备处理来电相关

    private boolean isCallComing = false;

    /**
     * 处理打电话状态
     *
     * @param state
     * @param phoneNumber
     */
    void handlePhoneState(int state, String phoneNumber) {
        try {
            boolean isPushCall = true;
            if (checkPNisAllowNotify(Global.PACKAGE_CALL)) {
                String callName = PhoneUtil.getContactNameFromPhoneBook(BaseApplication.mContext, phoneNumber);
                switch (state) {
                    //来电
                    case TelephonyManager.CALL_STATE_RINGING:
                        //Log.d(TAG,"TelephonyManager: CALL_STATE ---> CALL_STATE_RINGING");
                        if (!isCallComing) {
                            isCallComing = true;
                            LogUtils.d(TAG, "来电 = phoneNumber = " + phoneNumber + "   callName = " + callName, true);

                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getStartTypeTrack("消息通知"), "", false, true);
                            TrackingLog log = TrackingLog.getAppTypeTrack("来电通知");
                            log.setLog("来电 = phoneNumber = " + phoneNumber.length() + "   callName = " + callName.length() + "; isConnect:" + ControlBleTools.getInstance().isConnect());
                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, log, "", false, false);

                            if ((Math.abs(System.currentTimeMillis() - CALL_STATE_RINGING_LAST_TIME) > 2000)) {
                                CALL_STATE_RINGING_LAST_TIME = System.currentTimeMillis();
                                if (ControlBleTools.getInstance().isConnect()) {
                                    TrackingLog trackingLog = TrackingLog.getDevTyepTrack("发送通知至设备", "发送系统通知", "SEND_SYSTEM_NOTIFICATION", "");
                                    //trackingLog.setLog("来电 = phoneNumber = " + phoneNumber + "   callName = " + callName);
                                    ControlBleTools.getInstance().sendSystemNotification(0,
                                            TextUtils.isEmpty(phoneNumber) ? "" : phoneNumber,
                                            TextUtils.isEmpty(callName) ? "" : callName,
                                            "",
                                            new ParsingStateManager.SendCmdStateListener() {
                                                @Override
                                                public void onState(SendCmdState state) {
                                                    trackingLog.setEndTime(TrackingLog.getNowString());
                                                    trackingLog.setDevResult("state : " + state);
                                                    if (state == SendCmdState.SUCCEED) {
                                                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "", true, false);
                                                    } else {
                                                        if (ControlBleTools.getInstance().isConnect()) {
                                                            trackingLog.setLog(trackingLog.getLog() + "\n发送手机消息至设备失败或超时");
                                                            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "1715", true, false);
                                                        }
                                                    }
                                                }
                                            });
                                }
                            } else {
                                if (ControlBleTools.getInstance().isConnect()) {
                                    TrackingLog trackingLog = TrackingLog.getAppTypeTrack("来电2s内重复拦截");
                                    trackingLog.setLog("当前时间戳：" + System.currentTimeMillis() + ", 上一次通知时间戳：" + CALL_STATE_RINGING_LAST_TIME);
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "1714", true, false);
                                }
                            }
                        }
                        break;
                    //闲置状态
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Log.d(TAG,"TelephonyManager: CALL_STATE ---> CALL_STATE_IDLE");
                        isCallComing = false;
                        if (!TextUtils.isEmpty(phoneNumber)) {
                            LogUtils.d(TAG, "挂电话 ---> phoneNumber = " + phoneNumber + "   callName = " + callName, true);
                            if (Math.abs(System.currentTimeMillis() - OFFHOOK_LAST_TIME) > 2000) {
                                ControlBleTools.getInstance().sendCallState(1, null);
                                //PhoneUtil.endCall(BaseApplication.mContext);
                            }
                            OFFHOOK_LAST_TIME = System.currentTimeMillis();
                        }
                        ThreadUtils.runOnUiThreadDelayed(() -> {
                            PhoneUtil.recoverRingerMute(BaseApplication.mContext);
                        }, 1000);
                        break;
                    // 接电话了！
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        isCallComing = false;
                        Log.d(TAG, "TelephonyManager: CALL_STATE ---> CALL_STATE_OFFHOOK");
                        if (!TextUtils.isEmpty(phoneNumber)) {
                            if (Math.abs(System.currentTimeMillis() - OFFHOOK_LAST_TIME) > 2000) {
                                LogUtils.d(TAG, "接电话 ---> phoneNumber = " + phoneNumber + "   callName = " + callName, true);
                                ControlBleTools.getInstance().sendCallState(0, null);
                            }
                            OFFHOOK_LAST_TIME = System.currentTimeMillis();
                        }
                        ThreadUtils.runOnUiThreadDelayed(() -> {
                            PhoneUtil.recoverRingerMute(BaseApplication.mContext);
                        }, 1000);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设备处理来电状态
     */
    private void initCallStateCallBack() {
        CallBackUtils.callStateCallBack = new CallStateCallBack() {
            @Override
            public void onState(int state) {
                try {
                    switch (state) {
                        case 0:
                            LogUtils.e(TAG, "设备-结束电话");
                            //设备无此功能
                            //PhoneUtil.acceptRingingCall(BaseApplication.mContext);
                            //恢复静音
                            ThreadUtils.runOnUiThreadDelayed(() -> {
                                PhoneUtil.recoverRingerMute(BaseApplication.mContext);
                            }, 1000);
                            break;
                        case 1:
                            LogUtils.e(TAG, "设备-挂电话", true);
                            //恢复静音
                            ThreadUtils.runOnUiThreadDelayed(() -> {
                                PhoneUtil.recoverRingerMute(BaseApplication.mContext);
                            }, 1000);
                            if (mDeviceSettingBean != null && !mDeviceSettingBean.getReminderRelated().getIncoming_call_rejection()) {
                                //未启用来电拒接功能
                                com.jwei.xzfit.utils.LogUtils.e("endCall", "设备不支持来电拒接功能！！！！", true);
                                break;
                            }
                            PhoneUtil.endCall(BaseApplication.mContext);
                            break;
                        case 2:
                            LogUtils.e(TAG, "设备-来电静音", true);
                            if (isNotificationPolicyAccessGranted()) {
                                PhoneUtil.toggleRingerMute(BaseApplication.mContext);
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
    //endregion

    // region 音乐相关
//    private long lastMusicCmdTime = 0L;

    private void initMusicCallBack() {
        MusicSyncManager.getInstance().attachedNotificationListenerService(this);

        CallBackUtils.musicCallBack = new MusicCallBack() {
            @Override
            public void onRequestMusic() {
                LogUtils.d(TAG, "onRequestMusic = 收到设备获取音乐信息");
                if (mDeviceSettingBean != null && !mDeviceSettingBean.getFunctionRelated().getMusic_control()) {
                    //无权限
                    ControlBleTools.getInstance().syncMusicInfo(new MusicInfoBean(1, "", 0, 0), null);
                    com.jwei.xzfit.utils.LogUtils.e("onSendMusicCmd", "设备不支持音乐控制功能！！！！", true);
                    return;
                }
                if (!MyNotificationsService.isEnabled(context)) {
                    //无权限
                    ControlBleTools.getInstance().syncMusicInfo(new MusicInfoBean(1, "", 0, 0), null);
                    return;
                }
                MusicSyncManager.getInstance().deviceMusicQuit(false);
                MusicSyncManager.getInstance().clearLastData();
                MusicSyncManager.getInstance().getMusicInfo();
            }

            @Override
            public void onSyncMusic(int errorCode) {
                LogUtils.d(TAG, "onSyncMusic" + errorCode);
            }

            @Override
            public void onQuitMusic() {
                LogUtils.d(TAG, "onQuitMusic");
                MusicSyncManager.getInstance().deviceMusicQuit(true);
            }

            /**
             * 收到设备发过来的控制指令
             * @param command
             */
            @Override
            public void onSendMusicCmd(int command) {
                if (mDeviceSettingBean != null && !mDeviceSettingBean.getFunctionRelated().getMusic_control()) {
                    com.jwei.xzfit.utils.LogUtils.e("onSendMusicCmd", "设备不支持音乐控制功能！！！！", true);
                    return;
                }
                LogUtils.d(TAG, "音乐 onSendMusicCmd 收到设备的指令 = " + command, true);
                boolean isMediacontrolSuccess = false;
                switch (command) {
                    case MusicProtos.SEPlayerControlCommand.PLAYING_VALUE:
                        isMediacontrolSuccess = MusicSyncManager.getInstance().play();
                        com.blankj.utilcode.util.LogUtils.i("播放 " + isMediacontrolSuccess);
                        break;
                    case MusicProtos.SEPlayerControlCommand.PAUSE_VALUE:
                        isMediacontrolSuccess = MusicSyncManager.getInstance().pause();
                        com.blankj.utilcode.util.LogUtils.i("暂停 " + isMediacontrolSuccess);
                        break;
                    case MusicProtos.SEPlayerControlCommand.PREV_VALUE:
                        isMediacontrolSuccess = MusicSyncManager.getInstance().previous();
                        com.blankj.utilcode.util.LogUtils.i("上一曲 " + isMediacontrolSuccess);
                        break;
                    case MusicProtos.SEPlayerControlCommand.NEXT_VALUE:
                        isMediacontrolSuccess = MusicSyncManager.getInstance().next();
                        com.blankj.utilcode.util.LogUtils.i("下一曲 " + isMediacontrolSuccess);
                        break;
                    case MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_UP_VALUE:
                        //提高音量
                        com.blankj.utilcode.util.LogUtils.i("增加音量 ");
                        MusicSyncManager.getInstance().controlVolume(BaseApplication.mContext, AudioManager.ADJUST_RAISE);
                        break;
                    case MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_DOWN_VALUE:
                        //减小音量
                        com.blankj.utilcode.util.LogUtils.i("减小音量 ");
                        MusicSyncManager.getInstance().controlVolume(BaseApplication.mContext, AudioManager.ADJUST_LOWER);
                        break;
                }
            }
        };
    }
    //endregion

    //region 权限相关
    /**
     * 是否需要显示通知访问权限弹窗
     */
    public static boolean isShowingRequestNotification = false;

    /**
     * 访问通知权限
     */
    public static boolean checkNotificationIsEnable(Context context) {
        if (!MyNotificationsService.isEnabled(context)) {
            isShowingRequestNotification = true;
            StringBuilder msg = new StringBuilder();
            msg.append(context.getString(R.string.open_notify_hint));
            if (ActivityUtils.getTopActivity() != null) {
                DialogUtils.INSTANCE.showDialogTitleAndOneButton(
                        ActivityUtils.getTopActivity(),
                        /*context.getString(R.string.dialog_title_tips)*/null,
                        msg.toString(),
                        context.getString(R.string.know),
                        new DialogUtils.DialogClickListener() {
                            @Override
                            public void OnOK() {
                                MyNotificationsService.openNotificationAccess(context);
                                isShowingRequestNotification = false;
                            }

                            @Override
                            public void OnCancel() {
                                isShowingRequestNotification = false;
                            }
                        });
            }
            return false;
        }
        return true;
    }

    /**
     * 访问通知权限
     */
    public static boolean checkNotificationIsEnable(Activity context, int activityRequestCode) {
        if (!MyNotificationsService.isEnabled(context)) {
            isShowingRequestNotification = true;
            StringBuilder msg = new StringBuilder();
            msg.append(context.getString(R.string.open_notify_hint));
            if (ActivityUtils.getTopActivity() != null) {
                DialogUtils.INSTANCE.showDialogTitleAndOneButton(
                        ActivityUtils.getTopActivity(),
                        /*context.getString(R.string.dialog_title_tips)*/null,
                        msg.toString(),
                        context.getString(R.string.know),
                        new DialogUtils.DialogClickListener() {
                            @Override
                            public void OnOK() {
                                MyNotificationsService.openNotificationAccess(context, activityRequestCode);
                                isShowingRequestNotification = false;
                            }

                            @Override
                            public void OnCancel() {
                                isShowingRequestNotification = false;
                            }
                        });
            }
            return false;
        }
        return true;
    }

    /**
     * 是否获取访问通知权限
     *
     * @return true
     */
    public static boolean isEnabled(Context context) {
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 打开访问通知设置
     */
    public static void openNotificationAccess(Context context) {
        final String ACTION_NOTIFICATION_LISTENER_SETTINGS;
        if (Build.VERSION.SDK_INT >= 22) {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
        } else {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }
        context.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    /**
     * 打开访问通知设置
     */
    public static void openNotificationAccess(Activity context, int code) {
        final String ACTION_NOTIFICATION_LISTENER_SETTINGS;
        if (Build.VERSION.SDK_INT >= 22) {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
        } else {
            ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }
        context.startActivityForResult(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS), code);
    }

    /**
     * 检测并提示用户允许应用打开免打扰模式
     */
    public static boolean checkNotificationPolicyAccessGranted(Context context) {
        if (!MyNotificationsService.isNotificationPolicyAccessGranted()) {
            StringBuilder msg = new StringBuilder();
            msg.append(context.getString(R.string.open_notify_donotdisturb_hint));
            DialogUtils.INSTANCE.showDialogTitleAndOneButton(
                    ActivityUtils.getTopActivity(),
                    /*getString(R.string.dialog_title_tips)*/null,
                    msg.toString(),
                    context.getString(R.string.know),
                    new DialogUtils.DialogClickListener() {
                        @Override
                        public void OnOK() {
                            MyNotificationsService.openDoNotDisturb(context);
                        }

                        @Override
                        public void OnCancel() {

                        }
                    });
            return false;
        }
        return true;
    }

    /**
     * 是否开启了允许应用打开 免打扰模式
     */
    public static boolean isNotificationPolicyAccessGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager == null) notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                com.blankj.utilcode.util.LogUtils.d("isNotificationPolicyAccessGranted " + notificationManager.isNotificationPolicyAccessGranted());
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * 打开免打扰模式设置
     */
    public static void openDoNotDisturb(Context context) {
        if (notificationManager == null) notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            context.startActivity(intent);
        }
    }

    /**
     * 重置短信观察者初始化
     */
    public static void setIsRegisterContentObserverSms(boolean flag) {
        isRegisterContentObserverSms = false;
    }
    //endregion

    //region 通知相关
    private int OutTime = 3 * 1000;
    private long latestSendTime = 0;
    private String latestPageageName = "";
    private String latestMessage = "";
    private Map<Integer, Long> messageMap = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null) return;

            String packageName = "";
            String appName = "";

            CharSequence extraTitle = null;
            CharSequence extraText = null;
            CharSequence extraBigText = null;
            CharSequence tickerText = null;
            try {
                int progress = notification.extras.getInt(Notification.EXTRA_PROGRESS, 0);
                int progressMax = notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
                if (progressMax != 0 && progress != 0 && (progress != progressMax)) return;

                packageName = sbn.getPackageName();
                appName = AppUtils.getAppName(packageName);
                tickerText = notification.tickerText;
                extraTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                extraText = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
                extraBigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

                if (messageMap.containsKey(sbn.getId())) {
                    Long lastWhen = messageMap.get(sbn.getId());
                    if (lastWhen != null && lastWhen == notification.when) {
                        LogUtils.d(TAG, "app通知时间重复拦截" + ",接收到的通知栏消息 -->\n"
                                + "app name     -->" + appName
                                + " package name -->" + packageName
                                + " title        -->" + (!TextUtils.isEmpty(extraTitle) ? extraTitle.toString() : "")
                                + " text         -->" + (extraText != null ? extraText.toString() : "")
                                + " ticker text  -->" + (tickerText != null ? tickerText.toString() : ""));
                        return;
                    }
                }

                if (!TextUtils.isEmpty(extraBigText)) {
                    if (!TextUtils.isEmpty(extraTitle)) {
                        extraText = extraBigText.toString().trim().replace(extraTitle, " ");
                    } else {
                        extraText = extraBigText.toString().trim();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //region 辅助音乐标题
            //本地提前声明的音乐包名
            if (MusicSyncManager.getInstance().isMusicPackageName(packageName)) {
                LogUtils.i(TAG, "Media music title:" + extraText + ":" + extraTitle + ":" + tickerText);
                CharSequence title = null;
                if (TextUtils.isEmpty(title)) title = tickerText;
                if (TextUtils.isEmpty(title)) title = extraTitle;
                if (TextUtils.isEmpty(title)) title = extraText;
                if (!TextUtils.isEmpty(title)) {
                    MusicSyncManager.getInstance().notificationAncillaryMusicTitle(packageName, title.toString());
                }
            }
            //endregion
            //查看是否允许发送通知至设备
            if (!checkPNisAllowNotify(packageName)) return;
            LogUtils.w(TAG, "接收到的通知栏消息 -->\n"
                    + "app name     -->" + appName
                    + "\n package name -->" + packageName
                    + "\n title        -->" + (!TextUtils.isEmpty(extraTitle) ? extraTitle.toString() : "")
                    + "\n text         -->" + (extraText != null ? extraText.toString() : "")
                    + "\n ticker text  -->" + (tickerText != null ? tickerText.toString() : ""), true
            );
            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getStartTypeTrack("消息通知"), "", false, true);
            TrackingLog appNotifyLog = TrackingLog.getStartTypeTrack("接收到的通知栏消息");
            appNotifyLog.setLog("消息内容 -->\n"
                    + "app name     -->" + appName
                    + "\n package name -->" + packageName
                    + "\n title        -->" + (!TextUtils.isEmpty(extraTitle) ? extraTitle.toString() : "").length()
                    + "\n text         -->" + (extraText != null ? extraText.toString() : "").length()
                    + "\n ticker text  -->" + (tickerText != null ? tickerText.toString() : "").length());


            if (TextUtils.isEmpty(extraText)
                    && !TextUtils.isEmpty(tickerText)
                    && !TextUtils.isEmpty(extraTitle)
                    && !Objects.equals(tickerText, extraTitle)
            ) {
                extraText = tickerText;
            }

            StringBuilder str = new StringBuilder();
            str.append(appName)
                    .append(":")
                    .append(packageName)
                    .append(":")
                    .append(extraTitle)
                    .append(":")
                    .append(extraText);
            String nowMessage = str.toString().replace("null", "");

            long nowTime = System.currentTimeMillis();

            //新旧通知包名、内容，时间在3s内一致
            LogUtils.e(TAG, "\n latest msg is " + latestMessage + "\n now msg is " + nowMessage);
            LogUtils.e(TAG, "msg compare result is " + latestMessage.equals(nowMessage));
            if (latestPageageName.equalsIgnoreCase(packageName) && latestMessage.replaceAll("\n", "").equalsIgnoreCase(nowMessage.replaceAll("\n", ""))) {
                if (nowTime - latestSendTime < OutTime) {
                    LogUtils.w(TAG, "retuen 3s repeat message = " + (nowTime - latestSendTime) + "ms", true);
                    return;
                }
            }

            //过滤标题内容都为空的通知
            String title = !TextUtils.isEmpty(extraTitle) ? extraTitle.toString() : "";
            String text = extraText != null ? extraText.toString() : "";
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) {
                /*if (ControlBleTools.getInstance().isConnect()) {
                    appNotifyLog.setLog(appNotifyLog.getLog() + "\napp通知标题内容都为空拦截");
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, appNotifyLog, "1716", true, false);
                }*/
                return;
            }

            //过滤 不需要的通知
            if (checkIsSkipNotification(packageName, extraTitle + ":" + extraTitle)) {
                /*if (ControlBleTools.getInstance().isConnect()) {
                    appNotifyLog.setLog(appNotifyLog.getLog() + "\n过滤不需要的通知");
                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, appNotifyLog, "1717", true, false);
                }*/
                return;
            }

            AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, appNotifyLog, "", false, false);
            if (ControlBleTools.getInstance().isConnect()) {
                // 发送APP通知
                TrackingLog trackingLog = TrackingLog.getDevTyepTrack("发送通知至设备", "发送App通知", "SEND_APP_NOTIFICATION", "");

                ControlBleTools.getInstance().sendAppNotification(
                        appName,
                        packageName,
                        !TextUtils.isEmpty(extraTitle) ? extraTitle.toString().replace("\n", ": ") : "",
                        extraText != null ? extraText.toString().replaceAll("\n", " ") : "",
                        tickerText != null ? tickerText.toString() : "", new ParsingStateManager.SendCmdStateListener() {
                            @Override
                            public void onState(SendCmdState state) {
                                trackingLog.setEndTime(TrackingLog.getNowString());
                                trackingLog.setDevResult("state : " + state);
                                if (state == SendCmdState.SUCCEED || state == SendCmdState.UNINITIALIZED) {
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "", false, false);
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, TrackingLog.getEndTypeTrack("消息通知"), "", true, false);
                                } else {
                                    if (ControlBleTools.getInstance().isConnect()) {
                                        trackingLog.setLog(trackingLog.getLog() + "\n发送手机消息至设备失败或超时");
                                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_NOTIFY, trackingLog, "1715", true, false);
                                    }
                                }
                            }
                        });
            }

            try {
                messageMap.put(sbn.getId(), notification.when);
            } catch (Exception e) {
                e.printStackTrace();
            }
            latestPageageName = packageName;
            latestMessage = nowMessage.replaceAll("\n", "");
            latestSendTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        if (sbn != null) {
            messageMap.remove(sbn.getId());
        }
    }

    /**
     * 检查是否允许发送通知至设备
     */
    private Boolean checkPNisAllowNotify(String packageName) {
        //消息通知 总开关
        if (sysNotifyItems != null && sysNotifyItems.size() > 0) {
            for (NotifyItem notify : sysNotifyItems) {
                if (notify.getType() == 1 && notify.isTypeHeader()) {
                    if (!notify.isOpen()) {
                        return false;
                    }
                }
            }
        }
        //第三方APP
        if (appNotifyItems != null && appNotifyItems.size() > 0) {
            for (NotifyItem notify : appNotifyItems) {
                if (notify.getPackageName().equals(packageName) && notify.isOpen()) {
                    return true;
                }
            }
        }
        //系统
        if (sysNotifyItems != null && sysNotifyItems.size() > 0) {
            for (NotifyItem notify : sysNotifyItems) {
                if (notify.getPackageName().equals(packageName) && notify.isOpen()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测是否跳过该消息
     */
    private boolean checkIsSkipNotification(String packageName, String titleAndText) {
        if (SKIP_MAP != null && !SKIP_MAP.isEmpty()) {
            for (Map.Entry<String[], String[]> entry : SKIP_MAP.entrySet()) {
                for (String p : entry.getKey()) {
                    if (p.equals(packageName)) {
                        for (String v : entry.getValue()) {
                            if (titleAndText.contains(v)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 不需要的通知 常见的重复通知
     */
    private static Map<String[], String[]> SKIP_MAP;

    private void initSkipNotify() {
        if (SKIP_MAP == null) {
            SKIP_MAP = new HashMap<>();
            //skype
            SKIP_MAP.put(new String[]{"com.skype.raider", "com.skype.rover", "com.skype.insiders"},
                    new String[]{getString(R.string.notiface_skype_no_prompt1),
                            getString(R.string.notiface_skype_no_prompt2),
                            getString(R.string.notiface_skype_no_prompt3),
                            getString(R.string.notiface_skype_no_prompt4),
                            getString(R.string.notiface_skype_no_prompt5),
                            getString(R.string.notiface_skype_no_prompt6),
                            getString(R.string.notiface_skype_no_prompt7)});
            //微信
            SKIP_MAP.put(new String[]{"com.tencent.mm", "com.weipin1.mm", "com.weipin2.mm"},
                    new String[]{getString(R.string.notiface_wx_no_prompt1),
                            getString(R.string.notiface_wx_no_prompt2),
                            getString(R.string.notiface_wx_no_prompt3)});
            //QQ
            SKIP_MAP.put(new String[]{"com.tencent.mobileqq"},
                    new String[]{getString(R.string.notiface_no_prompt1),
                            getString(R.string.notiface_no_prompt2)});
            //FaceBook
            SKIP_MAP.put(new String[]{"com.facebook.katana", "com.facebook.orca"},
                    new String[]{getString(R.string.notiface_facebook_no_prompt1),
                            getString(R.string.notiface_facebook_no_prompt2),
                            getString(R.string.notiface_facebook_no_prompt3)});
            //snapchat
            SKIP_MAP.put(new String[]{"com.snapchat.android"},
                    new String[]{getString(R.string.notiface_snapchat_no_prompt1),
                            getString(R.string.notiface_snapchat_no_prompt2),
                            getString(R.string.notiface_snapchat_no_prompt3),
                            getString(R.string.notiface_snapchat_no_prompt4)});
            //ICQ New
            SKIP_MAP.put(new String[]{"com.icq.mobile.client"},
                    new String[]{getString(R.string.notiface_icq_no_prompt_1)});
        }
    }
    //endregion

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.e(TAG,"onDestroy");
        com.jwei.xzfit.utils.AppUtils.unregisterEventBus(this);
        unRegisterReceiver();
        BaseApplication.application.startMyNotificationsService();
    }
}
