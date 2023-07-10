package com.jwei.publicone.https.response

class MoreDialPageResponse {
    var count = "-1" //	Integer	总行数
    var list: List<Data>? = null

    class Data {
        var dialId = "" //	Long	表盘ID
        var dialName = "" //	String	表盘名称
        var effectImgUrl = "" //	String	表盘效果图URL

        override fun toString(): String {
            return "Data(dialId='$dialId', dialName='$dialName', effectImgUrl='$effectImgUrl')"
        }

    }

    override fun toString(): String {
        return "MoreDialPageResponse(count='$count', list=$list)"
    }

}