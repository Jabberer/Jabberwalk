package com.discobandit.app.jabberwalk.Utils

import android.content.Context

import com.discobandit.app.jabberwalk.R
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider

/**
 * Created by Kaine on 2/18/2018.
 */

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        log{"What de fucks"}
        return CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.app_id))
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}