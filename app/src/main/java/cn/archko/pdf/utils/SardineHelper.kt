package cn.archko.pdf.utils

import android.util.Log
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * @author: archko 2024/8/8 :10:14
 */
class SardineHelper {

    companion object {

        const val DEFAULT_JSON = "amupdf_lastest.json"
        const val KEY_CONFIG = "webdav"
        const val KEY_NAME = "name"
        const val KEY_PASS = "pass"

        const val JIANGUOYUN = "https://dav.jianguoyun.com"
        const val PATH = "/dav/我的坚果云"
        const val JIANGUOYUN_URL = "$JIANGUOYUN$PATH"

        fun testAuth(sardine: OkHttpSardine): Boolean {
            val dirPath = "$JIANGUOYUN_URL/amupdf/page"
            val exist = sardine.exists(dirPath)
            if (exist) {
                return true
            }
            val path = "$dirPath/page"
            sardine.createDirectory(path)
            return sardine.exists(path)
        }

        fun createDir(sardine: OkHttpSardine) {
            val dirPath = "$JIANGUOYUN_URL/amupdf"
            sardine.createDirectory(dirPath)
        }

        fun checkExistence(sardine: OkHttpSardine): Boolean {
            val dirPath = "$JIANGUOYUN_URL/amupdf"
            return sardine.exists(dirPath)
        }

        fun uploadFile(sardine: OkHttpSardine, data: ByteArray, name: String) {
            val filePath = "$JIANGUOYUN_URL/amupdf/$name"
            sardine.put(filePath, data)
        }

        fun uploadFile(sardine: OkHttpSardine, fileContent: String, name: String) {
            val filePath = "$JIANGUOYUN_URL/amupdf/$name"
            // 将变量转变为byte字节数组，以传输到网盘
            val data = fileContent.toByteArray()
            sardine.put(filePath, data)
        }

        fun listFiles(sardine: OkHttpSardine, name: String): MutableList<DavResource>? {
            val filePath = "$JIANGUOYUN_URL/$name"
            return sardine.list(filePath)
        }

        fun downloadFile(sardine: OkHttpSardine, name: String): String {
            var path = name
            if (name.startsWith(PATH)) {
                path = name.substring(PATH.length)
            }
            val filePath = "$JIANGUOYUN_URL$path"
            Log.d("TAG", "download:$path, $name")
            val download = sardine.get(filePath)
            // 以输入流的形式读取下载的文件，并转换为字符串
            val fileContent = BufferedReader(InputStreamReader(download)).useLines { lines ->
                val results = StringBuilder()
                lines.forEach {
                    results.append(it)
                }
                results.toString()
            }
            return fileContent
        }

        fun deleteFile(sardine: OkHttpSardine, name: String) {
            val filePath = "$JIANGUOYUN_URL/amupdf/$name"
            sardine.delete(filePath)
        }
    }
}