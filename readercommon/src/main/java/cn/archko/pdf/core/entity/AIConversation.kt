package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 对话历史实体
 * @author: archko 2026/3/1
 */
@Entity(
    tableName = "ai_conversation",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["document_path"])
    ]
)
public class AIConversation {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public var id: Long = 0

    @ColumnInfo(name = "document_path")
    public var documentPath: String = ""

    @ColumnInfo(name = "session_id")
    public var sessionId: String = ""

    @ColumnInfo(name = "role")
    public var role: String = ""  // user, assistant

    @ColumnInfo(name = "content")
    public var content: String = ""

    @ColumnInfo(name = "context_type")
    public var contextType: String? = null  // selection, page, document

    @ColumnInfo(name = "created_at")
    public var createdAt: Long = 0

    public constructor()

    @Ignore
    public constructor(
        documentPath: String,
        sessionId: String,
        role: String,
        content: String,
        contextType: String? = null
    ) {
        this.documentPath = documentPath
        this.sessionId = sessionId
        this.role = role
        this.content = content
        this.contextType = contextType
        this.createdAt = System.currentTimeMillis()
    }
}
