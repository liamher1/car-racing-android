package com.example.myapplication

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.myapplication.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TopTenFragment : Fragment() {

    private val viewModel: HighScoresViewModel by activityViewModels()
    private lateinit var adapter: ScoresAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_top_ten, container, false)
        
        val rvScores = view.findViewById<RecyclerView>(R.id.rvScores) ?: return view
        rvScores.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = ScoresAdapter(emptyList()) { score ->
            viewModel.selectedScore.value = score
        }
        rvScores.adapter = adapter

        loadScores()
        
        return view
    }

    private fun loadScores() {
        val db = HighScoresDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            val scores = withContext(Dispatchers.IO) {
                db.scoreDao().getTopTen()
            }
            adapter.updateScores(scores)
        }
    }

    private class ScoresAdapter(private var scores: List<ScoreRecord>, private val onClick: (ScoreRecord) -> Unit) :
        RecyclerView.Adapter<ScoresAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tvRank)
            val tvScoreValue: TextView = view.findViewById(R.id.tvScoreValue)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val score = scores[position]
            holder.tvRank.text = (position + 1).toString()
            holder.tvScoreValue.text = score.score.toString()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(score.date))
            holder.itemView.setOnClickListener { onClick(score) }
        }

        override fun getItemCount() = scores.size

        fun updateScores(newScores: List<ScoreRecord>) {
            scores = newScores
            notifyDataSetChanged()
        }
    }
}
