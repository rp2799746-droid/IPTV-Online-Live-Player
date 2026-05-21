package com.iptv.online.smart.liveplayer.tv.adsutils

import com.ads.module.ads.wrapper.ApNativeAd

sealed class NativeAdUiState {
    object Loading : NativeAdUiState()
    data class Success(val ad: ApNativeAd, val tagName: String,val adsID: String,val ctaHeight: Long) : NativeAdUiState()
    data class Failed(val tagName: String,val adsID: String, val error: String? = null) : NativeAdUiState()
    data class Empty(val tagName: String,val adsID: String) : NativeAdUiState()
}