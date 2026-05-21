package com.iptv.online.smart.liveplayer.tv.adsutils

import android.content.Context
import com.ads.module.ads.ERainAd

fun Context.getShouldDisplayNativeOnboardingFull1() = ERainAd.getInstance().shouldDisplayNativeOnboardingFull1

fun Context.getShouldDisplayInterOnboarding() = ERainAd.getInstance().shouldDisplayInterOnboarding
fun Context.getShouldDisplayWidgetUninstall() = ERainAd.getInstance().shouldDisplayWidgetUninstall
fun Context.getSHouldDisplayHighCTA() = ERainAd.getInstance().shouldDisplayHighCTA

//fun Context.getShouldDisplayNativeOnboardingFull1() = false
//
//fun Context.getShouldDisplayInterOnboarding() = false
//fun Context.getShouldDisplayWidgetUninstall() = false
//
//fun Context.getSHouldDisplayHighCTA() = false

