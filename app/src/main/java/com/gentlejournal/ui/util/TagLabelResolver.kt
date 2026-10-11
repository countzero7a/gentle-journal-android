package com.gentlejournal.ui.util

import android.content.Context
import com.gentlejournal.R

/**
 * For default tags, resolve the label from string resources (localized).
 * For custom tags (isDefault=false), return the label as-is.
 */

private val defaultEmotionMap = mapOf(
    "e_happy" to R.string.emotion_happy,
    "e_calm" to R.string.emotion_calm,
    "e_anxious" to R.string.emotion_anxious,
    "e_sad" to R.string.emotion_sad,
    "e_angry" to R.string.emotion_angry,
    "e_tired" to R.string.emotion_tired,
    "e_grateful" to R.string.emotion_grateful,
    "e_hopeful" to R.string.emotion_hopeful,
    "e_confused" to R.string.emotion_confused,
    "e_content" to R.string.emotion_content,
    "e_excited" to R.string.emotion_excited,
    "e_proud" to R.string.emotion_proud,
    "e_peaceful" to R.string.emotion_peaceful,
    "e_relaxed" to R.string.emotion_relaxed,
    "e_overwhelmed" to R.string.emotion_overwhelmed,
    "e_nervous" to R.string.emotion_nervous,
    "e_lonely" to R.string.emotion_lonely,
    "e_melancholy" to R.string.emotion_melancholy,
    "e_frustrated" to R.string.emotion_frustrated,
    "e_irritated" to R.string.emotion_irritated,
)

private val defaultActionMap = mapOf(
    "a_exercise" to R.string.action_exercise,
    "a_social" to R.string.action_social,
    "a_worked" to R.string.action_worked,
    "a_rested" to R.string.action_rested,
    "a_nature" to R.string.action_nature,
    "a_creative" to R.string.action_creative,
    "a_read" to R.string.action_read,
    "a_cooked" to R.string.action_cooked,
    "a_nothing" to R.string.action_nothing,
    "a_meditated" to R.string.action_meditated,
)

fun resolveEmotionLabel(context: Context, id: String, fallbackLabel: String): String {
    val resId = defaultEmotionMap[id]
    return if (resId != null) context.getString(resId) else fallbackLabel
}

fun resolveActionLabel(context: Context, id: String, fallbackLabel: String): String {
    val resId = defaultActionMap[id]
    return if (resId != null) context.getString(resId) else fallbackLabel
}
