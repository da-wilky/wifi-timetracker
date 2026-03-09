package app.swilk.wifitracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.swilk.wifitracker.data.local.dao.BssidDao
import app.swilk.wifitracker.data.local.dao.EventDao
import app.swilk.wifitracker.data.local.dao.TrackerDao
import app.swilk.wifitracker.data.local.entity.BssidEntity
import app.swilk.wifitracker.data.local.entity.EventEntity
import app.swilk.wifitracker.data.local.entity.TrackerEntity

@Database(
    entities = [TrackerEntity::class, EventEntity::class, BssidEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WifiTrackerDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao
    abstract fun eventDao(): EventDao
    abstract fun bssidDao(): BssidDao

    companion object {
        @Volatile
        private var INSTANCE: WifiTrackerDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 1: Create the bssids table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bssids` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `trackerId` INTEGER NOT NULL,
                        `bssid` TEXT NOT NULL,
                        `firstSeenAt` INTEGER NOT NULL,
                        FOREIGN KEY(`trackerId`) REFERENCES `trackers`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_bssids_trackerId_bssid` ON `bssids` (`trackerId`, `bssid`)"
                )

                // Step 2: Populate bssids from existing tracker BSSID values
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `bssids` (`trackerId`, `bssid`, `firstSeenAt`)
                    SELECT `id`, `bssid`, `createdAt` FROM `trackers` WHERE `bssid` IS NOT NULL
                    """.trimIndent()
                )

                // Step 3: De-duplicate trackers that share the same SSID.
                // For each SSID group keep the tracker with the smallest id (oldest).
                // Reassign events from duplicates to the winner, then delete duplicates.
                database.execSQL(
                    """
                    UPDATE `events` SET `trackerId` = (
                        SELECT MIN(t2.`id`) FROM `trackers` t2
                        WHERE t2.`ssid` = (SELECT `ssid` FROM `trackers` WHERE `id` = `events`.`trackerId`)
                    )
                    WHERE `trackerId` NOT IN (SELECT MIN(`id`) FROM `trackers` GROUP BY `ssid`)
                    """.trimIndent()
                )
                // Migrate bssids from duplicate trackers to the winner before deleting them
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `bssids` (`trackerId`, `bssid`, `firstSeenAt`)
                    SELECT (SELECT MIN(`id`) FROM `trackers` WHERE `ssid` = t.`ssid`),
                           b.`bssid`,
                           MIN(b.`firstSeenAt`)
                    FROM `bssids` b
                    JOIN `trackers` t ON b.`trackerId` = t.`id`
                    WHERE b.`trackerId` NOT IN (SELECT MIN(`id`) FROM `trackers` GROUP BY `ssid`)
                    GROUP BY (SELECT MIN(`id`) FROM `trackers` WHERE `ssid` = t.`ssid`), b.`bssid`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    DELETE FROM `trackers`
                    WHERE `id` NOT IN (SELECT MIN(`id`) FROM `trackers` GROUP BY `ssid`)
                    """.trimIndent()
                )

                // Step 4: Update the index on trackers from (ssid, bssid) to (ssid)
                database.execSQL("DROP INDEX IF EXISTS `index_trackers_ssid_bssid`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_trackers_ssid` ON `trackers` (`ssid`)"
                )

                // Step 5: Set all tracker bssid values to NULL (SSID-only tracking going forward)
                database.execSQL("UPDATE `trackers` SET `bssid` = NULL")
            }
        }

        fun getInstance(context: Context): WifiTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiTrackerDatabase::class.java,
                    "wifi_tracker_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

