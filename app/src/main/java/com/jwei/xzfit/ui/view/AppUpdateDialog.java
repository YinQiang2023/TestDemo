package com.jwei.xzfit.ui.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.blankj.utilcode.util.ClickUtils;
import com.jwei.xzfit.R;
import com.jwei.xzfit.ui.user.AppUpdateManager;

import java.util.Locale;


public class AppUpdateDialog extends Dialog {

    private TextView tvDialogTitle;
    private TextView tvDialogCenter;
    private ConstraintLayout lyTwoBtn;
    private TextView btnTvLeft;
    private TextView btnTvRight;

    public String tittle;
    public String content;
    public String cancelText;
    public String okText;
    public int flag;
    private AppUpdateCallback callback;
    private View view5;
    private CustomProgressTextView ctvDialogProgress;
    private View llAppDownloadProgress;
    private TextView tvDialogProgressTop;
    private TextView tvDialogProgressDown;

    public AppUpdateDialog(@NonNull Context context) {
        super(context, R.style.dialog);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setCanceledOnTouchOutside(false);
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        /*setLayoutParams(new FrameLayout.LayoutParams(
                (int) (display.getWidth() * 0.85), LinearLayout.LayoutParams.WRAP_CONTENT));*/

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_app_update);
        initView(getContext());
        setCancelable(false);
    }

    private void initView(Context context) {
        tvDialogTitle = (TextView) findViewById(R.id.tvDialogTitle);
        tvDialogCenter = (TextView) findViewById(R.id.tvDialogCenter);
        lyTwoBtn = (ConstraintLayout) findViewById(R.id.lyTwoBtn);
        btnTvLeft = (TextView) findViewById(R.id.btnTvLeft);
        btnTvRight = (TextView) findViewById(R.id.btnTvRight);
        view5 = (View) findViewById(R.id.view5);
        ctvDialogProgress = (CustomProgressTextView) findViewById(R.id.ctvDialogProgress);
        llAppDownloadProgress = (View) findViewById(R.id.llAppDownloadProgress);
        tvDialogProgressTop = (TextView) findViewById(R.id.tvDialogProgressTop);
        tvDialogProgressDown = (TextView) findViewById(R.id.tvDialogProgressDown);

        displayView(context);

        ClickUtils.applySingleDebouncing(btnTvLeft, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onCancel();
                dismiss();
            }
        });
        ClickUtils.applySingleDebouncing(btnTvRight, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onConfirm(flag);
                if (flag == AppUpdateManager.FLAG_NEED_UPDATE) {
                    dismiss();
                } else if (flag == AppUpdateManager.FLAG_NO_UPDATE) {
                    dismiss();
                } else if (flag == AppUpdateManager.FLAG_UPDATING) {
                } else {
                    // TODO: 2021/10/27 重新下载
                }
            }
        });
    }

    private void displayView(Context context) {
        String cancelText = "";
        String okText = "";
        String tittle = "";
        String content = "";
        if (flag == AppUpdateManager.FLAG_NEED_UPDATE) {
            tittle = context.getString(R.string.find_new_app_version);
            content = context.getString(R.string.find_new_app_version_tips);
            okText = context.getString(R.string.upgrade_immediately);
            cancelText = context.getString(R.string.dialog_cancel_btn);
            llAppDownloadProgress.setVisibility(View.GONE);
        } else if (flag == AppUpdateManager.FLAG_NO_UPDATE) {
            tittle = context.getString(R.string.dialog_title_tips);
            content = context.getString(R.string.current_app_version_is_latest);
            okText = context.getString(R.string.dialog_confirm_btn);
            llAppDownloadProgress.setVisibility(View.GONE);
        } else if (flag == AppUpdateManager.FLAG_UPDATING) {
            tittle = context.getString(R.string.app_updating);
            llAppDownloadProgress.setVisibility(View.VISIBLE);
        } else {
            tittle = context.getString(R.string.dialog_title_tips);
            okText = context.getString(R.string.app_redownload_version);
            content = context.getString(R.string.app_redownload_version_tips);
            cancelText = context.getString(R.string.privacy_statement_quit);
            llAppDownloadProgress.setVisibility(View.GONE);
        }

        tvDialogTitle.setText(tittle);
        tvDialogCenter.setText(content);
        btnTvLeft.setText(cancelText);
        btnTvRight.setText(okText);
        btnTvLeft.setVisibility(TextUtils.isEmpty(cancelText) ? View.GONE : View.VISIBLE);
        view5.setVisibility(TextUtils.isEmpty(cancelText) ? View.GONE : View.VISIBLE);
        lyTwoBtn.setVisibility((TextUtils.isEmpty(cancelText) && TextUtils.isEmpty(okText)) ? View.GONE : View.VISIBLE);
    }


    public void updateUI(int flag, int progress, float process, float length) {
        if (this.flag != flag) {
            this.flag = flag;
            displayView(getContext());
        }
        ctvDialogProgress.setProgress(progress);
        tvDialogProgressTop.setText(progress + "%");
        tvDialogProgressDown.setText(String.format(Locale.ENGLISH, "%.1fM/%.1fM", process, length));
    }

    public interface AppUpdateCallback {
        void onConfirm(int flag);

        void onCancel();
    }

    public void setCallback(AppUpdateCallback callback) {
        this.callback = callback;
    }

    public void onDestroy() {
        callback = null;
    }
}
