package com.smartwear.xzfit.ui.user

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.UriUtils
import com.yalantis.ucrop.UCrop
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityPersonalInfoBinding
import com.smartwear.xzfit.databinding.ItemPersonalInfoBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.ui.adapter.CommonAdapter
import com.smartwear.xzfit.ui.data.Global
import com.smartwear.xzfit.ui.user.bean.TargetBean
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.ui.user.utils.UnitConverUtils
import com.blankj.utilcode.util.FileUtils
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.view.wheelview.BirthdayPicker
import com.smartwear.xzfit.view.wheelview.MetricSystemPicker
import com.smartwear.xzfit.view.wheelview.NumberPicker
import com.smartwear.xzfit.view.wheelview.SexPicker
import com.smartwear.xzfit.viewmodel.UserModel
import kotlinx.coroutines.launch
import java.io.*
import java.util.*
import java.util.zip.GZIPOutputStream

class PersonalInfoActivity : BaseActivity<ActivityPersonalInfoBinding, UserModel>(ActivityPersonalInfoBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = PersonalInfoActivity::class.java.simpleName
    private lateinit var mUserBean: UserBean
    private lateinit var mTargetBean: TargetBean

    private val list: MutableList<MutableMap<String, *>> = ArrayList()

    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private val RESULT_GET_PHOTOGRAPH_CODE = 0x161

    //拍照
    private var takePictureUri: Uri? = null

    //裁剪
    private var cropUri: Uri? = null

    //最终资源uri
    private var avatarUri: Uri? = null

    //loading
    private var dialog: Dialog? = null

    // 弹框的信息
    private var dialogAvatar: Dialog? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            tvTitle?.id -> {
                /*if (isUnCommit) {
                    showUnCommitDialog()
                    return
                }*/
                finish()
            }
            binding.btnFinish.id -> {
                clickFinishBtn()
            }
            binding.cslPersonalInfoAvatar.id -> {
                PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    getString(R.string.permission_sdcard),
                    PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    showAvatarDialog()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_GET_PHOTOGRAPH_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (data == null || data.data == null) {
                        LogUtils.d(TAG, "获取相册异常：data == null", true)
                        return
                    }
                    try {
                        val resFile = UriUtils.uri2File(data.data)
                        // 跳转到图片裁剪页面，copy相册文件到缓存路径，防止裁剪异常导致相册文件出错
                        val destFile = AppUtils.copyPhotograph(resFile)
                        if (destFile == null) {
                            LogUtils.d(TAG, "获取相册异常：复制相册图片失败！", true)
                            ToastUtils.showToast(R.string.img_fail_tip)
                            return
                        }
                        cropImage(UriUtils.file2Uri(destFile))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        LogUtils.d(TAG, "获取相册异常：$e", true)
                        ToastUtils.showToast(R.string.img_fail_tip)
                    }
                }
            }
            UCrop.REQUEST_CROP -> {
                if (resultCode == RESULT_OK) {
                    //裁剪后
                    if (data == null) {
                        LogUtils.d(TAG, "裁剪异常：data == null", true)
                        delAllCacheImg()
                        ToastUtils.showToast(R.string.img_fail_tip)
                        return
                    }
                    if (avatarUri != null) {
                        //删除复制来的相册资源
                        FileUtils.delete(UriUtils.uri2File(avatarUri))
                    }
                    avatarUri = UCrop.getOutput(data)
                    cropDone()
                } else {
                    if (data != null) {
                        val cropError = UCrop.getError(data)
                        if (cropError != null) {
                            cropError.printStackTrace()
                            LogUtils.d(TAG, "裁剪异常：$cropError", true)
                            ToastUtils.showToast(R.string.img_fail_tip)
                        }
                    }
                    delAllCacheImg()
                }
            }
        }
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.user_info_title)
        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
        setViewsClickListener(this, tvTitle!!, binding.btnFinish, binding.cslPersonalInfoAvatar)
        initRv()

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result == true) {
                if (takePictureUri != null) {
                    cropImage(takePictureUri!!)
                }
            } else {
                delAllCacheImg()
            }
        }
    }

    override fun initData() {
        viewModel.upLoadUserInfo.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (avatarUri != null) {
                            uploadHead()
                        } else {
                            saveDataSuccess()
                        }

                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    HttpCommonAttributes.USER_NAME_ERR -> {
                        ToastUtils.showToast(getString(R.string.user_name_error_tips))
                    }
                }
            }
        })

        viewModel.upLoadAvatar.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        saveDataSuccess(true)
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                }
            }
        })
    }

    fun uploadHead() {
        try {
            viewModel.viewModelScope.launch {
                avatarUri?.let { uri ->
                    UriUtils.uri2File(uri)?.let { file ->
                        val head = viewModel.lubanHead(this@PersonalInfoActivity, file)
                        if (head == null) {
                            LogUtils.e(TAG, "上传头像异常--> avatarUri = null || lubanHead = null", true)
                            ToastUtils.showToast(R.string.img_fail_tip)
                            return@launch
                        }
                        FileIOUtils.readFile2BytesByStream(head)?.let { buffer ->
                            val avatarLocaData = String(Base64.encode(buffer, Base64.DEFAULT))
                            SpUtils.setValue(SpUtils.USER_INFO_AVATAR_URI, avatarLocaData)
                            val sendData = String(Base64.encode(compress(buffer), Base64.DEFAULT))
                            dialog = DialogUtils.dialogShowLoad(this@PersonalInfoActivity)
                            viewModel.upLoadAvatar(sendData)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            LogUtils.e(TAG, "上传头像异常-->${e.localizedMessage}", true)
            e.printStackTrace()
        }
    }

    fun saveDataSuccess(isChangeHead: Boolean = false) {
        ToastUtils.showToast(getString(R.string.successful_operation_tips))
        UserBean().saveData(mUserBean)
        SendCmdUtils.setUserInformation()
//        Global.removeOrAddPhysiologicalCycle()
        Global.fillListData()
        if (isChangeHead) {
            dialog = DialogUtils.dialogShowLoad(this)
            viewModel.getUserInfo()
            viewModel.getUserInfo.observe(this, Observer {
                if (!TextUtils.isEmpty(it)) {
                    dismissDialog()
                    when (it) {
                        HttpCommonAttributes.REQUEST_SUCCESS -> {
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
            })
        } else {
            this.finish()
        }
    }

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    private fun initRv() {
        fillData()
        restoreData()
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@PersonalInfoActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.personalInfoNameList)
        for (element in texts) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = element
            map["right"] = ""
            list.add(map)
        }
    }

    //加载，恢复数据
    private fun restoreData() {
        mUserBean = UserBean().getData()
        mTargetBean = TargetBean().getData()
        Log.i(TAG, "restoreData = mUserBean = $mUserBean")
        Log.i(TAG, "restoreData = mTargetBean = $mTargetBean")

        list.clear()
        val texts = resources.getStringArray(R.array.personalInfoNameList)
        for (element in texts) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = element
            map["unit"] = ""
            when (element.toString()) {
                getString(R.string.user_info_nickname) -> {
                    map["right"] = mUserBean.nickname
                }
                getString(R.string.user_info_sex) -> {
                    map["right"] = if (mUserBean.sex == "1") getString(R.string.user_info_female) else getString(R.string.user_info_male)
                }
                getString(R.string.birthday) -> {
                    map["right"] = mUserBean.birthDate
                }
                getString(R.string.user_info_height) -> {
                    map["right"] = getHeightByUnit(mUserBean.height, mUserBean.britishHeight)[1]
                    map["unit"] = getHeightByUnit(mUserBean.height, mUserBean.britishHeight)[0]
                }
                getString(R.string.user_info_weight) -> {
                    map["right"] = getWeightByUnit(mUserBean.weight, mUserBean.britishWeight)[1]
                    map["unit"] = getWeightByUnit(mUserBean.weight, mUserBean.britishWeight)[0]
                }
            }
            list.add(map)
        }

        var flag = (!TextUtils.isEmpty(mUserBean.head))
        if (flag) {
            GlideApp.with(this).load(mUserBean.head)
                .error(R.mipmap.ic_personal_avatar)
                .placeholder(R.mipmap.ic_personal_avatar)
                .into(binding.ivUserAvatar)
        } else {
            GlideApp.with(this).load(R.mipmap.ic_personal_avatar)
                .into(binding.ivUserAvatar)
        }

    }

    private fun getWeightByUnit(weight: String, britishWeight: String): Array<String> {
        val valueStr: Array<String> = arrayOf("", "")
        var temp = ""
        var unit = ""
        if (mTargetBean.unit == "0") {
            temp = weight.trim()
            unit = getString(R.string.unit_weight_0)
        } else {
            temp = if (britishWeight.isNotEmpty()) {
                britishWeight
            } else {
                UnitConverUtils.kGToLbString(weight.trim())
            }
            unit = getString(R.string.unit_weight_1)
        }
        valueStr[0] = unit
        valueStr[1] = temp
        return valueStr
    }

    private fun getHeightByUnit(height: String, britishHeight: String): Array<String> {
        val valueStr: Array<String> = arrayOf("", "")
        var unit = ""
        var temp = ""
        //公制
        if (mTargetBean.unit == "0") {
            temp = height.trim()
            unit = getString(R.string.unit_height_0)
        } else {
            temp = if (britishHeight.isNotEmpty()) {
                britishHeight
            } else {
                UnitConverUtils.cmToInchString(height.trim())
            }
            unit = getString(R.string.unit_height_1)
        }
        valueStr[0] = unit
        valueStr[1] = temp
        return valueStr
    }

    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemPersonalInfoBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemPersonalInfoBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemPersonalInfoBinding {
                return ItemPersonalInfoBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemPersonalInfoBinding, t: MutableMap<String, *>, position: Int) {
                //阿拉伯适配
                val language = Locale.getDefault().language;
                if (language.equals("ar")) {
                    val drawableLeft: Drawable? = getDrawable(R.mipmap.img_right_arrow)
                    v.tvItemRight?.setCompoundDrawablesWithIntrinsicBounds(
                        drawableLeft,
                        null, null, null
                    )
                    v.tvItemRight?.compoundDrawablePadding = 4
                }

                if (position == 0) {
                    v.etNickName.visibility = View.VISIBLE
                    v.tvItemRight.visibility = View.INVISIBLE
                    v.etNickName.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            if (s != null && s.isNotEmpty()) {
                                mUserBean.nickname = "$s"
                            } else {
                                mUserBean.nickname = ""
                            }
                        }

                        override fun afterTextChanged(s: Editable?) {
                        }

                    })
                    v.etNickName.setOnEditorActionListener { v, actionId, event -> true }
                }

                v.tvItemLeft.text = "${t["content"]}"
                if ((t["right"] as String) != "") {
                    if (v.etNickName.visibility == View.VISIBLE) {
                        v.etNickName.setText("${t["right"]}")
                        v.etNickName.setSelection(v.etNickName.text.toString().trim().length)
                        v.etNickName.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(20)

//                        v.etNickName.hint = "${t["right"]}"
                    } else {
                        v.tvItemRight.text = "${t["right"]} ${t["unit"]}"
                    }
                }

                if (position == (list.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }

                v.cslItemPersonalInfoParent.setOnClickListener {
                    try {
                        when (v.tvItemLeft.text.toString()) {
                            getString(R.string.user_info_sex) -> {
                                createSexDialog(position)
                            }
                            getString(R.string.birthday) -> {
                                createDateDialog(position)
                            }
                            getString(R.string.user_info_height) -> {
                                createHeightDialog("${t["right"]}", position)
                            }
                            getString(R.string.user_info_weight) -> {
                                createWeightDialog("${t["right"]}", position)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    private fun createWeightDialog(default: String, positions: Int) {

        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            var unit = ""
            var sum = "0"
            //公制
            if (mTargetBean.unit.trim().toInt() == 0) {
                mUserBean.weight = item.toString()
                sum = item.toString()
                unit = getString(R.string.unit_weight_0)
            }
            //英制
            else {
                mUserBean.weight = UnitConverUtils.lbToKGString(item.toString())
                mUserBean.britishWeight = item.toString()
                sum = item.toString()
                unit = getString(R.string.unit_weight_1)
            }
            resetRvData(sum, positions, unit)
        }

        //公制
        if (mTargetBean.unit.trim().toInt() == 0) {
            picker.setRange(Constant.WEIGHT_TARGET_METRIC_MIN_VALUE, Constant.WEIGHT_TARGET_METRIC_MAX_VALUE, 1)
        }
        //英制
        else {
            picker.setRange(Constant.WEIGHT_TARGET_BRITISH_MIN_VALUE, Constant.WEIGHT_TARGET_BRITISH_MAX_VALUE, 1)
        }

        if (TextUtils.isEmpty(default)) {
            //公制
            if (mTargetBean.unit.trim().toInt() == 0) {
                picker.setDefaultValue(Constant.WEIGHT_TARGET_METRIC_DEFAULT_VALUE)
            }
            //英制
            else {
                picker.setDefaultValue(Constant.WEIGHT_TARGET_BRITISH_DEFAULT_VALUE)
            }
        } else {
            picker.setDefaultValue(default)
        }

        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()

    }

    private fun createHeightDialog(defaultParam: String, positions: Int) {
        var default = defaultParam
        //默认值
        if (TextUtils.isEmpty(default)) {
            default =
                    //公制
                if (mTargetBean.unit.trim().toInt() == 0)
                    Constant.HEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString()
                //英制
                else
                    Constant.HEIGHT_TARGET_BRITISH_DEFAULT_VALUE.toString()
        }

        val picker = MetricSystemPicker(this)
        val data = mutableListOf<UserInfoActivity.MetricSystemBean>()

        //公制
        if (mTargetBean.unit.trim().toInt() == 0) {
            for (i in Constant.HEIGHT_TARGET_METRIC_MIN_VALUE..Constant.HEIGHT_TARGET_METRIC_MAX_VALUE) {
                data.add(UserInfoActivity.MetricSystemBean("$i"))
            }

            val defaultPosition = data.indexOfFirst { it.name == default }

            picker.setOnOptionPickedListener { position, item ->
                mUserBean.height = item.toString()
                resetRvData(mUserBean.height, positions, getString(R.string.unit_height_0))
            }

            picker.setDefaultPosition(defaultPosition)

        }
        //英制
        else {
            for (i in Constant.HEIGHT_TARGET_BRITISH_MIN_VALUE..Constant.HEIGHT_TARGET_BRITISH_MAX_VALUE) {
                data.add(UserInfoActivity.MetricSystemBean("$i"))
            }

            val defaultPosition = data.indexOfFirst {
                it.name == default
            }

            picker.setOnOptionPickedListener { position, item ->
                mUserBean.height = UnitConverUtils.inchToCmString(item.toString())
                mUserBean.britishHeight = item.toString()
                resetRvData((item.toString()), positions, getString(R.string.unit_height_1))
            }

            picker.setDefaultPosition(defaultPosition)

        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setShowModel(0)
        picker.rightLabel.visibility = View.GONE
        picker.show()
    }

    private fun createSexDialog(position: Int) {
        val picker = SexPicker(this, true)
        Log.i(TAG, "createSexDialog()")
        picker.setOnOptionPickedListener { pos, item ->
            Log.i(TAG, "createSexDialog() item = $item")
            mUserBean.sex = if (item.toString() == getString(R.string.user_info_female)) "1" else "0"
            Log.i(TAG, "createSexDialog = mUserBean.sex = " + mUserBean.sex)
            resetRvData(item.toString(), position, "")
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        if (mUserBean.sex.isNotEmpty()) {
            picker.setDefaultValue(if (mUserBean.sex == "1") getString(R.string.user_info_female) else getString(R.string.user_info_male))
        } else {
            picker.setDefaultValue(getString(R.string.user_info_male))
        }
        picker.show()
    }

    private fun getDate(year: Int, month: Int, day: Int): String {
        val year_str = year.toString()
        val month_str: String
        val day_str: String
        month_str = if (month < 10) {
            "0$month"
        } else {
            month.toString()
        }
        day_str = if (day < 10) {
            "0$day"
        } else {
            day.toString()
        }
        return "$year_str-$month_str-$day_str"
    }

    private fun createDateDialog(position: Int) {
        val picker = BirthdayPicker(this)
        picker.setOnDatePickedListener { year, month, day ->
            mUserBean.birthDate = getDate(year, month, day)
            resetRvData(mUserBean.birthDate, position, "")
        }
        if (mUserBean.birthDate.isNotEmpty()) {
            val tmp = mUserBean.birthDate.split("-")
            picker.setDefaultValue(
                tmp[0].trim().toInt(),
                tmp[1].trim().toInt(),
                tmp[2].trim().toInt()
            )
        } else {
            val date = Date()
            picker.setDefaultValue(TimeUtils.getYearFromDate(date), TimeUtils.getMonthFromDate(date), TimeUtils.getDayFromDate(date))
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun resetRvData(rightStr: String, position: Int, unit: String) {
        val map: MutableMap<String, Any> = HashMap()
        map["right"] = rightStr
        map["content"] = list[position]["content"] as String
        map["unit"] = unit
        list[position] = map
        binding.rvList.adapter?.notifyItemChanged(position)
    }

    private fun clickFinishBtn() {
        if (mUserBean.nickname.isNullOrEmpty() || mUserBean.birthDate.isNullOrEmpty()
            || mUserBean.height.isNullOrEmpty() || mUserBean.weight.isNullOrEmpty()
        ) {
            ToastUtils.showToast(getString(R.string.user_info_ok_btn_tips))
            return
        }
        dialog = DialogUtils.dialogShowLoad(this)
        viewModel.upLoadUserInfo(mUserBean)
    }

    private fun compress(data: ByteArray?): ByteArray? {
        var gzip: GZIPOutputStream? = null
        var baos: ByteArrayOutputStream? = null
        var newData: ByteArray? = null
        try {
            baos = ByteArrayOutputStream()
            gzip = GZIPOutputStream(baos)
            gzip.write(data)
            gzip.flush()
            gzip.finish()
            newData = baos.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                gzip!!.close()
                baos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return newData
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
            delAllCacheImg()
            PermissionUtils.checkRequestPermissions(
                this.lifecycle,
                getString(R.string.permission_camera),
                PermissionUtils.PERMISSION_GROUP_CAMERA
            ) {
                takePictures()
            }
        }
        view.findViewById<View>(R.id.albums).setOnClickListener {
            dialogAvatar?.dismiss()
            delAllCacheImg()
            getPhotograph()
        }
        view.findViewById<View>(R.id.cancel).setOnClickListener { dialogAvatar?.dismiss() }
        dialogAvatar?.onWindowAttributesChanged(wl)
        dialogAvatar?.setCanceledOnTouchOutside(true)
        dialogAvatar?.show()
    }

    /**
     * 拍照
     */
    @SuppressLint("SimpleDateFormat")
    private fun takePictures() {
        takePictureUri = UriUtils.file2Uri(AppUtils.createImageFile())
        takePhotoLauncher.launch(takePictureUri)
    }

    /**
     * 获取相册资源
     */
    private fun getPhotograph() {
        try {
            val intent = Intent(Intent.ACTION_PICK, null)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            this.startActivityForResult(intent, RESULT_GET_PHOTOGRAPH_CODE)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showToast(R.string.img_fail_tip)
        }
    }

    private fun cropImage(uri: Uri) {
        cropUri = UriUtils.file2Uri(AppUtils.createImageFile())
        avatarUri = AppUtils.limitMaximumBitmap(uri)
        val option = UCrop.Options()
        //状态栏色
        option.setStatusBarColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //标题栏色
        option.setToolbarColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //背景色
        option.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.index_bg_color))
        //标题控件-取消按钮
        option.setToolbarCancelDrawable(R.mipmap.img_back_btn)
        //标题控件-完成按钮
        option.setToolbarCropDrawable(R.mipmap.scan_right_finish)
        //标题文字
        option.setToolbarTitle("")
        //标题控件颜色
        option.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.color_FFFFFF))
        //底部控制按钮选中状态色
        option.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.selector_public_button_on))
        //圆形
        option.setCircleDimmedLayer(true)
        //启动裁剪
        UCrop.of(avatarUri!!, cropUri!!)
            .withOptions(option)
            .withMaxResultSize(3000, 3000)
            .start(this@PersonalInfoActivity)
    }

    fun cropDone() {
        if (avatarUri == null) {
            ToastUtils.showToast(R.string.img_fail_tip)
            return
        }
        //使用裁剪后的资源
        GlideApp.with(this)
            .load(avatarUri)
            .error(R.mipmap.ic_personal_avatar)
            .into(binding.ivUserAvatar)
    }

    fun delAllCacheImg() {
        AppUtils.tryBlock {
            FileUtils.deleteAllInDir(Global.LUBAN_CACHE_DIR)
        }
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        delAllCacheImg()
    }

}