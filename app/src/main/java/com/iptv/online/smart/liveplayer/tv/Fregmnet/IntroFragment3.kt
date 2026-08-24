package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro3Binding
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone



class IntroFragment3 : BaseFragment<FragmentIntro3Binding>() {


    override fun setViewBinding() = FragmentIntro3Binding.inflate(layoutInflater)

    private val configScript: RemoteConfigdata by lazy {
        RemoteConfigdata(requireActivity())
    }

    override fun bindObjects() {
        val isDone = requireActivity().isIntroFlowDone()

        if (configScript.isNeedToShowADs) {
          val adId4 = if (isDone) AdRemoteConfig.getInstance().native_onboarding_2_4.id else AdRemoteConfig.getInstance().native_onboarding_1_4.id
          val tag4 = if (isDone) "native_onboarding_2_4" else "native_onboarding_1_4"
          AdsManager.loadAd(requireActivity(), adId4, R.layout.layout_native_ad_large, tag4)
      }

    }

    override fun bindListener() {
        binding.btnNext.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
        // Lambo translated text single line ma marquee (scroll) thava mate.
        binding.tvNext.isSelected = true
    }

    override fun bindMethod() {
    }

    }