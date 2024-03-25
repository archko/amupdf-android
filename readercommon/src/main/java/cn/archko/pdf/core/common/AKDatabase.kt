package cn.archko.pdf.core.common

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.archko.pdf.core.entity.BookProgress
import cn.archko.pdf.core.entity.Bookmark
import cn.archko.pdf.core.entity.Booknote

@Database(
    entities = [
        BookProgress::class,
        Booknote::class,
        Bookmark::class,
    ],
    version = 7,
    exportSchema = false
)
//@TypeConverters(DateTimeTypeConverters::class)
abstract class AKDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
