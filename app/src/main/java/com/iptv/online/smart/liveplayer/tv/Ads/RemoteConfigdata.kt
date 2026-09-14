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

    var isNeedToShowADs: Boolean
        get() = Preference.getBoolean(context, "is_need_to_show_ads", true)
        set(value) = Preference.setBoolean(context, "is_need_to_show_ads", value)





    // ---------------- COLLAPSIBLE BANNER ----------------
    var bannerCollapsehomeOn: Boolean
        get() = Preference.getBoolean(context, "ad_banner_collapse__home_on", true)
        set(value) = Preference.setBoolean(context, "ad_banner_collapse__home_on", value)


    var isIntroPage: Boolean
        get() = Preference.getBoolean(context, "is_intro_page", true)
        set(value) = Preference.setBoolean(context, "is_intro_page", value)


    var isFullNativeCloseBtn: Boolean
        get() = Preference.getBoolean(context, "show_full_native_close_btn", true)
        set(value) = Preference.setBoolean(context, "show_full_native_close_btn", value)

    // iptv2 jevu: 'is_in_app_update' -> fakt fetch (IPTV2 ma pan use nathi thatu). Default false.
    var isInAppUpdate: Boolean
        get() = Preference.getBoolean(context, "is_in_app_update", false)
        set(value) = Preference.setBoolean(context, "is_in_app_update", value)

    // iptv2 jevu: 'is_all_ads_show' -> fakt fetch (IPTV2 ma pan use nathi thatu). Default true.
    var isAllAdsShow: Boolean
        get() = Preference.getBoolean(context, "is_all_ads_show", true)
        set(value) = Preference.setBoolean(context, "is_all_ads_show", value)

    var timeDelayLanguageDone: Long
        get() = Preference.getLong(context, "time_delay_show_language_done_button", 2000L)
        set(value) = Preference.setLong(context, "time_delay_show_language_done_button", value)



    var nativePlaylist: Boolean
        get() = Preference.getBoolean(context, "native_playlist", true)
        set(value) = Preference.setBoolean(context, "native_playlist", value)


    var nativeMirroring: Boolean
        get() = Preference.getBoolean(context, "native_mirroring", true)
        set(value) = Preference.setBoolean(context, "native_mirroring", value)
    var nativeChannelList: Boolean
        get() = Preference.getBoolean(context, "native_channel_list", true)
        set(value) = Preference.setBoolean(context, "native_channel_list", value)

    var nativechannel1409: Boolean
        get() = Preference.getBoolean(context, "native_channel_1409", true)
        set(value) = Preference.setBoolean(context, "native_channel_1409", value)
    var nativefavorite2005: Boolean
        get() = Preference.getBoolean(context, "native_favorite_2005", true)
        set(value) = Preference.setBoolean(context, "native_favorite_2005", value)

    var nativehistory2005: Boolean
        get() = Preference.getBoolean(context, "native_history_2005", true)
        set(value) = Preference.setBoolean(context, "native_history_2005", value)


    var interwelcomeback: Boolean
        get() = Preference.getBoolean(context, "interwelcomeback", true)
        set(value) = Preference.setBoolean(context, "interwelcomeback", value)


}
