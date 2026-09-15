package com.iptv.online.smart.liveplayer.tv.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeAdsEntryRuleTest {

    @Test
    fun welcomeHasPriorityWhenBothResumePlacementsAreEnabled() {
        assertEquals(
            ResumeAdsEntryMode.WELCOME,
            ResumeAdsEntryRule.resolve(
                adsEnabled = true,
                openResumeEnabled = true,
                welcomeEnabled = true,
            ),
        )
    }

    @Test
    fun welcomeIsUsedOnlyWhenOpenResumeIsDisabled() {
        assertEquals(
            ResumeAdsEntryMode.WELCOME,
            ResumeAdsEntryRule.resolve(
                adsEnabled = true,
                openResumeEnabled = false,
                welcomeEnabled = true,
            ),
        )
    }

    @Test
    fun disabledAdsCannotEnterAnyResumeFlow() {
        assertEquals(
            ResumeAdsEntryMode.NONE,
            ResumeAdsEntryRule.resolve(
                adsEnabled = false,
                openResumeEnabled = true,
                welcomeEnabled = true,
            ),
        )
    }
}
