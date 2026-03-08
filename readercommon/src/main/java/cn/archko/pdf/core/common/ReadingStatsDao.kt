package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.core.entity.ReadingStats

/**
 * 阅读统计数据访问对象
 * @author: archko 2026/3/1
 */
@Dao
public interface ReadingStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertStats(stats: ReadingStats): Long

    @Update
    public suspend fun updateStats(stats: ReadingStats)

    @Query("SELECT * FROM reading_stats WHERE path = :path")
    public suspend fun getStatsByPath(path: String): ReadingStats?

    @Query("SELECT * FROM reading_stats ORDER BY lastReadAt DESC")
    public suspend fun getAllStats(): List<ReadingStats>

    @Query("SELECT SUM(totalReadingTime) FROM reading_stats")
    public suspend fun getTotalReadingTime(): Long?

    @Query("SELECT * FROM reading_stats WHERE lastSessionDate >= :startDate AND lastSessionDate <= :endDate ORDER BY lastReadAt DESC")
    public suspend fun getStatsByDateRange(startDate: String, endDate: String): List<ReadingStats>

    @Query("DELETE FROM reading_stats WHERE path = :path")
    public suspend fun deleteStatsByPath(path: String)

    @Query("DELETE FROM reading_stats")
    public suspend fun deleteAllStats()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAllStats(stats: List<ReadingStats>)
}
