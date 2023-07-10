package com.jwei.publicone.ui.healthy.drag;


import android.app.Service;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.jwei.publicone.R;
import com.jwei.publicone.base.BaseApplication;
import com.jwei.publicone.ui.device.bean.DevAppItem;
import com.jwei.publicone.ui.healthy.bean.DragBean;

import java.util.ArrayList;
import java.util.List;

public class ItemDragCallback extends ItemTouchHelper.Callback {
    private DragAdapter adapter;
    //    private boolean isLongPressDragEnabled = true;
    private List<DragBean> mData = new ArrayList<>();
    //是否可以移动
    private boolean isCanMove = true;
    //首置位是否可以移动
    private boolean isZeroCanDrag = true;
    private OnDragListener listener;
    //是否再移动中
    private boolean isMoveing = false;
    //起始位置
    private int startPosition = -1;
    //结束位置
    private int endPosition = -1;

    public void setCanMove(boolean canMove) {
        isCanMove = canMove;
    }

    public ItemDragCallback(DragAdapter mAdapter, List<DragBean> mData, OnDragListener listener) {
        adapter = mAdapter;
        this.mData = mData;
        this.listener = listener;
    }

    public ItemDragCallback(DragAdapter mAdapter, List<DragBean> mData, boolean isZeroCanDrag, OnDragListener listener) {
        adapter = mAdapter;
        this.mData = mData;
        this.isZeroCanDrag = isZeroCanDrag;
        this.listener = listener;
    }

    /**
     * getMovementFlags（），这个方法是设置是否滑动时间，以及拖拽的方向，所以在这里需要判断一下是列表布局还是网格布局，
     * 如果是列表布局的话则拖拽方向为DOWN和UP，如果是网格布局的话则是DOWN和UP和LEFT和RIGHT
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        //获取当前position
        int position = viewHolder.getLayoutPosition();
        if (mData.size() <= 0) return 0;
        if (mData.get(position).isTitle()) {
            return 0;
        }
        if(!isCanMove){
            return 0;
        }
        if (!isZeroCanDrag && ((DevAppItem) mData.get(position)).getProtocolId() == 0) {
            return 0;
        }
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
    }

    /**
     * onMove（）方法则是我们在拖动的时候不断回调的方法，在这里我们需要将正在拖拽的item和集合的item进行交换元素，然后在通知适配器更新数据
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        //得到当拖拽的viewHolder的Position  //拖动的position
        int fromPosition = viewHolder.getAdapterPosition();
        //拿到当前拖拽到的item的viewHolder  //释放的position
        int toPosition = target.getAdapterPosition();
        //记录起末位置
        if (!isMoveing) {
            isMoveing = true;
            startPosition = fromPosition;
        }
        endPosition = toPosition;

        int position = viewHolder.getLayoutPosition();

        if (mData.get(position).isTitle()) {
            return false;
        }
        if (!isZeroCanDrag && ((DevAppItem) mData.get(toPosition)).getProtocolId() == 0) {
            return false;
        }
        return adapter.isMove(fromPosition, toPosition, position);
    }

    /**
     * onSwiped（）是替换后调用的方法，可以不用管
     */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        LogUtils.d("onSwiped --> " + direction);
    }

    /**
     * 希望在拖拽的时候将被拖拽的Item高亮，这样用户体验要好很多，所以我们要重写CallBack对象中的
     * onSelectedChanged（）和clearView（）方法，在选中的时候设置高亮背景色，在完成的时候移除高亮背景色
     */
    /**
     * 长按选中Item的时候开始调用
     *
     * @param viewHolder
     * @param actionState
     */
    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
//            viewHolder.itemView.setBackgroundColor(Color.LTGRAY);
            //获取系统震动服务
            Vibrator vib = (Vibrator) BaseApplication.mContext.getSystemService(Service.VIBRATOR_SERVICE);
            if (vib != null) {
                vib.vibrate(70);
            }
        }
        super.onSelectedChanged(viewHolder, actionState);
    }

    /**
     * 手指松开的时候还原
     *
     * @param recyclerView
     * @param viewHolder
     */
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        //LogUtils.d("clearView");
        viewHolder.itemView.setBackgroundResource(R.drawable.public_bg);

        if (listener != null) {
            listener.onComplete(startPosition, endPosition);
        }
        isMoveing = false;
        startPosition = -1;
        endPosition = -1;
    }

    public interface OnDragListener {
        void onComplete(int start, int end);
    }
}
