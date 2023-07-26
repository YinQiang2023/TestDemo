package com.jwei.xzfit.ui.region

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.databinding.SelectRegionActivityBinding
import com.jwei.xzfit.ui.login.LoginActivity
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils
import com.jwei.xzfit.viewmodel.UserModel


class SelectRegionActivity : BaseActivity<SelectRegionActivityBinding, UserModel>(SelectRegionActivityBinding::inflate, UserModel::class.java) {
    private val TAG: String = SelectRegionActivity::class.java.simpleName
    private var mRegion: RegionBean? = null

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        binding.tvContent.text = "\u3000\u3000${getString(R.string.select_region_tip)}"
        binding.tvTitle.setOnClickListener { }

        //选择地区
        binding.llSelectRegion.setOnClickListener {
            val intent = Intent(this, RegionSettingActivity::class.java)
            startActivityForResult(intent, RegionSettingActivity.KEY_TAG)
        }

        //继续下一步
        binding.btContinue.setOnClickListener {
            if (mRegion != null) {
                SpUtils.setValue(SpUtils.SERVICE_REGION_COUNTRY_CODE, mRegion!!.countryIsoCode)
                SpUtils.setValue(SpUtils.SERVICE_REGION_AREA_CODE, mRegion!!.areaCode)
                if (RegionSettingActivity.isChinaServiceUrl(mRegion!!.countryIsoCode, mRegion!!.areaCode)) {
                    SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE1)
                } else {
                    SpUtils.setValue(SpUtils.SERVICE_ADDRESS, SpUtils.SERVICE_ADDRESS_TO_TYPE2)
                }
                startActivity(Intent(this@SelectRegionActivity, LoginActivity::class.java))
                finish()
            } else {
                ToastUtils.showToast(R.string.select_region_no_select)
            }
        }
    }

    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RegionSettingActivity.KEY_TAG) {
                mRegion = data?.getSerializableExtra(RegionSettingActivity.KEY_REGION) as RegionBean?
                Log.i(TAG, "onActivityResult  " + mRegion.toString())
                if (mRegion != null) {
                    val name = RegionSettingActivity.getRegionName(mRegion!!.countryIsoCode, mRegion!!.areaCode)
                    binding.tvRegionName.text = name
                }
            }
        }
    }
}