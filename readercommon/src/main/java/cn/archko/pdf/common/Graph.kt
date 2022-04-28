package cn.archko.pdf.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.archko.pdf.App

object Graph {
    lateinit var database: AKDatabase
        private set

    private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `bookmark` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `progress_id` INTEGER NOT NULL, `path` TEXT, `page` INTEGER NOT NULL, `create_at` INTEGER NOT NULL, `content` TEXT)")
            database.execSQL("CREATE TABLE IF NOT EXISTS `booknote` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `progress_id` INTEGER NOT NULL, `path` TEXT, `page` INTEGER NOT NULL, `create_at` INTEGER NOT NULL, `content` TEXT)")
        }
    }

    fun provide(context: Context) {
        database = Room.databaseBuilder(context, AKDatabase::class.java, "abook_progress.db")
            .fallbackToDestructiveMigration()
            .addMigrations(MIGRATION_5_6)
            .build()
    }

    // 创建DataStore
    private val App.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = App.PDF_PREFERENCES_NAME
    )

    // 对外开放的DataStore变量
    val dataStore = App.instance!!.dataStore
}