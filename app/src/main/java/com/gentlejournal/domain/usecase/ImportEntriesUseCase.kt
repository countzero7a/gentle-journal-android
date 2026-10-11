package com.gentlejournal.domain.usecase

import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class ImportedEntry(
    val entry: EntryEntity,
    val emotionLabels: List<String>,
    val actionLabels: List<String>
)

data class ImportResult(
    val entries: List<ImportedEntry>,
    val errorCount: Int
)

/**
 * Parses a Gentle Journal JSON export and returns structured entries
 * ready to be saved to the database.
 */
class ImportEntriesUseCase {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    operator fun invoke(jsonText: String): ImportResult {
        var errorCount = 0
        val entries = mutableListOf<ImportedEntry>()

        try {
            val arr = JSONArray(jsonText)
            for (i in 0 until arr.length()) {
                try {
                    val obj = arr.getJSONObject(i)
                    val timestamp = try {
                        dateFormat.parse(obj.getString("timestamp"))?.time
                            ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                    val text = obj.optString("text", "")

                    val emotionLabels = mutableListOf<String>()
                    val emotionsArr = obj.optJSONArray("emotions")
                    if (emotionsArr != null) {
                        for (j in 0 until emotionsArr.length()) {
                            emotionLabels.add(emotionsArr.getString(j))
                        }
                    }

                    val actionLabels = mutableListOf<String>()
                    val actionsArr = obj.optJSONArray("actions")
                    if (actionsArr != null) {
                        for (j in 0 until actionsArr.length()) {
                            actionLabels.add(actionsArr.getString(j))
                        }
                    }

                    entries.add(
                        ImportedEntry(
                            entry = EntryEntity(
                                id = UUID.randomUUID().toString(),
                                timestamp = timestamp,
                                text = text
                            ),
                            emotionLabels = emotionLabels,
                            actionLabels = actionLabels
                        )
                    )
                } catch (e: Exception) {
                    errorCount++
                }
            }
        } catch (e: Exception) {
            errorCount++
        }

        return ImportResult(entries, errorCount)
    }
}
