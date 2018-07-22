package com.discobandit.app.jabberwalk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserServiceCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import com.discobandit.app.jabberwalk.DataStructures.BookWithTracks
import com.discobandit.app.jabberwalk.UI.NotificationManager
import com.discobandit.app.jabberwalk.Utils.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import org.jetbrains.anko.coroutines.experimental.bg
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import kotlin.reflect.KProperty

var eq: Equalizer? = null
val eqProfile: MutableList<Short> = mutableListOf(300,0,0,0,300)
val bandFreqs = mutableListOf<Int>()
var bookTracker = BookTracker { prop: KProperty<*>, old: Int, new: Int -> log { "Not working" } }
var bookSelected: MediaBrowserCompat.MediaItem? = null
var playbackSpeed = 1f
class MediaPlayerService : MediaBrowserServiceCompat(){
    val ROOTID = "ROOTID"
    val queue = mutableListOf<MediaDescriptionCompat>()
    val mediaPreparer by lazy{ MediaPreparer(this) }
    val player: SimpleExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
    }
    val notificationManager = NotificationManager()
    private val executorService = Executors.newSingleThreadScheduledExecutor()
    private var scheduleFuture: ScheduledFuture<*>? = null
    private var isRegistered = false
    private var handler = Handler()
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private lateinit var noisyReciver: BroadcastReceiver
    lateinit var mMediaSession: MediaSessionCompat
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val keyEvent = intent?.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if(keyEvent?.action == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) requestAudio()
        else
        MediaButtonReceiver.handleIntent(mMediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener{
        when(it){
            AudioManager.AUDIOFOCUS_GAIN -> {
                if(Lock.resumePlayback || Lock.delayedPlayback)
                    player.playWhenReady = true
                Lock.delayedPlayback = false
                showNotification()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Lock.resumePlayback = false
                Lock.delayedPlayback = false
                pause()
                }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->{
                Lock.resumePlayback = player.playWhenReady
                Lock.delayedPlayback = false
                player.playWhenReady = false
                pauseNotification()
            }

        }
    }
    lateinit var am: AudioManager
    var focusRequest: AudioFocusRequest? = null
    private fun requestAudio(){
        startService(Intent(this@MediaPlayerService, MediaPlayerService::class.java))
        val attributes = com.google.android.exoplayer2.audio.AudioAttributes.Builder().apply {
            setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            setUsage(AudioAttributes.USAGE_MEDIA)
        }.build()
        val result = if(Build.VERSION.SDK_INT >= 26) {
            val _attributes = AudioAttributes.Builder().apply {
                setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                setUsage(AudioAttributes.USAGE_MEDIA)
            }.build()
            player.audioAttributes = attributes
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).apply {
                setAudioAttributes(_attributes)
                setAcceptsDelayedFocusGain(true)
                setWillPauseWhenDucked(true)
                setOnAudioFocusChangeListener(audioFocusChangeListener)
            }.build()
            am.requestAudioFocus(focusRequest)
        } else {
            am.requestAudioFocus(audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN)
            }
            when (result) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> player?.playWhenReady = true
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> Lock.delayedPlayback = false
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> Lock.delayedPlayback = true
            }
        if(player.playWhenReady){
            if(!isRegistered)
            registerReceiver(noisyReciver, intentFilter)
            isRegistered = true
            scheduleDBupdate()

        }
        }
    private fun pause(){
        if(isRegistered) {
            unregisterReceiver(noisyReciver)
            isRegistered = false
        }
        stopDBUpdate()
        stopForeground(false)
        pauseNotification()
        player.playWhenReady = false
        isPlayingNow = false
    }
    var book: BookWithTracks? = null
        override fun onCreate() {
            super.onCreate()
            noisyReciver =             object: BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?) {
                    //if(isRegistered) {
                     //   unregisterReceiver(noisyReciver)
                     //   isRegistered = false
                   // }
                    pause()
                    pause()
                }
            }
            am = this@MediaPlayerService.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            player.setAudioDebugListener(object: AudioRendererEventListener{
                override fun onAudioEnabled(counters: DecoderCounters?) {
                }

                override fun onAudioInputFormatChanged(format: Format?) {
                }

                override fun onAudioDecoderInitialized(decoderName: String?, initializedTimestampMs: Long, initializationDurationMs: Long) {
                }

                override fun onAudioDisabled(counters: DecoderCounters?) {
                }

                override fun onAudioSinkUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                }

                override fun onAudioSessionId(audioSessionId: Int) {
                    if(eq!= null) eq?.release()
                    eq = Equalizer(0, audioSessionId)
                    eq ?: return
                    eq?.enabled = true
                    bandFreqs.clear()
                    eqProfile.withIndex().forEach{eq?.setBandLevel(it.index.toShort(),it.value)}
                    (0 until eq!!.numberOfBands).forEach{log{"" + eq?.properties!!.bandLevels[it]}}
                    for(it in eq!!.bandLevelRange){
                    }
                }
            })
            player.playbackParameters = PlaybackParameters(playbackSpeed, 1f)
            /*
            Set up the media session, set the flags, and hook it into the MediaSessionConnector. The Connector
            removes any need for a callback generic mathod. It handles all of the communication between the MediaBrowser and the ExoPlayer instance
             */
            mediaPreparer.populate()
            mMediaSession = MediaSessionCompat(this, "Logger")
            mMediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            sessionToken = mMediaSession.sessionToken
            val mMediaSessionConnector = MediaSessionConnector (mMediaSession,
                    object: DefaultPlaybackController() {
                        var isRegistered = false
                        override fun onPlay(player: Player?) {
                            requestAudio()
                            showNotification()
                            isPlayingNow = true
                        }
                        override fun onStop(player: Player?) {
                            if(Build.VERSION.SDK_INT < 26)
                            am.abandonAudioFocus(audioFocusChangeListener)
                            else
                            am.abandonAudioFocusRequest(focusRequest)
                            stopDBUpdate()
                            pauseNotification()
                            stopSelf()
                            player?.release()
                            if(isRegistered) {
                                unregisterReceiver(noisyReciver)
                                isRegistered = false
                            }
                            bookSelected = null
                            super.onStop(player)
                            eq?.release()
                            isPlayingNow = false
                        }

                        override fun onPause(player: Player?) {
                            pause()
                            super.onPause(player)
                        }

                        override fun onSeekTo(player: Player?, position: Long) {
                            if(currentBook!!.tracks!!.first().mime  == "m4b")
                                player!!.seekTo(position)
                            else {
                                val window = getPositionPair(queue, position)
                                player!!.seekTo(window.first, window.second)
                            }
                            bookTracker.currentPosition = position.toInt()

                        }

                        override fun onFastForward(player: Player?) {
                            player?.seekTo(player.currentPosition+30000)
                        }

                        override fun onRewind(player: Player?) {
                            val newPos = (player?.currentPosition ?: 30000) - 30000
                            if(newPos <= 0) onSeekTo(player, -30000L + bookTracker.currentPosition)
                            else player?.seekTo(newPos)
                        }

                        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
                            if(command == "speed"){
                                val adjustedSpeed: Float = extras?.getInt("speed")!!.toFloat()/10+.5f
                                player?.playbackParameters = PlaybackParameters(adjustedSpeed,1f)
                            }
                            super.onCommand(player, command, extras, cb)
                        }
                    }
                    , true)
            mMediaSessionConnector.setPlayer(player, object: MediaSessionConnector.PlaybackPreparer{
                override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
                }
                override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {
                }

                override fun getSupportedPrepareActions(): Long {
                    val supportedCommands: Long = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                    return supportedCommands
                }

                override fun getCommands(): Array<String> {
                    return emptyArray<String>()
                }

                override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                    mediaPreparer.refresh()
                    book = mediaPreparer.db.find { it.book!!.id == mediaId }
                    currentBook = book
                    val mediaSource = book!!.toMediaSource(this@MediaPlayerService)
                    queue.clear()
                    queue.addAll(mediaPreparer.getMetadata(book!!.tracks!!))
                    player.prepare(mediaSource)
                    if (book!!.book!!.timeStamp != 0L) {
                        if (currentBook?.tracks?.first()?.mime == "m4b")
                            player.seekTo(book!!.book!!.timeStamp)
                        else {
                            val coords = getPositionPair(queue, book!!.book!!.timeStamp)
                            player.seekTo(coords.first, coords.second)
                        }
                    }
                    if(extras?.getBoolean("play") ?: false){
                        requestAudio()
                    }
                    if(player.playWhenReady) {
                        showNotification()
                        scheduleDBupdate()
                    }
                }

                override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
                }

                override fun onPrepare() {
                }


            })
            val queueNavigator = object: TimelineQueueNavigator(mMediaSession){
                override fun getMediaDescription(windowIndex: Int): MediaDescriptionCompat {
                    return queue[windowIndex]
                }

                override fun onSkipToNext(player: Player?) {
                    log{"BOOP"}
                        super.onSkipToNext(player)
                        bookTracker.currentPosition = getPositionMS(queue, 0L, player!!.currentWindowIndex).toInt()
                    }

                override fun onSkipToPrevious(player: Player?) {
                    bookTracker.currentPosition = getPositionMS(queue, 0L, player!!.currentWindowIndex).toInt()
                    super.onSkipToPrevious(player)
                }
            }
            mMediaSessionConnector.setQueueNavigator(queueNavigator)
        }
        override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
            if(parentId == ROOTID){
                result.detach()
                result.sendResult(mediaPreparer.bookList)}
            else{
                result.detach()
                result.sendResult(queue.map{MediaBrowserCompat.MediaItem(it, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)}.toMutableList())
            }
            //else result.sendResult(mediaPreparer.getTracks(parentID))
        }

    private fun updateDB(){
        if(player.playbackParameters.speed != playbackSpeed) player.playbackParameters = PlaybackParameters(playbackSpeed, 1f)
        val time = player.currentPosition
        val window = player.currentWindowIndex
        bookTracker.currentPosition = getPositionMS(queue, time, window).toInt()
        mediaPreparer.updateTimeStamp(bookTracker.currentPosition.toLong(), book!!.book!!)
    }


    private fun scheduleDBupdate() {
        stopDBUpdate()
        if (!executorService.isShutdown) {
            scheduleFuture = executorService.scheduleAtFixedRate(
                    { handler.post({ updateDB() }) }, 100,
                    100, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
    }

    private fun stopDBUpdate() {
        if (scheduleFuture != null) {
            scheduleFuture!!.cancel(false)
        }
    }
    private fun pauseNotification(){
        bg{
        notificationManager.buildNotification(this, mMediaSession, book?.book!!, true)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(1235, notificationManager.notification)
        }
    }
    private fun showNotification(){
        bg {
            notificationManager.buildNotification(this@MediaPlayerService, mMediaSession, book!!.book!!)
            startForeground(1235, notificationManager.notification)
        }
    }
    override fun onDestroy() {
    }
        override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
            return BrowserRoot(ROOTID,null)
        }
    }