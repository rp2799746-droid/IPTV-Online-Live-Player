package com.iptv.online.smart.liveplayer.tv.adsutils

/**
 * The Language placement is selected from one durable fact only: whether the
 * user has completed Language before.  Keeping this value explicit on the
 * Intent prevents Activity recreation from accidentally falling back to _1.
 */
enum class LanguageAdVariant {
    FIRST,
    SUBSEQUENT;

    companion object {
        const val EXTRA = "language_ad_variant"
        private const val FIRST_VALUE = "first"
        private const val SUBSEQUENT_VALUE = "subsequent"

        fun fromLanguageCompleted(completed: Boolean): LanguageAdVariant =
            if (completed) SUBSEQUENT else FIRST

        fun fromIntentValue(value: String?, languageCompleted: Boolean): LanguageAdVariant =
            when (value) {
                FIRST_VALUE -> FIRST
                SUBSEQUENT_VALUE -> SUBSEQUENT
                else -> fromLanguageCompleted(languageCompleted)
            }

        fun intentValue(variant: LanguageAdVariant): String =
            if (variant == FIRST) FIRST_VALUE else SUBSEQUENT_VALUE
    }
}
