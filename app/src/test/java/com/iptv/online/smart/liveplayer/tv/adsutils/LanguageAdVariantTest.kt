package com.iptv.online.smart.liveplayer.tv.adsutils

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageAdVariantTest {

    @Test
    fun completedLanguageUsesSubsequentVariant() {
        assertEquals(
            LanguageAdVariant.SUBSEQUENT,
            LanguageAdVariant.fromLanguageCompleted(true),
        )
    }

    @Test
    fun missingIntentUsesPersistentCompletionState() {
        assertEquals(
            LanguageAdVariant.SUBSEQUENT,
            LanguageAdVariant.fromIntentValue(null, true),
        )
    }
}
