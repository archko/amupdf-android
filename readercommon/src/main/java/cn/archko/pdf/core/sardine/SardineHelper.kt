package cn.archko.pdf.core.sardine

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
        const val KEY_CONFIG_JSON = "webdav_config_json"
        const val KEY_CONFIG_USER = "webdav_config_user"
        const val KEY_NAME = "name"
        const val KEY_PASS = "pass"
        const val KEY_PATH = "path"
        const val KEY_HOST = "host"

        //const val JIANGUOYUN = "https://dav.jianguoyun.com"
        //const val PATH = "/dav/我的坚果云"
        //const val JIANGUOYUN_URL = "$JIANGUOYUN$PATH"

        fun testPathOrCreate(sardine: OkHttpSardine, host: String, path: String): Boolean {
            val dirPath = "$host$path"
            val exist = sardine.exists(dirPath)
            if (exist) {
                return true
            }
            sardine.createDirectory(dirPath)
            return sardine.exists(dirPath)
        }

        fun uploadFile(
            sardine: OkHttpSardine,
            data: ByteArray,
            path: String,
            webdavUser: WebdavUser?
        ) {
            val filePath = "${webdavUser?.host}${webdavUser?.path}/$path"
            sardine.put(filePath, data)
        }

        fun uploadFile(
            sardine: OkHttpSardine,
            fileContent: String,
            path: String,
            webdavUser: WebdavUser?
        ) {
            val filePath = "${webdavUser?.host}${webdavUser?.path}/$path"
            // 将变量转变为byte字节数组，以传输到网盘
            val data = fileContent.toByteArray()
            sardine.put(filePath, data)
        }

        fun listFiles(
            sardine: OkHttpSardine,
            path: String,
            webdavUser: WebdavUser?
        ): MutableList<DavResource>? {
            val filePath = "${webdavUser?.host}$path"
            return sardine.list(filePath)
        }

        fun downloadFile(sardine: OkHttpSardine, name: String, webdavUser: WebdavUser?): String {
            val filePath = "${webdavUser?.host}$name"
            Log.d("TAG", "download:$name")
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

        fun deleteFile(sardine: OkHttpSardine, name: String, webdavUser: WebdavUser?) {
            val filePath = "${webdavUser?.host}$name"
            sardine.delete(filePath)
        }
    }
}