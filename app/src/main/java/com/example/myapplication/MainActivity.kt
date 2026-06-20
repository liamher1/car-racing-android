package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.HighScoresDatabase
import com.example.myapplication.data.ScoreRecord
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class ObstacleType { FIRE, LIGHTNING, ROCK, TRUCK, COIN }

data class Obstacle(val id: Long, val lane: Int, var yPosition: Float, val type: ObstacleType)

enum class GameMode { BUTTON_SLOW, BUTTON_FAST, SENSOR }

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var roadLayout: FrameLayout
    private lateinit var tvScore: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var livesLayout: LinearLayout
    private lateinit var carView: TextView

    private lateinit var gameOverLayout: LinearLayout
    private lateinit var tvFinalScore: TextView
    private lateinit var btnPlayAgain: Button
    private lateinit var btnMainMenu: Button

    private var carLane = 2
    private var lives = 3
    private var score = 0
    private var distance = 0f
    private var speedKmH = 0
    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleViews = mutableMapOf<Long, TextView>()
    private var isGameRunning = false

    private val handler = Handler(Looper.getMainLooper())
    private val vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator ?: getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gameMode = GameMode.BUTTON_SLOW
    private var lastLaneChangeTime = 0L

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val gameLoop = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                tick()
                val delay = if (gameMode == GameMode.BUTTON_FAST) 30L else 50L
                handler.postDelayed(this, delay)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roadLayout  = findViewById(R.id.roadLayout)
        tvScore     = findViewById(R.id.tvScore)
        tvDistance  = findViewById(R.id.tvDistance)
        tvSpeed     = findViewById(R.id.tvSpeed)
        livesLayout = findViewById(R.id.livesLayout)

        gameOverLayout = findViewById(R.id.gameOverLayout)
        tvFinalScore = findViewById(R.id.tvFinalScore)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        btnMainMenu = findViewById(R.id.btnMainMenu)

        findViewById<Button>(R.id.btnLeft).setOnClickListener {
            if (isGameRunning && gameMode != GameMode.SENSOR && carLane > 0) { carLane--; updateCarPosition() }
        }
        findViewById<Button>(R.id.btnRight).setOnClickListener {
            if (isGameRunning && gameMode != GameMode.SENSOR && carLane < 4) { carLane++; updateCarPosition() }
        }

        btnPlayAgain.setOnClickListener {
            gameOverLayout.visibility = View.GONE
            resetGame()
            startGame()
        }

        btnMainMenu.setOnClickListener {
            finish()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val modeStr = intent.getStringExtra("GAME_MODE")
        gameMode = try {
            GameMode.valueOf(modeStr ?: GameMode.BUTTON_SLOW.name)
        } catch (e: Exception) {
            GameMode.BUTTON_SLOW
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        roadLayout.doOnLayout {
            setupLaneDividers()
            setupCar()
            startGame()
        }
    }

    private fun startGame() {
        isGameRunning = true
        if (gameMode == GameMode.SENSOR) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            findViewById<Button>(R.id.btnLeft).visibility = View.GONE
            findViewById<Button>(R.id.btnRight).visibility = View.GONE
        } else {
            findViewById<Button>(R.id.btnLeft).visibility = View.VISIBLE
            findViewById<Button>(R.id.btnRight).visibility = View.VISIBLE
        }
        
        updateLives()
        handler.post(gameLoop)
    }

    override fun onResume() {
        super.onResume()
        if (isGameRunning && gameMode == GameMode.SENSOR) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isGameRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupLaneDividers() {
        roadLayout.removeAllViews()
        val roadW = roadLayout.width
        if (roadW == 0) return

        for (i in 1..4) {
            val divider = View(this).apply {
                setBackgroundColor(Color.parseColor("#44FFFFFF"))
                val w = dp(2)
                layoutParams = FrameLayout.LayoutParams(w, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    leftMargin = (roadW / 5) * i - (w / 2)
                }
            }
            roadLayout.addView(divider)
        }
    }

    private fun setupCar() {
        carView = TextView(this).apply {
            text = "🚗"
            textSize = 38f
            gravity = Gravity.CENTER
            visibility = View.VISIBLE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        roadLayout.addView(carView)
        carLane = 2
        updateCarPosition()
    }

    private fun updateCarPosition() {
        val roadW = roadLayout.width
        val roadH = roadLayout.height
        if (roadW == 0 || roadH == 0) return

        val laneWidth = roadW / 5
        // Centering the car emoji within the lane
        carView.post {
            carView.x = (laneWidth * carLane).toFloat() + (laneWidth - carView.width) / 2
            carView.y = roadH * 0.82f
            carView.bringToFront()
        }
    }

    private fun tick() {
        distance += speedKmH / 100f
        tvDistance.text = "${distance.toInt()}m"
        
        val baseSpeed = if (gameMode == GameMode.BUTTON_FAST) 100 else 60
        speedKmH = if (gameMode == GameMode.SENSOR) {
            baseSpeed + (currentTiltY * 10).toInt().coerceIn(-40, 60)
        } else {
            baseSpeed
        }
        tvSpeed.text = "$speedKmH km/h"
        
        if (Random.nextInt(10) == 0) score++
        tvScore.text = "$score"

        val iter = obstacles.iterator()
        val moveStep = 0.02f * (speedKmH / 60f)
        while (iter.hasNext()) {
            val obs = iter.next()
            obs.yPosition += moveStep
            if (obs.yPosition > 1.1f) {
                roadLayout.removeView(obstacleViews.remove(obs.id))
                iter.remove()
            } else {
                placeObstacleView(obs)
            }
        }

        if (Random.nextFloat() < 0.08f && obstacles.size < 5) {
            val type = if (Random.nextFloat() < 0.15f) ObstacleType.COIN else ObstacleType.entries.filter { it != ObstacleType.COIN }.random()
            val obs = Obstacle(System.currentTimeMillis(), Random.nextInt(5), -0.1f, type)
            obstacles.add(obs)
            val tv = makeObstacleView(obs)
            obstacleViews[obs.id] = tv
            roadLayout.addView(tv)
            placeObstacleView(obs)
            if (::carView.isInitialized) carView.bringToFront()
        }

        val hitIndex = obstacles.indexOfFirst { it.lane == carLane && it.yPosition > 0.78f && it.yPosition < 0.90f }
        if (hitIndex != -1) {
            val hit = obstacles[hitIndex]
            obstacles.removeAt(hitIndex)
            roadLayout.removeView(obstacleViews.remove(hit.id))
            
            if (hit.type == ObstacleType.COIN) {
                score += 50
                tvScore.text = "$score"
            } else {
                lives--
                updateLives()
                playCrashSound()
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                if (lives <= 0) {
                    onGameOver()
                }
            }
        }
    }

    private fun onGameOver() {
        isGameRunning = false
        handler.removeCallbacks(gameLoop)
        sensorManager?.unregisterListener(this)
        
        tvFinalScore.text = "Final Score: $score"
        gameOverLayout.visibility = View.VISIBLE
        saveScore()
    }

    private fun saveScore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val lat = location?.latitude ?: (32.0853 + (Random.nextDouble() - 0.5) * 0.1)
                val lon = location?.longitude ?: (34.7818 + (Random.nextDouble() - 0.5) * 0.1)
                performSave(lat, lon)
            }
        } else {
            performSave(32.0853 + (Random.nextDouble() - 0.5) * 0.1, 34.7818 + (Random.nextDouble() - 0.5) * 0.1)
        }
    }

    private fun performSave(lat: Double, lon: Double) {
        val db = HighScoresDatabase.getDatabase(this)
        lifecycleScope.launch {
            val record = ScoreRecord(
                score = score,
                date = System.currentTimeMillis(),
                latitude = lat,
                longitude = lon
            )
            withContext(Dispatchers.IO) {
                db.scoreDao().insert(record)
            }
        }
    }

    private fun playCrashSound() {
        try {
            val resId = resources.getIdentifier("crash_sound", "raw", packageName)
            if (resId != 0) {
                val mp = MediaPlayer.create(this, resId)
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
            }
        } catch (_: Exception) {}
    }

    private fun resetGame() {
        lives = 3; score = 0; carLane = 2; distance = 0f
        obstacles.clear()
        obstacleViews.values.forEach { roadLayout.removeView(it) }
        obstacleViews.clear()
        
        roadLayout.removeAllViews()
        setupLaneDividers()
        setupCar()

        updateLives()
        tvScore.text = "0"
        tvDistance.text = "0m"
        tvSpeed.text = "0 km/h"
    }

    private fun makeObstacleView(obs: Obstacle): TextView {
        val emoji = when (obs.type) {
            ObstacleType.FIRE      -> "🔥"
            ObstacleType.LIGHTNING -> "⚡"
            ObstacleType.ROCK      -> "🪨"
            ObstacleType.TRUCK     -> "🚛"
            ObstacleType.COIN      -> "🟡"
        }
        return TextView(this).apply {
            text = emoji
            textSize = 32f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun placeObstacleView(obs: Obstacle) {
        obstacleViews[obs.id]?.apply {
            val roadW = roadLayout.width
            val roadH = roadLayout.height
            if (roadW > 0 && roadH > 0) {
                val laneWidth = roadW / 5
                post {
                    x = (laneWidth * obs.lane).toFloat() + (laneWidth - width) / 2
                    y = roadH * obs.yPosition
                }
            }
        }
    }

    private fun updateLives() {
        livesLayout.removeAllViews()
        repeat(3) { i ->
            livesLayout.addView(TextView(this).apply {
                text = if (i < lives) "❤️" else "🤍"
                textSize = 22f
                setPadding(4, 0, 4, 0)
            })
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private var currentTiltY = 0f
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isGameRunning || gameMode != GameMode.SENSOR) return
        
        val tiltX = event.values[0]
        currentTiltY = event.values[1]
        
        val now = System.currentTimeMillis()
        if (now - lastLaneChangeTime > 250) {
            if (tiltX > 3f && carLane > 0) {
                carLane--
                updateCarPosition()
                lastLaneChangeTime = now
            } else if (tiltX < -3f && carLane < 4) {
                carLane++
                updateCarPosition()
                lastLaneChangeTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
