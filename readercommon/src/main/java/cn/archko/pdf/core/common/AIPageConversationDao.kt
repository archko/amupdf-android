package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.archko.pdf.core.entity.AIPageConversation

/**
 * AI页面对话数据访问对象
 * @author: archko 2026/3/2
 */
@Dao
public interface AIPageConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertConversation(conversation: AIPageConversation): Long

    @Delete
    public suspend fun deleteConversation(conversation: AIPageConversation)

    @Query("SELECT * FROM ai_page_conversation WHERE document_path = :path AND page_index = :pageIndex ORDER BY created_at DESC")
    public suspend fun getConversationsByPage(
        path: String,
        pageIndex: Int
    ): List<AIPageConversation>

    @Query("SELECT * FROM ai_page_conversation WHERE document_path = :path ORDER BY page_index ASC, created_at DESC")
    public suspend fun getConversationsByDocument(path: String): List<AIPageConversation>

    @Query("DELETE FROM ai_page_conversation WHERE document_path = :path")
    public suspend fun deleteConversationsByPath(path: String)

    @Query("DELETE FROM ai_page_conversation")
    public suspend fun deleteAllConversations()
}
