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
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.AdUnitConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdsManager {

    private const val TAG = "AdManager123"

    private val _adStateFlow = MutableStateFlow<Map<String, NativeAdUiState>>(emptyMap())

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

    // Base flow channels.  Startup/onboarding screens observe these directly;
    // the tag-based state below remains only for unrelated legacy IPTV screens.
    val nativeLanguageAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdLive = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFullLive = MutableLiveData<ApNativeAd?>()
    var interWelcomeAdLive = MutableLiveData<ApInterstitialAd?>()
    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()

    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    // Je LiveData mate native request haju chalu chhe (loaded/failed nathi thayu).
    // Screen ne khabar pade ke null "haju loading" chhe ke "sacho fail", etle loading vakhte shimmer rahe.
    private val loadingLives = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<MutableLiveData<ApNativeAd?>, Boolean>()
    )

    fun isNativeLoading(liveData: MutableLiveData<ApNativeAd?>): Boolean = liveData in loadingLives

    /** Clears a prior process-static Language native without issuing a load. */
    fun clearNativeLanguage() {
        nativeLanguageAdLive.postValue(null)
    }

    fun onboarding1Tag(isFirst: Boolean) =
        if (isFirst) "native_onboarding_1_1_3009" else "native_onboarding_2_1_3009"

    /**
     * Single base-style native gate.  Every skipped or failed request emits
     * null so the owner cannot be left waiting on a stale shimmer/state.
     */
    private fun loadNativeInternal(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
        shouldDisplay: Boolean = true,
        name: String = "native",
    ) {
        val skipReasons = buildList {
            if (!RemoteConfigdata(activity).isNeedToShowADs) add("isNeedToShowADs=false")
            if (!config.isEnable) add("unit disabled in remote config")
            if (isPurchased(activity)) add("user purchased")
            if (!activity.isInternetAvailable()) add("no internet")
            if (!shouldDisplay) add("shouldDisplay=false (UA/limit check)")
        }
        if (skipReasons.isNotEmpty()) {
            Log.w(TAG, "⏭️ [$name] SKIP native: id=${config.id} | reason=${skipReasons.joinToString(", ")}")
            loadingLives.remove(liveData)
            liveData.postValue(null)
            return
        }

        loadingLives.add(liveData)
        Log.d(TAG, "🔄 [$name] Loading native: id=${config.id}")
        ERainAd.getInstance().loadNativeAdResultCallback(
            activity,
            config.id,
            layoutRes,
            object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    Log.d(TAG, "✅ [$name] Native loaded: id=${config.id}")
                    adConfigMap[nativeAd] = config
                    loadingLives.remove(liveData)
                    liveData.postValue(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    Log.e(
                        TAG,
                        "❌ [$name] Native failed: id=${config.id} | code=${adError?.code} | error=${adError?.message}"
                    )
                    loadingLives.remove(liveData)
                    liveData.postValue(null)
                }

                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    Log.e(TAG, "❌ [$name] Native failed to show: id=${config.id} | error=${adError?.message}")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d(TAG, "👆 [$name] Native clicked: id=${config.id}")
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "👁️ [$name] Native impression: id=${config.id}")
                }
            }
        )
    }

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
            val reason = buildList {
                if (!RemoteConfigdata(context).isNeedToShowADs) add("isNeedToShowADs=false")
                if (!context.isInternetAvailable()) add("no internet")
                if (!shouldDisplay) add("unit disabled / shouldDisplay=false")
            }.joinToString(", ")
            currentAds[adTag] = null
            updateAdState(adTag, NativeAdUiState.Empty(adTag, adId))
            Log.w("AdManager123", "⏭️ [$adTag] SKIP native: id=$adId | reason=$reason")
        }
    }

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.getInstance().native_language_1 else AdRemoteConfig.getInstance().native_language_2
        loadNativeInternal(activity, config, layoutRes, nativeLanguageAdLive, name = if (isFirst) "native_language_1_3009" else "native_language_2_3009")
    }

    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.getInstance().native_language_1_click else AdRemoteConfig.getInstance().native_language_2_click
        loadNativeInternal(activity, config, layoutRes, nativeLanguageClickAdLive, name = if (isFirst) "native_language_1_click_3009" else "native_language_2_click_3009")
    }

    fun loadNativeOnboarding1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.getInstance().native_onboarding_1_1 else AdRemoteConfig.getInstance().native_onboarding_2_1
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding1AdLive,
            name = if (isFirst) "native_onboarding_1_1_3009" else "native_onboarding_2_1_3009"
        )
    }

    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.getInstance().native_onboarding_1_4 else AdRemoteConfig.getInstance().native_onboarding_2_4
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding4AdLive,
            ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2(config.enableUaCheck) == true,
            name = if (isFirst) "native_onboarding_1_4_3009" else "native_onboarding_2_4_3009"
        )
    }

    fun loadNativeOnboardingFull(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2 else AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeAdOnBoardingFullLive,
            ERainAd.getInstance().getOrganic() == config.enableUaCheck,
            name = if (isFirst) "native_onboarding_fullscreen_1_2_3009" else "native_onboarding_fullscreen_2_2_3009"
        )
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
        Log.w(TAG, "⏭️ [inter_onboarding] SKIP load: reason=disabled in code (stub)")
        interOnboarding = null
    }

    fun showInterOnboarding(activity: Activity, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        Log.w(TAG, "⏭️ [inter_onboarding] SKIP show: reason=disabled in code (stub)")
        interOnboarding = null
        onAction()
    }

    fun loadInterWelcome(activity: Activity, ignoreLimit: Boolean = false) {
        val cached = interWelcomeAdLive.value?.takeIf { it.isReady }
        val request = MutableLiveData<ApInterstitialAd?>()
        interWelcomeAdLive = request
        fun emit(ad: ApInterstitialAd?) = request.postValue(ad)

        val config = RemoteConfigdata(activity)
        val cfg = AdRemoteConfig.getInstance().inter_welcome_back
        val skipReasons = buildList {
            if (!config.isNeedToShowADs) add("isNeedToShowADs=false")
            if (!config.interwelcomeback) add("interwelcomeback=false")
            if (!cfg.isEnable) add("unit disabled in remote config")
            if (isPurchased(activity)) add("user purchased")
            if (!activity.isInternetAvailable()) add("no internet")
            if (!ignoreLimit && !(ERainAd.getInstance()
                    .getShouldDisplayInterWelcomeBack(cfg.enableUaCheck) == true)
            ) add("UA/limit check failed")
        }
        if (skipReasons.isNotEmpty()) {
            Log.w(
                TAG,
                "⏭️ [inter_welcome_back] SKIP load: id=${cfg.id} | reason=${skipReasons.joinToString(", ")}"
            )
            emit(null)
            return
        }
        if (cached != null) {
            Log.d(TAG, "♻️ [inter_welcome_back] Using cached ready inter: id=${cfg.id}")
            emit(cached)
            return
        }

        Log.d(TAG, "🔄 [inter_welcome_back] Loading inter: id=${cfg.id}")
        var loaded: ApInterstitialAd? = null
        loaded = ERainAd.getInstance().getInterstitialAds(
            activity,
            cfg.id,
            object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    Log.d(
                        TAG,
                        "✅ [inter_welcome_back] Inter loaded: id=${cfg.id} | ready=${(apInterstitialAd ?: loaded)?.isReady}"
                    )
                    emit(apInterstitialAd ?: loaded)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    Log.e(
                        TAG,
                        "❌ [inter_welcome_back] Inter failed: id=${cfg.id} | code=${adError?.code} | error=${adError?.message}"
                    )
                    emit(null)
                }
            }
        )
        if (loaded?.isReady == true) {
            Log.d(TAG, "✅ [inter_welcome_back] Inter already ready on request: id=${cfg.id}")
            emit(loaded)
        }
    }

    fun showInterWelcome(
        activity: Activity,
        ignoreLimit: Boolean = false,
        onAction: () -> Unit
    ) {
        val interstitial = interWelcomeAdLive.value
        val remoteData = RemoteConfigdata(activity)
        val cfg = AdRemoteConfig.getInstance().inter_welcome_back
        val skipReasons = buildList {
            if (interstitial == null) add("no ad instance")
            else if (!interstitial.isReady) add("ad not ready")
            if (!remoteData.isNeedToShowADs) add("isNeedToShowADs=false")
            if (!remoteData.interwelcomeback) add("interwelcomeback=false")
            if (!cfg.isEnable) add("unit disabled in remote config")
            if (isPurchased(activity)) add("user purchased")
            if (!activity.isInternetAvailable()) add("no internet")
            if (!ignoreLimit && !(ERainAd.getInstance()
                    .getShouldDisplayInterWelcomeBack(cfg.enableUaCheck) == true)
            ) add("UA/limit check failed")
        }
        if (skipReasons.isEmpty() && interstitial != null) {
            Log.d(TAG, "📺 [inter_welcome_back] Showing inter: id=${cfg.id}")
            ERainAd.getInstance().forceShowInterstitial(
                activity, interstitial, object : AdCallback() {
                    override fun onAdFailedToShow(adError: AdError?) {
                        super.onAdFailedToShow(adError)
                        Log.e(
                            TAG,
                            "❌ [inter_welcome_back] Failed to show: id=${cfg.id} | error=${adError?.message}"
                        )
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        Log.d(TAG, "🚪 [inter_welcome_back] Inter closed: id=${cfg.id}")
                    }

                    override fun onNextAction() {
                        super.onNextAction()
                        Log.d(TAG, "➡️ [inter_welcome_back] Next action: id=${cfg.id}")
                        interWelcomeAdLive = MutableLiveData()
                        onAction()
                    }
                }, false
            )
        } else {
            Log.w(
                TAG,
                "⏭️ [inter_welcome_back] SKIP show: id=${cfg.id} | reason=${skipReasons.joinToString(", ")}"
            )
            onAction()
        }
    }

    private val interAdMap = mutableMapOf<String, ApInterstitialAd?>()

    private fun loadInterCentralized(activity: Activity, key: String, adId: String, enabled: Boolean) {
        val config = RemoteConfigdata(activity)
        if (!config.isNeedToShowADs || !enabled) {
            Log.w(
                TAG,
                "⏭️ [$key] SKIP load: id=$adId | reason=${if (!config.isNeedToShowADs) "isNeedToShowADs=false" else "unit disabled in remote config"}"
            )
            interAdMap[key] = null
            return
        }
        Log.d(TAG, "🔄 [$key] Loading inter: id=$adId")
        interAdMap[key] = ERainAd.getInstance()
            .getInterstitialAds(activity, adId, object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    Log.d(TAG, "✅ [$key] Inter loaded: id=$adId | ready=${apInterstitialAd?.isReady}")
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    Log.e(
                        TAG,
                        "❌ [$key] Inter failed: id=$adId | code=${adError?.code} | error=${adError?.message}"
                    )
                }
            })
    }

    private fun showInterCentralized(
        activity: Activity, key: String, enabled: Boolean, onAction: () -> Unit
    ) {
        val ad = interAdMap[key]
        val needToShow = RemoteConfigdata(activity).isNeedToShowADs
        if (ad != null && ad.isReady && needToShow && enabled) {
            Log.d(TAG, "📺 [$key] Showing inter")
            ERainAd.getInstance().forceShowInterstitial(activity, ad, object : AdCallback() {
                override fun onAdFailedToShow(adError: AdError?) {
                    super.onAdFailedToShow(adError)
                    Log.e(TAG, "❌ [$key] Failed to show: error=${adError?.message}")
                }

                override fun onAdClosed() {
                    super.onAdClosed()
                    Log.d(TAG, "🚪 [$key] Inter closed")
                }

                override fun onNextAction() {
                    super.onNextAction()
                    Log.d(TAG, "➡️ [$key] Next action")
                    onAction()
                }
            }, true)
        } else {
            val reason = when {
                ad == null -> "no ad instance (load not done / skipped)"
                !ad.isReady -> "ad not ready"
                !needToShow -> "isNeedToShowADs=false"
                else -> "unit disabled in remote config"
            }
            Log.w(TAG, "⏭️ [$key] SKIP show: reason=$reason")
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
    fun loadNativeMirroring(activity: Activity) {
        val cfg = AdRemoteConfig.getInstance().native_mirroring
        loadAd(activity, cfg.id, R.layout.layout_native_ad_large, "native_mirroring_3009", cfg.isEnable)
    }
    fun Activity.loadAndShowCollapsingBanner(adLayout: View) {
        val config = RemoteConfigdata(this)
        val cfg = AdRemoteConfig.getInstance().banner_collapsible_home
        val adId = cfg.id

        Log.d("AdManager123", "Attempting to load Collapsible Banner: $adId")

        if (config.isNeedToShowADs && cfg.isEnable) {
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