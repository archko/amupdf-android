package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.core.entity.ABookmark

/**
 * 书签数据访问对象
 * @author: archko 2026/3/1
 */
@Dao
public interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertBookmark(a: ABookmark): Long

    @Update
    public suspend fun updateBookmark(abookmark: ABookmark)

    @Delete
    public suspend fun deleteBookmark(abookmark: ABookmark)

    @Query("SELECT * FROM abookmark WHERE path = :path ORDER BY pageIndex ASC")
    public suspend fun getBookmarksByPath(path: String): List<ABookmark>

    @Query("SELECT * FROM abookmark ORDER BY updateAt DESC")
    public suspend fun getAllBookmarks(): List<ABookmark>

    @Query("SELECT * FROM abookmark WHERE path = :path AND pageIndex = :pageIndex LIMIT 1")
    public suspend fun getBookmarkByPageAndPath(path: String, pageIndex: Int): ABookmark?

    @Query("SELECT COUNT(*) FROM abookmark WHERE path = :path")
    public suspend fun getBookmarkCountByPath(path: String): Int

    @Query("DELETE FROM abookmark WHERE path = :path")
    public suspend fun deleteBookmarksByPath(path: String)

    @Query("DELETE FROM abookmark")
    public suspend fun deleteAllBookmarks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAllBookmarks(bookmarks: List<ABookmark>)
}
