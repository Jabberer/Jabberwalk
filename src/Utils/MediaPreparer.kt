package com.discobandit.app.jabberwalk.Utils

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Albums.ALBUM
import android.provider.MediaStore.Audio.Albums.ALBUM_ART
import android.provider.MediaStore.Audio.Albums.ARTIST
import android.provider.MediaStore.Audio.Media.ALBUM_ID
import android.provider.MediaStore.Audio.Media.DATA
import android.provider.MediaStore.Audio.Media.DURATION
import android.provider.MediaStore.MediaColumns.MIME_TYPE
import android.support.v4.media.MediaBrowserCompat
import com.discobandit.app.jabberwalk.DataStructures.Book
import com.discobandit.app.jabberwalk.DataStructures.BookWithTracks
import com.discobandit.app.jabberwalk.DataStructures.Track
import com.discobandit.app.jabberwalk.R.id.*
import com.discobandit.app.jabberwalk.UI.AppDataBase
import com.discobandit.app.jabberwalk.UI.update
import com.discobandit.app.jabberwalk.numberToImport
import org.jetbrains.anko.coroutines.experimental.bg
import wseemann.media.FFmpegMediaMetadataRetriever
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI as ALBUM_URI
import android.provider.MediaStore.Audio.Albums._ID as BOOK_ID
import android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI as TRACK_URI
import android.provider.MediaStore.Audio.Media.TITLE as TRACK
import android.provider.MediaStore.Audio.Media.TRACK as POSITION
import android.provider.MediaStore.Audio.Media._ID as TRACK_ID
val imageCache = mutableMapOf<String, Bitmap>()

class MediaPreparer(private val context: Context) {
	val mp4FromSearch = mutableListOf<BookWithTracks>()
	val bookList: MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()
	val db = mutableListOf<BookWithTracks>()
	var path = ""
	fun populate() {
		fillDatabase()
		refresh()
	}

	fun refresh(){
		db.clear()
		val books = AppDataBase.getInstance(context).bookDao().loadAllBooksWithTracks()
		books.forEach{
			val sortedTracks = mutableListOf<Track>()
			sortedTracks.addAll(it.tracks!!)
			sortedTracks.sortBy{it.position.toInt()}
			it.tracks = sortedTracks
		}
		db.addAll(books)
		bookList.clear()
		bookList.addAll(booksToMediaItems(db))
	}

	fun updateTimeStamp(position: Long, book: Book){
		AppDataBase.getInstance(context!!).bookDao().updateBook(book.copy(timeStamp = position))
	}


	/*
	Load all of the albums from the Android MediaStore
	 */
	private fun fillDatabase() {
		val cursor = getAlbumsFromStore()
		val trackCursor = getAllTracks()
		val tracks = with(trackCursor) {
			getMediaInfo(7)
					.map { Track(position = it[0], bookID = it[1], title = it[2], duration = it[3].toLong(), filePath = it[4], id = it[5], mime = it[6]) }
		}
		val books: List<Book> = with(cursor) {
			getMediaInfo(4)
					.map {
						val mediaID = it[0]
						Book(id = it[0], title = it[1], author = it[2], album_art = it[3], duration = tracks.filter { it.bookID == mediaID }.sumBy { it.duration.toInt() }.toLong(), description = "", timeStamp = 0, finished = false)
					}
		}
		/*val task = object: AsyncTask<String, Int, List<BookWithTracks>>() {
			override fun doInBackground(vararg params: String?): List<BookWithTracks> {
				return getMP4Files(path)
			}

			override fun onPostExecute(result: List<BookWithTracks>?) {
				log{"HEEEYEYEYEYEYEYEYE"}
				val booksFromSearch = result!!.map{it.book!!}
				if(booksFromSearch.isEmpty()) log{"List is empty for some reason"}
				else log{"List is ${booksFromSearch.count()}"}
				val tracksFromSearch = result!!.map{it.tracks!!}.flatten()
				dBdifferences(books.filter{it.duration > 0},booksFromSearch, tracks, tracksFromSearch)

			}
		}
		task.execute("")
		*/
		val result = getMP4Files(path)
		log{"HEEEYEYEYEYEYEYEYE"}
		val booksFromSearch = result!!.map{it.book!!}
		if(booksFromSearch.isEmpty()) log{"List is empty for some reason"}
		else log{"List is ${booksFromSearch.count()}"}
		val tracksFromSearch = result!!.map{it.tracks!!}.flatten()
		dBdifferences(books.filter{it.duration > 0},booksFromSearch, tracks, tracksFromSearch)
	}

	private fun booksToMediaItems(books: List<BookWithTracks>) = books.map { it.book!!.toMediaItem() }

	fun getMetadata(tracks: List<Track>) = tracks.map { it.metadata }
	//Generate a cursor with the album info
	private fun getAlbumsFromStore(): Cursor {
		val projection: Array<String> = arrayOf(
				BOOK_ID,
				ALBUM,
				ARTIST,
				ALBUM_ART)
		val sortOrder = MediaStore.Audio.Media.ALBUM
		return context.contentResolver.query(ALBUM_URI, projection, "", null, sortOrder)
	}
	//Generate a cursor with the track info
	private fun getAllTracks(): Cursor {
		path = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)?.getString("path", "")!!
		val args = if(path.isNullOrBlank()) null else arrayOf(path)
		val selection = if(args != null) MediaStore.Audio.Media.DATA + " like ? " else ""
		val projection = arrayOf(
				POSITION,
				ALBUM_ID,
				TRACK,
				DURATION,
				DATA,
				TRACK_ID,
				MIME_TYPE)
		val sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC"
		return context.contentResolver.query(TRACK_URI, projection, selection, args, sortOrder)
	}
	//Calculate differences between internal DB and the media store and update accordingly
	private fun dBdifferences(booksFromStore: List<Book>, booksFromSearch: List<Book>, tracksFromStore: List<Track>, tracksFromSearch: List<Track>) {
		AppDataBase.getInstance(context).bookDao().apply {
			val booksFromDB = loadAllBooks()
			val tracksFromDB = loadAllTracks()
			val booksToDelete = booksFromDB - booksFromStore - booksFromSearch
			val tracksToDelete = tracksFromDB - tracksFromStore - tracksFromSearch
			val booksToUpdate = booksFromDB - booksToDelete
			val tracksToAdd = (tracksFromStore + tracksFromSearch) - tracksFromDB
			insertBooks(booksFromStore + booksFromSearch)
			insertTracks(tracksToAdd)
			deleteBooks(booksToDelete)
			deleteTracksFromBook(tracksToDelete)
			update(booksToUpdate)
		}
	}

	fun getMP4Files(path: String?): List<BookWithTracks>{
				val books = AppDataBase.getInstance(context).bookDao().loadAllBooksWithTracks()
				mp4FromSearch.clear()
				val f = if(path!!.isNotEmpty()) File(path!!.substring(0, (path!!.length)-1))
				else Environment.getExternalStorageDirectory()
				val m4bPaths = mutableListOf<String>()
				recursiveScan(f, m4bPaths)
				val m4bSet = m4bPaths.toSet()
				val mmr = FFmpegMediaMetadataRetriever()
				val m4bBooks = mutableListOf<BookWithTracks>()
				val stepsize =	if(m4bSet.count() == 0) 100 else 100/m4bSet.count()
				 for(path in m4bSet){
					 numberToImport.booksLeft += stepsize
					 log{"numberToImport ${numberToImport.booksLeft} stepSize = $stepsize"}
                     val bookFound = books.find{it!!.book!!.id == path}
					 if(bookFound == null)
						try {
							mmr.setDataSource(path)
							val book = Book(
								id = path,
								title = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE),
								author = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST),
								album_art = path,
								description = "",
								duration = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION).toLong(),
								timeStamp = 0,
								finished = false
							)
							val file = File(context.filesDir, book.title)
							if(!file.exists()) {
								val rawAlbumArt = mmr.embeddedPicture
								saveBitmap(book.title, rawAlbumArt)
							}
							val bookWithTracks = BookWithTracks()
							bookWithTracks.book = book
							val chapters = mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_CHAPTER_COUNT).toInt()
							val tracks = mutableListOf<Track>()
							log{"Number of ChAPTAS $chapters"}
							for (chapter in 0 until chapters) {
								log{"Track Number $chapter"}
								val track = Track(
									position = (chapter + 1).toString(),
									duration = mmr.extractMetadataFromChapter(FFmpegMediaMetadataRetriever.METADATA_KEY_CHAPTER_START_TIME, chapter).toLong(),
									title = "",
									filePath = path,
									id = path + chapter,
									mime = "m4b",
									bookID = path
							)
							tracks.add(track)
						}
						bookWithTracks.tracks = tracks
						m4bBooks.add(bookWithTracks)
					}
					catch(e: Exception){
						log{"Failure on $path"}
					}
                     else {
						 m4bBooks.add(bookFound)
					 }
					 }
			for(it in m4bBooks){
				log{"m4b Book name is ${it.book!!.title}"}
				log{"Book has ${it.tracks!!.count()}"}
			}
			return m4bBooks
			}

	fun recursiveScan(f: File, list: MutableList<String>){
		val file = f.listFiles()
		if(file == null) return
		for(it in file){
			if(it.isDirectory) recursiveScan(it, list)
			if(it.isFile && it.path.toLowerCase().endsWith(".m4b")) list.add(it.path)
		}
	}
	fun saveBitmap(name: String, rawImage: ByteArray){
			log { "Starting save process" }
			val bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.size)
			val newFile = File(context.filesDir, name)
			val os = FileOutputStream(newFile)
			imageCache[name] = bitmap
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
			log { "File Saved: ${newFile.path}" }
			os.close()
	}
}

//Helper function for cursor weirdness
fun Cursor.getMediaInfo(size: Int): List<List<String>> {
	val listOfMetadata = mutableListOf<List<String>>()
	while(!this.isLast) {
		try {
			val metadata = mutableListOf<String>()
			this.moveToNext()
			(0 until size)
					.map { this.getString(it) }
					.forEach {
						if (it != null) metadata.add(it)
						else metadata.add("")
					}
			listOfMetadata.add(metadata)
		} catch(ex: Exception){
			break
		}
	}
	return listOfMetadata
}

