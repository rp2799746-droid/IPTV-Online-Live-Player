package com.iptv.online.smart.liveplayer.tv.activities.forIntro
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.utils.invisible


class IntroFragment1 : BaseFragment<FragmentIntro1Binding>() {


    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(requireActivity())
    }

    override fun setViewBinding() = FragmentIntro1Binding.inflate(layoutInflater)

    @SuppressLint("SuspiciousIndentation")
    override fun bindObjects() {
        // Next button SAUTHI PEHLA wire karo -> niche ad code (nativeAds / full-ad) exception
        // aape to pan Next button hammesha kaam kare (bindListener skip thay to pan chale).
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }

        nativeAds()
        val isDone = requireActivity().isIntroFlowDone()


        if (isDone) {
            binding.layoutNext.visible
            binding.layoutNext1.invisible
        } else {
            binding.layoutNext.invisible
            binding.layoutNext1.visible
        }


        val isFullAdEnabled =
            if (isDone) AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2.isEnable
            else AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2.isEnable

          if (configScript.isNeedToShowADs) {
           if (isFullAdEnabled) {
               val adIdFull =
                   if (isDone) AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2.id else AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2.id
               val tagFull = if (isDone) "native_onboarding_full_2" else "native_onboarding_full_1"
               AdsManager.loadAd(requireActivity(), adIdFull, R.layout.layout_native_ad_full, tagFull)
           }


       }
    }

    override fun bindListener() {
        // Banne rows na button wire karo - kyo row dekhay chhe e isDone par aadharit chhe.
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.tvNext.isSelected = true
        binding.tvNext1.isSelected = true
    }


    private fun setNextRowBottomMarginPx(px: Int) {
        val lp = binding.layoutNext1.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        lp.bottomMargin = px
        binding.layoutNext1.layoutParams = lp
    }

    private fun nativeAds() {
        val cfg = if (requireActivity().isIntroFlowDone())
            AdRemoteConfig.getInstance().native_onboarding_2_1
        else AdRemoteConfig.getInstance().native_onboarding_1_1
        // enableUaCheck config (JSON) mathi -> JSON pramane UA gate. false hoy to disable ma pan dekhay.
        if (!(ERainAd.getInstance().getShouldDisplayNativeOnboardingNormal1(cfg.enableUaCheck) == true)) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            return
        }
        val onbEnabled = cfg.isEnable
        if (configScript.isNeedToShowADs && onbEnabled) {
            val handledAds = mutableSetOf<String>()
            val retriedTags = mutableSetOf<String>()
            val activeActivity = activity ?: return
            val tag = if (activeActivity.isIntroFlowDone())
                "native_onboarding_2_1"
            else "native_onboarding_1_1"

            AdsManager.getAdLive(tag).observe(viewLifecycleOwner) { state ->

                    if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                        handledAds.add(tag)
                        Log.d("AdManager123", "[$tag] Success, 🚀 showing ID: [${state.adsID}]")

                        binding.adShimmer.root.gone
                        binding.frAds.visible


                        if (tag == "native_onboarding_1_1") {
                            // Ad thi thodu j uper - sdp vapriye chhe etle badha
                            // device par sarkhu rahe ane ad ne touch na thay.
                            setNextRowBottomMarginPx(
                                resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                            )
                            // Dots image 60sdp unchi chhe etle row unchi thati hati ane
                            // button ni niche khali jagya dekhati hati. Dots ni height
                            // button jetli (35sdp) kari do - banne center ma ek line ma
                            // rahe ane aakhi row ad ni adine aavi jay.
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

                        // Intro screen 1 ma Next button ad ni ekdum close aave e mate
                        // card no TOP margin 0 karo - ad potey jya chhe tya j rahe chhe.
                        // Layout share thay chhe etle ahiya code ma j.
                        if (tag == "native_onboarding_1_1") {
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

                        if (binding.frAds.visibility != View.VISIBLE) {
                            if (retriedTags.add(tag)) {
                                binding.adShimmer.root.visible
                                val adId = if (activeActivity.isIntroFlowDone())
                                    AdRemoteConfig.getInstance().native_onboarding_2_1.id else AdRemoteConfig.getInstance().native_onboarding_1_1.id
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
        } else {
            binding.adShimmer.root.gone
            binding.frAds.gone
        }
    }

    override fun bindMethod() {

    }


}
