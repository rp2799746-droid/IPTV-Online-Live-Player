package com.iptv.online.smart.liveplayer.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.module.admob.Admob
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.application.AdsMultiDexApplication
import com.ads.module.config.AdjustConfig
import com.ads.module.config.ERainAdConfig
import com.google.firebase.FirebaseApp
import com.google.android.gms.ads.MobileAds
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Activity.LanguageActivity
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.Activity.UninstallScreenActivity
import com.iptv.online.smart.liveplayer.tv.Activity.UninstallSurveyActivity
import com.iptv.online.smart.liveplayer.tv.Activity.WelcomeScreenActivity

import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.app.AppActivityLifecycleCallbacks
import com.iptv.online.smart.liveplayer.tv.app.AppLifecycleObserver
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
        lateinit var instance: App
        @SuppressLint("StaticFieldLeak")
        var currentActivity: Activity? = null
        const val STARTUP_TAG = "AppStartup"
    }

    override fun onCreate() {
        super.onCreate()
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

        app = this
        instance = this

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "FirebaseApp init error: ${e.message}")
        }

        try {
            AdRemoteConfig.initializeFromAssets(this)
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

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
        registerActivityLifecycleCallbacks(AppActivityLifecycleCallbacks())

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
            Admob.getInstance().setOpenActivityAfterShowInterAds(false)

            AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(LanguageActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(IntroActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(WelcomeScreenActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(UninstallScreenActivity::class.java)
            AppOpenManager.getInstance().disableAppResumeWithActivity(UninstallSurveyActivity::class.java)
            AppOpenManager.getInstance().disableAppResume()


        } catch (e: Exception) {
            Log.e(STARTUP_TAG, "initAds Error: ${e.message}")
        }
    }
}