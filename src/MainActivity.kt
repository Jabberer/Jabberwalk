package com.discobandit.app.jabberwalk

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.discobandit.app.jabberwalk.DataStructures.BookWithTracks
import com.discobandit.app.jabberwalk.UI.*
import com.discobandit.app.jabberwalk.Utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_book_player2.*
import org.jetbrains.anko.bundleOf

var numberToImport = ImportTracker{_,_,_ -> log{"Test"}}
var isPlayingNow: Boolean = false
var isCasting: Boolean = false
var currentBook: BookWithTracks? = null
var currentBookPosition: Int = 0
var currentFragment: Fragment? = null
class MainActivity : AppCompatActivity(), OnFragmentInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    lateinit var sharedPref: SharedPreferences
    private var controlsLocked = false
    val mMediaBrowser: MediaBrowserCompat by lazy{
        MediaBrowserCompat(this,
                ComponentName(applicationContext, MediaPlayerService::class.java),
                ConnectionCallback(this),
                null)
    }
/*    var castContext: CastContext? = null
    var castSession: CastSession? = null
    var sessionManager: SessionManager? = null
    var mediaClient: RemoteMediaClient? = null
    val sessionManagerListener = object: SessionManagerListener<Session>{
        override fun onSessionStarted(p0: Session?, p1: String?) {
        }

        override fun onSessionResumeFailed(p0: Session?, p1: Int) {
        }

        override fun onSessionSuspended(p0: Session?, p1: Int) {
        }

        override fun onSessionEnded(p0: Session?, p1: Int) {
        }

        override fun onSessionResumed(p0: Session?, p1: Boolean) {
        }

        override fun onSessionStarting(p0: Session?) {
        }

        override fun onSessionResuming(p0: Session?, p1: String?) {
        }

        override fun onSessionEnding(p0: Session?) {
        }

        override fun onSessionStartFailed(p0: Session?, p1: Int) {
        }
    }
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        //sessionManager = CastContext.getSharedInstance(this).sessionManager
        //castContext = CastContext.getSharedInstance(this)
        supportFragmentManager.fragments.clear()
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        sharedPref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        sharedPref.registerOnSharedPreferenceChangeListener(this)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MODIFY_AUDIO_SETTINGS), 100)
        supportFragmentManager.addOnBackStackChangedListener {
            if(supportFragmentManager.fragments.isNotEmpty()) currentFragment = supportFragmentManager.fragments[0]
            else this@MainActivity.finish()}
        super.onCreate(savedInstanceState)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.first() == PackageManager.PERMISSION_DENIED) throw SecurityException("You didnt give me permission")
        val chosen = sharedPref.getBoolean("path_chosen", false)
        log{"Chosen: " + chosen}
        if(!chosen) {
            val rootSelection = ChooseRoot()
            addFragment(rootSelection, R.id.fragmentContainer)
            val editor = sharedPref.edit()
            editor.putBoolean("path_chosen", true)
            editor.commit()
        }
            else{
                startService(Intent(applicationContext, MediaPlayerService::class.java))
                mMediaBrowser.connect()
            }
        }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "path") {
            log { "WHAT IS HAPPENING" }
            val intent = Intent(applicationContext, MediaPlayerService::class.java)
            stopService(intent)
            mMediaBrowser.disconnect()
            startService(intent)
            mMediaBrowser.connect()
        }
    }

    override fun onRootOptionSelected(view: View) {
        when (view.id) {
            R.id.root_choose_okay -> {
                performFileSearch()
            }
            R.id.root_choose_cancel -> {
                val editor = sharedPref.edit()
                editor.putString("path", "")
                editor.apply()
            }
        }
        removeFragment(supportFragmentManager.fragments.find{it is ChooseRoot}!!)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val rootSelection = ChooseRoot()
        addFragment(rootSelection, R.id.fragmentContainer)
        return true
    }

    override fun onBookSelected(mediaID: String, playOnly: Boolean, position: Int) {
        val isCurrentBook = mediaID == bookSelected?.mediaId
        currentBookPosition = position
        bookSelected = AppDataBase.getInstance(this).bookDao().loadBook(mediaID).toMediaItem()
        log{ currentBook?.tracks?.first()?.filePath ?: ""}
        val bookPlayer = MediaPlayerFragment()
        val isPlaying = MediaControllerCompat.getMediaController(this).playbackState.state == PlaybackStateCompat.STATE_PLAYING
        bookPlayer.isPlaying = isPlaying
        val bundle = Bundle()
        if(playOnly) {
                bundle.putBoolean("play", !isPlaying)
        }
        else{
            log{"This is where the transaction happens"}
            bundle.putBoolean("play", false)
            replaceFragment(bookPlayer, R.id.fragmentContainer)
        }
        if(!isCurrentBook || (playOnly && !isPlaying))MediaControllerCompat.getMediaController(this).transportControls.prepareFromMediaId(bookSelected?.mediaId, bundle)
        if(isCurrentBook && playOnly && isPlaying) MediaControllerCompat.getMediaController(this).transportControls.pause()
    }

    override fun obButtonClicked(view: View) {
        val m4b = currentBook?.tracks?.first()?.mime == "m4b"
        with(MediaControllerCompat.getMediaController(this).transportControls) {
                if(!controlsLocked && !isCasting) {
                    when (view.id) {
                        R.id.play -> playPause()
                        R.id.skip_next -> {
                            if(m4b) m4bNext()
                            else skipToNext()
                        }
                        R.id.skip_back -> {
                            if(m4b) m4bBack()
                            else skipToPrevious()}
                        R.id.forward_30 -> fastForward()
                        R.id.back_30 -> rewind()
                        R.id.equalizer -> if(eq != null) equalize()
                        R.id.speed_button -> speedChange()

                    }
                }
            if(view.id == R.id.lock) lock()
        }
    }
    fun m4bNext(){
        log{"BOOP"}
        val tracks = currentBook!!.tracks!!.sortedBy { it.duration }
        log{"${tracks.count()}"}
        for(it in tracks!!) {
            log { "${it.duration}" }
            if (it.duration > bookTracker.currentPosition && it.duration < currentBook!!.book!!.duration) {
                bookTracker.currentPosition = it.duration.toInt()
                onSeekbarChanged(it.duration.toInt())
                break
            }
        }
    }
    fun m4bBack(){
        log{"BOOP"}
        val tracks = currentBook!!.tracks!!.sortedBy { it.duration }.reversed()
        log{"${tracks.count()}"}
        for(it in tracks!!) {
            if (it.duration < (bookTracker.currentPosition - 1000) && it.duration >= 0) {
                bookTracker.currentPosition = it.duration.toInt()
                onSeekbarChanged(it.duration.toInt())
                break
            }
        }
    }
    private fun performFileSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, 11)
    }
    private fun equalize(){
        val eqPanel = BlankFragment()
        if(eq?.numberOfBands!! == 5.toShort())
        addFragment(eqPanel, R.id.eqPanel)
    }
    private fun speedChange(){
        val speedChanger = SpeedSelectionFragments()
        addFragment(speedChanger, R.id.eqPanel)
    }
    private fun lock(){
        controlsLocked = !controlsLocked
        val lock = supportFragmentManager.fragments.find { it is MediaPlayerFragment }!!.lock
        if(controlsLocked) lock.setImageResource(R.drawable.ic_lock_open_white_48dp )
        else lock.setImageResource(R.drawable.ic_lock_outline_white_48dp)
    }
    private fun playPause() {
        val fragment = supportFragmentManager?.fragments?.find { it is MediaPlayerFragment }
        fragment as MediaPlayerFragment
        fragment.isPlaying = !fragment.isPlaying
            with(MediaControllerCompat.getMediaController(this)) {
                when (playbackState.state) {
                    STATE_PLAYING -> {
                        transportControls.pause()
                    }
                    else -> {
                        transportControls.play()
                }
            }
        }
    }

    fun startBookSelectFragment(){
        mMediaBrowser.subscribe(mMediaBrowser.root, object: MediaBrowserCompat.SubscriptionCallback(){
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)
                val bookSelectGrid = SelectionGridFragment()
                children.sortBy { it.description.title.toString() }
                bookSelectGrid.bookList.addAll(children)
                replaceFragment(bookSelectGrid, R.id.fragmentContainer)
                with(MediaControllerCompat.getMediaController(this@MainActivity)) {
                    if (currentFragment is MediaPlayerFragment && bookSelected != null){
                        val mediaPlayer = MediaPlayerFragment()
                        mediaPlayer.isPlaying = true
                        val bundle = Bundle()
                        bundle.putBoolean("play", false)
                        if(!isPlayingNow) transportControls.prepareFromMediaId(bookSelected?.mediaId, bundle)
                        replaceFragment(mediaPlayer, R.id.fragmentContainer)
                    }
                }
            }
        })
    }

    fun iconSwitch(state: Int?){
        log{"Icons should be switching"}
        val playerRef = supportFragmentManager.fragments.find { it is MediaPlayerFragment }
        playerRef as MediaPlayerFragment?
        val gridRef = supportFragmentManager.fragments.find { it is SelectionGridFragment}
        gridRef as SelectionGridFragment?
        log{"gridRef is " + (gridRef == null)}
        val list = mutableListOf<Any>()
        when(state){
            PlaybackStateCompat.STATE_PLAYING -> {
                playerRef?.setPauseIcon()
                isPlayingNow = true
            }
            else -> {
                isPlayingNow = false
                playerRef?.setPlayIcon()
            }
        }
        gridRef?.notifyChange(currentBookPosition, list)
    }

    override fun onStop() {
        mMediaBrowser.disconnect()
        super.onStop()
    }

    override fun onDestroy() {
        mMediaBrowser.disconnect()
        super.onDestroy()
    }
    override fun onSeekbarChanged(progress: Int) {
        MediaControllerCompat.getMediaController(this).transportControls.seekTo(progress.toLong())
    }
    override fun onSpeedChanged(speed: Int){
        playbackSpeed = speed.toFloat()/10 + .5f
    }

    override fun onPause() {
     //   sessionManager?.removeSessionManagerListener(sessionManagerListener)
    //    castSession = null
        super.onPause()
    }
    override fun onResume() {
        if(isPlayingNow){
            val mediaFrag = supportFragmentManager.fragments.find{it is MediaPlayerFragment} ?: MediaPlayerFragment()
            mediaFrag as MediaPlayerFragment
            mediaFrag.isPlaying = true
            replaceFragment(mediaFrag, R.id.fragmentContainer)
        }
        super.onResume()
    }

    override fun onBackPressed() {
        if(currentFragment is SelectionGridFragment)
            finish()
        val frag = supportFragmentManager.fragments.find{it is BlankFragment || it is SpeedSelectionFragments || it is ChooseRoot}
        if(frag != null)
            removeFragment(frag)
        else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var importFragment = BookImportDial()
        addFragment(importFragment, R.id.fragmentContainer)
        var uri: Uri? = null
        val mContext = this
        numberToImport = ImportTracker { _, _, new -> run{
            removeFragment(importFragment)
            importFragment = BookImportDial()
            addFragment(importFragment,R.id.fragmentContainer)
        }
        }
        val task = object:AsyncTask<String, Int, Int>(){
            override fun doInBackground(vararg params: String?): Int {
                    if (data != null) {
                        uri = data.data
                        val path = FileUtil.getFullPathFromTreeUri(uri, mContext)
                        log { path.toString() }
                        val editor = sharedPref.edit()
                        editor.putString("path", path + "%")
                        editor.commit()
                        log { "PATH: " + sharedPref.getString("path", "") }
                    } else {
                        startService(Intent(applicationContext, MediaPlayerService::class.java))
                        mMediaBrowser.connect()
                    }
                    return 0
            }
        }
        task.execute("")
    }
    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {

    }

}
