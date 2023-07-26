package com.jwei.xzfit.ui.device.setting.notify

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.ActivityMsgNotifyBinding
import com.jwei.xzfit.databinding.ItemMsgNotifyBinding
import com.jwei.xzfit.dialog.DialogUtils
import com.jwei.xzfit.service.MyNotificationsService
import com.jwei.xzfit.ui.adapter.CommonAdapter
import com.jwei.xzfit.ui.device.bean.NotifyItem
import com.jwei.xzfit.ui.eventbus.EventAction
import com.jwei.xzfit.ui.eventbus.EventMessage
import com.jwei.xzfit.utils.AppUtils
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.viewmodel.MsgNotifyModel
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2021/10/6.
 * 消息通知设置
 */
class MsgNotifySetActivity : BaseActivity<ActivityMsgNotifyBinding, MsgNotifyModel>(
    ActivityMsgNotifyBinding::inflate, MsgNotifyModel::class.java
) {

    private var mDatas = mutableListOf<NotifyItem>()

    private var selectedPosition = -1

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_fragment_set_message)
        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(this@MsgNotifySetActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
//            addItemDecoration(object : RecyclerView.ItemDecoration() {
//                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//                    outRect.left = ConvertUtils.dp2px(24F)
//                    outRect.right = ConvertUtils.dp2px(24F)
//                }
//            })
        }

        MyNotificationsService.checkNotificationIsEnable(this)
    }

    override fun initData() {
        super.initData()

        viewModel.sysInfos.observe(this, {
            if (it.isNotEmpty()) {
                mDatas.clear()
                mDatas.addAll(it)
                binding.rvMsg.adapter?.notifyDataSetChanged()
                LogUtils.d("mDatas = ${GsonUtils.toJson(mDatas)}")
            }
        })
        viewModel.getNotifyItem(this)
    }


    private fun initAdapter(): CommonAdapter<NotifyItem, ItemMsgNotifyBinding> {
        return object : CommonAdapter<NotifyItem, ItemMsgNotifyBinding>(mDatas) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemMsgNotifyBinding {
                return ItemMsgNotifyBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemMsgNotifyBinding, t: NotifyItem, position: Int) {
                v.tvName.text = t.title
                if (t.imgName.isNotEmpty()) {
                    AppUtils.tryBlock {
                        v.icon.setImageDrawable(ContextCompat.getDrawable(this@MsgNotifySetActivity, resources.getIdentifier(t.imgName, "mipmap", packageName)))
                    }
                } else {
                    v.icon.setImageDrawable(null)
                }
                v.icon.visibility = if (t.imgName.isNotEmpty()) View.VISIBLE else View.GONE
                v.ivNext.visibility = if (t.isCanNext) View.VISIBLE else View.GONE
                v.mSwitch.visibility = if (t.type != 2 && t.isShowLine) View.VISIBLE else View.GONE
                v.viewLine01.visibility = if (t.isShowLine) View.VISIBLE else View.GONE
                //消息通知 总开关
                var isAllSwitch = false
                if (mDatas != null && mDatas.size > 0) {
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
                v.mSwitch.isChecked = t.isOpen
                v.mSwitch.setOnClickListener {
                    //var isRef = false
                    selectedPosition = position
                    if (v.mSwitch.isChecked) { //通知  && 来电/未接来电 检测危险权限
                        v.mSwitch.isChecked = checkPhonePermission()
                                && MyNotificationsService.checkNotificationIsEnable(this@MsgNotifySetActivity)
                                && MyNotificationsService.checkNotificationPolicyAccessGranted(this@MsgNotifySetActivity)

                        t.isOpen = v.mSwitch.isChecked
                        if (!t.isOpen) { //未授权
                            return@setOnClickListener
                        }
                    } else {
                        t.isOpen = v.mSwitch.isChecked
                    }
                    viewModel.postMessageTraceSave()

                    SpUtils.getSPUtilsInstance().put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(mData))
                    //发事件
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE))
                    notifyDataSetChanged()
                }

                v.cl.setOnClickListener {
                    if (t.type == 2 && t.isCanNext) {
                        if (!MyNotificationsService.checkNotificationIsEnable(this@MsgNotifySetActivity))
                            return@setOnClickListener
                        //子页面
                        startActivity(Intent(this@MsgNotifySetActivity, NotifyOtherSetActivity::class.java))
                    }
                }
                v.viewLine01.visibility = if (position == (mDatas.size - 1)) View.VISIBLE else View.GONE
            }
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
                override fun OnOK() {
                    requestPermissions()
                }

                override fun OnCancel() {

                }
            })
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        return  /*result*/false
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun requestPermissions() {
        com.jwei.xzfit.utils.PermissionUtils.requestPermissions(
            this.lifecycle,
            getString(R.string.permission_notifcation),
            com.jwei.xzfit.utils.PermissionUtils.PERMISSIONS_MAIL_AND_PHONE_LIST
        ) {
            if (MyNotificationsService.checkNotificationIsEnable(this@MsgNotifySetActivity)
                && MyNotificationsService.checkNotificationPolicyAccessGranted(this@MsgNotifySetActivity)
            ) {
                if (selectedPosition != -1) {
                    if (mDatas.get(selectedPosition).type == 1) {
                        mDatas.get(selectedPosition).isOpen = true
                        viewModel.postMessageTraceSave()
                        SpUtils.getSPUtilsInstance()
                            .put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(mDatas))
                        //发事件
                        EventBus.getDefault()
                            .post(EventMessage(EventAction.ACTION_SYS_NOTIFY_PERMISSION_CHANGE))
                        binding.rvMsg.post {
                            binding.rvMsg.adapter?.notifyDataSetChanged()
                        }
                    } else {
                        //第三方通知
                        startActivity(
                            Intent(
                                this@MsgNotifySetActivity,
                                NotifyOtherSetActivity::class.java
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}