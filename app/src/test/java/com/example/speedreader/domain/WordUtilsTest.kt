package com.example.speedreader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WordUtilsTest {

    @Test
    fun computeCenterIndex_basicWord() {
        assertEquals(2, WordUtils.computeCenterIndex("hello")) // 5 letters: idx 2
        assertEquals(2, WordUtils.computeCenterIndex("test"))  // 4 letters: idx 2 (t e [s] t)
        assertEquals(0, WordUtils.computeCenterIndex("a"))     // 1 letter: idx 0
    }

    @Test
    fun computeCenterIndex_withPunctuation() {
        // "hello" -> h=1, e=2, l=3, l=4, o=5. Center is l at index 3 in the string "\"hello\""
        // Wait, "hello" length is 5. Center of 5 is 5/2 = 2.
        // alphaIndices: 1(h), 2(e), 3(l), 4(l), 5(o). 
        // 5 / 2 = 2. alphaIndices[2] = 3.
        assertEquals(3, WordUtils.computeCenterIndex("\"hello\""))
        assertEquals(2, WordUtils.computeCenterIndex("hello,"))  // "hello," -> alpha at 0,1,2,3,4. 2nd is 2.
    }

    @Test
    fun computeCenterIndex_noLetters() {
        assertEquals(0, WordUtils.computeCenterIndex("---"))
        assertEquals(0, WordUtils.computeCenterIndex("123"))
    }

    @Test
    fun parseText_splitsCorrectly() {
        val words = WordUtils.parseText("Hello,\\nworld!  Testing   123.")
        assertEquals(4, words.size)
        assertEquals(2, words[0].centerIndex)
        assertEquals("world!", words[1].text)
        assertEquals("Testing", words[2].text)
        assertEquals("123.", words[3].text)
    }
}
