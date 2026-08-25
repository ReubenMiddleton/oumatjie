package com.granify.app.data.settings

/**
 * An in-app text size control layered on top of (multiplied with) the system font scale, so it
 * never fights the Android accessibility setting the user already chose.
 */
enum class TextScale(val multiplier: Float, val label: String) {
    STANDARD(1.0f, "Standard text"),
    LARGE(1.15f, "Large text"),
    EXTRA_LARGE(1.3f, "Extra large text"),
}
