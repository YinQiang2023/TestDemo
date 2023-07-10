package com.jwei.publicone.ui.debug

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.databinding.ActivityDebugNoticeBinding
import com.jwei.publicone.ui.debug.bean.NoticeSpinnerItem
import com.jwei.publicone.viewmodel.DeviceModel
import kotlinx.coroutines.*

class DebugNoticeActivity : BaseActivity<ActivityDebugNoticeBinding, DeviceModel>(
    ActivityDebugNoticeBinding::inflate, DeviceModel::class.java
), View.OnClickListener {
    private val mFilePath = PathUtils.getAppDataPathExternalFirst() + "/notice/app"
    private val mCharacterFilePath = PathUtils.getAppDataPathExternalFirst() + "/notice/character"

    private val appList: MutableList<NoticeSpinnerItem> = mutableListOf()

    private var myAdapter: MyAdapter? = null
    override fun setTitleId() = binding.title.root.id
    override fun initView() {
        super.initView()
        setTvTitle("第三方消息通知")
        myAdapter = MyAdapter(this@DebugNoticeActivity)
        binding.spinerSelect.adapter = myAdapter

        binding.etSend.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    val number = s.toString().toInt()
                    characterIndex = number - 1
                }
            }

            override fun afterTextChanged(s: Editable?) {}

        })
    }

    override fun initData() {
        super.initData()
        FileUtils.createOrExistsDir(mFilePath)
        FileUtils.createOrExistsDir(mCharacterFilePath)

        val files = FileUtils.listFilesInDir(mFilePath)
        if (files.isNullOrEmpty()) {
            ToastUtils.showShort("$mFilePath 目录文件为空")
            return
        }
        files.forEach {
            val reader = it.bufferedReader()
            while (reader.ready()) {
                val line = reader.readLine()
                val packageName = line.substringBefore("#")
                val appName = line.substringAfter("#")
                appList.add(NoticeSpinnerItem(packageName, appName))
            }
        }
        myAdapter?.notifyDataSetChanged()


        startEnabled.observe(this) {
            binding.btnStart.isClickable = it
        }
        progressIndex.observe(this) {
            var total = binding.tvProgress.text.toString().substringAfter("/")
            binding.tvProgress.text = "$it/$total"
        }
        refresh.observe(this) {
            if (it) {
                val number = characterIndex + 1
                binding.etSend.setText(number.toString())
                binding.tvSendContext.text = characterList[characterIndex]
            }
        }
    }

    private val characterList: MutableList<String> = mutableListOf()
    private var characterIndex = -1

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnStart.id -> {
                if (binding.etTime.text.toString().isEmpty() ||
                    binding.etCount.text.toString().isEmpty()
                ) {
                    ToastUtils.showShort("参数错误")
                    return
                }
                val appInfo = appList[binding.spinerSelect.selectedItemPosition]
                startSendMessage(
                    appInfo.appName,
                    appInfo.packageName,
                    binding.etTitle1.text.toString(),
                    binding.etContext1.text.toString(),
                    binding.etTitle2.text.toString(),
                    binding.etContext2.text.toString(),
                    binding.etTime.text.toString().toLong(),
                    binding.etCount.text.toString().toInt()
                )
            }
            binding.btnStart1.id -> {
                if (characterList.size == 0
                ) {
                    ToastUtils.showShort("未导入文件")
                    return
                }

                if (binding.btnStart1.isSelected) {
                    launchUI.cancel()
                    binding.btnStart1.text = "开始测试"
                    binding.btnStart1.setBackgroundColor(getColor(com.jwei.publicone.R.color.teal_200))

                } else {
                    if (binding.etTime1.text.toString().isEmpty()
                        ||
                        characterIndex > characterList.size - 1
                    ) {
                        ToastUtils.showShort("参数错误")
                        return
                    }
                    binding.btnStart1.text = "结束测试"
                    binding.btnStart1.setBackgroundColor(Color.RED)
                    startCharacterMessage(
                        binding.etTime1.text.toString().toLong()
                    )
                }
                binding.btnStart1.isSelected = !binding.btnStart1.isSelected

                binding.btnLast.isClickable = !binding.btnStart1.isSelected
                binding.btnNext.isClickable = !binding.btnStart1.isSelected
                if (binding.btnStart1.isSelected) {
                    binding.btnLast.setBackgroundColor(Color.GRAY)
                    binding.btnNext.setBackgroundColor(Color.GRAY)
                } else {
                    binding.btnLast.setBackgroundColor(getColor(com.jwei.publicone.R.color.teal_200))
                    binding.btnNext.setBackgroundColor(getColor(com.jwei.publicone.R.color.teal_200))
                }

            }


            binding.btnImport.id -> {
                val files = FileUtils.listFilesInDir(mCharacterFilePath)
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mCharacterFilePath 目录文件为空")
                    return
                }
                files.forEach {
                    val reader = it.bufferedReader()
                    while (reader.ready()) {
                        val line = reader.readLine()
                        characterList.add(line)
                    }
                }
                binding.tvTotal.text = "/${characterList.size}"
                ToastUtils.showShort("$mCharacterFilePath 导入成功")


            }

            binding.btnLast.id -> {
                if (characterIndex - 1 > -1) {
                    characterIndex--
                    refresh.setValue(true)
                    sendCharacterMessage()
                }
            }
            binding.btnNext.id -> {
                if (characterIndex + 1 < characterList.size) {
                    characterIndex++
                    refresh.setValue(true)
                    sendCharacterMessage()
                }
            }
        }
    }

    lateinit var launchUI: Job
    private fun startCharacterMessage(time: Long) {
        launchUI = launchUI {
            while (characterIndex < characterList.size - 1 && !this@DebugNoticeActivity.isDestroyed) {
                characterIndex++
                refresh.postValue(true)
                sendCharacterMessage()
                delay(time)
            }
        }
    }

    val refresh = MutableLiveData<Boolean>()
    private fun sendCharacterMessage() {
        val appInfo = appList[binding.spinerSelect.selectedItemPosition]
        ControlBleTools.getInstance().sendAppNotification(
            appInfo.appName,
            appInfo.packageName,
            "",
            characterList[characterIndex],
            "",
            object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
//                            if (state == SendCmdState.SUCCEED || state == SendCmdState.UNINITIALIZED)
                }
            }
        )
    }

    var startEnabled = MutableLiveData<Boolean>()
    var progressIndex = MutableLiveData<Int>()
    private fun startSendMessage(appName: String, packageName: String, title1: String, context1: String, title2: String, context2: String, time: Long, count: Int) {
        var index = 0
        binding.tvProgress.text = "/${count * 2}"
        launchUI {
            startEnabled.postValue(false)
            while (index < count && !this@DebugNoticeActivity.isDestroyed) {
                ControlBleTools.getInstance().sendAppNotification(
                    appName,
                    packageName,
                    title1,
                    context1,
                    "",
                    object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
//                            if (state == SendCmdState.SUCCEED || state == SendCmdState.UNINITIALIZED)
                        }
                    }
                )

                ControlBleTools.getInstance().sendAppNotification(
                    appName,
                    packageName,
                    title2,
                    context2,
                    "",
                    object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
//                            if (state == SendCmdState.SUCCEED || state == SendCmdState.UNINITIALIZED)
                        }
                    }
                )
                index++
                progressIndex.postValue(index * 2)
                delay(time)
            }
            startEnabled.postValue(true)
        }
    }

    fun launchUI(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.Default).launch {
        block()
//        try {
//            block()
//        } catch (e: Exception){
//            error.value = e
//            Log.e("xxx Exception", e.toString())
//        }
    }

    inner class MyAdapter(context: Context?) : BaseAdapter() {
        override fun getCount(): Int {
            return appList.size ?: 0
        }

        override fun getItem(position: Int): Any {
            return appList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
            val viewHolder: ViewHolder?
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.simple_spinner_item, parent, false)
                viewHolder = ViewHolder(convertView)
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            viewHolder.tvName?.text = appList[position].appName
            return convertView!!
        }

        internal inner class ViewHolder(rootView: View?) {
            var tvName: TextView? = null

            init {
                initView(rootView)
            }

            private fun initView(rootView: View?) {
                tvName = rootView!!.findViewById<View>(R.id.text1) as TextView
            }
        }
    }
}