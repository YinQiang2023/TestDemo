package com.smartwear.xzfit.ui.device.setting.more

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.*
import com.zhapp.ble.ControlBleTools
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivityDevMoreShortReplyBinding
import com.smartwear.xzfit.databinding.DialogShortReplyBinding
import com.smartwear.xzfit.databinding.ItemMsgNotifyBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.dialog.customdialog.CustomDialog
import com.smartwear.xzfit.dialog.customdialog.MyDialog
import com.smartwear.xzfit.ui.adapter.CommonAdapter
import com.smartwear.xzfit.utils.SpUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.xzfit.utils.ProhibitEmojiUtils
import kotlin.text.StringBuilder

/**
 * Created by Android on 2021/10/12.
 * 快捷回复设置
 */
class ShortReplyActivity : BaseActivity<ActivityDevMoreShortReplyBinding, DeviceModel>(
    ActivityDevMoreShortReplyBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private lateinit var loadDialg: android.app.Dialog

    private lateinit var mDeviceMac: String

    private var mDatas = arrayListOf<String>()

    private lateinit var mReplyMyDialog: MyDialog

    private var mSelectedData = ""
    private var mSelectedIndex = -1

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.dev_more_set_short_reply)

        setRightIconOrTitle(R.mipmap.icon_add, clickListener = this)

        loadDialg = DialogUtils.showLoad(this)

        binding.tvDelTip.text = getString(R.string.delete_list_reply)


        mDeviceMac = SpUtils.getValue(SpUtils.DEVICE_MAC, "test")
        if (mDeviceMac.isEmpty()) {
            finish()
            return
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShortReplyActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun initData() {
        super.initData()

        //获取设备的快捷回复数据
        loadDialg.show()
        viewModel.getDeviceShortReply(object : ParsingStateManager.SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                DialogUtils.dismissDialog(loadDialg)
                com.smartwear.xzfit.utils.ToastUtils.showSendCmdStateTips(state) {
                    finish()
                }
            }
        })

        viewModel.deviceSettingLiveData.getShortReply().observe(this) {
            if (it == null) return@observe
            LogUtils.d("快捷回复 ->" + it.size)
            /*if (it.size == 0) { //获取设备为空，检查本地
                val json = SpUtils.getSPUtilsInstance()
                    .getString(SpUtils.DEVICE_SHORT_REPLY_LIST + mDeviceMac, "")
                if (json.isNotEmpty()) {
                    val datas: ArrayList<String> =
                        GsonUtils.fromJson(json, object : TypeToken<ArrayList<String>>() {}.type)
                    LogUtils.e("本地快捷回复"+datas)
                    if (!datas.isNullOrEmpty()) {
                        mDatas.clear()
                        mDatas.addAll(datas)
                        loadDialg.show()
                        LogUtils.d("快捷回复 设置->${GsonUtils.toJson(mDatas)}")
                        viewModel.postDeviceShortReply(mDatas,MySendCallBack())
                    }
                }
            } else {*/
            mDatas.clear()
            mDatas.addAll(it)
            /*  SpUtils.getSPUtilsInstance()
                  .put(SpUtils.DEVICE_SHORT_REPLY_LIST + mDeviceMac, GsonUtils.toJson(mDatas))*/
//            }
            binding.clNoData.visibility = if (mDatas.size > 0) View.GONE else View.VISIBLE
            binding.recyclerView.visibility = if (mDatas.size > 0) View.VISIBLE else View.GONE
            binding.tvDelTip.visibility = if (mDatas.size > 0) View.VISIBLE else View.GONE
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun initAdapter(): CommonAdapter<String, ItemMsgNotifyBinding> {
        return object : CommonAdapter<String, ItemMsgNotifyBinding>(mDatas) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemMsgNotifyBinding {
                return ItemMsgNotifyBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemMsgNotifyBinding, t: String, position: Int) {
                v.icon.visibility = View.GONE
                v.mSwitch.visibility = View.GONE
                StringBuilder().append(position + 1).append(". ").append(t).let {
                    v.tvName.text = it.toString()
                }
                v.tvName.setPadding(0, 0, ConvertUtils.dp2px(30f), 0)
                v.ivNext.visibility = View.VISIBLE
                v.ivNext.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@ShortReplyActivity,
                        R.mipmap.icon_item_edit
                    )
                )

                v.root.setOnClickListener {
                    mSelectedData = t
                    mSelectedIndex = position
                    showCreateOrEditReplyDialog()
                }
                v.root.setOnLongClickListener {
                    showDelDialog(position)
                    true
                }
                v.viewLine01.visibility = if (position == (mDatas.size - 1)) View.VISIBLE else View.GONE
            }
        }
    }

    /**
     * 提示已经最大
     * */
    private fun showMaxHint() {
        val dialog = DialogUtils.showDialogTitleAndOneButton(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.dialog_max_reply_tips),
            getString(R.string.dialog_confirm_btn),
            null
        )
        dialog.show()
    }

    //region 回复编辑或添加
    private lateinit var dialogBinding: DialogShortReplyBinding

    /**
     * 显示增加或编辑回复
     * */
    @SuppressLint("NotifyDataSetChanged")
    private fun showCreateOrEditReplyDialog() {
        if (!ControlBleTools.getInstance().isConnect) {
            com.smartwear.xzfit.utils.ToastUtils.showToast(R.string.device_no_connection)
            return
        }
        if (!::mReplyMyDialog.isInitialized) {
            dialogBinding = DialogShortReplyBinding.inflate(layoutInflater)
            mReplyMyDialog = CustomDialog
                .builder(this)
                .setContentView(dialogBinding.root)
                .setCancelable(false)
                .build()
            dialogBinding.btnTvLeft.setOnClickListener { mReplyMyDialog.dismiss() }
            //EditTextUtils.evitTextLimit(dialogBinding.etContext)
            //val filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(30)
            dialogBinding.etContext.filters = ProhibitEmojiUtils.inputFilterProhibitEmoji(30)
            dialogBinding.rootLayout.setOnClickListener {
                KeyboardUtils.showSoftInput(dialogBinding.etContext)
            }

            dialogBinding.btnTvRight.setOnClickListener {
                val data = dialogBinding.etContext.text.toString().trim()
                if (data.isEmpty()) {
                    ToastUtils.showShort(R.string.reply_null_tips)
                    return@setOnClickListener
                }
                if (mSelectedData.isEmpty()) { //新建
                    mDatas.add(data)
                } else { //编辑
                    if (!TextUtils.equals(data, mSelectedData)) {
                        mDatas.set(mSelectedIndex, data)
                    }
                }
                binding.recyclerView.adapter?.notifyDataSetChanged()
                /* SpUtils.getSPUtilsInstance()
                     .put(SpUtils.DEVICE_SHORT_REPLY_LIST + mDeviceMac, GsonUtils.toJson(mDatas))*/
                loadDialg.show()
                LogUtils.d("快捷回复 设置->${GsonUtils.toJson(mDatas)}")
                viewModel.postDeviceShortReply(mDatas, MySendCallBack())

                mReplyMyDialog.dismiss()
                mSelectedIndex = -1
                mSelectedData = ""
            }
        }

        if (mSelectedData.isNotEmpty() && mSelectedIndex != -1) {
            dialogBinding.etContext.setText(mSelectedData)
            dialogBinding.etContext.setSelection(dialogBinding.etContext.text.toString().trim().length)
            /*StringBuilder().append(getString(R.string.edit_reply)).append("${mSelectedIndex + 1}")
                .apply {
                    dialogBinding.tvTitle.text = this.toString()
                }*/
            dialogBinding.tvTitle.text = getString(R.string.edit_reply)
        } else {
            dialogBinding.etContext.setText("")
            dialogBinding.tvTitle.text = getString(R.string.add_reply)
        }
        mReplyMyDialog.show()
    }
    //endregion

    /**
     * 删除提示
     * */
    fun showDelDialog(index: Int) {
        val dialog = DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            /*getString(R.string.dialog_title_tips)*/null,
            getString(R.string.delete_list_reply_tips),
            getString(R.string.dialog_cancel_btn),
            getString(R.string.dialog_confirm_btn),
            object : DialogUtils.DialogClickListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun OnOK() {
                    mDatas.removeAt(index)
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                    loadDialg.show()
                    viewModel.postDeviceShortReply(mDatas, MySendCallBack())
                    /*SpUtils.getSPUtilsInstance()
                        .put(SpUtils.DEVICE_SHORT_REPLY_LIST + mDeviceMac, GsonUtils.toJson(mDatas))*/
                }

                override fun OnCancel() {}
            })
        dialog.show()
    }


    override fun onClick(v: View) {
        when (v.id) {
            ivRightIcon?.id -> {
                if (mDatas.size < 5) {
                    mSelectedData = ""
                    mSelectedIndex = -1
                    showCreateOrEditReplyDialog()
                } else {
                    showMaxHint()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mReplyMyDialog.isInitialized && mReplyMyDialog.isShowing) {
            mReplyMyDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    inner class MySendCallBack : ParsingStateManager.SendCmdStateListener(lifecycle) {
        override fun onState(state: SendCmdState) {
            loadDialg.dismiss()
            viewModel.getDeviceShortReply(null)
            com.smartwear.xzfit.utils.ToastUtils.showSendCmdStateTips(state)
        }
    }

}