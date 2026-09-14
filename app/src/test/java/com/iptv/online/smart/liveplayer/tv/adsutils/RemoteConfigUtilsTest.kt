package com.iptv.online.smart.liveplayer.tv.adsutils

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteConfigUtilsTest {

    @Test
    fun `uses bundled ad config before firebase completes`() {
        assertEquals(
            "asset-json",
            resolveRemoteValue(
                fetchCompleted = false,
                remoteValue = "firebase-json",
                defaultValue = "asset-json"
            )
        )
    }

    @Test
    fun `uses bundled ad config when firebase value is blank`() {
        assertEquals(
            "asset-json",
            resolveRemoteValue(
                fetchCompleted = true,
                remoteValue = "",
                defaultValue = "asset-json"
            )
        )
    }

    @Test
    fun `uses firebase ad config after fetch completes`() {
        assertEquals(
            "firebase-json",
            resolveRemoteValue(
                fetchCompleted = true,
                remoteValue = "firebase-json",
                defaultValue = "asset-json"
            )
        )
    }
}
