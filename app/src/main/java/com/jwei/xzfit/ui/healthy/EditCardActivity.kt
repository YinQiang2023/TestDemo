package com.jwei.xzfit.ui.healthy

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson.JSON
import com.jwei.xzfit.R
import com.jwei.xzfit.base.BaseActivity
import com.jwei.xzfit.base.BaseViewModel
import com.jwei.xzfit.databinding.ActivityEditCardBinding
import com.jwei.xzfit.databinding.ItemEditCardBinding
import com.jwei.xzfit.databinding.ItemEditCardTitleBinding
import com.jwei.xzfit.ui.adapter.ViewHolder
import com.jwei.xzfit.ui.data.Global
import com.jwei.xzfit.ui.healthy.bean.DragBean
import com.jwei.xzfit.ui.healthy.drag.DragAdapter
import com.jwei.xzfit.ui.healthy.drag.ItemDragCallback
import com.jwei.xzfit.utils.SpUtils
import com.jwei.xzfit.utils.ToastUtils

class EditCardActivity : BaseActivity<ActivityEditCardBinding, BaseViewModel>(ActivityEditCardBinding::inflate, BaseViewModel::class.java), View.OnClickListener {
    val TAG = EditCardActivity::class.java.simpleName
    private lateinit var dragCallback: ItemTouchHelper
    var editCardTempList = mutableListOf<DragBean>()//临时缓存

    override fun setTitleId(): Int {
        isDarkFont = false
        return binding.layoutTitle.id
    }

    private fun initAdapter(): DragAdapter<DragBean, ViewBinding> {
        return object : DragAdapter<DragBean, ViewBinding>(editCardTempList) {
            override fun createBinding(parent: ViewGroup?, viewType: Int): ViewBinding {
                return if (viewType == 0) {
                    ItemEditCardTitleBinding.inflate(layoutInflater, parent, false)
                } else {
                    ItemEditCardBinding.inflate(layoutInflater, parent, false)
                }
            }

            override fun getItemType(t: DragBean): Int {
                return if (t.isTitle) 0 else 1
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun convert(
                holder: ViewHolder<ViewBinding>,
                v: ViewBinding,
                t: DragBean,
                position: Int,
                hiddenCount: Int,
            ) {

                //小标题
                if (t.isTitle) {
                    val itemView = v as ItemEditCardTitleBinding
                    itemView.ivItem.text = t.centerTextId
                    if (hiddenCount > 0) {
                        itemView.lyHiddenItem.visibility = View.GONE
                    } else {
                        itemView.lyHiddenItem.visibility = View.VISIBLE
                    }
                } else {
                    val itemView = v as ItemEditCardBinding
                    itemView.ivItemLeft.setImageResource(t.leftImg)
                    itemView.tvItemText.text = t.centerTextId
                    //隐藏区
                    if (t.isHide) {
                        itemView.tvItemText.setTextColor(ContextCompat.getColor(this@EditCardActivity, R.color.color_878787))
                    }
                    //显示区
                    else {
                        itemView.tvItemText.setTextColor(ContextCompat.getColor(this@EditCardActivity, R.color.color_171717))
                    }
                    itemView.ivItemRight.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            dragCallback.startDrag(holder)
                        }
                        false
                    }
                }
            }

            override fun convert(v: ViewBinding, t: DragBean, position: Int) {
            }
        }
    }


    override fun initView() {
        super.initView()
        setViewsClickListener(this, binding.ivTitleLeft, binding.ivTitleRight)
        binding.rvEditCard.apply {
            layoutManager = LinearLayoutManager(this@EditCardActivity)
            setHasFixedSize(true)
            adapter = initAdapter()
        }
        dragCallback = ItemTouchHelper(
            ItemDragCallback(
                binding.rvEditCard.adapter as DragAdapter<*, *>?,
                editCardTempList, object : ItemDragCallback.OnDragListener {
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onComplete(start: Int, end: Int) {
                        if (start == -1 || end == -1 || start == end) {
                            return
                        }
                        binding.rvEditCard.adapter?.notifyDataSetChanged()
                    }
                }
            )
        )
        dragCallback.attachToRecyclerView(binding.rvEditCard)

    }


    override fun initData() {
        super.initData()
        editCardTempList.clear()
        //读取数据
        for (i in Global.editCardList.indices) {
            Log.i(TAG, "initView() i = $i editCardList[i] = ${Global.editCardList[i]}")
            editCardTempList.add(Global.editCardList[i].clone())
        }
        (binding.rvEditCard.adapter as DragAdapter<*, *>?)?.calcHideCount()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivTitleLeft.id -> {
                finish()
            }
            binding.ivTitleRight.id -> {
                saveListData()
            }
        }
    }

    private fun saveListData() {
        var count = 0
        for (i in editCardTempList.indices) {
            if (!editCardTempList[i].isTitle && !editCardTempList[i].isHide) {
                count++
            }
        }
        if (count < 3) {
            ToastUtils.showToast(getString(R.string.edit_card_less_than_three_tips))
            return
        }
        //清空数据
        Global.editCardList.clear()
        //写入数据
        for (i in editCardTempList.indices) {
            Log.i(TAG, "saveListData() i = $i editCardTempList[i] = ${editCardTempList[i]}")
            Global.editCardList.add(editCardTempList[i].clone())
        }
        //保存数据
        val jsonString = JSON.toJSONString(Global.editCardList)
        SpUtils.setValue(SpUtils.EDIT_CARD_ITEM_LIST, jsonString)
        setResult(RESULT_OK)
        //填充数据
        Global.fillListData()
        finish()
    }

    override fun onBackPress() {
        super.onBackPress()
        Log.i(TAG, "onBackPress()")
    }
}