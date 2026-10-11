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
import com.gentlejournal.domain.model.InsightType
import com.gentlejournal.ui.util.LocaleHelper
import com.gentlejournal.ui.util.ResourceInsightStrings
import com.gentlejournal.ui.widget.MoodWidgetProvider
import androidx.room.withTransaction
import com.gentlejournal.ui.util.resolveEmotionLabel
import com.gentlejournal.ui.util.resolveActionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gentlejournal.R
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
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

    // Eager, so the latest weights are at hand whenever insights are generated
    private val engagement: StateFlow<List<InsightEngagementEntity>> =
        engagementDao.getAllEngagement()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** A tap lifts a type's weight above 1.0; the lift fades with a 14-day half-life. */
    private fun decayedWeight(e: InsightEngagementEntity, nowMs: Long): Float {
        val ageDays = (nowMs - e.lastUpdated).coerceAtLeast(0) / DAY_MS.toDouble()
        val decay = Math.pow(0.5, ageDays / ENGAGEMENT_HALF_LIFE_DAYS).toFloat()
        return 1f + (e.weight - 1f) * decay
    }

    private fun currentWeights(nowMs: Long): Map<String, Float> =
        engagement.value.associate { it.insightType to decayedWeight(it, nowMs) }

    fun recordInsightEngagement(insightType: String) {
        viewModelScope.launch {
            val existing = engagementDao.getEngagement(insightType)
            if (existing != null) {
                // Exponential moving average: 0.8 * old + 0.2 * boost
                val newWeight = decayedWeight(existing, System.currentTimeMillis()) * 0.8f + 1.5f * 0.2f
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

    // Live insights, recalculated off the main thread when entries, period or language change.
    // Engagement weights are read as a snapshot, so tapping an insight doesn't reorder the
    // list under the user's finger; the new ranking applies on the next recalculation.
    val insights: StateFlow<List<Insight>> = combine(
        entries, _insightPeriod, appLanguage
    ) { allEntries, period, language ->
        val context = localizedContext(language)
        val locale = LocaleHelper.localeFor(language)
        val generated = insightsUseCase(
            allEntries = allEntries,
            strings = ResourceInsightStrings(context, locale),
            periodDays = period,
            languageCode = locale.language,
            weights = currentWeights(System.currentTimeMillis()),
            labelResolver = createLabelResolver(context)
        )
        persistInsights(generated, period)
        generated
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Texts of the last batch written per period, so identical recalculations write nothing
    private val lastPersisted = mutableMapOf<Int, Set<String>>()

    /** Keeps one batch per period per day, for [HISTORY_DAYS] days, as "Past Insights". */
    private suspend fun persistInsights(insights: List<Insight>, periodDays: Int) {
        // Only real observations go into history, not the "keep journaling" notes
        val observations = insights.filter { it.type != InsightType.INFO }
        val texts = observations.map { it.text }.toSet()
        if (observations.isEmpty() || lastPersisted[periodDays] == texts) return

        val now = System.currentTimeMillis()
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entities = observations.map { insight ->
            com.gentlejournal.data.entity.InsightEntity(
                type = insight.type.name,
                text = insight.text,
                icon = insight.icon,
                generatedAt = now,
                periodDays = periodDays,
                periodLabel = when (periodDays) { 7 -> "week"; 14 -> "two_weeks"; 30 -> "month"; else -> "custom" }
            )
        }
        db.withTransaction {
            insightDao.deleteInsightsSince(periodDays, startOfToday)
            insightDao.deleteOldInsights(periodDays, now - HISTORY_DAYS * DAY_MS)
            insightDao.insertInsights(entities)
        }
        lastPersisted[periodDays] = texts
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

    private val _importStatus = MutableStateFlow<ImportStatus?>(null)
    val importStatus: StateFlow<ImportStatus?> = _importStatus

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
            MoodWidgetProvider.updateAll(getApplication())
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
            MoodWidgetProvider.updateAll(getApplication())
            resetForm()
            onComplete()
        }
    }

    fun deleteEntry(entryId: String, onComplete: () -> Unit) {
        runSaving {
            repository.deleteEntry(entryId)
            MoodWidgetProvider.updateAll(getApplication())
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
            MoodWidgetProvider.updateAll(getApplication())
            withContext(Dispatchers.IO) { ExportFiles.clear(getApplication()) }
        }
    }

    // ── Custom tags ─────────────────────────────────────────────────

    // Add and rename return false, without saving, when the name is blank or already used
    // by another tag (compared with both the stored and the translated name).

    fun addCustomEmotion(label: String, emoji: String): Boolean {
        if (label.isBlank() || emotionLabelTaken(label, exceptId = null)) return false
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
        return true
    }

    fun addCustomAction(label: String, emoji: String): Boolean {
        if (label.isBlank() || actionLabelTaken(label, exceptId = null)) return false
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
        return true
    }

    fun updateCustomEmotion(emotion: EmotionEntity, label: String, emoji: String): Boolean {
        if (emotion.isDefault || label.isBlank() || emotionLabelTaken(label, exceptId = emotion.id)) return false
        viewModelScope.launch {
            repository.updateEmotion(emotion.copy(label = label.trim(), emoji = emoji.ifBlank { emotion.emoji }))
        }
        return true
    }

    fun updateCustomAction(action: ActionEntity, label: String, emoji: String): Boolean {
        if (action.isDefault || label.isBlank() || actionLabelTaken(label, exceptId = action.id)) return false
        viewModelScope.launch {
            repository.updateAction(action.copy(label = label.trim(), emoji = emoji.ifBlank { action.emoji }))
        }
        return true
    }

    /** Entries keep their text; only their link to this tag is removed (cascade). */
    fun deleteCustomEmotion(emotion: EmotionEntity) {
        if (emotion.isDefault) return
        _formEmotionIds.value = _formEmotionIds.value - emotion.id
        viewModelScope.launch {
            repository.removeEmotion(emotion)
            MoodWidgetProvider.updateAll(getApplication())
        }
    }

    fun deleteCustomAction(action: ActionEntity) {
        if (action.isDefault) return
        _formActionIds.value = _formActionIds.value - action.id
        viewModelScope.launch { repository.removeAction(action) }
    }

    private fun emotionLabelTaken(label: String, exceptId: String?): Boolean {
        val context = localizedContext()
        return emotions.value.any {
            it.id != exceptId && sameLabel(label, it.label, resolveEmotionLabel(context, it.id, it.label))
        }
    }

    private fun actionLabelTaken(label: String, exceptId: String?): Boolean {
        val context = localizedContext()
        return actions.value.any {
            it.id != exceptId && sameLabel(label, it.label, resolveActionLabel(context, it.id, it.label))
        }
    }

    private fun sameLabel(label: String, vararg existing: String): Boolean =
        existing.any { it.trim().equals(label.trim(), ignoreCase = true) }

    // ── Export ───────────────────────────────────────────────────────

    /** Builds the export file off the main thread, then hands the share intent to [onReady]. */
    fun exportData(format: ExportFormat, onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = try {
                withContext(Dispatchers.IO) { writeExport(format) }
            } catch (e: Exception) {
                null
            }
            intent?.let(onReady)
        }
    }

    private suspend fun writeExport(format: ExportFormat): Intent? {
        val allEntries = repository.getAllEntriesOnce()
        if (allEntries.isEmpty()) return null

        val app = getApplication<Application>()
        val context = localizedContext()
        val content = exportUseCase(
            entries = allEntries,
            emotions = repository.getEmotionsOnce(),
            actions = repository.getActionsOnce(),
            format = format,
            emotionLabel = { resolveEmotionLabel(context, it.id, it.label) },
            actionLabel = { resolveActionLabel(context, it.id, it.label) }
        )
        val dateStr = LocalDate.now().toString()
        val fileName = "journal-export-$dateStr.${format.extension}"

        // Only the newest export is kept; older plain-text copies are removed first
        ExportFiles.clear(app)
        val exportDir = ExportFiles.dir(app).apply { mkdirs() }
        val file = File(exportDir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)

        return Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.export_subject, dateStr))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Import ───────────────────────────────────────────────────────

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = try {
                withContext(Dispatchers.IO) { runImport(uri) }
            } catch (e: Exception) {
                ImportStatus.Failed
            }
        }
    }

    private suspend fun runImport(uri: Uri): ImportStatus {
        val app = getApplication<Application>()
        val jsonText = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return ImportStatus.ReadError

        val parsed = importUseCase.parse(jsonText)
        if (parsed.entries.isEmpty()) {
            return if (parsed.errorCount > 0) ImportStatus.Failed else ImportStatus.NoEntries
        }

        val context = localizedContext()
        val plan = repository.importEntries { emotions, actions, entries ->
            importUseCase.plan(
                parsed = parsed,
                existingEmotions = emotions,
                existingActions = actions,
                existingEntries = entries,
                emotionLabel = { resolveEmotionLabel(context, it.id, it.label) },
                actionLabel = { resolveActionLabel(context, it.id, it.label) }
            )
        }
        if (plan.entries.isNotEmpty()) MoodWidgetProvider.updateAll(app)
        return ImportStatus.Done(plan.entries.size, plan.duplicateCount, plan.errorCount)
    }

    private companion object {
        const val DAY_MS = 86_400_000L
        const val HISTORY_DAYS = 30
        const val ENGAGEMENT_HALF_LIFE_DAYS = 14.0
    }
}
