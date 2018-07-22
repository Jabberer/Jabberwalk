package com.discobandit.app.jabberwalk.DataStructures

import android.arch.persistence.room.*
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat

/**
 * Created by Kaine on 12/28/2017.
 */
@Entity
data class Book(
                val id:  String,
                val title: String,
                val author: String,
                val album_art: String?,
                @PrimaryKey
                val duration: Long,
                val description: String?,
                val timeStamp: Long,
                val finished: Boolean
                ){
    override fun hashCode(): Int {
        return duration.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if(other == null || other !is Book) return false
        return duration == other.duration
    }

    fun toMediaItem(): MediaBrowserCompat.MediaItem{
        val bundle = Bundle().apply {
            putString("author", author)
            putLong("duration", duration)
            putLong("time_stamp", timeStamp)
        }
        val metadata = MediaDescriptionCompat.Builder().apply {
            setTitle(title)
            setMediaId(id)
            if(!album_art.isNullOrBlank()) setIconUri(Uri.parse(album_art))
            setDescription(description)
            setExtras(bundle)
        }.build()
        return MediaBrowserCompat.MediaItem(metadata,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE or
                        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }
}