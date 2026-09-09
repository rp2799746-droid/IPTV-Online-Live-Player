package com.iptv.online.smart.liveplayer.tv.adsutils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.ads.module.admob.Admob
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAdView


fun ERainAd.populateNativeAdView(
    activity: Activity,
    apNativeAd: ApNativeAd,
    adPlaceHolder: FrameLayout,
    containerShimmerLoading: ShimmerFrameLayout,
    ctaHeightInDp: Int = 40,
) {
    if (apNativeAd.admobNativeAd == null && apNativeAd.nativeView == null) {
        containerShimmerLoading.visibility = View.GONE
        return
    }

    val adView = LayoutInflater.from(activity)
        .inflate(apNativeAd.layoutCustomNative, null) as NativeAdView

    containerShimmerLoading.stopShimmer()
    containerShimmerLoading.visibility = View.GONE
    adPlaceHolder.visibility = View.VISIBLE

    Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.admobNativeAd, adView)
    adPlaceHolder.removeAllViews()
    adPlaceHolder.addView(adView)
}
