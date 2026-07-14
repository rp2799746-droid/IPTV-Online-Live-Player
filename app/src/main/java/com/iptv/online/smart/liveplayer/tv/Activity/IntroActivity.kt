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
//import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeOnboardingFull2
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
    // Show-rate: inter_onboarding ne intro START ma load na karo. User onboarding ma agal
    // vadhe (2ja page e) tyare load karo -> show point (onboarding end) ni najik -> ochi
    // drop-off -> uncho show rate. Ek j var trigger thay eno guard.
    private var interOnbLoadTriggered = false
    var fragments = emptyList<Fragment>()

    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }


    override fun bindObjects() {
        // inter_onboarding ne have ahiya (intro start) load nathi karta -> te bahu vehlu hatu
        // (show onboarding-end e thay -> vachche ghana exit -> show rate 19%). Have onPageSelected
        // ma user later page e pohonche tyare load thay che (show-rate optimization).
        // loadInterAds()
        val isDone = isIntroFlowDone()

        val isFullAdEnabled =
            if (isDone) configScript.nativeOnbFull2On else configScript.nativeOnbFull1On



        fragments = arrayListOf<Fragment>().apply {
            add(IntroFragment1()) // Index 0
            add(IntroFragment2())
            Log.d("aaaa", "ddd: +" + getShouldDisplayNativeOnboardingFull1())
            Log.d("aaaa", "ddd: +" + getShouldDisplayInterOnboarding())
            Log.d("aaaa", "ddd: +" + getShouldDisplayWidgetUninstall())
            Log.d("aaaa", "ddd: +" + getSHouldDisplayHighCTA())
            if (getShouldDisplayNativeOnboardingFull1() && isInternetAvailable() && configScript.isNeedToShowADs && isFullAdEnabled) {

                add(IntroFragmentFullAd())
            }

            add(IntroFragment3())
            add(IntroFragment4())
        }

        binding.pagerIntro.offscreenPageLimit = 5
    }

    /*
      override fun bindObjects() {
          loadInterAds()
          val isDone = isIntroFlowDone()

          fragments = arrayListOf<Fragment>().apply {
              add(IntroFragment1())               // Index 0 - Page 1
              add(IntroFragment2())               // Index 1 - Page 2

              // ---- FULL AD 1 : should FALSE hoy tyare ad ----
              val full1Should = getShouldDisplayNativeOnboardingFull1()
              val full1Net    = isInternetAvailable()
              val full1NeedAd = configScript.isNeedToShowADs
              val full1On     = configScript.nativeOnbFull1On

              Log.d("INTRO_ADS", "FULL_1 -> should=$full1Should net=$full1Net needAds=$full1NeedAd full1On=$full1On")

              if (!full1Should && full1Net && full1NeedAd && full1On) {   // <-- !full1Should
                  Log.d("INTRO_ADS", "FULL_1 -> ADDED ✅")
                  add(IntroFragmentFullAd())      // full ad 1
              } else {
                  Log.d("INTRO_ADS", "FULL_1 -> NOT added ❌")
                  add(IntroFragmentFullAd())      // full ad 1

              }

              add(IntroFragment3())               // Page 3

              // ---- FULL AD 2 : should FALSE hoy tyare ad ----
              val full2Should = getShouldDisplayNativeOnboardingFull2()
              val full2Net    = isInternetAvailable()
              val full2NeedAd = configScript.isNeedToShowADs
              val full2On     = configScript.nativeOnbFull2On

              Log.d("INTRO_ADS", "FULL_2 -> should=$full2Should net=$full2Net needAds=$full2NeedAd full2On=$full2On")

              if (!full2Should && full2Net && full2NeedAd && full2On) {   // <-- !full2Should
                  Log.d("INTRO_ADS", "FULL_2 -> ADDED ✅")
                  add(IntroFragmentFullAd())      // full ad 2
              } else {
                  Log.d("INTRO_ADS", "FULL_2 -> NOT added ❌");
                          add(IntroFragmentFullAd())      // full ad 1

              }

              add(IntroFragment4())               // Page 4
          }

          Log.d("INTRO_ADS", "Total fragments = ${fragments.size}")
          binding.pagerIntro.offscreenPageLimit = 5
      }
    */
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
                // Show-rate: user onboarding ma agal vadhe (page >= 1) tyare J inter_onboarding
                // load karo. Je users 1st page e j chhodi de emna mate request nathi thato ->
                // ane load show point (end) ni najik thay -> show rate uncho (target 19% -> 30%+).
                if (position >= 1 && !interOnbLoadTriggered) {
                    interOnbLoadTriggered = true
                    loadInterAds()
                }
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
//                Intent(this, MainActivity::class.java).apply {
//                    putExtra("isFromIntro", true)
//                    flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                }
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
            val adapter = binding.pagerIntro.adapter as FragmentStateAdapter
            fragments.getOrNull(index)?.let { fragment ->
                if (fragment is LazyShowAds  && fragment.isAdded) {
                    Log.d("AdManager123", "Calling showAds for fragment at index: $index")
                    fragment.showAds()
                }
            }
        }
    }
}