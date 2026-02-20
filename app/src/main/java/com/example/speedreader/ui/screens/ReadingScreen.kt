package com.example.speedreader.ui.screens

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
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedreader.domain.SessionStatus
import com.example.speedreader.ui.ReadingViewModel

@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var isZenMode by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isZenMode = !isZenMode
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
                    color = Color.Red,
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

        // Top progress indicator
        if (!isZenMode) {
            Text(
                text = "${state.currentIndex} / ${state.totalWords}",
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
            )
        }

        // Bottom Controls
        if (!isZenMode) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(if (isLandscape) 8.dp else 16.dp)
                    .padding(bottom = if (isLandscape) 8.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 8.dp)
            ) {
                if (isLandscape) {
                    val mins = state.timeRemainingSeconds / 60
                    val secs = state.timeRemainingSeconds % 60
                    val secStr = if (secs < 10) "0$secs" else "$secs"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spd: ${state.speedWpm} | Rem: $mins:$secStr", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text("1", color = Color.White, fontSize = 10.sp)
                        Slider(
                            value = state.speedWpm.toFloat(),
                            onValueChange = { viewModel.setSpeed(it.toInt()) },
                            valueRange = 1f..1000f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("1000", color = Color.White, fontSize = 10.sp)
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text("20", color = Color.White, fontSize = 10.sp)
                        Slider(
                            value = state.fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 20f..200f,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text("200", color = Color.White, fontSize = 10.sp)
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
                            Text("Ramp", color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = state.isVariableTimingEnabled,
                                onCheckedChange = { viewModel.toggleVariableTiming(it) }
                            )
                            Text("Var Time", color = Color.White, fontSize = 12.sp)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.status == SessionStatus.READING) {
                                Button(onClick = { viewModel.pauseReading() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Pause", fontSize = 12.sp)
                                }
                            } else {
                                Button(onClick = { viewModel.startReading() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play", fontSize = 12.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.stopSession()
                                    onNavigateBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop", color = Color.White, fontSize = 12.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            OutlinedButton(onClick = { isZenMode = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("Zen", color = Color.White, fontSize = 12.sp)
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
                        
                        OutlinedButton(onClick = { isZenMode = true }) {
                            Text("Zen (Hide)", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
