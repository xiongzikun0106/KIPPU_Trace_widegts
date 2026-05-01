package com.kippu.trace.utils

import kotlin.math.ceil

object TextUtils {
    /**
     * Calculates the "visual width" of a string.
     * Chinese characters count as 1.0, while English/Digits/Symbols count as 0.5.
     */
    fun getVisualWidth(text: String): Float {
        var width = 0f
        for (char in text) {
            if (char.code in 0..127) {
                width += 0.5f
            } else {
                width += 1.0f
            }
        }
        return width
    }

    /**
     * Forces character-level wrapping by inserting zero-width spaces.
     */
    fun forceCharacterWrap(text: String): String {
        return text.map { "$it\u200B" }.joinToString("")
    }

    /**
     * Truncates or validates string based on visual width limit.
     */
    fun isValidTitle(text: String, maxVisualWidth: Float = 10f): Boolean {
        return getVisualWidth(text) <= maxVisualWidth
    }
}
