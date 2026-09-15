package com.iptv.online.smart.liveplayer.tv.adsutils

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.gson.Gson
import com.iptv.online.smart.liveplayer.tv.App
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.app.AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON

internal fun resolveRemoteValue(
    fetchCompleted: Boolean,
    remoteValue: String,
    defaultValue: String,
): String = if (fetchCompleted) remoteValue.ifBlank { defaultValue } else defaultValue

object RemoteConfigUtils {
    var completed = false

    private const val DELAY_SHOW_LANGUAGE_DONE_BUTTON = "delay_button_done_language"
    private const val TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON = "time_delay_show_language_done_button"
    private const val ON_SHOW_NAVIGATION_BUTTON = "on_show_navigation_button"
    private const val ON_ENABLE_UNINSTALL_WIDGET = "on_enable_uninstall_widget"
    // Ad unit JSON published in Firebase Remote Config.
    private val AD_REMOTE_CONFIG = if (BuildConfig.DEBUG) "ad_config_debug" else "ad_config"
    private const val PRIVACY_POLICY = "privacy_policy"
    private const val HEIGHT_BUTTON_CTA = "height_button_cta"
    private const val IS_INTRO_PAGE = "is_intro_page"
    private const val SHOW_FULL_NATIVE_CLOSE_BUTTON = "show_full_native_close_btn"
    private const val IS_IN_APP_UPDATE = "is_in_app_update"
    private const val IS_ALL_ADS_SHOW = "is_all_ads_show"
    private const val INTER_WELCOME_BACK = "interwelcomeback"

    private const val AD_REMOTE_CONFIG_FILE_DEBUG = "ad_config_debug.json"
    private const val AD_REMOTE_CONFIG_FILE_RELEASE = "ad_config.json"

    private val mapConditionForAd: HashMap<String, Any> = hashMapOf(
        DELAY_SHOW_LANGUAGE_DONE_BUTTON to true,
        TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON to DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON,
        ON_SHOW_NAVIGATION_BUTTON to false,
        ON_ENABLE_UNINSTALL_WIDGET to false,
        PRIVACY_POLICY to "",
        HEIGHT_BUTTON_CTA to "40",
        IS_INTRO_PAGE to true,
        SHOW_FULL_NATIVE_CLOSE_BUTTON to true,
        IS_IN_APP_UPDATE to false,
        IS_ALL_ADS_SHOW to true,
        INTER_WELCOME_BACK to true,
    )

    interface Listener {
        fun loadSuccess()
    }

    private lateinit var listener: Listener
    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val gson = Gson()

    fun init(context: Context, mListener: Listener) {
        listener = mListener
        remoteConfig = getFirebaseRemoteConfig(context)
    }

    private fun getDefaultsFromAdConfig(context: Context): Map<String, Any> {
        val defaults = mutableMapOf<String, Any>()
        try {
            if (!AdRemoteConfig.isInitialized()) {
                AdRemoteConfig.initializeFromAssets(context)
            }
            AdRemoteConfig.getInstance().ads.forEach { (key, adUnitConfig) ->
                defaults[key] = gson.toJson(adUnitConfig)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return defaults
    }

    private fun getFirebaseRemoteConfig(context: Context): FirebaseRemoteConfig {
        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 60 * 60
        }
        val defaults = getDefaultsFromAdConfig(context).toMutableMap()
        remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            mapConditionForAd.forEach { (key, value) -> defaults[key] = value }
            setDefaultsAsync(defaults)
            fetchAndActivate().addOnCompleteListener {
                listener.loadSuccess()
                completed = true
            }
        }
        return remoteConfig
    }

    fun getAdRemoteConfig(): String {
        val defaultValue = loadDefaultAdRemoteConfig()
        val remoteValue = if (completed) remoteConfig.getString(AD_REMOTE_CONFIG) else ""
        return resolveRemoteValue(completed, remoteValue, defaultValue)
    }

    fun getOnShowNavigationButton(): Boolean = getBoolean(ON_SHOW_NAVIGATION_BUTTON)

    fun getOnEnableUninstallWidget(): Boolean = getBoolean(ON_ENABLE_UNINSTALL_WIDGET)

    fun shouldDelayLanguageDoneButton(): Boolean =
        getBoolean(DELAY_SHOW_LANGUAGE_DONE_BUTTON, true)

    fun getTimeDelayButtonDoneLanguage(): Long =
        getLong(TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON, DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON)

    fun applyAppSpecificConfig(context: Context) {
        if (!completed) return
        RemoteConfigdata(context).apply {
            delayButtonDoneLanguage = getBoolean(DELAY_SHOW_LANGUAGE_DONE_BUTTON, true)
            timeDelayLanguageDone = getLong(
                TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON,
                DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON,
            )
            privacyLink = getString(PRIVACY_POLICY)
            height_button_cta = getString(HEIGHT_BUTTON_CTA, "40")
            isIntroPage = getBoolean(IS_INTRO_PAGE, true)
            isFullNativeCloseBtn = getBoolean(SHOW_FULL_NATIVE_CLOSE_BUTTON, true)
            isInAppUpdate = getBoolean(IS_IN_APP_UPDATE)
            isAllAdsShow = getBoolean(IS_ALL_ADS_SHOW, true)
            interwelcomeback = getBoolean(INTER_WELCOME_BACK, true)
        }
    }

    private fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = try {
        if (!completed) defaultValue else remoteConfig.getBoolean(key)
    } catch (exception: Exception) {
        exception.printStackTrace()
        defaultValue
    }

    private fun getLong(key: String, defaultValue: Long = 0L): Long = try {
        if (!completed) defaultValue else remoteConfig.getLong(key)
    } catch (exception: Exception) {
        exception.printStackTrace()
        defaultValue
    }

    private fun getString(key: String, defaultValue: String = ""): String = try {
        if (!completed) defaultValue else remoteConfig.getString(key).ifBlank { defaultValue }
    } catch (exception: Exception) {
        exception.printStackTrace()
        defaultValue
    }

    private fun loadDefaultAdRemoteConfig(): String {
        val fileName = if (BuildConfig.DEBUG) AD_REMOTE_CONFIG_FILE_DEBUG else AD_REMOTE_CONFIG_FILE_RELEASE
        return try {
            App.instance.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (exception: Exception) {
            exception.printStackTrace()
            "{}"
        }
    }
}