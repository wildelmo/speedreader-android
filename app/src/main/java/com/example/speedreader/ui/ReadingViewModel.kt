package com.example.speedreader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedreader.domain.SessionState
import com.example.speedreader.domain.SessionStatus
import com.example.speedreader.domain.WordUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadingViewModel : ViewModel() {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var readingJob: Job? = null
    private var rampJob: Job? = null

    fun loadText(text: String) {
        val words = WordUtils.parseText(text)
        val avgLen = if (words.isNotEmpty()) words.map { it.text.length }.average().toFloat() else 5f
        
        _state.update { 
            it.copy(
                words = words, 
                currentIndex = 0, 
                status = SessionStatus.IDLE,
                avgWordLength = avgLen
            ) 
        }
    }

    fun startReading() {
        if (_state.value.words.isEmpty() || _state.value.currentIndex >= _state.value.words.size) return
        
        _state.update { 
            it.copy(
                status = SessionStatus.READING,
                // Apply ramp logic based on start conditions if ramp is enabled and we are starting from idle
                speedWpm = if (it.isRampEnabled && it.status == SessionStatus.IDLE) {
                    if (100 < it.targetSpeedWpm) 100 else it.targetSpeedWpm
                } else it.speedWpm
            ) 
        }
        
        startEngine()
        startRampLoop()
    }

    fun pauseReading() {
        _state.update { it.copy(status = SessionStatus.PAUSED) }
        stopEngine()
    }

    fun stopSession() {
        _state.update { it.copy(status = SessionStatus.IDLE, currentIndex = 0) }
        stopEngine()
    }

    private fun startEngine() {
        stopEngine()
        readingJob = viewModelScope.launch {
            while (_state.value.status == SessionStatus.READING) {
                val currentState = _state.value
                val word = currentState.currentWord
                if (word == null || currentState.currentIndex >= currentState.words.size) {
                    _state.update { it.copy(status = SessionStatus.COMPLETED) }
                    stopEngine()
                    break
                }

                val durationMs = WordUtils.calculateWordDurationMs(
                    wordLength = word.text.length,
                    avgWordLength = currentState.avgWordLength,
                    speedWpm = currentState.speedWpm,
                    isVariableTimingEnabled = currentState.isVariableTimingEnabled
                )

                delay(durationMs)

                // Advance word after delay
                _state.update { 
                    val nextIndex = it.currentIndex + 1
                    if (nextIndex >= it.words.size) {
                        it.copy(status = SessionStatus.COMPLETED)
                    } else {
                        it.copy(currentIndex = nextIndex)
                    }
                }
            }
        }
    }

    private fun stopEngine() {
        readingJob?.cancel()
        readingJob = null
        rampJob?.cancel()
        rampJob = null
    }

    private fun startRampLoop() {
        rampJob?.cancel()
        rampJob = viewModelScope.launch {
            while (_state.value.status == SessionStatus.READING && _state.value.isRampEnabled) {
                delay(4000L) // ramp interval is 4 seconds
                val currentState = _state.value
                val newSpeed = currentState.speedWpm + 15
                if (newSpeed >= currentState.targetSpeedWpm) {
                    _state.update { it.copy(speedWpm = currentState.targetSpeedWpm, isRampEnabled = false) }
                    break
                } else {
                    _state.update { it.copy(speedWpm = newSpeed) }
                }
            }
        }
    }

    fun setSpeed(wpm: Int) {
        val clamped = wpm.coerceIn(1, 1000)
        _state.update { it.copy(targetSpeedWpm = clamped, speedWpm = clamped, isRampEnabled = false) }
    }
    
    fun setFontSize(size: Float) {
        _state.update { it.copy(fontSize = size.coerceIn(20f, 200f)) }
    }
    
    fun toggleVariableTiming(enabled: Boolean) {
        _state.update { it.copy(isVariableTimingEnabled = enabled) }
    }
    
    fun setRampTargetSpeed(enabled: Boolean, targetSpeed: Int) {
        _state.update { it.copy(isRampEnabled = enabled, targetSpeedWpm = targetSpeed) }
    }
    
    fun adjustSpeed(amount: Int) {
        val newSpeed = (_state.value.targetSpeedWpm + amount).coerceIn(1, 1000)
        setSpeed(newSpeed)
    }

    fun maintainRampSpeed() {
        // "Maintain" stops the automatic ramp (current speed is held).
        _state.update { it.copy(isRampEnabled = false, targetSpeedWpm = it.speedWpm) }
    }
}
