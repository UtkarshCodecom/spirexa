package com.desire.photos.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.data.remote.AlbumDto
import com.desire.photos.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel : ViewModel() {

    private val api = ServiceLocator.apiClient

    private val _albums = MutableStateFlow<List<AlbumDto>>(emptyList())
    val albums: StateFlow<List<AlbumDto>> = _albums.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            api.listAlbums().onSuccess { _albums.value = it }
            _loading.value = false
        }
    }

    fun createAlbum(title: String, onResult: (Result<AlbumDto>) -> Unit) {
        viewModelScope.launch {
            val result = api.createAlbum(title.ifBlank { "New folder" })
            result.onSuccess { load() }
            onResult(result)
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            api.deleteAlbum(albumId)
            load()
        }
    }
}
