package com.example.myapplication.data

import android.content.Context
import androidx.room.*

@Entity(tableName = "high_scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val date: Long,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface ScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 10")
    fun getTopTen(): List<ScoreRecord>

    @Insert
    fun insert(score: ScoreRecord)

    @Query("DELETE FROM high_scores")
    fun deleteAll()
}

@Database(entities = [ScoreRecord::class], version = 1, exportSchema = false)
abstract class HighScoresDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile
        private var INSTANCE: HighScoresDatabase? = null

        fun getDatabase(context: Context): HighScoresDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    HighScoresDatabase::class.java,
                    "high_scores_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
