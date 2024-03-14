package cn.archko.pdf.common

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

    fun provide(context: Context) {
        database = Room.databaseBuilder(context, AKDatabase::class.java, "abook_progress.db")
            .fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .build()
    }
}