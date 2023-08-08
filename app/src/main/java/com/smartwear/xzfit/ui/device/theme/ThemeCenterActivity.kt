package com.smartwear.xzfit.ui.device.theme

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.NetworkUtils
import com.google.android.material.tabs.TabLayout
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.ActivityThemeCenterBinding
import com.smartwear.xzfit.databinding.ItemDialMoreBinding
import com.smartwear.xzfit.databinding.ItemOnlineDialBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.https.HttpCommonAttributes
import com.smartwear.xzfit.https.response.GetDialListResponse
import com.smartwear.xzfit.ui.adapter.MultiItemCommonAdapter
import com.smartwear.xzfit.ui.device.bean.WatchSystemBean
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.livedata.RefreshMyDialListState
import com.smartwear.xzfit.utils.FileUtils
import com.smartwear.xzfit.utils.GlideApp
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ThemeCenterActivity : BaseActivity<ActivityThemeCenterBinding, DeviceModel>(ActivityThemeCenterBinding::inflate, DeviceModel::class.java) {
    private val TAG = ThemeCenterActivity::class.java.simpleName
    private var myDialList = mutableListOf<WatchSystemBean>()
    private var onlineList = mutableListOf<GetDialListResponse.Data>()
    private var onlineAdapter: MultiItemCommonAdapter<GetDialListResponse.Data, ItemOnlineDialBinding>? = null
    private var myAdapter: MultiItemCommonAdapter<WatchSystemBean, ItemDialMoreBinding>? = null
    private var dialog: Dialog? = null

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public fun onEventMessage(msg: EventMessage) {
        when (msg.action) {
            EventAction.ACTION_NETWORK_DISCONNECTED -> {
                offlineView()
            }
            EventAction.ACTION_NETWORK_CONNECTED -> {
                offlineView()
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.theme_center_title)
        EventBus.getDefault().register(this)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                //选中
                when (tab.position) {
                    0 -> {
                        binding.layoutLocal.visibility = View.VISIBLE
                        binding.layoutOnline.visibility = View.GONE
                        binding.layoutOffline.layoutMain.visibility = View.GONE
                    }
                    1 -> {
                        offlineView()
                        if (onlineList.size == 0) {
                            dismissDialog()
                            onlineList.clear()
                            viewModel.getHomeByProductList()
                            dialog?.show()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                //未选中
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                //再次选中
            }
        })

        binding.rvLocalDial.apply {
            adapter = initLocalAdapter()
            myAdapter = adapter as MultiItemCommonAdapter<WatchSystemBean, ItemDialMoreBinding>
            layoutManager = GridLayoutManager(this.context, 3)
            setHasFixedSize(true)
        }

        binding.rvOnlineDial.apply {
            adapter = initOnlineAdapter()
            onlineAdapter = adapter as MultiItemCommonAdapter<GetDialListResponse.Data, ItemOnlineDialBinding>
            layoutManager = LinearLayoutManager(this.context)
            setHasFixedSize(true)
        }

        observe()
        dialog = DialogUtils.showLoad(this)

        setViewsClickListener({
            dismissDialog()
            binding.layoutOffline.btnRefresh.tag = true
            onlineList.clear()
            viewModel.getHomeByProductList()
            dialog?.show()
        }, binding.layoutOffline.btnRefresh)

        dialog?.show()
        viewModel.getDialFromDeviceAndOnline()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun offlineView() {
        if (NetworkUtils.isConnected()) {
            binding.layoutOffline.layoutMain.visibility = View.GONE
            if (binding.tabLayout.selectedTabPosition == 0) {
                binding.layoutLocal.visibility = View.VISIBLE
                binding.layoutOnline.visibility = View.GONE
            } else {
                binding.layoutLocal.visibility = View.GONE
                binding.layoutOnline.visibility = View.VISIBLE
            }
        } else {
            binding.layoutOffline.layoutMain.visibility = View.VISIBLE
            binding.layoutLocal.visibility = View.GONE
            binding.layoutOnline.visibility = View.GONE
        }
    }

    override fun initData() {
        super.initData()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() {

        /**
         * 监听-在线表盘-后台
         */
        viewModel.getHomeByProductList.observe(this, Observer {
            Log.e(TAG, "getHomeByProductList")
            if (it == null) return@Observer
            if (binding.layoutOffline.btnRefresh.tag != null && binding.layoutOffline.btnRefresh.tag == true) {
                offlineView()
                //???
                binding.layoutOffline.btnRefresh.tag == false
            }
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    viewModel.getDiyHomeList()
                    if (!it.data.list.isNullOrEmpty()) {
                        for (i in it.data.list.indices) {
                            if (it.data.list[i].dialList.isNotEmpty()) {
                                val bean = GetDialListResponse.Data()
                                bean.dialList = it.data.list[i].dialList
                                bean.dialType = it.data.list[i].dialType
                                bean.dialTypeId = it.data.list[i].dialTypeId
                                bean.dialTypeName = it.data.list[i].dialTypeName
                                onlineList.add(bean)
                            }
                        }
                        onlineAdapter?.notifyDataSetChanged()
                    }
                }
                else -> {

                }
            }
        })

        /**
         * 监听-内置表盘-设备
         */
        viewModel.getDialFromDevice.observe(this, Observer {
            Log.e(TAG, "getDialFromDevice")
            if (it == null) return@Observer
            dismissDialog()
            myDialList.clear()
            for (i in it.indices) {
                val bean = WatchSystemBean()
                bean.isCurrent = it[i].isCurrent
                bean.isRemove = it[i].isRemove
                bean.dialCode = it[i].id
                myDialList.add(bean)
            }
            binding.rvLocalDial.adapter!!.notifyDataSetChanged()
        })

        /**
         * 监听-内置表盘-后台
         */
        viewModel.getDialSystemInfo.observe(this, Observer {
            Log.e(TAG, "getDialSystemInfo ${DeviceUtils.getModel()}")
            if (it == null) return@Observer
            dismissDialog()
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (!it.data.dialSystemList.isNullOrEmpty()) {
                        for (i in it.data.dialSystemList.indices) {
                            val index = myDialList.indexOfFirst { date -> date.dialCode == it.data.dialSystemList[i].dialCode }
                            if (index != -1) {
                                myDialList[index].dialImageUrl = it.data.dialSystemList[i].dialImageUrl
                                myDialList[index].dialCode = it.data.dialSystemList[i].dialCode
                            }
                        }
                    }
                    myAdapter?.notifyDataSetChanged()
                }
                else -> {
                    myAdapter?.notifyDataSetChanged()
                }
            }
        })

        /**
         * 监听-Diy表盘-后台
         */
        viewModel.diyHomeList.observe(this, Observer {
            Log.e(TAG, "getDialSystemInfo ${DeviceUtils.getModel()}")
            if (it == null) return@Observer
            dismissDialog()
            when (it.code) {
                HttpCommonAttributes.REQUEST_SUCCESS -> {
                    if (it.data.dialList.isNotEmpty()) {
                        onlineList.forEach { data ->
                            if (data.dialTypeName == getString(R.string.diy_watch_face)) return@Observer
                        }
                        if (it.data.dialList.isNotEmpty()) {
                            val bean = GetDialListResponse.Data()
                            val dialList: MutableList<GetDialListResponse.Data2> = mutableListOf()
                            it.data.dialList.forEach { data ->
                                val temp = GetDialListResponse.Data2()
                                temp.dialId = data.dialId
                                temp.dialName = data.dialName
                                temp.effectImgUrl = data.effectImgUrl
                                dialList.add(temp)
                            }
                            bean.dialList = dialList
                            bean.dialTypeName = getString(R.string.diy_watch_face)
                            onlineList.add(bean)
                        }
                        onlineAdapter?.notifyDataSetChanged()
                    }
                }
                else -> {
                    myAdapter?.notifyDataSetChanged()
                }
            }
        })

        RefreshMyDialListState.observe(this, Observer {
            Log.e(TAG, "RefreshMyDialListState")
            if (it == null) return@Observer
            dialog?.show()
            viewModel.getDialFromDevice()
        })
    }

    /**
     * 本地表盘适配器初始化
     */
    private fun initLocalAdapter(): MultiItemCommonAdapter<WatchSystemBean, ItemDialMoreBinding> {
        return object : MultiItemCommonAdapter<WatchSystemBean, ItemDialMoreBinding>(myDialList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemDialMoreBinding {
                return ItemDialMoreBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemDialMoreBinding, t: WatchSystemBean, position: Int) {
                Log.i(TAG, "initLocalAdapter t.dialImageUrl $t position = $position")

                if (t.dialImageUrl.isEmpty()) {
                    //301为DIY表盘
                    if (t.dialCode.substring(5, 8) == "301") {
                        val path = SpUtils.getValue(SpUtils.DIY_RENDERINGS_PATH, "")
                        if (path.isNotEmpty())
                            GlideApp.with(BaseApplication.mContext).load(ConvertUtils.bytes2Bitmap(FileUtils.getBytes(path))).into(v.ivItem)
                    } else
                        GlideApp.with(this@ThemeCenterActivity).load(R.mipmap.no_data_transparent)
//                            .placeholder(R.mipmap.no_renderings)
//                            .error(R.mipmap.no_renderings)
//                            .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                            .into(v.ivItem)
                } else {
                    GlideApp.with(this@ThemeCenterActivity).load(t.dialImageUrl)
//                            .placeholder(R.mipmap.no_renderings)
//                            .error(R.mipmap.no_renderings)
//                            .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                        .into(v.ivItem)
                }

                var colckDialName = "";
                val nowPos = position + 1;
                if (nowPos < 10) {
                    colckDialName = getString(R.string.theme_center_name) + "0" + nowPos
                } else {
                    colckDialName = getString(R.string.theme_center_name) + nowPos
                }
                v.tvItemName.text = colckDialName
                if (t.isCurrent) {
                    v.ivItem.setBackgroundResource(R.drawable.clock_dial_bg_select_on)
                } else {
                    v.ivItem.setBackgroundResource(R.drawable.clock_dial_bg_select_off)
                }
                setViewsClickListener({
                    startActivity(
                        Intent(this@ThemeCenterActivity, MyDialCenterActivity::class.java)
                            .putExtra("data", t)
                    )
                }, v.lyItem)
            }

            override fun getItemType(t: WatchSystemBean): Int {
                return 0
            }

        }
    }

    /**
     * 在线表盘适配器初始化
     */
    private fun initOnlineAdapter(): MultiItemCommonAdapter<GetDialListResponse.Data, ItemOnlineDialBinding> {
        return object :
            MultiItemCommonAdapter<GetDialListResponse.Data, ItemOnlineDialBinding>(onlineList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemOnlineDialBinding {
                return ItemOnlineDialBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemOnlineDialBinding,
                t: GetDialListResponse.Data,
                position: Int,
            ) {
                v.itemTypeName.text = t.dialTypeName
                for (i in t.dialList.indices) {
                    Log.i(TAG, "initOnlineAdapter i $i")
                    when (i) {
                        0 -> {
                            v.ivItem1.visibility = View.VISIBLE
                            GlideApp.with(this@ThemeCenterActivity).load(t.dialList[i].effectImgUrl)
//                                    .placeholder(R.mipmap.no_renderings)
//                                    .error(R.mipmap.no_renderings)
//                                    .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                                .into(v.ivItem1)
                            v.tvItemName1.text = t.dialList[i].dialName
                        }
                        1 -> {
                            v.ivItem2.visibility = View.VISIBLE
                            GlideApp.with(this@ThemeCenterActivity).load(t.dialList[i].effectImgUrl)
//                                    .placeholder(R.mipmap.no_renderings)
//                                    .error(R.mipmap.no_renderings)
//                                    .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                                .into(v.ivItem2)
                            v.tvItemName2.text = t.dialList[i].dialName
                        }
                        2 -> {
                            v.ivItem3.visibility = View.VISIBLE
                            GlideApp.with(this@ThemeCenterActivity).load(t.dialList[i].effectImgUrl)
//                                    .placeholder(R.mipmap.no_renderings)
//                                    .error(R.mipmap.no_renderings)
//                                    .resize(ConvertUtils.dp2px(120f), ConvertUtils.dp2px(120f))
                                .into(v.ivItem3)
                            v.tvItemName3.text = t.dialList[i].dialName
                        }
                    }
                }

                if (t.dialList.size >= 3) {
                    v.tvMore.visibility = View.VISIBLE
                } else {
                    v.tvMore.visibility = View.GONE
                }

//                v.lyItem1.setOnClickListener{
//                    clickOnlineItem(t.dialList[0].dialId , t.dialList[0].effectImgUrl)
//                }
//                v.lyItem2.setOnClickListener{
//                    clickOnlineItem(t.dialList[1].dialId, t.dialList[1].effectImgUrl)
//                }
//                v.lyItem3.setOnClickListener{
//                    clickOnlineItem(t.dialList[2].dialId, t.dialList[2].effectImgUrl)
//                }
//                v.tvMore.setOnClickListener {
//                    startActivity(Intent(this@ThemeCenterActivity ,DialMoreActivity::class.java)
//                        .putExtra("dialId" , t.dialTypeId)
//                        .putExtra("typeName" , t.dialTypeName))
//                }

                setViewsClickListener({ view ->
                    when (view.id) {
                        v.lyItem1.id -> {
                            clickOnlineItem(t.dialList[0].dialId, t.dialList[0].effectImgUrl, t.dialTypeName)
                        }
                        v.lyItem2.id -> {
                            if (t.dialList.size > 1) {
                                clickOnlineItem(t.dialList[1].dialId, t.dialList[1].effectImgUrl, t.dialTypeName)
                            }
                        }
                        v.lyItem3.id -> {
                            if (t.dialList.size > 2) {
                                clickOnlineItem(t.dialList[2].dialId, t.dialList[2].effectImgUrl, t.dialTypeName)
                            }
                        }
                        v.tvMore.id -> {
                            startActivity(
                                Intent(this@ThemeCenterActivity, DialMoreActivity::class.java)
                                    .putExtra("dialId", t.dialTypeId)
                                    .putExtra("typeName", t.dialTypeName)
                            )
                        }
                    }
                }, v.lyItem1, v.lyItem2, v.lyItem3, v.tvMore)
            }

            override fun getItemType(t: GetDialListResponse.Data): Int {
                return 0
            }

        }
    }

    /**
     * 跳转表盘详情
     */
    private fun clickOnlineItem(dialId: String, url: String, dialTypeName: String) {
        if (dialTypeName == getString(R.string.diy_watch_face))
            startActivity(
                Intent(this@ThemeCenterActivity, DiyDialActivity::class.java)
                    .putExtra("dialId", dialId)
            )
        else
            startActivity(
                Intent(this, DialDetailsActivity::class.java)
                    .putExtra("url", url)
                    .putExtra("dialId", dialId)
            )

    }

    private fun dismissDialog() {
        DialogUtils.dismissDialog(dialog)
    }
}