package com.jwei.publicone.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Date;

public class ViewUtils {

    /**
     * 获取资源图片宽高
     *
     * @param res
     * @param id
     * @return
     */
    public static int[] getImageWidthAndHeight(Resources res, int id) {

        int[] ints = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;

        Bitmap bitmap = BitmapFactory.decodeResource(res, id, options);

        options.inSampleSize = 1;

        options.inJustDecodeBounds = false;

        int width = options.outWidth;

        int height = options.outHeight;

        ints[0] = width;
        ints[1] = height;
        return ints;
    }

    /**
     * 获取手机屏幕宽高
     *
     * @param context
     * @return
     */
    public static int[] getAndroidScreenProperty(Context context) {
        int[] ints = new int[2];
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels;// 屏幕宽度（像素）
            int height = dm.heightPixels; // 屏幕高度（像素）
            float density = dm.density;//屏幕密度（0.75 / 1.0 / 1.5）
            int densityDpi = dm.densityDpi;//屏幕密度dpi（120 / 160 / 240）
            //屏幕宽度算法:屏幕宽度（像素）/屏幕密度
            int screenWidth = (int) (width / density);//屏幕宽度(dp)
            int screenHeight = (int) (height / density);//屏幕高度(dp)
            ints[0] = width;
            ints[1] = height;
        }
        return ints;
    }

    /**
     * Gets the content:// URI  from the given corresponding path to a file
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                Uri uri = null;
                try {
                    uri = Uri.fromFile(imageFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (uri == null) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DATA, filePath);
                    return context.getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                } else {
                    return uri;
                }

            } else {
                return null;
            }
        }
    }


    /**
     * 根据Uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    public static String getRealPathFromUri(Context context, Uri uri) {

        // Uri分为三个部分   域名://主机名/路径/id
        // content://media/extenral/images/media/17766
        // content://com.android.providers.media.documents/document/image:2706
        // file://com.xxxx.xxxxx ---- 7.0 有限制

        int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion >= 19) { // api >= 19
            return getRealPathFromUriAboveApi19(context, uri);
        } else { // api < 19
            return getRealPathFromUriBelowAPI19(context, uri);
        }
    }

    /**
     * 适配api19以下(不包括api19),根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    private static String getRealPathFromUriBelowAPI19(Context context, Uri uri) {
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            data = getDataColumn(context, uri, null, null);
        }
        return data;
    }

    /**
     * 适配api19及以上,根据uri获取图片的绝对路径
     *
     * @param context 上下文对象
     * @param uri     图片的Uri
     * @return 如果Uri对应的图片存在, 那么返回该图片的绝对路径, 否则返回null
     */
    @SuppressLint("NewApi")
    private static String getRealPathFromUriAboveApi19(Context context, Uri uri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // 如果是document类型的 uri, 则通过document id来进行处理
            String documentId = DocumentsContract.getDocumentId(uri);
            if (isMediaDocument(uri)) { // MediaProvider
                final String[] divide = documentId.split(":");
                final String type = divide[0];
                Uri mediaUri = null;
                if ("image".equals(type)) {
                    mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    return null;
                }
                String selection = BaseColumns._ID + "=?";
                String[] selectionArgs = {divide[1]};
                filePath = getDataColumn(context, mediaUri, selection, selectionArgs);
            } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId));
                filePath = getDataColumn(context, contentUri, null, null);
            } else if (isExternalStorageDocument(uri)) {
                String[] split = documentId.split(":");
                if (split.length >= 2) {
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        filePath = /*Environment.getExternalStorageDirectory()*/context.getExternalFilesDir(null) + "/" + split[1];
                    }
                }
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            // 如果是 content 类型的 Uri
            filePath = getDataColumn(context, uri, null, null);
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            // 如果是 file 类型的 Uri,直接获取图片对应的路径
            filePath = uri.getPath();
        }
        return filePath;
    }

    /**
     * 获取数据库表中的 _data 列，即返回Uri对应的文件路径
     *
     * @return
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String path = null;

        String[] projection = new String[]{MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                path = cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri the Uri to check
     * @return Whether the Uri authority is DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                boolean isSuccess = file.mkdirs();
                Log.i("makeRootDirectory", "isSuccess = " + isSuccess);
            } else if (!file.isDirectory()) {
                boolean isde = file.delete();
                boolean isSuccess = file.mkdirs();
                Log.i("makeRootDirectory ", "isSuccess = " + isSuccess);
            }
        } catch (Exception e) {
            Log.i("makeRootDirectory:", e + "");
        }
    }

    public static Uri createImageUri(Context context) {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return context.getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    public static Uri uri;

    public static File createImageFile(Context context, boolean isCrop) {
        try {
            String timeStamp = com.jwei.publicone.utils.TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "";
            if (isCrop) {
                fileName = "IMG_" + timeStamp + "_CROP.jpg";
            } else {
                fileName = "IMG_" + timeStamp + ".jpg";
            }
            File rootFile = new File(getAppRootDirPath(context) + File.separator + "capture");
            if (!rootFile.exists()) {
                rootFile.mkdirs();
            }
            File imgFile = null;
            if (AppUtils.INSTANCE.isAboveR()) {
//                imgFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + File.separator + fileName);
                // 通过 MediaStore API 插入file 为了拿到系统裁剪要保存到的uri（因为App没有权限不能访问公共存储空间，需要通过 MediaStore API来操作）
                ContentValues values = new ContentValues();
//                values.put(MediaStore.Images.Media.DATA, imgFile.getAbsolutePath());
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                imgFile = new File(rootFile.getAbsolutePath() + File.separator + fileName);
            }
            return imgFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File getAppRootDirPath(Context context) {
        return context.getExternalFilesDir(null).getAbsoluteFile();
    }

    public static Bitmap readUriToBitmap(Context context, Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        FileDescriptor fileDescriptor = null;
        Bitmap tagBitmap = null;
        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            if (parcelFileDescriptor != null && parcelFileDescriptor.getFileDescriptor() != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                //转换uri为bitmap类型
                tagBitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                // 你可以做的~~
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
            }
        }
        return tagBitmap;
    }

    public static void setEditTextSelection(EditText editText) {
        if (!TextUtils.isEmpty(editText.getText().toString())) {
            int length = editText.getText().toString().length();
            editText.setSelection(length);
        }
    }


}
