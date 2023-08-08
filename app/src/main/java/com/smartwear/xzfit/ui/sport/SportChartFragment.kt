package com.smartwear.xzfit.ui.sport

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ImageUtils
import com.zhapp.ble.bean.DevSportInfoBean
import com.zhapp.ble.parsing.SportParsing
import com.zhapp.ble.utils.UnitConversionUtils
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseFragment
import com.smartwear.xzfit.databinding.FragmentSportChartBinding
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.utils.AppUtils
import com.smartwear.xzfit.utils.SpannableStringTool
import com.smartwear.xzfit.utils.manager.DevSportManager
import com.smartwear.xzfit.viewmodel.SportModel
import kotlin.math.roundToInt

/**
 * Created by Android on 2021/10/14.
 * 运动数据图表
 */
class SportChartFragment : BaseFragment<FragmentSportChartBinding, SportModel>(
    FragmentSportChartBinding::inflate, SportModel::class.java
) {

    private var sportModleInfo: SportModleInfo? = null

    private var isNoData = false

    override fun initView() {
        super.initView()

        binding.minkmLayout.mPaceCurveChartView.setType(1)
    }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun initData() {
        super.initData()

        sportModleInfo = viewmodel.sportLiveData.getSportModleInfo().value
        if (sportModleInfo == null) {
            binding.noData.layoutNoData.visibility = View.VISIBLE
            isNoData = true
            return
        }
        //LogUtils.e("sportModleInfo----------->")
        //LogUtils.json(sportModleInfo)
        //region APP运动
        if (sportModleInfo!!.dataSources == 0) {
            AppUtils.tryBlock {
                binding.noData.layoutNoData.visibility = View.GONE
                val xdata = arrayListOf<Double>()
                val yPacedata = arrayListOf<Double>()
                val ySpeeddata = arrayListOf<Double>()
                binding.minkmLayout.root.visibility = View.VISIBLE
                binding.minkmLayout.mPaceCurveChartView.setType(1)
                binding.minkmLayout.tvAvgMinKm.text = DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.avgPace.toInt())
                binding.minkmLayout.tvMinMinKm.text = DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.minPace.toInt())
                binding.minkmLayout.tvMaxMinKm.text = DevSportManager.calculateMinkm(sportModleInfo!!.exerciseApp!!.maxPace.toInt())
                //配速数据
                val paceData = sportModleInfo!!.exerciseApp!!.paceDatas.split(",")
                val xTIme = sportModleInfo!!.sportDuration / 60.0 / paceData.size
                for (i in paceData.indices) {
                    xdata.add(xTIme * i)

                    yPacedata.add(paceData.get(i).toDouble() * (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                    //* if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f
                }
                binding.minkmLayout.mPaceCurveChartView.setParameter(xdata, yPacedata)

                //速度数据
                binding.speedLayout.root.visibility = View.VISIBLE
                binding.speedLayout.mSpeedCurveChartView.setType(2)
                binding.speedLayout.tvAvgSpeed.text = SpannableStringTool.get()
                    .append(sportModleInfo!!.exerciseApp!!.avgSpeed)
                    .setFontSize(24f)
                    .setForegroundColor(Color.WHITE)
                    .append(" ")
                    .append(if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(R.string.unit_distance_1))
                    .setFontSize(12f)
                    .append("/")
                    .setFontSize(12f)
                    .append(getString(R.string.h))
                    .setFontSize(12f)
                    .setForegroundColor(Color.WHITE)
                    .create()

                binding.speedLayout.tvMaxSpeed.text = SpannableStringTool.get()
                    .append(sportModleInfo!!.exerciseApp!!.maxSpeed)
                    .setFontSize(24f)
                    .setForegroundColor(Color.WHITE)
                    .append(" ")
                    .append(if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_distance_0) else getString(R.string.unit_distance_1))
                    .setFontSize(12f)
                    .append("/").setFontSize(12f)
                    .append(getString(R.string.h))
                    .setFontSize(12f)
                    .setForegroundColor(Color.WHITE)
                    .create()
                xdata.clear()
                val speedData = sportModleInfo!!.exerciseApp!!.speedDatas.split(",")
                val xTIme2 = sportModleInfo!!.sportDuration / 60.0 / speedData.size
                for (i in speedData.indices) {
                    xdata.add(xTIme2 * i)
                    ySpeeddata.add(speedData.get(i).toDouble() / (if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                }
                binding.speedLayout.mSpeedCurveChartView.setParameter(xdata, ySpeeddata)
            }
        }
        //endregion
        //region 设备运动
        if (sportModleInfo!!.dataSources == 2) {
            var sportParsing = arrayListOf<DevSportInfoBean.RecordPointSportData>()
            if (SportParsing.isData1(sportModleInfo!!.exerciseType.toInt())) {
                //配速+步频
                if (sportModleInfo!!.exerciseOutdoor == null || sportModleInfo!!.exerciseOutdoor!!.recordPointSportData.isEmpty()) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                sportParsing = DevSportManager.parsingPointData(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseOutdoor!!.recordPointVersion,
                    sportModleInfo!!.exerciseOutdoor!!.recordPointSportData
                )
                //0配速  1速度  2步频  3心率  4卡路里
                showChart(sportParsing, 0, 2/*0,1,2,3,4*/)
                AppUtils.tryBlock {
                    binding.minkmLayout.tvAvgMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat()
                    )
                    binding.minkmLayout.tvMinMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseOutdoor!!.reportSlowestPace.toInt()
                    )
                    binding.minkmLayout.tvMaxMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseOutdoor!!.reportFastPace.toInt()
                    )
                }
                AppUtils.tryBlock {
                    var avgStep = 0
                    if (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() != 0) {
                        avgStep = (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() / (sportModleInfo!!.sportDuration / 60f)).toInt()
                    }
                    var stride = 0
                    if (sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() != 0) {
                        stride = (sportModleInfo!!.exerciseOutdoor!!.reportDistance.toFloat().toInt() * 100.0f /
                                sportModleInfo!!.exerciseOutdoor!!.reportTotalStep.toInt() *
                                if (AppUtils.getDeviceUnit() == 0) 1f else 0.393f).toInt()
                    }
                    binding.stepLayout.tvAvgStep.text = SpannableStringTool.get()
                        .append("$avgStep")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_steps))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.stepLayout.tvStride.text = SpannableStringTool.get()
                        .append("$stride")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_height_0) else getString(R.string.unit_height_1))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.stepLayout.tvMaxStep.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseOutdoor!!.reportMaxStepSpeed)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_steps))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
            } else if (SportParsing.isData3(sportModleInfo!!.exerciseType.toInt())) {
                //配速+速度
                if (sportModleInfo!!.exerciseOutdoor == null || sportModleInfo!!.exerciseOutdoor!!.recordPointSportData.isEmpty()) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                sportParsing = DevSportManager.parsingPointData(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseOutdoor!!.recordPointVersion,
                    sportModleInfo!!.exerciseOutdoor!!.recordPointSportData
                )
                //0配速  1速度  2步频  3心率  4卡路里
                showChart(sportParsing, 3, 4)
                AppUtils.tryBlock {
                    binding.heartLayout.tvAvgHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseOutdoor!!.reportAvgHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.heartLayout.tvMinHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseOutdoor!!.reportMinHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.heartLayout.tvMaxHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseOutdoor!!.reportMaxHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
                AppUtils.tryBlock {
                    val avgCalories = (sportModleInfo!!.burnCalories / (sportModleInfo!!.sportDuration / 60f)).roundToInt()
                    binding.calLayout.tvAvgCalories.text = SpannableStringTool.get()
                        .append("$avgCalories")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_calories))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.calLayout.tvCalories.text = SpannableStringTool.get()
                        .append("${sportModleInfo!!.burnCalories}")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.unit_calories))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
            } else if (SportParsing.isData2(sportModleInfo!!.exerciseType.toInt())) {
                //配速+步频
                if (sportModleInfo!!.exerciseIndoor == null || sportModleInfo!!.exerciseIndoor!!.recordPointSportData.isEmpty()) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                sportParsing = DevSportManager.parsingPointData(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseIndoor!!.recordPointVersion,
                    sportModleInfo!!.exerciseIndoor!!.recordPointSportData
                )
                //0配速  1速度  2步频  3心率  4卡路里
                showChart(sportParsing, 0, 2)
                AppUtils.tryBlock {
                    binding.minkmLayout.tvAvgMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat()
                    )
                    binding.minkmLayout.tvMinMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseIndoor!!.reportSlowestPace.toInt()
                    )
                    binding.minkmLayout.tvMaxMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseIndoor!!.reportFastPace.toInt()
                    )
                }
                AppUtils.tryBlock {
                    var avgStep = 0
                    if (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() != 0) {
                        avgStep = (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() / (sportModleInfo!!.sportDuration / 60f)).toInt()
                    }
                    var stride = 0
                    if (sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() != 0) {
                        stride = (sportModleInfo!!.exerciseIndoor!!.reportDistance.toFloat().toInt() * 100.0f /
                                sportModleInfo!!.exerciseIndoor!!.reportTotalStep.toInt() *
                                if (AppUtils.getDeviceUnit() == 0) 1f else 0.393f).toInt()
                    }
                    binding.stepLayout.tvAvgStep.text = SpannableStringTool.get()
                        .append("$avgStep")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_steps))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.stepLayout.tvStride.text = SpannableStringTool.get()
                        .append("$stride")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(if (AppUtils.getDeviceUnit() == 0) getString(R.string.unit_height_0) else getString(R.string.unit_height_1))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.stepLayout.tvMaxStep.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseIndoor!!.reportMaxStepSpeed)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_steps))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
            } else if (SportParsing.isData4(sportModleInfo!!.exerciseType.toInt())) {
                //心率＋卡路里
                if (sportModleInfo!!.exerciseIndoor == null || sportModleInfo!!.exerciseIndoor!!.recordPointSportData.isEmpty()) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                sportParsing = DevSportManager.parsingPointData(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseIndoor!!.recordPointVersion,
                    sportModleInfo!!.exerciseIndoor!!.recordPointSportData
                )
                //0配速  1速度  2步频  3心率  4卡路里
                showChart(sportParsing, 3, 4)
                AppUtils.tryBlock {
                    binding.heartLayout.tvAvgHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseIndoor!!.reportAvgHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.heartLayout.tvMinHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseIndoor!!.reportMinHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.heartLayout.tvMaxHeart.text = SpannableStringTool.get()
                        .append(sportModleInfo!!.exerciseIndoor!!.reportMaxHeart)
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.hr_unit_bpm))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
                AppUtils.tryBlock {
                    val avgCalories = (sportModleInfo!!.burnCalories / (sportModleInfo!!.sportDuration / 60f)).roundToInt()
                    binding.calLayout.tvAvgCalories.text = SpannableStringTool.get()
                        .append("$avgCalories")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(getString(R.string.unit_calories))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.calLayout.tvCalories.text = SpannableStringTool.get()
                        .append("${sportModleInfo!!.burnCalories}")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.unit_calories))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
            } else if (SportParsing.isData5(sportModleInfo!!.exerciseType.toInt())) {
                //划水频率＋SWOLF
                if (sportModleInfo!!.exerciseSwimming == null) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                var deviceSportList: ArrayList<DevSportInfoBean.DeviceSportSwimEntity>? = DevSportManager.parsingFitnessNew(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseSwimming!!.recordPointVersion,
                    sportModleInfo!!.exerciseSwimming!!.recordPointSportData
                )
                showSwimChart(deviceSportList)
                AppUtils.tryBlock {
                    binding.swimsLayout.tvAvg.text = SpannableStringTool.get()
                        .append(
                            (sportModleInfo!!.exerciseSwimming!!.numberOfSwims.toInt() /
                                    (sportModleInfo!!.sportDuration / 60f)).roundToInt().toString()
                        )
                        .setFontSize(30f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append("${getString(R.string.count)}/${getString(R.string.avg_min)}")
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.swimsLayout.tvBest.text = SpannableStringTool.get()
                        .append(
                            sportModleInfo!!.exerciseSwimming!!.maximumStrokeFrequency
                        )
                        .setFontSize(30f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append("${getString(R.string.count)}/${getString(R.string.avg_min)}")
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()

                }
                AppUtils.tryBlock {
                    binding.swolfLayout.tvAvg.text = sportModleInfo!!.exerciseSwimming!!.averageSwolf
                    binding.swolfLayout.tvBest.text = sportModleInfo!!.exerciseSwimming!!.bestSwolf
                }
            } else {
                //配速＋卡路里
                if (sportModleInfo!!.exerciseSwimming == null || sportModleInfo!!.exerciseSwimming!!.recordPointSportData.isEmpty()) {
                    isNoData = true
                    return
                }
                binding.noData.layoutNoData.visibility = View.GONE
                sportParsing = DevSportManager.parsingPointData(
                    sportModleInfo!!.exerciseType,
                    sportModleInfo!!.exerciseSwimming!!.recordPointVersion,
                    sportModleInfo!!.exerciseSwimming!!.recordPointSportData
                )
                //0配速  1速度  2步频  3心率  4卡路里
                showChart(sportParsing, 0, 4)
                AppUtils.tryBlock {
                    binding.minkmLayout.tvAvgMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.sportDuration * 1000L,
                        sportModleInfo!!.exerciseSwimming!!.reportDistance.toFloat()
                    )
                    binding.minkmLayout.tvMinMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseSwimming!!.reportSlowestPace.toInt()
                    )
                    binding.minkmLayout.tvMaxMinKm.text = DevSportManager.calculateMinkm(
                        sportModleInfo!!.exerciseSwimming!!.reportFastPace.toInt()
                    )
                }
                AppUtils.tryBlock {
                    val avgCalories = UnitConversionUtils.bigDecimalFormat(sportModleInfo!!.burnCalories / (sportModleInfo!!.sportDuration / 60f))
                    binding.calLayout.tvAvgCalories.text = SpannableStringTool.get()
                        .append("$avgCalories")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(
                            StringBuilder()
                                .append(" ")
                                .append(getString(R.string.unit_calories))
                                .append("/")
                                .append(getString(R.string.avg_min))
                                .toString()
                        )
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                    binding.calLayout.tvCalories.text = SpannableStringTool.get()
                        .append("${sportModleInfo!!.burnCalories}")
                        .setFontSize(24f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_171717))
                        .append(" ")
                        .append(getString(R.string.unit_calories))
                        .setFontSize(14f).setForegroundColor(ContextCompat.getColor(activity!!, R.color.color_878787))
                        .create()
                }
            }
        }
        //endregion
    }

    /**
     * 显示图表
     * @param data 设备打点数据
     * @param type 0配速  1速度  2步频  3心率  4卡路里
     * */
    fun showChart(data: ArrayList<DevSportInfoBean.RecordPointSportData>?, vararg type: Int) {
        //LogUtils.e("设备运动-图表数据")
        //LogUtils.json(data)
        for (t in type) {
            when (t) {
                0 -> {
                    binding.minkmLayout.root.visibility = View.VISIBLE
                }
                1 -> {
                    binding.speedLayout.root.visibility = View.VISIBLE
                }
                2 -> {
                    binding.stepLayout.root.visibility = View.VISIBLE
                }
                3 -> {
                    binding.heartLayout.root.visibility = View.VISIBLE
                }
                4 -> {
                    binding.calLayout.root.visibility = View.VISIBLE
                }
            }
        }
        fillChart(data)
    }

    /**
     * 填充图表数据
     * */
    fun fillChart(deviceSportList: ArrayList<DevSportInfoBean.RecordPointSportData>?) {
        //region 空数据
        if (deviceSportList.isNullOrEmpty()) {
            var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 16
            val xData = java.util.ArrayList<Double>()
            val yHeartData = java.util.ArrayList<Double>()
            val yPaceData = java.util.ArrayList<Double>()
            val yStepSpeedData = java.util.ArrayList<Double>()
            val ySpeedData = java.util.ArrayList<Double>()
            val yCalData = java.util.ArrayList<Double>()
            val yHeightData = java.util.ArrayList<Double>()
            for (i in 0 until 16) {
                xData.add((xTime * (i + 1)).toDouble())
                yHeartData.add(0.0)
                yPaceData.add(0.0)
                yStepSpeedData.add(0.0)
                ySpeedData.add(0.0)
                yCalData.add(0.0)
            }
            binding.heartLayout.mHeartCurveChartView.setParameter(xData, yHeartData)
            binding.minkmLayout.mPaceCurveChartView.setParameter(xData, yPaceData)
            binding.stepLayout.mStepSpeedCurveChartView.setParameter(xData, yStepSpeedData)
            binding.speedLayout.mSpeedCurveChartView.setParameter(xData, ySpeedData)
            binding.calLayout.mCalCurveChartView.setParameter(xData, yCalData)
            return
        }
        //endregion
        AppUtils.tryBlock {
            /*
             * 步频表值：RecordPointSportData.step * 6 = 每10s一个点的步数* 6 ; 单位：steps/min
             * 配速表值：10 / (RecordPointSportDatadistance / 1000 / 1f|1.61f) = 10s/每10s一个点的距离转公英里 = 一公里|英里所需秒；单位：分数‘秒数“
             * 速度表值：(RecordPointSportData.distance * 360.0 / 1000/ 1f|1.61f) = 每10s一个点的距离 * 360.0 /米转公英制；单位：km/h | mi/h
             * 心率表值：RecordPointSportData.heart ；单位：bmp
             * 卡路里表值：RecordPointSportData.cal ；单位：kcal
             * 高度表值：RecordPointSportData.height ；单位：cm | in
             * 划水频率表值 SportItem.swimItemExtra.strokeFrequency * 6; 单位 ： 次/分钟
             * 划水频率表值 SportItem.swimItemExtra.swolf ;
             */
            if (deviceSportList.size > 16) {
                val oneGroup = deviceSportList.size / 16
                val lastData = deviceSportList.size % 16
                if (lastData * 1f / oneGroup < 0.1) {
                    //region 16段数据
                    var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 16
                    val xData = java.util.ArrayList<Double>()
                    for (i in 0 until 16) {
                        xData.add((xTime * (i + 1)).toDouble())
                    }

                    val yHeartData = java.util.ArrayList<Double>()
                    val yPaceData = java.util.ArrayList<Double>()
                    val yStepSpeedData = java.util.ArrayList<Double>()
                    val ySpeedData = java.util.ArrayList<Double>()
                    val yCalData = java.util.ArrayList<Double>()
                    val yHeightData = java.util.ArrayList<Double>()
                    val yStrokeData = java.util.ArrayList<Double>()
                    val ySwolfData = java.util.ArrayList<Double>()

                    for (i in 0..15) {
                        var heart: Double = 0.0
                        var distance: Double = 0.0
                        var height: Double = 0.0
                        var step: Int = 0
                        var cal: Double = 0.0
                        var stroke: Int = 0
                        var swolf: Double = 0.0

                        var heartIndex: Int = 0

                        for (j in i * oneGroup until (i + 1) * oneGroup) {
                            var deviceSportEntity = deviceSportList[j];
                            heart += deviceSportEntity.heart
                            distance += deviceSportEntity.distance
                            height += deviceSportEntity.height
                            step += deviceSportEntity.step
                            cal += deviceSportEntity.cal

                            if (deviceSportEntity.heart != 0) {
                                heartIndex++
                            }
                        }

                        if (heartIndex == 0) {
                            heartIndex = 1
                        }

                        yHeartData.add(heart / heartIndex)
                        if (distance == 0.0) {
                            yPaceData.add(0.0)
                        } else {
                            val psce = 10 / (distance / oneGroup / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
                            if (DevSportManager.isShow00Pace(psce.toInt())) {
                                yPaceData.add(0.0)
                            } else {
                                yPaceData.add(psce.toInt().toDouble())
                            }
                        }
                        if (distance == 0.0) {
                            ySpeedData.add(0.0)
                        } else {
                            ySpeedData.add((distance * 360.0 / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f) / lastData)
                        }
                        yHeightData.add(height * if (AppUtils.getDeviceUnit() == 0) 1.0 else 0.39 / lastData)
                        yStepSpeedData.add(step * 6.0 / oneGroup)
                        yCalData.add(cal)
                    }
                    binding.heartLayout.mHeartCurveChartView.setParameter(xData, yHeartData)
                    binding.minkmLayout.mPaceCurveChartView.setParameter(xData, yPaceData)
                    binding.stepLayout.mStepSpeedCurveChartView.setParameter(xData, yStepSpeedData)
                    binding.speedLayout.mSpeedCurveChartView.setParameter(xData, ySpeedData)
                    binding.calLayout.mCalCurveChartView.setParameter(xData, yCalData)
                    //endregion
                } else {
                    //region 共17 段数据
                    var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 17
                    val xData = java.util.ArrayList<Double>()
                    for (i in 0 until 16) {
                        xData.add((xTime * (i + 1)).toDouble())
                    }
                    xData.add(lastData * 1.0 / 60 + xTime * 16)

                    val yHeartData = java.util.ArrayList<Double>()
                    val yPaceData = java.util.ArrayList<Double>()
                    val yStepSpeedData = java.util.ArrayList<Double>()
                    val ySpeedData = java.util.ArrayList<Double>()
                    val yCalData = java.util.ArrayList<Double>()
                    val yHeightData = java.util.ArrayList<Double>()

                    for (i in 0..15) {
                        var heart: Double = 0.0
                        var distance: Double = 0.0
                        var height: Double = 0.0
                        var step: Int = 0
                        var cal: Double = 0.0

                        var heartIndex: Int = 0

                        for (j in i * oneGroup until (i + 1) * oneGroup) {
                            var deviceSportEntity = deviceSportList[j];
                            heart += deviceSportEntity.heart
                            distance += deviceSportEntity.distance
                            height += deviceSportEntity.height
                            step += deviceSportEntity.step
                            cal += deviceSportEntity.cal

                            if (deviceSportEntity.heart != 0) {
                                heartIndex++
                            }
                        }

                        if (heartIndex == 0) {
                            heartIndex = 1
                        }

                        yHeartData.add(heart / heartIndex)
                        if (distance == 0.0) {
                            yPaceData.add(0.0)
                        } else {
                            val psce = 10 / (distance / oneGroup / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
                            if (DevSportManager.isShow00Pace(psce.toInt())) {
                                yPaceData.add(0.0)
                            } else {
                                yPaceData.add(psce.toInt().toDouble())
                            }
                        }
                        if (distance == 0.0) {
                            ySpeedData.add(0.0)
                        } else {
                            ySpeedData.add((distance * 360.0 / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f) / lastData)
                        }
                        yHeightData.add(height * if (AppUtils.getDeviceUnit() == 0) 1.0 else 0.39 / lastData)
                        yStepSpeedData.add(step * 6.0 / oneGroup)
                        yCalData.add(cal)
                    }

                    // 第十七段数据
                    var heart: Double = 0.0
                    var distance: Double = 0.0
                    var height: Double = 0.0
                    var step: Int = 0
                    var cal: Double = 0.0

                    var heartIndex: Int = 0

                    for (j in 16 * oneGroup until 16 * oneGroup + lastData) {
                        var deviceSportEntity = deviceSportList[j];
                        heart += deviceSportEntity.heart
                        distance += deviceSportEntity.distance
                        height += deviceSportEntity.height
                        step += deviceSportEntity.step
                        cal += deviceSportEntity.cal

                        if (deviceSportEntity.heart != 0) {
                            heartIndex++
                        }
                    }
                    if (heartIndex == 0) {
                        heartIndex = 1
                    }
                    yHeartData.add(heart / heartIndex)
                    //配速
                    if (distance == 0.0) {
                        yPaceData.add(0.0)
                    } else {
                        val psce = 10 / (distance / lastData / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
                        if (DevSportManager.isShow00Pace(psce.toInt())) {
                            yPaceData.add(0.0)
                        } else {
                            yPaceData.add(psce.toInt().toDouble())
                        }
                    }
                    if (distance == 0.0) {
                        ySpeedData.add(0.0)
                    } else {
                        ySpeedData.add((distance * 360.0 / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f) / lastData)
                    }
                    yHeightData.add(height * if (AppUtils.getDeviceUnit() == 0) 1.0 else 0.39 / lastData)
                    yStepSpeedData.add(step * 6.0 / lastData)
                    yCalData.add(cal)
                    binding.heartLayout.mHeartCurveChartView.setParameter(xData, yHeartData)
                    binding.minkmLayout.mPaceCurveChartView.setParameter(xData, yPaceData)
                    binding.stepLayout.mStepSpeedCurveChartView.setParameter(xData, yStepSpeedData)
                    binding.speedLayout.mSpeedCurveChartView.setParameter(xData, ySpeedData)
                    binding.calLayout.mCalCurveChartView.setParameter(xData, yCalData)
                    //binding.heightLayout.mHeightCurveChartView.setParameter(xData, yHeightData)
                    //endregion
                }
            } else {
                //region 不足16 （设备运动最短时间为1分钟，最少有6个点）
                val xData = java.util.ArrayList<Double>()
                /*val yData = java.util.ArrayList<Double>()*/
                val yHeartData = java.util.ArrayList<Double>()
                val yPaceData = java.util.ArrayList<Double>()
                val yStepSpeedData = java.util.ArrayList<Double>()
                val ySpeedData = java.util.ArrayList<Double>()
                val yCalData = java.util.ArrayList<Double>()
                val yHeightData = java.util.ArrayList<Double>()
                //16个点每个点所消耗的时间
                val xTime = sportModleInfo!!.sportDuration / 60.0 / 16

                for (i in 0 until 16) {
                    xData.add((xTime * (i + 1)))
                }
                /*扩容数据*/
                //还差多少达到16个点
                val targetDif = 16 - deviceSportList.size
                //前targetDif个数据从1开始补相同点
                capacityExpansion(deviceSportList, targetDif)
                //LogUtils.d("补点后数据：---->")
                //LogUtils.json(deviceSportList)
                /*前面画上*/
                for (i in 0 until deviceSportList.size) {
                    var heart: Double = 0.0
                    var distance: Double = 0.0
                    var height: Double = 0.0
                    var step: Int = 0
                    var cal: Double = 0.0
                    var deviceSportEntity = deviceSportList[i]
                    heart += deviceSportEntity.heart
                    distance += deviceSportEntity.distance
                    height += deviceSportEntity.height
                    step += deviceSportEntity.step
                    cal += deviceSportEntity.cal
                    //心率
                    yHeartData.add(heart)
                    //配速
                    if (distance == 0.0) {
                        yPaceData.add(0.0)
                    } else {
                        val psce = 10 / (distance / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f)
                        if (DevSportManager.isShow00Pace(psce.toInt())) {
                            yPaceData.add(0.0)
                        } else {
                            yPaceData.add(psce.toInt().toDouble())
                        }
                    }
                    //速度
                    if (distance == 0.0) {
                        ySpeedData.add(0.0)
                    } else {
                        ySpeedData.add((distance * 360.0 / 1000.0 / if (AppUtils.getDeviceUnit() == 0) 1f else 1.61f))
                    }
                    //高度
                    yHeightData.add(height * if (AppUtils.getDeviceUnit() == 0) 1.0 else 0.39)
                    //步频
                    yStepSpeedData.add(step * 6.0)
                    //卡路里
                    yCalData.add(cal)
                }
                binding.heartLayout.mHeartCurveChartView.setParameter(xData, yHeartData)
                binding.minkmLayout.mPaceCurveChartView.setParameter(xData, yPaceData)
                binding.stepLayout.mStepSpeedCurveChartView.setParameter(xData, yStepSpeedData)
                binding.speedLayout.mSpeedCurveChartView.setParameter(xData, ySpeedData)
                binding.calLayout.mCalCurveChartView.setParameter(xData, yCalData)
                //binding.heightLayout.mHeightCurveChartView.setParameter(xData, yHeightData)
                //endregion
            }
        }
    }


    /**
     * 显示图表
     * @param data 设备打点数据
     * @param
     * */
    fun showSwimChart(data: ArrayList<DevSportInfoBean.DeviceSportSwimEntity>?) {
        //LogUtils.e("设备运动-图表数据")
        //LogUtils.json(data)
        binding.swimsLayout.root.visibility = View.VISIBLE
        binding.swolfLayout.root.visibility = View.VISIBLE
        //region 空数据
        if (data.isNullOrEmpty()) {
            var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 16
            val xData = java.util.ArrayList<Double>()
            val yStrokeData = java.util.ArrayList<Double>()
            val ySwolfData = java.util.ArrayList<Double>()
            for (i in 0 until 16) {
                xData.add((xTime * (i + 1)).toDouble())
                yStrokeData.add(0.0)
                ySwolfData.add(0.0)
            }
            binding.swimsLayout.mSwimsChartView.setParameter(xData, yStrokeData)
            binding.swolfLayout.mSwolfChartView.setParameter(xData, ySwolfData)
            return
        }
        //endregion
        AppUtils.tryBlock {
            /*
             * 划水频率表值 SportItem.swimItemExtra.strokeFrequency * 6; 单位 ： 次/分钟
             * 划水频率表值 SportItem.swimItemExtra.swolf ;
             */
            if (data.size > 16) {
                val oneGroup = data.size / 16
                val lastData = data.size % 16
                if (lastData * 1f / oneGroup < 0.1) {
                    //region 16段数据
                    var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 16
                    val xData = java.util.ArrayList<Double>()
                    for (i in 0 until 16) {
                        xData.add((xTime * (i + 1)).toDouble())
                    }

                    val yStrokeData = java.util.ArrayList<Double>()
                    val ySwolfData = java.util.ArrayList<Double>()

                    for (i in 0..15) {
                        var stroke: Int = 0
                        var swolf: Double = 0.0
                        for (j in i * oneGroup until (i + 1) * oneGroup) {
                            stroke += data.get(j).swipe
                            swolf += data.get(j).swolf
                        }
                        yStrokeData.add(stroke * 1.0 / oneGroup)
                        ySwolfData.add(swolf)
                    }
                    binding.swimsLayout.mSwimsChartView.setParameter(xData, yStrokeData)
                    binding.swolfLayout.mSwolfChartView.setParameter(xData, ySwolfData)
                    //endregion
                } else {
                    //region 共17 段数据
                    var xTime = /*oneGroup * 1f / 60*/ sportModleInfo!!.sportDuration / 60.0 / 17
                    val xData = java.util.ArrayList<Double>()
                    for (i in 0 until 16) {
                        xData.add((xTime * (i + 1)).toDouble())
                    }
                    xData.add(lastData * 1.0 / 60 + xTime * 16)

                    val yStrokeData = java.util.ArrayList<Double>()
                    val ySwolfData = java.util.ArrayList<Double>()

                    for (i in 0..15) {
                        var stroke: Int = 0
                        var swolf: Double = 0.0
                        for (j in i * oneGroup until (i + 1) * oneGroup) {
                            stroke += data.get(j).swipe
                            swolf += data.get(j).swolf
                        }
                        yStrokeData.add(stroke * 1.0 / oneGroup)
                        ySwolfData.add(swolf)
                    }

                    // 第十七段数据
                    var stroke: Int = 0
                    var swolf: Double = 0.0

                    for (j in 16 * oneGroup until 16 * oneGroup + lastData) {
                        stroke += data.get(j).swipe
                        swolf += data.get(j).swolf
                    }
                    yStrokeData.add(stroke * 1.0 / oneGroup)
                    ySwolfData.add(swolf)

                    binding.swimsLayout.mSwimsChartView.setParameter(xData, yStrokeData)
                    binding.swolfLayout.mSwolfChartView.setParameter(xData, ySwolfData)
                    //endregion
                }
            } else {
                //region 不足16 （设备运动最短时间为1分钟，最少有6个点）
                val xData = java.util.ArrayList<Double>()
                val yStrokeData = java.util.ArrayList<Double>()
                val ySwolfData = java.util.ArrayList<Double>()
                //16个点每个点所消耗的时间
                val xTime = sportModleInfo!!.sportDuration / 60.0 / 16

                for (i in 0 until 16) {
                    xData.add((xTime * (i + 1)))
                }
                /*扩容数据*/
                //还差多少达到16个点
                val targetDif = 16 - data.size
                //前targetDif个数据从1开始补相同点
                capacityExpansion(data, targetDif, true)
                /*前面画上*/
                for (i in 0 until data.size) {
                    var stroke: Int = 0
                    var swolf: Double = 0.0
                    stroke += data.get(i).swipe
                    swolf += data.get(i).swolf
                    yStrokeData.add(stroke * 1.0)
                    ySwolfData.add(swolf)
                }
                binding.swimsLayout.mSwimsChartView.setParameter(xData, yStrokeData)
                binding.swolfLayout.mSwolfChartView.setParameter(xData, ySwolfData)
                //endregion
            }
        }
    }

    /**
     * 补点操作
     * */
    fun capacityExpansion(list: ArrayList<DevSportInfoBean.RecordPointSportData>, expansionNum: Int) {
        for (i in 0 until expansionNum) {
            if (list.size < 16) {
                when {
                    i * 2 + 1 < list.size -> {
                        list.add(i * 2 + 1, list.get(i * 2 + 1))
                    }
                    i * 2 + 1 == list.size -> {
                        list.add(list.get(list.size - 1))
                    }
                    else -> {
                        list.add(list.size - 1, list.get(list.size - 1))
                        //反向补点
                        list.reverse()
                        capacityExpansion(list, 16 - list.size)
                        list.reverse()
                    }
                }
            }
        }
    }

    /**
     * 补点操作
     * */
    fun capacityExpansion(list: ArrayList<DevSportInfoBean.DeviceSportSwimEntity>, expansionNum: Int, need: Boolean) {
        for (i in 0 until expansionNum) {
            if (list.size < 16) {
                when {
                    i * 2 + 1 < list.size -> {
                        list.add(i * 2 + 1, list.get(i * 2 + 1))
                    }
                    i * 2 + 1 == list.size -> {
                        list.add(list.get(list.size - 1))
                    }
                    else -> {
                        list.add(list.size - 1, list.get(list.size - 1))
                        //反向补点
                        list.reverse()
                        capacityExpansion(list, 16 - list.size, need)
                        list.reverse()
                    }
                }
            }
        }
    }

    //生产分享bitmap
    fun createDataBitMap(): Bitmap? {
        if (isNoData) {
            return null
        }
        return ImageUtils.view2Bitmap(binding.data)
    }
}