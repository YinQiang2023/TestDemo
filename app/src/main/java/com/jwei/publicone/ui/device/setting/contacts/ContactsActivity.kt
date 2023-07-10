package com.jwei.publicone.ui.device.setting.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.reflect.TypeToken
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ContactBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityContactsBinding
import com.jwei.publicone.databinding.ItemContactsBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.device.bean.ContactsItem
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.PermissionUtils
import com.jwei.publicone.utils.ToastUtils
import com.jwei.publicone.utils.manager.AppTrackingManager
import com.jwei.publicone.viewmodel.DeviceModel
import java.nio.charset.StandardCharsets

/**
 * Created by Android on 2022/5/25.
 */
class ContactsActivity : BaseActivity<ActivityContactsBinding, DeviceModel>(ActivityContactsBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    companion object {
        //最大数
        public const val MAX_CONTACT_NUM = 10
    }

    private val TAG: String = ContactsActivity::class.java.simpleName

    //等待loading
    private lateinit var loadDialog: Dialog

    private var mData = arrayListOf<ContactsItem>()

    private lateinit var addContactResultLauncher: ActivityResultLauncher<Intent>


    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.device_set_contacts)
        loadDialog = DialogUtils.showLoad(this)
        setViewsClickListener(this, binding.btnAdd, binding.btnAdd2)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@ContactsActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        initResultLauncher()
    }

    private fun initAdapter(): CommonAdapter<ContactsItem, ItemContactsBinding> {
        return object : CommonAdapter<ContactsItem, ItemContactsBinding>(mData) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemContactsBinding {
                return ItemContactsBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemContactsBinding, t: ContactsItem, position: Int) {
                v.tvName.text = AppUtils.biDiFormatterStr(t.name)
                v.tvPhone.text = t.phone
                v.cbSelected.visibility = View.GONE
                if (position == (mData.size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }
                v.llItemLayout.setOnLongClickListener {
                    //LogUtils.d("删除")
                    if (!ControlBleTools.getInstance().isConnect) {
                        ToastUtils.showToast(R.string.device_no_connection)
                        return@setOnLongClickListener true
                    }
                    removeContact(position)
                    return@setOnLongClickListener true
                }
            }
        }
    }

    override fun initData() {
        super.initData()

        viewModel.deviceSettingLiveData.getContactList().observe(this) {
            if (it == null) return@observe
            LogUtils.d(TAG, "contacts:" + GsonUtils.toJson(it))
            mData.clear()
            val list = mutableListOf<ContactsItem>()
            for (c in it) {
                list.add(ContactsItem(c.contacts_name, c.contacts_number, true))
            }
            mData.addAll(list)
            refreshUi()
        }
        getDeviceContacts()
    }

    fun getDeviceContacts() {
        if (!loadDialog.isShowing) {
            loadDialog.show()
        }
        ControlBleTools.getInstance().getContactList(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialog)
                ToastUtils.showSendCmdStateTips(state)
            }
        })
    }

    /**
     * 刷新UI
     */
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun refreshUi() {
        if (mData.size == 0) {
            binding.llData.visibility = View.GONE
            binding.llNoData.visibility = View.VISIBLE
            return
        }
        binding.llData.visibility = View.VISIBLE
        binding.llNoData.visibility = View.GONE
        binding.recycler.adapter?.notifyDataSetChanged()
        binding.tvContactsNum.text = "${getString(R.string.device_set_contacts)} ${mData.size}/$MAX_CONTACT_NUM"
        if (mData.size < 10) {
            binding.btnAdd.isEnabled = true
            binding.btnAdd2.isEnabled = true
        } else if (mData.size == 10) {
            binding.btnAdd.isEnabled = false
            binding.btnAdd2.isEnabled = false
        }
    }

    private fun initResultLauncher() {
        addContactResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult> {
            override fun onActivityResult(result: ActivityResult?) {
                if (result != null && result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val json = result.data!!.getStringExtra(AddContactActivity.RESULT_EXTRA_KEY)
                    val list: List<ContactsItem> = GsonUtils.fromJson(json, object : TypeToken<List<ContactsItem>>() {}.type)
                    addContact(list)
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnAdd.id, binding.btnAdd2.id -> {
                if (mData.size < MAX_CONTACT_NUM) {
                    PermissionUtils.checkRequestPermissions(lifecycle, getString(R.string.add_contact), arrayOf(Manifest.permission.READ_CONTACTS)) {
                        val intent = Intent(this, AddContactActivity::class.java)
                        intent.putExtra(AddContactActivity.DATA_KRY, GsonUtils.toJson(mData))
                        addContactResultLauncher.launch(intent)
                    }
                }
            }
        }
    }

    /**
     * 添加联系人
     */
    private fun addContact(data: List<ContactsItem>) {
        loadDialog.show()

        if (!ControlBleTools.getInstance().isConnect) {
            ToastUtils.showToast(R.string.device_no_connection)
            DialogUtils.dismissDialog(loadDialog)
            return
        }
        val list = arrayListOf<ContactBean>()
        for (item in data) {
            val contactBean = ContactBean()
            contactBean.contacts_name = item.name
            interceptName(contactBean)
            contactBean.contacts_number = item.phone
            list.add(contactBean)
        }
        LogUtils.d("setContactList" + GsonUtils.toJson(list))
        ControlBleTools.getInstance().setContactList(list, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                if (state != SendCmdState.SUCCEED) {
                    DialogUtils.dismissDialog(loadDialog)
                    ToastUtils.showSendCmdStateTips(state)
                } else {
                    getDeviceContacts()
                    if (list.isNotEmpty()) {
                        AppTrackingManager.saveOnlyBehaviorTracking("6","1")
                    }
                }
            }
        })

    }


    private fun interceptName(contactBean: ContactBean) {
        while (contactBean.contacts_name.toByteArray(StandardCharsets.UTF_8).size >= 52) {
            contactBean.contacts_name = contactBean.contacts_name.substring(0, contactBean.contacts_name.length - 1)
        }
    }

    /**
     * 匹配联系人名称是否一致
     */
    private fun matchContactsName(deviceContactsName: String, contactsName: String): Boolean {
        LogUtils.d("deviceContactsName: ${deviceContactsName.toByteArray(StandardCharsets.UTF_8).size}")
        LogUtils.d("contactsName: ${contactsName.toByteArray(StandardCharsets.UTF_8).size}")
        if (TextUtils.equals(deviceContactsName, contactsName)) return true
        if (deviceContactsName.endsWith("...")) {
            val name = deviceContactsName.substring(0, deviceContactsName.length - 3)
            if (contactsName.startsWith(name)) return true
        }
        return false
    }

    /**
     * 删除联系人
     */
    private fun removeContact(position: Int) {
        if (position >= mData.size) return

        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.contact_del_tip),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun OnOK() {

                    loadDialog.show()
                    val list = arrayListOf<ContactBean>()
                    for (d in mData) {
                        list.add(ContactBean().apply {
                            this.contacts_name = d.name
                            this.contacts_number = d.phone
                        })
                    }
                    list.removeAt(position)
                    ControlBleTools.getInstance().setContactList(list, object : ParsingStateManager.SendCmdStateListener(lifecycle) {
                        override fun onState(state: SendCmdState) {
                            if (state != SendCmdState.SUCCEED) {
                                DialogUtils.dismissDialog(loadDialog)
                                ToastUtils.showSendCmdStateTips(state)
                            } else {
                                getDeviceContacts()
                            }
                        }
                    })

                }

                override fun OnCancel() {}
            })
        dialog.show()
    }

}