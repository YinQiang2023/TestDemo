package com.smartwear.publicwatch.ui.debug

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.BaseAdapter
import android.widget.TextView
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.WeatherDayBean
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityDebugWeatherBinding
import com.smartwear.publicwatch.ui.debug.bean.WeatherSpinnerItem
import com.smartwear.publicwatch.viewmodel.DeviceModel
import java.util.*

class DebugWeatherActivity : BaseActivity<ActivityDebugWeatherBinding, DeviceModel>(
    ActivityDebugWeatherBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    private val mFilePath = PathUtils.getAppDataPathExternalFirst() + "/weather"
    private val weatherList: MutableList<WeatherSpinnerItem> = mutableListOf()
    private var myAdapter: MyAdapter? = null

    override fun setTitleId() = binding.title.root.id
    override fun initView() {
        super.initView()
        setTvTitle("天气设置")
        myAdapter = MyAdapter(this@DebugWeatherActivity)
        binding.spinerSelect.adapter = myAdapter

        binding.spinerSelect.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.etID.setText(weatherList[position].id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    override fun initData() {
        FileUtils.createOrExistsDir(mFilePath)
        val files = FileUtils.listFilesInDir(mFilePath)
        if (files.isNullOrEmpty()) {
            ToastUtils.showShort("$mFilePath 目录文件为空")
            return
        }
        files.forEach {
            val reader = it.bufferedReader()
            while (reader.ready()) {
                val line = reader.readLine()
                val weatherName = line.substringBefore("#")
                val id = line.substringAfter("#")
                weatherList.add(WeatherSpinnerItem(weatherName, id))
            }
        }
        myAdapter?.notifyDataSetChanged()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnSend.id -> {
                val bean = WeatherDayBean()
                val ca = Calendar.getInstance()
                ca.timeInMillis = System.currentTimeMillis()
                bean.year = ca.get(Calendar.YEAR)
                bean.month = ca.get(Calendar.MONTH) + 1
                bean.day = ca.get(Calendar.DAY_OF_MONTH)
                bean.hour = ca.get(Calendar.HOUR_OF_DAY)
                bean.minute = ca.get(Calendar.MINUTE)
                bean.second = ca.get(Calendar.SECOND)
                bean.cityName = binding.etCity.text.toString()
                bean.locationName = ""
                val listBean = WeatherDayBean.Data()
                listBean.now_temperature = binding.etNow.text.toString().toInt()
                listBean.low_temperature = binding.etLow.text.toString().toInt()
                listBean.high_temperature = binding.etHigh.text.toString().toInt()
                listBean.humidity = 0
                listBean.weather_id = binding.etID.text.toString().toInt()
                listBean.weather_name = weatherList[binding.spinerSelect.selectedItemPosition].weatherName
                listBean.Wind_speed = 0
                listBean.wind_info = 0
                listBean.Probability_of_rainfall = 0
                listBean.sun_rise = ""
                listBean.sun_set = ""
                bean.list.add(listBean)

                ControlBleTools.getInstance().sendWeatherDailyForecast(bean, object : ParsingStateManager.SendCmdStateListener() {
                    override fun onState(state: SendCmdState?) {
                        if (state == SendCmdState.SUCCEED || state == SendCmdState.UNINITIALIZED)
                            ToastUtils.showShort("发送成功")
                    }
                })
            }
        }
    }

    inner class MyAdapter(context: Context?) : BaseAdapter() {
        override fun getCount(): Int {
            return weatherList.size ?: 0
        }

        override fun getItem(position: Int): Any {
            return weatherList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
            val viewHolder: ViewHolder?
            if (convertView == null) {
                convertView = layoutInflater.inflate(android.R.layout.simple_spinner_item, parent, false)
                viewHolder = ViewHolder(convertView)
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            viewHolder.tvName?.text = weatherList[position].weatherName
            return convertView!!
        }

        internal inner class ViewHolder(rootView: View?) {
            var tvName: TextView? = null

            init {
                initView(rootView)
            }

            private fun initView(rootView: View?) {
                tvName = rootView!!.findViewById<View>(android.R.id.text1) as TextView
            }
        }
    }

}