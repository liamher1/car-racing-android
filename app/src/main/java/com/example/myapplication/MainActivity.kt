package com.example.myapplication

import android.app.AlertDialog
import android.content.Context
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
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
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

    private var carLane = 2
    private var lives = 3
    private var score = 0
    private var distance = 0f
    private var speedKmH = 0
    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleViews = mutableMapOf<Long, TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private val vibrator by lazy {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gameMode = GameMode.BUTTON_SLOW
    private var lastLaneChangeTime = 0L

    private val gameLoop = object : Runnable {
        override fun run() {
            tick()
            val delay = if (gameMode == GameMode.BUTTON_FAST) 30L else 50L
            handler.postDelayed(this, delay)
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

        findViewById<Button>(R.id.btnLeft).setOnClickListener {
            if (gameMode != GameMode.SENSOR && carLane > 0) { carLane--; updateCarPosition() }
        }
        findViewById<Button>(R.id.btnRight).setOnClickListener {
            if (gameMode != GameMode.SENSOR && carLane < 4) { carLane++; updateCarPosition() }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        showMenu()
    }

    private fun showMenu() {
        val options = arrayOf("2 Button Mode - Slow", "2 Button Mode - Fast", "1 Sensor Mode")
        AlertDialog.Builder(this)
            .setTitle("Select Game Mode")
            .setItems(options) { _, which ->
                gameMode = when (which) {
                    0 -> GameMode.BUTTON_SLOW
                    1 -> GameMode.BUTTON_FAST
                    else -> GameMode.SENSOR
                }
                startGame()
            }
            .setCancelable(false)
            .show()
    }

    private fun startGame() {
        if (gameMode == GameMode.SENSOR) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            findViewById<Button>(R.id.btnLeft).visibility = View.GONE
            findViewById<Button>(R.id.btnRight).visibility = View.GONE
        } else {
            findViewById<Button>(R.id.btnLeft).visibility = View.VISIBLE
            findViewById<Button>(R.id.btnRight).visibility = View.VISIBLE
        }
        
        roadLayout.post {
            setupLaneDividers()
            setupCar()
            updateLives()
            handler.post(gameLoop)
        }
    }

    override fun onResume() {
        super.onResume()
        if (gameMode == GameMode.SENSOR) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupLaneDividers() {
        roadLayout.removeAllViews() // Clear existing dividers if any
        val roadH = roadLayout.height
        val roadW = roadLayout.width
        val dashPx = dp(36)
        val gapPx  = dp(28)
        val count  = roadH / (dashPx + gapPx) + 2

        // 5 lanes means 4 dividers
        for (i in 1..4) {
            val xCenter = (roadW / 5) * i
            val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            repeat(count) {
                column.addView(View(this).apply {
                    setBackgroundColor(Color.argb(68, 255, 255, 255))
                    layoutParams = LinearLayout.LayoutParams(dp(3), dashPx)
                })
                column.addView(Space(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(3), gapPx)
                })
            }
            val lp = FrameLayout.LayoutParams(dp(3), FrameLayout.LayoutParams.MATCH_PARENT)
            lp.leftMargin = xCenter - dp(1)
            column.layoutParams = lp
            roadLayout.addView(column)
        }
    }

    private fun setupCar() {
        val size = roadLayout.width / 5
        carView = TextView(this).apply {
            text = "🚗"
            textSize = 40f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        roadLayout.addView(carView)
        updateCarPosition()
    }

    private fun updateCarPosition() {
        val laneWidth = roadLayout.width / 5
        carView.x = (laneWidth * carLane).toFloat()
        carView.y = roadLayout.height * 0.85f
    }

    private fun tick() {
        distance += speedKmH / 100f
        tvDistance.text = "${distance.toInt()}m"
        
        // Speed logic
        val baseSpeed = if (gameMode == GameMode.BUTTON_FAST) 100 else 60
        speedKmH = if (gameMode == GameMode.SENSOR) {
            // Speed adjusted by tilt (Y axis) - Bonus requirement
            // We use tiltY to modulate speed
            baseSpeed + (currentTiltY * 10).toInt().coerceIn(-40, 60)
        } else {
            baseSpeed
        }
        tvSpeed.text = "$speedKmH km/h"
        
        // Use speed to affect score slightly too
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

        // Spawn obstacles and coins
        if (Random.nextFloat() < 0.08f && obstacles.size < 6) {
            val type = if (Random.nextFloat() < 0.2f) ObstacleType.COIN else ObstacleType.entries.filter { it != ObstacleType.COIN }.random()
            val obs = Obstacle(System.currentTimeMillis(), Random.nextInt(5), -0.1f, type)
            obstacles.add(obs)
            val tv = makeObstacleView(obs)
            obstacleViews[obs.id] = tv
            roadLayout.addView(tv)
            placeObstacleView(obs)
        }

        // Collision check
        val hitIndex = obstacles.indexOfFirst { it.lane == carLane && it.yPosition > 0.8f && it.yPosition < 0.95f }
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
                    handler.removeCallbacks(gameLoop)
                    Toast.makeText(this, "Game Over! Score: $score", Toast.LENGTH_LONG).show()
                    handler.postDelayed({ showMenu(); resetGame() }, 2000)
                }
            }
        }
    }

    private fun playCrashSound() {
        try {
            // Using reflection to check if the resource exists to avoid build errors if the user hasn't added the file yet
            val resId = resources.getIdentifier("crash_sound", "raw", packageName)
            if (resId != 0) {
                val mp = MediaPlayer.create(this, resId)
                mp?.setOnCompletionListener { it.release() }
                mp?.start()
            } else {
                Toast.makeText(this, "CRASH!", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "CRASH!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetGame() {
        lives = 3; score = 0; carLane = 2; distance = 0f
        obstacles.clear()
        obstacleViews.values.forEach { roadLayout.removeView(it) }
        obstacleViews.clear()
        updateLives()
        updateCarPosition()
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
        val size = roadLayout.width / 5
        return TextView(this).apply {
            text = emoji
            textSize = 34f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
    }

    private fun placeObstacleView(obs: Obstacle) {
        obstacleViews[obs.id]?.apply {
            val laneWidth = roadLayout.width / 5
            x = (laneWidth * obs.lane).toFloat()
            y = roadLayout.height * obs.yPosition
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

    // Sensor Implementation
    private var currentTiltY = 0f
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || gameMode != GameMode.SENSOR) return
        
        val tiltX = event.values[0] // Lateral tilt
        currentTiltY = event.values[1] // Forward/Backward tilt (for speed)
        
        val now = System.currentTimeMillis()
        if (now - lastLaneChangeTime > 300) { // Debounce lane changes
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
