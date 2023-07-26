/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jwei.xzfit.ui.device.scan;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.GsonUtils;
import com.huawei.hms.hmsscankit.OnLightVisibleCallBack;
import com.huawei.hms.hmsscankit.OnResultCallback;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;
import com.jwei.xzfit.R;
import com.jwei.xzfit.db.model.track.TrackingLog;
import com.jwei.xzfit.dialog.DialogUtils;
import com.jwei.xzfit.ui.data.Global;
import com.jwei.xzfit.ui.device.bean.DeviceScanQrCodeBean;
import com.jwei.xzfit.ui.eventbus.EventAction;
import com.jwei.xzfit.ui.eventbus.EventMessage;
import com.jwei.xzfit.ui.user.QAActivity;
import com.jwei.xzfit.utils.LogUtils;
import com.jwei.xzfit.utils.manager.AppTrackingManager;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class ScanCodeActivity extends Activity {
    private static final String TAG = ScanCodeActivity.class.getSimpleName();

    private FrameLayout frameLayout;
    private RemoteView remoteView;
    private ImageView imgBtn;
    private ImageView flushBtn;
    private LinearLayout layoutBack;
    int mScreenWidth;
    int mScreenHeight;
    //The width and height of scan_view_finder is both 240 dp.
    final int SCAN_FRAME_SIZE = 240;

    //    private int[] img = {R.drawable.flashlight_on, R.drawable.flashlight_off};

    //Declare the key. It is used to obtain the value returned from Scan Kit.
    public static final String SCAN_RESULT = "scanResult";
    public static final String SCAN_CODE = "scanCode";
    public static final int REQUEST_CODE_PHOTO = 0X1113;
    private TextView tvHelp;
    private int errorCode = 0;
    //超时
    private static final long TIME_OUT = 30 * 1000L;
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRun = new Runnable() {
        @Override
        public void run() {
            DialogUtils.INSTANCE.showBaseDialog(ScanCodeActivity.this,
                    "",
                    getString(R.string.scan_timeout_tips),
                    true, false, getString(R.string.device_info_dialog_ok_btn),
                    getString(R.string.manual_bind),
                    new DialogUtils.DialogClickListener() {
                        @Override
                        public void OnOK() {
                            finish();
                            EventBus.getDefault().post(new EventMessage(EventAction.ACTION_QUIT_QR_SCAN_MANUAL_BIND));
                        }

                        @Override
                        public void OnCancel() {
                            startTimeOut();
                        }
                    }, false
            ).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.scan_code_activity);
        // Bind the camera preview screen.
        frameLayout = findViewById(R.id.rim);
        tvHelp = findViewById(R.id.tvHelp);
        tvHelp.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        tvHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanCodeActivity.this, QAActivity.class));
            }
        });

        //1. Obtain the screen density to calculate the viewfinder's rectangle.
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        //2. Obtain the screen size.
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        int scanFrameSize = (int) (SCAN_FRAME_SIZE * density);

        //3. Calculate the viewfinder's rectangle, which in the middle of the layout.
        //Set the scanning area. (Optional. Rect can be null. If no settings are specified, it will be located in the middle of the layout.)
        Rect rect = new Rect();
        rect.left = mScreenWidth / 2 - scanFrameSize / 2;
        rect.right = mScreenWidth / 2 + scanFrameSize / 2;
        rect.top = mScreenHeight / 2 - scanFrameSize / 2;
        rect.bottom = mScreenHeight / 2 + scanFrameSize / 2;


        //Initialize the RemoteView instance, and set callback for the scanning result.
        remoteView = new RemoteView.Builder().setContext(this).setBoundingBox(rect).setFormat(HmsScan.ALL_SCAN_TYPE).build();
        // When the light is dim, this API is called back to display the flashlight switch.
        flushBtn = findViewById(R.id.flush_btn);
        remoteView.setOnLightVisibleCallback(new OnLightVisibleCallBack() {
            @Override
            public void onVisibleChanged(boolean visible) {
                if (visible) {
                    flushBtn.setVisibility(View.VISIBLE);
                }
            }
        });


        // Subscribe to the scanning result callback event.
        remoteView.setOnResultCallback(new OnResultCallback() {
            @Override
            public void onResult(HmsScan[] result) {

                //Check the result.
                try {
                    if (result != null && result.length > 0 && result[0] != null && !TextUtils.isEmpty(result[0].getOriginalValue())) {
                        LogUtils.i(TAG, "onResult = " + result[0].getOriginalValue());
                        DeviceScanQrCodeBean mDeviceScanQrCodeBean = new DeviceScanQrCodeBean(result[0].getOriginalValue());
                        if (mDeviceScanQrCodeBean.getmDeviceRadioBroadcastBean() != null) {
                            LogUtils.i(TAG, "onResult = mDeviceScanQrCodeBean = " + mDeviceScanQrCodeBean.toString());

                            if (TextUtils.isEmpty(mDeviceScanQrCodeBean.getName()) ||
                                    TextUtils.isEmpty(mDeviceScanQrCodeBean.getRadio()) ||
                                    TextUtils.isEmpty(mDeviceScanQrCodeBean.getmDeviceRadioBroadcastBean().getDeviceMac()) ||
                                    mDeviceScanQrCodeBean.getmDeviceRadioBroadcastBean().getDeviceType() == 0
                            ) {
                                if (scanErrorDialog == null || !scanErrorDialog.isShowing()) {
                                    errorCode = 1;
                                    showErrorDialog();
                                    closeTimeOut();

                                    TrackingLog trackingLog = TrackingLog.getAppTypeTrack("二维码错误");
                                    trackingLog.setLog("二维码格式错误:" + GsonUtils.toJson(mDeviceScanQrCodeBean));
                                    AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, trackingLog, "1214", true, false);
                                }
                            } else {

                                boolean isOK = Global.INSTANCE.checkDeviceType(String.valueOf(mDeviceScanQrCodeBean.getmDeviceRadioBroadcastBean().getDeviceType()));
                                if (isOK) {
                                    LogUtils.i(TAG, "onResult = success");
                                    /*Intent intent = new Intent(ScanCodeActivity.this, BindDeviceActivity.class);
                                    intent.putExtra(SCAN_RESULT, result[0]);
                                    intent.putExtra(SCAN_CODE, SCAN_CODE);
                                    startActivity(intent);
                                    finish();*/
                                    Intent intent = new Intent();
                                    intent.putExtra(SCAN_RESULT, mDeviceScanQrCodeBean);
                                    intent.putExtra(SCAN_CODE, SCAN_CODE);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                } else {
                                    if (scanErrorDialog == null || !scanErrorDialog.isShowing()) {
                                        errorCode = 2;
                                        showErrorDialog();
                                        closeTimeOut();

                                        TrackingLog trackingLog = TrackingLog.getAppTypeTrack("二维码错误");
                                        trackingLog.setLog("设备产品列表不支持绑定该设备:" + GsonUtils.toJson(mDeviceScanQrCodeBean));
                                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, trackingLog, "1215", true, false);
                                    }
                                }
                            }
                        } else {
                            if (scanErrorDialog == null || !scanErrorDialog.isShowing()) {
                                errorCode = 3;
                                showErrorDialog();
                                closeTimeOut();

                                TrackingLog trackingLog = TrackingLog.getAppTypeTrack("二维码错误");
                                trackingLog.setLog("二维码错误:" + result[0].getOriginalValue());
                                AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, trackingLog, "1215", true, false);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (scanErrorDialog == null || !scanErrorDialog.isShowing()) {
                        errorCode = 4;
                        showErrorDialog();
                        closeTimeOut();

                        TrackingLog trackingLog = TrackingLog.getAppTypeTrack("二维码错误");
                        trackingLog.setLog("二维码解析异常:" + e);
                        AppTrackingManager.trackingModule(AppTrackingManager.MODULE_BIND, trackingLog, "1215", true, false);
                    }
                }

            }
        });
        // Load the customized view to the activity.
        remoteView.onCreate(savedInstanceState);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        frameLayout.addView(remoteView, params);
        // Set the back, photo scanning, and flashlight operations.
        setBackOperation();
        setPictureScanOperation();
        setFlashOperation();
    }

    private Dialog scanErrorDialog;

    private void showErrorDialog() {
        scanErrorDialog = DialogUtils.INSTANCE.showBaseDialog(ScanCodeActivity.this,
                getString(R.string.scan_error_dialog_title),
                getString(R.string.scan_error_dialog_content) + " (" + errorCode + ")",
                true, false, getString(R.string.know),
                getString(R.string.manual_bind),
                new DialogUtils.DialogClickListener() {
                    @Override
                    public void OnOK() {
                        finishResultCanceled();
                        EventBus.getDefault().post(new EventMessage(EventAction.ACTION_QUIT_QR_SCAN_MANUAL_BIND));
                    }

                    @Override
                    public void OnCancel() {
                        startTimeOut();
                    }
                }, false
        );
        scanErrorDialog.show();
    }

    /**
     * 倒计时
     */
    private void startTimeOut() {
        timeoutHandler.removeCallbacksAndMessages(null);
        timeoutHandler.postDelayed(timeoutRun, TIME_OUT);
    }

    private void closeTimeOut() {
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Call the lifecycle management method of the remoteView activity.
     */
    private void setPictureScanOperation() {
        imgBtn = findViewById(R.id.img_btn);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(pickIntent, REQUEST_CODE_PHOTO);

            }
        });
    }

    private void setFlashOperation() {
        flushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (remoteView.getLightStatus()) {
                    remoteView.switchLight();
//                    flushBtn.setImageResource(img[1]);
                } else {
                    remoteView.switchLight();
//                    flushBtn.setImageResource(img[0]);
                }
            }
        });
    }

    private void setBackOperation() {
        LinearLayout layoutBack = findViewById(R.id.layoutBack);
        layoutBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishResultCanceled();
            }
        });
    }

    /**
     * Call the lifecycle management method of the remoteView activity.
     */
    @Override
    protected void onStart() {
        super.onStart();
        remoteView.onStart();
        startTimeOut();
    }

    @Override
    protected void onResume() {
        super.onResume();
        remoteView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        remoteView.onPause();
        closeTimeOut();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        remoteView.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        remoteView.onStop();
    }

    public void finishResultCanceled() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /**
     * Handle the return results from the album.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                HmsScan[] hmsScans = ScanUtil.decodeWithBitmap(this, bitmap, new HmsScanAnalyzerOptions.Creator().setPhotoMode(true).create());
                if (hmsScans != null && hmsScans.length > 0 && hmsScans[0] != null && !TextUtils.isEmpty(hmsScans[0].getOriginalValue())) {
                    Intent intent = new Intent();
                    intent.putExtra(SCAN_RESULT, hmsScans[0]);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int cc = (int) (100 / (float) (100) * 100);
    }

}
