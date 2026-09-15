package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntroFullAdBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible

class IntroFragmentFullAd : BaseFragment<FragmentIntroFullAdBinding>(), LazyShowAds {

    private var isObserving = false

    override fun setViewBinding() = FragmentIntroFullAdBinding.inflate(layoutInflater)

    override fun bindObjects() = Unit

    override fun bindListener() {
        binding.ivCloseAd.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
    }

    override fun bindMethod() = Unit

    override fun showAds() {
        if (isObserving) return
        isObserving = true
        val isFirst = (activity as? IntroActivity)?.isFirstOnboarding ?: true
        AdsManager.nativeAdOnBoardingFullLive.observe(viewLifecycleOwner) { ad ->
            renderNative(ad, isFirst)
        }
    }

    private fun renderNative(ad: com.ads.module.ads.wrapper.ApNativeAd?, isFirst: Boolean) {
        if (ad == null) {
            binding.adShimmer.root.gone
            binding.frAds.gone
            binding.ivCloseAd.visible
            return
        }

        binding.frAds.removeAllViews()
        binding.frAds.visible
        binding.ivCloseAd.gone
        ERainAd.getInstance().populateNativeAdView(
            requireActivity(),
            ad,
            binding.frAds,
            binding.adShimmer.root
        )
        binding.adShimmer.root.gone

        if (RemoteConfigdata(requireContext()).isFullNativeCloseBtn) {
            binding.root.postDelayed({
                if (isAdded) binding.ivCloseAd.visible
            }, 1_000L)
        }

        binding.frAds.findViewById<View>(R.id.ad_call_to_action)?.let { button ->
            button.layoutParams?.let { params ->
                params.height = (AdsManager.getCTAButtonHeight(requireContext()) * resources.displayMetrics.density).toInt()
                button.layoutParams = params
            }
            if (isFirst) {
                val background = button.background?.mutate()
                if (background is GradientDrawable) {
                    background.setColor(Color.parseColor("#EE6E17"))
                } else if (background != null) {
                    background.setColorFilter(Color.parseColor("#EE6E17"), PorterDuff.Mode.SRC_ATOP)
                } else {
                    button.setBackgroundColor(Color.parseColor("#EE6E17"))
                }
                binding.frAds.findViewById<android.widget.TextView>(R.id.ad_attribution)
                    ?.setTextColor(Color.WHITE)
                binding.frAds.findViewById<View>(R.id.ad_attribution)
                    ?.setBackgroundColor(Color.parseColor("#EE6E17"))
            }
        }
    }
}
