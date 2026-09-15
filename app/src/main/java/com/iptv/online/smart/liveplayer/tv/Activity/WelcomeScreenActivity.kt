package com.iptv.online.smart.liveplayer.tv.Activity

import androidx.activity.OnBackPressedCallback
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.app.AppConstants
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityWelcomeScreenBinding

class WelcomeScreenActivity : Base__Activity<ActivityWelcomeScreenBinding>() {

    private var isStartReady = false

    override fun setViewBinding() = ActivityWelcomeScreenBinding.inflate(layoutInflater)

    override fun bindObjects() {
        binding.txtWelcome.isSelected = true
        AdsManager.loadInterWelcome(this)
        stateUi()
    }

    override fun bindObserver() {
        AdsManager.interWelcomeAdLive.observe(this) { showStart(ready = true) }
    }

    override fun bindListener() {
        onBackPressedDispatcher.addCallback(
            owner = this,
            onBackPressedCallback = object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    binding.btnNext.performClick()
                }
            },
        )

        binding.btnNext.setOnClickListener {
            if (!isStartReady) return@setOnClickListener
            AdsManager.showInterWelcome(this) {
                if (!isFinishing && !isDestroyed) finish()
            }
        }
    }

    override fun bindMethod() = Unit

    private fun stateUi() {
        showStart(ready = false)
        binding.btnNext.postDelayed(
            { showStart(ready = true) },
            AppConstants.DEFAULT_TIME_DELAY_LOAD_INTER_WELCOME,
        )
    }

    private fun showStart(ready: Boolean) {
        if (isFinishing || isDestroyed || (isStartReady && !ready)) return
        isStartReady = ready
        binding.btnNext.isEnabled = ready
    }

    override fun onDestroy() {
        binding.btnNext.handler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}