package com.discobandit.app.jabberwalk.UI

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.design.widget.FloatingActionButton
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.discobandit.app.jabberwalk.R
import com.discobandit.app.jabberwalk.R.id.*
import com.discobandit.app.jabberwalk.Utils.imageCache
import com.discobandit.app.jabberwalk.Utils.log
import com.discobandit.app.jabberwalk.bookTracker
import com.discobandit.app.jabberwalk.currentBook
import com.discobandit.app.jabberwalk.isPlayingNow
import kotlinx.android.synthetic.main.book_card.view.*
import org.jetbrains.anko.coroutines.experimental.bg
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Created by Kaine on 12/31/2017.
 */
class SelectionGridAdapter(val items: List<MediaBrowserCompat.MediaItem>, val listener: (MediaBrowserCompat.MediaItem, Boolean, Int) -> Unit): RecyclerView.Adapter<SelectionGridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.book_card, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position], listener, position, null)
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) = holder.bind(items[position], listener, position, payloads)
    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun bind(item: MediaBrowserCompat.MediaItem, listener: (MediaBrowserCompat.MediaItem, Boolean, Int) -> Unit, position: Int, payloads: MutableList<Any>?) = with(itemView) {
                log { "Grid has refreshed" }
                author.text = item.description.extras!!.getString("author")
                title.text = item.description.title
                progressBar.max = item.description.extras!!.getLong("duration").toInt()
                progressBar.progress = item.description.extras!!.getLong("time_stamp").toInt()
                floatingActionButton2.size = FloatingActionButton.SIZE_MINI
                if (currentBook?.book?.id == item.mediaId) {
                    progressBar.progress = bookTracker.currentPosition
                }
                if (isPlayingNow && currentBook?.book?.id == item.mediaId) {
                    floatingActionButton2.setImageResource(R.drawable.ic_pause_white_24dp)
                } else floatingActionButton2.setImageResource(R.drawable.ic_play_arrow_white_24dp)

                album_art.setImageResource(R.drawable.no_image)
                val file = File(context.filesDir, item.description.title.toString())
                if(file.exists() && imageCache[item.description.title.toString()] == null){
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    imageCache[item.description.title.toString()] = bitmap
                }
                if(imageCache[item.description.title.toString()] != null){
                    album_art.setImageBitmap(imageCache[item.description.title.toString()])
                }
                else if (item.description.iconUri != null) {
                    album_art.setImageBitmap(BitmapFactory.decodeFile(item.description.iconUri.toString()))
                }
                floatingActionButton2.setOnClickListener { listener(item, true, position) }
                setOnClickListener { listener(item, false, position) }
        }
    }
}