package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro1Binding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeOnboardingNormal1


class IntroFragment1 : BaseFragment<FragmentIntro1Binding>() {


    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(requireActivity())
    }

    override fun setViewBinding() = FragmentIntro1Binding.inflate(layoutInflater)

    @SuppressLint("SuspiciousIndentation")
    override fun bindObjects() {
        nativeAds()
        val isDone = requireActivity().isIntroFlowDone()


        val isFullAdEnabled =
            if (isDone) configScript.nativeOnbFull2On else configScript.nativeOnbFull1On

          if (configScript.isNeedToShowADs) {
           if (isFullAdEnabled) {
               val adIdFull =
                   if (isDone) AdsId.nativeOnboardingFull2 else AdsId.nativeOnboardingFull1
               val tagFull = if (isDone) "native_onboarding_full_2" else "native_onboarding_full_1"
               AdsManager.loadAd(requireActivity(), adIdFull, R.layout.layout_native_ad_full, tagFull)
           }


       }
    }

    override fun bindListener() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment()}
    }

    private fun nativeAds() {
        Log.d("hh", "nativeAds: "+requireActivity().getShouldDisplayNativeOnboardingNormal1())
        if (!requireActivity().getShouldDisplayNativeOnboardingNormal1()) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            return
        }
        if (configScript.isNeedToShowADs && configScript.nativeOnb11On) {
            val handledAds = mutableSetOf<String>()
            val retriedTags = mutableSetOf<String>()

            lifecycleScope.launchWhenStarted {
                AdsManager.adStateFlow.collect { states ->
                    val activeActivity = activity ?: return@collect

                    val tag = if (activeActivity.isIntroFlowDone())
                        "native_onboarding_2_1"
                    else "native_onboarding_1_1"

                    val state = states[tag]

                    if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                        handledAds.add(tag)
                        Log.d("AdManager123", "[$tag] Success, 🚀 showing ID: [${state.adsID}]")

                        binding.adShimmer.root.gone
                        binding.frAds.visible

                        ERainAd.getInstance().populateNativeAdView(
                            activeActivity,
                            state.ad,
                            binding.frAds,
                            binding.adShimmer.root
                        )
                        //// Height cta
                        val remoteHeightDp = state.ctaHeight
                        val adBtn = binding.frAds.findViewById<View>(R.id.ad_call_to_action)

                        if (adBtn != null) {
                            val density = resources.displayMetrics.density
                            val heightInPx = (remoteHeightDp * density).toInt()

                            val params = adBtn.layoutParams
                            params?.let {
                                it.height = heightInPx
                                adBtn.layoutParams = it
                                adBtn.requestLayout()
                                Log.d("AdManager123", "Height set to: $heightInPx px for tag: $tag")
                            }
                        } else {
                            Log.e("AdManager123", "CTA Button not found in layout for tag: $tag")
                        }
                        //

                    } else if (state is NativeAdUiState.Loading) {
                        if (binding.frAds.visibility != View.VISIBLE) {
                            binding.adShimmer.root.visible
                        }
                    } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                        // Ad ready nathi (preload j nathi thayu ke fail gayu) ->
                        // 90%+ show rate mate ahiya ek j var jate FRESH load karo (fallback).
                        if (binding.frAds.visibility != View.VISIBLE) {
                            if (retriedTags.add(tag)) {
                                binding.adShimmer.root.visible
                                val adId = if (activeActivity.isIntroFlowDone())
                                    AdsId.nativeOnboarding2_1 else AdsId.nativeOnboarding1_1
                                AdsManager.loadAd(
                                    activeActivity, adId, R.layout.layout_native_ad_large, tag
                                )
                                Log.d("AdManager123", "[$tag] fallback reload triggered")
                            } else {
                                // Fallback pachi pan na aavyu -> hide.
                                binding.adShimmer.root.gone
                                binding.frAds.gone
                            }
                        }
                    }
                }
            }
        } else {
            binding.adShimmer.root.gone
            binding.frAds.gone
        }
    }

    override fun bindMethod() {

    }


}