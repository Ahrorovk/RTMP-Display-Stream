package com.example.real_stream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.pedro.rtplibrary.rtmp.RtmpDisplay
import android.app.PendingIntent

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class DisplayService : Service(), ConnectCheckerRtmp {
    private var rtmpDisplay: RtmpDisplay? = null
    private var isStreaming = false
    private var notificationManager: NotificationManager? = null
    private var lastError: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Трансляция экрана",
                NotificationManager.IMPORTANCE_LOW
            )
      notificationManager?.createNotificationChannel(channel)
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopStream()
            stopSelf()
            return START_NOT_STICKY
        }
        if (action == ACTION_START) {
            // Для запуска нужны параметры
            val url = intent.getStringExtra("stream_url") ?: lastUrl
            val resultCode = intent.getIntExtra("result_code", lastResultCode)
            val data = intent.getParcelableExtra<Intent>("data") ?: lastData
            val width = intent.getIntExtra("width", lastWidth)
            val height = intent.getIntExtra("height", lastHeight)
            val fps = intent.getIntExtra("fps", lastFps)
            val bitrate = intent.getIntExtra("bitrate", lastBitrate)
            if (!isStreaming && url != null && data != null) {
                startRtmpStream(url, resultCode, data, width, height, fps, bitrate)
            }
            return START_STICKY
        }
        // Обычный запуск
        val url = intent?.getStringExtra("stream_url")
        val resultCode = intent?.getIntExtra("result_code", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("data")
        val width = intent?.getIntExtra("width", 1280) ?: 1280
        val height = intent?.getIntExtra("height", 720) ?: 720
        val fps = intent?.getIntExtra("fps", 25) ?: 25
        val bitrate = intent?.getIntExtra("bitrate", 2048 * 1024) ?: (2048 * 1024)

        // Сохраняем параметры для повторного запуска
        lastUrl = url
        lastResultCode = resultCode
        lastData = data
        lastWidth = width
        lastHeight = height
        lastFps = fps
        lastBitrate = bitrate

        // Первый запуск — только здесь вызываем startForeground
        startForeground(1, buildStreamNotification())

        if (url != null && data != null && !isStreaming) {
            startRtmpStream(url, resultCode, data, width, height, fps, bitrate)
        } else if (url == null || data == null) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun startRtmpStream(url: String, resultCode: Int, data: Intent, width: Int, height: Int, fps: Int, bitrate: Int) {
        try {
            rtmpDisplay = RtmpDisplay(this, true, this)
            rtmpDisplay?.setIntentResult(resultCode, data)
            val audioReady = rtmpDisplay?.prepareAudio(128 * 1024, 44100, true, false, false) == true
            val videoReady = rtmpDisplay?.prepareVideo(width, height, fps, bitrate, 0, resources.displayMetrics.densityDpi) == true
            if (audioReady && videoReady) {
                rtmpDisplay?.startStream(url)
                isStreaming = true
                updateStreamNotification()
            } else {
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("DisplayService", "Ошибка запуска стрима", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStream()
  }

  fun stopStream(reason: String? = null) {
        if (rtmpDisplay?.isStreaming == true) {
            rtmpDisplay?.stopStream()
        }
        isStreaming = false
        if (reason != null) {
            lastError = reason
            Log.e("DisplayService", "Stream stopped: $reason")
        }
        updateStreamNotification()
        stopForeground(true)
    }

    // ConnectCheckerRtmp implementation
    override fun onConnectionSuccessRtmp() {
        Log.d("DisplayService", "RTMP: Connected")
    }
    override fun onConnectionFailedRtmp(reason: String) {
        Log.e("DisplayService", "RTMP: Connection failed: $reason")
        stopStream(reason)
        stopSelf()
    }
    override fun onDisconnectRtmp() {
        Log.d("DisplayService", "RTMP: Disconnected")
        stopStream("Disconnected from server")
        stopSelf()
    }
    override fun onAuthErrorRtmp() {
        Log.e("DisplayService", "RTMP: Auth error")
        stopStream("Auth error")
        stopSelf()
    }
    override fun onAuthSuccessRtmp() {
        Log.d("DisplayService", "RTMP: Auth success")
    }
    override fun onConnectionStartedRtmp(rtmpUrl: String) {}
    override fun onNewBitrateRtmp(bitrate: Long) {}

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RTMP Трансляция")
            .setContentText(text)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
      .build()
    }

    private fun updateStreamNotification() {
        notificationManager?.notify(1, buildStreamNotification())
    }

    private fun buildStreamNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RTMP Трансляция")
            .setContentText(
                when {
                    isStreaming -> "Трансляция активна"
                    lastError != null -> "Трансляция остановлена: $lastError"
                    else -> "Трансляция остановлена"
                }
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val canRestart = !isStreaming && lastUrl != null && lastData != null && (Build.VERSION.SDK_INT < 34)
        if (canRestart) {
            // Кнопка "Включить" с передачей всех параметров (только до Android 14)
            val startIntent = Intent(this, DisplayService::class.java).apply {
                action = ACTION_START
                putExtra("stream_url", lastUrl)
                putExtra("result_code", lastResultCode)
                putExtra("data", lastData)
                putExtra("width", lastWidth)
                putExtra("height", lastHeight)
                putExtra("fps", lastFps)
                putExtra("bitrate", lastBitrate)
            }
            val startPendingIntent = PendingIntent.getService(this, 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Включить", startPendingIntent)
        }
        if (isStreaming) {
            // Кнопка "Остановить" только если стрим идёт
            val stopIntent = Intent(this, DisplayService::class.java).apply { action = ACTION_STOP }
            val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Остановить", stopPendingIntent)
        }
        return builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "stream_channel"
        private const val ACTION_START = "com.example.real_stream.action.START"
        private const val ACTION_STOP = "com.example.real_stream.action.STOP"
    }

    private var lastUrl: String? = null
    private var lastResultCode: Int = 0
    private var lastData: Intent? = null
    private var lastWidth: Int = 1280
    private var lastHeight: Int = 720
    private var lastFps: Int = 25
    private var lastBitrate: Int = 2048 * 1024
}