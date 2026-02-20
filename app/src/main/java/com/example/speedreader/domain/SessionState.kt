package com.example.speedreader.domain

enum class SessionStatus {
    IDLE,
    READING,
    PAUSED,
    COMPLETED
}

/**
 * Holds the logical state of a reading session.
 */
data class SessionState(
    val status: SessionStatus = SessionStatus.IDLE,
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val speedWpm: Int = 200,
    val isRampEnabled: Boolean = false,
    val isVariableTimingEnabled: Boolean = false,
    val isZenModeEnabled: Boolean = false,
    val fontSize: Float = 48f,
    val avgWordLength: Float = 5f,
    val countdownValue: Int? = null
) {
    val totalWords: Int get() = words.size
    val currentWord: Word? get() = if (words.isNotEmpty() && currentIndex in words.indices) words[currentIndex] else null
    val progress: Float get() = if (totalWords > 0) currentIndex.toFloat() / totalWords else 0f
    val wordsRemaining: Int get() = if (totalWords > 0) totalWords - currentIndex - 1 else 0
    val timeRemainingSeconds: Int get() = if (speedWpm > 0) kotlin.math.ceil((wordsRemaining * 60f) / speedWpm).toInt() else 0
}
