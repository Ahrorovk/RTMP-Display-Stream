package com.example.real_stream

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.media.projection.MediaProjectionManager

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class DisplayActivity : AppCompatActivity() {
    private lateinit var inputUserId: EditText
    private lateinit var spinnerQuality: Spinner
    private lateinit var buttonStart: Button
    private lateinit var statusLayout: LinearLayout
    private lateinit var statusIndicator: View
    private lateinit var statusText: TextView

    private val qualityOptions = mapOf(
        "1080p" to StreamSettings(1920, 1080, 25, 4096 * 1024),
        "720p" to StreamSettings(1280, 720, 25, 2048 * 1024)
    )
    private val baseRtmpUrl = "rtmp://live.twitch.tv/app/"

    data class StreamSettings(val width: Int, val height: Int, val fps: Int, val bitrate: Int)

    private val REQUEST_CODE_STREAM = 179 //random num
    private var isStreaming = false
    private var lastEndpoint: String? = null
    private var lastSettings: StreamSettings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        inputUserId = findViewById(R.id.inputUserId)
        spinnerQuality = findViewById(R.id.spinnerQuality)
        buttonStart = findViewById(R.id.buttonStart)
        statusLayout = findViewById(R.id.statusLayout)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusText = findViewById(R.id.statusText)

        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualityOptions.keys.toList())
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuality.adapter = qualityAdapter
        spinnerQuality.setSelection(1)

        buttonStart.setOnClickListener { onStartStopClick() }
        updateUI(false)
    }

    private fun onStartStopClick() {
        val userId = inputUserId.text.toString().trim()
        if (userId.isEmpty()) {
            Toast.makeText(this, "Введите ID пользователя", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedQuality = spinnerQuality.selectedItem.toString()
        val settings = qualityOptions[selectedQuality] ?: return
        val endpoint = baseRtmpUrl + userId
        if (!isStreaming) {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE_STREAM)
            lastEndpoint = endpoint
            lastSettings = settings
        } else {
            // Остановка стрима через сервис
            val stopIntent = Intent(this, DisplayService::class.java)
            stopService(stopIntent)
            isStreaming = false
            updateUI(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && requestCode == REQUEST_CODE_STREAM && resultCode == RESULT_OK) {
            val endpoint = lastEndpoint ?: return
            val settings = lastSettings ?: return
            val serviceIntent = Intent(this, DisplayService::class.java).apply {
                putExtra("stream_url", endpoint)
                putExtra("result_code", resultCode)
                putExtra("data", data)
                putExtra("width", settings.width)
                putExtra("height", settings.height)
                putExtra("fps", settings.fps)
                putExtra("bitrate", settings.bitrate)
            }
            startForegroundService(serviceIntent)
            isStreaming = true
            updateUI(true)
        } else {
            Toast.makeText(this, "Нет разрешения на захват экрана", Toast.LENGTH_SHORT).show()
            isStreaming = false
            updateUI(false)
        }
    }

    private fun updateUI(isStreaming: Boolean) {
        inputUserId.isEnabled = !isStreaming
        spinnerQuality.isEnabled = !isStreaming
        buttonStart.text = if (isStreaming) "Остановить" else "Начать трансляцию"
        statusLayout.visibility = View.VISIBLE
        statusIndicator.setBackgroundResource(if (isStreaming) android.R.color.holo_green_dark else android.R.color.darker_gray)
        statusText.text = if (isStreaming) "В сети" else "Оффлайн"
    }
}