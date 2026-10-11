package com.gentlejournal.domain.model

// Display names come from string resources (ui/util/EnumLabels.kt)
enum class AppFont(val familyName: String) {
    DEFAULT("sans-serif"),
    SERIF("serif"),
    MONOSPACE("monospace");
}

enum class AppFontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.2f),
    EXTRA_LARGE(1.4f);
}
