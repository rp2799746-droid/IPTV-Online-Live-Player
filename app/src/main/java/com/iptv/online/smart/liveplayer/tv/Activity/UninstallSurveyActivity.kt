package com.iptv.online.smart.liveplayer.tv.Activity
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.UninstallSurveyBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible

class UninstallSurveyActivity : Base__Activity<UninstallSurveyBinding>() {

    private val remoteData: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }
    private var configScript: RemoteConfigdata? = null


    override fun setViewBinding() = UninstallSurveyBinding.inflate(layoutInflater)

    override fun bindObjects() {

        configScript = RemoteConfigdata(this@UninstallSurveyActivity)
        AdsManager.loadAd(
            this@UninstallSurveyActivity,
            AdRemoteConfig.getInstance().native_survey_uninstall.id,
            R.layout.layout_native_ad_large,
            "native_survey_uninstall"
        )
        nativeAds()

    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (AdRemoteConfig.getInstance().native_survey_uninstall.isEnable) {

                    val handledAds = mutableSetOf<String>()
                    val tag = "native_survey_uninstall"

                    AdsManager.getAdLive(tag).observe(this@UninstallSurveyActivity) { state ->
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    this@UninstallSurveyActivity,
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

        binding.btnCancel.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnUninstall.setOnClickListener {

            openSystemUninstallDialog()
        }
    }

    override fun bindMethod() {
    }

    private fun openSystemUninstallDialog() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        startActivity(Intent(this, UninstallScreenActivity::class.java))
        finish()
    }
}
