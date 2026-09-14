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
import com.google.android.gms.ads.MobileAds
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Activity.LanguageActivity
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

@HiltAndroidApp
class App : AdsMultiDexApplication() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var app: App? = null
        var onRemoteFetched: ((FirebaseRemoteConfig?) -> Unit)? = null
        const val STARTUP_TAG = "AppStartup"
    }

    fun initFirebaseConfigs(mActivity: Activity) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener(mActivity) { task ->
            if (!mActivity.isFinishing && !mActivity.isDestroyed) {
                onRemoteFetched?.invoke(if (task.isSuccessful) remoteConfig else null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = this

        MobileAds.initialize(this) {}

        try {
            DevConfig.init(
                context = this,
                nkhStudioVersion = BuildConfig.ERAIN_STUDIO_VERSION,
                playServicesAdsVersion = BuildConfig.PLAY_SERVICES_ADS_VERSION,
                gdprModuleVersion = BuildConfig.GDPR_MODULE_VERSION
            )
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "DevConfig init error: ${e.message}")
        }

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "FirebaseApp init error: ${e.message}")
        }

        try {
            AdRemoteConfig.initialize(this)
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "AdRemoteConfig init error: ${e.message}")
        }


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


        initAds()

        try {
            OneSignal.initWithContext(this, "9971a8a0-4bb3-48ae-bac0-30af3026640d")
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "OneSignal init error: ${e.message}")
        }
    }

    private fun initAds() {
        try {
            val environment = if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
            mERainAdConfig = ERainAdConfig(this, environment)

            mERainAdConfig.adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
            mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
            mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.tiktok_token)

            mERainAdConfig.idAdResume = ""

            mERainAdConfig.intervalInterstitialAd = 35

            ERainAd.getInstance().init(this, mERainAdConfig)

            Admob.getInstance().setDisableAdResumeWhenClickAds(true)
            Admob.getInstance().setOpenActivityAfterShowInterAds(true)

            AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(IntroActivity::class.java)
            AppOpenManager.getInstance().disableAppResume()
            AppResumeWelcomeManager.init(this)


        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "initAds Error: ${e.message}")
        }
    }

    /** Called after Splash has applied the ad configuration selected by Firebase. */
    fun configureAppResume() {
        val config = AdRemoteConfig.getInstance().open_resume
        if (config.isEnable && config.id.isNotBlank()) {
            AppOpenManager.getInstance().setAppResumeAdId(config.id)
            AppOpenManager.getInstance().enableAppResume()
        } else {
            AppOpenManager.getInstance().disableAppResume()
        }
    }
}
