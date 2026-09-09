package com.iptv.online.smart.liveplayer.tv.activities.forIntro
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
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
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.utils.invisible


class IntroFragment4 : BaseFragment<FragmentIntro4Binding>(), LazyShowAds {


    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(requireContext())
    }

    override fun setViewBinding() = FragmentIntro4Binding.inflate(layoutInflater)

    override fun bindObjects() {
        // Next button SAUTHI PEHLA wire karo (btnNext + btnNext1 banne) -> je visible hoy e
        // wired rahe, ane niche ad code throw thay to pan Next hammesha kaam kare.
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }

        val isDone = requireActivity().isIntroFlowDone()


        if (isDone) {
            binding.layoutNext.visible
            binding.layoutNext1.invisible
        } else {
            binding.layoutNext.invisible
            binding.layoutNext1.visible
        }




        if (AdRemoteConfig.getInstance().native_home.isEnable) {
            AdsManager.loadAd(
                requireActivity(),
                AdRemoteConfig.getInstance().native_home.id,
                R.layout.layout_native_ad_medium,
                "native_home_2005"
            )
        }

    }

    override fun bindListener() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment()
            binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        }
        // Lambo translated text single line ma marquee (scroll) thava mate.
        binding.tvNext.isSelected = true
        binding.tvNext1.isSelected = true
    }

    override fun bindMethod() {

    }

    override fun showAds() {
        nativeAds()

    }
    private fun setNextRowBottomMarginPx(px: Int) {
        val lp = binding.layoutNext1.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        lp.bottomMargin = px
        binding.layoutNext1.layoutParams = lp
    }
    private fun nativeAds() {
        if (!isAdded) return
        val currentActivity = activity ?: return

        val cfg4 = if (requireActivity().isIntroFlowDone())
            AdRemoteConfig.getInstance().native_onboarding_2_4
        else AdRemoteConfig.getInstance().native_onboarding_1_4
        if (!(ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal2(cfg4.enableUaCheck) == true)) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            return
        }

        val currentConfig = configScript ?: return

        if (currentConfig.isNeedToShowADs) {
            val onb4Enabled = cfg4.isEnable
            if (onb4Enabled) {
                val handledAds = mutableSetOf<String>()
                val retriedTags = mutableSetOf<String>()
                val activeActivity = activity ?: return
                val tag = if (activeActivity.isIntroFlowDone())
                    "native_onboarding_2_4"
                else "native_onboarding_1_4"

                AdsManager.getAdLive(tag).observe(viewLifecycleOwner) { state ->

                        if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                            handledAds.add(tag)
                            Log.d("AdManager123", "[$tag] Success, 🚀 showing: [${state.adsID}]")
                            binding.adShimmer.root.gone
                            binding.frAds.visible




                            if (tag == "native_onboarding_1_4") {
                                setNextRowBottomMarginPx(
                                    resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                                )

                                binding.dotsIndicator1.layoutParams?.let { lp ->
                                    lp.height = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._35sdp)
                                    binding.dotsIndicator1.layoutParams = lp
                                }
                            }
                            ERainAd.getInstance().populateNativeAdView(
                                activeActivity,
                                state.ad,
                                binding.frAds,
                                binding.adShimmer.root

                            )

                            if (tag == "native_onboarding_1_4") {
                                binding.frAds.findViewById<View>(R.id.ad_card)?.let { card ->
                                    (card.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                                        lp.topMargin = 0
                                        card.layoutParams = lp
                                    }
                                }
                            }
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
                                // Ad-review: native_onboarding_1_4 nu CTA gradient
                                // #2663FF -> #7BD5F5. layout_native_ad_large bija unit
                                // pan vaapre chhe etle XML ma nahi, ahiya J lagavie.
                                if (tag == "native_onboarding_1_4") {
                                    adBtn.backgroundTintList = null
                                    adBtn.setBackgroundResource(R.drawable.bg_btn_native_cta_review)
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
                                        AdRemoteConfig.getInstance().native_onboarding_2_4.id else AdRemoteConfig.getInstance().native_onboarding_1_4.id
                                    AdsManager.loadAd(
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
