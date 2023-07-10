package com.jwei.publicone.ui.device.theme

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityFunctionSelectBinding
import com.jwei.publicone.ui.device.bean.diydial.MyDiyDialUtils
import com.jwei.publicone.viewmodel.DeviceModel
import kotlinx.android.synthetic.main.activity_function_select.*

class FunctionSelectActivity : BaseActivity<ActivityFunctionSelectBinding, DeviceModel>(
    ActivityFunctionSelectBinding::inflate,
    DeviceModel::class.java
) {
    companion object {
        const val ACTIVITY_DATA_TEXT = "type"
        const val RESULT_DATA_TEXT = "data"
        const val ACTIVITY_DATA_TITLE = "title"
    }

    private lateinit var functionsConfig: DiyWatchFaceConfigBean.FunctionsConfig
    private lateinit var functions: MutableList<DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType>
    private var typeChoose = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inits()
        intent.getStringExtra(ACTIVITY_DATA_TITLE)?.let { setTvTitle(it) }
    }

    override fun setTitleId(): Int {
        return binding.title.layoutTitle.id
    }

    override fun initView() {
        tvTitle?.setOnClickListener {
            val intent = intent
            intent.putExtra(RESULT_DATA_TEXT, GsonUtils.toJson(functionsConfig))
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
    private fun inits() {
        val json = intent.getStringExtra(ACTIVITY_DATA_TEXT)
        functionsConfig =
            GsonUtils.fromJson(json, DiyWatchFaceConfigBean.FunctionsConfig::class.java)
        functions = functionsConfig.functionsConfigTypes

        var havaOff = false
        functions.forEach {
            if (it.type == 0)
                havaOff = true
        }
        if (!havaOff) {
            functions.add(0, DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType(0))
        }

        typeChoose = functionsConfig.typeChoose
        if (functions.isNullOrEmpty()) {
            finish()
            return
        }
        LogUtils.json(functions)
        rvFunction.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvFunction.adapter = FunctionAdapter(this, functions) {
//            ToastUtils.showShort("function:" + functions.get(it))
            functionsConfig.typeChoose = functions.get(it).type
            typeChoose = functions.get(it).type
        }
    }

    inner class FunctionAdapter(
        private val context: Context,
        private val data: List<DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType>,
        var selected: (postion: Int) -> Unit,
    ) :
        RecyclerView.Adapter<FunctionAdapter.FunctionViewHolder>() {

        inner class FunctionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var rootLayout: ConstraintLayout = view.findViewById(R.id.root_layout)
            var tvTitle: TextView = view.findViewById(R.id.tv_title)
            var ivSelected: ImageView = view.findViewById(R.id.iv_selected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionViewHolder {
            return FunctionViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_function, parent, false)
            )
        }

        override fun onBindViewHolder(holder: FunctionViewHolder, position: Int) {
            val function = data.get(position)
            holder.tvTitle.text =
                MyDiyDialUtils.getFunctionsDetailNameByType(context, function.type)
            holder.ivSelected.visibility =
                if (function.type == typeChoose) View.VISIBLE else View.GONE
            holder.rootLayout.setOnClickListener {
                selected(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = data.size
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = intent
            intent.putExtra(RESULT_DATA_TEXT, GsonUtils.toJson(functionsConfig))
            setResult(Activity.RESULT_OK, intent)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}