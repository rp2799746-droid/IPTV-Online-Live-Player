package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdmobHelper
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment1
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment2
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment3
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment4
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager.showInterAds
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayInterOnboarding
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeOnboardingFull1
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityIntroBinding
import com.iptv.online.smart.liveplayer.tv.Fregmnet.IntroFragmentFullAd
import com.iptv.online.smart.liveplayer.tv.adsutils.getSHouldDisplayHighCTA
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayWidgetUninstall
import com.iptv.online.smart.liveplayer.tv.utils.Preference

import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.setIntroFlowDone
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class IntroActivity : Base__Activity<ActivityIntroBinding>() {

    override fun setViewBinding() = ActivityIntroBinding.inflate(layoutInflater)

    private var mInterstitialAd: ApInterstitialAd? = null
    var fragments = emptyList<Fragment>()

    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }


    override fun bindObjects() {
        loadInterAds()
        val isDone = isIntroFlowDone()

        val isFullAdEnabled =
            if (isDone) configScript.nativeOnbFull2On else configScript.nativeOnbFull1On

        if (configScript.isNeedToShowADs) {
            if (isFullAdEnabled) {
                val adIdFull =
                    if (isDone) AdsId.nativeOnboardingFull2 else AdsId.nativeOnboardingFull1
                val tagFull = if (isDone) "native_onboarding_full_2" else "native_onboarding_full_1"
                InfinityAdsManager.loadAd(this, adIdFull, R.layout.layout_native_ad_full, tagFull)
            }

            val adId1 = if (isDone) AdsId.nativeOnboarding2_1 else AdsId.nativeOnboarding1_1
            val tag1 = if (isDone) "native_onboarding_2_1" else "native_onboarding_1_1"
            InfinityAdsManager.loadAd(this, adId1, R.layout.layout_native_ad_large, tag1)

            val adId4 = if (isDone) AdsId.nativeOnboarding2_4 else AdsId.nativeOnboarding1_4
            val tag4 = if (isDone) "native_onboarding_2_4" else "native_onboarding_1_4"
            InfinityAdsManager.loadAd(this, adId4, R.layout.layout_native_ad_large, tag4)
        }

        fragments = arrayListOf<Fragment>().apply {
            add(IntroFragment1()) // Index 0
            add(IntroFragment2())
            Log.d("aaaa", "ddd: +"+getShouldDisplayNativeOnboardingFull1())
            Log.d("aaaa", "ddd: +"+getShouldDisplayInterOnboarding())
            Log.d("aaaa", "ddd: +"+getShouldDisplayWidgetUninstall())
            Log.d("aaaa", "ddd: +"+getSHouldDisplayHighCTA())
            if (getShouldDisplayNativeOnboardingFull1() && isInternetAvailable() &&
                configScript.isNeedToShowADs && isFullAdEnabled
            ) {

                Log.d("AdManager123", "Full Ad 2 Added")
                add(IntroFragmentFullAd())
            }

            add(IntroFragment3())
            add(IntroFragment4())
        }

        binding.pagerIntro.offscreenPageLimit = 5
    }

    private fun loadInterAds() {
        Log.i(
            "AdManager123",
            "shouldDisplayInterOnboarding  Load: ${getShouldDisplayInterOnboarding()}"
        )
        if (getShouldDisplayInterOnboarding() && isInternetAvailable() && RemoteConfigdata(this@IntroActivity).isNeedToShowADs && RemoteConfigdata(
                this@IntroActivity
            ).interOnboardingOn
        ) {
            ERainAd.getInstance()
                .getInterstitialAds(this, AdsId.interOnboarding, object : AdCallback() {
                    override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                        super.onApInterstitialLoad(apInterstitialAd)
                        mInterstitialAd = apInterstitialAd
                        Log.i("AdManager123", "Inter Load : $localClassName")
                    }
                })
        }
    }

    fun getNextFragment() {
        if (binding.pagerIntro.currentItem == fragments.size - 1) {
            callNext()
        } else {
            binding.pagerIntro.currentItem += 1
        }
    }

    private fun callNext() {

        setIntroFlowDone()
        if (getShouldDisplayInterOnboarding()) {
            showInterAds(mInterstitialAd) {
                redirectToNextActivity()
            }
        } else {
            redirectToNextActivity()
        }
    }

    override fun bindListener() {

    }

    override fun bindMethod() {

        binding.pagerIntro.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }
        binding.pagerIntro.offscreenPageLimit = 1
        binding.pagerIntro.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonStyles(position)
            }
        })

        updateButtonStyles(0)
    }

    private fun redirectToNextActivity() {
        Preference.setBoolean(this@IntroActivity, "isIntro", true)
        if (!isFinishing) {
            val intent = if (AdmobHelper.isPurchased(this))
                Intent(this, MainActivity::class.java).apply {
                    flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            else
                Intent(this, MainActivity::class.java).apply {
                    putExtra("isFromIntro", true)
                    flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        finishAffinity()
        exitProcess(0)
    }

    private fun updateButtonStyles(index: Int) {
        val isDone = isIntroFlowDone()

        if (index == 1 && getShouldDisplayNativeOnboardingFull1() && configScript.isNeedToShowADs) {
            val adIdFull = if (isDone) AdsId.nativeOnboardingFull2 else AdsId.nativeOnboardingFull1
            val tagFull = if (isDone) "native_onboarding_full_2" else "native_onboarding_full_1"

            InfinityAdsManager.loadAd(this, adIdFull, R.layout.layout_native_ad_full, tagFull)
        }

        if (index > 0) {
            val adapter = binding.pagerIntro.adapter as FragmentStateAdapter
            fragments.getOrNull(index)?.let { fragment ->
                if (fragment is LazyShowAds) {
                    Log.d("AdManager123", "Calling showAds for fragment at index: $index")
                    fragment.showAds()
                }
            }
        }
    }
}