package com.music.lastfm.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserTopArtistsResponse(
    val topartists: TopArtistsData
)

@Serializable
data class TopArtistsData(
    val artist: List<LastFmArtist> = emptyList()
)

@Serializable
data class LastFmArtist(
    val name: String = "",
    val playcount: String = "0",
    val url: String = "",
    val image: List<LastFmImage> = emptyList()
) {
    val playcountInt: Int get() = playcount.toIntOrNull() ?: 0
}

@Serializable
data class LastFmImage(
    @SerialName("#text") val url: String = "",
    val size: String = ""
)

@Serializable
data class UserTopTracksResponse(
    val toptracks: TopTracksData
)

@Serializable
data class TopTracksData(
    val track: List<LastFmTrack> = emptyList()
)

@Serializable
data class LastFmTrack(
    val name: String = "",
    val playcount: String = "0",
    val url: String = "",
    val artist: LastFmArtist = LastFmArtist(),
    val image: List<LastFmImage> = emptyList()
) {
    val playcountInt: Int get() = playcount.toIntOrNull() ?: 0
}
