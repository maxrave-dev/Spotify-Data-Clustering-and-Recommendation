package com.example.musicrecommend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendModel(
    val tracks: List<Track>
) {
    @Serializable
    data class Track(
        val id: String,
        val name: String,
        val artists: List<Artist>?,
        val album: Album?,
        @SerialName("preview_url")
        val previewUrl: String?
    ) {
        @Serializable
        data class Artist(
            val name: String
        )
        @Serializable
        data class Album(
            val images: List<Image>?
        ) {
            @Serializable
            data class Image(
                val url: String
            )
        }
    }
}