package com.gentlejournal.domain.model

data class Insight(
    val type: InsightType,
    val text: String,
    val icon: String,
    val trend: TrendDirection? = null,
    val periodDays: Int = 7
)

enum class InsightType {
    PATTERN,
    CORRELATION,
    TIME_BASED,
    TREND,
    SUMMARY,
    SEQUENCE,
    SEASONAL,
    KEYWORD,
    INFO
}

enum class TrendDirection {
    RISING,
    STABLE,
    FALLING
}
