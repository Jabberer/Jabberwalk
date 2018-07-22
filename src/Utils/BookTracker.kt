package com.discobandit.app.jabberwalk.Utils

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

/**
 * Created by Kaine on 2/12/2018.
 */
class BookTracker(lam: (prop: KProperty<*>, old: Int, new: Int) -> Unit) {
    var currentPosition: Int by Delegates.observable(0, lam)
}