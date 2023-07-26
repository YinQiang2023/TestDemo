package com.jwei.xzfit.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.FileIOUtils;
import com.blankj.utilcode.util.FileUtils;
import com.zhapp.ble.ThreadPoolService;
import com.jwei.xzfit.ui.data.Global;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Android on 2023/2/13.
 */
public class HttpLog {
    private static String FileName = "a01";
    private static File logFile = null;
    private static Context mContext;
    //日志路径
    private static String filePath = "";

    public static void init(Context context) {
        mContext = context;
    }

    private static void createFile() {
        FileName = createFileName();
        try {
            logFile = new File(mContext.getExternalFilesDir("log/http"),
                    FileName + ".txt");
            filePath = logFile.getAbsolutePath();
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String msg) {
        log("HttpLog", msg);
    }

    public static void log(String tag, String msg) {
        if (!AppUtils.isBetaApp()) return;
        Log.d(tag, msg);
        createFile();
        ThreadPoolService.getInstance().post(new Runnable() {
            @Override
            public void run() {
                checkHeader();
                String mTag = TextUtils.isEmpty(tag) ? "" : tag.toLowerCase();
                StringBuffer buffer = new StringBuffer();
                buffer.append(logTime());
                buffer.append(" ----> ");
                buffer.append(mTag);
                buffer.append(" ");
                for (int i = mTag.length(); i < 25; i++) {
                    buffer.append("-");
                }
                buffer.append("> ");
                buffer.append(msg);
                buffer.append("\r\n");
                writeFileFromString(logFile, buffer.toString(), true);
            }
        });
    }

    private static void checkHeader() {
        String oldLog = FileIOUtils.readFile2String(logFile);
        if (TextUtils.isEmpty(oldLog)) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("INFOWEAR HTTP LOG");
            buffer.append("\r\n");
            buffer.append("appVersion ----> ");
            buffer.append(AppUtils.getAppVersionName());
            buffer.append("\r\n");
            buffer.append("UserId ----> ");
            buffer.append(SpUtils.getValue(SpUtils.USER_ID, "0"));
            buffer.append("\r\n");
            if (!TextUtils.isEmpty(Global.deviceType)) {
                buffer.append("deviceType ----> ");
                buffer.append(Global.deviceType);
                buffer.append("\r\n");
            }
            if (!TextUtils.isEmpty(Global.deviceVersion)) {
                buffer.append("deviceVersion ----> ");
                buffer.append(Global.deviceVersion);
                buffer.append("\r\n");
            }
            buffer.append("phoneModel ----> ");
            buffer.append(android.os.Build.BRAND + " " + android.os.Build.MODEL);
            buffer.append("\r\n\n");
            writeFileFromString(logFile, buffer.toString(), true);
        }
    }

    private static boolean writeFileFromString(final File file,
                                               final String content,
                                               final boolean append) {
        if (file == null || content == null) return false;
        if (!file.isFile() || !file.exists()) {
            return false;
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, append));
            bw.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String logTime() {
        Date date = new Date();
        SimpleDateFormat dateFormat = com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return dateFormat.format(date);
    }

    private static String createFileName() {
        Date date = new Date();
        SimpleDateFormat dateFormat = com.jwei.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd");
        return "HTTP_" + dateFormat.format(date);
    }

    /**
     * 清除日志文件
     */
    public static void clearLog() {
        FileUtils.deleteFilesInDir(mContext.getExternalFilesDir("log/http"));
    }
}
