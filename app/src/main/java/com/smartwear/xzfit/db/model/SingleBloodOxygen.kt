package com.smartwear.xzfit.db.model

class SingleBloodOxygen : BaseData() {
    var userId: String = ""
    var singleBloodOxygenData: String = ""
    var measureTimestamp: String = ""
    override fun toString(): String {
        return "SingleBloodOxygen(userId='$userId', singleBloodOxygenData='$singleBloodOxygenData', measureTimestamp='$measureTimestamp')"
    }


}