package com.discobandit.app.jabberwalk.DataStructures

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation
import android.content.Context
import android.net.Uri
import com.discobandit.app.jabberwalk.Utils.log
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ClippingMediaSource
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

/**
 * Created by Kaine on 2/3/2018.
 */
data class BookWithTracks(
        @Embedded
        var book: Book? = null,
        @Relation(parentColumn = "id", entityColumn = "bookID", entity = Track::class)
        var tracks: List<Track>? = null){

    fun toMediaSource(context: Context): ConcatenatingMediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(context, "ua")
        val mime = tracks?.first()?.mime
        val sourceList = mutableListOf<ExtractorMediaSource>()
        if(mime == "m4b") {
            log{"Number of tracks" + tracks!!.count().toString()}
            sourceList.add(ExtractorMediaSource(Uri.parse(tracks!!.first().filePath), dataSourceFactory, DefaultExtractorsFactory(), null, null))
        }
        else {
            sourceList.addAll(tracks!!.map { ExtractorMediaSource(Uri.parse(it.filePath), dataSourceFactory, DefaultExtractorsFactory(), null, null) })
        }
        return ConcatenatingMediaSource(true, *sourceList.toTypedArray())
    }
}