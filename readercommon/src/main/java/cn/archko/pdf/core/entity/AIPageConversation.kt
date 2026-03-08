package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 页面对话实体 - 用于存储基于页面内容的单次问答
 * @author: archko 2026/3/2
 */
@Entity(
    tableName = "ai_page_conversation",
    indices = [
        Index(value = ["document_path", "page_index"])
    ]
)
public class AIPageConversation {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public var id: Long = 0

    @ColumnInfo(name = "document_path")
    public var documentPath: String = ""

    @ColumnInfo(name = "document_name")
    public var documentName: String = ""

    @ColumnInfo(name = "page_index")
    public var pageIndex: Int = 0

    @ColumnInfo(name = "question")
    public var question: String = ""

    @ColumnInfo(name = "answer")
    public var answer: String = ""

    @ColumnInfo(name = "page_content")
    public var pageContent: String = ""

    @ColumnInfo(name = "created_at")
    public var createdAt: Long = 0

    public constructor()

    @Ignore
    public constructor(
        documentPath: String,
        documentName: String,
        pageIndex: Int,
        question: String,
        answer: String,
        pageContent: String
    ) {
        this.documentPath = documentPath
        this.documentName = documentName
        this.pageIndex = pageIndex
        this.question = question
        this.answer = answer
        this.pageContent = pageContent
        this.createdAt = System.currentTimeMillis()
    }
}
