package com.smartwear.publicwatch.ui.guide.item

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.reflect.TypeToken
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.FragmentSelectAppNotificationBinding
import com.smartwear.publicwatch.databinding.ItemMsgNotifyBinding
import com.smartwear.publicwatch.base.BaseFragment
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.service.MyNotificationsService
import com.smartwear.publicwatch.ui.adapter.CommonAdapter
import com.smartwear.publicwatch.ui.device.bean.NotifyItem
import com.smartwear.publicwatch.ui.eventbus.EventAction
import com.smartwear.publicwatch.ui.eventbus.EventMessage
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.MsgNotifyModel
import org.greenrobot.eventbus.EventBus

class SelectAppNotificationFragment :
    BaseFragment<FragmentSelectAppNotificationBinding, MsgNotifyModel>(
        FragmentSelectAppNotificationBinding::inflate, MsgNotifyModel::class.java
    ), View.OnClickListener {

    companion object {
        val instance: SelectAppNotificationFragment
                by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
                    SelectAppNotificationFragment()
                }
    }

    private lateinit var loadDialog: Dialog
    private val mDatas = mutableListOf<NotifyItem>()
    private var selectedPosition = -1
    private var tempList: MutableList<NotifyItem> = arrayListOf()


    override fun onClick(v: View?) {

    }

    override fun initView() {
        super.initView()
        loadDialog = DialogUtils.showLoad(requireActivity())
        loadDialog.setCanceledOnTouchOutside(false)
        loadDialog.setCancelable(false)

        binding.layoutMasterSwitch.viewLine01.visibility = View.GONE
        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        binding.layoutMasterSwitch.tvName.text = getString(R.string.device_msg_notify_switch)
        binding.layoutMasterSwitch.mSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.rvMsg.visibility = View.VISIBLE
            } else {
                binding.rvMsg.visibility = View.GONE
            }
            if (tempList.isNotEmpty()) {
                tempList[0].isOpen = isChecked
                SpUtils.getSPUtilsInstance()
                    .put(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, GsonUtils.toJson(tempList))
            }
        }

        binding.layoutMasterSwitch.mSwitch.setOnClickListener {
            if (binding.layoutMasterSwitch.mSwitch.isChecked) { //通知  && 来电/未接来电 检测危险权限
                binding.layoutMasterSwitch.mSwitch.isChecked = checkPhonePermission(
                ) && MyNotificationsService.checkNotificationIsEnable(
                    requireActivity()
                ) && MyNotificationsService.checkNotificationPolicyAccessGranted(requireActivity())
            }
        }
    }

    override fun initData() {
        super.initData()
        viewmodel.appInfos.observe(this) { list ->
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
                    GsonUtils.toJson(BaseApplication.gson, mDatas)
                )
                binding.rvMsg.adapter!!.notifyDataSetChanged()
                DialogUtils.dismissDialog(loadDialog)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val callDataJson =
            SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST)
        if ((!callDataJson.isNullOrBlank())) {
//            val tempList: MutableList<NotifyItem>? = GsonUtils.fromJson(callDataJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
            tempList.apply {
                clear()
                addAll(
                    GsonUtils.fromJson(
                        callDataJson,
                        object : TypeToken<MutableList<NotifyItem>>() {}.type
                    )
                )
            }
            if (tempList.isNotEmpty()) {
                binding.layoutMasterSwitch.mSwitch.isChecked = tempList[0].isOpen
            }
        }
        if (binding.layoutMasterSwitch.mSwitch.isChecked) {
            binding.rvMsg.visibility = View.VISIBLE
        } else {
            binding.rvMsg.visibility = View.GONE
        }

        val appDataJson =
            SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER)
        if (!appDataJson.isNullOrBlank()) {
            mDatas.addAll(
                GsonUtils.fromJson(
                    BaseApplication.gson,
                    appDataJson,
                    object : TypeToken<MutableList<NotifyItem>>() {}.type
                )
            )
        }

        viewmodel.launchUI {
            viewmodel.getApps()
        }
        loadDialog.show()
    }

    private fun initAdapter(): CommonAdapter<NotifyItem, ItemMsgNotifyBinding> {
        return object : CommonAdapter<NotifyItem, ItemMsgNotifyBinding>(mDatas) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemMsgNotifyBinding {
                return ItemMsgNotifyBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemMsgNotifyBinding, t: NotifyItem, position: Int) {
                val params = v.root.layoutParams
                params.height = ConvertUtils.dp2px(45f)

                v.viewLine01.visibility = View.GONE
                v.tvName.setPadding(ConvertUtils.dp2px(10f), 0, ConvertUtils.dp2px(10f), 0)
                v.tvName.text = t.title /*+","+t.packageName*/
                if (t.icon != null) {
                    v.icon.setImageDrawable(t.icon)
                } else {
                    v.icon.setImageDrawable(viewmodel.getIconByPageName(t.packageName))
                }
                v.mSwitch.isChecked = t.isOpen

                v.mSwitch.setOnClickListener {
                    t.isOpen = v.mSwitch.isChecked
                    viewmodel.postMessageTraceSave()
                    //存
                    SpUtils.getSPUtilsInstance().put(
                        SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST_OTHER,
                        GsonUtils.toJson(BaseApplication.gson, mDatas)
                    )
                    //发事件
                    EventBus.getDefault()
                        .post(EventMessage(EventAction.ACTION_APP_NOTIFY_PERMISSION_CHANGE))
                    if (t.isOpen) {
                        ToastUtils.showToast(getString(R.string.app_notify_swtich_open_tips), Toast.LENGTH_LONG)
                    }
                }
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
                override fun OnOK() = requestPermissions()
                override fun OnCancel() {}
            })
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        return false
    }

    private fun requestPermissions() {
        com.smartwear.publicwatch.utils.PermissionUtils.requestPermissions(
            this.lifecycle,
            getString(R.string.permission_notifcation),
            com.smartwear.publicwatch.utils.PermissionUtils.PERMISSIONS_MAIL_AND_PHONE_LIST
        ) {
            if (MyNotificationsService.checkNotificationIsEnable(
                    requireActivity()
                ) && MyNotificationsService.checkNotificationPolicyAccessGranted(
                    requireActivity()
                )
            ) {
                binding.layoutMasterSwitch.mSwitch.isChecked = true
            }
        }
    }
}