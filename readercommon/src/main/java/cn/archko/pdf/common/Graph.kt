package cn.archko.pdf.common

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.migration.Migration

object Graph {
    lateinit var database: AKDatabase
        private set

    /*private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE " + RecentTableManager.ProgressTbl.TABLE_NAME + " ADD " + RecentTableManager.ProgressTbl.KEY_RECORD_AUTOCROP + " integer")
        }
    }

    private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE " + RecentTableManager.ProgressTbl.TABLE_NAME + " ADD " + RecentTableManager.ProgressTbl.KEY_RECORD_REFLOW + " integer")
        }
    }

    private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE " + RecentTableManager.ProgressTbl.TABLE_NAME + " ADD " + RecentTableManager.ProgressTbl.KEY_RECORD_IS_FAVORITED + " integer")
        }
    }

    private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE " + RecentTableManager.ProgressTbl.TABLE_NAME + " ADD " + RecentTableManager.ProgressTbl.KEY_RECORD_IS_IN_RECENT + " integer")
            val sql = StringBuilder(120)
            sql.append("UPDATE ")
            sql.append(RecentTableManager.ProgressTbl.TABLE_NAME)
            sql.append(" SET ")
            sql.append(RecentTableManager.ProgressTbl.KEY_RECORD_IS_IN_RECENT)
            sql.append("=0")
            database.execSQL(sql.toString())
        }
    }*/

    fun provide(context: Context) {
        database = Room.databaseBuilder(context, AKDatabase::class.java, "abook_progress.db")
            // This is not recommended for normal apps, but the goal of this sample isn't to
            // showcase all of Room.
            .fallbackToDestructiveMigration()
            //.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}