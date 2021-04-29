package cn.archko.pdf.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.entity.BookProgress
import java.util.ArrayList

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProgress(progress: BookProgress): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: BookProgress): Long

    @Query("SELECT * FROM progress WHERE name == :name and inRecent=:inRecent")
    suspend fun getProgress(name: String, inRecent: Int): BookProgress?

    @Query("SELECT * FROM progress order by lastTimestampe desc")
    suspend fun getAllProgress(name: String, inRecent: Int): BookProgress?

    @Query("SELECT * FROM progress order by lastTimestampe desc limit :start, :count")
    suspend fun getProgresses(start: Int, count: Int): ArrayList<BookProgress>?

    @Query("SELECT * FROM progress where :selection order by lastTimestampe desc limit :start, :count")
    suspend fun getProgresses(start: Int, count: Int, selection: String?): ArrayList<BookProgress>?

    @Query("SELECT count(_id) FROM progress")
    suspend fun progressCount(): Int

    //@Delete
    @Query("Delete FROM progress where name=:name")
    suspend fun deleteProgress(name: String)

    @Delete
    suspend fun deleteProgress(progress: BookProgress)

    //===================== favorite =====================
    @Query("SELECT count(_id) FROM progress where isFavorited=:isFavorited")
    suspend fun getFavoriteProgressCount(isFavorited: Int): Int

}