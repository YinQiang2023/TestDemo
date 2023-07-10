package com.jwei.publicone.https.response

class DiyInfoResponse {
    var dialId = ""    //String	表盘ID
    var userId = ""    //String	用户ID

    override fun toString(): String {
        return "DiyInfoResponse(dialId=$dialId,userId=$userId)"
    }
}