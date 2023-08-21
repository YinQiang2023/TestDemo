package com.smartwear.xzfit.ui.sport

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blankj.utilcode.util.*
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.zhapp.ble.parsing.SportParsing
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.ActivitySportDataBinding
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.SportTypeUtils
import com.smartwear.xzfit.viewmodel.SportModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

/**
 * Created by Android on 2021/10/11.
 * 运动数据查看
 */
class SportDataActivity : BaseActivity<ActivitySportDataBinding, SportModel>(
    ActivitySportDataBinding::inflate, SportModel::class.java
), View.OnClickListener {
    //data
    private var info: SportModleInfo? = null

    //TAB 标题
    private val tabs: MutableList<String> = mutableListOf()
    private val fragments: MutableList<Fragment> = mutableListOf()

    private val locusFragment = SportLocusFragment()
    private val chartFragment = SportChartFragment()
    private val detailsFragment = SportDetailsFragment()

    private var dialog: Dialog? = null

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        info = viewModel.sportLiveData.getSportModleInfo().value
        if (info != null) {
            setTvTitle(SportTypeUtils.getSportTypeName(info!!.dataSources, info!!.exerciseType))
        } else {
            finish()
            return
        }
        setRightIconOrTitle(R.mipmap.img_share, clickListener = this)
        //设备二四+五(游池游泳)无地图
        if (info!!.dataSources == 2 &&
            (SportParsing.isData2(info!!.exerciseType.toInt()) ||
                    SportParsing.isData4(info!!.exerciseType.toInt()) ||
                    info!!.exerciseType.toInt() == 200)
        ) {
            tabs.addAll(
                listOf(
                    //getString(R.string.sport_locus),
                    getString(R.string.sport_chart),
                    getString(R.string.sport_details)
                )
            )
            //fragment
            fragments.addAll(
                listOf(
//                    locusFragment,
                    chartFragment,
                    detailsFragment
                )
            )
        } else {
            tabs.addAll(
                listOf(
                    getString(R.string.sport_locus),
                    getString(R.string.sport_chart),
                    getString(R.string.sport_details)
                )
            )
            //fragment
            fragments.addAll(
                listOf(
                    locusFragment,
                    chartFragment,
                    detailsFragment
                )
            )
        }


        binding.vp.adapter = ViewPagerAdapter(this, fragments)
        binding.vp.offscreenPageLimit = fragments.size
        //绑定vp tab
        TabLayoutMediator(binding.tabLayout, binding.vp, true, true,
            object : TabLayoutMediator.TabConfigurationStrategy {
                override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
                    tab.text = tabs.get(position)
                }
            }).attach()
    }

    override fun initData() {
        super.initData()

        viewModel.sportLiveData.getSportModleInfo().value?.sportTime?.let {
            val item = TimeUtils.millis2String(it * 1000, com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat("yyyy-MM-dd HH:mm"))
            binding.tvDate.text = item.split(" ")[0]
            binding.tvTime.text = item.split(" ")[1]
        }
    }

    inner class ViewPagerAdapter(fa: FragmentActivity, private val tabs: MutableList<Fragment>) :
        FragmentStateAdapter(fa) {
        override fun getItemCount(): Int {
            return tabs.size
        }

        override fun createFragment(position: Int): Fragment {
            return tabs.get(position)
        }
    }

    override fun onClick(v: View?) {
        if (v?.id == ivRightIcon?.id) {
            share()
        }
    }


    /**
     * 分享
     * */
    fun share() {
        //binding.vp.currentItem = 0
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            dialog = DialogUtils.dialogShowLoad(this@SportDataActivity)
            val bmps = arrayListOf<Bitmap>()
            bmps.apply {
                //轨迹截图 只有app运动，设备运动（除去类型二四）
                if (info!!.dataSources == 0 ||
                    info!!.dataSources == 1 ||
                    (info!!.dataSources == 2
                            && !SportParsing.isData2(info!!.exerciseType.toInt())
                            && !SportParsing.isData4(info!!.exerciseType.toInt())
                            && info!!.exerciseType.toInt() != 200 //泳池游泳无地图
                            )
                ) {
                    //google 地图且google服务可用
                    if (AppUtils.isEnableGoogleMap()) {
                        if (AppUtils.checkGooglePlayServices(BaseApplication.mContext)) {
                            locusFragment.createDataBitMap()?.let {
                                add(it)
                            }
                        }
                    } else {
                        locusFragment.createDataBitMap()?.let {
                            add(it)
                        }
                    }
                }
                add(detailsFragment.createDataBitMap())
                chartFragment.createDataBitMap()?.let {
                    add(it)
                }
            }
            DialogUtils.dismissDialog(dialog)
            EventBus.getDefault().postSticky(EventMessage(EventAction.ACTION_SHARE_SPORT_DATA, bmps))
            startActivity(Intent(this@SportDataActivity, SportShareActivity::class.java))
        }

    }
}