package cn.archko.pdf.fragments

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cn.archko.pdf.core.common.BookProgressParser
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.sardine.SardineHelper
import cn.archko.pdf.core.sardine.WebdavUser
import cn.archko.pdf.core.utils.FileUtils
import com.tencent.mmkv.MMKV
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.Arrays

/**
 * @author: archko 2020/11/16 :11:23
 */
class BackupViewModel : ViewModel() {

    private val progressDao by lazy { Graph.database.progressDao() }
    val sardine = OkHttpSardine()
    var webdavUser: WebdavUser? = null

    private val _uiDavResourceModel = MutableLiveData<List<DavResource>?>()
    val uiDavResourceModel: LiveData<List<DavResource>?>
        get() = _uiDavResourceModel

    private val _uiRestoreModel = MutableLiveData<ResponseHandler<Boolean>>()
    val uiRestoreModel: LiveData<ResponseHandler<Boolean>>
        get() = _uiRestoreModel

    fun backupFiles() = flow {
        try {
            var files: Array<File>? = null
            val dir = FileUtils.getStorageDir("amupdf")
            if (dir.exists()) {
                files = dir.listFiles { pathname: File -> pathname.name.startsWith("mupdf_") }
                if (files != null) {
                    Arrays.sort(files) { f1: File?, f2: File? ->
                        if (f1 == null) throw RuntimeException("f1 is null inside sort")
                        if (f2 == null) throw RuntimeException("f2 is null inside sort")
                        return@sort f2.lastModified().compareTo(f1.lastModified())
                    }
                }
            }
            val list = ArrayList<File>()
            if (files != null) {
                for (f in files) {
                    list.add(f)
                }
            }

            emit(ResponseHandler.Success(list))
        } catch (e: Exception) {
            emit(ResponseHandler.Failure())
        }
    }

    fun backupToWebdav() = flow {
        try {
            if (!checkAndLoadUser()) {
                emit(false)
                return@flow
            }
            val root = JSONObject()
            val list = progressDao.getAllProgress()
            val ja = JSONArray()
            root.put("root", ja)
            //root.put("name", name)
            list?.run {
                for (progress in list) {
                    BookProgressParser.addProgressToJson(progress, ja)
                }
            }
            val content = root.toString()
            Logcat.d("backupToWebdav.content:$content")

            SardineHelper.uploadFile(
                sardine,
                content.toByteArray(),
                SardineHelper.DEFAULT_JSON,
                webdavUser
            )
            emit(true)
        } catch (e: JSONException) {
            emit(false)
            Logcat.e(Logcat.TAG, e.message)
        } catch (e: Exception) {
            emit(false)
            Logcat.e(Logcat.TAG, e.message)
        }
    }

    fun checkAndLoadUser(): Boolean {
        if (null == webdavUser) {
            val mmkv = MMKV.mmkvWithID(SardineHelper.KEY_CONFIG_JSON)
            val content = mmkv.decodeString(SardineHelper.KEY_CONFIG_USER)
            if (!TextUtils.isEmpty(content)) {
                Logcat.d(content)
                val jsonObject = JSONObject(content!!)
                val name = jsonObject.optString(SardineHelper.KEY_NAME)
                val pass = jsonObject.optString(SardineHelper.KEY_PASS)
                val host = jsonObject.optString(SardineHelper.KEY_HOST)
                val path = jsonObject.optString(SardineHelper.KEY_PATH)
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pass)
                    || TextUtils.isEmpty(host) || TextUtils.isEmpty(path)
                ) {
                    return false
                }
                webdavUser = WebdavUser(name, pass, host, path)

                sardine.setCredentials(webdavUser!!.name, webdavUser!!.pass)
                return true
            }
            return false
        }
        return true
    }

    suspend fun restoreFromWebdav(name: String) = flow {
        try {
            if (!checkAndLoadUser()) {
                emit(ResponseHandler.Failure())
                return@flow
            }
            val content = SardineHelper.downloadFile(
                sardine, name,
                webdavUser
            )
            val result = restore(content)
            emit(ResponseHandler.Success(result))
        } catch (e: JSONException) {
            emit(ResponseHandler.Failure())
            e.printStackTrace()
        } catch (e: Exception) {
            emit(ResponseHandler.Failure())
            Logcat.e(Logcat.TAG, e.message)
        }
    }.flowOn(Dispatchers.IO)
        .collectLatest { _uiRestoreModel.value = it }

    suspend fun webdavBackupFiles(path: String) =
        flow {
            if (!checkAndLoadUser()) {
                emit(listOf())
                return@flow
            }
            var list: MutableList<DavResource>? = null
            try {
                list = SardineHelper.listFiles(sardine, path, webdavUser)
            } catch (e: Exception) {
                Logcat.e(Logcat.TAG, e.message)
            }

            emit(list)
        }.flowOn(Dispatchers.IO)
            .collectLatest { _uiDavResourceModel.value = it }

    /**
     * 保存,需要确定path是要可建立目录的权限目录下,坚果云只能在"dav/我的坚果云"路径下才有权限.
     */
    suspend fun saveWebdavUser(name: String, pass: String, host: String, path: String) = flow {
        try {
            val httpSardine = OkHttpSardine()
            httpSardine.setCredentials(name, pass)
            val result = SardineHelper.testPathOrCreate(httpSardine, host, path)
            if (result) {
                val mmkv = MMKV.mmkvWithID(SardineHelper.KEY_CONFIG_JSON)
                val jsonObject = JSONObject()
                jsonObject.put(SardineHelper.KEY_NAME, name)
                jsonObject.put(SardineHelper.KEY_PASS, pass)
                jsonObject.put(SardineHelper.KEY_HOST, host)
                jsonObject.put(SardineHelper.KEY_PATH, path)
                webdavUser = WebdavUser(name, pass, host, path)
                sardine.setCredentials(name, pass)
                mmkv.encode(SardineHelper.KEY_CONFIG_USER, jsonObject.toString())
                emit(true)
            } else {
                emit(false)
            }
        } catch (e: Exception) {
            Logcat.e(e)
            emit(false)
        }
    }

    companion object {
        fun restore(content: String): Boolean {
            try {
                val progresses = BookProgressParser.parseProgresses(content)
                Graph.database.runInTransaction {
                    Graph.database.progressDao().deleteAllProgress()
                    Graph.database.progressDao().addProgresses(progresses)
                }
                return true
            } catch (e: Exception) {
                Logcat.e(Logcat.TAG, e.message)
            }
            return false
        }
    }
}