package com.iptv.online.smart.liveplayer.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.Ads.AppResumeWelcomeManager
import com.itg.devconfig.DevConfig
import com.onesignal.OneSignal
import dagger.hilt.android.HiltAndroidApp
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : AdsMultiDexApplication() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        @SuppressLint("StaticFieldLeak")
        var app: App? = null
        var onRemoteFetched: ((FirebaseRemoteConfig?) -> Unit)? = null
        var isNeedToStopOPenAds = false
        const val STARTUP_TAG = "AppStartup"
    }

    fun initFirebaseConfigs(mActivity: Activity) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener(mActivity) { _ ->
            if (!mActivity.isFinishing && !mActivity.isDestroyed) {
                onRemoteFetched?.invoke(remoteConfig)
            }
        }.addOnFailureListener(mActivity) {
            if (!mActivity.isFinishing && !mActivity.isDestroyed) {
                onRemoteFetched?.invoke(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this

        // 1. JSON Ad Config પેહલા sync લોડ કરો જેથી AdUnitId null ના રહે અને Crash ના થાય
        try {
            AdRemoteConfig.initialize(this)
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "AdRemoteConfig init error: ${e.message}")
        }

        // 2. ViewPump font init
        try {
            ViewPump.init(
                ViewPump.builder()
                    .addInterceptor(
                        CalligraphyInterceptor(
                            CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/sf_pro_text_medium.ttf")
                                .build()
                        )
                    )
                    .build()
            )
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "ViewPump init error: ${e.message}")
        }

        // 3. Ads Module Configuration (Splash Resume Ad Disable)
        initAds()

        // 4. ભારે SDKs (Firebase, OneSignal, DevConfig) બેકગ્રાઉન્ડ થ્રેડમાં
        appScope.launch {
            try {
                FirebaseApp.initializeApp(this@App)
            } catch (e: Exception) {
                Log.e(STARTUP_TAG, "FirebaseApp init error: ${e.message}")
            }

            try {
                OneSignal.initWithContext(this@App, "9971a8a0-4bb3-48ae-bac0-30af3026640d")
            } catch (e: Exception) {
                Log.e(STARTUP_TAG, "OneSignal init error: ${e.message}")
            }

            try {
                DevConfig.init(
                    context = this@App,
                    nkhStudioVersion = "2.0",
                    playServicesAdsVersion = "24.7.0",
                    gdprModuleVersion = "2.0.2"
                )
            } catch (e: Exception) {
                Log.e(STARTUP_TAG, "DevConfig init error: ${e.message}")
            }
        }
    }

    private fun initAds() {
        try {
            val environment =
                if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
            mERainAdConfig = ERainAdConfig(this, environment)

            mERainAdConfig.adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
            mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
            mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.tiktok_token)

            // Safe Ad ID Check - ક્યારેય null નહીં જાય
            val resumeId = AdRemoteConfig.getInstance()?.inter_welcome_back?.id ?: ""
            mERainAdConfig.idAdResume = if (resumeId.isNotEmpty()) resumeId else "ca-app-pub-3940256099942544/9257395921" // Fallback Test ID if blank

            mERainAdConfig.intervalInterstitialAd = 30

            ERainAd.getInstance().init(this, mERainAdConfig)

            Admob.getInstance().setDisableAdResumeWhenClickAds(true)
            Admob.getInstance().setOpenActivityAfterShowInterAds(true)

            // SplashActivity પર App Resume Open Ad બિલકુલ બંધ રાખો
            AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
            AppOpenManager.getInstance().disableAppResume()
            AppResumeWelcomeManager.init(this)
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "initAds Error: ${e.message}")
        }
    }
}
