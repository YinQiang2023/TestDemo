package com.smartwear.xzfit.utils

/**
 * @author YinQiang
 * @date 2023/3/18
 */
object UrlPathUtils{
    val filePath: MutableMap<String, String> = mutableMapOf()

    fun getUrlPath(url: String): String? {
        return filePath[url]
    }

    fun putPath(url: String, path: String) {
        filePath[url]=path
    }
}