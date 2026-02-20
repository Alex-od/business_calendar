package ua.danichapps.mybusinesscalendar

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import ua.danichapps.mybusinesscalendar.data.di.dataModule
import ua.danichapps.mybusinesscalendar.di.domainModule
import ua.danichapps.mybusinesscalendar.di.presentationModule
import ua.danichapps.mybusinesscalendar.notification.EventNotificationManager
import ua.danichapps.mybusinesscalendar.notification.EventNotificationWorker

/**
 * Application entry point.
 *
 * Responsibilities:
 * 1. Initialise Koin with all DI modules.
 * 2. Create the notification channel (required on Android 8+).
 * 3. Schedule the periodic notification worker.
 */
class MyBusinessCalendarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MyBusinessCalendarApp)
            modules(
                dataModule,
                domainModule,
                presentationModule,
            )
        }

        EventNotificationManager(this).createNotificationChannel()
        EventNotificationWorker.schedule(this)
    }
}
