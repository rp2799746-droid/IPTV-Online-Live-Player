package com.iptv.online.smart.liveplayer.tv.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View

const val THEME_DAY = "Light"
const val THEME_NIGHT = "Dark"
const val THEME_SYSTEM = "System_default"

fun View.triggerClick(onClick: (View?) -> Unit) {
    setOnClickListener {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onClick.invoke(it)
    }
}




inline fun <reified T : Activity> Context.openActivity(
    isNeedToClearTop: Boolean? = false,
    extras: Bundle? = null,
) {
    val intent = Intent(this, T::class.java)

    extras?.let { intent.putExtras(it) }

    if (isNeedToClearTop == true) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(intent)
}

inline fun <reified T : Activity> Context.openActivityWithoutPadding(
    isNeedToClearTop: Boolean? = false,
    extras: Bundle? = null,
) {
    val intent = Intent(this, T::class.java)

    extras?.let { intent.putExtras(it) }

    if (isNeedToClearTop == true) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
    startActivity(intent, options.toBundle())
}



