package com.iptv.online.smart.liveplayer.tv.Ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
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
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
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

    private val preloadHandler = Handler(Looper.getMainLooper())

    data class AdSpec(val adId: String, val layoutId: Int, val tag: String)

    fun preloadStaggered(activity: Activity, specs: List<AdSpec>, gapMs: Long = 250L) {
        specs.forEachIndexed { index, spec ->
            preloadHandler.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    loadAd(activity, spec.adId, spec.layoutId, spec.tag)
                }
            }, index * gapMs)
        }
    }

    private val currentAds = mutableMapOf<String, ApNativeAd?>()
    private val requestedTags = mutableSetOf<String>()
    fun wasRequested(tag: String) = tag in requestedTags
    fun onboarding1Tag(isDone: Boolean) =
        if (isDone) "native_onboarding_2_1" else "native_onboarding_1_1"

    fun loadAd(context: Activity, adId: String, layoutId: Int, adTag: String, shouldDisplay: Boolean = true,
    ) {
        if (RemoteConfigdata(context).isNeedToShowADs && context.isInternetAvailable() && shouldDisplay) {
            Log.d("AdManager123", "🔄 Loading ad for tag=$adTag, id=$adId")
            requestedTags += adTag
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
        // id + isEnable have JSON (AdRemoteConfig) mathi aave chhe.
        val cfg = if (screenCount == 1) AdRemoteConfig.getInstance().native_language_1
        else AdRemoteConfig.getInstance().native_language_2
        // native_language_1 (screen 1) -> ALAG layout (ad-review: height ochhi + edge-to-edge).
        // Baki (screen 2, onboarding, home...) ne layout_native_ad_large j rahe.
        val layoutRes = if (screenCount == 1) R.layout.layout_native_ad_lang_1
        else R.layout.layout_native_ad_large
        loadAd(activity, cfg.id, layoutRes, "native_lang_tag", cfg.isEnable)
    }

    fun loadNativeLanguageClick(activity: Activity, screenCount: Int) {
        // id + isEnable have JSON (AdRemoteConfig) mathi aave chhe.
        val cfg = if (screenCount == 1) AdRemoteConfig.getInstance().native_language_1_click
        else AdRemoteConfig.getInstance().native_language_2_click

        // Screen 1 = flush design (border/space nahi), Screen 2 = juno card design.
        val layoutRes = if (screenCount == 1) R.layout.layout_native_ad_lang_click
        else R.layout.layout_native_ad_lang_click_2

        loadAd(activity, cfg.id, layoutRes, "native_lang_click_tag", cfg.isEnable)
    }

    fun loadNativeOnboarding1(activity: Activity, isDone: Boolean) {
        // id + isEnable JSON (AdRemoteConfig) mathi.
        val cfg = if (isDone) AdRemoteConfig.getInstance().native_onboarding_2_1
        else AdRemoteConfig.getInstance().native_onboarding_1_1
        val tag = onboarding1Tag(isDone)
        loadAd(activity, cfg.id, R.layout.layout_native_ad_large, tag, cfg.isEnable)
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

            val isHighCTAEnabled = ERainAd.getInstance().getShouldDisplayHighCTA(true) == true

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
        val cfg = AdRemoteConfig.getInstance().inter_onboarding
        if (!config.isNeedToShowADs
            || !cfg.isEnable
            || (!ignoreLimit && !(ERainAd.getInstance().getShouldDisplayInterOnboarding(true) == true))
        ) {
            interOnboarding = null
            return
        }
        interOnboarding = ERainAd.getInstance()
            .getInterstitialAds(activity, cfg.id, object : AdCallback() {})
    }

    fun showInterOnboarding(activity: Activity, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null
            && interstitial.isReady
            && RemoteConfigdata(activity).isNeedToShowADs
            && (ignoreLimit || ERainAd.getInstance().getShouldDisplayInterOnboarding(true) == true)
        ) {
            showInterIgnoreGap(activity, interstitial, onAction)
        } else {
            onAction()
        }
    }

    private fun showInterIgnoreGap(activity: Activity, ad: ApInterstitialAd, onAction: () -> Unit) {
        val cfg = ERainAd.getInstance().adConfig
        val orig = cfg.intervalInterstitialAd
        cfg.intervalInterstitialAd = 0
        ERainAd.getInstance().forceShowInterstitial(activity, ad, object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                onAction()
            }
        }, true)
        cfg.intervalInterstitialAd = orig
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
        AdRemoteConfig.getInstance().inter_back.let {
            loadInterCentralized(activity, "interback", it.id, it.isEnable)
        }

    fun showInterBack(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interback", AdRemoteConfig.getInstance().inter_back.isEnable, onAction)

    fun loadInterHome(activity: Activity) =
        AdRemoteConfig.getInstance().inter_home.let {
            loadInterCentralized(activity, "interHome", it.id, it.isEnable)
        }

    fun showInterHome(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interHome", AdRemoteConfig.getInstance().inter_home.isEnable, onAction)

    fun loadInterMirroring(activity: Activity) =
        AdRemoteConfig.getInstance().inter_mirroring.let {
            loadInterCentralized(activity, "interMirroring", it.id, it.isEnable)
        }

    fun showInterMirroring(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interMirroring", AdRemoteConfig.getInstance().inter_mirroring.isEnable, onAction)

    fun loadInterAddPlaylist(activity: Activity) =
        AdRemoteConfig.getInstance().inter_add_playlist.let {
            loadInterCentralized(activity, "interAddPlaylist", it.id, it.isEnable)
        }

    fun showInterAddPlaylist(activity: Activity, onAction: () -> Unit) =
        showInterCentralized(activity, "interAddPlaylist", AdRemoteConfig.getInstance().inter_add_playlist.isEnable, onAction)

    fun Activity.loadAndShowCollapsingBanner(adLayout: View) {
        val config = RemoteConfigdata(this)
        val cfg = AdRemoteConfig.getInstance().banner_collapsible_home
        val adId = cfg.id

        Log.d("AdManager123", "Attempting to load Collapsible Banner: $adId")

        if (config.isNeedToShowADs && cfg.isEnable) {
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
