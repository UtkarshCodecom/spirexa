package com.desire.photos.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Search over backed-up photos, powered by the AI caption/tags the server
 * attaches on upload (see server/src/services/ai.service.ts) — entirely
 * client-side filtering over the already-fetched media list, so there's no
 * per-search network or AI cost.
 */
class SearchViewModel : ViewModel() {

    private val api = ServiceLocator.apiClient

    private val _all = MutableStateFlow<List<MediaDto>>(emptyList())
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val query = MutableStateFlow("")
    val documentsOnly = MutableStateFlow(false)

    val results: StateFlow<List<MediaDto>> =
        combine(_all, query, documentsOnly) { all, q, docsOnly ->
            val base = all.filter { !it.deleted }.let { if (docsOnly) it.filter { d -> d.isDocument } else it }
            val needle = q.trim().lowercase()
            when {
                needle.isBlank() && docsOnly -> base
                needle.isBlank() -> emptyList()
                else -> base.filter { dto ->
                    dto.fileName.lowercase().contains(needle) ||
                        dto.aiCaption?.lowercase()?.contains(needle) == true ||
                        dto.aiTags.any { it.contains(needle) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Drops one item from the in-memory results right away — used after deleting from the photo viewer. */
    fun remove(id: String) {
        _all.value = _all.value.filterNot { it.id == id }
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            var all = api.listAllMedia().getOrDefault(_all.value)
            _all.value = all
            _loading.value = false

            if (backfillAnalysis(all)) {
                all = api.listAllMedia().getOrDefault(all)
                _all.value = all
            }
        }
    }

    /**
     * Photos backed up before the AI pipeline existed (or before a Gemini
     * key was set) have no caption/tags yet. Analyze a small, capped batch
     * per screen visit — sequential and bounded so this can't turn into an
     * unbounded burst of paid API calls on a big library; the rest catch up
     * over a few more visits to this screen.
     */
    private suspend fun backfillAnalysis(all: List<MediaDto>): Boolean {
        val pending = all
            .filter { !it.deleted && !it.isVideo && it.aiCaption == null }
            .take(BACKFILL_BATCH_SIZE)
        if (pending.isEmpty()) return false

        var analyzed = false
        for (dto in pending) {
            api.analyzeMedia(dto.id).onSuccess { analyzed = true }
        }
        return analyzed
    }

    private companion object {
        const val BACKFILL_BATCH_SIZE = 12
    }
}
