package com.smartwear.xzfit.ui.sport

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.*
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.google.android.material.tabs.TabLayout
import com.zhapp.ble.parsing.SportParsing
import com.zhapp.ble.utils.UnitConversionUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.ActivitySportShareBinding
import com.smartwear.xzfit.databinding.ItemSportShareCustomBgBinding
import com.smartwear.xzfit.databinding.ItemSportShareCustomSytleBinding
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.ui.adapter.CommonAdapter
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.user.bean.UserBean
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.PermissionUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.utils.manager.DevSportManager
import com.smartwear.xzfit.viewmodel.SportModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Android on 2021/10/14.
 */
class SportShareActivity : BaseActivity<ActivitySportShareBinding, SportModel>(
    ActivitySportShareBinding::inflate, SportModel::class.java
), View.OnClickListener {
    private var TAG = "SportShareActivity"

    //0 ->轨迹  1 ->数据长图  2 ->自定义样式
    private var dataType = 0

    private var sportModleInfo: SportModleInfo? = null

    //数据bitmap
    private lateinit var mBms: ArrayList<Bitmap>

    //用户信息布局
    private lateinit var userLayout: ConstraintLayout

    //自定义布局
    private lateinit var customLayut: ConstraintLayout

    //最终分享照片储存地址
    private val saveShareImgPath =
        PathUtils.getExternalAppCachePath() + File.separator + "image" + File.separator
    private val saveImgName = "sport_share.jpg"

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.llTop.id
    }

    override fun initView() {
        super.initView()

        sportModleInfo = viewModel.sportLiveData.getSportModleInfo().value
        if (sportModleInfo == null) {
            finish()
            return
        }
        binding.userLayout.tvSportType.text = SportTypeUtils.getSportTypeName(
            sportModleInfo!!.dataSources,
            sportModleInfo!!.exerciseType
        )
        binding.customLayout.tvCustomSportName.text = SportTypeUtils.getSportTypeName(
            sportModleInfo!!.dataSources,
            sportModleInfo!!.exerciseType
        )
        val t = TimeUtils.millis2String(sportModleInfo!!.sportTime * 1000, com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy/MM/dd HH:mm")).split(" ")
        binding.userLayout.tvDate.text = SpannableStringTool.get()
            .append(t.get(0))
            .setFontSize(16f)
            .setForegroundColor(Color.WHITE)
            .append("   ")
            .append(t.get(1))
            .setFontSize(14f)
            .setForegroundColor(Color.WHITE)
            .create()
        binding.customLayout.tvCustomDate.text = TimeUtils.millis2String(sportModleInfo!!.sportTime * 1000, com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat("MM/dd"))

        binding.userLayout.tvSportShareTitle.text = AppUtils.biDiFormatterStr(
            StringBuilder().append(getString(R.string.main_app_name)).append(" ")
                .append(getString(R.string.sport_info_wear)).toString()
        )
        //用户信息
        val bean = UserBean().getData()
        var flag = (bean != null && !TextUtils.isEmpty(bean.head))
        if (flag) {
            GlideApp.with(this).load(bean.head)
                .error(R.mipmap.ic_mine_avatar)
                .placeholder(R.mipmap.ic_mine_avatar)
                .into(binding.userLayout.ivHead)
            GlideApp.with(this).load(bean.head)
                .error(R.mipmap.ic_mine_avatar)
                .placeholder(R.mipmap.ic_mine_avatar)
                .into(binding.customLayout.ivCustomHead)
        } else {
            GlideApp.with(this).load(R.mipmap.ic_mine_avatar)
                .into(binding.userLayout.ivHead)
            GlideApp.with(this).load(R.mipmap.ic_mine_avatar)
                .into(binding.customLayout.ivCustomHead)
        }
        binding.userLayout.tvName.text = AppUtils.biDiFormatterStr(
            if (!TextUtils.isEmpty(bean.nickname)) bean.nickname else getString(R.string.main_app_name)
        )
        binding.customLayout.tvCustomName.text = AppUtils.biDiFormatterStr(
            if (!TextUtils.isEmpty(bean.nickname)) bean.nickname else getString(R.string.main_app_name)
        )

        setViewsClickListener(this, binding.ivBack, binding.btnShare)
        if (AppUtils.isEnableGoogleMap() && !AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
            binding.tabLayout.removeTabAt(0)
            dataType = 1
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    dataType = tab.position + 1
                    changUi()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        } else {
            if (sportModleInfo!!.dataSources == 2 &&
                (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt())
                        || SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                        || sportModleInfo!!.exerciseType.toInt() == 200
                        )
            ) {
                binding.tabLayout.removeTabAt(0)
                dataType = 1
                binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        dataType = tab.position + 1
                        changUi()
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            } else {
                binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab) {
                        dataType = tab.position
                        changUi()
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                })
            }
        }
        initCustom()
        userLayout = binding.userLayout.root
        customLayut = binding.customLayout.root
        changUi()

        //拍照
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            takePictureUri?.let {
                //加载图片
                customBgData.forEach { it.isSelected = false }
                binding.customLayout.rvCustomBg.adapter?.notifyDataSetChanged()
                GlideApp.with(this).load(it).into(binding.customLayout.ivBg)
            }
        }
    }

    private fun initCustom() {
        initCustomBg()
        initCustomStyle()
        initStyle()
    }


    data class CustomBg(var imgId: Int, var isSelected: Boolean)

    //region 自定义背景图选择
    private fun initCustomBg() {
        binding.customLayout.rvCustomBg.apply {
            val manager = GridLayoutManager(context, 2)
            manager.orientation = LinearLayoutManager.HORIZONTAL
            layoutManager = manager
            setHasFixedSize(true)
            adapter = initCustomBgAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.apply {
                        right = ConvertUtils.dp2px(10F)
                        if (TextUtils.equals(Locale.getDefault().language, "ar")) left = ConvertUtils.dp2px(10F)
                        bottom = ConvertUtils.dp2px(11F)
                    }
                }
            })
        }
    }

    private val customBgData by lazy { mutableListOf<CustomBg>() }

    private fun initCustomBgAdapter(): CommonAdapter<CustomBg, ItemSportShareCustomBgBinding> {
        return object : CommonAdapter<CustomBg, ItemSportShareCustomBgBinding>(customBgData) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemSportShareCustomBgBinding {
                return ItemSportShareCustomBgBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemSportShareCustomBgBinding, t: CustomBg, position: Int) {
                if (t.imgId != -1) {
                    GlideApp.with(this@SportShareActivity).load(t.imgId).into(v.ivIcon)
                } else {
                    GlideApp.with(this@SportShareActivity).load(R.mipmap.sport_share_custom_camera).into(v.ivIcon)
                }
                v.ivSelected.visibility = if (t.isSelected) View.VISIBLE else View.GONE

                v.root.setOnClickListener {
                    if (t.imgId == -1) {
                        showCameraDialog()
                    } else {
                        if (!customBgData.get(position).isSelected) {
                            customBgData.forEach { it.isSelected = false }
                            customBgData.get(position).isSelected = true
                            notifyDataSetChanged()
                            GlideApp.with(this@SportShareActivity).load(t.imgId).into(binding.customLayout.ivBg)
                        }
                    }
                }

            }

        }
    }

    // 弹框的信息
    private var dialogAvatar: Dialog? = null

    //拍照
    private var takePictureUri: Uri? = null
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private val RESULT_GET_PHOTOGRAPH_CODE = 0x161

    private fun showCameraDialog() {
        PermissionUtils.checkRequestPermissions(
            this.lifecycle,
            getString(R.string.permission_sdcard),
            PermissionUtils.PERMISSION_GROUP_SDCARD
        ) {
            if (dialogAvatar != null && dialogAvatar!!.isShowing) {
                return@checkRequestPermissions
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
                    takePictures()
                }
            }
            view.findViewById<View>(R.id.albums).setOnClickListener {
                dialogAvatar?.dismiss()
                getPhotograph()
            }
            view.findViewById<View>(R.id.cancel).setOnClickListener { dialogAvatar?.dismiss() }
            dialogAvatar?.onWindowAttributesChanged(wl)
            dialogAvatar?.setCanceledOnTouchOutside(true)
            dialogAvatar?.show()
        }
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_GET_PHOTOGRAPH_CODE -> {
                if (resultCode == RESULT_OK) {
                    if (data == null || data.data == null) {
                        com.smartwear.xzfit.utils.LogUtils.d(TAG, "获取相册异常：data == null", true)
                        return
                    }
                    try {
                        val resFile = UriUtils.uri2File(data.data)
                        // 跳转到图片裁剪页面，copy相册文件到缓存路径，防止裁剪异常导致相册文件出错
                        val destFile = AppUtils.copyPhotograph(resFile)
                        if (destFile == null) {
                            com.smartwear.xzfit.utils.LogUtils.d(TAG, "获取相册异常：复制相册图片失败！", true)
                            ToastUtils.showToast(R.string.img_fail_tip)
                            return
                        }
                        //加载图片
                        customBgData.forEach { it.isSelected = false }
                        binding.customLayout.rvCustomBg.adapter?.notifyDataSetChanged()
                        GlideApp.with(this).load(destFile).into(binding.customLayout.ivBg)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        com.smartwear.xzfit.utils.LogUtils.d(TAG, "获取相册异常：$e", true)
                        ToastUtils.showToast(R.string.img_fail_tip)
                    }
                }
            }
        }
    }

    //endregion

    data class StyleItem(var styleType: Int, var imgId: Int, var isSelected: Boolean)

    //region 自定义文字样式选择
    private fun initCustomStyle() {
        binding.customLayout.rvCustomData.apply {
            val manager = GridLayoutManager(context, 1)
            manager.orientation = LinearLayoutManager.HORIZONTAL
            layoutManager = manager
            setHasFixedSize(true)
            adapter = initCustomDataAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.apply {
                        right = ConvertUtils.dp2px(10F)
                        if (TextUtils.equals(Locale.getDefault().language, "ar")) left = ConvertUtils.dp2px(10F)
                        bottom = ConvertUtils.dp2px(11F)
                    }
                }
            })
        }
    }

    private val customDataData by lazy { mutableListOf<StyleItem>() }

    private fun initCustomDataAdapter(): CommonAdapter<StyleItem, ItemSportShareCustomBgBinding> {
        return object : CommonAdapter<StyleItem, ItemSportShareCustomBgBinding>(customDataData) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemSportShareCustomBgBinding {
                return ItemSportShareCustomBgBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemSportShareCustomBgBinding, t: StyleItem, position: Int) {
                GlideApp.with(this@SportShareActivity).load(t.imgId).into(v.ivIcon)
                v.ivSelected.visibility = if (t.isSelected) View.VISIBLE else View.GONE
                v.root.setOnClickListener {
                    if (!customDataData.get(position).isSelected) {
                        customDataData.forEach { it.isSelected = false }
                        customDataData.get(position).isSelected = true
                        notifyDataSetChanged()
                        styleType = t.styleType
                        refStyleByType()
                    }
                }

            }
        }

    }

    //endregion

    data class StyleBean(var title: String, var value: String, var unit: String)


    //region 自定文字样式布局
    private var styleType = 1
    private val styleData by lazy { mutableListOf<StyleBean>() }
    private fun initStyle() {
        binding.customLayout.rvStyle.apply {
            val spanCount = when (styleType) {
                1, 5, 7 -> 1
                3 -> 2
                2, 4, 6 -> 3
                else -> 1
            }
            val manager = GridLayoutManager(context, spanCount)
            manager.orientation = LinearLayoutManager.VERTICAL
            layoutManager = manager
            setHasFixedSize(true)
            adapter = initStyleDataAdapter()
            if (itemDecorationCount > 0) {
                for (i in 0 until itemDecorationCount) {
                    removeItemDecorationAt(i)
                }
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.apply {
                        left = ConvertUtils.dp2px(10F)
                        if (TextUtils.equals(Locale.getDefault().language, "ar")) right = ConvertUtils.dp2px(10F)
                        bottom = ConvertUtils.dp2px(11F)
                    }
                }
            })
        }
    }

    private fun initStyleDataAdapter(): CommonAdapter<StyleBean, ItemSportShareCustomSytleBinding> {
        return object : CommonAdapter<StyleBean, ItemSportShareCustomSytleBinding>(styleData) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemSportShareCustomSytleBinding {
                return ItemSportShareCustomSytleBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemSportShareCustomSytleBinding, t: StyleBean, position: Int) {
                v.tvItemValue.typeface = FontProvider.getRobotoBoldCondensedItalic(this@SportShareActivity)
                if (t.title.isNotEmpty()) {
                    v.tvItemTitle.visibility = View.VISIBLE
                    v.tvItemTitle.text = t.title
                } else {
                    v.tvItemTitle.visibility = View.GONE
                    v.tvItemTitle.text = ""
                }
                if (t.value.isNotEmpty()) {
                    v.tvItemValue.visibility = View.VISIBLE
                    v.tvItemValue.text = t.value
                } else {
                    v.tvItemValue.visibility = View.GONE
                    v.tvItemValue.text = ""
                }
                if (t.unit.isNotEmpty()) {
                    v.tvItemUnit.visibility = View.VISIBLE
                    v.tvItemUnit.text = t.unit
                } else {
                    v.tvItemUnit.visibility = View.GONE
                    v.tvItemUnit.text = ""
                }
            }
        }
    }

    private fun refStyleByType() {
        styleData.clear()
        styleData.apply {
            when (styleType) {
                1 -> {
                    addDistanceBean(this)
                }
                2 -> {
                    addDistanceBean(this)
                    add(StyleBean("", "", ""))
                    addTimeBean(this)
                }
                3 -> {
                    addDistanceBean(this, true)
                    addPacesBean(this, true)
                    addTimeBean(this, true)
                    addCaloriesBean(this, true)
                }
                4 -> {
                    addDistanceBean(this)
                    addPacesBean(this)
                    addTimeBean(this)
                }
                5 -> {
                    addTimeBean(this)
                }
                6 -> {
                    addCaloriesBean(this)
                    add(StyleBean("", "", ""))
                    addTimeBean(this)
                }
                7 -> {
                    addCaloriesBean(this)
                }
            }
        }
        initStyle()
        binding.customLayout.rvStyle.adapter?.notifyDataSetChanged()
    }

    private fun addDistanceBean(list: MutableList<StyleBean>, isShowTitle: Boolean = false) {
        var unit = if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
            R.string.unit_distance_1
        )
        var value = ""
        var m = 0
        if (sportModleInfo!!.dataSources == 0) {
            m = sportModleInfo!!.exerciseApp!!.sportsMileage.toFloat().toInt()
        } else if (sportModleInfo!!.dataSources == 2) {
            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
            ) {
                if (sportModleInfo!!.exerciseOutdoor != null) {
                    m = sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat().toInt()
                }
            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
            ) {
                if (sportModleInfo!!.exerciseIndoor != null) {
                    m = sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat().toInt()
                }
            } else if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                if (sportModleInfo!!.exerciseSwimming != null) {
                    m = sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat().toInt()
                }
                value = (m / (if (AppUtils.getDeviceUnit() == 0) 1f else 0.3048f)).toInt().toString()
                unit = if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_meter) else getString(R.string.unit_ft)
                return
            }
        }
        value = UnitConversionUtils.bigDecimalFormat(
            (m / 1000f /
                    (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
        ).toString()
        list.add(StyleBean(if (isShowTitle) getString(R.string.healthy_sports_list_distance) else "", value, unit))
    }

    private fun addTimeBean(styleBeans: MutableList<StyleBean>, isShowTitle: Boolean = false) {
        styleBeans.add(
            StyleBean(
                if (isShowTitle) getString(R.string.sport_all_time) else "",
                com.smartwear.xzfit.utils.TimeUtils.millis2String(sportModleInfo!!.sportDuration * 1000L),
                ""
            )
        )
    }

    private fun addPacesBean(styleBeans: MutableList<StyleBean>, isShowTitle: Boolean = false) {
        var value = ""
        if (sportModleInfo!!.dataSources == 0) {
            value = DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.avgPace.toInt())
            /* "${sportModleInfo!!.exerciseApp!!.avgPace.toInt() / 60}'${sportModleInfo!!.exerciseApp!!.avgPace.toInt() % 60}\""*/
        } else if (sportModleInfo!!.dataSources == 2) {
            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
            ) {
                if (sportModleInfo!!.exerciseOutdoor != null) {
                    value = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat()
                    )
                }
            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
            ) {
                if (sportModleInfo!!.exerciseIndoor != null) {
                    value = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat()
                    )
                }
            } else {
                if (sportModleInfo!!.exerciseSwimming != null) {
                    value = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat()
                    )
                }
            }
        }
        styleBeans.add(
            StyleBean(
                if (isShowTitle) getString(R.string.sport_minkm) else "", value, "/${
                    if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                        R.string.unit_distance_1
                    )
                }"
            )
        )
    }

    private fun addCaloriesBean(styleBeans: MutableList<StyleBean>, isShowTitle: Boolean = false) {
        styleBeans.add(
            StyleBean(
                if (isShowTitle) getString(R.string.healthy_sports_list_calories) else "",
                "${sportModleInfo!!.burnCalories}",
                getString(R.string.unit_calories)
            )
        )
    }
    //endregion


    /**
     * 改变分享内容
     * */
    private fun changUi() {
        //移除所有view
        binding.shareDataLayout.removeAllViews()
        if (dataType == 0 || dataType == 1) {
            //加用户信息
            binding.userLayout.root.visibility = View.VISIBLE
            binding.shareDataLayout.addView(userLayout)
            //加数据
            when (dataType) {
                0 -> {
                    //轨迹
                    if (::mBms.isInitialized) {
                        val img = AppCompatImageView(this)
                        img.setImageBitmap(
                            ImageUtils.toRoundCorner(
                                mBms[0],
                                ConvertUtils.dp2px(14F).toFloat()
                            )
                        )
                        binding.shareDataLayout.addView(img)
                        //GlideApp.with(this).load(mBms.get(0)).into(img)
                    }
                }
                1 -> {
                    //长图
                    if (::mBms.isInitialized) {
                        for (i in 0 until mBms.size) {
                            val img = AppCompatImageView(this)
                            img.setImageBitmap(
                                if (i == 0)
                                    ImageUtils.toRoundCorner(mBms[i], ConvertUtils.dp2px(14F).toFloat())
                                else
                                    mBms[i]
                            )
                            binding.shareDataLayout.addView(img)
                        }
                    }

                }
            }
        } else {
            binding.customLayout.root.visibility = View.VISIBLE
            binding.shareDataLayout.addView(customLayut)
            LogUtils.d("加载自定义")
        }

    }


    override fun initData() {
        super.initData()
        customBgData.apply {
            add(CustomBg(-1, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_9, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_1, true))
            add(CustomBg(R.mipmap.sport_share_custom_bg_5, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_2, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_6, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_3, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_7, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_4, false))
            add(CustomBg(R.mipmap.sport_share_custom_bg_8, false))
        }
        binding.customLayout.rvCustomBg.adapter?.notifyDataSetChanged()



        customDataData.apply {
            if (sportModleInfo == null) {
                sportModleInfo = viewModel.sportLiveData.getSportModleInfo().value
            }
            if (
                sportModleInfo!!.dataSources == 0 ||
                (sportModleInfo!!.dataSources == 2 &&
                        (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())
                                ))

            ) { //带距离的运动
                add(StyleItem(1, R.mipmap.sport_share_custom_style_1, true))
                add(StyleItem(2, R.mipmap.sport_share_custom_style_2, false))
                add(StyleItem(3, R.mipmap.sport_share_custom_style_3, false))
                add(StyleItem(4, R.mipmap.sport_share_custom_style_4, false))
                add(StyleItem(5, R.mipmap.sport_share_custom_style_5, false))
                add(StyleItem(6, R.mipmap.sport_share_custom_style_6, false))
                add(StyleItem(7, R.mipmap.sport_share_custom_style_7, false))
            } else { //不带距离的运动
                add(StyleItem(1, R.mipmap.sport_share_custom_style_1, true))
                add(StyleItem(2, R.mipmap.sport_share_custom_style_2, false))
                add(StyleItem(7, R.mipmap.sport_share_custom_style_7, false))
            }
        }
        binding.customLayout.rvCustomData.adapter?.notifyDataSetChanged()

        styleType = 1
        refStyleByType()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onShareBitmap(event: EventMessage?) {
        if (event == null) return
        if (event.action == EventAction.ACTION_SHARE_SPORT_DATA) {
            LogUtils.e("收到图片")
            mBms = event.obj as ArrayList<Bitmap>
            LogUtils.e("图片数量${mBms.size}")
            changUi()
            EventBus.getDefault().removeStickyEvent(event)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.ivBack.id -> {
                finish()
            }
            binding.btnShare.id -> {
                //移除分享按钮
                binding.shareLayout.removeView(binding.btnShare)
                if (dataType == 0 || dataType == 1) {
                    sharePhoto(ImageUtils.view2Bitmap(binding.shareLayout))
                } else if (dataType == 2) {
                    sharePhoto(ImageUtils.view2Bitmap(binding.customLayout.shareCustomDataLayout))
                }
                binding.shareLayout.addView(binding.btnShare)
                changUi()
            }
        }
    }

    // 分享相片
    private fun sharePhoto(photoUri: Bitmap?) {
        try {
            val filePath = "$saveShareImgPath$saveImgName"
            LogUtils.d("分享图片保存地址： $filePath")
            FileUtils.createFileByDeleteOldFile(filePath)
            val os: OutputStream = FileOutputStream(filePath)
            photoUri?.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            //startActivity(IntentUtils.getShareImageIntent(File(filePath))) 此代码 荣耀9x 等手机分享多次后出现无法继续分享问题
            startActivity(shareImgIntent(UriUtils.file2Uri(FileUtils.getFileByPath(filePath))))
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showToast(getString(R.string.share_failed))
        }
    }

    /**
     * 分享图片
     */
    private fun shareImgIntent(imageUri: Uri): Intent {
        var intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share))
        intent.putExtra(Intent.EXTRA_STREAM, imageUri)
        intent.type = "image/*"
        intent = Intent.createChooser(intent, "")
        return intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.registerEventBus(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
        //回收bitmap
        if (::mBms.isInitialized && mBms.isNotEmpty()) {
            mBms.forEach { m ->
                if (!m.isRecycled) {
                    m.recycle()
                }
            }
            System.gc()
        }
        //清空分享文件
        FileUtils.delete("$saveShareImgPath$saveImgName")
    }
}