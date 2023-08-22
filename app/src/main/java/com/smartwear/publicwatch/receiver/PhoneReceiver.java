package com.smartwear.publicwatch.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.blankj.utilcode.util.LogUtils;
import com.smartwear.publicwatch.base.BaseApplication;
import com.smartwear.publicwatch.ui.data.Global;
import com.smartwear.publicwatch.ui.device.bean.PhoneDtoModel;
import com.smartwear.publicwatch.ui.eventbus.EventAction;
import com.smartwear.publicwatch.ui.eventbus.EventMessage;
import com.smartwear.publicwatch.utils.PhoneUtil;

import org.greenrobot.eventbus.EventBus;

/**
 * 来电广播监听
 */

public class PhoneReceiver extends BroadcastReceiver {

    private OnPhoneListener onPhoneListener;

    private MyPhoneStateListener listener;

    public PhoneReceiver() {}

    public PhoneReceiver(OnPhoneListener onPhoneListener) {
        this.onPhoneListener = onPhoneListener;
    }

    /**
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.d("PhoneReceiver onReceive = " + action);
        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) { //外呼
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            LogUtils.i("call OUT:" + phoneNumber);
        } else if (action.equals("android.intent.action.PHONE_STATE")) { //被呼叫
            //接受不到广播 考虑MIUI android.permission.READ_PHONE_STATE 空白通行证 https://privacy.miui.com/#/admin-privacy
            if (listener == null) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
                listener = new MyPhoneStateListener();
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) { //声音发生变化
            EventBus.getDefault().post(new EventMessage(EventAction.ACTION_VOLUME_CHANGE));
        } else if (action.equals("android.media.RINGER_MODE_CHANGED")) {
            EventBus.getDefault().post(new EventMessage(EventAction.RINGER_MODE_CHANGED));
        } else if (action.equals(Global.ACTION_CALLS_MISSED)) {
//            String name = intent.getStringExtra("name");
//            String number = intent.getStringExtra("number");
//            long date = intent.getLongExtra("date", 0);
//            PhoneDtoModel phoneDtoModel = new PhoneDtoModel(name, number);
//            phoneDtoModel.setDate(date);
//            EventBus.getDefault().post(new EventMessage(EventAction.ACTION_CALLS_MISSED, phoneDtoModel));
        } else if (action.equals(Global.ACTION_NEW_SMS)) {
            String content = intent.getStringExtra("content");
            String number = intent.getStringExtra("number");
            long date = intent.getLongExtra("date", 0);
            String name = PhoneUtil.getContactNameFromPhoneBook(BaseApplication.mContext, number);
            PhoneDtoModel phoneDtoModel = new PhoneDtoModel(name, number);
            phoneDtoModel.setDate(date);
            phoneDtoModel.setSmsContext(content);
            EventBus.getDefault().post(new EventMessage(EventAction.ACTION_NEW_SMS, phoneDtoModel));
        }
    }

    public interface OnPhoneListener {
        void onCallState(int state, String incomingNumber);
    }


    class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            super.onCallStateChanged(state, phoneNumber);
            if (onPhoneListener != null) {
                onPhoneListener.onCallState(state, phoneNumber);
            }
        }
    }


}