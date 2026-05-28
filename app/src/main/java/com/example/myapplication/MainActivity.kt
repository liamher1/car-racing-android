package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.ui.tooling.preview.Preview

// ── Obstacle variety ──────────────────────────────────────────────────────────

enum class ObstacleType { FIRE, LIGHTNING, ROCK, TRUCK }

data class Obstacle(val id: Long, val lane: Int, val yPosition: Float, val type: ObstacleType)

// ── Design tokens ─────────────────────────────────────────────────────────────

private val RoadDark    = Color(0xFF0A0A14)
private val RoadMid     = Color(0xFF12102A)
private val NeonPink    = Color(0xFFFF2D78)
private val NeonCyan    = Color(0xFF00E5FF)
private val NeonYellow  = Color(0xFFFFE600)
private val NeonOrange  = Color(0xFFFF6D00)
private val NeonPurple  = Color(0xFFBF5FFF)
private val HudBg       = Color(0xCC0D0D1A)
private val LaneLine    = Color(0x44FFFFFF)

private fun obstacleIcon(type: ObstacleType) = when (type) {
    ObstacleType.FIRE      -> Icons.Default.LocalFireDepartment
    ObstacleType.LIGHTNING -> Icons.Default.Bolt
    ObstacleType.ROCK      -> Icons.Default.Dangerous
    ObstacleType.TRUCK     -> Icons.Default.LocalShipping
}

private fun obstacleColor(type: ObstacleType) = when (type) {
    ObstacleType.FIRE      -> NeonOrange
    ObstacleType.LIGHTNING -> NeonYellow
    ObstacleType.ROCK      -> Color(0xFFB0BEC5)
    ObstacleType.TRUCK     -> NeonPurple
}

// ── Activity ──────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { CarGameApp() } }
    }
}

@Preview(showBackground = true)
@Composable
fun CarGamePreview() {
    MyApplicationTheme { CarGameApp() }
}

// ── Main composable ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarGameApp() {
    val context = LocalContext.current
    var carLane  by remember { mutableIntStateOf(1) }
    var lives    by remember { mutableIntStateOf(3) }
    var score    by remember { mutableIntStateOf(0) }
    var obstacles by remember { mutableStateOf(emptyList<Obstacle>()) }
    var gameActive by remember { mutableStateOf(true) }

    val vibrator = remember {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    }

    // Shared pulse animation used by every obstacle glow
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.15f, targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    // Game loop
    LaunchedEffect(gameActive) {
        while (gameActive) {
            delay(50)
            score += 1

            obstacles = obstacles
                .map { it.copy(yPosition = it.yPosition + 0.02f) }
                .filter { it.yPosition < 1.1f }

            if (Random.nextFloat() < 0.05f && obstacles.size < 3) {
                obstacles = obstacles + Obstacle(
                    id   = System.currentTimeMillis(),
                    lane = Random.nextInt(3),
                    yPosition = -0.1f,
                    type = ObstacleType.entries.random()
                )
            }

            val hit = obstacles.find {
                it.lane == carLane && it.yPosition > 0.8f && it.yPosition < 0.95f
            }
            if (hit != null) {
                obstacles = obstacles.filter { it.id != hit.id }
                lives -= 1
                Toast.makeText(context, "CRASH!", Toast.LENGTH_SHORT).show()
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))

                if (lives <= 0) {
                    Toast.makeText(context, "Game Over!  Score: $score", Toast.LENGTH_LONG).show()
                    delay(1000)
                    lives = 3; score = 0; obstacles = emptyList(); carLane = 1
                }
            }
        }
    }

    // ── Shell ─────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SPEED RUN",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        color = NeonCyan
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RoadDark)
            )
        },
        containerColor = RoadDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(RoadDark)
        ) {

            // ── HUD ───────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .background(HudBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        color = NeonCyan
                    )
                    Text(
                        "$score",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { i ->
                        Icon(
                            imageVector = if (i < lives) Icons.Default.Favorite
                                          else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (i < lives) NeonPink else Color(0x44FFFFFF),
                            modifier = Modifier
                                .size(30.dp)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }

            // ── Road ──────────────────────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(RoadDark, RoadMid, RoadDark))
                    )
            ) {
                val laneWidth   = maxWidth / 3
                val roadHeight  = maxHeight

                // Dashed lane dividers
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawDashedDividers()
                }

                // Obstacles
                obstacles.forEach { obstacle ->
                    val xOff = laneWidth * obstacle.lane
                    val yOff = roadHeight * obstacle.yPosition
                    val color = obstacleColor(obstacle.type)

                    Box(
                        modifier = Modifier
                            .size(laneWidth)
                            .offset(x = xOff, y = yOff),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing glow halo
                        Box(
                            modifier = Modifier
                                .size((laneWidth.value * pulseScale).dp)
                                .background(
                                    color.copy(alpha = pulseAlpha),
                                    RoundedCornerShape(50)
                                )
                        )
                        // Icon
                        Icon(
                            imageVector = obstacleIcon(obstacle.type),
                            contentDescription = obstacle.type.name,
                            modifier = Modifier.size(54.dp),
                            tint = color
                        )
                    }
                }

                // Car + glow
                val carXOff = laneWidth * carLane
                val carYOff = roadHeight * 0.85f
                Box(
                    modifier = Modifier
                        .size(laneWidth)
                        .offset(x = carXOff, y = carYOff),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeonPink.copy(alpha = 0.25f), RoundedCornerShape(50))
                    )
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car",
                        modifier = Modifier.size(64.dp),
                        tint = NeonPink
                    )
                }
            }

            // ── Controls ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudBg)
                    .padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    onClick = { if (carLane > 0) carLane -= 1 },
                    label = "LEFT"
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Left",
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Centre speed indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "${score / 20} km/h",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                ControlButton(
                    onClick = { if (carLane < 2) carLane += 1 },
                    label = "RIGHT"
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Right",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun ControlButton(
    onClick: () -> Unit,
    label: String,
    content: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = NeonPink,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(68.dp)
        ) { content() }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color(0x88FFFFFF)
        )
    }
}

private fun DrawScope.drawDashedDividers() {
    val dashLen = 36f
    val gap     = 28f
    val dividers = listOf(size.width / 3f, size.width * 2f / 3f)
    dividers.forEach { x ->
        var y = 0f
        while (y < size.height) {
            drawLine(
                color       = LaneLine,
                start       = Offset(x, y),
                end         = Offset(x, (y + dashLen).coerceAtMost(size.height)),
                strokeWidth = 3f
            )
            y += dashLen + gap
        }
    }
}
