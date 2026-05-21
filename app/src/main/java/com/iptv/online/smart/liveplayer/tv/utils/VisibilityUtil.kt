package com.iptv.online.smart.liveplayer.tv.utils

import android.view.View

val View.visible: Unit
    get() {
        this.visibility = View.VISIBLE
    }

val View.gone: Unit
    get() {
        this.visibility = View.GONE
    }

val View.invisible: Unit
    get() {
        this.visibility = View.INVISIBLE
    }
