package com.discobandit.app.jabberwalk.Utils

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.discobandit.app.jabberwalk.MainActivity

/**
 * Created by Kaine on 2/12/2018.
 */
 class ConnectionCallback(val context: Context) : MediaBrowserCompat.ConnectionCallback(){
    override fun onConnected() {
        context as MainActivity? ?: return
        val mediaBrowser = context.mMediaBrowser
        super.onConnected()
        val token = mediaBrowser.sessionToken
        val mMediaController = MediaControllerCompat(context, token)
        mMediaController.registerCallback(object: MediaControllerCompat.Callback(){
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                super.onPlaybackStateChanged(state)
                context.iconSwitch(state?.state)
            }
        })
        MediaControllerCompat.setMediaController(context, mMediaController)
        context.startBookSelectFragment()
    }
}