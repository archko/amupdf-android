package cn.archko.pdf.common

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import cn.archko.pdf.entity.BookProgress
import java.util.*

/**
 * @author archko
 */
class RecentTableManager(private val context: Context) {
    object ProgressTbl : BaseColumns {
        const val TABLE_NAME = "progress"
        const val KEY_INDEX = "pindex"
        const val KEY_PATH = "path"
        const val KEY_NAME = "name"
        const val KEY_EXT = "ext"
        const val KEY_MD5 = "md5"
        const val KEY_PAGE_COUNT = "page_count"
        const val KEY_SIZE = "size"
        const val KEY_RECORD_FIRST_TIMESTAMP = "record_first_timestamp"
        const val KEY_RECORD_LAST_TIMESTAMP = "record_last_timestamp"
        const val KEY_RECORD_READ_TIMES = "record_read_times"
        const val KEY_RECORD_PROGRESS = "record_progress"
        const val KEY_RECORD_PAGE = "record_page"
        const val KEY_RECORD_ABSOLUTE_ZOOM_LEVEL = "record_absolute_zoom_level"
        const val KEY_RECORD_ROTATION = "record_rotation"
        const val KEY_RECORD_OFFSETX = "record_offsetX"
        const val KEY_RECORD_OFFSETY = "record_offsety"
        const val KEY_RECORD_AUTOCROP = "record_autocrop"
        const val KEY_RECORD_REFLOW = "record_reflow"
        const val KEY_RECORD_IS_FAVORITED = "is_favorited"

        /**
         * 0:in recent list, -1:not in recent list,but it can be showed in favorite list
         */
        const val KEY_RECORD_IS_IN_RECENT = "is_in_recent"
    }

    private val DBHelper: DatabaseHelper
    var db: SQLiteDatabase? = null
        private set

    private class DatabaseHelper internal constructor(context: Context?) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(DATABASE_CREATE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            var oldVersion = oldVersion
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_AUTOCROP + " integer")
                oldVersion = 2
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_REFLOW + " integer")
                oldVersion = 3
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_IS_FAVORITED + " integer")
                oldVersion = 4
            }
            if (oldVersion < 5) {
                db.execSQL("ALTER TABLE " + ProgressTbl.TABLE_NAME + " ADD " + ProgressTbl.KEY_RECORD_IS_IN_RECENT + " integer")
                val sql = StringBuilder(120)
                sql.append("UPDATE ")
                sql.append(ProgressTbl.TABLE_NAME)
                sql.append(" SET ")
                sql.append(ProgressTbl.KEY_RECORD_IS_IN_RECENT)
                sql.append("=0")
                db.execSQL(sql.toString())
                oldVersion = 5
            }
        }
    }

    @Throws(SQLException::class)
    fun open(): RecentTableManager {
        db = DBHelper.writableDatabase
        return this
    }

    fun close() {
        DBHelper.close()
    }

    fun addProgress(progress: BookProgress): Long {
        val cv = ContentValues()
        cv.put(ProgressTbl.KEY_INDEX, progress.index)
        cv.put(ProgressTbl.KEY_PATH, progress.path)
        cv.put(ProgressTbl.KEY_NAME, progress.name)
        cv.put(ProgressTbl.KEY_EXT, progress.ext)
        cv.put(ProgressTbl.KEY_MD5, progress.md5)
        cv.put(ProgressTbl.KEY_PAGE_COUNT, progress.pageCount)
        cv.put(ProgressTbl.KEY_SIZE, progress.size)
        cv.put(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP, progress.firstTimestampe)
        cv.put(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP, progress.lastTimestampe)
        cv.put(ProgressTbl.KEY_RECORD_READ_TIMES, progress.readTimes)
        cv.put(ProgressTbl.KEY_RECORD_PROGRESS, progress.progress)
        cv.put(ProgressTbl.KEY_RECORD_PAGE, progress.page)
        cv.put(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL, progress.zoomLevel)
        cv.put(ProgressTbl.KEY_RECORD_ROTATION, progress.rotation)
        cv.put(ProgressTbl.KEY_RECORD_OFFSETX, progress.offsetX)
        cv.put(ProgressTbl.KEY_RECORD_OFFSETY, progress.offsetY)
        cv.put(ProgressTbl.KEY_RECORD_AUTOCROP, progress.autoCrop)
        cv.put(ProgressTbl.KEY_RECORD_REFLOW, progress.reflow)
        cv.put(ProgressTbl.KEY_RECORD_IS_FAVORITED, progress.isFavorited)
        cv.put(ProgressTbl.KEY_RECORD_IS_IN_RECENT, progress.inRecent)
        return db!!.insert(ProgressTbl.TABLE_NAME, null, cv)
    }

    fun updateProgress(progress: BookProgress): Long {
        val cv = ContentValues()
        cv.put(ProgressTbl.KEY_INDEX, progress.index)
        cv.put(ProgressTbl.KEY_PATH, progress.path)
        cv.put(ProgressTbl.KEY_NAME, progress.name)
        cv.put(ProgressTbl.KEY_EXT, progress.ext)
        cv.put(ProgressTbl.KEY_MD5, progress.md5)
        cv.put(ProgressTbl.KEY_PAGE_COUNT, progress.pageCount)
        cv.put(ProgressTbl.KEY_SIZE, progress.size)
        cv.put(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP, progress.firstTimestampe)
        cv.put(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP, progress.lastTimestampe)
        cv.put(ProgressTbl.KEY_RECORD_READ_TIMES, progress.readTimes)
        cv.put(ProgressTbl.KEY_RECORD_PROGRESS, progress.progress)
        cv.put(ProgressTbl.KEY_RECORD_PAGE, progress.page)
        cv.put(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL, progress.zoomLevel)
        cv.put(ProgressTbl.KEY_RECORD_ROTATION, progress.rotation)
        cv.put(ProgressTbl.KEY_RECORD_OFFSETX, progress.offsetX)
        cv.put(ProgressTbl.KEY_RECORD_OFFSETY, progress.offsetY)
        cv.put(ProgressTbl.KEY_RECORD_AUTOCROP, progress.autoCrop)
        cv.put(ProgressTbl.KEY_RECORD_REFLOW, progress.reflow)
        cv.put(ProgressTbl.KEY_RECORD_IS_FAVORITED, progress.isFavorited)
        cv.put(ProgressTbl.KEY_RECORD_IS_IN_RECENT, progress.inRecent)
        var count: Long = 0
        count = db!!.update(
            ProgressTbl.TABLE_NAME,
            cv,
            ProgressTbl.KEY_NAME + "='" + progress.name + "'",
            null
        ).toLong()
        return count
    }

    /**
     * get bookprogress by inRecent
     *
     * @param name
     * @param inRecent
     * @return
     */
    fun getProgress(name: String, inRecent: Int): BookProgress? {
        var entry: BookProgress? = null
        var cur: Cursor? = null
        try {
            var selection = (ProgressTbl.KEY_NAME + "=? and "
                    + ProgressTbl.KEY_RECORD_IS_IN_RECENT + "='" + inRecent + "'")
            if (inRecent == BookProgress.ALL) {
                selection = ProgressTbl.KEY_NAME + "=?"
            }
            cur = db!!.query(
                true, ProgressTbl.TABLE_NAME, null,
                selection, arrayOf(name),
                null, null, null, "1"
            )
            if (cur != null) {
                if (cur.moveToFirst()) {
                    entry = fillProgress(cur)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cur?.close()
        }
        return entry
    }

    private fun fillProgress(cur: Cursor): BookProgress {
        val entry: BookProgress
        val _id = cur.getInt(cur.getColumnIndex(BaseColumns._ID))
        val index = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_INDEX))
        val path = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_PATH))
        val name = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_NAME))
        val ext = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_EXT))
        val md5 = cur.getString(cur.getColumnIndex(ProgressTbl.KEY_MD5))
        val pageCount = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_PAGE_COUNT))
        val size = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_SIZE))
        val firstT = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP))
        val lastT = cur.getLong(cur.getColumnIndex(ProgressTbl.KEY_RECORD_LAST_TIMESTAMP))
        val readTime = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_READ_TIMES))
        val progress = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_PROGRESS))
        val page = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_PAGE))
        val zoomLevel = cur.getFloat(cur.getColumnIndex(ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL))
        val rotation = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_ROTATION))
        val offsetX = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_OFFSETX))
        val offsetY = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_OFFSETY))
        val autoCrop = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_AUTOCROP))
        val reflow = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_REFLOW))
        val isFavorited = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_IS_FAVORITED))
        val inRecent = cur.getInt(cur.getColumnIndex(ProgressTbl.KEY_RECORD_IS_IN_RECENT))
        entry = BookProgress(
            _id,
            index,
            path,
            name,
            ext,
            md5,
            pageCount,
            size,
            firstT,
            lastT,
            readTime,
            progress,
            page,
            zoomLevel,
            rotation,
            offsetX,
            offsetY,
            autoCrop,
            reflow,
            isFavorited,
            inRecent
        )
        return entry
    }

    //Collections.sort(list);
    val progresses: ArrayList<BookProgress?>?
        get() {
            var list: ArrayList<BookProgress?>? = null
            var cur: Cursor? = null
            try {
                cur = db!!.query(
                    ProgressTbl.TABLE_NAME, null,
                    null, null, null, null, ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " desc"
                )
                if (cur != null) {
                    list = ArrayList()
                    if (cur.moveToFirst()) {
                        do {
                            list.add(fillProgress(cur))
                        } while (cur.moveToNext())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cur?.close()
            }

            //Collections.sort(list);
            return list
        }

    fun getProgresses(start: Int, count: Int): ArrayList<BookProgress>? {
        return getProgresses(start, count, null)
    }

    fun getProgresses(start: Int, count: Int, selection: String?): ArrayList<BookProgress>? {
        var list: ArrayList<BookProgress>? = null
        var cur: Cursor? = null
        try {
            cur = db!!.query(
                ProgressTbl.TABLE_NAME,
                null,
                selection,
                null,
                null,
                null,
                ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " desc",
                "$start , $count"
            )
            if (cur != null) {
                list = ArrayList()
                if (cur.moveToFirst()) {
                    do {
                        list.add(fillProgress(cur))
                    } while (cur.moveToNext())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cur?.close()
        }

        //Collections.sort(list);
        return list
    }

    val progressCount: Int
        get() {
            var cur: Cursor? = null
            try {
                cur = db!!.query(
                    ProgressTbl.TABLE_NAME, arrayOf("_id"),
                    null, null, null, null, null
                )
                if (cur != null && cur.count > 0) {
                    return cur.count
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cur?.close()
            }
            return 0
        }

    fun deleteProgress(name: String) {
        db!!.delete(ProgressTbl.TABLE_NAME, ProgressTbl.KEY_NAME + "='" + name + "'", null)
    }

    fun deleteProgress(progress: BookProgress) {
        db!!.delete(ProgressTbl.TABLE_NAME, ProgressTbl.KEY_NAME + "='" + progress.name + "'", null)
    }

    //===================== favorite =====================
    fun getFavoriteProgressCount(isFavorited: Int): Int {
        var cur: Cursor? = null
        try {
            cur = db!!.query(
                ProgressTbl.TABLE_NAME,
                arrayOf("_id"),
                ProgressTbl.KEY_RECORD_IS_FAVORITED + "='" + isFavorited + "'",
                null,
                null,
                null,
                null
            )
            if (cur != null && cur.count > 0) {
                return cur.count
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cur?.close()
        }
        return 0
    }

    companion object {
        /**
         * version:4,add KEY_RECORD_IS_FAVORITED
         * version:5,add KEY_RECORD_IS_IN_RECENT
         */
        private const val DB_VERSION = 5
        private const val DB_NAME = "book_progress.db"
        private const val DATABASE_CREATE = ("create table "
                + ProgressTbl.TABLE_NAME
                + "(" + BaseColumns._ID + " integer primary key autoincrement,"
                + "" + ProgressTbl.KEY_INDEX + " integer,"
                + "" + ProgressTbl.KEY_PATH + " text ,"
                + "" + ProgressTbl.KEY_NAME + " text ,"
                + "" + ProgressTbl.KEY_EXT + " text ,"
                + "" + ProgressTbl.KEY_MD5 + " text ,"
                + "" + ProgressTbl.KEY_PAGE_COUNT + " text ,"
                + "" + ProgressTbl.KEY_SIZE + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_FIRST_TIMESTAMP + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_LAST_TIMESTAMP + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_READ_TIMES + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_PROGRESS + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_PAGE + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_ABSOLUTE_ZOOM_LEVEL + " real ,"
                + "" + ProgressTbl.KEY_RECORD_ROTATION + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_OFFSETX + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_OFFSETY + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_AUTOCROP + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_REFLOW + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_IS_FAVORITED + " integer ,"
                + "" + ProgressTbl.KEY_RECORD_IS_IN_RECENT + " integer "
                + ");")
    }

    init {
        DBHelper = DatabaseHelper(context)
    }
}