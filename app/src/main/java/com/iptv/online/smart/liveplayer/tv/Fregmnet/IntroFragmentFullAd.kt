package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntroFullAdBinding

class IntroFragmentFullAd : BaseFragment<FragmentIntroFullAdBinding>(), LazyShowAds {



    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding() = FragmentIntroFullAdBinding.inflate(layoutInflater)

    override fun bindObjects() {
    }

    override fun bindListener() {
        binding.ivCloseAd.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
    }

    override fun bindMethod() {
        configScript = context?.let { RemoteConfigdata(it) }
    }

    override fun bindObserver() {
        super.bindObserver()

    }

    override fun showAds() {
        nativeAds()
    }

    private fun nativeAds() {
        val currentActivity = activity ?: return
        val currentConfig = configScript ?: return

        if (currentConfig.isNeedToShowADs) {
            if (currentConfig.nativeOnbFull1On) {
                val handledAds = mutableSetOf<String>()

                lifecycleScope.launchWhenStarted {
                    InfinityAdsManager.adStateFlow.collect { states ->
                        val activeActivity = activity ?: return@collect

                        val tag = if (activeActivity.isIntroFlowDone())
                            "native_onboarding_full_2"
                        else "native_onboarding_full_1"

                        val state = states[tag]

                        if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                            handledAds.add(tag)
                            Log.d("AdManager123", "[$tag] Success, 🚀 showing full ad: [${state.adsID}]")

                            binding.adShimmer.root.gone
                            binding.frAds.visible
                            binding.ivCloseAd.visible

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
                                binding.ivCloseAd.gone
                            }
                        } else if (state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            binding.adShimmer.root.gone
                            binding.frAds.gone
                            binding.ivCloseAd.visible

                            // ઓટોમેટિક નેક્સ્ટ કરવું હોય તો:
                            // (activity as? IntroActivity)?.getNextFragment()
                        }
                    }
                }
            } else {
                binding.adShimmer.root.gone
                binding.frAds.gone
                binding.ivCloseAd.visible
            }
        } else {
            binding.adShimmer.root.gone
            binding.frAds.gone
            binding.ivCloseAd.visible
        }
    }}