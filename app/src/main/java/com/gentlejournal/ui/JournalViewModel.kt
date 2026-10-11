package com.gentlejournal.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gentlejournal.data.ExportFiles
import com.gentlejournal.data.JournalDatabase
import com.gentlejournal.data.ThemePreferences
import com.gentlejournal.data.entity.ActionEntity
import com.gentlejournal.data.entity.EmotionEntity
import com.gentlejournal.data.entity.EntryEntity
import com.gentlejournal.data.entity.EntryWithTags
import com.gentlejournal.data.entity.InsightEngagementEntity
import com.gentlejournal.data.repository.JournalRepository
import com.gentlejournal.domain.model.AppFont
import com.gentlejournal.domain.model.AppFontSize
import com.gentlejournal.domain.model.AppTheme
import com.gentlejournal.domain.model.AppLanguage
import com.gentlejournal.domain.model.ExportFormat
import com.gentlejournal.domain.model.Insight
import com.gentlejournal.domain.usecase.ExportEntriesUseCase
import com.gentlejournal.domain.usecase.GenerateInsightsUseCase
import com.gentlejournal.domain.usecase.ImportEntriesUseCase
import com.gentlejournal.ui.util.LocaleHelper
import com.gentlejournal.ui.util.resolveEmotionLabel
import com.gentlejournal.ui.util.resolveActionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JournalDatabase.getInstance(application)
    private val repository = JournalRepository(db)
    private val insightDao = db.insightDao()
    private val engagementDao = db.engagementDao()
    private val insightsUseCase = GenerateInsightsUseCase()
    private val exportUseCase = ExportEntriesUseCase()
    private val importUseCase = ImportEntriesUseCase()
    private val themePreferences = ThemePreferences(application)

    // ── Preferences ─────────────────────────────────────────────────

    val appTheme: StateFlow<AppTheme> = themePreferences.selectedTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.MEADOW)

    val appLanguage: StateFlow<AppLanguage> = themePreferences.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.SYSTEM)

    val appFont: StateFlow<AppFont> = themePreferences.selectedFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFont.DEFAULT)

    val appFontSize: StateFlow<AppFontSize> = themePreferences.selectedFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFontSize.MEDIUM)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { themePreferences.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { themePreferences.setLanguage(language) }
    }

    fun setFont(font: AppFont) {
        viewModelScope.launch { themePreferences.setFont(font) }
    }

    fun setFontSize(fontSize: AppFontSize) {
        viewModelScope.launch { themePreferences.setFontSize(fontSize) }
    }

    // ── Widget preference (v3.1) ────────────────────────────────────

    val widgetEnabled: StateFlow<Boolean> = themePreferences.widgetEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setWidgetEnabled(enabled) }
    }

    // ── Reminder ────────────────────────────────────────────────────

    val reminderEnabled: StateFlow<Boolean> = themePreferences.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderHour: StateFlow<Int> = themePreferences.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 21)

    val reminderMinute: StateFlow<Int> = themePreferences.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            themePreferences.setReminder(enabled, hour, minute)
            val app = getApplication<Application>()
            if (enabled) {
                com.gentlejournal.data.ReminderManager.schedule(app, hour, minute)
            } else {
                com.gentlejournal.data.ReminderManager.cancel(app)
            }
        }
    }

    // ── Observable state ────────────────────────────────────────────

    val entries: StateFlow<List<EntryWithTags>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emotions: StateFlow<List<EmotionEntity>> = repository.allEmotions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actions: StateFlow<List<ActionEntity>> = repository.allActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entryCount: StateFlow<Int> = repository.entryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── v3.0: Engagement tracking ───────────────────────────────────

    private val engagementWeights: StateFlow<Map<String, Float>> =
        engagementDao.getAllEngagement()
            .map { list -> list.associate { it.insightType to it.weight } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun recordInsightEngagement(insightType: String) {
        viewModelScope.launch {
            val existing = engagementDao.getEngagement(insightType)
            if (existing != null) {
                // Exponential moving average: 0.8 * old + 0.2 * boost
                val newWeight = existing.weight * 0.8f + 1.5f * 0.2f
                engagementDao.upsertEngagement(
                    existing.copy(
                        weight = newWeight,
                        interactionCount = existing.interactionCount + 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                engagementDao.upsertEngagement(
                    InsightEngagementEntity(
                        insightType = insightType,
                        weight = 1.2f,
                        interactionCount = 1,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // ── v3.0: Label resolver ────────────────────────────────────────

    // The Application context ignores the in-app language, so resolve through a localized one
    private fun localizedContext(language: AppLanguage = appLanguage.value): Context =
        LocaleHelper.localizedContext(getApplication(), language)

    private fun createLabelResolver(context: Context): (String, String) -> String {
        return { id: String, fallbackLabel: String ->
            if (id.startsWith("e_")) {
                resolveEmotionLabel(context, id, fallbackLabel)
            } else if (id.startsWith("a_")) {
                resolveActionLabel(context, id, fallbackLabel)
            } else {
                fallbackLabel
            }
        }
    }

    // ── Insights (v3.0: engagement-ranked, multilingual, resolved) ──

    private val _insightPeriod = MutableStateFlow(7)
    val insightPeriod: StateFlow<Int> = _insightPeriod

    fun setInsightPeriod(days: Int) {
        _insightPeriod.value = days
    }

    // Saved insights from DB
    val savedInsights: StateFlow<List<com.gentlejournal.data.entity.InsightEntity>> =
        insightDao.getAllInsights()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live insights -- recalculated reactively on entries, period, language, or engagement change
    val insights: StateFlow<List<Insight>> = combine(
        entries, _insightPeriod, appLanguage, engagementWeights
    ) { allEntries, period, language, weights ->
        val langCode = LocaleHelper.localeFor(language).language
        val labelResolver = createLabelResolver(localizedContext(language))
        val generated = insightsUseCase(
            allEntries = allEntries,
            periodDays = period,
            languageCode = langCode,
            weights = weights,
            labelResolver = labelResolver
        )
        persistInsights(generated, period)
        generated
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun persistInsights(insights: List<Insight>, periodDays: Int) {
        if (insights.isEmpty()) return
        val now = System.currentTimeMillis()
        // Clean old insights for this period (keep last 24h)
        insightDao.deleteOldInsights(periodDays, now - 24 * 60 * 60 * 1000)
        // Save current batch
        val entities = insights.map { insight ->
            com.gentlejournal.data.entity.InsightEntity(
                type = insight.type.name,
                text = insight.text,
                icon = insight.icon,
                generatedAt = now,
                periodDays = periodDays,
                periodLabel = when (periodDays) { 7 -> "week"; 14 -> "two_weeks"; 30 -> "month"; else -> "custom" }
            )
        }
        insightDao.insertInsights(entities)
    }

    // ── Entry form state ────────────────────────────────────────────

    private val _formText = MutableStateFlow("")
    val formText: StateFlow<String> = _formText

    private val _formEmotionIds = MutableStateFlow<List<String>>(emptyList())
    val formEmotionIds: StateFlow<List<String>> = _formEmotionIds

    private val _formActionIds = MutableStateFlow<List<String>>(emptyList())
    val formActionIds: StateFlow<List<String>> = _formActionIds

    private val _editingEntryId = MutableStateFlow<String?>(null)
    val editingEntryId: StateFlow<String?> = _editingEntryId

    // Guards against a double tap saving the same entry twice
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    // ── Import state ────────────────────────────────────────────────

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus

    fun clearImportStatus() { _importStatus.value = null }

    // ── Form actions ────────────────────────────────────────────────

    fun updateFormText(text: String) { _formText.value = text }

    fun toggleFormEmotion(emotionId: String) {
        val current = _formEmotionIds.value.toMutableList()
        val allEmotions = emotions.value

        // Find the tapped emotion
        val tappedEmotion = allEmotions.find { it.id == emotionId }

        if (current.contains(emotionId)) {
            // Deselecting: if it's a parent, also remove its children
            current.remove(emotionId)
            if (tappedEmotion != null && tappedEmotion.parentId == null) {
                // It's a parent -- remove all children
                val childIds = allEmotions.filter { it.parentId == emotionId }.map { it.id }
                current.removeAll(childIds)
            }
            _formEmotionIds.value = current
        } else if (current.size < 2) {
            if (tappedEmotion != null && tappedEmotion.parentId != null) {
                // It's a sub-emotion -- replace its parent with this sub
                val parentId = tappedEmotion.parentId
                if (current.contains(parentId)) {
                    current.remove(parentId)
                }
                current.add(emotionId)
            } else {
                current.add(emotionId)
            }
            _formEmotionIds.value = current
        }
    }

    fun toggleFormAction(actionId: String) {
        val current = _formActionIds.value
        _formActionIds.value = if (current.contains(actionId)) {
            current - actionId
        } else {
            current + actionId
        }
    }

    fun resetForm() {
        _formText.value = ""
        _formEmotionIds.value = emptyList()
        _formActionIds.value = emptyList()
        _editingEntryId.value = null
    }

    fun loadEntryForEditing(entryId: String) {
        viewModelScope.launch {
            val entryWithTags = repository.getEntry(entryId) ?: return@launch
            _editingEntryId.value = entryId
            _formText.value = entryWithTags.entry.text
            _formEmotionIds.value = entryWithTags.emotions.map { it.id }
            _formActionIds.value = entryWithTags.actions.map { it.id }
        }
    }

    // ── CRUD operations ─────────────────────────────────────────────

    fun saveNewEntry(onComplete: () -> Unit) {
        val text = _formText.value.trim()
        val emotionIds = _formEmotionIds.value
        val actionIds = _formActionIds.value
        if (emotionIds.isEmpty() && text.isBlank()) return

        runSaving {
            val entry = EntryEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                text = text
            )
            repository.saveEntry(entry, emotionIds, actionIds)
            resetForm()
            onComplete()
        }
    }

    fun updateExistingEntry(onComplete: () -> Unit) {
        val entryId = _editingEntryId.value ?: return
        val text = _formText.value.trim()
        val emotionIds = _formEmotionIds.value
        val actionIds = _formActionIds.value
        if (emotionIds.isEmpty() && text.isBlank()) return

        runSaving {
            val existing = repository.getEntry(entryId) ?: return@runSaving
            repository.updateEntry(existing.entry.copy(text = text), emotionIds, actionIds)
            resetForm()
            onComplete()
        }
    }

    fun deleteEntry(entryId: String, onComplete: () -> Unit) {
        runSaving {
            repository.deleteEntry(entryId)
            resetForm()
            onComplete()
        }
    }

    private fun runSaving(block: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAllEntries()
            withContext(Dispatchers.IO) { ExportFiles.clear(getApplication()) }
        }
    }

    // ── Custom tags ─────────────────────────────────────────────────

    fun addCustomEmotion(label: String, emoji: String) {
        if (label.isBlank()) return
        viewModelScope.launch {
            repository.addEmotion(
                EmotionEntity(
                    id = UUID.randomUUID().toString(),
                    label = label.trim(),
                    emoji = emoji.ifBlank { "💭" },
                    isDefault = false,
                    sortOrder = 100 + emotions.value.size
                )
            )
        }
    }

    fun addCustomAction(label: String, emoji: String) {
        if (label.isBlank()) return
        viewModelScope.launch {
            repository.addAction(
                ActionEntity(
                    id = UUID.randomUUID().toString(),
                    label = label.trim(),
                    emoji = emoji.ifBlank { "✨" },
                    isDefault = false,
                    sortOrder = 100 + actions.value.size
                )
            )
        }
    }

    // ── Export ───────────────────────────────────────────────────────

    fun exportData(format: ExportFormat): Intent? {
        val allEntries = entries.value
        if (allEntries.isEmpty()) return null

        val content = exportUseCase(allEntries, format)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "journal-export-$dateStr.${format.extension}"

        return try {
            val app = getApplication<Application>()
            val exportDir = File(app.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, fileName).apply { writeText(content) }
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)

            Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Gentle Journal Export — $dateStr")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            null
        }
    }

    // ── Import ───────────────────────────────────────────────────────

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val inputStream = app.contentResolver.openInputStream(uri)
                val jsonText = inputStream?.bufferedReader()?.use { it.readText() } ?: run {
                    _importStatus.value = "Could not read file"
                    return@launch
                }

                val result = importUseCase(jsonText)
                if (result.entries.isEmpty()) {
                    _importStatus.value = "No entries found in file"
                    return@launch
                }

                // Match emotion/action labels to existing tags
                val currentEmotions = emotions.value
                val currentActions = actions.value

                var importedCount = 0
                result.entries.forEach { imported ->
                    val emotionIds = imported.emotionLabels.mapNotNull { label ->
                        currentEmotions.find { it.label.equals(label, ignoreCase = true) }?.id
                    }
                    val actionIds = imported.actionLabels.mapNotNull { label ->
                        currentActions.find { it.label.equals(label, ignoreCase = true) }?.id
                    }

                    repository.saveEntry(imported.entry, emotionIds, actionIds)
                    importedCount++
                }

                val msg = StringBuilder("Imported $importedCount entries")
                if (result.errorCount > 0) {
                    msg.append(" (${result.errorCount} skipped)")
                }
                _importStatus.value = msg.toString()
            } catch (e: Exception) {
                _importStatus.value = "Import failed: ${e.message}"
            }
        }
    }
}
