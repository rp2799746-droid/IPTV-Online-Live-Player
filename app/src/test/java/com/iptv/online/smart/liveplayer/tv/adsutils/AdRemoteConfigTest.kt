package com.iptv.online.smart.liveplayer.tv.adsutils

import org.junit.Assert.assertFalse
import org.junit.Test

class AdRemoteConfigTest {

    @Test
    fun interOnboardingRemainsDisabledWhenRemoteConfigProvidesAnEnabledId() {
        val constructor = AdRemoteConfig::class.java
            .getDeclaredConstructor(Map::class.java)
            .apply { isAccessible = true }
        val config = constructor.newInstance(
            mapOf("inter_onboarding" to AdUnitConfig(id = "remote-interstitial-id", isEnable = true)),
        )

        assertFalse(config.inter_onboarding.isEnable)
        assertFalse(config.inter_onboarding.id.isNotBlank())
    }
}
