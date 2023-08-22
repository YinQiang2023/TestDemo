package com.smartwear.publicwatch.ui.user;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.UriUtils;
import com.smartwear.publicwatch.R;
import com.smartwear.publicwatch.base.BaseApplication;
import com.smartwear.publicwatch.utils.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;

import com.smartwear.publicwatch.utils.*;

public class UpdateInfoService {
    private final String TAG = UpdateInfoService.class.getSimpleName();
    // 文件目录
    public final String APP_NAME = "com.smartwear.publicwatch";
    public String HOME_DIR = "";
    public String APK_DIR = "";

    //    Dialog progressDialog;
    Handler handler = new Handler();
    WeakReference<Context> context;
    private UpdateListener listener;
    private int length;

    public UpdateInfoService(Context context) {
        this.context = new WeakReference<>(context);
        File downloadDir = BaseApplication.mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null) {
            HOME_DIR = downloadDir.getPath();
        } else {
            HOME_DIR = PathUtils.getAppDataPathExternalFirst();
        }
        APK_DIR = HOME_DIR + File.separator + "apk" + File.separator;
        LogUtils.e(TAG, "APK_DIR :"+APK_DIR);
    }

    public void downLoadFile(final String url) {
//        ProgressBar progressBar = pDialog.findViewById(R.id.progress);

        new Thread() {
            public void run() {

                HttpURLConnection httpURLConnection = null;
                try {

                    URL connectUrl = new URL(url);
                    httpURLConnection = (HttpURLConnection) connectUrl.openConnection();
                    // 获取文件大小
                    length = httpURLConnection.getContentLength();
//                    progressBar.setMax(length); // 设置进度条的总长度
                    InputStream is = httpURLConnection.getInputStream();
                    FileOutputStream fileOutputStream = null;
                    DecimalFormat format = new DecimalFormat("0.0");
                    if (is != null) {
                        //DFU需要这里下载路径
                        ViewUtils.makeRootDirectory(APK_DIR);
                        File file = new File(APK_DIR, context.get().getString(R.string.main_app_name) + "_update.apk");
                        fileOutputStream = new FileOutputStream(file);
                        // 这个是缓冲区，即一次读取10个比特，我弄的小了点，因为在本地，所以数值太大一下就下载完了,
                        // 看不出progressbar的效果。
                        byte[] buf = new byte[1024];
                        int ch = -1;
                        int process = 0;
                        if (handler != null)
                            handler.post(() -> {
                                if (listener != null) listener.onStart();
                            });
                        while ((ch = is.read(buf)) != -1) {
                            fileOutputStream.write(buf, 0, ch);
                            process += ch;
//                            progressBar.setProgress(process); // 这里就是关键的实时更新进度了！
                            int finalProcess = process;
                            if (handler != null)
                                handler.post(() -> {
                                    float all = (float) ((length / 1024.0) / 1024);
                                    float percent = (float) ((finalProcess / 1024.0) / 1024);
                                    if (listener != null && length > 0)
                                        listener.onProgress((int) ((finalProcess + 0.f) * 100 / length), percent, all);
                                });
                        }

                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (handler != null)
                        handler.post(() -> {
                            if (listener != null) listener.onSuccess();
                            checkIsAndroidO();
                        });

                } catch (Exception e) {
                    e.printStackTrace();

                    try {
                        if (handler != null)
                            handler.post(() -> {
                                if (listener != null) listener.onFailed(-1);
                            });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }.start();

    }

    public static final int INSTALL_PACKAGES_REQUESTCODE = 0xFE;
    public static final int GET_UNKNOWN_APP_SOURCES = 0xFF;
    public boolean checkIsAndroidOneTime;

    void checkIsAndroidO() {
        try {
            LogUtils.e(TAG, "updateApk check android O");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                boolean b = false;
                try {
                    b = context.get().getPackageManager().canRequestPackageInstalls();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (b) {
                    installApk26Plus();
                } else {
                    if (checkIsAndroidOneTime) {
                        return;
                    }
                    checkIsAndroidOneTime = true;
                    // 请求安装未知应用来源的权限
                    if (ActivityUtils.getTopActivity() != null) {
                        ActivityCompat.requestPermissions(ActivityUtils.getTopActivity(), new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUESTCODE);
                    }
                }
            } else {
                installApk26();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void installApk26Plus() {
        try {
            LogUtils.INSTANCE.e(TAG, "installApk26Plus");
            File apkFile = new File(APK_DIR, context.get().getString(R.string.main_app_name) + "_update.apk");
            Uri apkOutputUri = UriUtils.file2Uri(apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkOutputUri, "application/vnd.android.package-archive");
            // 查询所有符合 intent 跳转目标应用类型的应用，注意此方法必须放置在 setDataAndType 方法之后
            List<ResolveInfo> resolveLists = context.get().getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            // 然后全部授权
            for (ResolveInfo resolveInfo : resolveLists) {
                String packageName = resolveInfo.activityInfo.packageName;
                context.get().grantUriPermission(packageName, apkOutputUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            context.get().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePermissionsResult(int requestCode, int[] grantResults) {
        if (requestCode == INSTALL_PACKAGES_REQUESTCODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                installApk26Plus();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                ((Activity) context.get()).startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
            }
        }
    }

    public void handleActivityResult(int requestCode) {
        switch (requestCode) {
            case GET_UNKNOWN_APP_SOURCES:
                checkIsAndroidO();
                break;
        }
    }

    private void installApk26() {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                installApk26Plus();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(APK_DIR, context.get().getString(R.string.main_app_name) + "_update.apk")), "application/vnd.android.package-archive");
            context.get().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        listener = null;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        handler = null;
        context = null;
    }

    public void setListener(UpdateListener listener) {
        this.listener = listener;
    }

    public interface UpdateListener {
        void onStart();

        void onProgress(int progress, float process, float length);

        void onSuccess();

        void onFailed(int errorCode);
    }

}
