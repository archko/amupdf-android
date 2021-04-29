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
    fun addProgress(progress: BookProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addProgresses(progress: List<BookProgress>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateProgress(progress: BookProgress)

    @Query("SELECT * FROM progress WHERE name == :name")
    fun getProgress(name: String): BookProgress?

    @Query("SELECT * FROM progress WHERE name == :name and is_in_recent=:inRecent")
    fun getProgress(name: String, inRecent: Int): BookProgress?

    @Query("SELECT * FROM progress order by record_last_timestamp desc")
    fun getAllProgress(): List<BookProgress>?

    @Query("SELECT * FROM progress order by record_last_timestamp desc limit :start, :count")
    fun getProgresses(start: Int, count: Int): List<BookProgress>?

    @Query("SELECT * FROM progress where is_in_recent=:is_in_recent order by record_last_timestamp desc limit :start, :count")
    fun getProgresses(start: Int, count: Int, is_in_recent: Int): List<BookProgress>?

    @Query("SELECT * FROM progress where is_favorited=:is_favorited order by record_last_timestamp desc limit :start, :count")
    fun getFavoriteProgresses(
        start: Int,
        count: Int,
        is_favorited: String
    ): List<BookProgress>?

    @Query("SELECT count(_id) FROM progress")
    fun progressCount(): Int

    //@Delete
    @Query("Delete FROM progress where name=:name")
    fun deleteProgress(name: String)

    //@Delete
    @Query("Delete FROM progress")
    fun deleteAllProgress()

    @Delete
    fun deleteProgress(progress: BookProgress)

    //===================== favorite =====================
    @Query("SELECT count(_id) FROM progress where is_favorited=:isFavorited")
    fun getFavoriteProgressCount(isFavorited: Int): Int

}