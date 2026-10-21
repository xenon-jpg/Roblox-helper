package com.ybahelper.app

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val robloxPackage = "com.roblox.client"
    private val ybaUrl = "https://www.roblox.com/games/2809202155/Your-Bizarre-Adventure"
    private lateinit var robloxStatus: TextView
    private lateinit var ybaStatus: TextView
    private lateinit var usageStatus: TextView
    private lateinit var autoReturnButton: Button
    private lateinit var root: LinearLayout
    private var autoRefresh = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 16, 26)
        window.navigationBarColor = Color.rgb(16, 16, 26)
        buildUi()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(Color.rgb(16, 16, 26))
        }
        scroll.addView(root)
        setContentView(scroll)

        val logo = text("YBA HELPER", 30f, Color.WHITE, true).apply { letterSpacing = 0.06f }
        root.addView(logo, matchWrap())
        root.addView(text("YOUR BIZARRE ADVENTURE • MOBILE COMPANION", 11f, Color.rgb(167,139,250), true), marginParams(0, 5, 0, 22))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Color.rgb(30, 29, 47), 22)
        }
        root.addView(card, marginParams(0, 0, 0, 16))
        card.addView(text("SYSTEM STATUS", 12f, Color.rgb(167,139,250), true), matchWrap())
        robloxStatus = text("Roblox: проверка…", 18f, Color.WHITE, true)
        card.addView(robloxStatus, marginParams(0, 12, 0, 8))
        ybaStatus = text("YBA: не подтверждена", 16f, Color.LTGRAY, false)
        card.addView(ybaStatus, matchWrap())
        usageStatus = text("Для автоповтора нужен доступ к статистике использования", 12f, Color.GRAY, false)
        card.addView(usageStatus, marginParams(0, 12, 0, 0))

        root.addView(button("▶   ЗАПУСТИТЬ YBA") { launchYba() }, marginParams(0, 0, 0, 10))
        root.addView(button("↻   ОБНОВИТЬ СТАТУС") { refreshStatus() }, marginParams(0, 0, 0, 22))

        root.addView(text("НАСТРОЙКИ", 13f, Color.rgb(167,139,250), true), marginParams(0, 0, 0, 10))
        root.addView(button("Разрешить проверку активности") { openUsageAccess() }, marginParams(0, 0, 0, 10))

        autoReturnButton = button("Автовозврат Roblox: ВЫКЛ") {
            if (!hasUsageAccess()) {
                Toast.makeText(this, "Сначала разрешите доступ к статистике использования", Toast.LENGTH_LONG).show()
                openUsageAccess()
            } else {
                if (android.os.Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 3001)
                    Toast.makeText(this, "Разрешите уведомления, затем нажмите переключатель ещё раз", Toast.LENGTH_LONG).show()
                    return@button
                }
                val enabled = !AutoReturnService.isEnabled(this)
                getSharedPreferences("yba_helper_settings", MODE_PRIVATE).edit()
                    .putBoolean("auto_return_enabled", enabled).apply()
                val serviceIntent = Intent(this, AutoReturnService::class.java).apply {
                    action = if (enabled) AutoReturnService.ACTION_START else AutoReturnService.ACTION_STOP
                }
                if (enabled) {
                    if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                updateAutoReturnButton()
                Toast.makeText(this, if (enabled) "Автовозврат включён" else "Автовозврат выключен", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(autoReturnButton, marginParams(0, 0, 0, 10))
        root.addView(button("Автообновление статуса: ВКЛ") {
            autoRefresh = !autoRefresh
            (it as Button).text = if (autoRefresh) "Автообновление статуса: ВКЛ" else "Автообновление статуса: ВЫКЛ"
            refreshStatus()
        }, marginParams(0, 0, 0, 10))
        root.addView(text("Автовозврат проверяет передний план примерно каждые 3 секунды. Если Roblox не активен 12 секунд, помощник пытается открыть его один раз и ждёт подтверждения, прежде чем пробовать снова. Android может блокировать запуск приложения из фона; в таком случае используйте уведомление «Вернуться в Roblox».", 12f, Color.GRAY, false), marginParams(2, 12, 2, 0))
    }

    private fun updateAutoReturnButton() {
        if (::autoReturnButton.isInitialized) {
            autoReturnButton.text = if (AutoReturnService.isEnabled(this)) "Автовозврат Roblox: ВКЛ" else "Автовозврат Roblox: ВЫКЛ"
        }
    }

    private fun refreshStatus() {
        val installed = isRobloxInstalled()
        robloxStatus.text = if (installed) "● Roblox установлен" else "● Roblox не найден"
        robloxStatus.setTextColor(if (installed) Color.rgb(74, 222, 128) else Color.rgb(248, 113, 113))
        val allowed = hasUsageAccess()
        usageStatus.text = when {
            !allowed -> "Проверка активности: разрешите Usage Access в настройках"
            AutoReturnService.isEnabled(this) -> "Автовозврат включён • задержка 12 секунд"
            else -> "Проверка активности разрешена • автовозврат выключен"
        }
        if (allowed && installed) {
            ybaStatus.text = if (isRobloxForeground()) "Roblox активен • YBA не подтверждена" else "Roblox сейчас не на переднем плане"
        } else if (!installed) {
            ybaStatus.text = "Установите Roblox, чтобы запускать YBA"
        } else {
            ybaStatus.text = "Включите разрешение для проверки активности"
        }
        updateAutoReturnButton()
    }

    private fun isRobloxInstalled(): Boolean =
        try { packageManager.getPackageInfo(robloxPackage, 0); true } catch (_: Exception) { false }

    private fun launchYba() {
        if (!isRobloxInstalled()) {
            Toast.makeText(this, "Сначала установите Roblox", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ybaUrl)).apply {
                setPackage(robloxPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ybaUrl))) }
            catch (_: Exception) { Toast.makeText(this, "Не удалось открыть YBA", Toast.LENGTH_LONG).show() }
        }
    }

    private fun openUsageAccess() {
        try { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        catch (_: Exception) { Toast.makeText(this, "Откройте настройки Android → Специальный доступ → Доступ к статистике использования", Toast.LENGTH_LONG).show() }
    }

    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isRobloxForeground(): Boolean = currentForegroundPackage() == robloxPackage

    private fun currentForegroundPackage(): String? {
        return try {
            val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val events = manager.queryEvents(end - 60_000, end)
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

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized && autoRefresh) refreshStatus()
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(label: String, action: (View) -> Unit) = Button(this).apply {
        text = label; textSize = 14f; isAllCaps = false; setTextColor(Color.WHITE)
        background = rounded(Color.rgb(91, 65, 190), 16)
        setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener(action)
    }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun marginParams(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(dp(l), dp(t), dp(r), dp(b))
    }
}
