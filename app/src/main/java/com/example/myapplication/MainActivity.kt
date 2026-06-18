package com.example.myapplication

import android.content.Context
import android.graphics.Color
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

enum class ObstacleType { FIRE, LIGHTNING, ROCK, TRUCK }

data class Obstacle(val id: Long, val lane: Int, var yPosition: Float, val type: ObstacleType)

class MainActivity : ComponentActivity() {

    private lateinit var roadLayout: FrameLayout
    private lateinit var tvScore: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var livesLayout: LinearLayout
    private lateinit var carView: TextView

    private var carLane = 1
    private var lives = 3
    private var score = 0
    private val obstacles = mutableListOf<Obstacle>()
    private val obstacleViews = mutableMapOf<Long, TextView>()

    private val handler = Handler(Looper.getMainLooper())
    private val vibrator by lazy {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    private val gameLoop = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        roadLayout  = findViewById(R.id.roadLayout)
        tvScore     = findViewById(R.id.tvScore)
        tvSpeed     = findViewById(R.id.tvSpeed)
        livesLayout = findViewById(R.id.livesLayout)

        findViewById<Button>(R.id.btnLeft).setOnClickListener {
            if (carLane > 0) { carLane--; updateCarPosition() }
        }
        findViewById<Button>(R.id.btnRight).setOnClickListener {
            if (carLane < 2) { carLane++; updateCarPosition() }
        }

        roadLayout.post {
            setupLaneDividers()
            setupCar()
            updateLives()
            handler.post(gameLoop)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupLaneDividers() {
        val roadH = roadLayout.height
        val roadW = roadLayout.width
        val dashPx = dp(36)
        val gapPx  = dp(28)
        val count  = roadH / (dashPx + gapPx) + 2

        listOf(roadW / 3, roadW * 2 / 3).forEach { xCenter ->
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
        val size = roadLayout.width / 3
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
        carView.x = (roadLayout.width / 3 * carLane).toFloat()
        carView.y = roadLayout.height * 0.85f
    }

    private fun tick() {
        score++
        tvScore.text = "$score"
        tvSpeed.text = "${score / 20} km/h"

        val iter = obstacles.iterator()
        while (iter.hasNext()) {
            val obs = iter.next()
            obs.yPosition += 0.02f
            if (obs.yPosition > 1.1f) {
                roadLayout.removeView(obstacleViews.remove(obs.id))
                iter.remove()
            } else {
                placeObstacleView(obs)
            }
        }

        if (Random.nextFloat() < 0.05f && obstacles.size < 3) {
            val obs = Obstacle(System.currentTimeMillis(), Random.nextInt(3), -0.1f, ObstacleType.entries.random())
            obstacles.add(obs)
            val tv = makeObstacleView(obs)
            obstacleViews[obs.id] = tv
            roadLayout.addView(tv)
            placeObstacleView(obs)
        }

        val hit = obstacles.find { it.lane == carLane && it.yPosition > 0.8f && it.yPosition < 0.95f }
        if (hit != null) {
            obstacles.remove(hit)
            roadLayout.removeView(obstacleViews.remove(hit.id))
            lives--
            updateLives()
            Toast.makeText(this, "CRASH!", Toast.LENGTH_SHORT).show()
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            if (lives <= 0) {
                Toast.makeText(this, "Game Over! Score: $score", Toast.LENGTH_LONG).show()
                handler.postDelayed({ resetGame() }, 1000)
            }
        }
    }

    private fun resetGame() {
        lives = 3; score = 0; carLane = 1
        obstacles.clear()
        obstacleViews.values.forEach { roadLayout.removeView(it) }
        obstacleViews.clear()
        updateLives()
        updateCarPosition()
        tvScore.text = "0"
        tvSpeed.text = "0 km/h"
    }

    private fun makeObstacleView(obs: Obstacle): TextView {
        val emoji = when (obs.type) {
            ObstacleType.FIRE      -> "🔥"
            ObstacleType.LIGHTNING -> "⚡"
            ObstacleType.ROCK      -> "🪨"
            ObstacleType.TRUCK     -> "🚛"
        }
        val size = roadLayout.width / 3
        return TextView(this).apply {
            text = emoji
            textSize = 40f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
    }

    private fun placeObstacleView(obs: Obstacle) {
        obstacleViews[obs.id]?.apply {
            x = (roadLayout.width / 3 * obs.lane).toFloat()
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
}
