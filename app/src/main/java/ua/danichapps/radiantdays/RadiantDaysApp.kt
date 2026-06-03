package ua.danichapps.radiantdays

import android.app.Application
import android.os.Bundle
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.java.KoinJavaComponent.getKoin
import ua.danichapps.radiantdays.data.di.dataModule
import ua.danichapps.radiantdays.di.domainModule
import ua.danichapps.radiantdays.di.presentationModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ua.danichapps.radiantdays.notification.AlarmScheduler
import ua.danichapps.radiantdays.notification.EventNotificationManager
import ua.danichapps.radiantdays.notification.ReminderFallbackWorker
import ua.danichapps.radiantdays.sync.WebSocketBridgeClient

/**
 * Application entry point.
 *
 * Responsibilities:
 * 1. Initialise Koin with all DI modules.
 * 2. Create the notification channel (required on Android 8+).
 * 3. Schedule the periodic notification worker.
 */
class RadiantDaysApp : Application() {

    private lateinit var webSocketBridgeClient: WebSocketBridgeClient
    private var startedActivities = 0

    private val appLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: android.app.Activity) {
            startedActivities += 1
            if (startedActivities == 1) {
                webSocketBridgeClient.start()
            }
        }

        override fun onActivityResumed(activity: android.app.Activity) = Unit

        override fun onActivityPaused(activity: android.app.Activity) = Unit

        override fun onActivityStopped(activity: android.app.Activity) {
            startedActivities -= 1
            if (startedActivities <= 0 && !activity.isChangingConfigurations) {
                startedActivities = 0
                webSocketBridgeClient.stop()
            }
        }

        override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: android.app.Activity) = Unit
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@RadiantDaysApp)
            modules(
                dataModule,
                domainModule,
                presentationModule,
            )
        }

        webSocketBridgeClient = getKoin().get<WebSocketBridgeClient>()
        registerActivityLifecycleCallbacks(appLifecycleCallbacks)

        EventNotificationManager(this).createNotificationChannel()
        ReminderFallbackWorker.schedule(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            getKoin().get<AlarmScheduler>().rescheduleAll()
        }
    }

    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(appLifecycleCallbacks)
        if (::webSocketBridgeClient.isInitialized) {
            webSocketBridgeClient.shutdown()
        }
        super.onTerminate()
    }
}
