package com.gentlejournal.domain.model

enum class AppFont(val displayName: String, val familyName: String) {
    DEFAULT("System Default", "sans-serif"),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace");
}

enum class AppFontSize(val displayName: String, val scaleFactor: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f),
    EXTRA_LARGE("Extra Large", 1.4f);
}
