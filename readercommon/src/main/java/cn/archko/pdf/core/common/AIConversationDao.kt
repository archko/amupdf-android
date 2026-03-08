package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.archko.pdf.core.entity.AIConversation

/**
 * AI 对话历史数据访问对象
 * @author: archko 2026/3/1
 */
@Dao
public interface AIConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertConversation(conversation: AIConversation): Long

    @Delete
    public suspend fun deleteConversation(conversation: AIConversation)

    @Query("SELECT * FROM ai_conversation WHERE session_id = :sessionId ORDER BY created_at ASC")
    public suspend fun getConversationsBySession(sessionId: String): List<AIConversation>

    @Query("SELECT * FROM ai_conversation WHERE document_path = :documentPath ORDER BY created_at DESC")
    public suspend fun getConversationsByDocument(documentPath: String): List<AIConversation>

    @Query("SELECT DISTINCT session_id FROM ai_conversation WHERE document_path = :documentPath ORDER BY created_at DESC")
    public suspend fun getSessionsByDocument(documentPath: String): List<String>

    @Query("DELETE FROM ai_conversation WHERE session_id = :sessionId")
    public suspend fun deleteConversationsBySession(sessionId: String)

    @Query("DELETE FROM ai_conversation WHERE document_path = :documentPath")
    public suspend fun deleteConversationsByDocument(documentPath: String)

    @Query("DELETE FROM ai_conversation WHERE created_at < :timestamp")
    public suspend fun deleteExpiredConversations(timestamp: Long)

    @Query("DELETE FROM ai_conversation")
    public suspend fun deleteAllConversations()

    @Query("SELECT COUNT(*) FROM ai_conversation WHERE session_id = :sessionId")
    public suspend fun getConversationCount(sessionId: String): Int
}
