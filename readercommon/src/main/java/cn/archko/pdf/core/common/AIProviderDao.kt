package cn.archko.pdf.core.common

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.archko.pdf.core.entity.AIProvider

/**
 * AI 提供商数据访问对象
 * @author: archko 2026/3/1
 */
@Dao
public interface AIProviderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertProvider(provider: AIProvider): Long

    @Update
    public suspend fun updateProvider(provider: AIProvider)

    @Delete
    public suspend fun deleteProvider(provider: AIProvider)

    @Query("SELECT * FROM ai_provider")
    public suspend fun getAllProviders(): List<AIProvider>

    @Query("SELECT * FROM ai_provider WHERE id = :id LIMIT 1")
    public suspend fun getProviderById(id: String): AIProvider?

    @Query("SELECT * FROM ai_provider WHERE is_default = 1 AND enabled = 1 LIMIT 1")
    public suspend fun getDefaultProvider(): AIProvider?

    @Query("SELECT * FROM ai_provider WHERE enabled = 1")
    public suspend fun getEnabledProviders(): List<AIProvider>

    @Query("UPDATE ai_provider SET is_default = 0")
    public suspend fun clearAllDefaults()

    @Query("UPDATE ai_provider SET is_default = 1 WHERE id = :id")
    public suspend fun setDefault(id: String)

    @Query("DELETE FROM ai_provider")
    public suspend fun deleteAllProviders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAllProviders(providers: List<AIProvider>)
}
