package com.iptv.online.smart.liveplayer.tv

import android.annotation.SuppressLint
import android.app.Activity
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
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.Ads.AppResumeWelcomeManager

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
    }

    fun initFirebaseConfigs(mActivity: Activity) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            // પ્રોડક્શનમાં આ સમય વધારવો જોઈએ, પણ ટેસ્ટિંગ માટે 1 સેકન્ડ બરાબર છે
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 1 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // ફક્ત એક જ વાર fetchAndActivate કોલ કરો
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

    override fun onCreate() {
        super.onCreate()
        app = this

        FirebaseApp.initializeApp(this)
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

        OneSignal.initWithContext(this,"9971a8a0-4bb3-48ae-bac0-30af3026640d")


        val environment =
            if (BuildConfig.DEBUG) ERainAdConfig.ENVIRONMENT_DEVELOP else ERainAdConfig.ENVIRONMENT_PRODUCTION
        mERainAdConfig = ERainAdConfig(this, environment)

        mERainAdConfig.adjustConfig = AdjustConfig(true, resources.getString(R.string.adjust_token))
        mERainAdConfig.facebookClientToken = resources.getString(R.string.facebook_client_token)
        mERainAdConfig.adjustTokenTiktok = resources.getString(R.string.tiktok_token)
//        mERainAdConfig.idAdResume = (AdsId.openResume)
        mERainAdConfig.idAdResume = (AdsId.interwelcomeback)

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
