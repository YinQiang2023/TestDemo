package com.jwei.xzfit.ui.guide.item

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseFragment
import com.jwei.xzfit.databinding.FragmentSelectNotificationBinding
import com.jwei.xzfit.databinding.ItemMsgNotifyBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.service.MyNotificationsService
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.device.bean.NotifyItem
import com.jwei.xzfit.ui.device.setting.notify.NotifyOtherSetActivity
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.viewmodel.MsgNotifyModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SelectNotificationFragment : BaseFragment<FragmentSelectNotificationBinding, MsgNotifyModel>(
    FragmentSelectNotificationBinding::inflate, MsgNotifyModel::class.java
), View.OnClickListener {

    private var isOpenMasterSwitch:Boolean = false

    companion object {
        val instance: SelectNotificationFragment by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SelectNotificationFragment()
        }
    }

    private lateinit var loadDialog: Dialog
    private val mDatas = mutableListOf<NotifyItem>()
    private var selectedPosition = -1

    override fun onClick(v: View?) {}

    override fun initView() {
        super.initView()
        loadDialog = DialogUtils.showLoad(requireActivity())
        loadDialog.setCanceledOnTouchOutside(false)
        loadDialog.setCancelable(false)

        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            setHasFixedSize(true)
            adapter = initAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.left = ConvertUtils.dp2px(16F)
                    outRect.right = ConvertUtils.dp2px(16F)
                }
            })
        }
//        MyNotificationsListenerService.checkNotificationIsEnable(requireActivity())
    }

    override fun initData() {
        super.initData()
        EventBus.getDefault().register(this)
        viewmodel.sysInfos.observe(this) {
            DialogUtils.dismissDialog(loadDialog)
            if (it.isNotEmpty()) {
                mDatas.clear()
                mDatas.addAll(it)
                binding.rvMsg.adapter?.notifyDataSetChanged()
                LogUtils.d("mDatas = ${GsonUtils.toJson(mDatas)}")
            }
        }
        loadDialog.show()
        viewmodel.getNotifyItem(requireActivity(), hasApp = true)
    }

    private fun initAdapter(): CommonAdapter<NotifyItem, ItemMsgNotifyBinding> {
        return object : CommonAdapter<NotifyItem, ItemMsgNotifyBinding>(mDatas) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemMsgNotifyBinding {
                return ItemMsgNotifyBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun convert(v: ItemMsgNotifyBinding, t: NotifyItem, position: Int) {
                val params = v.root.layoutParams
                params.height = ConvertUtils.dp2px(50f)
                v.root.layoutParams = params
                if (!t.isTypeHeader && !t.isShowLine) {
                    v.tvName.textSize = 12f
                }
                v.icon.visibility = View.GONE
                v.viewLine01.visibility = View.GONE
                v.tvName.setPadding(0, 0, ConvertUtils.dp2px(30f), 0)
                v.tvName.text = t.title
                if (t.imgName.isNotEmpty()) {
                    AppUtils.tryBlock {
                        v.icon.setImageDrawable(
                            ContextCompat.getDrawable(
                                requireActivity(),
                                resources.getIdentifier(t.imgName, "mipmap", requireActivity().packageName)
                            )
                        )
                    }
//                    v.tvName.setPadding(ConvertUtils.dp2px(12f), 0, ConvertUtils.dp2px(30f), 0)
                } else {
                    v.icon.setImageDrawable(null)
//                    v.tvName.setPadding(0, 0, ConvertUtils.dp2px(30f), 0)
                }
//                v.icon.visibility = if (t.imgName.isNotEmpty()) View.VISIBLE else View.GONE
                v.ivNext.visibility = if (t.isCanNext) View.VISIBLE else View.GONE
                v.mSwitch.visibility = if (t.type != 2 && t.isShowLine) View.VISIBLE else View.GONE
//                v.line.visibility = if (t.isShowLine) View.VISIBLE else View.GONE
                //消息通知 总开关
                var isAllSwitch = false
                if (mDatas.size > 0) {
                    for (item in mDatas) {
                        if (item.type == 1 && item.isTypeHeader) {
                            isAllSwitch = item.isOpen
                        }
                    }
                }
                if ((t.type != 1 || !t.isTypeHeader) && t.isShowLine) {
                    v.cl.isEnabled = isAllSwitch
                    v.mSwitch.isEnabled = isAllSwitch
                    v.cl.alpha = if (isAllSwitch) {
                        1f
                    } else {
                        0.5f
                    }
                } else {
                    v.cl.isEnabled = true
                    v.mSwitch.isEnabled = true
                    v.cl.alpha = 1f
                }
                //v.cl.isEnabled = false

                v.mSwitch.isChecked = t.isOpen
                v.mSwitch.setOnClickListener {
                    //var isRef = false
                    selectedPosition = position
                    if (v.mSwitch.isChecked) { //通知  && 来电/未接来电 检测危险权限
                        v.mSwitch.isChecked = checkPhonePermission(
                        ) && MyNotificationsService.checkNotificationIsEnable(
                            requireActivity()
                        ) && MyNotificationsService.checkNotificationPolicyAccessGranted(requireActivity())

                        t.isOpen = v.mSwitch.isChecked
                        if (!t.isOpen) { //未授权
                            return@setOnClickListener
                        }
                    } else {
                        t.isOpen = v.mSwitch.isChecked
                    }
                    SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(mData))
                    EventBus.getDefault().postSticky(EventMessage(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE))
                    binding.rvMsg.post {
                        binding.rvMsg.adapter?.notifyDataSetChanged()
                    }
                }

                if ((t.type == 1 && t.isTypeHeader && !t.isCanNext) && isOpenMasterSwitch) {
                    isOpenMasterSwitch = false
                    v.mSwitch.isChecked = true
                    v.mSwitch.callOnClick()
                }

                v.cl.setOnClickListener {
                    if (t.type == 2 && t.isCanNext) {
                        if (!MyNotificationsService.checkNotificationIsEnable(requireActivity())) {
                            return@setOnClickListener
                        }
                        startActivity(Intent(requireActivity(), NotifyOtherSetActivity::class.java))
                    }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMessage(msg: EventMessage) {
        if(TextUtils.equals(msg.action,EventAction.ACTION_GUIDE_NOTIFY_SWITCH)){
            //触发消息通知总开关
            isOpenMasterSwitch = true
            binding.rvMsg.adapter?.notifyDataSetChanged()
        }
    }


    /**
     * 危险权限
     * */
    private fun checkPhonePermission(): Boolean {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            getString(R.string.apply_permission),
            getString(R.string.notify_per_explain),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() = requestPermissions()
                override fun OnCancel() {}
            })
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        return false
    }

    private fun requestPermissions() {
        com.jwei.xzfit.utils.PermissionUtils.requestPermissions(
            this.lifecycle,
            getString(R.string.permission_notifcation),
            com.jwei.xzfit.utils.PermissionUtils.PERMISSIONS_MAIL_AND_PHONE_LIST
        ) {
            if (MyNotificationsService.checkNotificationIsEnable(requireActivity())
                && MyNotificationsService.checkNotificationPolicyAccessGranted(requireActivity())
            ) {
                if (selectedPosition != -1) {
                    if (mDatas[selectedPosition].type == 1) {
                        mDatas[selectedPosition].isOpen = true
                        viewmodel.postMessageTraceSave()
                        binding.rvMsg.adapter?.notifyDataSetChanged()
                        SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(mDatas))
                        EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE))
                    } else {
                        startActivity(Intent(requireActivity(), NotifyOtherSetActivity::class.java))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

}