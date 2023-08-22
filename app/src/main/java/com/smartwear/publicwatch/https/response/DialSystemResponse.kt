package com.smartwear.publicwatch.https.response

class DialSystemResponse {
    lateinit var dialSystemList: List<Data>

    class Data {
        var dialCode = ""    //String	表盘ID
        var dialImageUrl = ""    //String	效果图URL
    }

    override fun toString(): String {
        return "DialSystemResponse(dialSystemList=$dialSystemList)"
    }

}