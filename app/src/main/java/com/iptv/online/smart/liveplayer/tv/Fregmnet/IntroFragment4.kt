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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IntroFragment4.newInstance] factory method to
 * create an instance of this fragment.
 */
class IntroFragment4 : BaseFragment<FragmentIntro4Binding>(), LazyShowAds {


    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding() = FragmentIntro4Binding.inflate(layoutInflater)

    override fun bindObjects() {

    }

    override fun bindListener() {
        binding.btnNext.triggerClick { (activity as? IntroActivity)?.getNextFragment()}
    }

    override fun bindMethod() {
        configScript = context?.let { RemoteConfigdata(it) }
    }

    override fun showAds() {
        nativeAds()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IntroFragment4.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IntroFragment4().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun nativeAds() {
        val currentActivity = activity ?: return
        val currentConfig = configScript ?: return

        if (currentConfig.isNeedToShowADs) {
            // --- Onboarding Ad (4th Screen) ---
            if (currentConfig.nativeOnb14On) {
                val handledAds = mutableSetOf<String>()

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
                        } else if (state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            if (binding.frAds.visibility != View.VISIBLE) {
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
    }}