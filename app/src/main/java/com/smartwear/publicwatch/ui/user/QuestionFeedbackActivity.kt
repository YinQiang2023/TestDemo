package com.smartwear.publicwatch.ui.user

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.ControlBleTools
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.*
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.ui.adapter.FeedbackImgItem
import com.smartwear.publicwatch.ui.adapter.FeedbackScreenshotsAdapter
import com.smartwear.publicwatch.ui.adapter.MultiItemCommonAdapter
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.PermissionUtils
import com.smartwear.publicwatch.utils.RegexUtils
import com.smartwear.publicwatch.utils.TimeUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.view.wheelview.TimePicker
import com.smartwear.publicwatch.view.wheelview.widget.FeedbackDatePicker
import com.smartwear.publicwatch.viewmodel.UserModel
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import top.zibin.luban.Luban
import java.io.*
import java.util.*
import kotlin.coroutines.resume


class QuestionFeedbackActivity : BaseActivity<ActivityQuestionFeedbackBinding, UserModel>(ActivityQuestionFeedbackBinding::inflate, UserModel::class.java),
    View.OnClickListener {
    private var dialog: Dialog? = null
    private lateinit var feedbackScreenshotsAdapter: FeedbackScreenshotsAdapter

    // 弹框的信息
    private var dialogAvatar: Dialog? = null
    private val REQUEST_SELECT_PICTURE_FOR_UPLOAD = 0x01
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>

    //拍照
    private var takePictureUri: Uri? = null
    private var cacheDir = PathUtils.getAppDataPathExternalFirst() + "/cache/imgCache/feedback/"


    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.question_feedback)
        binding.title.tvTitle.setOnClickListener(this)
        binding.btnFinish.setOnClickListener(this)
        binding.tvQuestionFeedbackDate.setOnClickListener(this)
        binding.tvQuestionFeedbackDate.text = TimeUtils.date2Str(Date(), TimeUtils.DATEFORMAT_YEAR_MONTH_DAY)
        binding.tvQuestionFeedbackTime.setOnClickListener(this)
        binding.tvQuestionFeedbackTime.text = TimeUtils.date2Str(Date(), TimeUtils.DATEFORMAT_HOUR_MIN)

        //设置EditText的显示方式为多行文本输入
        binding.etFeedbackContentDesc.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
        //改变默认的单行模式
        binding.etFeedbackContentDesc.isSingleLine = false
        //水平滚动设置为False
        binding.etFeedbackContentDesc.setHorizontallyScrolling(false)
        //限制表情输入
        binding.etFeedbackContentDesc.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(500)

        binding.etFeedbackContentDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                judgeSubmitContent()
                (java.lang.String.valueOf(binding.etFeedbackContentDesc.text.toString().length).toString() + "/500").also { binding.tvFeedbackSubmitContentNum.text = it }
            }
        })

        binding.etFeedbackContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                judgeSubmitContent()
            }
        })

        val supportDeviceEntities: ArrayList<FeedbackImgItem> = ArrayList<FeedbackImgItem>()
        feedbackScreenshotsAdapter = FeedbackScreenshotsAdapter(this@QuestionFeedbackActivity, supportDeviceEntities)
        feedbackScreenshotsAdapter.mData.add(FeedbackImgItem(FeedbackImgItem.IMG_ADD))
        binding.rcvFeedbackScreenshots.apply {
            layoutManager = GridLayoutManager(this.context, 3)
            adapter = feedbackScreenshotsAdapter
        }
        feedbackScreenshotsAdapter.setFeedbackClickListener(object : FeedbackScreenshotsAdapter.FeedbackClickListener {
            override fun onAdd(feedbackImgItem: FeedbackImgItem?, position: Int) {
                PermissionUtils.checkRequestPermissions(
                    this@QuestionFeedbackActivity.lifecycle,
                    getString(R.string.permission_sdcard),
                    PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    showAvatarDialog()
                }
            }

            override fun onDel(feedbackImgItem: FeedbackImgItem?, position: Int) {
                val list: MutableList<FeedbackImgItem> = feedbackScreenshotsAdapter.mData
                if (list.size > 1) {
                    list.remove(feedbackImgItem)
                    if (list[list.size - 1].itemType == FeedbackImgItem.IMG) {
                        val feedbackImgItem1 = FeedbackImgItem(FeedbackImgItem.IMG_ADD)
                        list.add(feedbackImgItem1)
                    }
                    feedbackScreenshotsAdapter.notifyDataSetChanged()
                    updatePicNums(list.size - 1)
                }
            }

        })
//        binding.tvFeedbackNeedDeviceLog.isSelected = true
//        binding.tvFeedbackNeedAppLog.isSelected = true
        binding.tvFeedbackLog.isSelected = true
//        binding.tvFeedbackNeedDeviceLog.setCompoundDrawables(this@QuestionFeedbackActivity.resources.getDrawable( R.mipmap.check_circle_s),null,null,null)
        binding.tvFeedbackNeedDeviceLog.setOnClickListener(this)
        binding.tvFeedbackNeedAppLog.setOnClickListener(this)
        binding.tvFeedbackLog.setOnClickListener(this)
        initQuestionType()

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            takePictureUri?.let { picCompress(this@QuestionFeedbackActivity, it) }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            tvTitle!!.id -> {
                finish()
            }
            binding.tvQuestionFeedbackDate.id -> {
                createDateDialog()
            }
            binding.tvQuestionFeedbackTime.id -> {
                createTimeDialog()
            }
            binding.tvFeedbackNeedAppLog.id -> {
                binding.tvFeedbackNeedAppLog.isSelected = !binding.tvFeedbackNeedAppLog.isSelected
            }
            binding.tvFeedbackNeedDeviceLog.id -> {
                binding.tvFeedbackNeedDeviceLog.isSelected = !binding.tvFeedbackNeedDeviceLog.isSelected
            }
            binding.tvFeedbackLog.id -> {
                binding.tvFeedbackLog.isSelected = !binding.tvFeedbackLog.isSelected
            }
            binding.btnFinish.id -> {
                clickFinishBtn()
            }
        }
    }


    private fun clickFinishBtn(isignore: Boolean = false) {
        if (!NetworkUtils.isConnected()) {
            ToastUtils.showToast(getString(R.string.not_network_tips))
            return
        }
        if (binding.etFeedbackContentDesc.text.toString().trim().isEmpty()) {
            ToastUtils.showToast(getString(R.string.empty_desc_tips))
            return
        }
        if (binding.etFeedbackContact.text.toString().trim().isEmpty()) {
            ToastUtils.showToast(getString(R.string.register_account_email_tips))
            return
        }
        if (!RegexUtils.isEmail(binding.etFeedbackContact.text.toString().trim())) {
            ToastUtils.showToast(getString(R.string.regex_email_tips))
            return
        }
        dialog = DialogUtils.dialogShowLoad(this@QuestionFeedbackActivity)
        viewModel.launchUI {
            try {
                val type = when (clickIndex) {
                    0 -> {
                        "10"
                    }
                    1 -> {
                        "20"
                    }
                    2 -> {
                        "30"
                    }
                    3 -> {
                        "40"
                    }
                    4 -> {
                        "41"
                    }
                    /* 5 -> {
                         "42"
                     }
                     6 -> {
                         "43"
                     }
                     7 -> {
                         "44"
                     }
                     8 -> {
                         "999"
                     }*/
                    else -> {
                        "999"
                    }
                }
                val builder = MultipartBody.Builder()
                builder.addFormDataPart("questionType", type)
                //val dateTimeStr = binding.tvQuestionFeedbackDate.text.toString().replace(".", "-") + " " + binding.tvQuestionFeedbackTime.text.toString()
                builder.addFormDataPart("timeOfFailure", com.blankj.utilcode.util.TimeUtils.getNowString(TimeUtils.getSafeDateFormat(TimeUtils.DATEFORMAT_COM_YYMMDD_HHMM)))
                builder.addFormDataPart("contentDescription", binding.etFeedbackContentDesc.text.toString().trim())
                builder.addFormDataPart("contactInformation", binding.etFeedbackContact.text.toString().trim())
                builder.addFormDataPart("phoneModel", AppUtils.getPhoneType())
                builder.addFormDataPart("systemType", "2")

                val data: MutableList<FeedbackImgItem> = feedbackScreenshotsAdapter.mData as MutableList<FeedbackImgItem>
                for (i in data.indices) {
                    val multipleItem = data[i]
                    if (multipleItem.extra != null) {
                        val extra = multipleItem.extra as File
                        when (i) {
                            0 -> {
                                val file1: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), extra)
                                builder.addFormDataPart("img1", extra.name, file1)
                            }
                            1 -> {
                                val file2: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), extra)
                                builder.addFormDataPart("img2", extra.name, file2)
                            }
                            2 -> {
                                val file3: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), extra)
                                builder.addFormDataPart("img3", extra.name, file3)
                            }
                        }

                    }
                }
                LogUtils.d("path -->${SaveLog.filePath} ,${com.zhapp.ble.utils.SaveLog.filePath}")
                if (binding.tvFeedbackNeedAppLog.isSelected) {
                    if (FileUtils.isFile(SaveLog.filePath)) {
                        val file = File(SaveLog.filePath)
                        val ApplogFile: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
                        builder.addFormDataPart("appFile", file.name, ApplogFile)
                    } else {
                        // 无app日志
                        if (!isignore) {
                            showNoLogDialog(1)
                            return@launchUI
                        }
                    }
                }
                if (binding.tvFeedbackNeedDeviceLog.isSelected) {
                    if (FileUtils.isFile(com.zhapp.ble.utils.SaveLog.filePath)) {
                        val file = File(com.zhapp.ble.utils.SaveLog.filePath)
                        val ApplogFile: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
                        builder.addFormDataPart("deviceFile", file.name, ApplogFile)
                    } else {
                        // 无设备日志
                        if (!isignore) {
                            showNoLogDialog(2)
                            return@launchUI
                        }
                    }
                }
                //意见反馈日志
                //增加意见反馈日志内容
                addFeedBackLog()
                if (binding.tvFeedbackLog.isSelected) {
                    //文件限制
                    val fileSizeLimit = 8L * 1024 * 1024
                    val dir = getExternalFilesDir("log/feedback")
                    if (dir != null) {
                        val logs = FileUtils.listFilesInDirWithFilter(dir, object : FileFilter {
                            override fun accept(pathname: File?): Boolean {
                                if (pathname != null) {
                                    //取文件夹内所有bin
                                    if (pathname.absolutePath.endsWith("bin")) {
                                        return true
                                    }
                                }
                                return false
                            }
                        })
                        logs.sortDescending()
                        if (logs.size > 0) {
                            var fileSize = 0L
                            val logFiles = mutableListOf<File>()
                            for (i in 0 until logs.size) {
                                //LogUtils.d("file: " + logs.get(i).absolutePath)
                                fileSize += logs.get(i).length()
                                if (fileSize <= fileSizeLimit) {
                                    logFiles.add(logs.get(i))
                                } else {
                                    break
                                }
                            }
                            //一个文件都没有，第一个文件就大于文件限制
                            if (logFiles.size == 0) {
                                val compressDeferred = async { cropStringFile(logs.get(0), fileSizeLimit) }
                                val file = compressDeferred.await()
                                if (file != null) {
                                    logFiles.add(file)
                                }
                            }
                            if (logFiles.isNotEmpty()) {
                                val zipFile = FileUtils.getFileByPath("${dir.absolutePath}${File.separator}feedback_${System.currentTimeMillis() / 1000}.zip")
                                ZipUtils.zipFiles(logFiles, zipFile)
                                val ApplogFile: RequestBody = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), zipFile)
                                builder.addFormDataPart("appFile", zipFile.name, ApplogFile)
                            }
                        }
                    }
                }
                val userId = SpUtils.getValue(SpUtils.USER_ID, "")
                if (userId.isNotEmpty())
                    builder.addFormDataPart("userId", userId)
                if (ControlBleTools.getInstance().isConnect) {
                    builder.addFormDataPart("deviceType", Global.deviceType)
                    builder.addFormDataPart("deviceName", SpUtils.getValue(SpUtils.DEVICE_NAME, ""))
                    builder.addFormDataPart("deviceVersion", Global.deviceVersion)
                }
                val body: RequestBody = builder
                    .build()
                viewModel.uploadFeedbackInfo(body)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showToast(getString(R.string.operation_failed_tips))
            }
        }
    }

    /**
     * 增加用户反馈日志日志
     */
    private suspend fun addFeedBackLog() {
        withContext(Dispatchers.IO) {
            //消息通知开关
            SaveLog.suncWriteFile("系统消息开关", SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, ""), true)
            SaveLog.suncWriteFile("App消息开关", SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER, ""), true)
            //天气开关，最后一次天气数据
            val weatherLog = StringBuffer()
            val wTime = SpUtils.getValue(SpUtils.WEATHER_SYNC_TIME, "0")
            weatherLog.append("天气开关：").append(SpUtils.getValue(SpUtils.WEATHER_SWITCH, "false")).append("；")
                .append("同步时间戳：").append(wTime).append("；")
                .append("同步时间：").append(com.blankj.utilcode.util.TimeUtils.millis2String(wTime.toLong(), TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS"))).append("；")
                .append("定位经纬度：").append(SpUtils.getValue(SpUtils.WEATHER_LONGITUDE_LATITUDE, "")).append("；")
                .append("天气4天数据：").append(SpUtils.getValue(SpUtils.WEATHER_DAYS_INFO, "")).append("；")
            //.append("小时数据：").append(SpUtils.getValue(SpUtils.WEATHER_PER_HOUR_INFO, "")).append("；") 数据量较大
            SaveLog.suncWriteFile("天气相关数据", weatherLog.toString(), true)
            LogUtils.d(weatherLog)
            //APGS文件时间服务端设备端
            val agpsLog = StringBuffer()
            agpsLog.append("从服务端更新时间：").append(SpUtils.getValue(SpUtils.AGPS_DOWNLOAD_TIME, "0")).append("；")
                .append("同步至设备时间：").append(SpUtils.getValue(SpUtils.AGPS_SYNC_TIME, "0")).append("；")
            SaveLog.suncWriteFile("AGPS时间：", agpsLog.toString(), true)
            LogUtils.d(agpsLog)
            //同步时间
            val timeLog = StringBuffer()
            val sendTime = SpUtils.getValue(SpUtils.TIME_SET_SEND_TIME, "0")
            val sendSucTime = SpUtils.getValue(SpUtils.TIME_SET_SUCCESS_TIME, "0")
            timeLog.append("设置时间：").append(sendTime).append(",")
                .append(com.blankj.utilcode.util.TimeUtils.millis2String(sendTime.toLong(), TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS"))).append(";")
                .append("设置成功：").append(sendSucTime).append(",")
                .append(com.blankj.utilcode.util.TimeUtils.millis2String(sendSucTime.toLong(), TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm:ss:SSS")))
                .append(";")
            SaveLog.suncWriteFile("时间设置：", timeLog.toString(), true)
            LogUtils.d(timeLog)
        }
    }

    /**
     * 裁剪日志内容
     */
    private suspend fun cropStringFile(file: File, fileSizeLimit: Long): File? {
        return withContext(Dispatchers.IO) {
            return@withContext withTimeoutOrNull(30 * 1000) {
                suspendCancellableCoroutine {
                    try {
                        val newFilePath = FileUtils.getDirName(file) + FileUtils.getFileNameNoExtension(file) + "_" + System.currentTimeMillis() / 1000 + ".bin"
                        FileUtils.createOrExistsFile(newFilePath)
                        val newFile = FileUtils.getFileByPath(newFilePath)
                        val cropRatio = 1 - (fileSizeLimit * 1f / file.length())
                        val context = FileIOUtils.readFile2String(file)
                        val cs = context.replace("\n", "").replace("\r", "").split("#").toMutableList()
                        val startIndex = (cs.size * cropRatio).toInt()
                        for (i in startIndex until cs.size) {
                            SaveLog.writeFileFromString(newFile, cs[i], true)
                        }
                        LogUtils.d("cropStringFile newFilePath:$newFilePath,size:${newFile.length()}")
                        it.resume(newFile)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        it.resume(null)
                    }
                }
            }
        }
    }

    private fun showNoLogDialog(i: Int) {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            when (i) {
                1 -> {
                    getString(R.string.feedback_no_app_log_tips)
                }
                2 -> {
                    getString(R.string.feedback_no_ble_log_tips)
                }
                else -> {
                    getString(R.string.feedback_no_app_log_tips)
                }
            },
            getString(R.string.cancel_submission),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    clickFinishBtn(true)
                }

                override fun OnCancel() {}
            })
        dialog.show()
    }

    private fun createDateDialog() {
        val picker = FeedbackDatePicker(this)
        picker.setOnDatePickedListener { year, month, day ->
            binding.tvQuestionFeedbackDate.text = "$year.$month.$day"
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun createTimeDialog() {
        val picker = TimePicker(this)
        picker.setOnTimePickedListener { hour, minute, second ->
            binding.tvQuestionFeedbackTime.text = TimeUtils.date2Str(TimeUtils.setHoursAndMinutesForDate(hour, minute), TimeUtils.DATEFORMAT_HOUR_MIN)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }


    private fun initQuestionType() {
        binding.rvList.apply {
            layoutManager = GridLayoutManager(this.context, 2)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    //默认选中其它
    private var clickIndex = 4
    private fun initAdapter(): MultiItemCommonAdapter<kotlin.String, ItemQuestionTypeBinding> {
        val array = resources.getStringArray(R.array.questionFeedBackType)
        return object : MultiItemCommonAdapter<kotlin.String, ItemQuestionTypeBinding>(array.toMutableList()) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemQuestionTypeBinding {
                return ItemQuestionTypeBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemQuestionTypeBinding, t: kotlin.String, position: Int) {
                v.tvItem.text = t
                if (clickIndex == position) {
                    v.lyItem.setBackgroundResource(R.drawable.feed_back_type_press)
                    v.tvItem.setTextColor(ContextCompat.getColor(this@QuestionFeedbackActivity, R.color.color_FFFFFF))
                } else {
                    v.lyItem.setBackgroundResource(R.drawable.public_bg)
                    v.tvItem.setTextColor(ContextCompat.getColor(this@QuestionFeedbackActivity, R.color.color_171717))
                }
                v.lyItem.setOnClickListener {
                    clickIndex = position
                    notifyDataSetChanged()
                }
            }

            override fun getItemType(t: kotlin.String): Int {
                return 0
            }
        }
    }

    private fun judgeSubmitContent() {

    }

    private fun showAvatarDialog() {
        if (dialogAvatar != null && dialogAvatar!!.isShowing) {
            return
        }
        val view: View = layoutInflater.inflate(
            R.layout.dialog_photo_choose, null
        )
        dialogAvatar = Dialog(this, R.style.transparentFrameWindowStyle)
        dialogAvatar?.setContentView(
            view, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val window: Window = dialogAvatar?.window!!
        window.setWindowAnimations(R.style.picker_view_slide_anim)
        val wl = window.attributes
        wl.x = 0
        wl.y = windowManager.defaultDisplay.height
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT
        view.findViewById<View>(R.id.photograph).setOnClickListener {
            dialogAvatar?.dismiss()
            PermissionUtils.checkRequestPermissions(
                this.lifecycle,
                getString(R.string.permission_camera),
                PermissionUtils.PERMISSION_GROUP_CAMERA
            ) {
                takePhoto()
            }
        }
        view.findViewById<View>(R.id.albums).setOnClickListener {
            dialogAvatar?.dismiss()
            pickFromGallery()
        }
        view.findViewById<View>(R.id.cancel).setOnClickListener { dialogAvatar?.dismiss() }
        dialogAvatar?.onWindowAttributesChanged(wl)
        dialogAvatar?.setCanceledOnTouchOutside(true)
        dialogAvatar?.show()
    }

    private fun pickFromGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, null)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            this.startActivityForResult(intent, REQUEST_SELECT_PICTURE_FOR_UPLOAD)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showToast(R.string.img_fail_tip)
        }
    }

    private fun takePhoto() {
        takePictureUri = UriUtils.file2Uri(AppUtils.createImageFile())
        takePhotoLauncher.launch(takePictureUri)
    }

    @SuppressLint("SimpleDateFormat")
    fun createImageFile(): File? {
        var imageFile: File? = null
        val path = "$cacheDir${com.blankj.utilcode.util.TimeUtils.date2String(Date(), TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss_SSS"))}.png"
        if (FileUtils.createOrExistsFile(path)) {
            imageFile = FileUtils.getFileByPath(path)
        }
        return imageFile
    }

    private fun updateFeedbackPicList(file: File) {
        val datas: MutableList<FeedbackImgItem> = feedbackScreenshotsAdapter.mData
        if (datas.size < 3) {
            val feedbackImgItem = FeedbackImgItem(FeedbackImgItem.IMG)
            feedbackImgItem.extra = file
            datas.add(datas.size - 1, feedbackImgItem)
            val lastMultipleItem = datas[datas.size - 1]
            lastMultipleItem.extra = null
            lastMultipleItem.setItemType(FeedbackImgItem.IMG_ADD)
            updatePicNums(datas.size - 1)
        } else if (datas.size == 3) {
            val multipleItem = datas[2]
            multipleItem.extra = file
            multipleItem.setItemType(FeedbackImgItem.IMG)
            updatePicNums(3)
        }
        feedbackScreenshotsAdapter.notifyDataSetChanged()
    }

    private fun updatePicNums(picNum: Int) {
        binding.tvFeedbackPicNum.text = "$picNum/3"
    }

    override fun initData() {
        restoreData()
        viewModel.uploadFeedbackInfo.observe(this) {
            dismissDialog()
            if (!TextUtils.isEmpty(it)) {
                //删除反馈路径下所有zip,txt文件
                FileUtils.deleteFilesInDirWithFilter(getExternalFilesDir("log/feedback"), object : FileFilter {
                    override fun accept(pathname: File?): Boolean {
                        if (pathname != null) {
                            if (pathname.absolutePath.endsWith("zip") ||
                                pathname.absolutePath.endsWith("txt") ||
                                pathname.absolutePath.endsWith("bin")
                            ) {
                                return true
                            }
                        }
                        return false
                    }
                })
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        ToastUtils.showToast(getString(R.string.submit_feedback_success))
                        this.finish()
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun restoreData() {
        val UserID = SpUtils.getValue(SpUtils.USER_ID, "")
        binding.tvQuestionFeedbackUserId.text = getString(R.string.userid) + ": " + AppUtils.encryptionUid(UserID)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_SELECT_PICTURE_FOR_UPLOAD) {
                try {
                    val resFile: File? = UriUtils.uri2File(data?.data)
                    if (resFile == null) {
                        ToastUtils.showToast(R.string.img_fail_tip)
                        return
                    }
                    picCompress(this@QuestionFeedbackActivity, resFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    com.smartwear.publicwatch.utils.LogUtils.d("获取相册", "获取相册异常：$e", true)
                    ToastUtils.showToast(R.string.img_fail_tip)
                }
            }
        }
    }

    private fun picCompress(context: Context, inputFile: Any) {

        var load: Luban.Builder? = null
        load = if (inputFile is File) {
            Luban.with(context)
                .load(inputFile)
        } else {
            val resFile: File? = UriUtils.uri2File(inputFile as Uri)
            Luban.with(context)
                .load(resFile as File)
        }

        var files: List<File?>? = null
        try {
            files = load
                .ignoreBy(100)
                .filter { path -> !(TextUtils.isEmpty(path) || path.lowercase(Locale.ENGLISH).endsWith(".gif")) }
                .setTargetDir(Global.LUBAN_CACHE_DIR)
                .get()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (files == null || files.isEmpty()) {
            return
        }
        val file = files[0]
        if (file != null) {
            updateFeedbackPicList(file)
        }
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.tryBlock {
            //清除反馈图片缓存文件
            FileUtils.deleteAllInDir(cacheDir)
            //清除鲁班压缩文件
            FileUtils.deleteAllInDir(Global.LUBAN_CACHE_DIR)
        }
    }
}