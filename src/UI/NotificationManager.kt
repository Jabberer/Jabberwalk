package com.discobandit.app.jabberwalk.UI

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.discobandit.app.jabberwalk.DataStructures.Book
import com.discobandit.app.jabberwalk.MainActivity
import com.discobandit.app.jabberwalk.MediaPlayerService
import com.discobandit.app.jabberwalk.R

/**
 * Created by Kaine on 2/13/2018.
 */
class NotificationManager{
    private lateinit var notificationBuilder: NotificationCompat.Builder
    val notification: Notification
        get() = notificationBuilder.build()

    fun buildNotification(context: Context, mediaSession: MediaSessionCompat, book: Book, paused: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 26) {
            val channelManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channelManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        notificationBuilder = NotificationCompat.Builder(context, "Jabberwalk").apply {
            setDefaults(0)
            setContentTitle(book.title)
            setContentText(book.author)
            setLargeIcon(BitmapFactory.decodeFile(book.album_art))
            setContentIntent(pendingIntent)
            setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, ComponentName(context, MediaPlayerService::class.java), PlaybackStateCompat.ACTION_STOP))
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.exo_edit_mode_logo)
            setSound(null)
            priority = NotificationManager.IMPORTANCE_LOW
            color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
            addAction(NotificationCompat.Action(
                    R.drawable.ic_replay_30_white_24dp, "back_30",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_REWIND)
            ))
            val playPauseIcon = if(paused)R.drawable.ic_play_arrow_white_24dp else R.drawable.ic_pause_white_24dp
            addAction(NotificationCompat.Action(
                    playPauseIcon, "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
            ))

            addAction(NotificationCompat.Action(
                    R.drawable.ic_forward_30_white_24dp, "forward_30",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_FAST_FORWARD)
            ))
            setStyle(
                    android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.sessionToken)
                            .setShowActionsInCompactView(1)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                                    ComponentName(context, MediaPlayerService::class.java),
                                    PlaybackStateCompat.ACTION_STOP))
            )
        }

    }

    companion object Channel {
        var channel: NotificationChannel? = null
        val channelID = "Jabberwalk"

        init {
            if (Build.VERSION.SDK_INT < 26) {
            } else {
                channel = NotificationChannel(channelID, "JabberwalkGrop", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "A Channel of Moose"
                    enableLights(true)
                    lightColor = Color.CYAN
                    enableVibration(false)
                    setSound(null, null)
                }
            }
        }
    }
}