package com.music.vivi.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.lastfm.LastFM
import com.music.lastfm.models.LastFmArtist
import com.music.lastfm.models.LastFmTrack
import com.music.vivi.blend.BlendRecord
import com.music.vivi.blend.SupabaseBlendClient
import com.music.vivi.blend.generateCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

data class BlendResult(
    val user1: String,
    val user2: String,
    val user1Artists: List<LastFmArtist>,
    val user2Artists: List<LastFmArtist>,
    val user1Tracks: List<LastFmTrack>,
    val user2Tracks: List<LastFmTrack>,
    val sharedArtists: List<String>,
    val compatibilityScore: Int,
    val blendCode: String? = null
)

@HiltViewModel
class BlendViewModel @Inject constructor() : ViewModel() {

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val blendResult = MutableStateFlow<BlendResult?>(null)
    val savedBlendCode = MutableStateFlow<String?>(null)
    val joinedBlend = MutableStateFlow<BlendRecord?>(null)
    val isSaving = MutableStateFlow(false)
    val isJoining = MutableStateFlow(false)

    fun createBlend(user1: String, user2: String) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            blendResult.value = null

            val u1 = user1.trim()
            val u2 = user2.trim()

            if (u1.isBlank() || u2.isBlank()) {
                error.value = "Please enter both Last.fm usernames"
                isLoading.value = false
                return@launch
            }

            val user1ArtistsResult = LastFM.getUserTopArtists(u1, limit = 30)
            val user2ArtistsResult = LastFM.getUserTopArtists(u2, limit = 30)
            val user1TracksResult = LastFM.getUserTopTracks(u1, limit = 20)
            val user2TracksResult = LastFM.getUserTopTracks(u2, limit = 20)

            val user1Artists = user1ArtistsResult.getOrElse {
                error.value = "Could not fetch data for '$u1'. Check the username."
                isLoading.value = false
                return@launch
            }
            val user2Artists = user2ArtistsResult.getOrElse {
                error.value = "Could not fetch data for '$u2'. Check the username."
                isLoading.value = false
                return@launch
            }
            val user1Tracks = user1TracksResult.getOrDefault(emptyList())
            val user2Tracks = user2TracksResult.getOrDefault(emptyList())

            val u1Names = user1Artists.map { it.name.lowercase() }.toSet()
            val u2Names = user2Artists.map { it.name.lowercase() }.toSet()
            val shared = u1Names.intersect(u2Names)
            val sharedArtists = user1Artists.filter { it.name.lowercase() in shared }.map { it.name }

            val score = computeCompatibility(user1Artists, user2Artists, user1Tracks, user2Tracks)

            blendResult.value = BlendResult(
                user1 = u1,
                user2 = u2,
                user1Artists = user1Artists.take(10),
                user2Artists = user2Artists.take(10),
                user1Tracks = user1Tracks.take(10),
                user2Tracks = user2Tracks.take(10),
                sharedArtists = sharedArtists.take(5),
                compatibilityScore = score
            )
            isLoading.value = false
        }
    }

    fun saveToSupabase() {
        val result = blendResult.value ?: return
        viewModelScope.launch {
            isSaving.value = true
            val code = generateCode()
            val record = BlendRecord(
                code = code,
                user1_username = result.user1,
                user2_username = result.user2,
                user1_top_artists = result.user1Artists.joinToString(",") { it.name },
                user2_top_artists = result.user2Artists.joinToString(",") { it.name },
                compatibility_score = result.compatibilityScore.toFloat(),
                shared_artists = result.sharedArtists.joinToString(",")
            )
            val saved = SupabaseBlendClient.saveBlend(record)
            savedBlendCode.value = saved.getOrNull()?.code ?: code
            blendResult.value = result.copy(blendCode = savedBlendCode.value)
            isSaving.value = false
        }
    }

    fun joinBlend(code: String) {
        viewModelScope.launch {
            isJoining.value = true
            error.value = null
            val result = SupabaseBlendClient.fetchBlend(code.trim().uppercase())
            joinedBlend.value = result.getOrElse {
                error.value = "Could not find blend with code ${code.uppercase()}"
                null
            }
            isJoining.value = false
        }
    }

    private fun computeCompatibility(
        u1Artists: List<LastFmArtist>,
        u2Artists: List<LastFmArtist>,
        u1Tracks: List<LastFmTrack>,
        u2Tracks: List<LastFmTrack>
    ): Int {
        val u1ArtistNames = u1Artists.map { it.name.lowercase() }.toSet()
        val u2ArtistNames = u2Artists.map { it.name.lowercase() }.toSet()
        val artistOverlap = u1ArtistNames.intersect(u2ArtistNames).size.toFloat()
        val artistUnion = (u1ArtistNames + u2ArtistNames).size.toFloat()
        val jaccardArtist = if (artistUnion > 0) artistOverlap / artistUnion else 0f

        val u1TrackNames = u1Tracks.map { "${it.artist.name.lowercase()}:${it.name.lowercase()}" }.toSet()
        val u2TrackNames = u2Tracks.map { "${it.artist.name.lowercase()}:${it.name.lowercase()}" }.toSet()
        val trackOverlap = u1TrackNames.intersect(u2TrackNames).size.toFloat()
        val trackUnion = (u1TrackNames + u2TrackNames).size.toFloat()
        val jaccardTrack = if (trackUnion > 0) trackOverlap / trackUnion else 0f

        val topN = min(u1Artists.size, min(u2Artists.size, 10))
        val u1Top = u1Artists.take(topN).map { it.name.lowercase() }
        val u2Top = u2Artists.take(topN).map { it.name.lowercase() }
        val topNShared = u1Top.count { it in u2Top }.toFloat()
        val topNScore = if (topN > 0) topNShared / topN else 0f

        val raw = (jaccardArtist * 0.4f + jaccardTrack * 0.2f + topNScore * 0.4f) * 100f
        return (raw * 1.5f).coerceIn(10f, 99f).roundToInt()
    }
}
