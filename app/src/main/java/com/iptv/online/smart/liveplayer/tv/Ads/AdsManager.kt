package com.iptv.online.smart.liveplayer.tv.Ads

import android.app.Activity
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.funtion.AdCallback
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.getSHouldDisplayHighCTA
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayInterOnboarding
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdsManager {

    private val _adStateFlow = MutableStateFlow<Map<String, NativeAdUiState>>(emptyMap())
    val adStateFlow: StateFlow<Map<String, NativeAdUiState>> get() = _adStateFlow

    private val adLiveMap = mutableMapOf<String, MutableLiveData<NativeAdUiState>>()
    fun getAdLive(tag: String): LiveData<NativeAdUiState> =
        adLiveMap.getOrPut(tag) { MutableLiveData() }

    private val currentAds = mutableMapOf<String, ApNativeAd?>()
    fun loadAd(context: Activity, adId: String, layoutId: Int, adTag: String, shouldDisplay: Boolean = true,
    ) {
        if (RemoteConfigdata(context).isNeedToShowADs && context.isInternetAvailable() && shouldDisplay) {
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

    fun loadNativeLanguage(activity: Activity, screenCount: Int) {
        val adId = if (screenCount == 1) AdsId.nativeLanguage1 else AdsId.nativeLanguage2
        loadAd(activity, adId, R.layout.layout_native_ad_large, "native_lang_tag")
    }

    fun loadNativeLanguageClick(activity: Activity, screenCount: Int) {
        val adId = if (screenCount == 1) AdsId.nativeLanguage1Click else AdsId.nativeLanguage2Click
        val enabled = if (screenCount == 1) RemoteConfigdata(activity).nativeLang1ClickOn
        else RemoteConfigdata(activity).nativeLang2ClickOn
        loadAd(activity, adId, R.layout.layout_native_ad_lang_click, "native_lang_click_tag", enabled)
    }

    fun loadNativeOnboarding1(activity: Activity, isDone: Boolean) {
        val adId = if (isDone) AdsId.nativeOnboarding2_1 else AdsId.nativeOnboarding1_1
        val tag = if (isDone) "native_onboarding_2_1" else "native_onboarding_1_1"
        loadAd(activity, adId, R.layout.layout_native_ad_large, tag)
    }

    private fun updateAdState(tagName: String, state: NativeAdUiState) {
        _adStateFlow.value = _adStateFlow.value.toMutableMap().apply {
            this[tagName] = state
        }
        adLiveMap.getOrPut(tagName) { MutableLiveData() }.postValue(state)
    }

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
            40L
        }
    }



    private var interOnboarding: ApInterstitialAd? = null

    fun loadInterOnboarding(activity: Activity, ignoreLimit: Boolean = false) {
        val config = RemoteConfigdata(activity)
        if (!config.isNeedToShowADs
            || !config.interOnboardingOn
            || (!ignoreLimit && !activity.getShouldDisplayInterOnboarding())
        ) {
            interOnboarding = null
            return
        }
        interOnboarding = ERainAd.getInstance()
            .getInterstitialAds(activity, AdsId.interOnboarding, object : AdCallback() {})
    }

    fun showInterOnboarding(activity: Activity, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null
            && interstitial.isReady
            && RemoteConfigdata(activity).isNeedToShowADs
            && (ignoreLimit || activity.getShouldDisplayInterOnboarding())
        ) {
            ERainAd.getInstance()
                .forceShowInterstitial(activity, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onAction()
                    }
                }, true)
        } else {
            onAction()
        }
    }

    private val interAdMap = mutableMapOf<String, ApInterstitialAd?>()

    private fun loadInterCentralized(activity: Activity, key: String, adId: String, enabled: Boolean) {
        val config = RemoteConfigdata(activity)
        if (!config.isNeedToShowADs || !enabled) {
            interAdMap[key] = null
            return
        }
        interAdMap[key] = ERainAd.getInstance()
            .getInterstitialAds(activity, adId, object : AdCallback() {})
    }

    private fun showInterCentralized(
        activity: Activity, key: String, enabled: Boolean, onAction: () -> Unit
    ) {
        val ad = interAdMap[key]
        if (ad != null && ad.isReady && RemoteConfigdata(activity).isNeedToShowADs && enabled) {
            ERainAd.getInstance().forceShowInterstitial(activity, ad, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, true)
        } else {
            onAction()
        }
    }

    fun loadInterBack(activity: Activity) =
        loadInterCentralized(activity, "interback", AdsId.interback, RemoteConfigdata(activity).interback)

    fun showInterBack(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interback", RemoteConfigdata(activity).interback, onAction)

    fun loadInterHome(activity: Activity) =
        loadInterCentralized(activity, "interHome", AdsId.interHome, RemoteConfigdata(activity).interHomeOn)

    fun showInterHome(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interHome", RemoteConfigdata(activity).interHomeOn, onAction)

    fun loadInterMirroring(activity: Activity) =
        loadInterCentralized(activity, "interMirroring", AdsId.INTER_MIRRORING, RemoteConfigdata(activity).interMirroring)

    fun showInterMirroring(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interMirroring", RemoteConfigdata(activity).interMirroring, onAction)

    fun loadInterAddPlaylist(activity: Activity) =
        loadInterCentralized(activity, "interAddPlaylist", AdsId.INTER_ADD_PLAYLIST, RemoteConfigdata(activity).interAddPlaylist)

    fun showInterAddPlaylist(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interAddPlaylist", RemoteConfigdata(activity).interAddPlaylist, onAction)

    fun Activity.loadAndShowCollapsingBanner(adLayout: View) {
        val config = RemoteConfigdata(this)
        val adId = AdsId.banner_collap_home

        Log.d("AdManager123", "Attempting to load Collapsible Banner: $adId")

        if (config.isNeedToShowADs) {
            // Container turat batavo -> shimmer instant dekhay, pachi banner emma aave.
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
                        adLayout.gone
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
