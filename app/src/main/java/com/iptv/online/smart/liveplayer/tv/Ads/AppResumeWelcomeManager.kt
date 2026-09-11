package com.iptv.online.smart.liveplayer.tv.Ads

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Activity.LanguageActivity
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.Activity.UninstallSurveyActivity
import com.iptv.online.smart.liveplayer.tv.Activity.WelcomeScreenActivity
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata


object AppResumeWelcomeManager : LifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val TAG = "WelcomeFlow"
    private var currentActivity: Activity? = null

    private var isColdStart = true

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        Log.d(TAG, "App foreground (ON_START) -> currentActivity=${currentActivity?.javaClass?.simpleName}")

        if (isColdStart) {
            isColdStart = false
            Log.d(TAG, "cold start -> skip welcome screen")
            return
        }

        val activity = currentActivity ?: run {
            Log.d(TAG, "currentActivity null -> skip")
            return
        }


        if (activity is SplashActivity ||
            activity is WelcomeScreenActivity ||
            activity is LanguageActivity ||
            activity is IntroActivity ||
            activity is UninstallSurveyActivity
        ) {
            Log.d(TAG, "skip on ${activity.javaClass.simpleName}")
            return
        }
        if (AppOpenManager.getInstance().isInterstitialShowing) {
            Log.d(TAG, "interstitial showing -> skip welcome")
            return
        }


        if (!AppOpenManager.getInstance().isInitialized) {
            Log.d(TAG, "sdk not initialized -> skip welcome")
            return
        }


        if (isPurchased(activity)) {
            Log.d(TAG, "purchased -> skip welcome")
            return
        }
        val config = RemoteConfigdata(activity)
        val unit = AdRemoteConfig.getInstance().inter_welcome_back

// Base SDK nu eligibility check - organic/UA user pramane SDK nakki kare chhe.
        val isEligible = ERainAd.getInstance()
            .getShouldDisplayInterWelcomeBack(unit.enableUaCheck) == true

        Log.d(
            TAG,
            "flags -> isNeedToShowADs=${config.isNeedToShowADs}, " +
                    "interwelcomeback=${config.interwelcomeback}, " +
                    "unitEnable=${unit.isEnable}, sdkEligible=$isEligible"
        )

        if (!config.isNeedToShowADs || !config.interwelcomeback || !unit.isEnable || !isEligible) {
            Log.d(TAG, "welcome back disabled -> skip")
            return
        }
        if (AppOpenManager.getInstance().isDisableAdResumeByClickAction) {
            AppOpenManager.getInstance().isDisableAdResumeByClickAction = false
            Log.d(TAG, "back from ad click -> skip welcome")
            return
        }

        Log.d(TAG, "Opening WelcomeScreen over ${activity.javaClass.simpleName}")
        val intent = Intent(activity, WelcomeScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
