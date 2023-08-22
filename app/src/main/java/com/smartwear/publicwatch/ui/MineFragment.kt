package com.smartwear.publicwatch.ui

import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSON
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.Gson
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.databinding.FragmentMineBinding
import com.smartwear.publicwatch.databinding.ItemUserInfoBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.expansion.postDelay
import com.smartwear.publicwatch.ui.adapter.CommonAdapter
import com.smartwear.publicwatch.ui.debug.DebugActivity
import com.smartwear.publicwatch.ui.device.bean.DeviceSettingBean
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.ui.user.*
import com.smartwear.publicwatch.ui.user.bean.UserBean
import com.smartwear.publicwatch.ui.user.bean.UserLocalData
import com.smartwear.publicwatch.utils.*
import com.smartwear.publicwatch.view.wheelview.widget.MapTypePicker
import com.smartwear.publicwatch.view.wheelview.widget.WearPicker
import com.smartwear.publicwatch.viewmodel.UserModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MineFragment : BaseFragment<FragmentMineBinding, UserModel>(FragmentMineBinding::inflate, UserModel::class.java) {
    private val TAG: String = MineFragment::class.java.simpleName
    private lateinit var mUserBean: UserBean
    private val list: MutableList<MutableMap<String, *>> = ArrayList()

    //产品功能列表
    private var deviceSettingBean: DeviceSettingBean? = null

    override fun setTitleId(): Int {
        return binding.topView.id
    }

    override fun initView() {
        if (this.activity == null) {
            return
        }
        AppUtils.registerEventBus(this)
        initImmersionBar()


        binding.ivMineAvatar.setOnClickListener {
            if (AppUtils.isBetaApp()) {
                DialogUtils.showDialogTwoBtn(
                    ActivityUtils.getTopActivity(),
                    null,
                    "是否进入DEBUG页面？",
                    BaseApplication.mContext.getString(R.string.dialog_cancel_btn),
                    BaseApplication.mContext.getString(R.string.dialog_confirm_btn),
                    object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            startActivity(Intent(requireActivity(), DebugActivity::class.java))
                        }

                        override fun OnCancel() {}
                    }
                ).show()
            }
        }
    }

    override fun initData() {
        if (this.context == null) {
            return
        }
        deviceSettingBean = JSON.parseObject(
            SpUtils.getValue(
                SpUtils.DEVICE_SETTING,
                ""
            ), DeviceSettingBean::class.java
        )
        getUserInfoData()
        initRv()
    }

    private fun getUserInfoData() {
        mUserBean = UserBean().getData()
        com.blankj.utilcode.util.LogUtils.d("getUserInfoData user -->${GsonUtils.toJson(mUserBean)}")
        val flag = (!TextUtils.isEmpty(mUserBean.head))
        if (flag) {
            GlideApp.with(requireActivity()).load(mUserBean.head)
                .into(binding.ivMineAvatar)
        }

        if (mUserBean != null) {
            binding.tvMineName.text = mUserBean.nickname
            val UserID = SpUtils.getValue(SpUtils.USER_ID, "")
            ("ID:${AppUtils.encryptionUid(UserID)}").also { binding.tvMineId.text = it }
        }
    }

    private fun initRv() {
        fillData()
        binding.rcvMine.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    override fun onResume() {
        super.onResume()
        postDelay(200) {
            getUserInfoData()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMsg(event: EventMessage) {
        when (event.action) {
            EventAction.ACTION_REF_DEVICE_SETTING -> {
                deviceSettingBean = JSON.parseObject(
                    SpUtils.getValue(
                        SpUtils.DEVICE_SETTING,
                        ""
                    ), DeviceSettingBean::class.java
                )
                fillData()
            }
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.mineInfoNameList)
        val imgs = resources.obtainTypedArray(R.array.mineInfoImgList)
        val userLocalData = Gson().fromJson(SpUtils.getValue(SpUtils.USER_LOCAL_DATA, ""), UserLocalData::class.java)

        for (i in 0 until imgs.length()) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = texts[i]
            map["img"] = imgs.getResourceId(i, 0)
            map["right"] = ""
            if (texts[i] == getString(R.string.map)) {
                map["right"] = if (AppUtils.isEnableGoogleMap()) getString(R.string.google) else getString(R.string.gothe)
            } else if (texts[i] == getString(R.string.wear)) {
                map["right"] = if (userLocalData != null) userLocalData.getWearLeftRightIndexStr(context) else getString(R.string.left_hand)
            }
            if (texts[i] == getString(R.string.device_fragment_set_goal)) {
                if (deviceSettingBean != null) {
                    if (!deviceSettingBean!!.settingsRelated.step_goal &&
                        !deviceSettingBean!!.settingsRelated.distance_target &&
                        !deviceSettingBean!!.settingsRelated.calorie_goal &&
                        !deviceSettingBean!!.settingsRelated.sleep_goal
                    ) continue //目标设置全部禁用 不加载目标设置

                } else {
                    //未配置 不加载目标设置
                    continue
                }
            }
            if (texts[i] == getString(R.string.wear)) {
                if (deviceSettingBean != null) {
                    if (!deviceSettingBean!!.settingsRelated.wearing_method) continue //未启用 不加载佩戴方式
                } else {
                    //未配置 不加载佩戴方式
                    continue
                }
            }
            if (texts[i] == getString(R.string.data_reduction)) {
                if (deviceSettingBean != null) {
                    if (!deviceSettingBean!!.settingsRelated.GoogleFit
                        && !deviceSettingBean!!.settingsRelated.Strava
                    ) continue //GoogleFit & Strava 未启用 不加载数据授权
                } else {
                    //未配置 数据授权
                    continue
                }
            }
            list.add(map)
        }
        imgs.recycle()
        binding.rcvMine.adapter?.notifyDataSetChanged()
    }

    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemUserInfoBinding {
                return ItemUserInfoBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemUserInfoBinding, t: MutableMap<String, *>, position: Int) {
                v.ivItemLeft.setImageResource(t["img"] as Int)
                v.tvItemLeft.text = "${t["content"]}"
                v.tvItemRight.text = "${t["right"]}"

                if (position == (list.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }

                v.cslItemUserInfoParent.setOnClickListener {
                    try {
                        when (v.tvItemLeft.text.toString().trim()) {
                            getString(R.string.user_info_title) -> {
                                startActivity(Intent(context, PersonalInfoActivity::class.java))
                            }
                            getString(R.string.unit_setting) -> {
                                startActivity(Intent(context, UnitSettingActivity::class.java))
                            }
                            getString(R.string.device_fragment_set_goal) -> {
                                startActivity(Intent(context, TargetSettingActivity::class.java))
                            }
                            getString(R.string.map) -> {
                                createMapPickDialog(position)
                            }
                            getString(R.string.wear) -> {
                                createWearDialog(position)
                            }
                            getString(R.string.question_feedback) -> {
                                startActivity(Intent(context, QuestionFeedbackActivity::class.java))
                            }
                            getString(R.string.data_reduction) -> {
                                startActivity(Intent(context, DataReductionActivity::class.java))
                            }
                            getString(R.string.common_question_help) -> {
                                startActivity(Intent(context, QAActivity::class.java))
                            }
                            getString(R.string.accounts_ecurity) -> {
                                startActivity(Intent(context, AccountSecurityActivity::class.java))
                            }
                            getString(R.string.about) -> {
                                startActivity(Intent(context, AboutActivity::class.java))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun createWearDialog(position: Int) {
        if (context == null) return
        val picker = WearPicker(activity)
        var userLocalData = Gson().fromJson(SpUtils.getValue(SpUtils.USER_LOCAL_DATA, ""), UserLocalData::class.java)
        if (userLocalData == null) {
            userLocalData = UserLocalData()
            userLocalData.userId = SpUtils.getValue(SpUtils.USER_ID, "")
            userLocalData.userMapIndex = if (AppUtils.isEnableGoogleMap()) 0 else 1
        }
        picker.setDefaultPosition(userLocalData.wearLeftRightIndex)
        picker.setOnOptionPickedListener { pos, item ->
            userLocalData.wearLeftRightIndex = pos
            SpUtils.setValue(SpUtils.USER_LOCAL_DATA, Gson().toJson(userLocalData))
            resetRvData(item.toString(), position, "")
        }
        picker.setBackground(ContextCompat.getDrawable(requireActivity(), R.drawable.dialog_bg))
        picker.show()
    }

    private fun resetRvData(rightStr: String, position: Int, unit: String) {
        val map: MutableMap<String, Any> = HashMap()
        map["img"] = list[position]["img"] as Int
        map["right"] = rightStr
        map["content"] = list[position]["content"] as String
        map["unit"] = unit
        list[position] = map
        binding.rcvMine.adapter?.notifyItemChanged(position)
    }

    private fun createMapPickDialog(position: Int) {
        if (context == null) return
        val picker = MapTypePicker(activity)
        var userLocalData = Gson().fromJson(SpUtils.getValue(SpUtils.USER_LOCAL_DATA, ""), UserLocalData::class.java)
        if (userLocalData == null) {
            userLocalData = UserLocalData()
            userLocalData.userId = SpUtils.getValue(SpUtils.USER_ID, "")
            userLocalData.userMapIndex = if (AppUtils.isEnableGoogleMap()) 0 else 1
        }
        picker.setDefaultPosition(userLocalData.userMapIndex)
        picker.setOnOptionPickedListener { pos, item ->
            userLocalData.userMapIndex = pos
            SpUtils.setValue(SpUtils.USER_LOCAL_DATA, Gson().toJson(userLocalData))
            resetRvData(item.toString(), position, "")
            EventBus.getDefault().post(EventMessage(EventAction.ACTION_MAP_CHANGE))
        }
        picker.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.dialog_bg))
        picker.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUtils.unregisterEventBus(this)
    }

}