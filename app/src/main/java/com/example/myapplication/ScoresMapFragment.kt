package com.example.myapplication

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.HighScoresDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class ScoresMapFragment : Fragment() {

    private val viewModel: HighScoresViewModel by activityViewModels()
    private var mapView: MapView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // OSMdroid requires context for configuration
        Configuration.getInstance().load(requireContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        
        val view = inflater.inflate(R.layout.fragment_scores_map, container, false)
        mapView = view.findViewById(R.id.map)
        
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        mapView?.controller?.setZoom(5.0)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadAndDisplayMarkers()

        viewModel.selectedScore.observe(viewLifecycleOwner) { score ->
            score?.let {
                val pos = GeoPoint(it.latitude, it.longitude)
                mapView?.controller?.animateTo(pos)
                mapView?.controller?.setZoom(15.0)
            }
        }
    }

    private fun loadAndDisplayMarkers() {
        lifecycleScope.launch {
            val db = HighScoresDatabase.getDatabase(requireContext())
            val scores = withContext(Dispatchers.IO) {
                db.scoreDao().getTopTen()
            }
            
            mapView?.let { map ->
                map.overlays.clear()
                if (scores.isEmpty()) return@launch
                
                for (s in scores) {
                    val marker = Marker(map)
                    marker.position = GeoPoint(s.latitude, s.longitude)
                    marker.title = "Score: ${s.score}"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                }
                
                // Set initial position to first score if available
                if (scores.isNotEmpty()) {
                    val first = GeoPoint(scores[0].latitude, scores[0].longitude)
                    map.controller.setCenter(first)
                    map.controller.setZoom(10.0)
                }
                
                map.invalidate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
}
