# APK Antivirus - Android Jetpack Compose Project

## Talablar:
- Android Studio Ladybug / Koala / Hedgehog
- JDK 17
- Min SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 15)

## Arxitektura:
- **UI:** Jetpack Compose + Material 3 (Kiberxavfsizlik qorong'u mavzu)
- **Fon Xizmati:** Foreground Service (START_STICKY)
- **Paket Tutuvchi:** BroadcastReceiver (ACTION_PACKAGE_ADDED)
- **Dvigatel:** ApkScannerEngine (Coroutines, Hevristik tahlil, QUERY_ALL_PACKAGES)
- **Doze Optimization:** BatteryOptimizationManager (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

## Android UI moslashuvi (v1.1):
- Edge-to-edge (status/navigatsiya paneli ostiga to'liq ekran), SplashScreen API
- Planshet va landshaft uchun ikki panelli maket (>= 720dp), telefonda bitta ustun
- Pull-to-refresh, filtr chiplari (Hammasi / Xavfli / Foydalanuvchi / Tizim)
- Ilovalarning haqiqiy ikonkalari, TalkBack tavsiflari, matnlar `strings.xml` da
- Radar animatsiyasi faqat skanerlash paytida ishlaydi (batareya tejash)
