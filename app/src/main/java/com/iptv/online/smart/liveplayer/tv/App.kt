package com.iptv.online.smart.liveplayer.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
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
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
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
        var isNeedToStopOPenAds = false

        /** Startup timing log no tag - "adb logcat -s AppStartup" thi joi shakay. */
        const val STARTUP_TAG = "AppStartup"
    }

    fun initFirebaseConfigs(mActivity: Activity) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        remoteConfig.fetchAndActivate().addOnCompleteListener(mActivity) { task ->
            if (!mActivity.isFinishing) {
                onRemoteFetched?.invoke(remoteConfig)
            }
        }.addOnFailureListener(mActivity) {
            if (!mActivity.isFinishing) {
                onRemoteFetched?.invoke(null)
            }
        }
    }

    /**
     * Startup na kaya step ma ketlo time jaay chhe e mapva mate.
     * Logcat ma "AppStartup" filter karo - ms ma dekhashe.
     * Je step sauthi bhare hoy e j pachi defer/background par khasedvano.
     */
    private inline fun <T> step(name: String, block: () -> T): T {
        val start = SystemClock.uptimeMillis()
        val result = block()
        Log.d(STARTUP_TAG, "$name = ${SystemClock.uptimeMillis() - start} ms")
        return result
    }

    override fun onCreate() {
        val onCreateStart = SystemClock.uptimeMillis()
        super.onCreate()
        Log.d(STARTUP_TAG, "super.onCreate (ads SDK base) = ${SystemClock.uptimeMillis() - onCreateStart} ms")
        app = this

        step("FirebaseApp.initializeApp") { FirebaseApp.initializeApp(this) }

        // ViewPump pehla j joie chhe - first Activity na view inflate thata pehla
        // font interceptor lagelo hovo joie, nahi to font lagu na thay.
        step("ViewPump.init (font)") {
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
        }

        // iptv2 jevu JSON ad-config load (debug -> test json, release -> real json).
        step("AdRemoteConfig.initialize (asset JSON)") { AdRemoteConfig.initialize(this) }

        // AA ONCREATE MA J RAHEVU JOIE - DEFER NA KARVU.
        // Andar ERainAd.init() -> AppOpenManager.init() chhe, je
        // ProcessLifecycleOwner par observer add kare chhe. Jo aa first Activity
        // resume thaya PACHI chale, to Lifecycle turat ON_RESUME dispatch kare ane
        // AppOpenManager.onResume() ma currentActivity null hovathi NPE crash thay:
        //   "Failed to call observer method ... AppOpenManager.onResume"
        // Prayog kari joyo hato - app chalu thata j crash thai gai hati.
        step("initAds (ERainAd/Admob/Adjust/Facebook)") { initAds() }

        val mainHandler = Handler(Looper.getMainLooper())

        // ---- WebView / Chromium warm-up: PRAYOG KARI JOYO, KAAM NA LAGYO ----
        // Crashlytics no J.N.* ANR (J.N.OOZ/JJ/VZ/IZ, 100% background) nu karan chhe
        // Chromium engine nu pehli-var startup main thread par:
        //   com.google.android.gms.ads -> AwBrowserProcess.b
        //   -> BrowserStartupControllerImpl.h -> J.N.IZ  ("Root blocking")
        // Etle `WebView(this).destroy()` thi engine vahelu chalu karva prayatna karyo.
        // Vivo V2037 par maapelu:
        //   deferred ma CHHELLE  -> 31 ms  (nakamu - Chromium pehla thi chalu)
        //   onCreate ni andar    -> 532 ms, pan TOTAL 582 -> 1115 ms. First frame 532 ms
        //                           modu = vadhu var kori screen = user vahelo bahar
        //                           nikle = e j background-ANR vadhe. Ulto asar.
        //   deferred ma PEHLU    -> 44 ms (haju modu). Karan: onCreate pura thaya pachi
        //                           main thread 1.65 s sudhi Splash + consent + ad ma
        //                           vyast rahe chhe, ane E gaala ma j Chromium chalu thai
        //                           jaay chhe. Handler.post e queue ma aagal kudi na shake.
        // Nishkarsh: first frame pachi ni koi pan jagya modi chhe, ane pehla ni jagya
        // no bhaav startup chhe. Vachche jagya j nathi. ETLE AA PRAYATNA CHHODI DIDHO -
        // fari na karvo.

        // DevConfig fakt Language screen na title par 10-tap e joie chhe,
        // ane OneSignal pan turat nathi joito - e banne defer thai shake chhe.
        mainHandler.post {
            step("DevConfig.init (deferred)") {
                DevConfig.init(
                    context = this,
                    nkhStudioVersion = "2.0",
                    playServicesAdsVersion = "24.7.0",
                    gdprModuleVersion = "2.0.2"
                )
            }
        }

        mainHandler.post {
            step("OneSignal.initWithContext (deferred)") {
                OneSignal.initWithContext(this, "9971a8a0-4bb3-48ae-bac0-30af3026640d")
            }
        }

        Log.d(STARTUP_TAG, "===== App.onCreate TOTAL = ${SystemClock.uptimeMillis() - onCreateStart} ms =====")
    }

    private fun initAds() {
        val environment =
            if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
        mERainAdConfig = ERainAdConfig(this, environment)

        mERainAdConfig.adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
        mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
        mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.tiktok_token)
//        mERainAdConfig.idAdResume = (AdsId.openResume)
        mERainAdConfig.idAdResume = (AdRemoteConfig.getInstance().inter_welcome_back.id)

        // Be interstitial ad ni vachche ochha ma ochho 30 second no gap raakhvo.
        // ERainAd.forceShowInterstitial aa value (second) check kare chhe, etle
        // welcome-back ane infinity badha inter ne aa gap lagu padshe.
        mERainAdConfig.intervalInterstitialAd = 30

        ERainAd.getInstance().init(this, mERainAdConfig)

        Admob.getInstance().setDisableAdResumeWhenClickAds(true)
        Admob.getInstance().setOpenActivityAfterShowInterAds(true)

        AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)

        // App resume par App-Open ad ni jagya e Welcome Screen batavvi che,
        // etle resume open-ad disable rakhiye ane WelcomeManager init kariye.
        AppOpenManager.getInstance().disableAppResume()
        AppResumeWelcomeManager.init(this)
    }



}
