package com.smartwear.xzfit.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AssetUtils {
    public static final String ASSETS_CUSTOM_DIAL_DIR = "custom_dial_data" + File.separator;

    /**
     * 获取Asset文件-bin文件
     */
    public static byte[] getAssetBytes(Context context, String fileName) {
        byte[] buffer = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * 获取Asset文件-图片
     */
    public static Bitmap getAssetBitmap(Context context, String fileName) {
        Bitmap bitmap = null;
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open(fileName);//filename是assets目录下的图片名
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}
