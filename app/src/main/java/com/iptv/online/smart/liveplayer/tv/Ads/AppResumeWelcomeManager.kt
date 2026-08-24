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
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.Activity.WelcomeScreenActivity
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata

/**
 * App jyare background mathi foreground ma pachhi aave (resume) tyare
 * App-Open resume ad ni jagya e WelcomeScreenActivity kholi aape che.
 *
 * App.onCreate() ma init(this) call karvano.
 */
object AppResumeWelcomeManager : LifecycleObserver, Application.ActivityLifecycleCallbacks {

    private const val TAG = "WelcomeFlow"
    private var currentActivity: Activity? = null

    // App pehli var (cold start) khule tyare welcome screen na batavo — e resume nથી.
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

        // AppOpen jem j: fakt Splash par skip.
        // WelcomeScreen par pan skip karvu j pade (nahitar infinite loop thay).
        if (activity is SplashActivity ||
            activity is WelcomeScreenActivity
        ) {
            Log.d(TAG, "skip on ${activity.javaClass.simpleName}")
            return
        }

        val config = RemoteConfigdata(activity)
        Log.d(TAG, "flags -> isNeedToShowADs=${config.isNeedToShowADs}, interwelcomeback=${config.interwelcomeback}")
        if (!config.isNeedToShowADs || !AdRemoteConfig.getInstance().inter_welcome_back.isEnable) {
            Log.d(TAG, "welcome back disabled by remote config -> skip")
            return
        }

        Log.d(TAG, "Opening WelcomeScreen over ${activity.javaClass.simpleName}")
        val intent = Intent(activity, WelcomeScreenActivity::class.java)
        // Tarat j (animation vagar) kholo, jethi niche ni screen na dekhay
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
