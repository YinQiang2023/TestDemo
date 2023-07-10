package com.jwei.publicone.ui.healthy.bean

open class DragBean :Cloneable{
    var leftImg = 0
    //    var centerText = ""
    var centerTextId = ""
    var isTitle = false
    var isHide = false

    var tag = ""
    override fun toString(): String {
        return "DragBean(leftImg=$leftImg, centerTextId='$centerTextId', isTitle=$isTitle, isHide=$isHide, tag='$tag')"
    }

    public override fun clone(): DragBean {
        return super.clone() as DragBean
    }

}