package com.discobandit.app.jabberwalk.DataStructures

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata

/**
 * Created by Kaine on 2/3/2018.
 */
@Entity
data class Track(val position: String,
                 val bookID: String,
                 val title: String,
                 val duration: Long,
                 val filePath: String,
                 @PrimaryKey val id:  String,
                 val mime: String){
    val metadata: MediaDescriptionCompat
    get() = MediaDescriptionCompat.Builder().apply {
        setMediaUri(Uri.parse(filePath))
        setTitle(title)
        setMediaId(id)
        val bundle = Bundle()
        bundle.putLong("duration", duration)
        setExtras(bundle)
    }.build()
    val mediaInfo: MediaInfo
    get() = MediaInfo.Builder("http://techslides.com/demos/samples/sample.m4a").apply{
        setContentType(mime)
        setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
        setStreamDuration(duration)
        val metadata = MediaMetadata().apply{
            this.putString(MediaMetadata.KEY_TITLE, "Mouse")
        }
        setMetadata(metadata)
    }.build()
}