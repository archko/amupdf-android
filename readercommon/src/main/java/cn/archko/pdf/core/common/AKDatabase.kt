package cn.archko.pdf.core.common

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.archko.pdf.core.entity.*

@Database(
    entities = [
        BookProgress::class,
        Booknote::class,
        Bookmark::class,
        AICache::class,
        AIConversation::class,
        AIPageConversation::class,
        AIProvider::class,
        ABookmark::class,
        ReadingStats::class,
    ],
    version = 8,
    exportSchema = false
)
//@TypeConverters(DateTimeTypeConverters::class)
abstract class AKDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    public abstract fun bookmarkDao(): BookmarkDao
    public abstract fun readingStatsDao(): ReadingStatsDao
    public abstract fun aiProviderDao(): AIProviderDao
    public abstract fun aiCacheDao(): AICacheDao
    public abstract fun aiConversationDao(): AIConversationDao
    public abstract fun aiPageConversationDao(): AIPageConversationDao
}
