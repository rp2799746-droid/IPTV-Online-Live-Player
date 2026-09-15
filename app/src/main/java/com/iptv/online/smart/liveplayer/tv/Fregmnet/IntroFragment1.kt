package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.view.View
import android.view.ViewGroup
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro1Binding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.invisible
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible

class IntroFragment1 : BaseFragment<FragmentIntro1Binding>() {

    override fun setViewBinding() = FragmentIntro1Binding.inflate(layoutInflater)

    override fun bindObjects() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }

        val isFirst = (activity as? IntroActivity)?.isFirstOnboarding ?: true
        if (isFirst) {
            binding.layoutNext.invisible
            binding.layoutNext1.visible
        } else {
            binding.layoutNext.visible
            binding.layoutNext1.invisible
        }

        AdsManager.nativeOnboarding1AdLive.observe(viewLifecycleOwner) { ad ->
            renderNative(ad, isFirst)
        }
    }

    override fun bindListener() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.btnNext1.triggerClick { (activity as? IntroActivity)?.getNextFragment() }
        binding.tvNext.isSelected = true
        binding.tvNext1.isSelected = true
    }

    override fun bindMethod() = Unit

    private fun renderNative(ad: com.ads.module.ads.wrapper.ApNativeAd?, isFirst: Boolean) {
        if (ad == null) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            return
        }

        if (isFirst) {
            (binding.layoutNext1.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.bottomMargin = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._2sdp)
                binding.layoutNext1.layoutParams = params
            }
            binding.dotsIndicator1.layoutParams?.let { params ->
                params.height = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._35sdp)
                binding.dotsIndicator1.layoutParams = params
            }
        }

        binding.frAds.removeAllViews()
        binding.frAds.visible
        ERainAd.getInstance().populateNativeAdView(
            requireActivity(),
            ad,
            binding.frAds,
            binding.adShimmer.root
        )
        binding.adShimmer.root.gone

        if (isFirst) {
            binding.frAds.findViewById<View>(R.id.ad_card)?.let { card ->
                (card.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                    params.topMargin = 0
                    card.layoutParams = params
                }
            }
        }
        binding.frAds.findViewById<View>(R.id.ad_call_to_action)?.let { button ->
            button.layoutParams?.let { params ->
                params.height = (AdsManager.getCTAButtonHeight(requireContext()) * resources.displayMetrics.density).toInt()
                button.layoutParams = params
            }
        }
    }
}
