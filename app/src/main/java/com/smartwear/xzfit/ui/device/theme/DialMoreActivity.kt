package com.smartwear.xzfit.ui.device.theme

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityDialMoreBinding
import com.smartwear.xzfit.databinding.ItemDialMoreBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.https.response.MoreDialPageResponse
import com.smartwear.xzfit.ui.adapter.MultiItemCommonAdapter
import com.smartwear.xzfit.ui.refresh.CustomizeRefreshHeader
import com.smartwear.xzfit.utils.GlideApp
import com.smartwear.xzfit.utils.ToastUtils
import com.smartwear.xzfit.viewmodel.DeviceModel

class DialMoreActivity : BaseActivity<ActivityDialMoreBinding, DeviceModel>(ActivityDialMoreBinding::inflate, DeviceModel::class.java) {
    private val TAG: String = DialMoreActivity::class.java.simpleName
    private var dialog: Dialog? = null
    private var list = mutableListOf<MoreDialPageResponse.Data>()
    private var count = 0
    private var page = 1
    private var adapter: MultiItemCommonAdapter<MoreDialPageResponse, ItemDialMoreBinding>? = null

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    var typeName: String = ""

    override fun initView() {
        super.initView()
        typeName = intent.getStringExtra("typeName").toString()
        val dialId = if (intent.getStringExtra("dialId").isNullOrEmpty()) "" else intent.getStringExtra("dialId").toString()
        if (dialId == "" && typeName != getString(R.string.diy_watch_face)) {
            ToastUtils.showToast(R.string.theme_center_item_dial_id_error)
            finish()
        }
        setTvTitle(intent.getStringExtra("typeName") ?: "")

        binding.rvList.apply {
            adapter = initAdapter()
            this@DialMoreActivity.adapter = adapter as MultiItemCommonAdapter<MoreDialPageResponse, ItemDialMoreBinding>
            layoutManager = GridLayoutManager(this.context, 3)
            setHasFixedSize(true)
        }

        observe()
        dialog = DialogUtils.dialogShowLoad(this)
        viewModel.moreDialPageByProductList(dialId, isDiy = typeName == getString(R.string.diy_watch_face))
        //下拉刷新
        binding.lyRefresh.setOnRefreshListener {
            dialog?.show()
            page = 1
            list.clear()
            viewModel.moreDialPageByProductList(dialId, isDiy = typeName == getString(R.string.diy_watch_face))

        }
        //上拉加载
        binding.lyRefresh.setOnLoadMoreListener {

            if (!list.isNullOrEmpty() && list.size % 20 == 0) {
                dialog?.show()
                page++
                viewModel.moreDialPageByProductList(dialId, page.toString(), isDiy = typeName == getString(R.string.diy_watch_face))

            } else {
                binding.lyRefresh.setEnableLoadMore(false)
                binding.lyRefresh.finishLoadMore(2000)
            }
        }
        binding.lyRefresh.post {
            if (binding.lyRefresh.refreshHeader is CustomizeRefreshHeader) {
                val header = binding.lyRefresh.refreshHeader as CustomizeRefreshHeader
                header.setCanShowRefreshing(true)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() {
        viewModel.moreDialPageByProductList.observe(this, Observer {
            if (it == null) return@Observer
            dismissDialog()
            binding.lyRefresh.complete()
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (it.data != null && !it.data.list.isNullOrEmpty()) {
                        count = it.data.count.trim().toInt()
                        binding.lyRefresh.setEnableLoadMore(list.size % 20 == 0)
                        //binding.lyRefresh.finishLoadMore()
                        list.addAll(it.data.list!!)
                        adapter?.notifyDataSetChanged()
                    }
                }
                HttpCommonAttributes.SERVER_ERROR -> {
                    ToastUtils.showToast(getString(R.string.server_exception_tips))
                }
            }
        })
    }

    private fun initAdapter(): MultiItemCommonAdapter<MoreDialPageResponse.Data, ItemDialMoreBinding> {
        return object :
            MultiItemCommonAdapter<MoreDialPageResponse.Data, ItemDialMoreBinding>(list) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemDialMoreBinding {
                return ItemDialMoreBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemDialMoreBinding, t: MoreDialPageResponse.Data, position: Int) {
                GlideApp.with(this@DialMoreActivity).load(t.effectImgUrl)
//                        .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                    .into(v.ivItem)
                v.tvItemName.text = t.dialName
                setViewsClickListener({
                    if (typeName == getString(R.string.diy_watch_face))
                        startActivity(
                            Intent(this@DialMoreActivity, DiyDialActivity::class.java)
                                .putExtra("dialId", t.dialId)
                        )
                    else
                        startActivity(
                            Intent(this@DialMoreActivity, DialDetailsActivity::class.java)
                                .putExtra("url", t.effectImgUrl)
                                .putExtra("dialId", t.dialId)
                        )
                }, v.lyItem)
            }

            override fun getItemType(t: MoreDialPageResponse.Data): Int {
                return 0
            }

        }
    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog, 500)
    }

}