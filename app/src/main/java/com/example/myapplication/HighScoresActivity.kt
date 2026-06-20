package com.example.myapplication

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.data.ScoreRecord

class HighScoresViewModel : ViewModel() {
    val selectedScore = MutableLiveData<ScoreRecord?>()
}

class HighScoresActivity : AppCompatActivity() {

    private val viewModel: HighScoresViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        if (savedInstanceState == null) {
            val listFrag = TopTenFragment()
            val mapFrag = ScoresMapFragment()
            
            supportFragmentManager.beginTransaction()
                .add(R.id.listContainer, listFrag)
                .add(R.id.mapContainer, mapFrag)
                .commit()
        }
    }
}
