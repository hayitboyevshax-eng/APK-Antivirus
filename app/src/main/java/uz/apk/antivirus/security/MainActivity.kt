package uz.apk.antivirus.security

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import uz.apk.antivirus.security.service.SecurityScanService
import uz.apk.antivirus.security.ui.theme.CyberDarkBackground
import uz.apk.antivirus.security.ui.theme.CyberTheme
import uz.apk.antivirus.security.viewmodel.AntivirusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AntivirusViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* natija talab qilinmaydi */ }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun safeStartActivity(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // Ba'zi qurilmalarda bunday ekran bo'lmasligi mumkin — ilova yiqilmasin
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash ekran super.onCreate'dan oldin o'rnatilishi shart
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Edge-to-edge: status va navigatsiya paneli ostiga to'liq ekran, ikonkalar och rangda
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        requestNotificationPermissionIfNeeded()

        // Real vaqt himoya xizmatini ishga tushirish
        SecurityScanService.startRealTimeProtection(this)

        setContent {
            CyberTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberDarkBackground
                ) {
                    CyberDashboardScreen(
                        viewModel = viewModel,
                        onUninstallRequested = { pkg ->
                            safeStartActivity(
                                Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:$pkg")
                                }
                            )
                        },
                        onRequestBatteryExemption = {
                            safeStartActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
