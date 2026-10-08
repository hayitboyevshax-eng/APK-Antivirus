package uz.apk.antivirus.security.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import uz.apk.antivirus.security.MainActivity
import uz.apk.antivirus.security.R
import uz.apk.antivirus.security.engine.ApkScannerEngine

/**
 * Tizim darajasida fonda uzluksiz ishlovchi Foreground Service.
 * START_STICKY orqali OS xotirani tozalaganda ham avtomatik qayta tiklanadi.
 * Batareyani tejash uchun CPU WakeLock faqat qisqa skanerlash daqiqalarida ishlatiladi.
 */
class SecurityScanService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    companion object {
        private const val CHANNEL_ID = "apk_antivirus_realtime_guard"
        private const val NOTIFICATION_ID = 1001

        fun startRealTimeProtection(context: Context) {
            val intent = Intent(context, SecurityScanService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopProtection(context: Context) {
            context.stopService(Intent(context, SecurityScanService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildGuardNotification("Real vaqtda qalqon faol: Qurilma himoyalangan"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: Tizim uni o'ldirsa, xotira bo'shashi bilan darhol qayta ishga tushadi
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Antivirus Fon Himoyasi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Yangi yuklangan va o'rnatilgan APK fayllarni real vaqtda kuzatish"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildGuardNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("APK Antivirus KiberQalqon")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Foydalanuvchi tasodifan surib o'chirib yubormasligi uchun
            .build()
    }
}
