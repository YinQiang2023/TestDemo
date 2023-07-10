package com.jwei.publicone.utils;

/**
 * Created by zjw on 2017/6/15.
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.jwei.publicone.ui.device.bean.PhoneDtoModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PhoneUtil {

    private static final String TAG = PhoneUtil.class.getSimpleName();

    /**
     * 挂电话
     * 挂断电话
     *
     * @param context
     */
    public static void endCall(Context context) {
        try {
            Log.w(TAG, "SDK=" + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (PermissionUtils.isGranted(Manifest.permission.ANSWER_PHONE_CALLS)) {
                    TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                    if (tm != null) {
                        boolean success = tm.endCall();
                        Log.w(TAG, "endCall success=" + success);
                    }
                    return;
                }
            } else {
                if (PermissionUtils.isGranted(Manifest.permission.CALL_PHONE)) {
                    Object telephonyObject = getTelephonyObject(context);
                    if (telephonyObject != null) {
                        Class telephonyClass = telephonyObject.getClass();
                        Method endCallMethod = telephonyClass.getMethod("endCall");
                        endCallMethod.setAccessible(true);
                        endCallMethod.invoke(telephonyObject);
                    }
                }
            }
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            Log.w(TAG, "endCall=" + e);
            //api 27 : 红米 6A
            com.jwei.publicone.utils.LogUtils.e("挂电话失败", "endCall" + e, true);
            e.printStackTrace();
        }

    }

    private static Object getTelephonyObject(Context context) {
        Object telephonyObject = null;
        try {
            // 初始化iTelephony
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // Will be used to invoke hidden methods with reflection
            // Get the current object implementing ITelephony interface
            Class telManager = telephonyManager.getClass();
            @SuppressLint("SoonBlockedPrivateApi") Method getITelephony = telManager.getDeclaredMethod("getITelephony");
            getITelephony.setAccessible(true);
            telephonyObject = getITelephony.invoke(telephonyManager);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return telephonyObject;
    }


    /**
     * 接电话
     */
    @SuppressLint("MissingPermission")
    public static void acceptRingingCall(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (PermissionUtils.isGranted(Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.MODIFY_PHONE_STATE)) {
                TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (tm != null) {
                    boolean success = false;
                    tm.acceptRingingCall();
                    Log.w(TAG, "acceptRingingCall success=" + success);
                }
                return;
            }
        }
        answerRingingCallWithReflect(context);
    }

    /**
     * 通过反射调用的方法，接听电话，该方法只在android 2.3之前的系统上有效。
     *
     * @param context
     */
    private static void answerRingingCallWithReflect(Context context) {
        try {
            Object telephonyObject = getTelephonyObject(context);
            if (null != telephonyObject) {
                Class telephonyClass = telephonyObject.getClass();
                Method endCallMethod = telephonyClass.getMethod("answerRingingCall");
                endCallMethod.setAccessible(true);

                endCallMethod.invoke(telephonyObject);
                // ITelephony iTelephony = (ITelephony) telephonyObject;
                // iTelephony.answerRingingCall();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    /**
     * 打电话
     *
     * @param context
     * @param phoneNumber
     */
    @SuppressLint("MissingPermission")
    public static void callPhone(Context context, String phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                context.startActivity(callIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 拨电话
     *
     * @param context
     * @param phoneNumber
     */
    public static void dialPhone(Context context, String phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                context.startActivity(callIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 来电静音
     *
     * @param context
     */
    public static void toggleRingerMute(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int ringMode = am.getRingerMode();
                beforeMuteRingModeMode = ringMode;
                //响铃模式才获取音量，修改音量
                if (beforeMuteRingModeMode == AudioManager.RINGER_MODE_NORMAL) {
                    int volume = am.getStreamVolume(AudioManager.STREAM_RING);
                    beforeMuteVolume = volume;
                    am.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                }
                //先NORMAL，后SILENT 才能成功设置静音模式
                //am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                LogUtils.d(TAG, "toggleRingerMute --> beforeMuteVolume:" + beforeMuteVolume + ",beforeMuteRingModeMode:" + beforeMuteRingModeMode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //静音前铃声音量
    private static int beforeMuteVolume = -1;

    //静音前铃声模式
    private static int beforeMuteRingModeMode = -1;

    /**
     * 恢复来电静音
     *
     * @param context
     */
    public static void recoverRingerMute(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                LogUtils.d(TAG, "recoverRingerMute --> beforeMuteVolume:" + beforeMuteVolume + ",beforeMuteRingModeMode:" + beforeMuteRingModeMode);
                if (beforeMuteRingModeMode != -1) {
                    am.setRingerMode(beforeMuteRingModeMode);
                }

                if (beforeMuteVolume != -1) {
                    if (beforeMuteRingModeMode == AudioManager.RINGER_MODE_NORMAL)
                        am.setStreamVolume(AudioManager.STREAM_RING, beforeMuteVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        beforeMuteVolume = -1;
        beforeMuteRingModeMode = -1;
    }


    /**
     * 联系人显示名称
     **/
    private static final int PHONES_DISPLAY_NAME_INDEX = 0;

    public static String contacNameByNumber(Context context, String number) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return number;
        }
        String name = number;
        Uri uri = Uri.parse("content://com.android.contacts/data/phones/filter/" + number);
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri, new String[]{"display_name"}, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(PHONES_DISPLAY_NAME_INDEX);
                return name;
            }
            cursor.close();
            cursor = null;
        }
        return name;
    }

    @SuppressLint("Range")
    public static String getContactNameFromPhoneBook(Context context, String phoneNum) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (TextUtils.isEmpty(phoneNum)) {
                return "";
            }
        }
        try {
            String contactName = "";
            if (TextUtils.isEmpty(phoneNum))
                return contactName;

            ContentResolver cr = context.getContentResolver();
            String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER};
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNum));

            Cursor pCur = cr.query(uri, projection,
                    null, null, null);
            if (pCur != null && pCur.getCount() > 0) {
                if (pCur.moveToFirst()) {
                    contactName = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    pCur.close();
                }
            }
            return contactName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    // 号码
    public final static String NUM = ContactsContract.CommonDataKinds.Phone.NUMBER;
    // 联系人姓名
    public final static String NAME = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;

    public static Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;


    //获取所有联系人
    @SuppressLint("Range")
    public static List<PhoneDtoModel> getPhoneAllContacts(Context context) {
        //联系人提供者的uri
        List<PhoneDtoModel> PhoneDtoModelList = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(phoneUri, new String[]{NUM, NAME}, null, null, null);
        while (cursor.moveToNext()) {
            PhoneDtoModel phoneDtoModel = new PhoneDtoModel(cursor.getString(cursor.getColumnIndex(NAME)), cursor.getString(cursor.getColumnIndex(NUM)));
            if (!TextUtils.isEmpty(phoneDtoModel.getTelPhone())) {
                phoneDtoModel.setTelPhone(phoneDtoModel.getTelPhone().replace("-", "").replace(" ", "").trim());
                if (TextUtils.isEmpty(phoneDtoModel.getName())) {
                    phoneDtoModel.setName(phoneDtoModel.getTelPhone());
                }
                //WhatsApp 添加的一个联系人有两个号码
                //[
                // {"name":"iPhone 8plus","telPhone":"+86 111 1111 1111"}
                // {"name":"iPhone 8plus","telPhone":"+8611111111111"}
                // ]
                //LogUtils.d(TAG,phoneDtoModel.toString());
                boolean isCanAdd = true;
                if (PhoneDtoModelList.size() > 0) {
                    for (PhoneDtoModel model : PhoneDtoModelList) {
                        //ios wtf 2023\4\10:
                        //相同的名称 不同的号码 在app端两个或多个都可以添加成功 固件端的常用联系人里也可保留两个或多个 设备端来电时正常显示即可
                        //相同的号码 不同的名称 在app端常用联系人里只保留最后一次添加的名称，在固件端同样保留添加的最后一位 设备端来电时正常显示即可
                        if (TextUtils.equals(model.getTelPhone(), phoneDtoModel.getTelPhone())
                            /*&&TextUtils.equals(model.getName(), phoneDtoModel.getName())*/
                        ) {
                            model.setName(phoneDtoModel.getName());
                            isCanAdd = false;
                        }
                    }
                }
                if (isCanAdd) PhoneDtoModelList.add(phoneDtoModel);
            }
        }
        cursor.close();
        return PhoneDtoModelList;
    }


    private static String[] CALL_RECORDS_COLUMNS = {
            CallLog.Calls.CACHED_NAME// 通话记录的联系人
            , CallLog.Calls.NUMBER// 通话记录的电话号码
            , CallLog.Calls.DATE// 通话记录的日期
            , CallLog.Calls.DURATION// 通话时长
            , CallLog.Calls.TYPE// 通话类型
            , CallLog.Calls._ID// 通话ID
    };

    /**
     * 获取未接来电
     */
    @SuppressLint("Range")
    public static int readMissCall(Context context) {
        int result = 0;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return -1;
        }
        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, CALL_RECORDS_COLUMNS,
                null, null, CallLog.Calls.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            result = cursor.getCount();
            while (cursor.moveToNext()) {
                int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                if (type == CallLog.Calls.MISSED_TYPE) {
                    String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    long dateLong = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    //type : CallLog.Calls.MISSED_TYPE 未接 CallLog.Calls.INCOMING_TYPE 打入 CallLog.Calls.OUTGOING_TYPE 拨出
                    int id = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                    LogUtils.d("未接来电：" + name + "," + number + "," + dateLong + "," + duration + "," + type + "," + id);
                    //未接来电：null,18166122129,1636026552067,0,3,95
                }
            }
            cursor.close();
        }
        return result;
    }

}
