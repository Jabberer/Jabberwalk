package com.discobandit.app.jabberwalk.Utils

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ImportTracker(lam: (prop: KProperty<*>, old: Int, new: Int) -> Unit) {
    var booksLeft: Int by Delegates.observable(0, lam)
}