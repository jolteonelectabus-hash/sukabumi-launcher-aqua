package com.sukabumi.tvlauncher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.sukabumi.tvlauncher.services.OverlayService
import com.sukabumi.tvlauncher.workers.AdzanWorker
import com.sukabumi.tvlauncher.workers.MotivasiWorker
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        startForegroundService(Intent(this, OverlayService::class.java))
        scheduleWorkers()

        setContent {
            SukabumiTVTheme {
                LauncherScreen()
            }
        }
    }

    private fun scheduleWorkers() {
        val motivasiRequest = PeriodicWorkRequestBuilder<MotivasiWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        val adzanRequest = PeriodicWorkRequestBuilder<AdzanWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).apply {
            enqueueUniquePeriodicWork("motivasi", ExistingPeriodicWorkPolicy.KEEP, motivasiRequest)
            enqueueUniquePeriodicWork("adzan", ExistingPeriodicWorkPolicy.KEEP, adzanRequest)
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    var isScreensaver by remember { mutableStateOf(false) }
    var currentMotivasi by remember { mutableStateOf(MotivasiRepo.getByHour()) }
    val prayerTimes by remember { mutableStateOf(PrayerTimeCalculator.getSukabumiTimes()) }
    var nextPrayer by remember { mutableStateOf(prayerTimes.getNext()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            val now = LocalTime.now()
            isScreensaver = now.hour >= 22 || now.hour < 5
            currentMotivasi = MotivasiRepo.getByHour()
            nextPrayer = prayerTimes.getNext()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1120), Color(0xFF111E36), Color(0xFF0B1120))
                )
            )
    ) {
        AnimatedContent(targetState = isScreensaver, label = "mode") { screensaver ->
            if (screensaver) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("SUKABUMI TV", color = Color.White.copy(0.5f), fontSize = 14.sp, letterSpacing = 8.sp)
                    Spacer(Modifier.height(24.dp))
                    PrayerCard(prayerTimes = prayerTimes, isCenter = true)
                    Spacer(Modifier.height(48.dp))
                    MotivasiCard(motivasi = currentMotivasi, isScreensaver = true)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Assalamu'alaikum", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        Text("Sukabumi • ${java.time.LocalDate.now()}", color = Color.White.copy(0.6f), fontSize = 16.sp)
                        MotivasiCard(motivasi = currentMotivasi, isScreensaver = false)
                        WeatherCard()
                        FootballCard()
                    }
                    Column(modifier = Modifier.weight(0.8f)) {
                        PrayerCard(prayerTimes = prayerTimes, isCenter = false)
                        Spacer(Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Adzan Berikutnya", color = Color.White.copy(0.6f), fontSize = 12.sp)
                                Text("${nextPrayer.name} • ${nextPrayer.time}", color = Color(0xFF22C55E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerCard(prayerTimes: SukabumiPrayerTimes, isCenter: Boolean) {
    Card(
        modifier = Modifier.then(if (isCenter) Modifier.width(600.dp) else Modifier.fillMaxWidth()),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF162032).copy(0.8f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(Color(0xFF22C55E), RoundedCornerShape(50)))
                Text("JADWAL SHOLAT SUKABUMI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
            }
            prayerTimes.list.forEach { (name, time) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, color = Color.White.copy(0.7f), fontSize = if (isCenter) 18.sp else 14.sp)
                    Text(time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isCenter) 18.sp else 14.sp)
                }
                Divider(color = Color.White.copy(0.08f))
            }
            Text("Metode: Kemenag RI • Zona: WIB", color = Color.White.copy(0.3f), fontSize = 10.sp)
        }
    }
}

@Composable
fun WeatherCard() {
    val weather by produceState(initialValue = "Memuat cuaca Sukabumi...") {
        value = WeatherRepo.getSukabumiWeather()
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⛅", fontSize = 24.sp)
            Column {
                Text("Cuaca Sukabumi", color = Color.White.copy(0.6f), fontSize = 11.sp)
                Text(weather, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FootballCard() {
    var matches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            matches = try { LiveScoreManager.getLiveLiga1() + LiveScoreManager.getLiveEPL() } catch (e: Exception) { emptyList() }
            kotlinx.coroutines.delay(60_000)
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚽ LIVE SCORE • LIGA 1 + EPL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (matches.isEmpty()) {
                Text("Tidak ada pertandingan live saat ini", color = Color.White.copy(0.5f), fontSize = 13.sp)
            } else {
                matches.take(4).forEach { m ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${m.home} ${m.homeScore} - ${m.awayScore} ${m.away}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(m.status, color = if (m.isLive) Color(0xFFEF4444) else Color.White.copy(0.5f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MotivasiCard(motivasi: Motivasi, isScreensaver: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isScreensaver) Color.Transparent else Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.then(if (isScreensaver) Modifier.fillMaxWidth() else Modifier.fillMaxWidth())
    ) {
        Column(
            Modifier.padding(if (isScreensaver) 8.dp else 20.dp),
            horizontalAlignment = if (isScreensaver) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                ""${motivasi.text}"",
                color = Color.White,
                fontSize = if (isScreensaver) 20.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = if (isScreensaver) 28.sp else 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(motivasi.source, color = Color(0xFF22C55E), fontSize = if (isScreensaver) 14.sp else 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
