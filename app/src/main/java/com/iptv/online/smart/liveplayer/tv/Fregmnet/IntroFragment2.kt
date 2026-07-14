package com.iptv.online.smart.liveplayer.tv.activities.forIntro

import android.os.Bundle
import androidx.fragment.app.Fragment

import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro2Binding
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity


class IntroFragment2 : BaseFragment<FragmentIntro2Binding>() {


    override fun setViewBinding() = FragmentIntro2Binding.inflate(layoutInflater)

    override fun bindObjects() {
    }

    override fun bindListener() {
        binding.btnNext.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
    }

    override fun bindMethod() {
    }


}