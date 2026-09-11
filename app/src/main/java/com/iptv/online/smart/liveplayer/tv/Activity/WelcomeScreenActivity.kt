package com.iptv.online.smart.liveplayer.tv.Activity

import android.util.Log
import androidx.activity.OnBackPressedCallback
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityWelcomeScreenBinding



class WelcomeScreenActivity : Base__Activity<ActivityWelcomeScreenBinding>() {


    private val LOG = "WelcomeFlow"
    private var configScript: RemoteConfigdata? = null
    override fun setViewBinding() = ActivityWelcomeScreenBinding.inflate(layoutInflater)

    override fun bindObjects() {
        Log.d(LOG, "WelcomeScreen -> bindObjects (screen open thai)")

        RemoteConfigdata(this@WelcomeScreenActivity).also { configScript = it }

        binding.txtWelcome.isSelected = true
//        binding.txtDesc.isSelected = true

        AdsManager.loadInterWelcomeBack(this)

    }
    override fun bindListener() {
        onBackPressedDispatcher.addCallback(owner = this, onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
            override fun handleOnBackPressed() {
                Log.d(LOG, "WelcomeScreen -> back pressed")
                binding.btnNext.performClick()
            }
        })

        binding.btnNext.setOnClickListener {
            Log.d(LOG, "WelcomeScreen -> btnNext click, show welcome-back inter")

            AdsManager.showInterWelcomeBack(activity = this) {
                Log.d(LOG, "WelcomeScreen -> inter done, finish()")

                if (!isFinishing && !isDestroyed) {
                    finish()
                }
            }
        }
    }

    override fun bindMethod() {

    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG, "WelcomeScreen -> onResume")
    }


}