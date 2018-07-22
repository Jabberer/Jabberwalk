package com.discobandit.app.jabberwalk.UI

import android.arch.persistence.room.*
import android.content.Context
import com.discobandit.app.jabberwalk.DataStructures.Book
import com.discobandit.app.jabberwalk.DataStructures.BookWithTracks
import com.discobandit.app.jabberwalk.DataStructures.SingletonHolder
import com.discobandit.app.jabberwalk.DataStructures.Track
import com.discobandit.app.jabberwalk.Utils.log

/**
 * Created by Kaine on 2/3/2018.
 */
@Dao
interface BookDao{
    //Insert a single book
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertBook(book: Book)
    //Insert a List of books
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBooks(books: List<Book>)
    //Update a book's data in the database
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertTrack(track: Track)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTracks(tracks: List<Track>)
    @Query("UPDATE Book SET timeStamp= :timeStamp WHERE id= :id") fun updateTimeStamp(id: String, timeStamp: Long)
    //Delete a single book
    @Update fun updateBook(book: Book)
    @Delete fun deleteBook(book: Book)
    //Delete a list of books
    @Update fun updateBooks(books: List<Book>)
    @Delete
    fun deleteBooks(books: List<Book>)
    @Delete
    fun deleteTracksFromBook(tracks: List<Track>)
    @Query("SELECT * from Book where id = :id LIMIT 1") fun getBookWithTracks(id: String): BookWithTracks
    @Query("SELECT * from Book where id = :id LIMIT 1") fun loadBook(id: String): Book
    @Query("SELECT * FROM Book")
    fun loadAllBooksWithTracks(): List<BookWithTracks>
    @Query("SELECT * FROM Book")
    fun loadAllBooks(): List<Book>
    @Query("SELECT * FROM Track")
    fun loadAllTracks(): List<Track>
    @Query("SELECT * FROM Track WHERE bookID=:id") fun getBookTracks(id: String): List<Track>
    //Returns all books by a given author
}

fun BookDao.update(booksToUpdate: List<Book>){
    val books = loadAllBooks()
    if(booksToUpdate.isEmpty()) return
    val updatedbooks = booksToUpdate.map{
        val id = it.id
        val timeStamp = it.timeStamp
        books.find { it.id == id }?.copy(timeStamp = timeStamp) ?: return
    }
    updateBooks(updatedbooks)
}

@Database(entities = [Book::class, Track::class], version = 12)
abstract class AppDataBase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object : SingletonHolder<AppDataBase, Context>({
        Room.databaseBuilder(it.applicationContext,
                AppDataBase::class.java, "books.db").allowMainThreadQueries().fallbackToDestructiveMigration()
                .build()
    })
}