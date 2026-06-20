package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.HighScoresDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val rgMode = findViewById<RadioGroup>(R.id.rgMode)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnHighScores = findViewById<Button>(R.id.btnHighScores)

        btnStart.setOnClickListener {
            val selectedMode = when (rgMode.checkedRadioButtonId) {
                R.id.rbSlow -> GameMode.BUTTON_SLOW.name
                R.id.rbFast -> GameMode.BUTTON_FAST.name
                R.id.rbSensor -> GameMode.SENSOR.name
                else -> GameMode.BUTTON_SLOW.name
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("GAME_MODE", selectedMode)
            }
            startActivity(intent)
        }

        btnHighScores.setOnClickListener {
            startActivity(Intent(this, HighScoresActivity::class.java))
        }

        // Add a long click listener to the High Scores button to clear data for testing
        btnHighScores.setOnLongClickListener {
            lifecycleScope.launch {
                val db = HighScoresDatabase.getDatabase(this@MainMenuActivity)
                withContext(Dispatchers.IO) {
                    db.scoreDao().deleteAll()
                }
                android.widget.Toast.makeText(this@MainMenuActivity, "High Scores Cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}
