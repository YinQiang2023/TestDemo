package com.smartwear.publicwatch.ui.device.setting.worldclock

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.*
import com.zhapp.ble.bean.WorldClockBean
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityAddWorldClockBinding
import com.smartwear.publicwatch.databinding.ItemAddWorldClockBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.adapter.CommonAdapter
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.DeviceModel


/**
 * Created by Android on 2022/9/28.
 */
class AddWorldClockActivity : BaseActivity<ActivityAddWorldClockBinding, DeviceModel>
    (ActivityAddWorldClockBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    private var mCityList: MutableList<WorldClockBean> = mutableListOf()
    private var devData = mutableListOf<WorldClockBean>()
    private val loadDialog: Dialog by lazy { DialogUtils.showLoad(this, false) }
    private var isSearching = false

    companion object {
        //添加运动请求码
        const val RESULT_DATA = "result_data"
    }

    override fun setTitleId() = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.select_city)

        binding.noData.tvTips.text = getString(R.string.no_search_data)

        binding.recyclerClock.apply {
            layoutManager = LinearLayoutManager(this@AddWorldClockActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }

        initEvent()
    }

    private fun initEvent() {
        setViewsClickListener(this, binding.ivSearch)

        binding.sideBar.setOnTouchingLetterChangedListener { word ->
            ToastUtils.showToast(word)
            for (i in 0 until mCityList.size) {
                if (TextUtils.equals(mCityList.get(i).cityName, word)) {
                    //binding.recyclerClock.smoothScrollToPosition(i)
                    (binding.recyclerClock.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(i, 0)
                    break
                }
            }
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, event ->
            if ((actionId == EditorInfo.IME_ACTION_UNSPECIFIED || actionId == EditorInfo.IME_ACTION_SEARCH) && event != null) {
                val filter = binding.etSearch.text.toString().trim()
                if (!TextUtils.isEmpty(filter)) {
                    search(binding.etSearch.text.toString().trim())
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                //清空搜索框还原
                if (s.isNullOrEmpty()) {
                    //loadDialog.show()
                    isSearching = false
                    viewModel.getWorldClockCityStr(filterData = devData)
                }
            }
        })
    }

    /**
     * 搜索
     */
    private fun search(filter: String) {
        isSearching = filter.isNotEmpty()
        KeyboardUtils.hideSoftInput(binding.etSearch)
        loadDialog.show()
        viewModel.getWorldClockCityStr(filter, devData)
    }

    private fun initAdapter(): CommonAdapter<WorldClockBean, ItemAddWorldClockBinding> {
        return object : CommonAdapter<WorldClockBean, ItemAddWorldClockBinding>(mCityList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemAddWorldClockBinding {
                return ItemAddWorldClockBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemAddWorldClockBinding, t: WorldClockBean, position: Int) {

                v.tvCityName.text = t.cityName

                if (t.cityId < 0) { // 中英的A-Z
                    v.tvCityName.setPadding(
                        ConvertUtils.dp2px(10f), ConvertUtils.dp2px(20f),
                        ConvertUtils.dp2px(10f), ConvertUtils.dp2px(5f)
                    )
                } else {
                    v.tvCityName.setPadding(
                        ConvertUtils.dp2px(10f), ConvertUtils.dp2px(5f),
                        ConvertUtils.dp2px(10f), ConvertUtils.dp2px(5f)
                    )
                }

                ClickUtils.applySingleDebouncing(v.root, 600) {
                    if (t.cityId <= 0) return@applySingleDebouncing
                    //ToastUtils.showToast("点击：$t")
                    setResult(RESULT_OK, intent.putExtra(RESULT_DATA,t))
                    finish()
                }

            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun initData() {
        super.initData()

        try {
            devData.addAll(intent.getSerializableExtra(WorldClockActivity.KEY_LIST_DATA) as MutableList<WorldClockBean>)
            LogUtils.d("传入的数据：${GsonUtils.toJson(devData)}")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModel.worldClockCityList.observe(this) { citys ->
            if(loadDialog.isShowing) DialogUtils.dismissDialog(loadDialog)
            if (citys == null) return@observe
            binding.sideBar.visibility = if (AppUtils.isZhOrEn(this) && !isSearching) View.VISIBLE else View.GONE
            binding.noData.layoutNoData.visibility = if(citys.isEmpty()) View.VISIBLE else View.GONE
            //LogUtils.d("mCtiylits : " + citys)
            mCityList.clear()
            mCityList.addAll(citys)
            binding.recyclerClock.adapter?.notifyDataSetChanged()
        }
        loadDialog.show()
        viewModel.getWorldClockCityStr(filterData = devData)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivSearch.id -> {
                val filter = binding.etSearch.text.toString().trim()
                if (!TextUtils.isEmpty(filter)) {
                    search(filter)
                }
            }
        }
    }

}