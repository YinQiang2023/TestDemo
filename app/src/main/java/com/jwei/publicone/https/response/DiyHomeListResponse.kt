package com.jwei.publicone.https.response

class DiyHomeListResponse {
    lateinit var dialList: List<Data>

    class Data {
        var dialId = ""    //String	表盘ID
        var dialName = ""    //String	表盘名称
        var effectImgUrl = ""    //String	表盘效果图UR

        override fun toString(): String {
            return "Data(dialId='$dialId', dialName='$dialName', effectImgUrl='$effectImgUrl')"
        }
    }

    override fun toString(): String {
        return "DiyHomeListResponse(dialList=$dialList)"
    }
}