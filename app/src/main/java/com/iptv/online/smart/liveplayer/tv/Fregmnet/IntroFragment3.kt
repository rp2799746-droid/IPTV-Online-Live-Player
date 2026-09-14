package com.iptv.online.smart.liveplayer.tv.activities.forIntro


import com.iptv.online.smart.liveplayer.tv.Fregmnet.BaseFragment
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentIntro3Binding
import com.iptv.online.smart.liveplayer.tv.utils.triggerClick
import com.iptv.online.smart.liveplayer.tv.Activity.IntroActivity



class IntroFragment3 : BaseFragment<FragmentIntro3Binding>() {


    override fun setViewBinding() = FragmentIntro3Binding.inflate(layoutInflater)
    override fun bindObjects() {
    }

    override fun bindListener() {
        binding.btnNext.triggerClick {
            (activity as? IntroActivity)?.getNextFragment()
        }
        binding.tvNext.isSelected = true
    }

    override fun bindMethod() {
    }

    }
