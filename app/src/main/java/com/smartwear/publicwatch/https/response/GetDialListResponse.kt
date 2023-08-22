package com.smartwear.publicwatch.https.response

class GetDialListResponse {
    lateinit var list: List<Data>

    class Data {
        var dialTypeId = ""//	Integer	风格ID、颜色ID
        var dialTypeName = ""//	String	风格名称、颜色名称
        var dialType = ""//	int	数据类型 1：风格 2：颜色
        var dialList: List<Data2> = mutableListOf() //--	集合	表盘列表 最多3条

        override fun toString(): String {
            return "Data(dialTypeId='$dialTypeId', dialTypeName='$dialTypeName', dialType='$dialType', dialList=$dialList)"
        }

    }

    class Data2 {
        var dialId = ""//	Long	表盘ID
        var dialName = ""//	String	表盘名称
        var effectImgUrl = ""//	String	表盘效果图URL

        override fun toString(): String {
            return "Data2(dialId='$dialId', dialName='$dialName', effectImgUrl='$effectImgUrl')"
        }

    }

    override fun toString(): String {
        return "GetDialListResponse(list=$list)"
    }
}