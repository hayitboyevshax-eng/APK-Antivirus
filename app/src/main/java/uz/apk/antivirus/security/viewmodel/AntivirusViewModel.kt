package uz.apk.antivirus.security.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.apk.antivirus.security.engine.ApkScannerEngine
import uz.apk.antivirus.security.engine.AppScanResult
import uz.apk.antivirus.security.engine.ThreatLevel

data class ScanState(
    val isScanning: Boolean = false,
    val threatsFound: Int = 0
)

class AntivirusViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = ApkScannerEngine(application.applicationContext)

    private val _scanState = MutableStateFlow(ScanState())
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppScanResult>>(emptyList())
    val installedApps: StateFlow<List<AppScanResult>> = _installedApps.asStateFlow()

    private val _isRealTimeActive = MutableStateFlow(true)
    val isRealTimeActive: StateFlow<Boolean> = _isRealTimeActive.asStateFlow()

    private var scanJob: Job? = null

    init {
        startDeepScan()
    }

    /**
     * @param animate true bo'lsa radar aylanadi (foydalanuvchi so'ragan skan);
     * false bo'lsa jimgina yangilanadi (masalan, ilovaga qaytganda yoki o'chirishdan keyin).
     */
    fun startDeepScan(animate: Boolean = true) {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            if (animate) _scanState.update { it.copy(isScanning = true) }
            try {
                val results = engine.scanAllInstalledApps()
                    .sortedByDescending { it.threatScore }
                if (animate) delay(900) // radar animatsiyasi ko'rinib turishi uchun
                _installedApps.value = results
                _scanState.value = ScanState(
                    isScanning = false,
                    threatsFound = results.count {
                        it.threatLevel == ThreatLevel.HIGH || it.threatLevel == ThreatLevel.CRITICAL
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _scanState.update { it.copy(isScanning = false) }
            }
        }
    }
}
