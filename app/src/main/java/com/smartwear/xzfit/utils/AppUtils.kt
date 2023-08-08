package com.smartwear.xzfit.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.user.bean.TargetBean
import com.smartwear.xzfit.ui.user.bean.UserLocalData
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.lang.reflect.Method
import java.util.*
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay


/**
 * Created by android
 * on 2021/7/14
 */
object AppUtils {

    fun getPhoneType(): String {
        return Build.BRAND + " " + Build.MODEL
    }

    fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    fun getAppName(): String {
        return BaseApplication.mContext?.resources?.getString(R.string.main_app_name).toString()
    }

    @JvmStatic
    fun isBetaApp(): Boolean {
        try {
            val vn = getAppVersionName()
            val vns = vn.split(".")
            return vns.get(2) != "0"
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @JvmStatic
    fun getAppVersionName(): String {
        try {
            val pkName = BaseApplication.mContext?.packageName.toString()
            var versionName = BaseApplication.mContext?.packageManager?.getPackageInfo(
                pkName, 0
            )?.versionName
            return "V$versionName"
        } catch (e: Exception) {
        }
        return "unknown"
    }

    fun getVersionCode(): Int {
        val context: Context = BaseApplication.mContext

        //获取包管理器
        val pm = context.packageManager
        //获取包信息
        try {
            val packageInfo = pm?.getPackageInfo(context.packageName, 0)
            //返回版本号
            return packageInfo?.versionCode ?: 0
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return 0
    }

    @JvmStatic
    fun getUserAgent(): String? {
        var result = ""
        val userAgent = System.getProperty("http.agent")
        val appVersionName: String = getAppVersionName()
        val appVersionNumber: String = java.lang.String.valueOf(getVersionCode())
        val appName: String = getAppName()
        result = "$appName/$appVersionNumber $userAgent"
        return result
    }

    fun isZh(context: Context): Boolean {
        val locale = context.resources.configuration.locale
        val language = locale.language
        val country = locale.country.lowercase()
        Log.i("iszh", "当前语言类型 = " + language + "地区 = " + country)
        return if (language.endsWith("zh") && country.endsWith("cn")) true else false
    }

    /**
     * 是否中文或者英文语言
     */
    fun isZhOrEn(context: Context): Boolean {
        val language = Locale.getDefault().language
        return language.equals(Locale.CHINA.language) || language.equals(Locale.ENGLISH.language)
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     *
     * @param context
     * @return true 表示开启
     */
    fun isGPSOpen(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //         通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * 打开系统位置信息访问权限设置
     * */
    fun openGpsActivity() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityUtils.getTopActivity().startActivity(settingsIntent)
    }

    /**
     * 提示打开GPS
     * */
    fun showGpsOpenDialog() {
        DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            BaseApplication.mContext.getString(R.string.open_gps_hint),
            BaseApplication.mContext.getString(R.string.know),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    openGpsActivity()
                }

                override fun OnCancel() {
                }
            })
    }

    /**
     * 获取系统屏幕高度
     * @return屏幕高度 px
     * */
    fun getScreenHeight(): Int {
        val wm = BaseApplication.mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        wm ?: return -1
        return if (AppUtils.isAboveR()) {
            wm.currentWindowMetrics.bounds.height()
        } else {
            val point = Point()
            wm.getDefaultDisplay().getRealSize(point)
            point.y
        }
    }

    /**
     * 获取系统屏幕宽度
     * @return屏幕宽度 px
     * */
    fun getScreenWidth(): Int {
        val wm = BaseApplication.mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        wm ?: return -1
        return if (AppUtils.isAboveR()) {
            wm.currentWindowMetrics.bounds.width()
        } else {
            val point = Point()
            wm.defaultDisplay.getRealSize(point)
            point.x
        }
    }

    /**
     * 注册EventBus
     * */
    @JvmStatic
    fun registerEventBus(any: Any) {
        if (!EventBus.getDefault().isRegistered(any)) {
            EventBus.getDefault().register(any)
        }
    }

    /**
     * 解注EventBus
     * */
    @JvmStatic
    fun unregisterEventBus(any: Any) {
        EventBus.getDefault().unregister(any)
    }

    /**
     * 全局try
     * */
    fun tryBlock(toast: String = "", block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
            com.smartwear.xzfit.utils.LogUtils.e("AppUtils", "tryBlock = $e", true)
            if (toast.isNotEmpty()) {
                ToastUtils.showToast(toast)
            }
        }
    }

    /**
     * 挂起函数try
     * */
    suspend fun trySuspendBlock(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 获取用户设置地图是否google
     * */
    fun isEnableGoogleMap(): Boolean {
        var userLocalData = GsonUtils.fromJson(SpUtils.getValue(SpUtils.USER_LOCAL_DATA, ""), UserLocalData::class.java)
        return if (userLocalData != null) {
            userLocalData.userMapIndex == 0
        } else {
            //中文默认高德， 其它语言默认Google
            return !isZh(Utils.getApp().applicationContext)
        }
    }

    /**
     * 获取用户单位偏好设置
     * @return 0 : 公里  1：英里
     * */
    fun getDeviceUnit(): Int {
        return TargetBean().getData().unit.toInt()
    }

    /**
     * 是否运行在Android 11 以上
     */
    fun isAboveR(): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
    }

    /**
     * 获取项目标签数据
     * @param key
     * */
    fun getMetaDataByKey(key: String): String? {
        var data: String? = null
        tryBlock {
            data = BaseApplication.mContext.packageManager.getApplicationInfo(
                com.blankj.utilcode.util.AppUtils.getAppPackageName(), PackageManager.GET_META_DATA
            )?.metaData?.getString(key)
        }
        return data
    }

    /**
     * 检查 Google Play 服务
     */
    fun checkGooglePlayServices(context: Context): Boolean {
        // 验证是否已在此设备上安装并启用Google Play服务，以及此设备上安装的旧版本是否为此客户端所需的版本
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return code == ConnectionResult.SUCCESS // 支持Google服务
        /**
         * 依靠 Play 服务 SDK 运行的应用在访问 Google Play 服务功能之前，应始终检查设备是否拥有兼容的 Google Play 服务 APK。
         * 我们建议您在以下两个位置进行检查：主 Activity 的 onCreate() 方法中，及其 onResume() 方法中。
         * onCreate() 中的检查可确保该应用在检查成功之前无法使用。
         * onResume() 中的检查可确保当用户通过一些其他方式返回正在运行的应用（比如通过返回按钮）时，检查仍将继续进行。
         * 如果设备没有兼容的 Google Play 服务版本，您的应用可以调用以下方法，以便让用户从 Play 商店下载 Google Play 服务。
         * 它将尝试在此设备上提供Google Play服务。如果Play服务已经可用，则Task可以立即完成返回。
         */
        //GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(activity)
    }

    /**
     * 蓝牙是否开启
     */
    fun isOpenBluetooth(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return mBluetoothAdapter?.isEnabled!!
    }

    /**
     * 开启蓝牙，需要在 Activity 的 onActivityResult() 方法中监听开启结果
     *
     * @param activity 传入当前所在的 Activity
     * @return 是否成功调用开启方法，这里返回 true 不代表开启成功
     */
    fun enableBluetooth(activity: Activity, requestCode: Int): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return false
            // 如果本地蓝牙没有开启，则开启
            if (!mBluetoothAdapter.isEnabled) {
                // 我们通过startActivityForResult()方法发起的Intent将会在onActivityResult()回调方法中获取用户的选择，比如用户单击了Yes开启，
                // 那么将会收到RESULT_OK的结果，
                // 如果RESULT_CANCELED则代表用户不愿意开启蓝牙
                val mIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activity.startActivityForResult(mIntent, requestCode)
                //用enable()方法来开启，无需询问用户(无声息的开启蓝牙设备),这时就需要用到android.permission.BLUETOOTH_ADMIN权限。
                //mBluetoothAdapter.enable();
                // mBluetoothAdapter.disable();//关闭蓝牙
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    const val multiple = 9
    const val differential = 1052954232

    /**
     * ID加密
     *
     * @return
     */
    fun encryptionUid(user_id: String): String {
        val result: Long = try {
            val uid = user_id.toLong()
            uid * multiple + differential
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return ""
        }
        return result.toString()
    }

    /**
     * 判断当前输入内容是否包含Emoji表情
     */
    fun containsEmoji(source: String): Boolean {
        val len = source.length
        val isEmoji = false
        for (i in 0 until len) {
            val hs = source[i]
            if (0xd800 <= hs.toInt() && hs.toInt() <= 0xdbff) {
                if (source.length > 1) {
                    val ls = source[i + 1]
                    val uc = (hs.toInt() - 0xd800) * 0x400 + (ls.toInt() - 0xdc00) + 0x10000
                    if (0x1d000 <= uc && uc <= 0x1f77f) {
                        return true
                    }
                }
            } else {
                // non surrogate
                if (0x2100 <= hs.toInt() && hs.toInt() <= 0x27ff && hs.toInt() != 0x263b) {
                    return true
                } else if (0x2B05 <= hs.toInt() && hs.toInt() <= 0x2b07) {
                    return true
                } else if (0x2934 <= hs.toInt() && hs.toInt() <= 0x2935) {
                    return true
                } else if (0x3297 <= hs.toInt() && hs.toInt() <= 0x3299) {
                    return true
                } else if (hs.toInt() == 0xa9 || hs.toInt() == 0xae || hs.toInt() == 0x303d || hs.toInt() == 0x3030 || hs.toInt() == 0x2b55 || hs.toInt() == 0x2b1c || hs.toInt() == 0x2b1b || hs.toInt() == 0x2b50 || hs.toInt() == 0x231a) {
                    return true
                }
                if (!isEmoji && source.length > 1 && i < source.length - 1) {
                    val ls = source[i + 1]
                    if (ls.toInt() == 0x20e3) {
                        return true
                    }
                }
            }
        }
        return isEmoji
    }

    /**
     * 复制相册文件至另外一个路径
     */
    fun copyPhotograph(srcFile: File?): File? {
        var desFile: File? = null
        try {
            if (srcFile != null) {
                desFile = createImageFile()
                com.blankj.utilcode.util.FileUtils.copy(srcFile, desFile) { srcFile, destFile ->
                    return@copy true //替换目标路径下的旧文件
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return desFile
    }

    /**
     * 创建一个缓存图片路径
     */
    @SuppressLint("SimpleDateFormat")
    fun createImageFile(): File? {
        var imageFile: File? = null
        val path = "${Global.LUBAN_CACHE_DIR}${File.separator}${TimeUtils.date2String(Date(), com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss_SSS"))}.png"
        if (com.blankj.utilcode.util.FileUtils.createOrExistsFile(path)) {
            imageFile = com.blankj.utilcode.util.FileUtils.getFileByPath(path)
        }
        return imageFile
    }

    /**
     * 获取OpenGLRenderer Bitmap 最大支持值
     */
    fun getMaximumTextureSize(): Int {
        val egl: EGL10 = EGLContext.getEGL() as EGL10
        val display: EGLDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        // Initialise
        val version = IntArray(2)
        egl.eglInitialize(display, version)
        // Query total number of configurations
        val totalConfigurations = IntArray(1)
        egl.eglGetConfigs(display, null, 0, totalConfigurations)
        // Query actual list configurations
        val configurationsList: Array<EGLConfig?> = arrayOfNulls<EGLConfig>(totalConfigurations[0])
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)
        val textureSize = IntArray(1)
        var maximumTextureSize = 0
        // Iterate through all the configurations to located the maximum texture size
        for (i in 0 until totalConfigurations[0]) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize)

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0]) {
                maximumTextureSize = textureSize[0]
            }
            //Log.i("GLHelper", Integer.toString(textureSize[0]))
        }
        // Release
        egl.eglTerminate(display)
        //Log.i("GLHelper", "Maximum GL texture size: " + Integer.toString(maximumTextureSize))
        return maximumTextureSize
    }

    /**
     * 调整图片大小
     *
     * @param bitmap 源
     * @param dst_w  输出宽度
     * @param dst_h  输出高度
     * @return
     */
    fun imageScale(bitmap: Bitmap, dst_w: Int, dst_h: Int): Bitmap {
        val src_w = bitmap.width
        val src_h = bitmap.height
        val scale_w = dst_w.toFloat() / src_w
        val scale_h = dst_h.toFloat() / src_h
        val matrix = Matrix()
        matrix.postScale(scale_w, scale_h)
        return Bitmap.createBitmap(
            bitmap, 0, 0, src_w, src_h, matrix,
            true
        )
    }

    /**
     * 防止图片太大加载不出来 ： Bitmap too large to be uploaded into a texture (3120x4160, max=4096x4096)
     */
    fun limitMaximumBitmap(uri: Uri): Uri {
        var legitimateUri: Uri = uri
        tryBlock {
            val bmFile = UriUtils.uri2File(uri)
            val bmStream = FileIOUtils.readFile2BytesByStream(bmFile)
            val bitmap = ImageUtils.bytes2Bitmap(bmStream)
            val width = bitmap.width
            val height = bitmap.height
            LogUtils.d("bitmap: w:${width} H:${height}")
            val maxRegion = Math.max(width, height)
            val maximumTextureSize = getMaximumTextureSize()
            if (maxRegion >= maximumTextureSize) {
                val legitimateBm = if (width == maxRegion) {
                    imageScale(bitmap, maximumTextureSize, height * maximumTextureSize / width)
                } else {
                    imageScale(bitmap, width * maximumTextureSize / height, maximumTextureSize)
                }
                val legitimateBmFile = createImageFile()
                ImageUtils.save(legitimateBm, legitimateBmFile, Bitmap.CompressFormat.JPEG)
                legitimateUri = UriUtils.file2Uri(legitimateBmFile)
            }
        }
        return legitimateUri
    }

    /**
     * 获取系统是否开启了无障碍-高对比度文字
     * @Deprecated 已经失效
     */
    fun isHighContrastTextEnabled(context: Context?): Boolean {
        if (context != null) {
            val am: AccessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            var m: Method? = null
            if (am != null) {
                try {
                    m = am.javaClass.getMethod("isHighTextContrastEnabled", null)
                } catch (e: NoSuchMethodException) {
                    Log.i("FAIL", "isHighTextContrastEnabled not found in AccessibilityManager")
                }
            }
            val result: Any
            if (m != null) {
                try {
                    result = m.invoke(am, null)
                    if (result is Boolean) {
                        return result
                    }
                } catch (e: java.lang.Exception) {
                    Log.i("fail", "isHighTextContrastEnabled invoked with an exception" + e.message)
                }
            }
        }
        return false
    }

    /**
     * 应用是否开启忽略电池优化
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val packageName: String = BaseApplication.mContext.getPackageName()
                val pm: PowerManager = BaseApplication.mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    return true
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    //private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    fun toSimpleJsonString(data: Any): String {
        return GsonUtils.toJson(data)
    }


    /**
     * 强制String段落方向为 Locale 方向
     */
    fun biDiFormatterStr(res: String): String {
        return BidiFormatter.getInstance(Locale.getDefault())
            .unicodeWrap(res, TextDirectionHeuristicsCompat.LOCALE)
    }


}