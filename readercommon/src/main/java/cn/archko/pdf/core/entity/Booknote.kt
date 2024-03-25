package cn.archko.pdf.core.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author: archko 2022/4/17 :16:27
 */
@Entity(
    tableName = "booknote",
)
class Booknote : Serializable, Comparator<Booknote> {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var _id: Int = 0

    /**
     * 进度的id
     */
    @JvmField
    @ColumnInfo(name = "progress_id")
    var progressId: Int = 0

    /**
     * 文件路径,不是全路径,是除去/sdcard/这部分的路径.与bookprogress一致
     */
    @JvmField
    @ColumnInfo(name = "path")
    var path: String? = null

    /**
     * 页码
     */
    @JvmField
    @ColumnInfo(name = "page")
    var page: Int = 0

    @JvmField
    @ColumnInfo(name = "content")
    var content: String? = null

    @JvmField
    @ColumnInfo(name = "create_at")
    var createAt: Long = 0

    override fun compare(lhs: Booknote, rhs: Booknote): Int {
        if (lhs.page < rhs.page) {    //页码小的放前面
            return -1
        } else if (lhs.page > rhs.page) {
            return 1
        }
        return 0
    }
}