package com.smartwear.publicwatch.receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;

import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.smartwear.publicwatch.base.BaseApplication;
import com.smartwear.publicwatch.ui.data.Global;
import com.smartwear.publicwatch.ui.device.bean.PhoneDtoModel;
import com.smartwear.publicwatch.ui.eventbus.EventAction;
import com.smartwear.publicwatch.ui.eventbus.EventMessage;
import com.smartwear.publicwatch.utils.LogUtils;
import com.smartwear.publicwatch.utils.PhoneUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by https://blog.csdn.net/whu_zhangmin/article/details/17401323
 * on 2021/7/19
 */
public class MissedCallContentObserver extends ContentObserver {
    private Context mContext;
    private static final String TAG = "MissedCallContentObserver";


    public static final String[] PROJECT = new String[]{
            CallLog.Calls.NEW,              // 未接来电类型
            CallLog.Calls.CACHED_NAME,      // 通话记录的联系人
            CallLog.Calls.NUMBER,           // 通话记录的电话号码
            CallLog.Calls.DATE,             // 通话记录的日期
            CallLog.Calls.DURATION,         // 通话时长
            CallLog.Calls.TYPE,             // 通话类型
            CallLog.Calls._ID,              // 通话ID
    };
    private static final int MISSED_CALL_MSG = 1;
    public static long mMissedCallDate;


    public MissedCallContentObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    private final Map<Integer, Long> map = new HashMap<>();


    @SuppressLint({"LongLogTag", "Range"})
    @Override
    public void onChange(boolean selfChange) {
        LogUtils.w(TAG, "MissedCallContentObserver onChange");
        try {
            //恢复静音
            ThreadUtils.runOnUiThreadDelayed(() -> {
                PhoneUtil.recoverRingerMute(BaseApplication.mContext);
            }, 1000);

            Cursor cursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    PROJECT, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int callLogId = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                    long callLogDate = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));

//                    int type_ = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
//                    int duration_ = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
//                    String name_ = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
//                    String number_ = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));

                    if (map.containsKey(callLogId)) {
                        if (map.get(callLogId) == callLogDate) {
                            return;
                        }
                    }

                    int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    long logDate = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    LogUtils.d(TAG, "missed type--->" + type + "，date = " + TimeUtils.millis2String(logDate, com.smartwear.publicwatch.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:dd")));
                    switch (type) {
                        case CallLog.Calls.MISSED_TYPE:
                            Log.d(TAG, "missed type");
                            int newMissType = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.NEW));
                            Log.d(TAG, " newMissType  = " + newMissType);
                            if (newMissType == 1 || newMissType == 0) {
                                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                                long date = System.currentTimeMillis();
                                Log.w(TAG, " date = " + date);
                                if (!checkSameItem(date)) {
                                    mMissedCallDate = date;
                                    Log.w(TAG, " not the same missed!");
                                    Intent intent = new Intent(Global.ACTION_CALLS_MISSED);
                                    intent.putExtra("name", name);
                                    intent.putExtra("number", number);
                                    intent.putExtra("date", date);
//                                    mContext.sendBroadcast(intent);
                                    PhoneDtoModel phoneDtoModel = new PhoneDtoModel(name, number);
                                    phoneDtoModel.setDate(date);
                                    EventBus.getDefault().post(new EventMessage(EventAction.ACTION_CALLS_MISSED, phoneDtoModel));
                                    map.put(callLogId, callLogDate);
                                } else {
                                    Log.v(TAG, " The same missed call, ignore it!");
                                }
                            }
                            break;
                        case CallLog.Calls.INCOMING_TYPE:
                            Log.d(TAG, "incoming type");
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            Log.d(TAG, "outgoing type");
                            break;
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean checkSameItem(long date) {
        return mMissedCallDate == date;
    }

}
