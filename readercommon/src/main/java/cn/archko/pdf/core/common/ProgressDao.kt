package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.entity.Booknote

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addProgress(progress: BookProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addProgresses(progress: List<BookProgress>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateProgress(progress: BookProgress)

    @Query("SELECT * FROM progress WHERE name = :name")
    fun getProgress(name: String): BookProgress?

    @Query("SELECT * FROM progress WHERE name = :name and is_in_recent = :inRecent")
    fun getProgress(name: String, inRecent: Int): BookProgress?

    @Query("SELECT * FROM progress order by record_last_timestamp desc")
    fun getAllProgress(): List<BookProgress>?

    @Query("SELECT * FROM progress order by record_last_timestamp desc limit :start, :count")
    fun getProgresses(start: Int, count: Int): List<BookProgress>?

    @Query("SELECT * FROM progress where is_in_recent = :is_in_recent order by record_last_timestamp desc limit :start, :count")
    fun getProgresses(start: Int, count: Int, is_in_recent: Int): List<BookProgress>?

    @Query("SELECT * FROM progress where is_favorited = :is_favorited order by record_last_timestamp desc limit :start, :count")
    fun getFavoriteProgresses(
        start: Int,
        count: Int,
        is_favorited: String
    ): List<BookProgress>?

    @Query("SELECT count(_id) FROM progress")
    fun progressCount(): Int

    //@Delete
    @Query("Delete FROM progress where name = :name")
    fun deleteProgress(name: String) :Int

    //@Delete
    @Query("Delete FROM progress")
    fun deleteAllProgress()

    @Delete
    fun deleteProgress(progress: BookProgress)

    //===================== favorite =====================
    @Query("SELECT count(_id) FROM progress where is_favorited = :isFavorited")
    fun getFavoriteProgressCount(isFavorited: Int): Int

    @Query("SELECT * FROM progress where is_in_recent='0' and name like '%' || :keyword || '%' order by record_last_timestamp desc ")
    fun searchHistory(keyword: String): List<BookProgress>?

    @Query("SELECT * FROM progress where is_favorited='1' and name like '%' || :keyword || '%' order by record_last_timestamp desc ")
    fun searchFavorite(keyword: String): List<BookProgress>?

    //===================== book note =====================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBooknote(booknote: Booknote): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBooknotes(progress: List<Booknote>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateBooknote(booknote: Booknote)

    @Query("SELECT * FROM booknote WHERE path = :path order by page")
    fun getBooknote(path: String): List<Booknote>?

    @Query("SELECT * FROM booknote WHERE progress_id = :progressId order by page")
    fun getBooknote(progressId: Int): List<Booknote>?

    @Query("Delete FROM booknote where _id = :noteid")
    fun deleteBooknote(noteid: Int)

    @Query("Delete FROM booknote where progress_id = :progressId")
    fun deleteBooknotesByProgress(progressId: Int)

    //===================== book mark =====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBookmark(bookmark: Bookmark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBookmarks(progress: List<Bookmark>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmark WHERE path = :path order by page")
    fun getBookmark(path: String): List<Bookmark>?

    @Query("SELECT * FROM bookmark WHERE progress_id = :progressId order by page")
    fun getBookmark(progressId: Int): List<Bookmark>?

    @Query("Delete FROM bookmark where _id = :noteid")
    fun deleteBookmark(noteid: Int)

    @Query("Delete FROM bookmark where progress_id = :progressId")
    fun deleteBookmarksByProgress(progressId: Int)
}