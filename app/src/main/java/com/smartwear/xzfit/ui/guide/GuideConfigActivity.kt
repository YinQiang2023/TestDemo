package com.smartwear.xzfit.ui.guide

import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.reflect.TypeToken
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.base.BaseApplication
import com.smartwear.xzfit.databinding.ActivityGuideConfigBinding
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.device.bean.NotifyItem
import com.smartwear.xzfit.ui.device.setting.more.LanguageSetActivity
import com.smartwear.xzfit.ui.eventbus.EventAction
import com.smartwear.xzfit.ui.eventbus.EventMessage
import com.smartwear.xzfit.ui.guide.item.SelectNotificationFragment
import com.smartwear.xzfit.utils.StatusbarColorUtils
import com.smartwear.xzfit.viewmodel.DeviceModel
import com.smartwear.xzfit.ui.guide.item.SelectLanguageFragment
import com.smartwear.xzfit.ui.guide.item.SelectWeatherFragment
import com.smartwear.xzfit.utils.SpUtils
import org.greenrobot.eventbus.EventBus

class GuideConfigActivity : BaseActivity<ActivityGuideConfigBinding, DeviceModel>(
    ActivityGuideConfigBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    companion object {
        const val GUIDE_PAGE_DATA = "guidePageData"
        const val BIND_DEVICE_MAC = "mac"
    }

    private var isBrDevice = false
    private var guidePageData = ""
    private var bindDeviceMac: String? = null

    private val TAG: String = this::class.java.simpleName
    private var fragments: MutableList<Fragment> = mutableListOf()

    private var nowPosition = 0

    //是否消息通知页面
    private var isMsgNotificationGuide = false
    //是否允许提醒消息通知
    private var canShowNotifiSwitchHint = true


    override fun initView() {
        super.initView()
        StatusbarColorUtils.setStatusBarDarkIcon(this, true)
        setViewsClickListener(this, binding.btNext)

        bindDeviceMac = intent.getStringExtra(LanguageSetActivity.BIND_DEVICE_MAC)
        //检测bt配对
        EventBus.getDefault().post(EventMessage(EventAction.ACTION_HEADSET_BOND, bindDeviceMac))
        //数据格式1,2,3
        guidePageData = intent.getStringExtra(GUIDE_PAGE_DATA).toString()

        val guidePageArr = guidePageData.split(",")

        guidePageArr.forEach {
            when (it.toInt()) {
                1 -> fragments.add(SelectLanguageFragment())
                2 -> fragments.add(SelectNotificationFragment())
                3 -> fragments.add(SelectWeatherFragment())
            }
        }

        binding.pageIndicator.initIndicator(fragments.size)
        val vpAdapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size

            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        binding.vpGuide.apply {
            adapter = vpAdapter
            offscreenPageLimit = 3
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    chanceBtNextUI(position)
                }
            })
            setPageTransformer(DepthPageTransformer())
        }

        binding.vpGuide.currentItem = 0
        binding.pageIndicator.setSelectedPage(0)
        chanceBtNextUI(0)
        vpAdapter.notifyItemRangeChanged(0, fragments.size - 1)
    }

    override fun initData() {
        super.initData()
        isBrDevice = intent.getBooleanExtra("EXTAR_IS_BR_DEVICE", false)

    }


    private fun chanceBtNextUI(index: Int) {
        nowPosition = index
        binding.pageIndicator.setSelectedPage(index)
        if (index < fragments.size) {
            if (fragments.get(index) is SelectLanguageFragment) {
                binding.tvHint.text = getString(R.string.guide_hint_language)
                isMsgNotificationGuide = false
            } else if (fragments.get(index) is SelectNotificationFragment) {
                binding.tvHint.text = getString(R.string.guide_hint_call_notification)
                isMsgNotificationGuide = true
            } else if (fragments.get(index) is SelectWeatherFragment) {
                binding.tvHint.text = getString(R.string.guide_hint_weather)
                isMsgNotificationGuide = false
            }
        }
        binding.btNext.text = if (index != fragments.size - 1) getString(R.string.button_next) else getString(R.string.dialog_complete_btn)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btNext.id -> {
                if(canShowNotifiSwitchHint) {
                    if (isMsgNotificationGuide) {
                        if (!isNotifySwitchOpen()) {
                            canShowNotifiSwitchHint = false
                            showNotifySwitchHint()
                            return
                        }
                    }
                }
                //完成 / 下一步
                if (nowPosition == fragments.size - 1) {
                    setResult(RESULT_OK, intent)
                    closeNow()
                    return
                } else {
                    binding.vpGuide.currentItem = nowPosition + 1
                }
            }
        }
    }

    //region 消息通知开关提醒
    /**
     * 消息通知总开关是否打开
     */
    private fun isNotifySwitchOpen():Boolean{
        val notifyJson = SpUtils.getSPUtilsInstance().getString(SpUtils.DEVICE_MSG_NOTIFY_ITEM_LIST, "")
        val tempList: MutableList<NotifyItem>? = GsonUtils.fromJson(notifyJson, object : TypeToken<MutableList<NotifyItem>>() {}.type)
        if (!tempList.isNullOrEmpty()) {
            for (n in tempList) {
                //总开关状态
                if (n.type == 1 && n.isTypeHeader && !n.isCanNext) {
                    return n.isOpen
                }
            }
        }
        return false
    }

    private fun showNotifySwitchHint(){
        DialogUtils.showDialogTwoBtn(
            ActivityUtils.getTopActivity(),
            null,
            getString(R.string.notify_guided_hint),
            BaseApplication.mContext.getString(R.string.set_up_later),
            BaseApplication.mContext.getString(R.string.running_permission_set),
            object : DialogUtils.DialogClickListener {
                override fun OnOK() {
                    EventBus.getDefault().post(EventMessage(EventAction.ACTION_GUIDE_NOTIFY_SWITCH))
                }

                override fun OnCancel() {
                    //直接跳过
                    //binding.btNext.callOnClick()
                }
            }
        ).show()
    }
    //endregion

    //region vp2切换效果

    class DepthPageTransformer : ViewPager2.PageTransformer {
        private val MIN_SCALE = 0.75f
        override fun transformPage(view: View, position: Float) {
            view.apply {
                val pageWidth = width
                when {
                    position < -1 -> { // [-Infinity,-1)
                        // This page is way off-screen to the left.
                        alpha = 0f
                    }
                    position <= 0 -> { // [-1,0]
                        // Use the default slide transition when moving to the left page
                        alpha = 1f
                        translationX = 0f
                        translationZ = 0f
                        scaleX = 1f
                        scaleY = 1f
                    }
                    position <= 1 -> { // (0,1]
                        // Fade the page out.
                        alpha = 1 - position

                        // Counteract the default slide transition
                        translationX = pageWidth * -position
                        // Move it behind the left page
                        translationZ = -1f

                        // Scale the page down (between MIN_SCALE and 1)
                        val scaleFactor = (MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position)))
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    else -> { // (1,+Infinity]
                        // This page is way off-screen to the right.
                        alpha = 0f
                    }
                }
            }
        }
    }


    //endregion

    //region 屏蔽返回键
    /**
     * 屏蔽返回键
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false
        }
        return super.onKeyDown(keyCode, event)
    }
    //endregion

}