package com.jwei.publicone.ui.user

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSON
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.ActivityDataReductionBinding
import com.jwei.publicone.ui.device.bean.DeviceSettingBean
import com.jwei.publicone.utils.SpUtils
import java.util.ArrayList

/**
 * Created by Android on 2022/3/14.
 */
class DataReductionActivity : BaseActivity<ActivityDataReductionBinding, BaseViewModel>
    (ActivityDataReductionBinding::inflate, BaseViewModel::class.java), View.OnClickListener {

    private val list: MutableList<Bean> = ArrayList()

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    //产品功能列表
    private val deviceSettingBean by lazy {
        JSON.parseObject(SpUtils.getValue(SpUtils.DEVICE_SETTING, ""), DeviceSettingBean::class.java)
    }

    override fun onClick(v: View?) {

    }

    override fun initView() {
        super.initView()
        setTvTitle(R.string.data_reduction)
        setView()
    }

    private fun setView() {
        binding.layoutList.removeAllViews()
        var texts = resources.getStringArray(R.array.dataReductionString2List)
        var imgs = resources.obtainTypedArray(R.array.dataReductionImage2List)
        for (i in 0 until imgs.length()) {
            val bean = Bean()
            bean.img = imgs.getResourceId(i, 0)
            bean.text = texts[i]
            list.add(bean)
        }
        imgs.recycle()
        val inflater = LayoutInflater.from(this)
        for (i in list.indices) {
            if (checkLoad(list[i].text)) {
                val constraintLayout = inflater.inflate(R.layout.device_set_item, null)
                val image = constraintLayout.findViewById<ImageView>(R.id.icon)
                val ivNext = constraintLayout.findViewById<ImageView>(R.id.ivNext)
                val tvName = constraintLayout.findViewById<TextView>(R.id.tvName)
                val mSwitchCompat = constraintLayout.findViewById<SwitchCompat>(R.id.mSwitchCompat)
                val viewLine01 = constraintLayout.findViewById<View>(R.id.viewLine01)
                image.background = ContextCompat.getDrawable(this, list[i].img) //R.mipmap.icon_google_fit
                tvName.text = list[i].text  //getString(R.string.google_fit)
                mSwitchCompat.visibility = View.GONE
                ivNext.visibility = View.VISIBLE
                setViewsClickListener({
                    when (tvName.text.toString().trim()) {
                        getString(R.string.google_fit) -> {
                            startActivity(Intent(this, GoogleFitActivity::class.java))
                        }
                        getString(R.string.strava) -> {
                            startActivity(Intent(this, StravaActivity::class.java))
                        }
                    }
                }, constraintLayout)
                binding.layoutList.addView(constraintLayout)
                if (i == (list.size - 1)) {
                    viewLine01.visibility = View.GONE
                } else {
                    viewLine01.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * 设备是否支持功能
     * */
    private fun checkLoad(s: String?): Boolean {
        if (deviceSettingBean != null) {
            when (s) {
                getString(R.string.google_fit) -> {
                    return deviceSettingBean.settingsRelated.GoogleFit
                }
                getString(R.string.strava) -> {
                    return deviceSettingBean.settingsRelated.Strava
                }
            }
        }
        return true
    }

    class Bean {
        var img: Int = 0
        var text: String = ""
    }
}