package com.iptv.online.smart.liveplayer.tv.adsutils

import android.app.Activity
import android.util.Log
import android.widget.FrameLayout
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

object NativeAdManager {

    private const val TAG_APP = "NativeAdManager"

    // Store multiple native ads by TAG
    private val adMap = HashMap<String, ApNativeAd?>()

    // ===============================
    //  PRELOAD AD WITH TAG
    // ===============================
    fun preloadNativeAd(
        activity: Activity,
        tag: String,              // unique for each activity
        adId: String,
        layoutId: Int,
    ) {
        Log.d(TAG_APP, "Preloading native ad for $tag")

        ERainAd.getInstance().loadNativeAdResultCallback(
            activity,
            adId,
            layoutId,
            object : AdCallback() {

                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    adMap[tag] = nativeAd
                    Log.d(TAG_APP, "Loaded native ad for $tag")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    adMap[tag] = null
                    Log.e(TAG_APP, "Failed loading native ad for $tag: ${i?.message}")
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    adMap[tag] = null
                    Log.e(TAG_APP, "Failed showing native ad for $tag: ${adError?.message}")
                }
            }
        )
    }

    // ===============================
    // SHOW AD BASED ON TAG
    // ===============================
    fun showNativeAd(
        activity: Activity,
        tag: String,
        container: FrameLayout,
        shimmer: ShimmerFrameLayout,
    ) {
        val ad = adMap[tag]

        if (ad != null) {
            ERainAd.getInstance().populateNativeAdView(
                activity,
                ad,
                container,
                shimmer
            )
            Log.d(TAG_APP, "Showing native ad for $tag")
            adMap[tag] = null   // clear after use if you want
        } else {
            Log.d(TAG_APP, "No preloaded ad available for $tag")
        }
    }

    // OPTIONAL: Clear specific tag
    fun clear(tag: String) {
        adMap[tag] = null
    }

    // OPTIONAL: Clear all ads
    fun clearAll() {
        adMap.clear()
    }
}