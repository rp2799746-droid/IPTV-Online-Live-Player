package com.iptv.online.smart.liveplayer.tv.Ads

import android.app.Activity
import android.util.Log
import android.view.View
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.funtion.AdCallback
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.getSHouldDisplayHighCTA
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object InfinityAdsManager {

    private val _adStateFlow = MutableStateFlow<Map<String, NativeAdUiState>>(emptyMap())
    val adStateFlow: StateFlow<Map<String, NativeAdUiState>> get() = _adStateFlow
    private val currentAds = mutableMapOf<String, ApNativeAd?>()
    fun loadAd(context: Activity, adId: String, layoutId: Int, adTag: String) {
        if (RemoteConfigdata(context).isNeedToShowADs) {
            Log.d("AdManager123", "🔄 Loading ad for tag=$adTag, id=$adId")
            ERainAd.getInstance().loadNativeAdResultCallback(
                context, adId, layoutId, object : AdCallback() {
                    override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                        currentAds[adTag] = nativeAd
                        val heightValue = getCTAButtonHeight(context)

                        Log.d(
                            "AdManager123",
                            "✅[$adTag] Ad loaded successfully: id=$adId, CTA Height=$heightValue"
                        )
                        updateAdState(
                            adTag,
                            NativeAdUiState.Success(nativeAd, adTag, adId, heightValue)
                        )


                    }

                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        currentAds[adTag] = null
                        Log.e(
                            "AdManager123",
                            "❌ Ad failed for [$adTag] and id=[$adId] reason= | error=${i?.message}"
                        )
                        updateAdState(adTag, NativeAdUiState.Failed(adTag, adId, i?.message))
                    }

                    override fun onAdFailedToShow(adError: AdError?) {
                        super.onAdFailedToShow(adError)
                        currentAds[adTag] = null
                        Log.e(
                            "AdManager123",
                            "[$adTag] Failed to show: $adTag | id=[$adId] | error=${adError?.message}"
                        )
                        updateAdState(adTag, NativeAdUiState.Failed(adTag, adId, adError?.message))
                    }
                })
        } else {
            currentAds[adTag] = null
            Log.d("AdManager123", "Ads Close")
        }
    }

    private fun updateAdState(tagName: String, state: NativeAdUiState) {
        _adStateFlow.value = _adStateFlow.value.toMutableMap().apply {
            this[tagName] = state
        }
    }

    // InfinityAdsManager object ની અંદર
    fun getCTAButtonHeight(context: android.content.Context): Long {
        return try {
            val remoteData = RemoteConfigdata(context)
            val value = remoteData.height_button_cta.toLongOrNull() ?: 40L

            val isHighCTAEnabled = context.getSHouldDisplayHighCTA()

            return if (isHighCTAEnabled) {
                value.coerceIn(36L, 52L)
            } else {
                value.coerceIn(36L, 46L)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            40L // કોઈ પણ ભૂલના કિસ્સામાં ડિફોલ્ટ 40
        }
    }

    // Be interstitial ni vachche 30 second no gap App.kt ma
    // mERainAdConfig.intervalInterstitialAd = 30 thi centralized rite enforce
    // thay chhe (ERainAd.forceShowInterstitial ni andar). Etle ahiya alag
    // gap-manager ni jarur nathi — welcome-back ne pan e j gap lagu padshe.
    fun Activity.showInterAds(
        mInterstitialAd: ApInterstitialAd?,
        onComplete: () -> Unit,
    ) {
        if (RemoteConfigdata(this).isNeedToShowADs && isInternetAvailable()) {
            Log.i("AdManager123", "Show Inter : ${this.localClassName}")
            ERainAd.getInstance().forceShowInterstitial(
                this, mInterstitialAd, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onComplete.invoke()
                    }
                }, true
            )
        } else {
            onComplete.invoke()
        }
    }
    fun Activity.loadAndShowCollapsingBanner(adLayout: View) {
        val config = RemoteConfigdata(this)
        val adId = AdsId.banner_collap_home

        Log.d("AdManager123", "Attempting to load Collapsible Banner: $adId")

        if (config.isNeedToShowADs) {
            Log.d("AdManager123", "Ads Enabled: true. Showing layout and calling load.")
            adLayout.visible

            ERainAd.getInstance().loadCollapsibleBanner(
                this,
                adId,
                AppConstant.CollapsibleGravity.BOTTOM,
                object : AdCallback() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.i("AdManager123", "Collapsible Banner Loaded Successfully")
                    }

                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        Log.e("AdManager123", "Collapsible Banner Failed to Load. Error Code: $i")
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        Log.d("AdManager123", "Collapsible Banner Clicked")
                    }

                    override fun onNextAction() {
                        super.onNextAction()
                        Log.d("AdManager123", "Collapsible Banner Next Action Called")
                    }
                }
            )
        } else {
            Log.w("AdManager123", "Ads Disabled in Remote Config. Hiding layout.")
            adLayout.gone
        }
    }


}
