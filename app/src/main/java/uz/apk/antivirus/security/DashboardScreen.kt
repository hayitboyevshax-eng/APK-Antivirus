package uz.apk.antivirus.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.apk.antivirus.security.engine.AppScanResult
import uz.apk.antivirus.security.engine.ThreatLevel
import uz.apk.antivirus.security.ui.theme.CyberDarkBackground
import uz.apk.antivirus.security.ui.theme.CyberNeonCyan
import uz.apk.antivirus.security.ui.theme.CyberNeonGreen
import uz.apk.antivirus.security.ui.theme.CyberTextSecondary
import uz.apk.antivirus.security.ui.theme.CyberWarningOrange
import uz.apk.antivirus.security.viewmodel.AntivirusViewModel
import uz.apk.antivirus.security.viewmodel.ScanState

/** Shu kenglikdan boshlab (planshet / telefon landshaft) ikki panelli maket ishlatiladi. */
private val TwoPaneMinWidth = 720.dp
private val ContentMaxWidth = 640.dp
private val LeftPaneWidth = 340.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberDashboardScreen(
    viewModel: AntivirusViewModel,
    onUninstallRequested: (String) -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isRealTimeActive by viewModel.isRealTimeActive.collectAsStateWithLifecycle()

    // Ilovaga qaytganda (masalan, o'chirish oynasidan keyin) ro'yxatni jimgina yangilash
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.startDeepScan(animate = false)
    }

    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    val filteredApps = remember(installedApps, filter) {
        when (filter) {
            AppFilter.ALL -> installedApps
            AppFilter.RISKY -> installedApps.filter { it.threatLevel >= ThreatLevel.MEDIUM }
            AppFilter.USER -> installedApps.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> installedApps.filter { it.isSystemApp }
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBackground)
            // Landshaftda kesim (notch) va navigatsiya panelidan chetga surish
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val twoPane = maxWidth >= TwoPaneMinWidth
        val radarSize = if (maxHeight < 480.dp) 140.dp else 180.dp

        Column(modifier = Modifier.fillMaxSize()) {
            CyberTopBar(isRealTimeActive = isRealTimeActive)

            PullToRefreshBox(
                isRefreshing = scanState.isScanning,
                onRefresh = { viewModel.startDeepScan() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (twoPane) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .width(LeftPaneWidth)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = navBarBottom + 16.dp)
                        ) {
                            ScanHeader(
                                scanState = scanState,
                                radarSize = radarSize,
                                onScan = { viewModel.startDeepScan() },
                                onShowApps = { scope.launch { listState.animateScrollToItem(0) } },
                                onBattery = onRequestBatteryExemption
                            )
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 8.dp, end = 16.dp, top = 8.dp, bottom = navBarBottom + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            appsSection(
                                totalCount = installedApps.size,
                                filter = filter,
                                onFilterChange = { filter = it },
                                apps = filteredApps,
                                isScanning = scanState.isScanning,
                                onUninstall = onUninstallRequested
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .widthIn(max = ContentMaxWidth)
                                .fillMaxHeight(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBarBottom + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item(key = "scan_header") {
                                ScanHeader(
                                    scanState = scanState,
                                    radarSize = radarSize,
                                    onScan = { viewModel.startDeepScan() },
                                    onShowApps = { scope.launch { listState.animateScrollToItem(1) } },
                                    onBattery = onRequestBatteryExemption
                                )
                            }
                            appsSection(
                                totalCount = installedApps.size,
                                filter = filter,
                                onFilterChange = { filter = it },
                                apps = filteredApps,
                                isScanning = scanState.isScanning,
                                onUninstall = onUninstallRequested
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Radar + tezkor harakat tugmalari. */
@Composable
private fun ScanHeader(
    scanState: ScanState,
    radarSize: Dp,
    onScan: () -> Unit,
    onShowApps: () -> Unit,
    onBattery: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CyberShieldIndicator(
            isScanning = scanState.isScanning,
            threatsFound = scanState.threatsFound,
            onStartDeepScan = onScan,
            radarSize = radarSize
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Security,
                title = stringResource(R.string.action_deep_scan),
                modifier = Modifier.weight(1f),
                accentColor = CyberNeonGreen,
                onClick = onScan
            )
            QuickActionButton(
                icon = Icons.Default.Apps,
                title = stringResource(R.string.action_apps),
                modifier = Modifier.weight(1f),
                accentColor = CyberNeonCyan,
                onClick = onShowApps
            )
            QuickActionButton(
                icon = Icons.Default.BatteryChargingFull,
                title = stringResource(R.string.action_battery),
                modifier = Modifier.weight(1f),
                accentColor = CyberWarningOrange,
                onClick = onBattery
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** Sarlavha + filtr chiplari + ilovalar kartalari (yoki bo'sh holat). */
private fun LazyListScope.appsSection(
    totalCount: Int,
    filter: AppFilter,
    onFilterChange: (AppFilter) -> Unit,
    apps: List<AppScanResult>,
    isScanning: Boolean,
    onUninstall: (String) -> Unit
) {
    item(key = "apps_header", contentType = "header") {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.section_apps, totalCount),
                color = CyberTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFilterRow(selected = filter, onSelected = onFilterChange)
            Spacer(modifier = Modifier.height(2.dp))
        }
    }

    if (apps.isEmpty()) {
        item(key = "apps_empty", contentType = "empty") {
            EmptyState(
                text = stringResource(
                    if (isScanning && totalCount == 0) R.string.scanning_apps else R.string.empty_filter
                )
            )
        }
    } else {
        items(apps, key = { it.packageName }, contentType = { "app" }) { app ->
            AppThreatCard(app = app, onUninstall = { onUninstall(app.packageName) })
        }
    }
}
