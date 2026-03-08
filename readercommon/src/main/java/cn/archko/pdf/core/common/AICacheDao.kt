package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cn.archko.pdf.core.entity.AICache

/**
 * AI 缓存数据访问对象
 * @author: archko 2026/3/1
 */
@Dao
public interface AICacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertCache(cache: AICache): Long

    @Delete
    public suspend fun deleteCache(cache: AICache)

    @Query("SELECT * FROM ai_cache WHERE document_path = :documentPath AND feature_type = :featureType AND input_hash = :inputHash LIMIT 1")
    public suspend fun getCacheByHash(
        documentPath: String,
        featureType: String,
        inputHash: String
    ): AICache?

    @Query("SELECT * FROM ai_cache WHERE document_path = :documentPath ORDER BY created_at DESC")
    public suspend fun getCachesByDocument(documentPath: String): List<AICache>

    @Query("SELECT * FROM ai_cache WHERE feature_type = :featureType ORDER BY created_at DESC LIMIT :limit")
    public suspend fun getCachesByType(featureType: String, limit: Int = 50): List<AICache>

    @Query("DELETE FROM ai_cache WHERE created_at < :timestamp")
    public suspend fun deleteExpiredCache(timestamp: Long)

    @Query("DELETE FROM ai_cache WHERE document_path = :documentPath")
    public suspend fun deleteCachesByDocument(documentPath: String)

    @Query("DELETE FROM ai_cache")
    public suspend fun deleteAllCache()

    @Query("SELECT COUNT(*) FROM ai_cache")
    public suspend fun getCacheCount(): Int
}
