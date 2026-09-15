package com.iptv.online.smart.liveplayer.tv.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.iptv.online.smart.liveplayer.tv.App

class AppActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) {
        App.currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        if (App.currentActivity === activity) App.currentActivity = null
    }
}