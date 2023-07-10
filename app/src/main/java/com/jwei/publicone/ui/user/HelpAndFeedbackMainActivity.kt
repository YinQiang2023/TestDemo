package com.jwei.publicone.ui.user

import android.content.Intent
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.jwei.publicone.R
import com.jwei.publicone.base.BaseActivity
import com.jwei.publicone.ui.adapter.CommonAdapter
import java.lang.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import com.jwei.publicone.base.BaseViewModel
import com.jwei.publicone.databinding.*


class HelpAndFeedbackMainActivity : BaseActivity<ActivityHelpAndFeedbackMainBinding, BaseViewModel>(
    ActivityHelpAndFeedbackMainBinding::inflate,
    BaseViewModel::class.java
), View.OnClickListener {

    private val list: MutableList<MutableMap<String, *>> = ArrayList()

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.appCompatTextView2.id -> {
                finish()
            }
        }
    }

    override fun initView() {
        binding.appCompatTextView2.setOnClickListener(this)
        initRv()
    }

    override fun initData() {
    }

    override fun setTitleId(): Int {
        isKeyBoard = false
        isDarkFont = false
        return binding.layoutTitle.id
    }

    private fun initRv() {
        fillData()
        binding.rvList.apply {
            layoutManager = LinearLayoutManager(this@HelpAndFeedbackMainActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
    }

    private fun fillData() {
        list.clear()
        val texts = resources.getStringArray(R.array.helpAndFeedbackNameList)
        val imgs = resources.obtainTypedArray(R.array.helpAndFeedbackImgList)
        for (index in texts.indices) {
            val map: MutableMap<String, Any> = HashMap()
            map["content"] = texts[index]
            map["img"] = imgs.getResourceId(index, 0)
            list.add(map)
        }
        imgs.recycle()
    }

    private fun initAdapter(): CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding> {
        return object : CommonAdapter<MutableMap<String, *>, ItemUserInfoBinding>(list) {

            override fun createBinding(parent: ViewGroup?, viewType: Int): ItemUserInfoBinding {
                return ItemUserInfoBinding.inflate(layoutInflater, parent, false)
            }

            override fun convert(v: ItemUserInfoBinding, t: MutableMap<String, *>, position: Int) {
                v.tvItemLeft.text = "${t["content"]}"
                v.ivItemLeft.setImageResource(t["img"] as Int)
                v.tvItemRight.visibility = View.GONE
                v.cslItemUserInfoParent.setOnClickListener {
                    try {
                        when (v.tvItemLeft.text.toString().trim()) {
                            getString(R.string.question_feedback) -> {
                                startActivity(Intent(this@HelpAndFeedbackMainActivity, QuestionFeedbackActivity::class.java))
                            }
                            getString(R.string.common_question_help) -> {
                                startActivity(Intent(this@HelpAndFeedbackMainActivity, QAActivity::class.java))
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

}