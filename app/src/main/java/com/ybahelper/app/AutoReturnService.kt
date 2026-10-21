package com.ybahelper.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast

class AutoReturnService : Service() {
    companion object {
        const val ACTION_START = "com.ybahelper.app.action.START_AUTO_RETURN"
        const val ACTION_STOP = "com.ybahelper.app.action.STOP_AUTO_RETURN"
        private const val PREFS = "yba_helper_settings"
        private const val KEY_ENABLED = "auto_return_enabled"
        private const val CHANNEL_ID = "yba_auto_return"
        private const val NOTIFICATION_ID = 2401
        private const val ROBLOX = "com.roblox.client"
        private const val YBA_URL = "https://www.roblox.com/games/2809202155/Your-Bizarre-Adventure"

        fun isEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastRobloxForegroundAt = 0L
    private var hasSeenRoblox = false
    private var attemptedThisAwayPeriod = false
    private var lastLaunchAttemptAt = 0L
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isEnabled(this@AutoReturnService)) {
                stopSelf()
                return
            }
            checkForegroundAndRecover()
            handler.postDelayed(this, 3_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Ожидание Roblox…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
                handler.removeCallbacks(checkRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, true).apply()
                handler.removeCallbacks(checkRunnable)
                handler.post(checkRunnable)
                updateNotification("Включено • проверка каждые 3 секунды")
                return START_STICKY
            }
            else -> return START_NOT_STICKY
        }
    }

    private fun checkForegroundAndRecover() {
        val foreground = currentForegroundPackage()
        val now = System.currentTimeMillis()

        if (foreground == ROBLOX) {
            hasSeenRoblox = true
            lastRobloxForegroundAt = now
            attemptedThisAwayPeriod = false
            updateNotification("Roblox на переднем плане")
            return
        }

        if (!hasSeenRoblox) {
            updateNotification("Ожидание первого запуска Roblox")
            return
        }

        val awayFor = now - lastRobloxForegroundAt
        if (awayFor < 12_000L || attemptedThisAwayPeriod) {
            if (attemptedThisAwayPeriod) {
                updateNotification("Ожидание возврата Roblox • нажмите уведомление")
            } else {
                updateNotification("Roblox не активен • проверка ещё ${((12_000L - awayFor).coerceAtLeast(0L) / 1000L)} сек.")
            }
            return
        }

        // One attempt per away period. Never spawn repeated launch attempts while Android
        // delays/blockades activity starts; reset only after Roblox is observed foreground.
        attemptedThisAwayPeriod = true
        if (now - lastLaunchAttemptAt >= 30_000L) {
            lastLaunchAttemptAt = now
            updateNotification("Пытаюсь вернуть Roblox…")
            tryOpenRoblox()
        } else {
            updateNotification("Нажмите уведомление, чтобы вернуться в Roblox")
        }
    }

    private fun currentForegroundPackage(): String? {
        return try {
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val events = manager.queryEvents(end - 60_000L, end)
            val event = android.app.usage.UsageEvents.Event()
            var latestPackage: String? = null
            var latestTime = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if ((event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) && event.timeStamp >= latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
            latestPackage
        } catch (_: Exception) { null }
    }

    private fun tryOpenRoblox() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YBA_URL)).apply {
            setPackage(ROBLOX)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Android may block background activity launches. The persistent notification
            // still provides a user-initiated PendingIntent to reopen Roblox.
            updateNotification("Автозапуск ограничен Android • нажмите, чтобы вернуться")
        }
    }

    private fun buildNotification(message: String): Notification {
        val returnIntent = Intent(Intent.ACTION_VIEW, Uri.parse(YBA_URL)).apply {
            setPackage(ROBLOX)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        val returnPending = PendingIntent.getActivity(this, 2402, returnIntent, pendingFlags)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            else Notification.Builder(this)
        return builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("YBA Helper • автoвозврат")
            .setContentText(message)
            .setContentIntent(returnPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Автовозврат Roblox", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Статус проверки переднего плана Roblox"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
