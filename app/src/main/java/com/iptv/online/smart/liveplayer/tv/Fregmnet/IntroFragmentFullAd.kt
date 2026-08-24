package com.iptv.online.smart.liveplayer.tv.Fregmnet
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.canShowFullScreenAd
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntroFullAdBinding
import kotlinx.coroutines.delay

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
            // iptv2 jevu: fullscreen native mate JSON enable_ua_check + organic (XOR) gate.
            val fullCfg = if (currentActivity.isIntroFlowDone())
                AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2
            else AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2
            // Pure JSON: canShowFullScreenAd() pote isEnable + enable_ua_check + organic check kare chhe.
            if (fullCfg.canShowFullScreenAd()) {
                val handledAds = mutableSetOf<String>()
                val retriedTags = mutableSetOf<String>()
                val activeActivity = activity ?: return
                val tag = if (activeActivity.isIntroFlowDone())
                    "native_onboarding_full_2"
                else "native_onboarding_full_1"

                AdsManager.getAdLive(tag).observe(viewLifecycleOwner) { state ->

                        if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                            handledAds.add(tag)
                            Log.d("AdManager123", "[$tag] Success, 🚀 showing full ad: [${state.adsID}]")

                            binding.adShimmer.root.gone
                            binding.frAds.visible
                            binding.ivCloseAd.gone

                            ERainAd.getInstance().populateNativeAdView(
                                activeActivity,
                                state.ad,
                                binding.frAds,
                                binding.adShimmer.root
                            )
                            // iptv2 jevu: show_full_native_close_btn true hoy to j close (X) button
                            // 3 sec pachi batavo -> banne full_1 ane full_2 mate (pehla fakt full_2
                            // ne j batavtu, full_1 trap thato e pan fix). false hoy to close na batavo.
                            if (currentConfig.isFullNativeCloseBtn) {
                                lifecycleScope.launchWhenStarted {
                                    delay(1000)   // iptv2 jevu: 1 sec pachi close button
                                    binding.ivCloseAd.visible
                                }
                            }
                           /* lifecycleScope.launchWhenStarted {
                                delay(3000)
                                binding.ivCloseAd.visible
                            }*/
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

                                if (tag == "native_onboarding_full_1") {
                                    val bgColor = Color.parseColor("#EE6E17")
                                    val bg = adBtn.background?.mutate()
                                    if (bg is GradientDrawable) {
                                        bg.setColor(bgColor)
                                    } else if (bg != null) {
                                        bg.mutate().setColorFilter(bgColor, PorterDuff.Mode.SRC_ATOP)
                                    } else {
                                        adBtn.setBackgroundColor(bgColor)
                                    }

                                    val adAttr = binding.frAds.findViewById<android.widget.TextView>(R.id.ad_attribution)
                                    adAttr?.setTextColor(Color.parseColor("#FFFFFF"))
                                    val adRoot = binding.frAds.findViewById<View>(R.id.ad_attribution)
                                    adRoot?.setBackgroundColor(Color.parseColor("#EE6E17"))
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
                        } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            // Ad ready nathi -> 90%+ show rate mate ek j var jate FRESH load (fallback).
                            if (retriedTags.add(tag)) {
                                binding.adShimmer.root.visible
                                binding.frAds.gone
                                binding.ivCloseAd.gone
                                val adId = if (activeActivity.isIntroFlowDone())
                                    AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2.id else AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2.id
                                AdsManager.loadAd(
                                    activeActivity, adId, R.layout.layout_native_ad_full, tag
                                )
                                Log.d("AdManager123", "[$tag] fallback reload triggered")
                            } else {
                                // Fallback pachi pan na aavyu -> hide + close batavo jethi user aagal jai sake.
                                binding.adShimmer.root.gone
                                binding.frAds.gone
                                binding.ivCloseAd.visible
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
