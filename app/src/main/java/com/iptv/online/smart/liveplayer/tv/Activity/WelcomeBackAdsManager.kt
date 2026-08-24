package com.iptv.online.smart.liveplayer.tv.Activity

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable


object WelcomeBackAdsManager {

    private const val TAG = "WelcomeFlow"
    private var interAd: ApInterstitialAd? = null

    fun preload(context: Context) {
        val config = RemoteConfigdata(context)
        if (!config.isNeedToShowADs || !AdRemoteConfig.getInstance().inter_welcome_back.isEnable) {
            Log.d(TAG, "preload skipped: ads/config off")
            return
        }
        if (interAd != null) {
            Log.d(TAG, "preload skipped: already loaded")
            return
        }
        ERainAd.getInstance().getInterstitialAds(
            context, AdRemoteConfig.getInstance().inter_welcome_back.id, object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    interAd = apInterstitialAd
                    Log.i(TAG, "Welcome back interstitial loaded")
                }
            }
        )
    }

    fun showInterWelcomeBack(activity: Activity, onDone: () -> Unit) {
        val config = RemoteConfigdata(activity)
        if (config.isNeedToShowADs && activity.isInternetAvailable() && interAd != null) {
            ERainAd.getInstance().forceShowInterstitial(
                activity, interAd, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        interAd = null
                        onDone()
                    }
                }, true
            )
        } else {
            onDone()
        }
    }
}
