package com.jwei.publicone.ui.device.setting.notify

import android.annotation.SuppressLint
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityNotifyOtherBinding
import com.jwei.publicone.databinding.ItemMsgNotifyBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.device.bean.NotifyItem
import com.jwei.publicone.ui.eventbus.EventAction
import com.jwei.publicone.ui.eventbus.EventMessage
import com.jwei.publicone.utils.SpUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.viewmodel.MsgNotifyModel
import org.greenrobot.eventbus.EventBus


/**
 * Created by Android on 2021/10/7.
 * 其它消息通知设置
 */
class NotifyOtherSetActivity : BaseActivity<ActivityNotifyOtherBinding, MsgNotifyModel>(
    ActivityNotifyOtherBinding::inflate, MsgNotifyModel::class.java
) {

    private lateinit var loadDialog: Dialog

    private var mDatas = mutableListOf<NotifyItem>()
    private var mDataTotal = mutableListOf<NotifyItem>()

    private var mGson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()


    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_msg_notify_other)

        loadDialog = DialogUtils.showLoad(this)

        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(this@NotifyOtherSetActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
//            addItemDecoration(object : RecyclerView.ItemDecoration() {
//                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//                    outRect.left = ConvertUtils.dp2px(24F)
//                    outRect.right = ConvertUtils.dp2px(24F)
//                }
//            })
        }

        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                val chanceList = arrayListOf<NotifyItem>()

                for (item in mDataTotal) {
                    if (item.title.trim().uppercase().contains(text.uppercase())) {
                        chanceList.add(item)
                    }
                }
                if (text.trim().isEmpty()) {
                    mDatas.apply {
                        clear()
                        addAll(mDataTotal)
                    }
                } else {
                    mDatas.apply {
                        clear()
                        addAll(chanceList)
                    }
                }

                binding.rvMsg.adapter?.notifyDataSetChanged()
            }

            override fun afterTextChanged(s: Editable?) {}

        })

    }

    override fun initData() {
        super.initData()

        loadDialog.show()
        val dataJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER)
        if (!dataJson.isNullOrBlank()) {
            mDatas.addAll(GsonUtils.fromJson(mGson, dataJson, object : TypeToken<MutableList<NotifyItem>>() {}.type))
            mDataTotal.addAll(mDatas)
            //LogUtils.d( "   mDatas 1 = " +GsonUtils.toJson(mGson,mDatas))
        }
        viewModel.appInfos.observe(this) { list ->
            if (list.isNotEmpty()) {
                //已卸载的删除
                val dataIterator = mDatas.iterator()
                while (dataIterator.hasNext()) {
                    val item = dataIterator.next()
                    if (list.find { item.packageName == it.packageName } == null) {
                        dataIterator.remove()
                    }
                }
                //新安装的添加
                for (info in list) {
                    val item = mDatas.find { it.packageName == info.packageName }
                    if (item == null) {
                        mDatas.add(NotifyItem(2, info.packageName, info.title, icon = info.icon))
                    } else {
                        //重新设置icon,昵称
                        item.icon = info.icon
                        item.title = info.title
                    }
                }
                //LogUtils.d("   mDatas 2 = " + GsonUtils.toJson(mGson, mDatas))
                //缓存新数据
                SpUtils.getSPUtilsInstance().put(
                    SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER,
                    GsonUtils.toJson(mGson, mDatas)
                )
                binding.rvMsg.adapter!!.notifyDataSetChanged()
                DialogUtils.dismissDialog(loadDialog)
            }
        }

        viewModel.launchUI {
            viewModel.getApps()
        }
    }

    private fun initAdapter(): CommonAdapter<NotifyItem, ItemMsgNotifyBinding> {
        return object : CommonAdapter<NotifyItem, ItemMsgNotifyBinding>(mDatas) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemMsgNotifyBinding {
                return ItemMsgNotifyBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemMsgNotifyBinding, t: NotifyItem, position: Int) {

                v.tvName.text = t.title /*+","+t.packageName*/
                if (t.icon != null) {
                    v.icon.setImageDrawable(t.icon)
                } else {
                    v.icon.setImageDrawable(viewModel.getIconByPageName(t.packageName))
                }
                v.mSwitch.isChecked = t.isOpen

                v.mSwitch.setOnClickListener {
                    t.isOpen = v.mSwitch.isChecked
                    //存
                    SpUtils.getSPUtilsInstance().put(
                        SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER,
                        GsonUtils.toJson(mGson, mDatas)
                    )
                    //发事件
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_APP_NOTIFY_PERMISSION_CHANGE))
                    viewModel.postMessageTraceSave()
                    if (t.packageName.contains("com.whatsapp") || t.packageName.contains("com.whatsapp.w4b")) {
                        //Whatsapp开关行为上报
                        viewModel.traceSave("device_set", "WhatsApp", "1", "WhatsApp ：${t.packageName} NotifySwitch State == ${if (t.isOpen) "open" else "close"}")
                    }
                    if (t.isOpen) {
                        ToastUtils.showToast(getString(R.string.app_notify_swtich_open_tips), Toast.LENGTH_LONG)
                    }
                }
                v.viewLine01.visibility = if (position == (mDatas.size - 1)) View.VISIBLE else View.GONE
            }

            override fun getItemCount(): Int {
                return super.getItemCount()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}