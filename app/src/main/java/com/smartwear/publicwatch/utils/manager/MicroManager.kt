package com.smartwear.publicwatch.utils.manager

import android.app.Activity
import android.app.Dialog
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.Vibrator
import androidx.appcompat.widget.AppCompatTextView
import com.alibaba.fastjson.JSON
import com.android.mycamera.CameraActivity
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WidgetBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.MicroCallBack
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.device.DeviceSettingLiveData
import com.smartwear.publicwatch.ui.device.bean.DeviceSettingBean
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.SpUtils
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Created by Android on 2021/11/5.
 * 小功能综合管理类
 * 设备找手机 APP找手表 设备应用列表排序
 */
object MicroManager {
    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    fun initMicroCallBack() {
        CallBackUtils.microCallBack = object : MicroCallBack {
            override fun onPhotograph(status: Int) {
                LogUtils.e("设备触发摇摇拍照 $status")
                if (status == 2) {
                    sendTakePhotoBroadcast()
                } else if (status == 1) {
                    ActivityUtils.finishActivity(CameraActivity::class.java)
                }
            }

            override fun onWearSendFindPhone(mode: Int) {
                LogUtils.e("设备找手机 $mode")
                findPhone(mode)
            }

            override fun onWidgetList(list: MutableList<WidgetBean>) {
                LogUtils.e("设备直达卡片列表" + GsonUtils.toJson(list))
                DeviceSettingLiveData.instance.getCardList().postValue(list)
            }

            override fun onApplicationList(list: MutableList<WidgetBean>) {
                LogUtils.e("获取设备应用列表")
                DeviceSettingLiveData.instance.postWidgetList(list)
            }

            override fun onSportTypeIconList(list: MutableList<WidgetBean>?) {
            }

            override fun onSportTypeOtherList(list: MutableList<WidgetBean>?) {
            }

            override fun onQuickWidgetList(list: MutableList<WidgetBean>?) {
            }

            override fun onSportWidgetSortList(list: MutableList<WidgetBean>?) {
                LogUtils.e("设备运动排序" + GsonUtils.toJson(list))
                if (list != null) {
                    DeviceSettingLiveData.instance.postSportList(list)
                }
            }
        }
        AppUtils.registerEventBus(this)
        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMsg(event: EventMessage) {
        when (event.action) {
            EventAction.ACTION_REF_DEVICE_SETTING -> {
                deviceSettingBean = JSON.parseObject(
                    SpUtils.getValue(
                        SpUtils.DEVICE_SETTING,
                        ""
                    ), DeviceSettingBean::class.java
                )
            }
        }
    }

    //region 发送相机拍照广播
    private var lastTakePhotoTime = 0L
    private fun sendTakePhotoBroadcast() {
        if (Math.abs(System.currentTimeMillis() - lastTakePhotoTime) > 3500) {
            lastTakePhotoTime = System.currentTimeMillis()
            val intent = Intent()
            intent.action = Global.TAG_SEND_PHOTO_ACTION
            BaseApplication.mContext.sendBroadcast(intent)
        }
    }
    //endregion


    //region 设备查找手机
    fun findPhone(mode: Int) {
        //设备产品功能列表不支持
        if (deviceSettingBean != null && !deviceSettingBean!!.functionRelated.find_phone) {
            com.smartwear.publicwatch.utils.LogUtils.e("findPhone", "设备不支持找手机功能！！！！", true)
            return
        }
        if (mode == 0) {
            startFindPhone()
        } else {
            stopFindPhone()
        }
    }

    private var mMediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volume = 0 //上次音量

    private var notificationId = -1 //通知标识id

    private var findPhoneDialog: Dialog? = null
    private var findPhoneBtn: AppCompatTextView? = null
    private var findPhoneTimer: CountDownTimer? = null
    private var timeout = 0L


    private fun startFindPhone() {
        //已经有通知了不做处理
        //if (notificationId != -1) return
        //有dialog不做处理
        if (findPhoneDialog != null && findPhoneDialog!!.isShowing) return
        //播放铃声
        val mediaUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(BaseApplication.mContext, mediaUri)
            mMediaPlayer?.setLooping(true)
            mMediaPlayer?.start()
        }
        volume = VolumeUtils.getVolume(AudioManager.STREAM_MUSIC)
        //音量飚大
        VolumeUtils.setVolume(
            AudioManager.STREAM_MUSIC,
            VolumeUtils.getMaxVolume(AudioManager.STREAM_MUSIC),
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
        //震动
        vibrator = BaseApplication.mContext.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
        if (vibrator != null) {
            val patter = longArrayOf(2000, 1000, 2000, 1000) // 停-动-停-动
            vibrator!!.vibrate(patter, 0)
        }

        //显示弹窗
        ActivityUtils.getTopActivity()?.let {
            findPhoneDialog = DialogUtils.showDialogTitleAndOneButton(
                ActivityUtils.getTopActivity(),
                ActivityUtils.getTopActivity().getString(R.string.device_find_phone),
                ActivityUtils.getTopActivity().getString(R.string.device_find_phone_dialog_tip),
                ActivityUtils.getTopActivity().getString(R.string.dialog_confirm_btn),
                object : DialogUtils.DialogClickListener {
                    override fun OnOK() {
                        stopFindPhone(isSendCloseCmd = true)
                    }

                    override fun OnCancel() {}
                })
            findPhoneDialog?.setCancelable(false)
            findPhoneBtn = findPhoneDialog!!.findViewById(R.id.btnTvRight)

            //弹窗的页面被关掉了
            ActivityUtils.addActivityLifecycleCallbacks(ActivityUtils.getTopActivity(), object : Utils.ActivityLifecycleCallbacks() {
                override fun onActivityDestroyed(activity: Activity) {
                    super.onActivityDestroyed(activity)
                    if (findPhoneTimer != null) {
                        stopFindPhone(false)
                        startFindPhone()
                    }
                }
            })
        }

        startFindPhoneTimeOut()
        //region 淘汰的提示方式
        //显示通知
        /*if (NotificationUtils.areNotificationsEnabled()
            && MyNotificationsService.isEnabled(BaseApplication.mContext)
            && false  // 通知不需要了
        ) { //通知 与 访问通知权限
            if (notificationId != -1) { //上一条没消失的话先取消上一条通知
                NotificationUtils.cancel(notificationId)
            }
            //生成唯一id
            notificationId = View.generateViewId()
            NotificationUtils.notify(
                Global.FIND_PHONE_NOTIFICATION_TAG, notificationId
            ) { builder: NotificationCompat.Builder ->
                builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(BaseApplication.mContext.getString(R.string.device_looking_phone))
                    .setContentText(BaseApplication.mContext.getString(R.string.stop_looking_phone))
                    .setAutoCancel(false) //设置取消通知事件广播意图
                    .setDeleteIntent(
                        PendingIntent.getBroadcast(
                            BaseApplication.mContext,
                            notificationId,
                            Intent(
                                BaseApplication.mContext,
                                StopFindPhoneReceiver::class.java
                            ),
                            PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_IMMUTABLE
                        )
                    )
            }
        }*/

        //超时
        /*ThreadUtils.executeByIo(object : ThreadUtils.Task<Int?>() {
            @Throws(Throwable::class)
            override fun doInBackground(): Int? {
                Thread.sleep(Global.FIND_PHONE_TIMEOUT)
                return 0
            }

            override fun onSuccess(result: Int?) {
                stopFindPhone()
            }

            override fun onCancel() {}
            override fun onFail(t: Throwable) {
                stopFindPhone()
            }
        })*/
        //endregion

    }

    private fun startFindPhoneTimeOut() {
        AppUtils.apply {
            if (findPhoneTimer == null) {
                ActivityUtils.getTopActivity()?.let { activity ->
                    findPhoneTimer = object : CountDownTimer(Global.FIND_PHONE_TIMEOUT, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            if (!activity.isDestroyed && !activity.isFinishing) {
                                if (findPhoneDialog != null && !findPhoneDialog!!.isShowing) {
                                    findPhoneDialog!!.show()
                                }
                                findPhoneBtn?.text = StringBuilder()
                                    .append(activity.getString(R.string.dialog_confirm_btn))
                                    .append("( ")
                                    .append(millisUntilFinished / 1000)
                                    .append("s )")
                                    .toString()
                            }
                        }

                        override fun onFinish() {
                            stopFindPhone(isSendCloseCmd = true)
                        }
                    }
                    findPhoneTimer?.start()
                }
            }
        }
    }

    private fun stopFindPhone(closeTimer: Boolean = true, isSendCloseCmd: Boolean = false) {
        AppUtils.tryBlock {
            if (mMediaPlayer != null) {
                mMediaPlayer!!.stop()
                mMediaPlayer!!.release()
                mMediaPlayer = null
            }
            if (vibrator != null) {
                vibrator!!.cancel()
                vibrator = null
            }
            if (volume != 0) {
                VolumeUtils.setVolume(
                    AudioManager.STREAM_MUSIC,
                    volume,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
            }
            //取消通知
            /*if (notificationId != -1) {
                NotificationUtils.cancel(Global.FIND_PHONE_NOTIFICATION_TAG, notificationId)
                notificationId = -1
            }*/
            if (findPhoneTimer != null && closeTimer) {
                findPhoneTimer?.cancel()
                findPhoneTimer = null
            }
            DialogUtils.getDialogActivity(findPhoneDialog?.context)?.let {
                if (findPhoneDialog != null &&
                    findPhoneDialog!!.isShowing &&
                    !it.isFinishing &&
                    !it.isDestroyed
                ) {
                    //TODO 三星手机任务栏杀死app，实际app存活，activity销毁，不会走下面代码
                    // 异常：如果在找手机中出现上述情况，重启app重连后findPhoneDialog!=null导致找手机被拦截
                    findPhoneDialog!!.dismiss()
                    //findPhoneBtn = null
                    //findPhoneDialog = null
                }
            }
            findPhoneBtn = null
            findPhoneDialog = null
            //发送关闭找手机指令
            if (isSendCloseCmd) {
                ControlBleTools.getInstance().sendCloseFindPhone(null)
            }
        }
    }
    //endregion
}