package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro4Binding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeOnboardingNormal2


class IntroFragment4 : BaseFragment<FragmentIntro4Binding>(), LazyShowAds {


    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(requireContext())
    }

    override fun setViewBinding() = FragmentIntro4Binding.inflate(layoutInflater)

    override fun bindObjects() {

        if (configScript!!.nativehome2005) {
            InfinityAdsManager.loadAd(
                requireActivity(),
                AdsId.nativehome2005,
                R.layout.layout_native_ad_medium,
                "native_home_2005"
            )
        }

    }

    override fun bindListener() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
    }

    override fun bindMethod() {

    }

    override fun showAds() {
        nativeAds()

    }


    private fun nativeAds() {
        if (!isAdded) return
        val currentActivity = activity ?: return

        if (!currentActivity    .getShouldDisplayNativeOnboardingNormal2()) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            return
        }

        val currentConfig = configScript ?: return

        if (currentConfig.isNeedToShowADs) {
            if (currentConfig.nativeOnb14On) {
                val handledAds = mutableSetOf<String>()
                val retriedTags = mutableSetOf<String>()

                lifecycleScope.launchWhenStarted {
                    InfinityAdsManager.adStateFlow.collect { states ->
                        val activeActivity = activity ?: return@collect

                        val tag = if (activeActivity.isIntroFlowDone())
                            "native_onboarding_2_4"
                        else "native_onboarding_1_4"

                        val state = states[tag]

                        if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                            handledAds.add(tag)
                            Log.d("AdManager123", "[$tag] Success, 🚀 showing: [${state.adsID}]")
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
                                    Log.d(
                                        "AdManager123",
                                        "Height set to: $heightInPx px for tag: $tag"
                                    )
                                }
                            } else {
                                Log.e(
                                    "AdManager123",
                                    "CTA Button not found in layout for tag: $tag"
                                )
                            }
                            //

                        } else if (state is NativeAdUiState.Loading) {
                            if (binding.frAds.visibility != View.VISIBLE) {
                                binding.adShimmer.root.visible
                            }
                        } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            // Ad ready nathi -> 90%+ show rate mate ek j var jate FRESH load (fallback).
                            if (binding.frAds.visibility != View.VISIBLE) {
                                if (retriedTags.add(tag)) {
                                    binding.adShimmer.root.visible
                                    val adId = if (activeActivity.isIntroFlowDone())
                                        AdsId.nativeOnboarding2_4 else AdsId.nativeOnboarding1_4
                                    InfinityAdsManager.loadAd(
                                        activeActivity, adId, R.layout.layout_native_ad_large, tag
                                    )
                                    Log.d("AdManager123", "[$tag] fallback reload triggered")
                                } else {
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


        } else {
            binding.adShimmer.root.gone
            binding.frAds.gone
        }
    }
}