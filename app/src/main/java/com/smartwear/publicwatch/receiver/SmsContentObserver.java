package com.smartwear.publicwatch.receiver;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.LogUtils;
import com.smartwear.publicwatch.ui.data.Global;

/**
 * Created by android
 * on 2021/7/19
 */
public class SmsContentObserver extends ContentObserver {
    private static final String TAG = SmsContentObserver.class.getSimpleName();
    private final Context mContext;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public SmsContentObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    public static final String[] PROJECT = new String[]{
            Telephony.Sms.BODY,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SEEN
    };

    private static String lastDate = String.valueOf(System.currentTimeMillis());
    private static String lastContent = "";

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        LogUtils.d(TAG, "SmsContentObserver onChange");
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.d(TAG, "SmsContentObserver permission.READ_SMS != PackageManager.PERMISSION_GRANTED");
            return;
        }
        try {
            Cursor cursor = mContext.getContentResolver().query(Telephony.Sms.CONTENT_URI, PROJECT,
                    null,
                    null, "_id desc");

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String body = cursor.getString(0);
                    String address = cursor.getString(1);
                    String date = cursor.getString(2);
                    String type = cursor.getString(3);
                    String status = cursor.getString(4);
                    String seen = cursor.getString(5);

                    com.smartwear.publicwatch.utils.LogUtils.d(TAG, "new sms body =" + body + " address = " + address + " date = " + date + " type = " + type + " uri = " + uri + " seen = " + seen + " status = " + status, true);
                    if (!lastDate.equalsIgnoreCase(date)) {
                        // 收到短信
                        if (Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX == Integer.parseInt(type)) {
                            lastDate = date;
                            lastContent = body;
                            Intent intent = new Intent(Global.ACTION_NEW_SMS);
                            intent.putExtra("number", address);
                            intent.putExtra("content", body);
                            try {
                                intent.putExtra("date", Long.parseLong(date));
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            LogUtils.d(TAG, "sendBroadcast:"+lastDate);
                            mContext.sendBroadcast(intent);
                        }
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            LogUtils.d(TAG, "SmsContentObserver Exception");
            e.printStackTrace();
        }
        super.onChange(selfChange);
    }
}
