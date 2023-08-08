package com.smartwear.xzfit.ui.healthy.ecg;

import com.smartwear.xzfit.db.model.Ecg;
import com.smartwear.xzfit.ui.healthy.ecg.view.ECGAllView;
import com.smartwear.xzfit.ui.healthy.ecg.view.ECGView;

public class EcgUtils {

    public static boolean isAutoRefresh = false;//心电数据列表是否自动刷新
    public static Ecg cacheEcg;//缓存心电数据

    //心率范围
    public static final int USER_HEART_MIN = 30;
    public static final int USER_HEART_MAX = 280;

    static boolean checkHrValue(int heartValue) {
        return heartValue >= USER_HEART_MIN && heartValue <= USER_HEART_MAX;
    }

    // 心电图相关=====================
    static int DrawEcgTime = 10;
    static float DrawEcgZip = 0.0085f * 3 * 1.5f;
    static float DrawEcgHeight = 900f;
    static int DrawEcgWidth = 500;

    //初始化心电
    static void initEcgView(ECGView ecgView) {
        ecgView.setMaxPointAmount(DrawEcgWidth);
        ecgView.setMaxYNumber(DrawEcgHeight);
        ecgView.setRemovedPointNum(20);
        ecgView.setEveryNPoint(5);
        ecgView.setEffticeValue(50);
        ecgView.setEveryNPointRefresh(1);
        ecgView.setTitle("");
    }

    //处理心电数据
    static double getEcgDrawValue(double value) {
        return DrawEcgHeight / 2 - value * DrawEcgHeight * DrawEcgZip;
    }

    // 心电报告相关=====================
    public final static int MaxWidth = 1250;
    public final static float MaxHeight = 300;
    public final static int LineNumber = 3;

    //初始化心电
    static void initEcgAllView(ECGAllView eCGAllView) {
        eCGAllView.setMaxPointAmount(MaxWidth);
        eCGAllView.setMaxYNumber(MaxHeight);
        eCGAllView.setRemovedPointNum(20);
        eCGAllView.setEveryNPoint(5);//
        eCGAllView.setEffticeValue(50);//
        eCGAllView.setEveryNPointRefresh(1);//
        eCGAllView.setTitle("");
    }

    //处理心电数据
    static double getAllEcgDrawValue(double value) {
        return MaxHeight / LineNumber / 2 - value * MaxHeight / LineNumber * DrawEcgZip;
    }
}
