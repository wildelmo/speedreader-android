package com.example.speedreader.ui.screens

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.speedreader.domain.SessionStatus
import com.example.speedreader.ui.ReadingViewModel
import com.example.speedreader.ui.theme.ZenAccentRed

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isMenuHidden = state.isZenModeEnabled && state.status == SessionStatus.READING
    val interactionSource = remember { MutableInteractionSource() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(state.status) {
        view.keepScreenOn = (state.status == SessionStatus.READING)
        onDispose {
            view.keepScreenOn = false
        }
    }
    DisposableEffect(state.isZenModeEnabled) {
        (context as? Activity)?.let { activity ->
            val insetsController = WindowCompat.getInsetsController(activity.window, view)
            if (state.isZenModeEnabled) {
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            (context as? Activity)?.let { activity ->
                WindowCompat.getInsetsController(activity.window, view).show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (state.status == SessionStatus.READING) {
                    viewModel.pauseReading()
                }
            }
    ) {
        // Center text display
        val currentWord = state.currentWord
        if (currentWord != null) {
            val text = currentWord.text
            val centerIndex = currentWord.centerIndex

            val beforeCenter = if (centerIndex > 0) text.substring(0, centerIndex) else ""
            val centerLetter = if (text.isNotEmpty()) text[centerIndex].toString() else ""
            val afterCenter = if (text.length > centerIndex + 1) text.substring(centerIndex + 1) else ""

            val fontSize = state.fontSize.sp

            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left part (Right-aligned against the center letter)
                Text(
                    text = beforeCenter,
                    color = Color.White,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )

                // Center red letter
                Text(
                    text = centerLetter,
                    color = ZenAccentRed,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // Right part (Left-aligned)
                Text(
                    text = afterCenter,
                    color = Color.White,
                    fontSize = fontSize,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
            }
        } else if (state.status == SessionStatus.COMPLETED) {
            Text(
                "Reading Complete!",
                color = Color.White,
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Countdown overlay
        AnimatedVisibility(
            visible = state.countdownValue != null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = state.countdownValue,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "countdown"
                ) { countdown ->
                    if (countdown != null) {
                        Text(
                            text = countdown.toString(),
                            color = Color.White,
                            fontSize = 150.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Top progress indicator
        if (!isMenuHidden) {
            Text(
                text = "${state.currentIndex} / ${state.totalWords}",
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(top = 32.dp)
            )
        }

        // Bottom Controls
        if (!isMenuHidden) {
            val bannerPadding = if (isLandscape) 6.dp else 16.dp
            val bannerSpacing = if (isLandscape) 4.dp else 8.dp
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(bannerPadding)
                    .padding(bottom = bannerPadding),
                verticalArrangement = Arrangement.spacedBy(bannerSpacing)
            ) {
                if (isLandscape) {
                    val mins = state.timeRemainingSeconds / 60
                    val secs = state.timeRemainingSeconds % 60
                    val secStr = if (secs < 10) "0$secs" else "$secs"
                    
                    // Info row (match portrait)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speed: ${state.speedWpm} WPM", color = Color.White)
                        Text("Remaining: $mins:$secStr", color = Color.White)
                    }
                    
                    // Slider row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1", color = Color.White)
                        Slider(
                            value = state.speedWpm.toFloat(),
                            onValueChange = { viewModel.setSpeed(it.toInt()) },
                            valueRange = 1f..1000f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("1000", color = Color.White)
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("20px", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = state.fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 20f..200f,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text("200px", color = Color.White, fontSize = 12.sp)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.isRampEnabled,
                                onCheckedChange = { viewModel.toggleRamp(it) }
                            )
                            Text("Ramp up", color = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Checkbox(
                                checked = state.isVariableTimingEnabled,
                                onCheckedChange = { viewModel.toggleVariableTiming(it) }
                            )
                            Text("Variable Time", color = Color.White)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.status == SessionStatus.READING) {
                                Button(onClick = { viewModel.pauseReading() }) {
                                    Text("Pause")
                                }
                            } else {
                                Button(onClick = { viewModel.startReading() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play")
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.stopSession()
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", color = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = { viewModel.toggleZenMode(!state.isZenModeEnabled) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isZenModeEnabled) Color.White.copy(alpha = 0.2f) else Color.Transparent
                                )
                            ) {
                                Text("Zen", color = Color.White)
                            }
                        }
                    }
                } else {
                    // Portrait logic
                    // Info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speed: ${state.speedWpm} WPM", color = Color.White)
                        
                        val mins = state.timeRemainingSeconds / 60
                        val secs = state.timeRemainingSeconds % 60
                        val secStr = if (secs < 10) "0$secs" else "$secs"
                        Text("Remaining: $mins:$secStr", color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("1", color = Color.White)
                        Slider(
                            value = state.speedWpm.toFloat(),
                            onValueChange = { viewModel.setSpeed(it.toInt()) },
                            valueRange = 1f..1000f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("1000", color = Color.White)
                    }

                    // Font size Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("20px", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = state.fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 20f..200f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("200px", color = Color.White, fontSize = 12.sp)
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.isRampEnabled,
                                onCheckedChange = { viewModel.toggleRamp(it) }
                            )
                            Text("Ramp up", color = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.isVariableTimingEnabled,
                                onCheckedChange = { viewModel.toggleVariableTiming(it) }
                            )
                            Text("Variable Time", color = Color.White)
                        }
                    }

                    // Play/Pause/Stop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.status == SessionStatus.READING) {
                            Button(onClick = { viewModel.pauseReading() }) {
                                Text("Pause")
                            }
                        } else {
                            Button(onClick = { viewModel.startReading() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                viewModel.stopSession()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = { viewModel.toggleZenMode(!state.isZenModeEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isZenModeEnabled) Color.White.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Text("Zen", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
