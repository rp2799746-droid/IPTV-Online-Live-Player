package com.iptv.online.smart.liveplayer.tv.adsutils

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * iptv2 jevu ad-unit config (JSON mathi). Gson thi parse thay chhe.
 * JSON key `enable_ua_check` -> field `enableUaCheck`.
 */
@Keep
data class AdUnitConfig(
    @SerializedName("id") val id: String = "",
    @SerializedName("isEnable") val isEnable: Boolean = false,
    @SerializedName("enable_ua_check") val enableUaCheck: Boolean = false,
    @SerializedName("reloadIntervalSeconds") val reloadIntervalSeconds: Int? = null,
    @SerializedName("colorCTA") val colorCTA: String = "default",
    @SerializedName("heightCTA") val heightCTA: Int = 40,
    @SerializedName("positionCTA") val positionCTA: String = "BOTTOM",
    @SerializedName("components") val components: List<String> = listOf("icon_headline", "body", "media", "cta")
)
