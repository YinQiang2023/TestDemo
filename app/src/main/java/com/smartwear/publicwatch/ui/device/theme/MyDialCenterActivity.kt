package com.smartwear.publicwatch.ui.device.theme

import android.app.Dialog
import android.view.View
import com.blankj.utilcode.util.ConvertUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.WatchFaceCallBack
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.databinding.ActivityMyDialCenterBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.ui.device.bean.WatchSystemBean
import com.smartwear.publicwatch.ui.livedata.RefreshMyDialListState
import com.smartwear.publicwatch.utils.FileUtils
import com.smartwear.publicwatch.utils.GlideApp
import com.smartwear.publicwatch.utils.SpUtils
import com.smartwear.publicwatch.utils.ToastUtils
import com.smartwear.publicwatch.viewmodel.DeviceModel
import java.lang.ref.WeakReference

class MyDialCenterActivity : BaseActivity<ActivityMyDialCenterBinding, DeviceModel>(ActivityMyDialCenterBinding::inflate, DeviceModel::class.java), View.OnClickListener {
    private val TAG = MyDialCenterActivity::class.java.simpleName
    private var dialog: Dialog? = null
    var data: WatchSystemBean? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSet.id -> {
                if (!ControlBleTools.getInstance().isConnect) {
                    ToastUtils.showToast(getString(R.string.device_no_connection))
                    return
                }
                dialog?.show()
                data?.dialCode?.let { viewModel.setDialDefault(it) }
                RefreshMyDialListState.postValue(true)
            }
            binding.btnDelete.id -> {
                dialog = DialogUtils.showDialogContentAndTwoBtn(this, getString(R.string.theme_my_dial_delete_tips), getString(R.string.dialog_cancel_btn),
                    getString(R.string.dialog_confirm_btn), object : DialogUtils.DialogClickListener {
                        override fun OnOK() {
                            dialog = DialogUtils.showLoad(this@MyDialCenterActivity)
                            data?.dialCode?.let { viewModel.deleteDialDefault(it) }
                            RefreshMyDialListState.postValue(true)
                            dialog?.show()
                        }

                        override fun OnCancel() {
                        }
                    })
                dialog?.show()
            }
        }
    }

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.theme_center_tab_my_center)

        data = intent.getSerializableExtra("data") as WatchSystemBean?

        setViewsClickListener(this, binding.btnSet, binding.btnDelete)

        binding.btnSet.isEnabled = data?.isCurrent != true

        binding.btnDelete.isEnabled = data?.isRemove == true
        if (data?.isRemove == false) {
            binding.btnDelete.visibility = View.GONE
        }

        if (data?.dialImageUrl.isNullOrEmpty()) {
            //301为DIY表盘
            if (data?.dialCode?.substring(5, 8) == "301") {
                val path = SpUtils.getValue(SpUtils.DIY_RENDERINGS_PATH, "")
                if (path.isNotEmpty())
                    GlideApp.with(BaseApplication.mContext).load(ConvertUtils.bytes2Bitmap(FileUtils.getBytes(path))).into(binding.ivThemeMain)
            } else
                GlideApp.with(this).load(R.mipmap.no_data_transparent)
                    .into(binding.ivThemeMain)
        } else {
            GlideApp.with(this).load(data?.dialImageUrl)
//                .placeholder(R.mipmap.no_renderings)
//                .error(R.mipmap.no_renderings)
                .into(binding.ivThemeMain)
        }
        observe()
        dialog = DialogUtils.showLoad(this)
    }

    private fun observe() {
        CallBackUtils.watchFaceCallBack = MyWatchFaceCallBack(this)
    }

    class MyWatchFaceCallBack(activity: MyDialCenterActivity) : WatchFaceCallBack {
        private var wrActivity: WeakReference<MyDialCenterActivity>? = null

        init {
            wrActivity = WeakReference(activity)
        }

        override fun setWatchFace(isSet: Boolean) {
            wrActivity?.get()?.apply {
                if (isSet) {
                    binding.btnSet.setBackgroundResource(R.drawable.selector_public_button_1)
                    binding.btnSet.isEnabled = false
                    dismissDialog()
                    ToastUtils.showToast(R.string.set_success)
                    finish()
                } else {
                    ToastUtils.showToast(R.string.set_fail)
                }
            }
        }

        override fun removeWatchFace(isRemove: Boolean) {
            wrActivity?.get()?.apply {
                if (isRemove) {
                    binding.btnSet.setBackgroundResource(R.drawable.selector_public_button_1)
                    binding.btnSet.isEnabled = false
                    binding.btnDelete.setBackgroundResource(R.drawable.selector_public_button_1)
                    binding.btnDelete.isEnabled = false
                    dismissDialog()
                    ToastUtils.showToast(R.string.set_success)
                    finish()
                } else {
                    ToastUtils.showToast(R.string.set_fail)
                }
            }
        }

    }

    private fun dismissDialog() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
        }
    }

}