package cn.archko.pdf.common

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.archko.pdf.entity.BookProgress

@Database(
    entities = [
        BookProgress::class,
    ],
    version = 6,
    exportSchema = false
)
//@TypeConverters(DateTimeTypeConverters::class)
abstract class AKDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
