package com.smartwear.xzfit.ui.user

import android.app.Dialog
import android.text.TextUtils
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.ui.adapter.CommonAdapter
import com.smartwear.xzfit.utils.ToastUtils
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import androidx.lifecycle.Observer
import com.smartwear.xzfit.databinding.ActivityUnitSettingBinding
import com.smartwear.xzfit.databinding.ItemUnitSettingBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.ui.livedata.RefreshHealthyFragment
import com.smartwear.xzfit.ui.user.bean.TargetBean
import com.smartwear.xzfit.utils.SendCmdUtils
import com.smartwear.xzfit.viewmodel.UserModel


class UnitSettingActivity : BaseActivity<ActivityUnitSettingBinding, UserModel>(ActivityUnitSettingBinding::inflate, UserModel::class.java), View.OnClickListener {
    private val TAG: String = UnitSettingActivity::class.java.simpleName
    private lateinit var mTargetBean: TargetBean
    private val list: MutableList<MutableMap<String, *>> = ArrayList()
    private var dialog: Dialog? = null

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.unit_setting)
        setViewsClickListener(
            this, binding.btnFinish,
            tvTitle!!
        )
        initRv()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            tvTitle!!.id -> {
                finish()
            }
            binding.btnFinish.id -> {
                clickFinishBtn()
            }
        }
    }


    override fun initData() {
        viewModel.uploadTargetInfo.observe(this, Observer {
            if (!TextUtils.isEmpty(it)) {
                dismissDialog()
                when (it) {
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        TargetBean().saveData(mTargetBean)
                        SendCmdUtils.setUserInformation()
                        RefreshHealthyFragment.postValue(true)
                        finish()
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


    private fun initRv() {
        fillData()
        restoreData()
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@UnitSettingActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.unitSettingNameList)
        val descStr = resources.getStringArray(R.array.unitSettingDescList)
        for (index in texts.indices) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = texts[index]
            map["metricUnitDesc"] = "(" + descStr[index * 2] + ")"
            map["imperialUnitDesc"] = "(" + descStr[index * 2 + 1] + ")"
            map["isMetricUnits"] = true
            if (index == 0) {
                map["metricUnitName"] = getString(R.string.user_info_unit_metric)
                map["imperialUnitName"] = getString(R.string.user_info_unit_imperial)
            } else {
                map["metricUnitName"] = getString(R.string.celsius_name)
                map["imperialUnitName"] = getString(R.string.fahrenheit_name)
            }
            list.add(map)
        }
    }

    private fun restoreData() {
        mTargetBean = TargetBean().getData()
        if (mTargetBean != null) {
            list.clear()
            val texts = resources.getStringArray(R.array.unitSettingNameList)
            val descStr = resources.getStringArray(R.array.unitSettingDescList)
            for (index in texts.indices) {
                val map: MutableMap<String, Any> = HashMap()
                map["content"] = texts[index]
                map["metricUnitDesc"] = "(" + descStr[index * 2] + ")"
                map["imperialUnitDesc"] = "(" + descStr[index * 2 + 1] + ")"
                if (index == 0) {
                    map["isMetricUnits"] = if (TextUtils.isEmpty(mTargetBean.unit)) true else mTargetBean.unit == "0"
                    map["metricUnitName"] = getString(R.string.user_info_unit_metric)
                    map["imperialUnitName"] = getString(R.string.user_info_unit_imperial)
                } else {
                    map["isMetricUnits"] = if (TextUtils.isEmpty(mTargetBean.temperature)) true else mTargetBean.temperature == "0"
                    map["metricUnitName"] = getString(R.string.celsius_name)
                    map["imperialUnitName"] = getString(R.string.fahrenheit_name)
                }
                list.add(map)
            }
        }
    }

    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemUnitSettingBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemUnitSettingBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemUnitSettingBinding {
                return ItemUnitSettingBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemUnitSettingBinding, t: MutableMap<String, *>, position: Int) {
                var isMetricUnits: Boolean = t["isMetricUnits"] as Boolean
                v.tvItemUnitSettingTittle.text = "${t["content"]}"
                v.tvItemUnitSettingMetricDesc.text = "${t["metricUnitDesc"]}"
                v.tvItemUnitSettingMetricName.isSelected = isMetricUnits
                v.tvItemUnitSettingImperialName.isSelected = !isMetricUnits
                v.tvItemUnitSettingImperialDesc.text = "${t["imperialUnitDesc"]}"
                v.tvItemUnitSettingMetricName.text = "${t["metricUnitName"]}"
                v.tvItemUnitSettingImperialName.text = "${t["imperialUnitName"]}"

                if (position == (list.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }

                v.llItemUnitSettingMetricParent.setOnClickListener {
                    if (!it.isSelected) {
                        resetRvData(true, position)
                    }
                }
                v.llItemUnitSettingImperialParent.setOnClickListener {
                    resetRvData(false, position)
                }
            }
        }
    }

    private fun resetRvData(isMetricUnits: Boolean, position: Int) {
        if (position == 0) {
            mTargetBean.unit = if (isMetricUnits) "0" else "1"
        } else {
            mTargetBean.temperature = if (isMetricUnits) "0" else "1"
        }
        val map: MutableMap<String, Any> = HashMap()
        map["content"] = list[position]["content"] as String
        map["metricUnitDesc"] = list[position]["metricUnitDesc"] as String
        map["imperialUnitDesc"] = list[position]["imperialUnitDesc"] as String
        map["metricUnitName"] = list[position]["metricUnitName"] as String
        map["imperialUnitName"] = list[position]["imperialUnitName"] as String
        map["isMetricUnits"] = isMetricUnits
        list[position] = map
        binding.rvList.adapter?.notifyItemChanged(position)
    }

    private fun clickFinishBtn() {
        if (mTargetBean != null) {
            dialog = DialogUtils.dialogShowLoad(this)
            viewModel.uploadTargetInfo(mTargetBean)
        }
    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }
}