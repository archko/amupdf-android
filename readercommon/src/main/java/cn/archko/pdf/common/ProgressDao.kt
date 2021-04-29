package cn.archko.pdf.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.entity.BookProgress

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProgress(progress: BookProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addProgresses(progress: List<BookProgress>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: BookProgress)

    @Query("SELECT * FROM progress WHERE name == :name and is_in_recent=:inRecent")
    suspend fun getProgress(name: String, inRecent: Int): BookProgress

    @Query("SELECT * FROM progress order by record_last_timestamp desc")
    suspend fun getAllProgress(): BookProgress

    @Query("SELECT * FROM progress order by record_last_timestamp desc limit :start, :count")
    suspend fun getProgresses(start: Int, count: Int): List<BookProgress>

    @Query("SELECT * FROM progress where :selection order by record_last_timestamp desc limit :start, :count")
    suspend fun getProgresses(start: Int, count: Int, selection: String?): List<BookProgress>

    @Query("SELECT count(_id) FROM progress")
    suspend fun progressCount(): Int

    //@Delete
    @Query("Delete FROM progress where name=:name")
    suspend fun deleteProgress(name: String)

    //@Delete
    @Query("Delete FROM progress")
    fun deleteAllProgress()

    @Delete
    suspend fun deleteProgress(progress: BookProgress)

    //===================== favorite =====================
    @Query("SELECT count(_id) FROM progress where is_favorited=:isFavorited")
    suspend fun getFavoriteProgressCount(isFavorited: Int): Int

}