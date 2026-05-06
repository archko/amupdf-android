package cn.archko.pdf.core.common

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Graph {
    lateinit var database: AKDatabase
        private set

    private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `bookmark` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `progress_id` INTEGER NOT NULL, `path` TEXT, `page` INTEGER NOT NULL, `create_at` INTEGER NOT NULL, `content` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `booknote` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `progress_id` INTEGER NOT NULL, `path` TEXT, `page` INTEGER NOT NULL, `create_at` INTEGER NOT NULL, `content` TEXT)")
        }
    }

    private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE progress ADD 'scroll_orientation' INTEGER default 1 not null")
        }
    }

    private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 创建 reading_stats 表
            database.execSQL("CREATE TABLE IF NOT EXISTS `reading_stats` (`path` TEXT NOT NULL PRIMARY KEY, `totalReadingTime` INTEGER NOT NULL, `lastSessionTime` INTEGER NOT NULL, `averageSessionTime` INTEGER NOT NULL, `firstReadAt` INTEGER NOT NULL, `lastReadAt` INTEGER NOT NULL, `completedPages` INTEGER NOT NULL, `totalPages` INTEGER NOT NULL, `sessionCount` INTEGER NOT NULL, `lastSessionDate` TEXT NOT NULL, `consecutiveDays` INTEGER NOT NULL, `annotationCount` INTEGER NOT NULL, `bookmarkCount` INTEGER NOT NULL)")
            
            // 创建 abookmark 表（如果尚未创建）
            database.execSQL("CREATE TABLE IF NOT EXISTS `abookmark` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `path` TEXT NOT NULL, `pageIndex` INTEGER NOT NULL, `title` TEXT, `note` TEXT, `createAt` INTEGER NOT NULL, `updateAt` INTEGER NOT NULL, `color` INTEGER, `scrollY` INTEGER)")
            
            // 为 abookmark 表创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_abookmark_path` ON `abookmark` (`path`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_abookmark_path_pageIndex` ON `abookmark` (`path`, `pageIndex`)")
            
            // 创建 ai_provider 表
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_provider` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `api_key` TEXT NOT NULL, `base_url` TEXT NOT NULL, `model` TEXT NOT NULL, `max_tokens` INTEGER NOT NULL, `temperature` REAL NOT NULL, `enabled` INTEGER NOT NULL, `is_default` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)")
            
            // 创建 ai_cache 表
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_cache` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `document_path` TEXT NOT NULL, `feature_type` TEXT NOT NULL, `input_hash` TEXT NOT NULL, `input_text` TEXT NOT NULL, `output_text` TEXT NOT NULL, `provider_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
            
            // 为 ai_cache 表创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_cache_document_path` ON `ai_cache` (`document_path`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_cache_input_hash` ON `ai_cache` (`input_hash`)")
            // 删除可能存在的旧索引（非唯一索引），然后创建唯一索引
            database.execSQL("DROP INDEX IF EXISTS `index_ai_cache_document_path_feature_type_input_hash`")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_cache_document_path_feature_type_input_hash` ON `ai_cache` (`document_path`, `feature_type`, `input_hash`)")
            
            // 创建 ai_conversation 表
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_conversation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `document_path` TEXT NOT NULL, `session_id` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `context_type` TEXT, `created_at` INTEGER NOT NULL)")
            
            // 为 ai_conversation 表创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversation_session_id` ON `ai_conversation` (`session_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversation_document_path` ON `ai_conversation` (`document_path`)")
            
            // 创建 ai_page_conversation 表
            database.execSQL("CREATE TABLE IF NOT EXISTS `ai_page_conversation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `document_path` TEXT NOT NULL, `document_name` TEXT NOT NULL, `page_index` INTEGER NOT NULL, `question` TEXT NOT NULL, `answer` TEXT NOT NULL, `page_content` TEXT NOT NULL, `created_at` INTEGER NOT NULL)")
            
            // 为 ai_page_conversation 表创建索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_page_conversation_document_path_page_index` ON `ai_page_conversation` (`document_path`, `page_index`)")
        }
    }

    fun provide(context: Context) {
        database = Room.databaseBuilder(context, AKDatabase::class.java, "abook_progress.db")
            .fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_7_8)
            .build()
    }
}