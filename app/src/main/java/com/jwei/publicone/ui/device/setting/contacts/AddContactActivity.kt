package com.jwei.publicone.ui.device.setting.contacts

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.*
import com.google.gson.reflect.TypeToken
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityAddContactBinding
import com.jwei.publicone.databinding.ItemContactsBinding
import com.jwei.publicone.dialog.DialogUtils
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.device.bean.ContactsItem
import com.jwei.publicone.ui.device.bean.PhoneDtoModel
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.PhoneUtil
import com.jwei.publicone.viewmodel.DeviceModel
import java.nio.charset.StandardCharsets

/**
 * Created by Android on 2022/5/26.
 */
class AddContactActivity : BaseActivity<ActivityAddContactBinding, DeviceModel>(ActivityAddContactBinding::inflate, DeviceModel::class.java), View.OnClickListener {

    companion object {
        const val DATA_KRY = "data"
        const val RESULT_EXTRA_KEY = "PhoneDtoModel"
    }

    private val TAG: String = AddContactActivity::class.java.simpleName

    //等待loading
    private lateinit var loadDialog: Dialog

    private var mAlreadyAddData = arrayListOf<ContactsItem>()

    private var mDatas = arrayListOf<ContactsItem>()
    private var mSearchDatas = arrayListOf<ContactsItem>()

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.add_contact)
        binding.noData.tvTips.text = getString(R.string.search_contact_no_found)
        loadDialog = DialogUtils.showLoad(this)

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(this@AddContactActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        event()
    }

    private fun initAdapter(): CommonAdapter<ContactsItem, ItemContactsBinding> {
        val data = arrayListOf<ContactsItem>().apply { addAll(mDatas) }

        fun getSelectedData(): List<ContactsItem> {
            val list = mDatas.filter { it.isChecked }
            return list
        }

        return object : CommonAdapter<ContactsItem, ItemContactsBinding>(data) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemContactsBinding {
                return ItemContactsBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemContactsBinding, t: ContactsItem, position: Int) {
                v.tvName.text = AppUtils.biDiFormatterStr(t.name)
                v.tvPhone.text = t.phone
                if (!t.isChecked) {
                    v.llItemLayout.alpha = if (getSelectedData().size >= ContactsActivity.MAX_CONTACT_NUM) 0.6f else 1f
                } else {
                    v.llItemLayout.alpha = 1f
                }

                v.cbSelected.isChecked = t.isChecked
                v.cbSelected.setOnClickListener {
                    if (v.cbSelected.isChecked) {
                        if (getSelectedData().size < ContactsActivity.MAX_CONTACT_NUM) {
                            t.isChecked = v.cbSelected.isChecked
                            if (getSelectedData().size == ContactsActivity.MAX_CONTACT_NUM) {
                                com.jwei.publicone.utils.ToastUtils.showToast(R.string.contact_max_tips)
                            }
                        } else {
                            t.isChecked = !v.cbSelected.isChecked
                        }
                    } else {
                        t.isChecked = v.cbSelected.isChecked
                    }
                    notifyDataSetChanged()
                }

                if (position == (getSelectedData().size - 1)) {
                    v.viewLine01.visibility = View.GONE;
                } else {
                    v.viewLine01.visibility = View.VISIBLE;
                }
            }
        }
    }

    override fun initData() {
        super.initData()
        mAlreadyAddData = GsonUtils.fromJson(intent.getStringExtra(DATA_KRY) ?: "", object : TypeToken<List<ContactsItem>>() {}.type)
        getContacts()
    }

    /**
     * 获取联系人
     */
    private fun getContacts() {
        ThreadUtils.executeByIo(object : ThreadUtils.Task<List<PhoneDtoModel>>() {
            override fun doInBackground(): List<PhoneDtoModel> {
                return PhoneUtil.getPhoneAllContacts(this@AddContactActivity)
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onSuccess(result: List<PhoneDtoModel>?) {
                LogUtils.d("all list : " + GsonUtils.toJson(result))
                if (result != null) {
                    mDatas.clear()
                    for (item in result.sortedBy { it.name }) {
                        mDatas.add(ContactsItem(item.name, item.telPhone.replace("-", "").replace(" ", "").trim(), false))
                        for (a in mAlreadyAddData) {
                            if (TextUtils.equals(a.phone, item.telPhone.replace("-", "").replace(" ", "").trim()) &&
                                matchContactsName(a.name, item.name)
                            ) {
                                mDatas.removeAt(mDatas.size - 1)
                                mDatas.add(ContactsItem(item.name, item.telPhone.replace("-", "").replace(" ", "").trim(), true))
                                break
                            }
                        }
                    }
                    search()
                }
            }

            override fun onCancel() {}

            override fun onFail(t: Throwable?) {
                com.jwei.publicone.utils.LogUtils.e(TAG, "获取联系人异常：$t", true)
            }
        })
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

    private fun event() {
        setViewsClickListener(this, binding.ivSearch, binding.btnSave)

        binding.etSearch.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    KeyboardUtils.hideSoftInput(this@AddContactActivity)
                    search()
                }
                return false
            }
        })
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                search()
            }

        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivSearch.id -> {
                KeyboardUtils.hideSoftInput(this@AddContactActivity)
                search()
            }
            binding.btnSave.id -> {
                saveData()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search() {
        val search = binding.etSearch.text.toString().trim()
        mSearchDatas.clear()
        for (item in mDatas) {
            if (item.name.uppercase().contains(search.uppercase()) || item.phone.uppercase().contains(search.uppercase())) {
                mSearchDatas.add(item)
            }
        }
        (binding.recycler.adapter as CommonAdapter<ContactsItem, ItemContactsBinding>?)?.apply {
            this.mData.clear()
            this.mData.addAll(mSearchDatas)
            binding.recycler.visibility = if (this.mData.size > 0) View.VISIBLE else View.INVISIBLE
            binding.noData.layoutNoData.visibility = if (this.mData.size == 0) View.VISIBLE else View.INVISIBLE
            binding.btnSave.visibility = if (mData.size == 0) View.GONE else View.VISIBLE
            notifyDataSetChanged()
        }
    }

    private fun saveData() {
        val list = mDatas.filter { it.isChecked }
        val intent = intent
        intent.putExtra(RESULT_EXTRA_KEY, GsonUtils.toJson(list))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }


}