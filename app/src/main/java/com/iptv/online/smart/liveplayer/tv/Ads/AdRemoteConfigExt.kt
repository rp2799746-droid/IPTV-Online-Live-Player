package com.iptv.online.smart.liveplayer.tv.adsutils

import android.util.Log
import com.ads.module.ads.ERainAd

/**
 * iptv2 jevu fullscreen native gate (SDK nu getShouldDisplay... aa project ma stub chhe,
 * etle custom XOR gate vaparyo):
 *   switch: non-organic = true, organic = false.   show = enable_ua_check XOR switch
 *     ua=false, organic     -> NO       ua=false, non-organic -> SHOW
 *     ua=true,  organic     -> SHOW     ua=true,  non-organic -> NO
 */
fun AdUnitConfig.canShowFullScreenAd(): Boolean {
    if (!isEnable) return false
    val organic = ERainAd.getInstance().organic      // getOrganic() (nullable)
    val switch = organic != true                     // non-organic = true, organic = false
    val show = enableUaCheck != switch               // XOR
    Log.d("OrganicCheck", "canShowFullScreenAd($id) ua=$enableUaCheck switch=$switch -> show=$show")
    return show
}
