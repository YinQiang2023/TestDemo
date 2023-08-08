package com.smartwear.xzfit.ui.sport

import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.TimeUtils
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import com.smartwear.xzfit.R
import com.smartwear.xzfit.base.BaseActivity
import com.smartwear.xzfit.databinding.ActivitySportRecordBinding
import com.smartwear.xzfit.databinding.ItemSportRecordCommonBinding
import com.smartwear.xzfit.db.model.sport.SportModleInfo
import com.smartwear.xzfit.db.model.track.BehaviorTrackingLog
import com.smartwear.xzfit.dialog.DialogUtils
import com.smartwear.xzfit.ui.adapter.MultiItemCommonAdapter
import com.smartwear.xzfit.ui.refresh.CustomizeRefreshHeader
import com.smartwear.xzfit.utils.*
import com.smartwear.xzfit.utils.manager.AppTrackingManager
import com.smartwear.xzfit.viewmodel.SportModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Android on 2021/10/16.
 * 运动记录
 */
class SportRecordActivity : BaseActivity<ActivitySportRecordBinding, SportModel>(
    ActivitySportRecordBinding::inflate, SportModel::class.java
), View.OnClickListener {

    //选中的日期
    private var mSelectionDate = ""

    //日期选择间隔，避免两次日历变化导致第一次无效的请求
    private val AVOID_REPEATER_DATE_CHANGE = 200L

    //选中过的日期
    private var mSDates = mutableListOf<String>()

    //当前查询页
    private var pageIndex = 1

    //是否手动改变
    private var isManual = true

    private var infos = mutableListOf<SportModleInfo>()

    private lateinit var loadDialog: Dialog

    //行为埋点
    private var behaviorTrackingLog: BehaviorTrackingLog? = null

    override fun setTitleId(): Int = binding.title.layoutTitle.id

    override fun initView() {
        super.initView()
        setTvTitle(R.string.sport_record)
        behaviorTrackingLog = AppTrackingManager.getNewBehaviorTracking("10", "32")
        loadDialog = DialogUtils.showLoad(this)

        mSelectionDate = TimeUtils.getNowString(com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat(DateUtils.TIME_YYYY_MM_DD))
        mSDates.add(mSelectionDate)
        binding.tvDate.text = mSelectionDate
        //binding.calendarLayout.expand()

        if (!loadDialog.isShowing) {
            loadDialog.show()
        }
        //获取今日数据
        viewModel.querySportRecordData(mSelectionDate)

        binding.lyRefresh.setEnableLoadMore(false)
        //下拉刷新
        binding.lyRefresh.setOnRefreshListener {
            pageIndex = 1
            viewModel.querySportRecordData(mSelectionDate)
        }
        binding.lyRefresh.post {
            if (binding.lyRefresh.refreshHeader is CustomizeRefreshHeader) {
                val header = binding.lyRefresh.refreshHeader as CustomizeRefreshHeader
                header.setCanShowRefreshing(true)
            }
        }

        //BUG
//        binding.lyRefresh.setOnLoadMoreListener {
//            if(!infos.isNullOrEmpty() && infos.size % 15 == 0) {
//                LogUtils.d("上拉加载。。。。")
//                pageIndex ++
//                viewModel.querySportRecordData(mSelectionDate,pageIndex = pageIndex)
//            }else{
//                binding.lyRefresh.finishLoadMore()
//            }
//            binding.lyRefresh.finishLoadMore(2000)
//        }


        //选择日期刷新
        binding.calendarView.setOnCalendarSelectListener(object :
            CalendarView.OnCalendarSelectListener {
            override fun onCalendarOutOfRange(calendar: Calendar?) {
            }

            override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
                if (isClick) {
                    val date = DateUtils.FormatDateYYYYMMDD(calendar)
                    mSelectionDate = date
                    if (isClick && binding.calendarLayout.isExpand) {
                        binding.calendarLayout.shrink()
                        isManual = true
                        rotateArrow()
                    }
                    calendarSelectHandler.removeCallbacksAndMessages(null)
                    calendarSelectHandler.postDelayed(calendarSelectRunnable, AVOID_REPEATER_DATE_CHANGE)
                    if (!loadDialog.isShowing) {
                        loadDialog.show()
                    }
                } else {
                    binding.tvDate.text = TimeUtils.millis2String(calendar.timeInMillis, com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat(DateUtils.TIME_YYYY_MM))
                }
            }
        })

        binding.calendarView.setOnViewChangeListener(object : CalendarView.OnViewChangeListener {
            override fun onViewChange(isMonthView: Boolean) {
                //LogUtils.e("onViewChange = $isMonthView")
                if (isManual) {
                    isManual = false
                    return
                }
                rotateArrow()
            }
        })

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SportRecordActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position: Int = parent.getChildAdapterPosition(view)
                    outRect.top = ConvertUtils.dp2px(12F)
                    if (position == infos.size - 1) {
                        outRect.bottom = ConvertUtils.dp2px(30F)
                    }
                }
            })
        }

        setViewsClickListener(this, binding.lyDate)
    }

    //region 用户选择了日历日期
    private var calendarSelectHandler = Handler(Looper.getMainLooper())
    private var calendarSelectRunnable = Runnable {
        calendarSelect()
    }

    fun calendarSelect() {
        val calendarData = java.util.Calendar.getInstance()
        calendarData.timeInMillis = DateUtils.getLongTime(mSelectionDate, DateUtils.TIME_YYYY_MM_DD)
        var registerTime = DateUtils.getLongTime(SpUtils.getValue(SpUtils.REGISTER_TIME, "0"), DateUtils.TIME_YYYY_MM_DD)
        LogUtils.d("注册日期 ${SpUtils.getValue(SpUtils.REGISTER_TIME, "0")}, 选择日期 $mSelectionDate")
        if (calendarData.timeInMillis < registerTime) {
            ToastUtils.showToast(R.string.history_over_time_tips2)
        }
        if (calendarData.timeInMillis > System.currentTimeMillis()) {
            calendarData.timeInMillis = System.currentTimeMillis()
            ToastUtils.showToast(R.string.history_over_time_tips)
        }

        binding.tvDate.text = mSelectionDate
        //每次生命周期同步过云的日期不再同步
        var isSync = false
        if (!mSDates.contains(mSelectionDate)) {
            isSync = true
            mSDates.add(mSelectionDate)
        }
        pageIndex = 1
        viewModel.querySportRecordData(mSelectionDate, isSync)
    }
    //endregion


    override fun initData() {
        super.initData()
        startVisibleTimeTimer()
        viewModel.sportRecordData.observe(this) {
            binding.lyRefresh.complete()
            DialogUtils.dismissDialog(loadDialog)
            if (it != null) {
                infos.clear()
                infos.addAll(it)
                binding.recyclerView.adapter?.notifyDataSetChanged()

                LogUtils.d("运动记录 info size == ${infos.size}")
                //LogUtils.json(infos)
                binding.recyclerView.visibility = if (infos.size > 0) View.VISIBLE else View.GONE
                binding.noData.layoutNoData.visibility =
                    if (infos.size > 0) View.GONE else View.VISIBLE
            }
        }
    }

    private fun initAdapter(): MultiItemCommonAdapter<SportModleInfo, ViewBinding> {
        return object : MultiItemCommonAdapter<SportModleInfo, ViewBinding>(infos) {
            override fun getItemType(t: SportModleInfo): Int {
                return 0
            }

            override fun createBinding(parent: ViewGroup?, viewType: Int): ViewBinding {
                return when (viewType) {
                    0 -> ItemSportRecordCommonBinding.inflate(layoutInflater, parent, false)
                    else -> ItemSportRecordCommonBinding.inflate(layoutInflater, parent, false)
                }
            }

            override fun convert(v: ViewBinding, t: SportModleInfo, position: Int) {
                val itemBinding = when (getItemType(t)) {
                    0 -> v as ItemSportRecordCommonBinding
                    else -> v as ItemSportRecordCommonBinding
                }
//                itemBinding.vBg.setCardBackgroundColor(
//                    when (position % 4) {
//                        0 -> ContextCompat.getColor(this@SportRecordActivity, R.color.color_3C243A)
//                        1 -> ContextCompat.getColor(this@SportRecordActivity, R.color.color_28243C)
//                        2 -> ContextCompat.getColor(this@SportRecordActivity, R.color.color_24303C)
//                        3 -> ContextCompat.getColor(this@SportRecordActivity, R.color.color_243C38)
//                        else -> ContextCompat.getColor(
//                            this@SportRecordActivity,
//                            R.color.color_3C243A
//                        )
//                    }
//                )

                itemBinding.tvType.text = SportTypeUtils.getSportTypeName(t.dataSources, t.exerciseType)
                val drawable: Drawable? = ContextCompat.getDrawable(
                    this@SportRecordActivity,
                    if (t.dataSources == 0) R.mipmap.sport_scene_phone else R.mipmap.sport_scene_wear
                )
                if (drawable != null) {
                    itemBinding.imType.setImageDrawable(drawable)
                }

                itemBinding.ivIcon.setImageDrawable(
                    SportTypeUtils.getSportTypeImg(
                        t.dataSources,
                        t.exerciseType
                    )
                )
                itemBinding.tvStartTime.text = TimeUtils.millis2String(
                    t.sportTime * 1000,
                    com.smartwear.xzfit.utils.TimeUtils.getSafeDateFormat(com.smartwear.xzfit.utils.TimeUtils.DATEFORMAT_HOUR_MIN)
                )
                itemBinding.tvTime.text = com.smartwear.xzfit.utils.TimeUtils.millis2String(t.sportDuration * 1000L)
                itemBinding.tvCalories.text = SpannableStringTool.get()
                    .append(t.burnCalories.toString())
                    .setFontSize(16f)
                    .setForegroundColor(ContextCompat.getColor(this@SportRecordActivity, R.color.color_171717))
                    .append(" ")
                    .append(getString(R.string.unit_calories))
                    .setFontSize(12f)
                    .setForegroundColor(ContextCompat.getColor(this@SportRecordActivity, R.color.color_171717))
                    .create()

                itemBinding.root.setOnClickListener {
                    gotoDetail(t)
                }
            }
        }
    }

    /**
     * 进入详情
     * */
    private fun gotoDetail(t: SportModleInfo) {
        var isDetails = false
        when (t.dataSources) {
            0 -> {
                isDetails = t.exerciseApp != null
            }
            1 -> {
                isDetails = t.exerciseAuxiliary != null
            }
            2 -> {
                isDetails =
                    (t.exerciseIndoor != null || t.exerciseOutdoor != null || t.exerciseSwimming != null)
            }
        }
        if (isDetails) { //有详情
            viewModel.sportLiveData.getSportModleInfo().postValue(t)
            startActivity(Intent(this@SportRecordActivity, SportDataActivity::class.java))
            return
        }

        //请求详情并储存
        loadDialog.show()
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val info = viewModel.querySportDetailsData(t)
            DialogUtils.dismissDialog(loadDialog)
            if (info != null) {
                withContext(Dispatchers.Main) {
                    viewModel.sportLiveData.getSportModleInfo().postValue(info)
                    startActivity(Intent(this@SportRecordActivity, SportDataActivity::class.java))
                }
            } else {
                withContext(Dispatchers.Main) {
                    ToastUtils.showToast(getString(R.string.no_data))
                }
            }
        }
    }

    /**
     * icon跟随日期布局开闭状态旋转
     * */
    private fun rotateArrow() {
        val degree = if (binding.ivDateArrow.tag == null || binding.ivDateArrow.tag == true) {
            binding.ivDateArrow.tag = false
            -180F
        } else {
            binding.ivDateArrow.tag = true
            0F
        }
        binding.ivDateArrow.animate().setDuration(350).rotation(degree)
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.lyDate.id -> {
                isManual = true
                rotateArrow()
                if (binding.calendarLayout.isExpand) {
                    binding.calendarLayout.shrink()
                } else {
                    binding.calendarLayout.expand()
                }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d("visibleTime:$visibleTime")
        if (visibleTime >= 9999) {
            visibleTime = 9999
        }
        behaviorTrackingLog?.let {
            it.functionStatus = "1"
            it.durationSec = visibleTime.toString()
            AppTrackingManager.saveBehaviorTracking(it)
        }
    }

}