package com.smartwear.publicwatch.ui.guide

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.viewpager.widget.ViewPager
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.base.BaseApplication
import com.smartwear.publicwatch.base.BaseViewModel
import com.smartwear.publicwatch.databinding.ActivityGuidePageBinding
import com.smartwear.publicwatch.ui.HomeActivity
import com.smartwear.publicwatch.ui.adapter.BannerPagerAdapter
import com.smartwear.publicwatch.ui.adapter.MZViewHolder
import com.smartwear.publicwatch.utils.*

class GuidePageActivity : BaseActivity<ActivityGuidePageBinding, BaseViewModel>(ActivityGuidePageBinding::inflate, BaseViewModel::class.java), View.OnClickListener {

    private var mParentHeight: Int = 0
    private var mParentWidth: Int = 0

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnFinish.id -> {
//                SpUtils.setValue(SpUtils.USER_IS_LOGIN, "1")
                startActivity(Intent(this, HomeActivity::class.java))
                this.finish()
                ManageActivity.cancelAll()
            }
        }
    }

    override fun setTitleId(): Int {
        return binding.topView.id
    }

    override fun initView() {
        super.initView()
        //TODO UI:直接完成，不需要等待最后一页
        /* binding.btnFinish.isEnabled = false
         binding.btnFinish.setBackgroundResource(R.drawable.login_home_login_grey_btn)*/
        setViewsClickListener(this, binding.btnFinish)

        /* fragmentList.add(GuideFragment01())
         fragmentList.add(GuideFragment02())
         fragmentList.add(GuideFragment03())
         fragmentList.add(GuideFragment04())*/

//        val fm: FragmentManager = supportFragmentManager
        val arrayListOf = if (AppUtils.isZh(BaseApplication.mContext)) arrayListOf(
            R.mipmap.guide01, R.mipmap.guide02,
            R.mipmap.guide03, R.mipmap.guide04
        ) else arrayListOf(
            R.mipmap.guide01_en, R.mipmap.guide02_en,
            R.mipmap.guide03_en, R.mipmap.guide04_en
        )
        val bannerViewHolder = BannerViewHolder()
        val mAdapter = BannerPagerAdapter(arrayListOf, { bannerViewHolder }, false)
        mAdapter.setUpViewViewPager(binding.vpGuide)
        binding.pageIndicator.initIndicator(4)
//        binding.vpGuide.pageMargin = ScaleUtils.dip2px(20f)

        binding.vpGuide.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int,
            ) {
            }

            override fun onPageSelected(position: Int) {
                binding.pageIndicator.setSelectedPage(position % 4)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }

        })
        binding.vpGuide.post {
            mParentHeight = binding.vpGuide.height
            mParentWidth = binding.vpGuide.width

            val imgRes = R.mipmap.guide01
            val widthAndHeight = ViewUtils.getImageWidthAndHeight(this@GuidePageActivity.resources, imgRes)
            val imageWidth = widthAndHeight?.get(0)
            val imageHeight = widthAndHeight?.get(1)
//                LogUtils.i("GuidePageActivity", " widthAndHeight " + widthAndHeight.contentToString() + " measuredHeight " + this@GuidePageActivity.mParentHeight)
            if (widthAndHeight?.isNotEmpty() == true && mParentHeight > 0
                && (imageWidth!! > mParentWidth || imageHeight!! > mParentHeight)
            ) {
                val min = kotlin.math.min(mParentWidth.toFloat() / imageWidth, mParentHeight.toFloat() / imageHeight!!)
                binding.vpGuide.layoutParams.width = (imageWidth.toFloat() * min).toInt()
                binding.vpGuide.layoutParams.height = (imageHeight.toFloat() * min).toInt()
                binding.parent.requestLayout()
            }

            binding.vpGuide.apply {
                adapter = mAdapter
            }
        }
    }

    override fun initData() {
        super.initData()
        SpUtils.setValue(SpUtils.USER_IS_LOGIN, "1")
    }

    inner class BannerViewHolder : MZViewHolder<Int?> {
        private var bannerImage: ImageView? = null

        override fun createView(context: Context?): View {
            // 返回页面布局
            val view: View = LayoutInflater.from(context).inflate(R.layout.banner_intro_item, null)
            bannerImage = view.findViewById(R.id.banner_image)
            return view
        }

        override fun onBind(context: Context?, position: Int, data: Int?) {
            if (bannerImage != null) {
                data?.let { GlideApp.with(this@GuidePageActivity).load(it).into(bannerImage!!) }
            }
        }
    }


}