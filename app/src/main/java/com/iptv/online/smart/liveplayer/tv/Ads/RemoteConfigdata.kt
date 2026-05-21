package com.iptv.online.smart.liveplayer.tv.adsutils

import android.content.Context
import com.iptv.online.smart.liveplayer.tv.utils.Preference

class RemoteConfigdata(
    private val context: Context,
) {

    // ---------------- EXISTING FIELDS ----------------
    var delayButtonDoneLanguage: Boolean
        get() = Preference.getBoolean(context, "delay_button_done_language", true)
        set(value) = Preference.setBoolean(context, "delay_button_done_language", value)

    var privacyLink: String
        get() = Preference.getString(context, "privacy_policy")
        set(value) = Preference.setString(context, "privacy_policy", value)

    var height_button_cta: String
        get() = Preference.getString(context, "height_button_cta")
        set(value) = Preference.setString(context, "height_button_cta", value)



//    var terms_of_uses: String
//        get() = Preference.getString(context, "terms_of_uses")
//        set(value) = Preference.setString(context, "terms_of_uses", value)
//
//    var aboutUsLink: String
//        get() = Preference.getString(context, "about_us")
//        set(value) = Preference.setString(context, "about_us", value)

    var isNeedToShowADs: Boolean
        get() = Preference.getBoolean(context, "is_need_to_show_ads", true)
        set(value) = Preference.setBoolean(context, "is_need_to_show_ads", value)


    // ---------------- SPLASH ----------------
    var interSplashOn: Boolean
        get() = Preference.getBoolean(context, "ad_inter_splash_on", true)
        set(value) = Preference.setBoolean(context, "ad_inter_splash_on", value)

    var bannerSplashOn: Boolean
        get() = Preference.getBoolean(context, "ad_banner_splash_on", true)
        set(value) = Preference.setBoolean(context, "ad_banner_splash_on", value)

    var appOpenOn: Boolean
        get() = Preference.getBoolean(context, "ad_app_open_on", true)
        set(value) = Preference.setBoolean(context, "ad_app_open_on", value)


    // ---------------- LANGUAGE SCREEN ----------------
    var nativeLang1On: Boolean
        get() = Preference.getBoolean(context, "ad_native_lang1_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_lang1_on", value)

    var nativeLang2On: Boolean
        get() = Preference.getBoolean(context, "ad_native_lang2_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_lang2_on", value)

    var nativeLang1ClickOn: Boolean
        get() = Preference.getBoolean(context, "ad_native_lang1_click_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_lang1_click_on", value)

    var nativeLang2ClickOn: Boolean
        get() = Preference.getBoolean(context, "ad_native_lang2_click_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_lang2_click_on", value)


    // ---------------- ONBOARDING ----------------
    var nativeOnb11On: Boolean
        get() = Preference.getBoolean(context, "ad_native_onb_1_1_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_onb_1_1_on", value)

    var nativeOnbFull1On: Boolean
        get() = Preference.getBoolean(context, "ad_native_onb_full_1_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_onb_full_1_on", value)

    var nativeOnb14On: Boolean
        get() = Preference.getBoolean(context, "ad_native_onb_1_4_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_onb_1_4_on", value)

    var nativeOnbFull2On: Boolean
        get() = Preference.getBoolean(context, "ad_native_onb_full_2_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_onb_full_2_on", value)

    var nativeOnb21On: Boolean
        get() = Preference.getBoolean(context, "native_onboarding_2_1", true)
        set(value) = Preference.setBoolean(context, "native_onboarding_2_1", value)

    var nativeOnb24On: Boolean
        get() = Preference.getBoolean(context, "ad_native_onb_2_4_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_onb_2_4_on", value)

    var interOnboardingOn: Boolean
        get() = Preference.getBoolean(context, "ad_inter_onb_on", true)
        set(value) = Preference.setBoolean(context, "ad_inter_onb_on", value)


    // ---------------- HOME ----------------
    var nativeHomeOn: Boolean
        get() = Preference.getBoolean(context, "ad_native_home_on", true)
        set(value) = Preference.setBoolean(context, "ad_native_home_on", value)

    var interHomeOn: Boolean
        get() = Preference.getBoolean(context, "ad_inter_home_on", true)
        set(value) = Preference.setBoolean(context, "ad_inter_home_on", value)


    // ---------------- COLLAPSIBLE BANNER ----------------
    var bannerCollapsehomeOn: Boolean
        get() = Preference.getBoolean(context, "ad_banner_collapse_home_on", true)
        set(value) = Preference.setBoolean(context, "ad_banner_collapse__home_on", value)


    // ---------------- REWARD ----------------
    var rewardOn: Boolean
        get() = Preference.getBoolean(context, "ad_reward_on", true)
        set(value) = Preference.setBoolean(context, "ad_reward_on", value)

    var isInterOnSplash: Boolean
        get() = Preference.getBoolean(context, "is_inter_on_splash", true)
        set(value) = Preference.setBoolean(context, "is_inter_on_splash", value)


    var interback: Boolean
        get() = Preference.getBoolean(context, "inter_back", true)
        set(value) = Preference.setBoolean(context, "inter_back", value)

    var nativeChannel: Boolean
        get() = Preference.getBoolean(context, "native_channel", true)
        set(value) = Preference.setBoolean(context, "native_channel", value)

    var nativeFavorite: Boolean
        get() = Preference.getBoolean(context, "native_favorite", true)
        set(value) = Preference.setBoolean(context, "native_favorite", value)

    var nativeHistory: Boolean
        get() = Preference.getBoolean(context, "native_history", true)
        set(value) = Preference.setBoolean(context, "native_history", value)
    var nativePlaylist: Boolean
        get() = Preference.getBoolean(context, "native_playlist", true)
        set(value) = Preference.setBoolean(context, "native_playlist", value)

    var interMirroring: Boolean
        get() = Preference.getBoolean(context, "inter_mirroring", true)
        set(value) = Preference.setBoolean(context, "inter_mirroring", value)
    var nativeMirroring: Boolean
        get() = Preference.getBoolean(context, "native_mirroring", true)
        set(value) = Preference.setBoolean(context, "native_mirroring", value)
    var nativeChannelList: Boolean
        get() = Preference.getBoolean(context, "native_channel_list", true)
        set(value) = Preference.setBoolean(context, "native_channel_list", value)

    var interAddPlaylist: Boolean
        get() = Preference.getBoolean(context, "inter_add_playlist", true)
        set(value) = Preference.setBoolean(context, "inter_add_playlist", value)
    var interSplashUninstall: Boolean
        get() = Preference.getBoolean(context, "inter_splash_uninstall", true)
        set(value) = Preference.setBoolean(context, "inter_splash_uninstall", value)
    var bannerSplashUninstall: Boolean
        get() = Preference.getBoolean(context, "banner_splash_uninstall", true)
        set(value) = Preference.setBoolean(context, "banner_splash_uninstall", value)
    var nativeUninstall: Boolean
        get() = Preference.getBoolean(context, "native_uninstall", true)
        set(value) = Preference.setBoolean(context, "native_uninstall", value)
    var nativesurveyUninstall: Boolean
        get() = Preference.getBoolean(context, "native_survey_uninstall", true)
        set(value) = Preference.setBoolean(context, "native_survey_uninstall", value)







}
