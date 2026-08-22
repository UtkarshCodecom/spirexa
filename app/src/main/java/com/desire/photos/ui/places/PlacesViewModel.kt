package com.desire.photos.ui.places

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desire.photos.data.remote.MediaDto
import com.desire.photos.di.ServiceLocator
import com.desire.photos.ui.home.signatureOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/** A cluster of backed-up photos taken near each other. */
data class Place(
    val key: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val items: List<MediaDto>,
)

class PlacesViewModel : ViewModel() {

    private val api = ServiceLocator.apiClient
    private val mediaRepo = ServiceLocator.mediaRepository

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /**
     * Only looks at backed-up photos (their location travels with the upload
     * metadata) — not a fresh per-file EXIF scan of the whole local library,
     * which would be far too slow to do every time this screen opens.
     */
    fun load(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            var all = api.listAllMedia().getOrDefault(emptyList())
            if (backfillLocations(all)) {
                all = api.listAllMedia().getOrDefault(emptyList())
            }
            val located = all.filter { it.hasLocation && !it.deleted }
            _places.value = cluster(located)
            _loading.value = false

            // Reverse-geocoding needs a real device lookup — resolve names
            // after the clusters already show up with coordinate fallback names.
            if (located.isNotEmpty()) {
                val named = resolveNames(context, _places.value)
                _places.value = named
            }
        }
    }

    /**
     * Photos backed up before location capture existed — or before the
     * (optional) media-location permission was granted — sit on the server
     * with no location even though the on-device file's EXIF still has it.
     * One-time, best-effort pass: match already-uploaded local photos to
     * their server record by filename, re-read EXIF, and patch it in.
     * Returns true if anything was patched (caller should re-fetch).
     */
    private suspend fun backfillLocations(remote: List<MediaDto>): Boolean {
        val missing = remote
            .filter { !it.hasLocation && !it.deleted && !it.isVideo }
            .associateBy { signatureOf(it.fileName) }
        if (missing.isEmpty()) return false

        var patched = false
        val local = mediaRepo.queryMedia()
        for (item in local) {
            if (item.isVideo) continue
            val dto = missing[signatureOf(item.displayName)] ?: continue
            val loc = mediaRepo.readExifLocation(item.uri) ?: continue
            api.updateLocation(dto.id, loc.first, loc.second).onSuccess { patched = true }
        }
        return patched
    }

    /** Groups items into ~1.1km grid cells so nearby shots become one place. */
    private fun cluster(items: List<MediaDto>): List<Place> {
        val groups = LinkedHashMap<String, MutableList<MediaDto>>()
        for (item in items) {
            val lat = item.latitude ?: continue
            val lng = item.longitude ?: continue
            val key = "${roundTo(lat)}:${roundTo(lng)}"
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }
        return groups.map { (key, groupItems) ->
            val avgLat = groupItems.mapNotNull { it.latitude }.average()
            val avgLng = groupItems.mapNotNull { it.longitude }.average()
            Place(
                key = key,
                name = coordinateLabel(avgLat, avgLng),
                latitude = avgLat,
                longitude = avgLng,
                items = groupItems.sortedByDescending { it.dateTakenSec },
            )
        }.sortedByDescending { it.items.size }
    }

    private fun roundTo(value: Double): Double = (value * 100).roundToInt() / 100.0

    private fun coordinateLabel(lat: Double, lng: Double): String =
        String.format(Locale.US, "%.2f, %.2f", lat, lng)

    private suspend fun resolveNames(context: Context, places: List<Place>): List<Place> =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext places
            val geocoder = Geocoder(context, Locale.getDefault())
            places.map { place ->
                val name = runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(place.latitude, place.longitude, 1)
                        ?.firstOrNull()
                        ?.let { addr ->
                            addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
                        }
                }.getOrNull()
                if (name != null) place.copy(name = name) else place
            }
        }
}
