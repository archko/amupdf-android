package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书签实体
 * @author: archko 2026/3/1
 */
@Entity(
    tableName = "abookmark",
    indices = [
        Index(value = ["path"]),
        Index(value = ["path", "pageIndex"])
    ]
)
public class ABookmark {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public var id: Long = 0

    @ColumnInfo(name = "path")
    public var path: String = ""

    @ColumnInfo(name = "pageIndex")
    public var pageIndex: Int = 0

    @ColumnInfo(name = "title")
    public var title: String? = null

    @ColumnInfo(name = "note")
    public var note: String? = null

    @ColumnInfo(name = "createAt")
    public var createAt: Long = 0

    @ColumnInfo(name = "updateAt")
    public var updateAt: Long = 0

    @ColumnInfo(name = "color")
    public var color: Long? = null

    @ColumnInfo(name = "scrollY")
    public var scrollY: Long? = null

    public constructor()

    @Ignore
    public constructor(
        path: String,
        pageIndex: Int,
        title: String? = null,
        note: String? = null,
        color: Long? = null,
        scrollY: Long? = null
    ) {
        this.path = path
        this.pageIndex = pageIndex
        this.title = title
        this.note = note
        this.color = color
        this.scrollY = scrollY
        this.createAt = System.currentTimeMillis()
        this.updateAt = System.currentTimeMillis()
    }

    override fun toString(): String {
        return "Bookmark(id=$id, path='$path', pageIndex=$pageIndex, title=$title, note=$note, createAt=$createAt, updateAt=$updateAt, color=$color, scrollY=$scrollY)"
    }
}
