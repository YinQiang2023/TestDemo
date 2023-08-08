package com.smartwear.xzfit.db.model;

import com.smartwear.xzfit.utils.LogUtils;

import org.litepal.Operator;
import org.litepal.crud.LitePalSupport;

import java.util.List;

public class BaseData extends LitePalSupport {
    public String createDateTime;
    public String upDateTime;
    public boolean isUpLoad = false;
    public String deviceType = "";
    public String deviceMac = "";
    public String deviceVersion = "";
    public String timeStamp = "";
    public String date = "";
    public String appVersion = "";

    public boolean saveUpdate(Class cls, String... conditions) {
        LogUtils.i("saveUpdate","start");
        synchronized (BaseData.class) {
            try {
                if (conditions == null) {
                    createDateTime = System.currentTimeMillis() + "";
                    upDateTime = System.currentTimeMillis() + "";
                    return save();
                }
                List<LitePalSupport> list = (List<LitePalSupport>) Operator.where(conditions).find(cls);
                if (list.isEmpty()) {
                    createDateTime = System.currentTimeMillis() + "";
                    upDateTime = System.currentTimeMillis() + "";
                    return save();
                } else {
                    upDateTime = System.currentTimeMillis() + "";
                    this.updateAll(conditions);
                    return true;
                }
            } catch (Exception e) {
                LogUtils.e("saveUpdate","Exception e = " + e,true);
//                createDateTime = System.currentTimeMillis() + "";
//                upDateTime = System.currentTimeMillis() + "";
//                return save();
                return false;
            }
        }
    }

    @Override
    public String toString() {
        return "BaseData{" +
                "createDateTime='" + createDateTime + '\'' +
                ", upDateTime='" + upDateTime + '\'' +
                ", isUpLoad=" + isUpLoad +
                '}';
    }
}
