package com.iptv.online.smart.liveplayer.tv.adsutils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptv.online.smart.liveplayer.tv.BuildConfig

/**
 * iptv2 jevu JSON-driven ad config (Gson - aa project ma Moshi nathi).
 *  - Firebase remote config (ad_remote_config / ad_rem_dup) mathi JSON aave to e vapre,
 *    nahi to bundled asset (release=ad_config.json, debug=ad_config_debug.json).
 *  - App.onCreate() ma initialize(context) (asset), pachi SplashActivity ma
 *    initialize(context, firebaseJson) call thay chhe.
 */
class AdRemoteConfig private constructor(
    val ads: Map<String, AdUnitConfig>
) {

    fun unit(key: String): AdUnitConfig = ads[key] ?: AdUnitConfig()

    // ---- Splash ----
    val inter_splash get() = unit("inter_splash")
    val banner_splash get() = unit("banner_splash")
    val open_resume get() = unit("open_resume")

    // ---- Language ----
    val native_language_1 get() = unit("native_language_1")
    val native_language_1_click get() = unit("native_language_1_click")
    val native_language_2 get() = unit("native_language_2")
    val native_language_2_click get() = unit("native_language_2_click")

    // ---- Onboarding ----
    val native_onboarding_1_1 get() = unit("native_onboarding_1_1")
    val native_onboarding_2_1 get() = unit("native_onboarding_2_1")
    val native_onboarding_1_4 get() = unit("native_onboarding_1_4")
    val native_onboarding_2_4 get() = unit("native_onboarding_2_4")
    val native_onboarding_fullscreen_1_2 get() = unit("native_onboarding_fullscreen_1_2")
    val native_onboarding_fullscreen_2_2 get() = unit("native_onboarding_fullscreen_2_2")
    val inter_onboarding get() = unit("inter_onboarding")

    // ---- Home ----
    val inter_home get() = unit("inter_home")
    val inter_back get() = unit("inter_back")
    val banner_collapsible_home get() = unit("banner_collapsible_home")
    val native_home get() = unit("native_home")
    val native_channel get() = unit("native_channel")
    val native_favorite get() = unit("native_favorite")
    val native_history get() = unit("native_history")

    // ---- Playlist / Mirroring ----
    val native_playlist get() = unit("native_playlist")
    val native_channellist get() = unit("native_channellist")
    val inter_add_playlist get() = unit("inter_add_playlist")
    val inter_mirroring get() = unit("inter_mirroring")
    val native_mirroring get() = unit("native_mirroring")

    // ---- Welcome-back / Uninstall ----
    val inter_welcome_back get() = unit("inter_welcome_back")
    val inter_splash_uninstall get() = unit("inter_splash_uninstall")
    val banner_splash_uninstall get() = unit("banner_splash_uninstall")
    val native_uninstall get() = unit("native_uninstall")
    val native_survey_uninstall get() = unit("native_survey_uninstall")

    companion object {
        private const val RELEASE_FILE = "ad_config.json"
        private const val DEBUG_FILE = "ad_config_debug.json"
        private const val TAG = "AdRemoteConfig"

        @Volatile
        private var instance: AdRemoteConfig? = null

        /** Init nathi thayu to pan crash na thay - empty config return kare. */
        fun getInstance(): AdRemoteConfig = instance ?: AdRemoteConfig(emptyMap())

        fun isInitialized(): Boolean = instance != null

        /** Asset mathi (App.onCreate ma pehli var). */
        fun initialize(context: Context) {
            synchronized(this) {
                val fileName = if (BuildConfig.DEBUG) DEBUG_FILE else RELEASE_FILE
                instance = fromAssets(context, fileName)
            }
        }

        /** Firebase-fetched JSON thi (SplashActivity ma). Blank/fail thay to asset fallback. */
        fun initialize(context: Context, json: String?) {
            synchronized(this) {
                val fileName = if (BuildConfig.DEBUG) DEBUG_FILE else RELEASE_FILE
                instance = if (!json.isNullOrBlank()) {
                    fromJson(json) ?: fromAssets(context, fileName)
                } else {
                    fromAssets(context, fileName)
                }
            }
        }

        private fun parse(json: String): Map<String, AdUnitConfig> {
            val type = object : TypeToken<Map<String, AdUnitConfig>>() {}.type
            return Gson().fromJson(json, type) ?: emptyMap()
        }

        private fun fromJson(json: String): AdRemoteConfig? {
            return try {
                val map = parse(json)
                Log.d(TAG, "Loaded from Firebase json with ${map.size} ad units")
                AdRemoteConfig(map)
            } catch (e: Exception) {
                Log.e(TAG, "Firebase json parse fail: ${e.message}")
                null
            }
        }

        private fun fromAssets(context: Context, fileName: String): AdRemoteConfig {
            return try {
                val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val map = parse(json)
                Log.d(TAG, "Loaded $fileName with ${map.size} ad units")
                AdRemoteConfig(map)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load $fileName: ${e.message}")
                AdRemoteConfig(emptyMap())
            }
        }
    }
}
