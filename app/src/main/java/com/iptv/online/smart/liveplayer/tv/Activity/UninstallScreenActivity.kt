package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.UninstallScreeenBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible

class UninstallScreenActivity : Base__Activity<UninstallScreeenBinding>() {


    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding() = UninstallScreeenBinding.inflate(layoutInflater)

    override fun bindObjects() {
        configScript = RemoteConfigdata(this@UninstallScreenActivity)
        AdsManager.loadAd(
            this@UninstallScreenActivity,
            AdsId.NATIVE_UNINSTALL,
            R.layout.layout_native_ad_large,
            "native_uninstall"
        )
        nativeAds()

    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativeUninstall) {

                    val handledAds = mutableSetOf<String>()
                    val tag = "native_uninstall"

                    AdsManager.getAdLive(tag).observe(this@UninstallScreenActivity) { state ->
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    this@UninstallScreenActivity,
                                    state.ad,
                                    binding?.frAds,
                                    binding?.adShimmer?.root
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
                                binding?.adShimmer?.root?.visible
                                binding?.frAds?.visible

                            } else if (state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.gone
                            }

                    }

                } else {
                    binding?.adShimmer?.root?.gone
                    binding?.frAds?.gone
                }

            } else {
                binding?.adShimmer?.root?.gone
                binding?.frAds?.gone
            }
        }
    }

    override fun bindListener() {
        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.btnTryAgain.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnUninstall.setOnClickListener {
            startActivity(Intent(this, UninstallSurveyActivity::class.java))
        }
    }

    override fun bindMethod() {
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}