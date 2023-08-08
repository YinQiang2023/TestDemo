package com.smartwear.xzfit.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

/**
 * Created by Android on 2021/12/18.
 */
public class DisplayUtil {
    private final static String TAG = DisplayUtil.class.getSimpleName();

    /**
     * 保持字体大小不随系统设置变化（用在界面加载之前）
     * 要重写Activity的attachBaseContext()
     */
    public static Context attachBaseContext(Context context, float fontScale) {
        Configuration config = context.getResources().getConfiguration();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            config.fontScale = fontScale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            return context;
        }
        //Log.i(TAG, "changeActivityFontScaleA " + config.fontScale + ", " + fontScale);
        config.fontScale = fontScale;
        context = context.createConfigurationContext(config);
        return context;
    }

    /**
     * 保持字体大小不随系统设置变化（用在界面加载之前）
     * 要重写Activity的getResources()
     */
    public static Resources getResources(Context context, Resources resources, float fontScale) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (resources != null && resources.getConfiguration().fontScale != 1f) {
                Configuration configuration = resources.getConfiguration();
                configuration.fontScale = 1f;
                resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            }
            return resources;
        }
        Configuration configuration = resources.getConfiguration();
        LocaleList locales = configuration.getLocales();
        String language = locales.get(0).getLanguage();
        Configuration config = resources.getConfiguration();
        //Log.i(TAG, "changeActivityFontScaleR " + config.fontScale + ", " + fontScale);
        if (config.fontScale != fontScale) {
            config.fontScale = fontScale;
            return context.createConfigurationContext(config).getResources();
        } else {
            return resources;
        }
    }

    /**
     * 保存字体大小，后通知界面重建，它会触发attachBaseContext，来改变字号
     */
    public static void recreate(Activity activity) {
        activity.recreate();
    }
}

