package com.jwei.publicone.utils

import java.io.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object FileUtils {
    /**
     * 删除文件夹里面的所有文件
     *
     * @param path 绝对地址
     * @return 删除成功，则返回true，删除失败，则返回false
     */
    @JvmStatic
    fun deleteAll(path: String): Boolean {
        var path = path
        if (!path.endsWith(File.separator)) { // 如果path不以文件分隔符结尾，自动添加文件分隔符
            path += File.separator
        }
        val dirFile = File(path)
        if (!dirFile.exists() || !dirFile.isDirectory) {
            return false // 如果dirFiler对应的文件不存在，或者不是一个目录，则退出
        }
        var flag = true
        // 删除文件夹下的所有文件(包括子目录)
        val files = dirFile.listFiles() ?: return false
        for (file in files) {
            if (file.isFile) {
                file.delete() // 删除子文件
            } else { // 删除子目录
                flag = deleteAll(file.absolutePath)
                if (!flag) {
                    break
                }
            }
        }
        return if (!flag) {
            false
        } else dirFile.delete() // 删除当前目录
    }

    @JvmStatic
    fun getBytes(filePath: String?): ByteArray? {
        var buffer: ByteArray? = null
        try {
            val file = File(filePath)
            val fis = FileInputStream(file)
            val bos = ByteArrayOutputStream(1000)
            val b = ByteArray(1000)
            var n: Int
            while (fis.read(b).also { n = it } != -1) {
                bos.write(b, 0, n)
            }
            fis.close()
            bos.close()
            buffer = bos.toByteArray()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return buffer
    }

    /**
     * 获取文件的大小
     */
    fun getSize(size: Long): String {
        var result = "0 B"
        val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.ENGLISH))
        val size = size
        result = if (size < 1024) {
            df.format(size) + " B"
        } else if (size < 1048576) {
            df.format(size.toDouble() / 1024) + " KB"
        } else if (size < 1073741824) {
            df.format(size.toDouble() / 1048576) + " MB"
        } else {
            df.format(size.toDouble() / 1073741824) + " GB"
        }
        return result
    }

    fun getSizeForKb(size: Int): String {
        var result = "0 KB"
        val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.ENGLISH))
        result = if (size >= 1024) {
            df.format(size.toDouble() / 1024) + " MB"
        } else {
            "$size KB"
        }
        return result
    }
}