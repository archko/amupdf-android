package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * 阅读统计实体
 * @author: archko 2026/3/1
 */
@Entity(tableName = "reading_stats")
public class ReadingStats {
    @PrimaryKey
    @ColumnInfo(name = "path")
    public var path: String = ""

    @ColumnInfo(name = "totalReadingTime")
    public var totalReadingTime: Long = 0  // 总阅读时长（秒）

    @ColumnInfo(name = "lastSessionTime")
    public var lastSessionTime: Long = 0   // 上次阅读时长

    @ColumnInfo(name = "averageSessionTime")
    public var averageSessionTime: Long = 0  // 平均每次阅读时长

    @ColumnInfo(name = "firstReadAt")
    public var firstReadAt: Long = 0       // 首次阅读时间

    @ColumnInfo(name = "lastReadAt")
    public var lastReadAt: Long = 0        // 最后阅读时间

    @ColumnInfo(name = "completedPages")
    public var completedPages: Int = 0     // 已读完的页数

    @ColumnInfo(name = "totalPages")
    public var totalPages: Int = 0         // 总页数

    @ColumnInfo(name = "sessionCount")
    public var sessionCount: Int = 0       // 阅读会话数

    @ColumnInfo(name = "lastSessionDate")
    public var lastSessionDate: String = "" // 最后会话日期（yyyy-MM-dd）

    @ColumnInfo(name = "consecutiveDays")
    public var consecutiveDays: Int = 0    // 连续阅读天数

    @ColumnInfo(name = "annotationCount")
    public var annotationCount: Int = 0    // 批注数量

    @ColumnInfo(name = "bookmarkCount")
    public var bookmarkCount: Int = 0      // 书签数量

    public constructor()

    @Ignore
    public constructor(
        path: String,
        totalPages: Int
    ) {
        this.path = path
        this.totalPages = totalPages
        this.firstReadAt = System.currentTimeMillis()
        this.lastReadAt = System.currentTimeMillis()
        this.lastSessionDate = getCurrentDate()
        this.consecutiveDays = 1
    }

    private fun getCurrentDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    override fun toString(): String {
        return "ReadingStats(path='$path', totalReadingTime=$totalReadingTime, sessionCount=$sessionCount, completedPages=$completedPages/$totalPages, consecutiveDays=$consecutiveDays)"
    }
    
    public fun copy(): ReadingStats {
        return ReadingStats().apply {
            this.path = this@ReadingStats.path
            this.totalReadingTime = this@ReadingStats.totalReadingTime
            this.lastSessionTime = this@ReadingStats.lastSessionTime
            this.averageSessionTime = this@ReadingStats.averageSessionTime
            this.firstReadAt = this@ReadingStats.firstReadAt
            this.lastReadAt = this@ReadingStats.lastReadAt
            this.completedPages = this@ReadingStats.completedPages
            this.totalPages = this@ReadingStats.totalPages
            this.sessionCount = this@ReadingStats.sessionCount
            this.lastSessionDate = this@ReadingStats.lastSessionDate
            this.consecutiveDays = this@ReadingStats.consecutiveDays
            this.annotationCount = this@ReadingStats.annotationCount
            this.bookmarkCount = this@ReadingStats.bookmarkCount
        }
    }
}
