package app.swilk.wifitracker.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.room.Room
import app.swilk.wifitracker.data.local.WifiTrackerDatabase
import app.swilk.wifitracker.data.local.dao.BssidDao
import app.swilk.wifitracker.data.local.dao.EventDao
import app.swilk.wifitracker.data.local.dao.TrackerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWifiTrackerDatabase(
        @ApplicationContext context: Context
    ): WifiTrackerDatabase {
        return Room.databaseBuilder(
            context,
            WifiTrackerDatabase::class.java,
            "wifi_tracker_db"
        ).addMigrations(WifiTrackerDatabase.MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideTrackerDao(database: WifiTrackerDatabase): TrackerDao {
        return database.trackerDao()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: WifiTrackerDatabase): EventDao {
        return database.eventDao()
    }

    @Provides
    @Singleton
    fun provideBssidDao(database: WifiTrackerDatabase): BssidDao {
        return database.bssidDao()
    }

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
}
