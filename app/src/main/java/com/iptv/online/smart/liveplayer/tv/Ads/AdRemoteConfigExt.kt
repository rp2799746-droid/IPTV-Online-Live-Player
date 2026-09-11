package com.iptv.online.smart.liveplayer.tv.adsutils

import android.util.Log
import com.ads.module.ads.ERainAd


fun AdUnitConfig.canShowFullScreenAd(): Boolean {
    if (!isEnable) return false
    val organic = ERainAd.getInstance().organic      // getOrganic() (nullable)
    val switch = organic != true                     // non-organic = true, organic = false
    val show = enableUaCheck != switch               // XOR
    Log.d("OrganicCheck", "canShowFullScreenAd($id) ua=$enableUaCheck switch=$switch -> show=$show")
    return show
}
