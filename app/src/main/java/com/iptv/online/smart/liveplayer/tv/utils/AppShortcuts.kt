package com.iptv.online.smart.liveplayer.tv.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.iptv.online.smart.liveplayer.tv.Activity.MainActivity
import com.iptv.online.smart.liveplayer.tv.Activity.MirrorStepsActivity
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.R

object AppShortcuts {

    fun initShortCut(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val manager = context.getSystemService(ShortcutManager::class.java)
            manager.removeAllDynamicShortcuts()

            // 1. Stream TV
            val streamtv = ShortcutInfo.Builder(context, "stream_id")
                .setShortLabel(context.getString(R.string.stream_your_tv))
                .setIcon(Icon.createWithResource(context, R.drawable.stream_tv))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("widget_flow", "flow_iptv")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                })
                .build()

            // 2. IPTV Smart
            val mirrorShortcut = ShortcutInfo.Builder(context, "mirror_id")
                .setShortLabel(context.getString(R.string.iptv_smart))
                .setIcon(Icon.createWithResource(context, R.drawable.iptv_wid))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("ACTION_TYPE", "OPEN_IPTV")
                    putExtra("widget_flow", "flow_mirror")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                })
                .build()

            // 3. Cast to TV
            val castShortcut = ShortcutInfo.Builder(context, "cast_id")
                .setShortLabel(context.getString(R.string.cast_to_tv))
                .setIcon(Icon.createWithResource(context, R.drawable.cast_wiget))
                .setIntent(Intent(context, MirrorStepsActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("widget_flow", "flow_tv")
                })
                .build()

            // 4. Uninstall
            val uninstallShortcut = ShortcutInfo.Builder(context, "uninstall_id")
                .setShortLabel(context.getString(R.string.uninstall))
                .setIcon(Icon.createWithResource(context, R.drawable.uninstall_wid))
                .setIntent(Intent(context, SplashActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("widget_flow", "flow_uninstall")
                })
                .build()

            manager.dynamicShortcuts = listOf(
                streamtv,
                mirrorShortcut,
                castShortcut,
                uninstallShortcut
            )
        } catch (e: Exception) {
            Log.e("AppShortcuts", "initShortCut error: ${e.message}")
        }
    }
}