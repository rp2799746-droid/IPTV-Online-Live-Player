package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context


fun Context.isIntroFlowDone(): Boolean {
    return Preference.getBoolean(this, "isIntroFlowCompleted", false)
}

fun Context.setIntroFlowDone() {
    Preference.setBoolean(this, "isIntroFlowCompleted", true)
}

fun Context.isLanguageDone(): Boolean {
    return Preference.getBoolean(this, "isLanguageCompleted", false)
}

fun Context.setLanguageDone() {
    Preference.setBoolean(this, "isLanguageCompleted", true)
}
