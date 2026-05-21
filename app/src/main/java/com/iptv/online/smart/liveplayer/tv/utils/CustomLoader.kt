package com.iptv.online.smart.liveplayer.tv.utils

import android.app.Activity
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import com.iptv.online.smart.liveplayer.tv.R

class CustomLoader(activity: Activity) {

    private val alertDialog: AlertDialog

    init {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.loader_dialog, null)
        builder.setView(view)

        alertDialog = builder.create().apply {
            window?.setBackgroundDrawable(ColorDrawable(0))
            setCancelable(false)
        }
    }

    fun show() {
        if (!alertDialog.isShowing) {
            alertDialog.show()
        }
    }

    fun dismiss() {
        if (alertDialog.isShowing) {
            alertDialog.dismiss()
        }
    }
}
