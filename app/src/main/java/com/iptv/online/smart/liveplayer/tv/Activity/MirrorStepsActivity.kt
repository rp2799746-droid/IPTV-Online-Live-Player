package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityMirrorStepsBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible

class MirrorStepsActivity : Base__Activity<ActivityMirrorStepsBinding>() {

    private var isSmartTV = true
    private var audioManager: AudioManager? = null
    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding(): ActivityMirrorStepsBinding {
        return ActivityMirrorStepsBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        if (isTaskRoot) {
            MainActivity.triggerFromMirroring = true
            // Demo jevu: centralized showInterBack -> ad ready hoy to show, nahi to sidhu navigate.
            InfinityAdsManager.showInterBack(this) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
            return
        }
        MainActivity.triggerFromMirroring = true

        super.onBackPressed()
    }

    override fun bindObjects() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?

        binding.switchSound.isChecked = true
        configScript = RemoteConfigdata(this@MirrorStepsActivity)

        nativeAds()
        // Demo jevu CENTRALIZED: interback ne InfinityAdsManager thi load karo.
        InfinityAdsManager.loadInterBack(this)
    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativeMirroring) {

                    val handledAds = mutableSetOf<String>()

                    lifecycleScope.launchWhenStarted {
                        InfinityAdsManager.adStateFlow.collect { states ->
                            val tag = "native_mirroring"
                            val state = states[tag]
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    this@MirrorStepsActivity,
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
        // બેક બટન
        binding.toolbar.back.setOnClickListener { onBackPressed() }

        // સ્માર્ટ ટીવી ટેબ સિલેક્શન
        binding.tvSmartTVTab.setOnClickListener {
            isSmartTV = true
            updateTabUI(true)
        }

        // ડાયરેક્ટ ટેબ સિલેક્શન
        binding.tvDirectTab.setOnClickListener {
            isSmartTV = false
            updateTabUI(false)
        }

        // મિરરિંગ સ્ટાર્ટ બટન
        binding.btnStartMirroring.setOnClickListener {
            if (isSmartTV) {
                Toast.makeText(this, getString(R.string.connecting_to_smart_tv), Toast.LENGTH_SHORT)
                    .show()
                openSmartTVMirroring()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.searching_for_direct_miracast),
                    Toast.LENGTH_SHORT
                ).show()
                openDirectMirroring()
            }
        }

        // સાઉન્ડ સ્વીચ લોજિક
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                unmuteSound()
                Toast.makeText(this, getString(R.string.sound_enabled), Toast.LENGTH_SHORT).show()
            } else {
                muteSound()
                Toast.makeText(this, getString(R.string.sound_disabled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun bindMethod() {
        // શરૂઆતમાં સ્માર્ટ ટીવી ટેબ એક્ટિવ રાખો
        updateTabUI(true)
    }

    private fun muteSound() {
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
    }

    private fun unmuteSound() {
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
    }

    private fun openSmartTVMirroring() {
        try {
            startActivity(Intent("android.settings.CAST_SETTINGS"))
        } catch (e: Exception) {
            openDirectMirroring()
        }
    }

    private fun openDirectMirroring() {
        try {
            startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
            } catch (e2: Exception) {
                Toast.makeText(
                    this,
                    getString(R.string.mirroring_not_supported_on_this_device),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateTabUI(isSmartTVSelected: Boolean) {
        if (isSmartTVSelected) {
            binding.apply {
                tvSmartTVTab.setBackgroundResource(R.drawable.round_borderfill_box_hover)
                tvSmartTVTab.setTextColor(
                    ContextCompat.getColor(
                        this@MirrorStepsActivity,
                        R.color.white
                    )
                )
                smartLn.visibility = View.VISIBLE
                directLn.visibility = View.GONE
                tvDirectTab.background = null
                tvDirectTab.setTextColor(
                    ContextCompat.getColor(
                        this@MirrorStepsActivity,
                        R.color.darker_gray
                    )
                )
            }
        } else {
            binding.apply {
                tvDirectTab.setBackgroundResource(R.drawable.round_borderfill_box_hover)
                tvDirectTab.setTextColor(
                    ContextCompat.getColor(
                        this@MirrorStepsActivity,
                        R.color.white
                    )
                )
                smartLn.visibility = View.GONE
                directLn.visibility = View.VISIBLE
                tvSmartTVTab.background = null
                tvSmartTVTab.setTextColor(
                    ContextCompat.getColor(
                        this@MirrorStepsActivity,
                        R.color.darker_gray
                    )
                )
            }
        }
    }
}