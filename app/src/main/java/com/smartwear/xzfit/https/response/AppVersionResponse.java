package com.smartwear.xzfit.https.response;

import android.text.TextUtils;

import com.smartwear.xzfit.utils.AppUtils;

public class AppVersionResponse {
    public String appDownloadUrl;
    public String appName;
    public String appVersion;
    public int appVersionCode;
    public int id;
    public int mustUpdate;
    public String remark;

    @Override
    public String toString() {
        return "AppVersionResponse{" +
                "appDownloadUrl='" + appDownloadUrl + '\'' +
                ", appName='" + appName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", appVersionCode=" + appVersionCode +
                ", id=" + id +
                ", mustUpdate=" + mustUpdate +
                ", remark='" + remark + '\'' +
                '}';
    }

    /**
     * 会否需要升级
     *
     * @return
     */
    public boolean isAppUpdate() {
        if (appVersionCode > 0 && !TextUtils.isEmpty(appDownloadUrl)) {
            int version_code = AppUtils.INSTANCE.getVersionCode();
            int service_code = appVersionCode;
            return service_code > version_code;
        } else {
            return false;
        }
    }

}
