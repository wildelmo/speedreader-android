package com.example.speedreader.domain

/**
 * Represents a single word to be displayed in the RSVP view.
 *
 * @param text The full string of the word, including any punctuation.
 * @param centerIndex The index (0-based) of the center letter in the original [text] string.
 */
data class Word(
    val text: String,
    val centerIndex: Int
)
