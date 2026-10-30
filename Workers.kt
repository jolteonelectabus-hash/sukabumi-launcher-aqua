package com.sukabumi.tvlauncher.workers

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sukabumi.tvlauncher.MotivasiRepo
import com.sukabumi.tvlauncher.PrayerTimeCalculator
import com.sukabumi.tvlauncher.services.AdzanService
import java.time.LocalTime

class MotivasiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val motivasi = MotivasiRepo.getByHour()
            val prefs = applicationContext.getSharedPreferences("sukabumi_launcher", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("current_motivasi_text", motivasi.text)
                .putString("current_motivasi_source", motivasi.source)
                .putLong("last_motivasi_update", System.currentTimeMillis())
                .apply()
            val intent = Intent("com.sukabumi.tvlauncher.MOTIVASI_UPDATED")
            applicationContext.sendBroadcast(intent)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class AdzanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val prayerTimes = PrayerTimeCalculator.getSukabumiTimes()
            val now = LocalTime.now()
            val next = prayerTimes.getNextOrCurrent(now)
            val nowMinutes = now.hour * 60 + now.minute
            val prayerMinutes = next.hour * 60 + next.minute
            val diff = kotlin.math.abs(nowMinutes - prayerMinutes)
            if (diff <= 2) {
                val prefs = applicationContext.getSharedPreferences("sukabumi_launcher", Context.MODE_PRIVATE)
                val todayKey = "adzan_${java.time.LocalDate.now()}_${next.name}"
                val alreadyPlayed = prefs.getBoolean(todayKey, false)
                if (!alreadyPlayed) {
                    val adzanIntent = Intent(applicationContext, AdzanService::class.java).apply {
                        putExtra("prayer_name", next.name)
                    }
                    applicationContext.startForegroundService(adzanIntent)
                    prefs.edit().putBoolean(todayKey, true).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

object PrayerTimeCalculator {
    data class Prayer(val name: String, val time: String, val hour: Int, val minute: Int)
    data class SukabumiPrayerTimes(val list: List<Prayer>) {
        fun getNext(now: LocalTime = LocalTime.now()): Prayer {
            val nowM = now.hour * 60 + now.minute
            return list.firstOrNull { (it.hour * 60 + it.minute) > nowM } ?: list.first()
        }
        fun getNextOrCurrent(now: LocalTime): Prayer {
            val nowM = now.hour * 60 + now.minute
            return list.minByOrNull { kotlin.math.abs((it.hour * 60 + it.minute) - nowM) } ?: list.first()
        }
    }
    fun getSukabumiTimes(): SukabumiPrayerTimes {
        val times = listOf(
            Prayer("Subuh", "04:42", 4, 42),
            Prayer("Dzuhur", "11:55", 11, 55),
            Prayer("Ashar", "15:17", 15, 17),
            Prayer("Maghrib", "18:05", 18, 5),
            Prayer("Isya", "19:16", 19, 16)
        )
        return SukabumiPrayerTimes(times)
    }
}
