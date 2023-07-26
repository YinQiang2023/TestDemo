package com.jwei.xzfit.ui.livedata.bean

class BatteryBean {
    var capacity = 0
    var chargeStatus = 0

    constructor(capacity: Int, chargeStatus: Int) {
        this.capacity = capacity
        this.chargeStatus = chargeStatus
    }
}