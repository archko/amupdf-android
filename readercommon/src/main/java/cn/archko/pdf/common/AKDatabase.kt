package cn.archko.pdf.common

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.archko.pdf.entity.BookProgress
import cn.archko.pdf.entity.Bookmark
import cn.archko.pdf.entity.Booknote

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
