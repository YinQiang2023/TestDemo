/*
 * Copyright (c) 2016-present 贵州纳雍穿青人李裕江<1032694760@qq.com>
 *
 * The software is licensed under the Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *     http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 * PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.smartwear.publicwatch.view.wheelview.contract;


import com.smartwear.publicwatch.view.wheelview.widget.WheelView;

/**
 * 滚轮滑动接口
 *
 * @author Florent Champigny
 * @since 2019/5/14 20:03
 */
public interface OnWheelChangedListener {

    /**
     * Invoke when scroll stopped
     * Will return a distance offset which between current scroll position and
     * initial position, this offset is a positive or a negative, positive means
     * scrolling from bottom to top, negative means scrolling from top to bottom
     *
     * @param view   wheel view
     * @param offset Distance offset which between current scroll position and initial position
     */
    void onWheelScrolled(WheelView view, int offset);

    /**
     * Invoke when scroll stopped
     * This method will be called when wheel stop and return current selected item data's
     * position in list
     *
     * @param view     wheel view
     * @param position Current selected item data's position in list
     */
    void onWheelSelected(WheelView view, int position);

    /**
     * Invoke when scroll state changed
     * The state always between idle, dragging, and scrolling, this method will
     * be called when they switch
     *
     * @param view  wheel view
     * @param state {@link WheelView#SCROLL_STATE_IDLE}
     *              {@link WheelView#SCROLL_STATE_DRAGGING}
     *              {@link WheelView#SCROLL_STATE_SCROLLING}
     *              <p>
     *              State only one of the following
     *              {@link WheelView#SCROLL_STATE_IDLE}
     *              Express WheelPicker in state of idle
     *              {@link WheelView#SCROLL_STATE_DRAGGING}
     *              Express WheelPicker in state of dragging
     *              {@link WheelView#SCROLL_STATE_SCROLLING}
     *              Express WheelPicker in state of scrolling
     */
    void onWheelScrollStateChanged(WheelView view, int state);

    /**
     * Invoke when loop finished
     *
     * @param view wheel view
     */
    void onWheelLoopFinished(WheelView view);

}
