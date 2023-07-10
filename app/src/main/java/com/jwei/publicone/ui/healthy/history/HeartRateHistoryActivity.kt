package com.jwei.publicone.ui.healthy.history

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityHeartRateHistoryBinding
import com.jwei.publicone.databinding.ItemHeartRateHistoryBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.https.HttpCommonAttributes
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.healthy.bean.HeartRateHistoryBean
import com.jwei.publicone.utils.DateUtils
import com.jwei.publicone.viewmodel.DailyModel

class HeartRateHistoryActivity : BaseActivity<ActivityHeartRateHistoryBinding, DailyModel>(
    ActivityHeartRateHistoryBinding::inflate,
    DailyModel::class.java
), View.OnClickListener {
    private var list = mutableListOf<HeartRateHistoryBean>()
    private var curDay = ""
    private var dialog: Dialog? = null

    override fun onClick(v: View?) {
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()

        binding.title.tvTitle.text = getString(R.string.heart_rate_history_title)
        binding.rvHeartRateHistory.apply {
            layoutManager = LinearLayoutManager(this.context)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        if (intent.getStringExtra("date") != null) {
            curDay = intent.getStringExtra("date")!!
        }

        dialog = DialogUtils.showLoad(this)
        showNoDataView(list.size == 0)
        observer()
    }

    override fun initData() {
        super.initData()
        viewModel.getSingleHeartRateDataByDay(curDay)
    }

    private fun observer() {
        viewModel.getSingleHeartRateDataByDay.observe(this, Observer {
            dismissDialog()
            if (it != null) {
                when (it.code) {
                    HttpCommonAttributes.REQUEST_FAIL -> {
                        list.clear()
                    }
                    HttpCommonAttributes.REQUEST_SUCCESS -> {
                        for (i in it.data.dataList.indices) {
                            val bean = HeartRateHistoryBean()
                            bean.id = it.data.dataList[i].id
                            bean.measureData = it.data.dataList[i].measureData
                            bean.measureTime = it.data.dataList[i].measureTime
                            list.add(bean)
                        }
                    }
                    HttpCommonAttributes.REQUEST_SEND_CODE_NO_DATA -> {
                        list.clear()
                    }
                    HttpCommonAttributes.REQUEST_CODE_ERROR -> {
                        list.clear()
                    }
                }
                showNoDataView(list.size == 0)
            }
        })
    }

    private fun initAdapter(): CommonAdapter<HeartRateHistoryBean, ItemHeartRateHistoryBinding> {
        return object : CommonAdapter<HeartRateHistoryBean, ItemHeartRateHistoryBinding>(list) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int,
            ): ItemHeartRateHistoryBinding {
                return ItemHeartRateHistoryBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemHeartRateHistoryBinding,
                t: HeartRateHistoryBean,
                position: Int,
            ) {
                v.tvItemHeartRateDate.text = DateUtils.getStringDate(list[position].measureTime.toLong(), "MM-dd HH:mm")
                v.tvItemHeartRateHistory.text = list[position].measureData
            }

        }
    }

    private fun showNoDataView(isShow: Boolean) {
        if (isShow) {
            binding.layoutNoData.layoutNoData.visibility = View.VISIBLE
            binding.rvHeartRateHistory.visibility = View.GONE
        } else {
            binding.layoutNoData.layoutNoData.visibility = View.GONE
            binding.rvHeartRateHistory.visibility = View.VISIBLE
        }
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }
}