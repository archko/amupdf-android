package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 缓存实体
 * @author: archko 2026/3/1
 */
@Entity(
    tableName = "ai_cache",
    indices = [
        Index(value = ["document_path"]),
        Index(value = ["input_hash"]),
        Index(value = ["document_path", "feature_type", "input_hash"], unique = true)
    ]
)
public class AICache {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public var id: Long = 0

    @ColumnInfo(name = "document_path")
    public var documentPath: String = ""

    @ColumnInfo(name = "feature_type")
    public var featureType: String = ""  // summary, qa, analysis

    @ColumnInfo(name = "input_hash")
    public var inputHash: String = ""

    @ColumnInfo(name = "input_text")
    public var inputText: String = ""

    @ColumnInfo(name = "output_text")
    public var outputText: String = ""

    @ColumnInfo(name = "provider_id")
    public var providerId: String = ""

    @ColumnInfo(name = "created_at")
    public var createdAt: Long = 0

    public constructor()

    @Ignore
    public constructor(
        documentPath: String,
        featureType: String,
        inputHash: String,
        inputText: String,
        outputText: String,
        providerId: String
    ) {
        this.documentPath = documentPath
        this.featureType = featureType
        this.inputHash = inputHash
        this.inputText = inputText
        this.outputText = outputText
        this.providerId = providerId
        this.createdAt = System.currentTimeMillis()
    }
}
