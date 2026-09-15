package com.iptv.online.smart.liveplayer.tv.app

import com.iptv.online.smart.liveplayer.tv.App
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata

enum class ResumeAdsEntryMode {
    OPEN_RESUME,
    WELCOME,
    NONE,
}

object ResumeAdsEntryRule {

    internal fun resolve(
        adsEnabled: Boolean,
        openResumeEnabled: Boolean,
        welcomeEnabled: Boolean,
    ): ResumeAdsEntryMode {
        if (!adsEnabled) return ResumeAdsEntryMode.NONE
        return if (welcomeEnabled) ResumeAdsEntryMode.WELCOME else ResumeAdsEntryMode.NONE
    }

    fun currentMode(): ResumeAdsEntryMode {
        if (!AdRemoteConfig.isInitialized()) return ResumeAdsEntryMode.NONE
        val context = App.app ?: return ResumeAdsEntryMode.NONE
        val remoteData = RemoteConfigdata(context)
        val config = AdRemoteConfig.getInstance()
        return resolve(
            adsEnabled = remoteData.isNeedToShowADs,
            openResumeEnabled = false,
            welcomeEnabled = remoteData.interwelcomeback &&
                config.inter_welcome_back.isEnable &&
                config.inter_welcome_back.id.isNotBlank(),
        )
    }

    fun shouldEnableOpenResume(): Boolean =
        currentMode() == ResumeAdsEntryMode.OPEN_RESUME

    fun shouldShowWelcomeOnResume(): Boolean =
        currentMode() == ResumeAdsEntryMode.WELCOME
}
