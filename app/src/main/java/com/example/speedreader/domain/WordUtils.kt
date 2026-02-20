package com.example.speedreader.domain

object WordUtils {
    /**
     * Finds the index of the center letter.
     * Computes the center of only the alphabetic characters, then maps back to the original string index.
     * Edge case: if the token has no letters, use index 0.
     */
    fun computeCenterIndex(word: String): Int {
        val alphaIndices = mutableListOf<Int>()
        for (i in word.indices) {
            if (word[i].isLetter()) {
                alphaIndices.add(i)
            }
        }
        
        if (alphaIndices.isEmpty()) {
            return 0
        }
        
        val centerAlphaIndex = alphaIndices.size / 2
        return alphaIndices[centerAlphaIndex]
    }

    /**
     * Splits input text on whitespace and produces a list of Word objects.
     */
    fun parseText(text: String): List<Word> {
        if (text.isBlank()) return emptyList()
        val tokens = text.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return tokens.map { Word(it, computeCenterIndex(it)) }
    }

    /**
     * Calculates the duration (in milliseconds) a word should be displayed.
     * @param wordLength Length of the current word.
     * @param avgWordLength the average word length of the document (default 5.0)
     * @param speedWpm target words per minute
     * @param isVariableTimingEnabled whether variable timing multiplier is applied
     */
    fun calculateWordDurationMs(
        wordLength: Int,
        avgWordLength: Float,
        speedWpm: Int,
        isVariableTimingEnabled: Boolean
    ): Long {
        if (speedWpm <= 0) return 1000L
        val baseMs = (60f / speedWpm) * 1000f
        
        if (!isVariableTimingEnabled) {
            return baseMs.toLong()
        }
        
        val safeAvg = if (avgWordLength > 0f) avgWordLength else 5f
        var multiplier = wordLength / safeAvg
        
        // Clamp multipliers per specification (0.75 - 1.25)
        if (multiplier < 0.75f) multiplier = 0.75f
        if (multiplier > 1.25f) multiplier = 1.25f
        
        return (baseMs * multiplier).toLong()
    }
}
