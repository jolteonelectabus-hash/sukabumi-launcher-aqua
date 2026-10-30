package com.sukabumi.tvlauncher.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sukabumi.tvlauncher.R

class OverlayService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification("Sukabumi Launcher Aktif"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) return
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("sukabumi_launcher","Sukabumi Launcher Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, com.sukabumi.tvlauncher.MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "sukabumi_launcher")
            .setContentTitle("Sukabumi TV Launcher V3").setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pending).setOngoing(true).build()
    }
}

class AdzanService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("adzan_channel","Adzan Sukabumi", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi Adzan Mekkah"; setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerName = intent?.getStringExtra("prayer_name") ?: "Sholat"
        val notification = NotificationCompat.Builder(this, "adzan_channel")
            .setContentTitle("Waktu $prayerName Tiba").setContentText("Adzan Mekkah - Sukabumi")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setOngoing(true).build()
        startForeground(2, notification)
        playAdzan()
        android.os.Handler(mainLooper).postDelayed({ stopSelf() }, 3 * 60 * 1000)
        return START_NOT_STICKY
    }
    private fun playAdzan() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.adzan).apply {
                isLooping = false; setVolume(1.0f, 1.0f)
                setOnCompletionListener { stopSelf() }; start()
            }
        } catch (e: Exception) {
            e.printStackTrace(); stopSelf()
        }
    }
    override fun onDestroy() {
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, OverlayService::class.java))
            } else {
                context.startService(Intent(context, OverlayService::class.java))
            }
        }
    }
}
