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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AIProvider

        if (maxTokens != other.maxTokens) return false
        if (temperature != other.temperature) return false
        if (enabled != other.enabled) return false
        if (isDefault != other.isDefault) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (apiKey != other.apiKey) return false
        if (baseUrl != other.baseUrl) return false
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxTokens
        result = 31 * result + temperature.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + isDefault.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + apiKey.hashCode()
        result = 31 * result + baseUrl.hashCode()
        result = 31 * result + model.hashCode()
        return result
    }

}
