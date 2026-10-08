package uz.apk.antivirus.security

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class AntivirusApp : Application() {

    companion object {
        const val ALERT_CHANNEL_ID = "apk_antivirus_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createAlertChannel()
    }

    /** Xavfli APK aniqlanganda Heads-up chiqishi uchun yuqori muhimlikdagi kanal */
    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Xavf haqida ogohlantirishlar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shubhali yoki zararli ilova aniqlanganda darhol xabar beradi"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
