package com.jwei.publicone.utils;

import android.content.Context;
import android.text.TextUtils;

import com.zhapp.ble.ThreadPoolService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SaveLog {
    public static String FileName = "a01";
    private static File logFile = null;
    private static Context mContext;
    //日志路径
    public static String filePath = "";


    //意见反馈
    public static String feedbackFileName = "a01";
    private static File feedbackLogFile = null;
    public static String feedbackFilePath = "";
    //是否允许写入文件
    private static boolean isWriteLog = true;

    public static void setIsWriteLog(boolean isWriteLog) {
        SaveLog.isWriteLog = isWriteLog;
    }

    public static void init(Context context) {
        mContext = context;
        //仅测试版本写日志
        LogUtils.INSTANCE.setWriteLog(AppUtils.isBetaApp());
    }

    private static void createFile() {
        FileName = createFileName();
        try {
            logFile = new File(mContext.getExternalFilesDir("log/app"),
                    FileName + ".txt");
            filePath = logFile.getAbsolutePath();
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createFeedbackFile() {
        feedbackFileName = createFeedbackFileName();
        try {
            feedbackLogFile = new File(mContext.getExternalFilesDir("log/feedback"),
                    feedbackFileName + ".bin");
            feedbackFilePath = feedbackLogFile.getAbsolutePath();
            if (!feedbackLogFile.exists()) {
                feedbackLogFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 记录日志文件，用于测试版本apk记录日志
     *
     * @param tag
     * @param msg
     */
    public static void writeFile(String tag, String msg) {
        if (!isWriteLog) return;
        createFile();
        ThreadPoolService.getInstance().post(new Runnable() {
            @Override
            public void run() {
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

    /**
     * 记录正式版反馈需要的日志
     *
     * @param tag
     * @param msg
     * @param isFeedback
     */
    public static void writeFile(String tag, String msg, boolean isFeedback) {
        //记录到普通日志
        writeFile(tag, msg);
        if (!isFeedback) {
            return;
        }
        //记录加密反馈日志
        createFeedbackFile();
        ThreadPoolService.getInstance().post(new Runnable() {
            @Override
            public void run() {
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
                try {
                    String log = AESUtils.encrypt(buffer.toString(), JsonUtils.serviceKey)
                            .replace("\n", "")
                            .replace("\r", "") + "#";
                    writeFileFromString(feedbackLogFile, log, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 记录正式版反馈需要的日志-> 同步写入
     *
     * @param tag
     * @param msg
     * @param isFeedback
     */
    public static void suncWriteFile(String tag, String msg, boolean isFeedback) {
        //记录到普通日志
        writeFile(tag, msg);
        if (!isFeedback) {
            return;
        }
        //记录加密反馈日志
        createFeedbackFile();
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
        try {
            String log = AESUtils.encrypt(buffer.toString(), JsonUtils.serviceKey)
                    .replace("\n", "")
                    .replace("\r", "") + "#";
            writeFileFromString(feedbackLogFile, log, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean writeFileFromString(final File file,
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

    public static String logTime() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    private static String createFileName() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return "APP_" + dateFormat.format(date);
    }

    private static String createFeedbackFileName() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        return "Feedback_" + dateFormat.format(date);
    }


}
