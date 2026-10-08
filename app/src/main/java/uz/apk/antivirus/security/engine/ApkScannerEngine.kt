package uz.apk.antivirus.security.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ThreatLevel { SAFE, LOW, MEDIUM, HIGH, CRITICAL }

data class AppScanResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val threatLevel: ThreatLevel,
    val threatScore: Int,
    val dangerousPermissions: List<String>,
    val reason: String
)

class ApkScannerEngine(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Xavfli ruxsatlar ro'yxati va ularning xavf og'irligi
    private val dangerousPermissionsWeights = mapOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to 40, // Keylogger, Avto-bosish
        "android.permission.SYSTEM_ALERT_WINDOW" to 25,       // Soxta ekran / Overlay
        "android.permission.READ_SMS" to 25,                  // OTP parollarni o'g'irlash
        "android.permission.SEND_SMS" to 25,                  // Pulli xizmatlarga obuna
        "android.permission.RECEIVE_SMS" to 20,
        "android.permission.RECORD_AUDIO" to 20,              // Spyware mikrofon
        "android.permission.CAMERA" to 15,
        "android.permission.READ_CONTACTS" to 15,             // Kontaktlarni o'g'irlash
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 20, // Fonda kuzatuv
        "android.permission.REQUEST_INSTALL_PACKAGES" to 25   // Dropper zararli yuklovchi
    )

    suspend fun scanAllInstalledApps(): List<AppScanResult> = withContext(Dispatchers.IO) {
        val installed = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        installed.map { pkg ->
            analyzePackage(pkg)
        }
    }

    suspend fun inspectSinglePackage(packageName: String): AppScanResult = withContext(Dispatchers.IO) {
        val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        analyzePackage(pkg)
    }

    private fun analyzePackage(pkgInfo: android.content.pm.PackageInfo): AppScanResult {
        val isSystem = (pkgInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
        val requested = pkgInfo.requestedPermissions?.toList() ?: emptyList()
        val appName = pkgInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: pkgInfo.packageName

        var score = 0
        val detected = mutableListOf<String>()
        val reasons = mutableListOf<String>()

        requested.forEach { perm ->
            val weight = dangerousPermissionsWeights[perm]
            if (weight != null) {
                score += weight
                detected.add(perm)
            }
        }

        // Qoida 1: Banking Trojan (Overlay + Accessibility)
        if (requested.contains("android.permission.SYSTEM_ALERT_WINDOW") &&
            requested.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")
        ) {
            score += 35
            reasons.add("Bank trojan belgisi (Ekran ustiga ruxsat + Maxsus imkoniyatlar)")
        }

        // Qoida 2: SMS Spy / OTP theft
        if (requested.contains("android.permission.READ_SMS") &&
            requested.contains("android.permission.SEND_SMS")
        ) {
            score += 30
            reasons.add("Moliyaviy firibgarlik: SMS larni o'qish va yuborish imkoniyati")
        }

        // Qoida 3: Yashirin audio va video yozish
        if (requested.contains("android.permission.RECORD_AUDIO") &&
            requested.contains("android.permission.CAMERA")
        ) {
            reasons.add("Ayg'oqchi dastur (Mikrofon va Kamera)")
        }

        // Tizim ilovalari uchun ishonch koeffitsiyenti
        if (isSystem) {
            score = (score * 0.2).toInt()
        }

        score = score.coerceIn(0, 100)

        val level = when {
            score >= 75 -> ThreatLevel.CRITICAL
            score >= 50 -> ThreatLevel.HIGH
            score >= 25 -> ThreatLevel.MEDIUM
            score >= 10 -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }

        return AppScanResult(
            packageName = pkgInfo.packageName,
            appName = appName,
            versionName = pkgInfo.versionName ?: "1.0",
            isSystemApp = isSystem,
            threatLevel = level,
            threatScore = score,
            dangerousPermissions = detected,
            reason = if (reasons.isNotEmpty()) reasons.joinToString(", ") else "Xavfsiz va sertifikatlangan"
        )
    }
}
