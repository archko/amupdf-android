package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * AI 提供商配置实体
 * @author: archko 2026/3/1
 */
@Entity(tableName = "ai_provider")
public class AIProvider {
    @PrimaryKey
    @ColumnInfo(name = "id")
    public var id: String = ""  // deepseek, qwen, glm

    @ColumnInfo(name = "name")
    public var name: String = ""

    @ColumnInfo(name = "api_key")
    public var apiKey: String = ""

    @ColumnInfo(name = "base_url")
    public var baseUrl: String = ""

    @ColumnInfo(name = "model")
    public var model: String = ""

    @ColumnInfo(name = "max_tokens")
    public var maxTokens: Int = 2000

    @ColumnInfo(name = "temperature")
    public var temperature: Float = 0.7f

    @ColumnInfo(name = "enabled")
    public var enabled: Boolean = true

    @ColumnInfo(name = "is_default")
    public var isDefault: Boolean = false

    @ColumnInfo(name = "created_at")
    public var createdAt: Long = 0

    @ColumnInfo(name = "updated_at")
    public var updatedAt: Long = 0

    public constructor()

    @Ignore
    public constructor(
        id: String,
        name: String,
        apiKey: String,
        baseUrl: String,
        model: String,
        maxTokens: Int = 2000,
        temperature: Float = 0.7f,
        enabled: Boolean = true,
        isDefault: Boolean = false
    ) {
        this.id = id
        this.name = name
        this.apiKey = apiKey
        this.baseUrl = baseUrl
        this.model = model
        this.maxTokens = maxTokens
        this.temperature = temperature
        this.enabled = enabled
        this.isDefault = isDefault
        this.createdAt = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }
}
