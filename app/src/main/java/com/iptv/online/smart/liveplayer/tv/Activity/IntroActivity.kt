package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdmobHelper
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment1
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment2
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment3
import com.iptv.online.smart.liveplayer.tv.activities.forIntro.IntroFragment4
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.LazyShowAds
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityIntroBinding
import com.iptv.online.smart.liveplayer.tv.Fregmnet.IntroFragmentFullAd
//import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeOnboardingFull2
import com.iptv.online.smart.liveplayer.tv.utils.Preference

import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.setIntroFlowDone
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class IntroActivity : Base__Activity<ActivityIntroBinding>() {

    override fun setViewBinding() = ActivityIntroBinding.inflate(layoutInflater)

    var fragments = emptyList<Fragment>()

    var isFirstOnboarding = true
        private set


    override fun bindObjects() {
        isFirstOnboarding = !isIntroFlowDone()
        val fullConfig = if (isFirstOnboarding) {
            AdRemoteConfig.getInstance().native_onboarding_fullscreen_1_2
        } else {
            AdRemoteConfig.getInstance().native_onboarding_fullscreen_2_2
        }
        fragments = arrayListOf<Fragment>().apply {
            add(IntroFragment1()) // Index 0
            add(IntroFragment2())
            add(IntroFragment3())
            if (isInternetAvailable()
                && ERainAd.getInstance()
                    .getShouldDisplayNativeOnboardingFull1(fullConfig.enableUaCheck) == true
            ) {
                add(IntroFragmentFullAd())
            }
            add(IntroFragment4())
        }

        binding.pagerIntro.offscreenPageLimit = 5

        // Match the base preloading point.  The IPTV-only native_home preload
        // stays here so fragments remain render-only.
        binding.root.postDelayed({
            if (!isFinishing && !isDestroyed) {
                AdsManager.loadNativeOnboarding4(
                    this,
                    isFirstOnboarding,
                    R.layout.layout_native_ad_large
                )
                AdsManager.loadNativeOnboardingFull(
                    this,
                    isFirstOnboarding,
                    R.layout.layout_native_ad_full
                )
                AdsManager.loadInterOnboarding(this)
                preloadAppSpecificAds()
            }
        }, 100L)
    }

    private fun preloadAppSpecificAds() {
        val nativeHome = AdRemoteConfig.getInstance().native_home
        if (nativeHome.isEnable) {
            AdsManager.loadAd(
                this,
                nativeHome.id,
                R.layout.layout_native_ad_medium,
                "native_home_2005"
            )
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
        AdsManager.showInterOnboarding(this) {
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

                Intent(this, ActivityNotice::class.java).apply {
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


        if (index > 0) {
            fragments.getOrNull(index)?.let { fragment ->
                if (fragment is LazyShowAds  && fragment.isAdded) {
                    Log.d("AdManager123", "Calling showAds for fragment at index: $index")
                    fragment.showAds()
                }
            }
        }
    }
}