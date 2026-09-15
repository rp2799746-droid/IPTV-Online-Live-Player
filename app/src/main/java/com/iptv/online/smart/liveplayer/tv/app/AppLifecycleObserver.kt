package com.iptv.online.smart.liveplayer.tv.app

import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Activity.LanguageActivity
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity
import com.iptv.online.smart.liveplayer.tv.Activity.UninstallScreenActivity
import com.iptv.online.smart.liveplayer.tv.Activity.UninstallSurveyActivity
import com.iptv.online.smart.liveplayer.tv.Activity.WelcomeScreenActivity
import com.iptv.online.smart.liveplayer.tv.App
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val listActivityDisableResume = arrayListOf(
        SplashActivity::class.java,
        LanguageActivity::class.java,
        IntroActivity::class.java,
        WelcomeScreenActivity::class.java,
        UninstallScreenActivity::class.java,
        UninstallSurveyActivity::class.java,
    )

    override fun onStart(owner: LifecycleOwner) {
        val currentActivity = App.currentActivity ?: return
        val isDisable = listActivityDisableResume.any { it.isInstance(currentActivity) }
        val welcomeConfig = AdRemoteConfig.getInstance().inter_welcome_back

        if (!isDisable &&
            ResumeAdsEntryRule.shouldShowWelcomeOnResume() &&
            !AppOpenManager.getInstance().isInterstitialShowing &&
            !isPurchased(currentActivity) &&
            ERainAd.getInstance()
                .getShouldDisplayInterWelcomeBack(welcomeConfig.enableUaCheck) == true
        ) {
            if (AppOpenManager.getInstance().isDisableAdResumeByClickAction) {
                AppOpenManager.getInstance().isDisableAdResumeByClickAction = false
            } else {
                currentActivity.startActivity(
                    Intent(currentActivity, WelcomeScreenActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                )
                currentActivity.overridePendingTransition(0, 0)
            }
        }
    }
}
