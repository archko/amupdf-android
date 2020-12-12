package cn.archko.pdf.common

import android.text.TextUtils
import cn.archko.pdf.App
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.common.BookProgressParser.addProgressToJson
import cn.archko.pdf.common.BookProgressParser.parseProgresses
import cn.archko.pdf.common.Logcat.d
import cn.archko.pdf.common.Logcat.longLog
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.DateUtils
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.StreamUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * 存储最近阅读的记录
 *
 * @author: archko 2014/4/17 :15:05
 */
class RecentManager private constructor() {
    val recentTableManager: RecentTableManager

    private object Factory {
        val instance = RecentManager()
    }

    //------------------- operation of db -------------------
    fun addAsyncToDB(progress: BookProgress?, dataListener: DataListener?) {
        AppExecutors.instance.diskIO().execute(Runnable {
            addToDb(progress)
            dataListener?.onSuccess()
        })
    }

    fun addToDb(progress: BookProgress?) {
        if (null == progress || TextUtils.isEmpty(progress.path) || TextUtils.isEmpty(progress.name)) {
            d("", "path is null.$progress")
            return
        }
        try {
            val filepath = FileUtils.getStoragePath(progress.path)
            val file = File(filepath)
            var old = recentTableManager.getProgress(file.name, BookProgress.ALL)
            if (old == null) {
                old = progress
                old.lastTimestampe = System.currentTimeMillis()
                recentTableManager.addProgress(old)
            } else {
                progress.lastTimestampe = System.currentTimeMillis()
                progress.isFavorited = old.isFavorited
                recentTableManager.updateProgress(progress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteFromDb(absolutePath: String): ArrayList<BookProgress>? {
        d(TAG, "remove:$absolutePath")
        if (TextUtils.isEmpty(absolutePath)) {
            d("", "path is null.")
            return null
        }
        try {
            val file = File(absolutePath)
            recentTableManager.deleteProgress(file.name)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * only remove progress,not isfavorited
     *
     * @param absolutePath
     * @return
     */
    fun removeRecentFromDb(absolutePath: String): ArrayList<BookProgress>? {
        d(TAG, "removeRecentFromDb:$absolutePath")
        if (TextUtils.isEmpty(absolutePath)) {
            d("", "path is null.")
            return null
        }
        try {
            val file = File(absolutePath)
            val bookProgress = recentTableManager.getProgress(file.name, BookProgress.ALL)
            bookProgress?.run {
                firstTimestampe = 0
                page = 0
                progress = 0
                inRecent = -1
                return@run recentTableManager.updateProgress(bookProgress)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    @JvmOverloads
    fun readRecentFromDb(
        absolutePath: String?,
        recent: Int = BookProgress.IN_RECENT
    ): BookProgress? {
        var progress: BookProgress? = null
        try {
            val file = File(absolutePath)
            progress = recentTableManager.getProgress(file.name, recent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return progress
    }

    fun readRecentFromDb(start: Int, count: Int): ArrayList<BookProgress>? {
        var list: ArrayList<BookProgress>? = null
        try {
            list = recentTableManager.getProgresses(
                start,
                count,
                RecentTableManager.ProgressTbl.KEY_RECORD_IS_IN_RECENT + "='0'"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    val progressCount: Int
        get() {
            try {
                return recentTableManager.progressCount
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        }

    fun backupFromDb(): String? {
        val name =
            "mupdf_" + DateUtils.formatTime(System.currentTimeMillis(), "yyyy-MM-dd-HH-mm-ss")
        return backupFromDb(name)
    }

    fun backupFromDb(name: String): String? {
        try {
            val list = recentTableManager.progresses
            val root = JSONObject()
            val ja = JSONArray()
            root.put("root", ja)
            root.put("name", name)
            list?.run {
                for (progress in list) {
                    addProgressToJson(progress!!, ja)
                }
            }
            val dir = FileUtils.getStorageDir("amupdf")
            if (dir != null && dir.exists()) {
                val filePath = dir.absolutePath + File.separator + name
                d(TAG, "backup.name:$filePath root:$root")
                StreamUtils.copyStringToFile(root.toString(), filePath)
            }
            return name
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun restoreToDb(absolutePath: String?): Boolean {
        return restoreToDb(File(absolutePath))
    }

    fun restoreToDb(file: File): Boolean {
        var flag = false
        try {
            val content = StreamUtils.readStringFromFile(file)
            longLog(TAG, "restore.file:" + file.absolutePath + " content:" + content)
            val progresses = parseProgresses(content)
            try {
                recentTableManager.db?.beginTransaction()
                recentTableManager.db?.delete(RecentTableManager.ProgressTbl.TABLE_NAME, null, null)
                for (progress in progresses) {
                    if (!TextUtils.isEmpty(progress.name)) {
                        recentTableManager.addProgress(progress)
                        //Logcat.d(TAG, "add progress:" + progress);
                    }
                }
                flag = true
                recentTableManager.db?.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                recentTableManager.db?.endTransaction()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return flag
    }

    val backupFile: File?
        get() {
            val dir = FileUtils.getStorageDir("amupdf")
            if (!dir.exists()) {
                return null
            }
            var file: File? = null
            try {
                val files = dir.listFiles { pathname -> pathname.name.startsWith("mupdf_") }
                if (files.size > 0) {
                    Arrays.sort(files) { f1: File?, f2: File? ->
                        if (f1 == null) throw RuntimeException("f1 is null inside sort")
                        if (f2 == null) throw RuntimeException("f2 is null inside sort")
                        try {
                            return@sort (f2.lastModified() - f1.lastModified()).toInt()
                        } catch (e: NullPointerException) {
                            throw RuntimeException("failed to compare $f1 and $f2", e)
                        }
                    }
                    file = files[0]
                }
            } catch (e: NullPointerException) {
                throw RuntimeException("failed to sort file list " + " for path ", e)
            }
            return file
        }
    val backupFiles: List<File>?
        get() {
            val dir = FileUtils.getStorageDir("amupdf")
            if (!dir.exists()) {
                return null
            }
            val files = dir.listFiles { pathname: File -> pathname.name.startsWith("mupdf_") }
            Arrays.sort(files) { f1: File?, f2: File? ->
                if (f1 == null) throw RuntimeException("f1 is null inside sort")
                if (f2 == null) throw RuntimeException("f2 is null inside sort")
                try {
                    return@sort (f2.lastModified() - f1.lastModified()).toInt()
                } catch (e: NullPointerException) {
                    throw RuntimeException("failed to compare $f1 and $f2", e)
                }
            }
            return Arrays.asList(*files)
        }

    //===================== favorite =====================
    fun readFavoriteFromDb(start: Int, count: Int): ArrayList<BookProgress>? {
        var list: ArrayList<BookProgress>? = null
        try {
            list = recentTableManager.getProgresses(
                start,
                count,
                RecentTableManager.ProgressTbl.KEY_RECORD_IS_FAVORITED + "='1'"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    val favoriteProgressCount: Int
        get() {
            try {
                return recentTableManager.getFavoriteProgressCount(1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return 0
        }

    companion object {
        const val TAG = "RecentManager"
        val instance: RecentManager
            get() = Factory.instance
    }

    init {
        recentTableManager = RecentTableManager(App.instance!!)
    }
}