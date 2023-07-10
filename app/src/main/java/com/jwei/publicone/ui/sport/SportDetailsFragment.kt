package com.jwei.publicone.ui.sport

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.ImageUtils
import com.zhapp.ble.parsing.SportParsing
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseFragment
import com.jwei.publicone.databinding.FragmentSportDetailsBinding
import com.jwei.publicone.databinding.ItemSportDetailsDataBinding
import com.jwei.publicone.databinding.ItemSportHeartRangeBinding
import com.jwei.publicone.db.model.sport.SportModleInfo
import com.jwei.publicone.ui.adapter.CommonAdapter
import com.jwei.publicone.ui.sport.bean.SportDetailsDataBean
import com.jwei.publicone.ui.sport.bean.SportHeartRangeBean
import com.jwei.publicone.utils.AppUtils
import com.jwei.publicone.utils.SpannableStringTool
import com.jwei.publicone.utils.TimeUtils
import com.jwei.publicone.viewmodel.SportModel
import com.zhapp.ble.utils.UnitConversionUtils
import com.jwei.publicone.base.BaseApplication
import com.jwei.publicone.utils.manager.DevSportManager
import java.util.*
import kotlin.math.roundToInt

/**
 * Created by Android on 2021/10/14.
 * 运动数据详情
 */
class SportDetailsFragment : BaseFragment<FragmentSportDetailsBinding, SportModel>(
    FragmentSportDetailsBinding::inflate, SportModel::class.java
) {

    private var sportModleInfo: SportModleInfo? = null

    private var isNoData = false

    //region 数据列表模块
    private val mSportDetailsData = mutableListOf<SportDetailsDataBean>()
    //endregion

    //region 心率区间
    private val mSportHeartRangeData = mutableListOf<SportHeartRangeBean>()
    //endregion

    override fun initView() {
        super.initView()

        sportModleInfo = viewmodel.sportLiveData.getSportModleInfo().value
        if (sportModleInfo == null) {
            isNoData = true
            return
        }
        binding.noData.layoutNoData.visibility = View.GONE

        //region 数据列表模块
        binding.dataLayout.rvData.apply {
            layoutManager = GridLayoutManager(context, 2)
            setHasFixedSize(true)
            adapter = initAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.apply {
                        left = ConvertUtils.dp2px(10F)
                        if (TextUtils.equals(Locale.getDefault().language, "ar")) right = ConvertUtils.dp2px(10F)
                        bottom = ConvertUtils.dp2px(10F)
                        //第一二个设置top
                        val position: Int = parent.getChildAdapterPosition(view)
                        if (position == 0 || position == 1) {
                            top = ConvertUtils.dp2px(10F)
                        }
                    }
                }
            })
        }
        //endregion

        //region 数据列表模块
        mSportDetailsData.apply {
            add(
                SportDetailsDataBean(
                    R.mipmap.sport_time_checked,
                    getString(R.string.sport_time),
                    getString(R.string.no_data_sign),
                    ""
                )
            )
            if (sportModleInfo!!.dataSources == 0 ||
                (sportModleInfo!!.dataSources == 2 &&
                        (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())
                                )
                        )
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_calories_checked,
                        getString(R.string.healthy_sports_list_calories),
                        getString(R.string.no_data_sign),
                        getString(R.string.unit_calories)
                    )
                )
            }

            if (sportModleInfo!!.dataSources == 0 ||
                (sportModleInfo!!.dataSources == 2 &&
                        (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())
                                ))
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_distance_checked,
                        getString(R.string.healthy_sports_list_distance),
                        getString(R.string.no_data_sign),
                        if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                            R.string.unit_distance_1
                        )
                    )
                )
            }

            if (sportModleInfo!!.dataSources == 0 ||
                (sportModleInfo!!.dataSources == 2 &&
                        (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData2(sportModleInfo!!.exerciseType.toInt())))
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_avg_minkm_checked,
                        getString(R.string.sport_avg_minkm),
                        getString(R.string.no_data_sign),
                        "/${
                            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                R.string.unit_distance_1
                            )
                        }"
                    )
                )
            }

            if (sportModleInfo!!.dataSources == 0 ||
                (sportModleInfo!!.dataSources == 2 &&
                        SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()))
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_speed_checked,
                        getString(R.string.sport_data_type_speed),
                        getString(R.string.no_data_sign),
                        StringBuilder().append(
                            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(
                                R.string.unit_distance_1
                            )
                        )
                            .append("/")
                            .append(getString(R.string.h))
                            .toString()
                    )
                )
            }

            if (sportModleInfo!!.dataSources == 2 && (
                        SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt()))
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_heart_rate_checked,
                        getString(R.string.heart_rate_average_heart_rate),
                        getString(R.string.no_data_sign),
                        /*getString(R.string.unit_heart)*/
                        getString(R.string.hr_unit_bpm)
                        /*StringBuilder()
                            .append(getString(R.string.count))
                            .append("/")
                            .append(getString(R.string.avg_min))
                            .toString()*/
                    )
                )
            }

            if (sportModleInfo!!.dataSources == 2 &&
                (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                        SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()))
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_step_num_checked,
                        getString(R.string.healthy_sports_list_step),
                        getString(R.string.no_data_sign),
                        getString(R.string.unit_steps)
                    )
                )
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_step_rate_checked,
                        getString(R.string.sport_avg_step_rate),
                        getString(R.string.no_data_sign),
                        StringBuilder()
                            .append(getString(R.string.unit_steps))
                            .append("/")
                            .append(getString(R.string.avg_min))
                            .toString()
                    )
                )
            }

            /*if(sportModleInfo!!.dataSources == 2 && SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
            ){
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_height_checked,
                        getString(R.string.sport_height),
                        getString(R.string.no_data_sign),
                        getString(R.string.unit_meter)
                    )
                )
            }*/
            if (sportModleInfo!!.dataSources == 2 && SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())
            ) {
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_stroke_checked,
                        getString(R.string.sport_swimming_stroke),
                        getString(R.string.no_data_sign),
                        ""
                    )
                )
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_stroke_count_checked,
                        getString(R.string.sport_swolf),
                        getString(R.string.no_data_sign),
                        getString(R.string.count)
                    )
                )
                if (sportModleInfo!!.exerciseType.toInt() == 200) {
                    add(
                        SportDetailsDataBean(
                            R.mipmap.sport_pool_length_checked,
                            getString(R.string.pool_length),
                            getString(R.string.no_data_sign),
                            if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_meter) else getString(R.string.unit_ft)
                        )
                    )
                    add(
                        SportDetailsDataBean(
                            R.mipmap.sport_trip_checked,
                            getString(R.string.trip),
                            getString(R.string.no_data_sign), ""
                        )
                    )
                }
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_avg_frequency_checked,
                        getString(R.string.avg_stroke_frequency),
                        getString(R.string.no_data_sign),
                        StringBuilder()
                            .append(getString(R.string.count))
                            .append("/")
                            .append(getString(R.string.avg_min))
                            .toString()
                    )
                )
                add(
                    SportDetailsDataBean(
                        R.mipmap.sport_avg_swolf_checked,
                        getString(R.string.avg_swolf),
                        getString(R.string.no_data_sign), ""
                    )
                )
            }
        }
        //endregion

        // 根据运动类型判断是否显示心率区间
        if (sportModleInfo!!.dataSources == 2 &&
            (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                    SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                    SportParsing.isData3(sportModleInfo!!.exerciseType.toInt()) ||
                    SportParsing.isData4(sportModleInfo!!.exerciseType.toInt()))
        ) {
            binding.heartRRLayout.rootLayout.visibility = View.VISIBLE
        }

        //region 心率区间
        binding.heartRRLayout.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = initHeartAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position: Int = parent.getChildAdapterPosition(view)
                    if (position != 0) {
                        outRect.top = ConvertUtils.dp2px(10F)
                    }
                }
            })
        }
        //endregion

        //region 心率区间
        mSportHeartRangeData.apply {
            add(
                SportHeartRangeBean(
                    R.drawable.sport_heart_range_bg_1,
                    getString(R.string.heart_rate_range_limit),
                    null,
                    0
                )
            )
            add(
                SportHeartRangeBean(
                    R.drawable.sport_heart_range_bg_2,
                    getString(R.string.heart_rate_range_anaerobic),
                    null,
                    0
                )
            )
            add(
                SportHeartRangeBean(
                    R.drawable.sport_heart_range_bg_3,
                    getString(R.string.heart_rate_range_aerobic),
                    null,
                    0
                )
            )
            add(
                SportHeartRangeBean(
                    R.drawable.sport_heart_range_bg_4,
                    getString(R.string.heart_rate_range_fat_burning),
                    null,
                    0
                )
            )
            add(
                SportHeartRangeBean(
                    R.drawable.sport_heart_range_bg_5,
                    getString(R.string.heart_rate_range_warm_up),
                    null,
                    0
                )
            )
        }
        //endregion

    }

    //region 数据列表模块
    private fun initAdapter(): CommonAdapter<SportDetailsDataBean, ItemSportDetailsDataBinding> {
        return object :
            CommonAdapter<SportDetailsDataBean, ItemSportDetailsDataBinding>(mSportDetailsData) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemSportDetailsDataBinding {
                return ItemSportDetailsDataBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(
                v: ItemSportDetailsDataBinding,
                t: SportDetailsDataBean,
                position: Int
            ) {
                AppUtils.tryBlock {
                    v.ivIcon.setImageDrawable(ContextCompat.getDrawable(activity!!, t.imgId))
                }
                v.tvName.text = t.name
                v.tvValue.text = SpannableStringTool.get()
                    .append(t.value)
                    .setFontSize(18f)
                    .setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                    .append(" ")
                    .setFontSize(12f)
                    .append(t.unit)
                    .setFontSize(12f)
                    .setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                    .create()
            }

        }
    }
    //endregion

    //region 心率区间
    private fun initHeartAdapter(): CommonAdapter<SportHeartRangeBean, ItemSportHeartRangeBinding> {
        return object :
            CommonAdapter<SportHeartRangeBean, ItemSportHeartRangeBinding>(mSportHeartRangeData) {
            override fun createBinding(
                parent: ViewGroup?,
                viewType: Int
            ): ItemSportHeartRangeBinding {
                return ItemSportHeartRangeBinding.inflate(layoutInflater, parent, false)
            }

            @SuppressLint("SetTextI18n")
            override fun convert(
                v: ItemSportHeartRangeBinding,
                t: SportHeartRangeBean,
                position: Int
            ) {
                AppUtils.tryBlock {
                    v.v1.background = ContextCompat.getDrawable(activity!!, t.iconBgId)
                }
                v.tvName.text = t.name
                t.timeBuilder?.let {
                    v.tvTime.text = it
                }
                v.tvRange.text = "${t.range}%"
            }

        }
    }
    //endregion

    @SuppressLint("NotifyDataSetChanged")
    override fun initData() {
        super.initData()

        //region 数据列表模块
        for (detail in mSportDetailsData) {
            AppUtils.tryBlock {
                when (detail.name) {
                    getString(R.string.sport_time) -> {
                        detail.value =
                            TimeUtils.millis2String(sportModleInfo!!.sportDuration * 1000L)
                    }
                    getString(R.string.healthy_sports_list_calories) -> {
                        detail.value = "${sportModleInfo!!.burnCalories}"
                    }
                    getString(R.string.healthy_sports_list_distance) -> {
                        var m = 0
                        if (sportModleInfo!!.dataSources == 0) {
                            m = sportModleInfo!!.exerciseApp!!.sportsMileage.toFloat().toInt()
                        } else if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    m = sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat().toInt()
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    m = sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat().toInt()
                                }
                            } else if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    m = sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat().toInt()
                                }
                                detail.value = (m / (if (AppUtils.getDeviceUnit() == 0) 1f else 0.3048f)).toInt().toString()
                                detail.unit = if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_meter) else getString(R.string.unit_ft)
                                return@tryBlock
                            }
                        }
                        detail.value = UnitConversionUtils.bigDecimalFormat(
                            (m / 1000f /
                                    (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                        ).toString()
                    }
                    getString(R.string.sport_avg_minkm) -> {
                        if (sportModleInfo!!.dataSources == 0) {
                            detail.value = DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.avgPace.toInt())
                            /* "${sportModleInfo!!.exerciseApp!!.avgPace.toInt() / 60}'${sportModleInfo!!.exerciseApp!!.avgPace.toInt() % 60}\""*/
                        } else if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    detail.value = DevSportManager.calculateMinkm(
                                        sportModleInfo!!.sportDuration * 1000L,
                                        sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat()
                                    )
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    detail.value = DevSportManager.calculateMinkm(
                                        sportModleInfo!!.sportDuration * 1000L,
                                        sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat()
                                    )
                                }
                            } else {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = DevSportManager.calculateMinkm(
                                        sportModleInfo!!.sportDuration * 1000L,
                                        sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat()
                                    )
                                }
                            }
                        }
                    }
                    getString(R.string.sport_data_type_speed) -> {
                        if (sportModleInfo!!.dataSources == 0) {
                            detail.value = sportModleInfo!!.exerciseApp!!.avgSpeed
                        } else if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    detail.value = UnitConversionUtils.bigDecimalFormat(
                                        (sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat().toInt() / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                                                / (sportModleInfo!!.sportDuration / 60f / 60f)
                                    )
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    detail.value = UnitConversionUtils.bigDecimalFormat(
                                        (sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat().toInt() / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                                                / (sportModleInfo!!.sportDuration / 60f / 60f)
                                    )
                                }
                            } else {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = UnitConversionUtils.bigDecimalFormat(
                                        (sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat()
                                            .toInt() / 1000f / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                                                / (sportModleInfo!!.sportDuration / 60f / 60f)
                                    )
                                }
                            }
                        }
                    }
                    getString(R.string.heart_rate_average_heart_rate) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    detail.value = sportModleInfo!!.exerciseOutdoor!!.reportAvgHeart
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    detail.value = sportModleInfo!!.exerciseIndoor!!.reportAvgHeart
                                }
                            }/*else{
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.reportAvgHeart
                                }
                            }*/
                        }
                    }
                    getString(R.string.healthy_sports_list_step) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    detail.value = sportModleInfo!!.exerciseOutdoor!!.reportTotalStep
                                    if (detail.value == "0") {
                                        detail.unit = getString(R.string.unit_step)
                                    }
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    detail.value = sportModleInfo!!.exerciseIndoor!!.reportTotalStep
                                    if (detail.value == "0") {
                                        detail.unit = getString(R.string.unit_step)
                                    }
                                }
                            }/*else{
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.reportTotalStep
                                }
                            }*/
                        }
                    }
                    getString(R.string.sport_avg_step_rate) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    if (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() == 0) {
                                        detail.value = "0"
                                    } else {
                                        detail.value = (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() /
                                                (sportModleInfo!!.sportDuration / 60f)).toInt().toString()
                                    }
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    if (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() == 0) {
                                        detail.value = "0"
                                    } else {
                                        detail.value = (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() /
                                                (sportModleInfo!!.sportDuration / 60f)).toInt().toString()
                                    }
                                }
                            }/*else{
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.reportTotalStep
                                }
                            }*/
                        }
                    }
                    getString(R.string.sport_height) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseOutdoor != null) {
                                    if (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() == 0) {
                                        detail.value = "0"
                                    } else {
                                        detail.value = (sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat().toInt() * 100.0f /
                                                (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() *
                                                        if (AppUtils.getDeviceUnit() == 0) 1f else 0.393f
                                                        )
                                                ).toInt().toString()
                                    }
                                }
                            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                                SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                            ) {
                                if (sportModleInfo!!.exerciseIndoor != null) {
                                    if (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() == 0) {
                                        detail.value = "0"
                                    } else {
                                        detail.value = (sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat().toInt() * 100.0f /
                                                (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() *
                                                        if (AppUtils.getDeviceUnit() == 0) 1f else 0.393f
                                                        )
                                                ).toInt().toString()
                                    }
                                }
                            }/*else{
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.reportTotalStep
                                }
                            }*/
                        }
                    }
                    getString(R.string.sport_swimming_stroke) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = when (sportModleInfo!!.exerciseSwimming!!.description) {
                                        "0" -> {
                                            getString(R.string.sport_swimming_stroke_0)
                                        }
                                        "1" -> {
                                            getString(R.string.sport_swimming_stroke_1)
                                        }
                                        "2" -> {
                                            getString(R.string.sport_swimming_stroke_2)
                                        }
                                        "3" -> {
                                            getString(R.string.sport_swimming_stroke_3)
                                        }
                                        "4" -> {
                                            getString(R.string.sport_swimming_stroke_4)
                                        }
                                        else -> {
                                            getString(R.string.no_data_sign)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    getString(R.string.sport_swolf) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.numberOfSwims
                                }
                            }
                        }
                    }
                    getString(R.string.pool_length) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.poolWidth
                                }
                            }
                        }
                    }
                    getString(R.string.trip) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.numberOfTurns
                                }
                            }
                        }
                    }
                    getString(R.string.avg_stroke_frequency) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = (
                                            sportModleInfo!!.exerciseSwimming!!.numberOfSwims.toInt() /
                                                    (sportModleInfo!!.sportDuration / 60f)).roundToInt().toString()

                                }
                            }
                        }
                    }
                    getString(R.string.avg_swolf) -> {
                        if (sportModleInfo!!.dataSources == 2) {
                            if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                                if (sportModleInfo!!.exerciseSwimming != null) {
                                    detail.value = sportModleInfo!!.exerciseSwimming!!.averageSwolf
                                }
                            }
                        }
                    }
                }
            }
        }
        binding.dataLayout.rvData.adapter?.notifyDataSetChanged()
        //endregion

        //region 心率区间
        var reportHeartLimitTime: Long = 0  //心率-极限时长
        var reportHeartAnaerobic: Long = 0  //心率-无氧耐力时长
        var reportHeartAerobic: Long = 0    //心率-有氧耐力时长
        var reportHeartFatBurning: Long = 0 //心率-燃脂时长
        var reportHeartWarmUp: Long = 0     //心率-热身时长
        if (sportModleInfo!!.dataSources == 2) {
            AppUtils.tryBlock {
                if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt()) ||
                    SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())
                ) {
                    if (sportModleInfo!!.exerciseOutdoor != null) {
                        reportHeartLimitTime = sportModleInfo!!.exerciseOutdoor!!.reportHeartLimitTime.toLong()
                        reportHeartAnaerobic = sportModleInfo!!.exerciseOutdoor!!.reportHeartAnaerobic.toLong()
                        reportHeartAerobic = sportModleInfo!!.exerciseOutdoor!!.reportHeartAerobic.toLong()
                        reportHeartFatBurning = sportModleInfo!!.exerciseOutdoor!!.reportHeartFatBurning.toLong()
                        reportHeartWarmUp = sportModleInfo!!.exerciseOutdoor!!.reportHeartWarmUp.toLong()
                    }
                } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt()) ||
                    SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())
                ) {
                    if (sportModleInfo!!.exerciseIndoor != null) {
                        reportHeartLimitTime = sportModleInfo!!.exerciseIndoor!!.reportHeartLimitTime.toLong()
                        reportHeartAnaerobic = sportModleInfo!!.exerciseIndoor!!.reportHeartAnaerobic.toLong()
                        reportHeartAerobic = sportModleInfo!!.exerciseIndoor!!.reportHeartAerobic.toLong()
                        reportHeartFatBurning = sportModleInfo!!.exerciseIndoor!!.reportHeartFatBurning.toLong()
                        reportHeartWarmUp = sportModleInfo!!.exerciseIndoor!!.reportHeartWarmUp.toLong()
                    }
                }
            }
        }

        val allHeartRange = reportHeartLimitTime + reportHeartAnaerobic + reportHeartAerobic + reportHeartFatBurning + reportHeartWarmUp

        binding.heartRRLayout.tvAllTime.text = getHeartDateDescribe(allHeartRange)
        var addup = 0
        for (heart in mSportHeartRangeData) {
            when (heart.name) {
                getString(R.string.heart_rate_range_limit) -> {
                    addup += setHeartData(heart, allHeartRange, reportHeartLimitTime)
                }
                getString(R.string.heart_rate_range_anaerobic) -> {
                    addup += setHeartData(heart, allHeartRange, reportHeartAnaerobic)
                }
                getString(R.string.heart_rate_range_aerobic) -> {
                    addup += setHeartData(heart, allHeartRange, reportHeartAerobic)
                }
                getString(R.string.heart_rate_range_fat_burning) -> {
                    addup += setHeartData(heart, allHeartRange, reportHeartFatBurning)
                }
                getString(R.string.heart_rate_range_warm_up) -> {
                    setLastHeartData(heart, addup, reportHeartWarmUp)
                }
            }
        }
        binding.heartRRLayout.progressView.start(
            if (reportHeartLimitTime in 1..4) 5 else reportHeartLimitTime.toInt(), //1..4 补圆角
            if (reportHeartAnaerobic in 1..4) 5 else reportHeartAnaerobic.toInt(),
            if (reportHeartAerobic in 1..4) 5 else reportHeartAerobic.toInt(),
            if (reportHeartFatBurning in 1..4) 5 else reportHeartFatBurning.toInt(),
            if (reportHeartWarmUp in 1..4) 5 else reportHeartWarmUp.toInt(),
            R.color.sport_heart_rate_1,
            R.color.sport_heart_rate_2,
            R.color.sport_heart_rate_3,
            R.color.sport_heart_rate_4,
            R.color.sport_heart_rate_5
        )
        binding.heartRRLayout.recycler.adapter?.notifyDataSetChanged()
        //endregion
    }

    /**
     * 心率区间赋值
     * */
    private fun setHeartData(h: SportHeartRangeBean, all: Long, value: Long): Int {
        AppUtils.tryBlock {
            h.timeBuilder = getHeartDateDescribe(value)
            if (all != 0L) {
                /*var v = (value * 1f / all * 100)
                if (v > 0f && v < 1f) {
                    v = 1f
                }
                h.range = v.roundToInt()*/
                var v = (value * 100 / all).toInt()
                if (v == 0 && value > 0) {
                    v = 1
                }
                h.range = v
            } else {
                h.range = 0
            }

        }
        return h.range
    }

    private fun setLastHeartData(h: SportHeartRangeBean, addUp: Int, value: Long) {
        AppUtils.tryBlock {
            h.timeBuilder = getHeartDateDescribe(value)
            if (value != 0L) {
                h.range = 100 - addUp
            } else {
                h.range = 0
            }
        }
    }

    /**
     * 计算心率区间时间描述
     * */
    private fun getHeartDateDescribe(allHeartRange: Long): SpannableStringBuilder {
        var allH = "00"
        var allM = "00"
        var allS = "00"
        var isHour = false
        var isMinute = false
        if (allHeartRange > 0) {
            when {
                allHeartRange >= 60 * 60 -> {
                    isHour = true
                    val h = (allHeartRange / 60 / 60)
                    allH = if (h >= 10) h.toString() else "0$h"

                    val m = ((allHeartRange / 60) % 60)
                    allM = if (m >= 10) m.toString() else "0$m"

                    val s = (allHeartRange % 60)
                    allS = if (s >= 10) s.toString() else "0$s"
                }
                allHeartRange >= 60 -> {
                    isMinute = true
                    val m = (allHeartRange / 60)
                    allM = if (m >= 10) m.toString() else "0$m"

                    val s = (allHeartRange % 60)
                    allS = if (s >= 10) s.toString() else "0$s"
                }
                else -> {
                    val s = /*allHeartRange*/ allHeartRange % 60
                    allS = if (s >= 10) s.toString() else "0$s"
                }
            }
        }

        when {
            isHour -> { //XX时XX分
                return SpannableStringTool.get()
                    .append(allH).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.hours_text)).setFontSize(11f)
                    .setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(allM).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.minutes_text)).setFontSize(11f)
                    .append(" ").setFontSize(10f)
                    .append(allS).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.unit_secs)).setFontSize(11f)
                    .setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .create()
            }
            isMinute -> { //XX分XX秒
                return SpannableStringTool.get()
                    .append(allM).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.minutes_text)).setFontSize(11f)
                    .setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(allS).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.unit_secs)).setFontSize(11f)
                    .setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .create()
            }
            else -> { //XX秒
                return SpannableStringTool.get()
                    .append(allS).setFontSize(16f).setForegroundColor(ContextCompat.getColor(BaseApplication.mContext, R.color.color_171717))
                    .append(" ").setFontSize(10f)
                    .append(getString(R.string.unit_secs)).setFontSize(11f)
                    .setForegroundColor(Color.WHITE)
                    .create()
            }
        }
    }

    //生产分享bitmap
    fun createDataBitMap(): Bitmap {
        return ImageUtils.view2Bitmap(binding.data)
    }
}