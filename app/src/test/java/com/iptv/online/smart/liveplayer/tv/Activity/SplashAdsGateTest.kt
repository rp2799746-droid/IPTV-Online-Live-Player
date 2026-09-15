package com.iptv.online.smart.liveplayer.tv.Activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashAdsGateTest {

    @Test
    fun remoteInformationalFlagDoesNotDisableAdsForFreeUser() {
        assertTrue(shouldEnableAdsForPurchase(isPurchased = false))
    }

    @Test
    fun purchaseDisablesGlobalAdsGate() {
        assertFalse(shouldEnableAdsForPurchase(isPurchased = true))
    }
}
