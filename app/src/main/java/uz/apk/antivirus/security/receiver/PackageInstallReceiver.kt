package uz.apk.antivirus.security.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.apk.antivirus.security.AntivirusApp
import uz.apk.antivirus.security.R
import uz.apk.antivirus.security.engine.ApkScannerEngine
import uz.apk.antivirus.security.engine.ThreatLevel
import uz.apk.antivirus.security.service.SecurityScanService

/**
 * Yangi APK o'rnatilganda (ACTION_PACKAGE_ADDED) tizimdan darhol signali keladi.
 * Ushbu klass ilovani tahlil qilib, xavfli ruxsatlarni tekshiradi va Heads-up bildirishnoma beradi.
 */
class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // Telefon qayta yoqilganda himoya xizmatini qayta ishga tushirish
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            SecurityScanService.startRealTimeProtection(context)
            return
        }

        val packageName = intent.data?.schemeSpecificPart ?: return

        if (action == Intent.ACTION_PACKAGE_ADDED || action == Intent.ACTION_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            // Asinxron tahlil (asosiy oqimni to'xtatmaslik uchun)
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val scanner = ApkScannerEngine(context.applicationContext)
                    val scanResult = scanner.inspectSinglePackage(packageName)

                    if (scanResult.threatLevel == ThreatLevel.CRITICAL || scanResult.threatLevel == ThreatLevel.HIGH) {
                        showMalwareAlertNotification(context, scanResult.appName, packageName, scanResult.reason)
                    }
                } catch (e: Exception) {
                    // Paket tahlil paytida o'chirilgan bo'lishi mumkin
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showMalwareAlertNotification(
        context: Context,
        appName: String,
        packageName: String,
        reason: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Bir bosishda o'chirish Intent-i
        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val uninstallPending = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            uninstallIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AntivirusApp.ALERT_CHANNEL_ID)
            .setContentTitle("OGOHLANTIRISH: Xavfli APK Aniqlandi!")
            .setContentText("$appName ilovasida shubhali ruxsatlar topildi: $reason")
            .setSmallIcon(R.drawable.ic_warning_shield)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Heads-up ekran ustiga chiqadi
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_delete, "Zudlik bilan O'chirish", uninstallPending)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }
}
