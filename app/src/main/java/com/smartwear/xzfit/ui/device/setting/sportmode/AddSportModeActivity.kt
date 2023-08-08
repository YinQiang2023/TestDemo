package com.smartwear.xzfit.ui.device.setting.sportmode

import android.annotation.SuppressLint
import android.app.Activity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.*
import com.zhapp.ble.bean.WidgetBean
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityAddSportModeBinding
import com.smartwear.xzfit.databinding.ItemEditCardBinding
import com.smartwear.xzfit.ui.adapter.CommonAdapter
import com.smartwear.xzfit.utils.GlideApp
import com.smartwear.xzfit.utils.SportTypeUtils
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import java.io.Serializable

/**
 * Created by Android on 2022/8/23.
 */
class AddSportModeActivity : BaseActivity<ActivityAddSportModeBinding, DeviceModel>
    (ActivityAddSportModeBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    private var isSearch = false

    private var devSportData = mutableListOf<WidgetBean>()

    private var searchData = mutableListOf<WidgetBean>()

    private var asmAdapter: CommonAdapter<WidgetBean, ItemEditCardBinding>? = null

    companion object {
        //添加运动请求码
        const val RESULT_DATA = "sport_result_data"
    }

    override fun setTitleId() = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.add_sport_mode)
        setRightIconOrTitle(R.mipmap.icon_search, clickListener = this)
        binding.noData.tvTips.text = getString(R.string.no_search_data)
        setViewsClickListener(this, ivRightIcon!!, binding.btnSave)

        binding.recyclerSport.apply {
            layoutManager = LinearLayoutManager(this@AddSportModeActivity)
            setHasFixedSize(true)
            asmAdapter = initAdapter()
            adapter = asmAdapter
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                refSearchData()
            }

        })
    }

    private fun initAdapter(): CommonAdapter<WidgetBean, ItemEditCardBinding> {
        return object : CommonAdapter<WidgetBean, ItemEditCardBinding>(devSportData) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemEditCardBinding {
                return ItemEditCardBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemEditCardBinding, t: WidgetBean, position: Int) {
                v.ivItemLeft.visibility = View.GONE
                v.tvItemText.setPadding(ConvertUtils.dp2px(12f), 0, 0, 0)
                v.tvItemText.text = SportTypeUtils.getSportTypeName(2, t.functionId.toString())
                if (getEnableSize() >= 10) {
                    GlideApp.with(this@AddSportModeActivity)
                        .load(if (t.isEnable) R.mipmap.icon_sport_check else R.mipmap.icon_sport_disabled)
                        .into(v.ivItemRight)
                } else {
                    GlideApp.with(this@AddSportModeActivity)
                        .load(if (t.isEnable) R.mipmap.icon_sport_check else R.mipmap.icon_sport_uncheck)
                        .into(v.ivItemRight)
                }
                ClickUtils.applySingleDebouncing(v.root, object : View.OnClickListener {
                    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
                    override fun onClick(v: View?) {
                        if (getEnableSize() == 1 && t.isEnable) {
                            ToastUtils.showToast(getString(R.string.min_sport_tips))
                        } else if (getEnableSize() == 10 && !t.isEnable) {
                            ToastUtils.showToast(R.string.max_sport_tips)
                        } else {
                            t.isEnable = !t.isEnable
                            //更新数量
                            binding.tvMax.text = "${getString(R.string.edit_card_display_area)}（${getEnableSize()}/10）"
                            notifyDataSetChanged()
                        }

                        if (isSearch) {
                            mergeSearchDevSportData()
                        }
                    }
                })

            }
        }
    }

    /**
     * 获取选中的数量
     */
    private fun getEnableSize(): Int {
        var size = 0
        if (devSportData.size > 0) {
            for (i in devSportData) {
                if (i.isEnable) {
                    size++
                }
            }
        }
        return size
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun initData() {
        super.initData()
        try {
            devSportData.addAll(intent.getSerializableExtra(SportsModeActivity.KEY_LIST_DATA) as MutableList<WidgetBean>)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogUtils.d("传入搜索运动排序的数据：${GsonUtils.toJson(devSportData)}")
        binding.recyclerSport.adapter?.notifyDataSetChanged()
        binding.tvMax.text = "${getString(R.string.edit_card_display_area)}（${getEnableSize()}/10）"
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            ivRightIcon?.id -> {
                isSearch = true
                binding.btnSave.visibility = View.GONE
                ivRightIcon?.visibility = View.GONE
                binding.etSearch.visibility = View.VISIBLE
                binding.line.visibility = View.VISIBLE
                setTvTitle(R.string.sports_mode)
                binding.etSearch.setText("")
                refSearchData()
            }
            binding.btnSave.id -> {
                val intent = getIntent()
                intent.putExtra(RESULT_DATA, devSportData as Serializable)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refSearchData() {
        searchData.clear()
        val match = binding.etSearch.text.toString().trim()
        if (!TextUtils.isEmpty(match)) {
            for (item in devSportData) {
                val name = SportTypeUtils.getSportTypeName(2, item.functionId.toString())
                if (name.isNotEmpty() && (name.contains(match) || name.lowercase().contains(match.lowercase()))) {
                    searchData.add(item)
                }
            }
        }
        asmAdapter?.mData = searchData
        asmAdapter?.mData?.size?.let {
            binding.noData.layoutNoData.visibility = if (it > 0) View.GONE else View.VISIBLE
            if (it > 0) {
                binding.recyclerSport.smoothScrollToPosition(0)
            }
        }
        asmAdapter?.notifyDataSetChanged()
    }


    private fun mergeSearchDevSportData() {
        if (searchData.isNotEmpty()) {
            for (s in searchData) {
                for (d in devSportData) {
                    if (s.functionId == d.functionId) {
                        d.isEnable = s.isEnable
                        break
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun finish() {
        if (isSearch) {
            isSearch = false

            binding.btnSave.visibility = View.VISIBLE
            ivRightIcon?.visibility = View.VISIBLE
            binding.etSearch.visibility = View.GONE
            binding.line.visibility = View.GONE
            setTvTitle(R.string.add_sport_mode)
            KeyboardUtils.hideSoftInput(this)

            asmAdapter?.mData = devSportData
            asmAdapter?.mData?.size?.let {
                binding.noData.layoutNoData.visibility = if (it > 0) View.GONE else View.VISIBLE
                if (it > 0) {
                    binding.recyclerSport.smoothScrollToPosition(0)
                }
            }
            asmAdapter?.notifyDataSetChanged()
            return
        }
        super.finish()
    }

}