package com.smartwear.publicwatch.ui.user

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Base64
import android.view.*
import androidx.core.content.ContextCompat
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityUserInfoBinding
import com.smartwear.publicwatch.ui.user.bean.UserBean
import com.smartwear.publicwatch.viewmodel.UserModel
import java.io.*
import java.lang.Exception
import java.util.*
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList
import androidx.lifecycle.Observer
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.https.HttpCommonAttributes
import com.smartwear.publicwatch.ui.user.utils.UnitConverUtils
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.view.wheelview.*
import com.smartwear.publicwatch.view.wheelview.contract.*
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.UriUtils
import com.yalantis.ucrop.UCrop
import com.smartwear.publicwatch.ui.HomeActivity
import com.smartwear.publicwatch.ui.data.Global
import com.smartwear.publicwatch.ui.user.bean.TargetBean
import com.smartwear.publicwatch.utils.manager.AppTrackingManager
import kotlinx.coroutines.launch


class UserInfoActivity : BaseActivity<ActivityUserInfoBinding, UserModel>(ActivityUserInfoBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = UserInfoActivity::class.java.simpleName
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
    private var heightTemp = ""
    private var weightTemp = ""
    private var distanceTemp = ""

    //用户信息录入
    //private val inputUserInfoTrackingLog by lazy { TrackingLog.getSerTypeTrack("用户信息录入", "保存用户基本信息", "ffit/userInfo/save") }
    //上传用户头像
    //private val uploadHeadTrackingKig by lazy { TrackingLog.getSerTypeTrack("用户头像上传", "上传头像", "ffit/userInfo/uploadHeadUrl") }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnFinish.id -> {
                clickFinishBtn()
            }
            binding.ivUserAvatar.id -> {
                PermissionUtils.checkRequestPermissions(
                    this.lifecycle,
                    getString(R.string.permission_sdcard),
                    PermissionUtils.PERMISSION_GROUP_CAMERA
                ) {
                    showAvatarDialog()
                }
            }
            binding.layoutSexRight.id -> {
                createSexDialog()
            }
            binding.layoutUnitRight.id -> {
                createUnitDialog(binding.tvUnitRight.text.toString().trim())
            }
            binding.layoutBirthDayRight.id -> {
                createDateDialog()
            }
            binding.layoutHeightRight.id -> {
                createHeightDialog(heightTemp)
            }
            binding.layoutWeightRight.id -> {
                createWeightDialog(weightTemp)
            }
            binding.layoutStepRight.id -> {
                createStepGoalDialog()
            }
            binding.layoutDistanceRight.id -> {
                createDistanceTargetDialog()
            }
            binding.layoutCaloriesRight.id -> {
                createCaloriesTargetDialog()
            }
            binding.layoutSleepRight.id -> {
                createSleepTargetDialog()
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
        val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        binding.etNickName.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(20)
        binding.etNickName.addTextChangedListener(object : TextWatcher {
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
        binding.etNickName.setOnEditorActionListener { v, actionId, event -> true }
        setViewsClickListener(
            this,
            binding.btnFinish,
            binding.ivUserAvatar,
            binding.layoutSexRight,
            binding.layoutUnitRight,
            binding.layoutBirthDayRight,
            binding.layoutHeightRight,
            binding.layoutWeightRight,
            binding.layoutStepRight,
            binding.layoutDistanceRight,
            binding.layoutCaloriesRight,
            binding.layoutSleepRight
        )

        mTargetBean = TargetBean()
        mUserBean = UserBean()

        mTargetBean.unit = "0"
        binding.tvUnitRight.text = getString(R.string.user_info_unit_metric)

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result == true) {
                if (takePictureUri != null) {
                    cropImage(takePictureUri!!)
                }
            }else{
                delAllCacheImg()
            }
        }
    }

    override fun initData() {
        super.initData()

        viewModel.inputUserInfo.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                /*AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, inputUserInfoTrackingLog.apply {
                    endTime = TrackingLog.getNowString()
                })
                if(it != HttpCommonAttributes.REQUEST_SUCCESS){
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        TrackingLog.getAppTypeTrack("用户信息录入失败"), "1016", true
                    )
                }*/
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        if (avatarUri != null) {
                            uploadHead()
                        } else {
                            updateUserDataSuccess()
                        }
                        AppTrackingManager.saveBehaviorTracking(AppTrackingManager.getNewBehaviorTracking("11","36").apply {
                            functionStatus = "1"
                        })
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

                /*AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, uploadHeadTrackingKig.apply {
                    endTime = TrackingLog.getNowString()
                })
                if(it != HttpCommonAttributes.REQUEST_SUCCESS){
                    AppTrackingManager.trackingModule(
                        AppTrackingManager.MODULE_LOGIN,
                        TrackingLog.getAppTypeTrack("头像上传失败"), "1017", true
                    )
                }*/

                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        updateUserDataSuccess()
                    }
                    HttpCommonAttributes.REQUEST_PARAMS_NULL -> {
                        ToastUtils.showToast(getString(R.string.empty_request_parameter_tips))
                    }
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                    else -> {
                        ToastUtils.showToast(getString(R.string.operation_failed_tips))
                    }
                }
            }
        })
    }

    private fun uploadHead() {
        try {
            viewModel.viewModelScope.launch {
                avatarUri?.let { uri ->
                    UriUtils.uri2File(uri)?.let { file ->
                        val head = viewModel.lubanHead(this@UserInfoActivity, file)
                        if (head == null) {
                            LogUtils.e(TAG, "上传头像异常--> avatarUri = null || lubanHead = null", true)
                            ToastUtils.showToast(R.string.img_fail_tip)
                            return@launch
                        }
                        FileIOUtils.readFile2BytesByStream(head)?.let { buffer ->
                            val avatarLocaData = String(Base64.encode(buffer, Base64.DEFAULT))
                            SpUtils.setValue(SpUtils.USER_INFO_AVATAR_URI, avatarLocaData)
                            val sendData = String(Base64.encode(compress(buffer), Base64.DEFAULT))
                            dialog = DialogUtils.dialogShowLoad(this@UserInfoActivity)
                            viewModel.upLoadAvatar(sendData)
                            return@launch
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "上传头像异常-->${e.localizedMessage}", true)
            e.printStackTrace()
        }
    }

    private fun updateUserDataSuccess() {
        Log.i(TAG, "updateUserDataSuccess")
        UserBean().saveData(mUserBean)
        TargetBean().saveData(mTargetBean)
        ToastUtils.showToast(getString(R.string.set_success))
        startActivity(Intent(this, HomeActivity::class.java))
        this.finish()
        ManageActivity.cancelAll()

        //AppTrackingManager.trackingModule(AppTrackingManager.MODULE_LOGIN, TrackingLog.getEndTypeTrack("登录"), isEnd = true)
    }

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.layoutTitle.id
    }

    private fun createUnitDialog(default: String/*, positions: Int*/) {
        val data = mutableListOf<UnitBean>()
        data.add(UnitBean(getString(R.string.user_info_unit_metric)))
        data.add(UnitBean(getString(R.string.user_info_unit_imperial)))
        val defaultPosition = data.indexOfFirst { it.name == default }
        val picker = OptionPicker(this)
        picker.setOnOptionPickedListener { position, item ->
            if ("$item" == getString(R.string.user_info_unit_metric)) {
                mTargetBean.unit = "0"
            } else {
                mTargetBean.unit = "1"
            }
//            resetRvData("$item", positions, "")
            binding.tvUnitRight.text = "$item"
            if (!TextUtils.isEmpty(mUserBean.height)) {
                var unit = ""
                var temp = ""
                var sum = mUserBean.height
                Log.i(TAG, "createUnitDialog = sum 1 = $sum")
                if (TextUtils.isEmpty(sum)
                    || Integer.valueOf(sum) < Constant.HEIGHT_TARGET_METRIC_MIN_VALUE
                    || Integer.valueOf(sum) > Constant.HEIGHT_TARGET_METRIC_MAX_VALUE
                ) {
                    sum = Constant.HEIGHT_TARGET_METRIC_DEFAULT_VALUE.toString()
                }
                Log.i(TAG, "createUnitDialog = sum 2 = $sum")
                //公制
                if (mTargetBean.unit == "0") {
                    temp = sum
                    Log.i(TAG, "createUnitDialog = temp 1 = $temp")
                    unit = getString(R.string.unit_height_0)
                }
                //英制
                else {
                    temp = if (mUserBean.britishHeight.isNotEmpty()) {
                        mUserBean.britishHeight
                    } else {
                        UnitConverUtils.cmToInchString(sum)
                    }
                    Log.i(TAG, "createUnitDialog = temp 2 = $temp")
                    unit = getString(R.string.unit_height_1)
                }
                heightTemp = temp
                refreshHeight(heightTemp, unit)
            }

            //体重
            if (!TextUtils.isEmpty(mUserBean.weight)) {
                var temp = ""
                var unit = ""
                //公制
                if (mTargetBean.unit == "0") {
                    temp = mUserBean.weight.trim()
                    unit = getString(R.string.unit_weight_0)
                }
                //英制
                else {
                    temp = if (mUserBean.britishWeight.isNotEmpty()) {
                        mUserBean.britishWeight
                    } else {
                        UnitConverUtils.kGToLbString(mUserBean.weight.trim())
                    }
                    unit = getString(R.string.unit_weight_1)
                }
                weightTemp = temp
                refreshWeight(weightTemp, unit)
            }

            //距离目标
            if (!TextUtils.isEmpty(mTargetBean.distanceTarget)) {
                var temp = mTargetBean.distanceTarget
                var unit = ""
                if (mTargetBean.unit == "0") {
                    unit = getString(R.string.unit_distance_0)
                } else {
                    unit = getString(R.string.unit_distance_1)
                }
                distanceTemp = temp
                refreshDistance(distanceTemp, unit)
            }
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setDefaultPosition(defaultPosition)
        picker.show()
    }

    class UnitBean(/*var value: Int, */var name: String) : TextProvider, Serializable {
        override fun provideText(): String {
            return name
        }

        override fun toString(): String {
            return "$name"
        }
    }

    private fun createSleepTargetDialog(/*position: Int*/) {
        val sleepList = resources.getStringArray(R.array.sleepTarget)
        val sleepUnitList = resources.getStringArray(R.array.unit_sleep)
        val picker = SleepPicker(this)
        picker.setData(sleepList.toMutableList(), sleepUnitList.toMutableList())
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setOnOptionPickedListener { item ->
            mTargetBean.sleepTarget = TimeUtils.getMinutesByTimeForStyle(item)
            binding.tvSleepRight.text = TimeUtils.getHoursAndMinutes(mTargetBean.sleepTarget.trim().toInt(), this)
        }
        if (!TextUtils.isEmpty(mTargetBean.sleepTarget) && mTargetBean.sleepTarget.trim()
                .toInt() != 0
        ) {
            val timeForStyleByMinutes =
                TimeUtils.getTimeForStyleByMinutes(mTargetBean.sleepTarget.trim().toInt())
            picker.setDefaultValue(timeForStyleByMinutes[0], timeForStyleByMinutes[1])
        } else {
            picker.setDefaultValue("08", "00")
        }
        picker.show()
    }

    @SuppressLint("SetTextI18n")
    private fun createCaloriesTargetDialog(/*position: Int*/) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.consumeTarget = item.toString()
            binding.tvCaloriesRight.text = "${mTargetBean.consumeTarget} ${getString(R.string.unit_calories)}"
        }
        picker.setRangeStep(
            Constant.CALORIE_TARGET_MIN_VALUE / Constant.CALORIE_TARGET_SCALE_VALUE,
            Constant.CALORIE_TARGET_MAX_VALUE / Constant.CALORIE_TARGET_SCALE_VALUE,
            Constant.CALORIE_TARGET_SCALE_VALUE
        )
        if (mTargetBean.consumeTarget != "") {
            picker.setDefaultValue(mTargetBean.consumeTarget)
        } else {
            picker.setDefaultValue(Constant.CALORIE_TARGET_DEFAULT_VALUE)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshDistance(data: String, unit: String) {
        if (!TextStringUtils.isNull(data)) {
            binding.tvDistanceRight.text = "$data $unit"
        }
    }

    private fun createDistanceTargetDialog(/*position: Int*/) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.distanceTarget = item.toString()
            //公制
            if (mTargetBean.unit == "0") {
//                resetRvData(
//                    mTargetBean.distanceTarget,
//                    position,
//                    getString(R.string.unit_distance_0)
//                )
                distanceTemp = mTargetBean.distanceTarget
                refreshDistance(distanceTemp, getString(R.string.unit_distance_0))
            }
            //英制
            else {
//                resetRvData(
//                    mTargetBean.distanceTarget,
//                    position,
//                    getString(R.string.unit_distance_1)
//                )
                distanceTemp = mTargetBean.distanceTarget
                refreshDistance(distanceTemp, getString(R.string.unit_distance_1))
            }
        }
        picker.setRange(Constant.DISTANCE_TARGET_MIN_VALUE, Constant.DISTANCE_TARGET_MAX_VALUE, 1)
        if (mTargetBean.distanceTarget != "") {
            Log.i(
                TAG,
                "createDistanceTargetDialog() userBean.distanceTarget = " + mTargetBean.distanceTarget
            )
            picker.setDefaultValue(mTargetBean.distanceTarget)
        } else {
            Log.i(TAG, "createDistanceTargetDialog() userBean.distanceTarget == null")
            picker.setDefaultValue(Constant.DISTANCE_TARGET_DEFAULT_VALUE)
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()

    }

    @SuppressLint("SetTextI18n")
    private fun createStepGoalDialog(/*position: Int*/) {
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            mTargetBean.sportTarget = item.toString()
//            resetRvData(mTargetBean.sportTarget, position, getString(R.string.unit_step))
            binding.tvStepRight.text = "${mTargetBean.sportTarget} ${getString(R.string.unit_steps)}"
        }
        picker.setRangeStep(
            Constant.STEP_TARGET_MIN_VALUE / Constant.STEP_TARGET_SCALE_VALUE,
            Constant.STEP_TARGET_MAX_VALUE / Constant.STEP_TARGET_SCALE_VALUE,
            Constant.STEP_TARGET_SCALE_VALUE
        )

        Log.i(TAG, "createStepGoalDialog userBean.stepTarget = " + mTargetBean.sportTarget)

        if (mTargetBean.sportTarget != "") {
            picker.setDefaultValue(mTargetBean.sportTarget)
        } else {
            picker.setDefaultValue(Constant.STEP_TARGET_DEFAULT_VALUE)
        }

        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    @SuppressLint("SetTextI18n")
    private fun refreshWeight(data: String, unit: String) {
        if (!TextStringUtils.isNull(data)) {
            binding.tvWeightRight.text = "$data $unit"
        }
    }

    private fun createWeightDialog(default: String/*, positions: Int*/) {
        if (TextUtils.isEmpty(mTargetBean.unit)) {
            ToastUtils.showToast(getString(R.string.user_info_unit_is_null_tips))
            return
        }
        val picker = NumberPicker(this)
        picker.setOnNumberPickedListener { pos, item ->
            var unit = ""
            var sum = "0"
            //公制
            if (mTargetBean.unit.trim().toInt() == 0) {
                mUserBean.weight = item.toString()
                mUserBean.britishWeight = UnitConverUtils.kGToLbString(item.toString())
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
//            resetRvData(sum, positions, unit)
            weightTemp = sum
            refreshWeight(weightTemp, unit)
        }

        //公制
        if (mTargetBean.unit.trim().toInt() == 0) {
            picker.setRange(
                Constant.WEIGHT_TARGET_METRIC_MIN_VALUE,
                Constant.WEIGHT_TARGET_METRIC_MAX_VALUE,
                1
            )
        }
        //英制
        else {
            picker.setRange(
                Constant.WEIGHT_TARGET_BRITISH_MIN_VALUE,
                Constant.WEIGHT_TARGET_BRITISH_MAX_VALUE,
                1
            )
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

    @SuppressLint("SetTextI18n")
    private fun refreshHeight(data: String, unit: String) {
        if (!TextStringUtils.isNull(data)) {
            binding.tvHeightRight.text = "$data $unit"
        }
    }

    private fun createHeightDialog(defaultParam: String/*, positions: Int*/) {
        if (TextUtils.isEmpty(mTargetBean.unit)) {
            ToastUtils.showToast(getString(R.string.user_info_unit_is_null_tips))
            return
        }
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
        val data = mutableListOf<MetricSystemBean>()

        //公制
        if (mTargetBean.unit.trim().toInt() == 0) {
            for (i in Constant.HEIGHT_TARGET_METRIC_MIN_VALUE..Constant.HEIGHT_TARGET_METRIC_MAX_VALUE) {
                data.add(MetricSystemBean("$i"))
            }

            val defaultPosition = data.indexOfFirst { it.name == default }

            picker.setOnOptionPickedListener { position, item ->
                mUserBean.height = item.toString()
//                resetRvData(mUserBean.height, positions, getString(R.string.unit_height_0))
                heightTemp = item.toString()
                refreshHeight(heightTemp, getString(R.string.unit_height_0))
            }

            picker.setDefaultPosition(defaultPosition)

        }
        //英制
        else {
            for (i in Constant.HEIGHT_TARGET_BRITISH_MIN_VALUE..Constant.HEIGHT_TARGET_BRITISH_MAX_VALUE) {
                data.add(MetricSystemBean("$i"))
            }

            val defaultPosition = data.indexOfFirst {
                it.name == default
            }

            picker.setOnOptionPickedListener { position, item ->
                mUserBean.height = UnitConverUtils.inchToCmString(item.toString())
                mUserBean.britishHeight = item.toString()
//                resetRvData((item.toString()), positions, getString(R.string.unit_height_1))
                heightTemp = item.toString()
                refreshHeight(heightTemp, getString(R.string.unit_height_1))
            }

            picker.setDefaultPosition(defaultPosition)

        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.setData(data)
        picker.setShowModel(0)
        picker.rightLabel.visibility = View.GONE
        picker.show()
    }

    class MetricSystemBean(var name: String) : TextProvider, Serializable {
        override fun provideText(): String {
            return name
        }

        override fun toString(): String {
            return "$name"
        }
    }

    private fun createSexDialog(/*position: Int*/) {
        val picker = SexPicker(this, true)
        Log.i(TAG, "createSexDialog()")
        picker.setOnOptionPickedListener { pos, item ->
            Log.i(TAG, "createSexDialog() item = $item")
            mUserBean.sex =
                if (item.toString() == getString(R.string.user_info_female)) "1" else "0"
            Log.i(TAG, "createSexDialog = mUserBean.sex = " + mUserBean.sex)
//            resetRvData(item.toString(), position, "")
            binding.tvSexRight.text = item.toString()
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        if (mUserBean.sex.isNotEmpty()) {
            picker.setDefaultValue(
                if (mUserBean.sex == "1") getString(R.string.user_info_female) else getString(
                    R.string.user_info_male
                )
            )
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

    private fun createDateDialog(/*position: Int*/) {
        val picker = BirthdayPicker(this)
        picker.setOnDatePickedListener { year, month, day ->
            mUserBean.birthDate = getDate(year, month, day)
//            resetRvData(mUserBean.birthDate, position, "")
            binding.tvBirthDayRight.text = mUserBean.birthDate
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
            picker.setDefaultValue(
                TimeUtils.getYearFromDate(date),
                TimeUtils.getMonthFromDate(date),
                TimeUtils.getDayFromDate(date)
            )
        }
        picker.setBackground(ContextCompat.getDrawable(this, R.drawable.dialog_bg))
        picker.show()
    }

    private fun clickFinishBtn() {
        if (mUserBean.nickname.isNullOrEmpty() || mUserBean.sex.isNullOrEmpty() || mTargetBean.unit.isNullOrEmpty() || mUserBean.birthDate.isNullOrEmpty()
            || mUserBean.height.isNullOrEmpty() || mUserBean.weight.isNullOrEmpty() || mTargetBean.sportTarget.isNullOrEmpty()
            || mTargetBean.distanceTarget.isNullOrEmpty() || mTargetBean.consumeTarget.isNullOrEmpty()
            || mTargetBean.distanceTarget.isNullOrEmpty() || mTargetBean.sleepTarget.isNullOrEmpty()
        ) {
            ToastUtils.showToast(getString(R.string.user_info_ok_btn_tips))
            return
        }
        Log.i(TAG, "clickFinishBtn() mUserBean = $mUserBean")
        Log.i(TAG, "clickFinishBtn() mTargetBean = $mTargetBean")
        dialog = DialogUtils.dialogShowLoad(this)
        viewModel.inputUserInfo(mUserBean, mTargetBean/*,inputUserInfoTrackingLog*/)
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
            .start(this@UserInfoActivity)
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

    //region 屏蔽返回键
    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        delAllCacheImg()
    }

}