package com.smartwear.publicwatch.db.model

class SingleBloodOxygen : BaseData() {
    var userId: String = ""
    var singleBloodOxygenData: String = ""
    var measureTimestamp: String = ""
    override fun toString(): String {
        return "SingleBloodOxygen(userId='$userId', singleBloodOxygenData='$singleBloodOxygenData', measureTimestamp='$measureTimestamp')"
    }


}