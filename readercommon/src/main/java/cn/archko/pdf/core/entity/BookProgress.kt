package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.archko.pdf.core.utils.FileUtils
import java.io.File
import java.io.Serializable

/**
 * @author: archko 2014/4/17 :16:27
 */
@Entity(
    tableName = "progress",
)
class BookProgress : Serializable, Comparator<BookProgress> {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var _id: Int = 0

    /**
     * 索引
     */
    @JvmField
    @ColumnInfo(name = "index")
    var index: Int = 0

    /**
     * 文件路径,不是全路径,是除去/sdcard/这部分的路径.
     */
    @JvmField
    @ColumnInfo(name = "path")
    var path: String? = null

    /**
     * 文件名.包含扩展名
     */
    @JvmField
    @ColumnInfo(name = "name")
    var name: String? = null

    @JvmField
    @ColumnInfo(name = "ext")
    var ext: String? = null

    @JvmField
    @ColumnInfo(name = "md5")
    var md5: String? = null

    @JvmField
    @ColumnInfo(name = "page_count")
    var pageCount: Int = 0

    @JvmField
    @ColumnInfo(name = "size")
    var size: Long = 0

    @JvmField
    @ColumnInfo(name = "record_first_timestamp")
    var firstTimestampe: Long = 0

    @JvmField
    @ColumnInfo(name = "record_last_timestamp")
    var lastTimestampe: Long = 0

    @JvmField
    @ColumnInfo(name = "record_read_times")
    var readTimes: Int = 0

    /**
     * 进度0-100,not used
     */
    @JvmField
    @ColumnInfo(name = "record_progress")
    var progress: Int = 0

    @JvmField
    @ColumnInfo(name = "record_page")
    var page: Int = 0

    @JvmField
    @ColumnInfo(name = "record_absolute_zoom_level")
    var zoomLevel = 1000f

    @JvmField
    @ColumnInfo(name = "record_rotation")
    var rotation: Int = 0

    @JvmField
    @ColumnInfo(name = "record_offsetX")
    var offsetX: Int = 0

    @JvmField
    @ColumnInfo(name = "record_offsety")
    var offsetY: Int = 0

    /**
     * 2.5.9 add auto crop,0:autocrop,1:no crop, 2:manunal crop
     */
    @JvmField
    @ColumnInfo(name = "record_autocrop")
    var autoCrop: Int = 0

    /**
     * 3.2.0 add textreflow:0,no reflow mode,1,reflow mode,2 scan pdf reflow mode
     */
    @JvmField
    @ColumnInfo(name = "record_reflow")
    var reflow: Int = 0

    //3.4.0 add isFavorited: 0,not in favorities,1,is in favorities
    @JvmField
    @ColumnInfo(name = "is_favorited")
    var isFavorited = 0

    @JvmField
    @ColumnInfo(name = "is_in_recent")
    var inRecent = 0 //0:in recent,-1:not in recent,-2:all

    // defaule 1,scroll vertical
    @JvmField
    @ColumnInfo(name = "scroll_orientation")
    var scrollOrientation = 1

    constructor(path: String?) {
        index = 0
        this.path = path
        val file = File(FileUtils.getStoragePath(path))
        if (file.exists()) {
            size = file.length()
            ext = FileUtils.getExtension(file)
            name = file.name
        } else {
            size = 0
            name = path
        }
        firstTimestampe = System.currentTimeMillis()
        lastTimestampe = System.currentTimeMillis()
        readTimes = 1
        scrollOrientation = 1
    }

    /*public BookProgress(int _id, int index, String path, String name, String ext, String md5, int pageCount,
                        long size, long firstTimestampe, long lastTimestampe, int readTimes, int progress,
                        int page, float zoomLevel, int rotation, int offsetX, int offsety, int autoCrop, int reflow) {
        this(_id, index, path, name, ext, md5, pageCount, size, firstTimestampe, lastTimestampe,
                readTimes, progress, page, zoomLevel, rotation, offsetX, offsety, autoCrop, reflow, 0, 0);
    }*/
    constructor(
        _id: Int,
        index: Int,
        path: String?,
        name: String?,
        ext: String?,
        md5: String?,
        pageCount: Int,
        size: Long,
        firstTimestampe: Long,
        lastTimestampe: Long,
        readTimes: Int,
        progress: Int,
        page: Int,
        zoomLevel: Float,
        rotation: Int,
        offsetX: Int,
        offsety: Int,
        autoCrop: Int,
        reflow: Int,
        isFavorited: Int,
        inRecent: Int,
        scrollOrientation: Int,
    ) {
        this._id = _id
        this.index = index
        this.path = path
        this.name = name
        this.ext = ext
        this.md5 = md5
        this.pageCount = pageCount
        this.size = size
        this.firstTimestampe = firstTimestampe
        this.lastTimestampe = lastTimestampe
        this.readTimes = readTimes
        this.progress = progress
        this.page = page
        this.zoomLevel = zoomLevel
        this.rotation = rotation
        this.offsetX = offsetX
        offsetY = offsety
        this.autoCrop = autoCrop
        this.reflow = reflow
        this.isFavorited = isFavorited
        this.inRecent = inRecent
        this.scrollOrientation = scrollOrientation
    }

    override fun toString(): String {
        return "BookProgress{" +
                "_id=" + _id +
                ", index=" + index +
                ", name='" + name + '\'' +
                ", pageCount=" + pageCount +
                ", size=" + size +
                ", readTimes=" + readTimes +
                ", progress=" + progress +
                ", page=" + page +
                ", scrollOrientation=" + scrollOrientation +
                ", firstTimestampe=" + firstTimestampe +
                ", lastTimestampe=" + lastTimestampe +
                ", autoCrop=" + autoCrop +
                ", reflow=" + reflow +
                ", isFavorited=" + isFavorited +
                ", inRecent=" + inRecent +
                ", ext='" + ext + '\'' +
                ", md5='" + md5 + '\'' +
                ", path='" + path + '\'' +
                ", zoomLevel=" + zoomLevel +
                ", rotation=" + rotation +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                '}'
    }

    override fun compare(lhs: BookProgress, rhs: BookProgress): Int {
        if (lhs.lastTimestampe > rhs.lastTimestampe) {    //时间大的放前面
            return -1
        } else if (lhs.lastTimestampe < rhs.lastTimestampe) {
            return 1
        }
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookProgress

        if (_id != other._id) return false
        if (index != other.index) return false
        if (path != other.path) return false
        if (name != other.name) return false
        if (ext != other.ext) return false
        if (md5 != other.md5) return false
        if (pageCount != other.pageCount) return false
        if (size != other.size) return false
        if (firstTimestampe != other.firstTimestampe) return false
        if (lastTimestampe != other.lastTimestampe) return false
        if (readTimes != other.readTimes) return false
        if (progress != other.progress) return false
        if (page != other.page) return false
        if (zoomLevel != other.zoomLevel) return false
        if (rotation != other.rotation) return false
        if (offsetX != other.offsetX) return false
        if (offsetY != other.offsetY) return false
        if (autoCrop != other.autoCrop) return false
        if (reflow != other.reflow) return false
        if (isFavorited != other.isFavorited) return false
        if (inRecent != other.inRecent) return false
        if (scrollOrientation != other.scrollOrientation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _id
        result = 31 * result + index
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (ext?.hashCode() ?: 0)
        result = 31 * result + (md5?.hashCode() ?: 0)
        result = 31 * result + pageCount
        result = 31 * result + size.hashCode()
        result = 31 * result + firstTimestampe.hashCode()
        result = 31 * result + lastTimestampe.hashCode()
        result = 31 * result + readTimes
        result = 31 * result + progress
        result = 31 * result + page
        result = 31 * result + zoomLevel.hashCode()
        result = 31 * result + rotation
        result = 31 * result + offsetX
        result = 31 * result + offsetY
        result = 31 * result + autoCrop
        result = 31 * result + reflow
        result = 31 * result + isFavorited
        result = 31 * result + inRecent
        result = 31 * result + scrollOrientation
        return result
    }

    companion object {
        const val IN_RECENT = 0
        const val NOT_IN_RECENT = -1
        const val ALL = -2

        const val REFLOW_NO = 0
        const val REFLOW_TXT = 1
        const val REFLOW_SCAN = 2
    }
}