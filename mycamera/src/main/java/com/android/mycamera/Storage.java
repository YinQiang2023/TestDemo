/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.mycamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.android.mycamera.data.LocalData;
import com.android.mycamera.exif.ExifInterface;
import com.android.mycamera.util.ApiHelper;

public class Storage {
    private static final String TAG = "CameraStorage";

    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY = DCIM + "/Camera";

    // Match the code in MediaProvider.computeBucketValues().
    public static final String BUCKET_ID =
            String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void setImageSize(ContentValues values, int width, int height) {
        // The two fields are available since ICS but got published in JB
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaColumns.WIDTH, width);
            values.put(MediaColumns.HEIGHT, height);
        }
    }

    public static void writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    // Save the image and add it to media store.
    public static Uri addImage(Context context, ContentResolver resolver, String title,
                               long date, Location location, int orientation, ExifInterface exif,
                               byte[] jpeg, int width, int height) {
        // Save the image.
        String path = generateFilepath(title);
        if (exif != null) {
            try {
                exif.writeExif(jpeg, path);
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else {
            writeFile(path, jpeg);
        }
        return addImage(context, resolver, jpeg, title, date, location, orientation,
                jpeg.length, path, width, height);
    }

    // Add the image to media store.
    public static Uri addImage(
            Context context,
            ContentResolver resolver,
            byte[] jpeg,
            String title,
            long date,
            Location location,
            int orientation,
            int jpegLength,
            String path,
            int width,
            int height
    ) {
        Log.i(TAG, "insert image path is " + path);
        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(ImageColumns.TITLE, title);
        values.put(ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(ImageColumns.DATE_TAKEN, date);
        values.put(ImageColumns.DATE_ADDED, date);
        values.put(ImageColumns.DATE_MODIFIED, date);
        values.put(ImageColumns.MIME_TYPE, LocalData.MIME_TYPE_JPEG);
        // Clockwise rotation in degrees. 0, 90, 180, or 270.
        values.put(ImageColumns.ORIENTATION, orientation);
        values.put(ImageColumns.SIZE, jpegLength);
        if (Build.VERSION.SDK_INT >= 29) {
            values.put("is_pending", 1);
            values.put("relative_path", path);
        } else {
            values.put(ImageColumns.DATA, path);
        }
        setImageSize(values, width, height);
        if (location != null) {
            values.put(ImageColumns.LATITUDE, location.getLatitude());
            values.put(ImageColumns.LONGITUDE, location.getLongitude());
        }
        Uri uri = null;
        try {
            uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
            //INTERNAL_CONTENT_URI
            //EXTERNAL_CONTENT_URI
        } catch (Throwable th) {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }

        //将原先的文件复制到该uri中
        if (Build.VERSION.SDK_INT >= 29) {
            OutputStream os = null;
            try {
                if (uri != null) {
                    os = resolver.openOutputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    values.clear();
                    values.put("is_pending", 0);
                    resolver.update(uri, values, null, null);
                    if (!bitmap.isRecycled()) bitmap.recycle();
                }
            } catch (Exception e) {
                resolver.delete(uri, null, null);
                e.printStackTrace();
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{path},
                    null,
                    (path1, uri1) -> Log.v("MediaScannerConnection", "file " + path1 + " was scanned seccessfully: " + uri1));
        } else {
            //旧版本刷新媒体库
            notifySystemToScan(context, new File(path));
        }
        return uri;
    }

    public static void deleteImage(ContentResolver resolver, Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (Throwable th) {
            Log.e(TAG, "Failed to delete image: " + uri);
        }
    }

    /**
     * Notify system to scan the file.
     *
     * @param file The file.
     */
    public static void notifySystemToScan(Context context, File file) {
        try {
            if (file == null || !file.exists()) return;
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.parse("file://" + file.getAbsolutePath()));
            context.sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateFilepath(String title) {
        return DIRECTORY + '/' + title + ".jpg";
    }

    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }
}
