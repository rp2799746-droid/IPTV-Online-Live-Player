package com.iptv.online.smart.liveplayer.tv.Activity

import android.util.Log
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Adapter.ChannelAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityChannelListBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible

class ChannelListActivity : Base__Activity<ActivityChannelListBinding>() {

    private var adapter: ChannelAdapter? = null
    private var pName: String? = null
    private var gName: String? = null
    private var configScript: RemoteConfigdata? = null
    override fun setViewBinding(): ActivityChannelListBinding {
        return ActivityChannelListBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun bindObjects() {
        pName = intent.getStringExtra("PLAYLIST_NAME")
        gName = intent.getStringExtra("GROUP_NAME")

        binding.rvChannels.layoutManager = LinearLayoutManager(this)

        configScript = RemoteConfigdata(this@ChannelListActivity)

        nativeAds()
    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativeChannelList) {

                    val handledAds = mutableSetOf<String>()
                    val tag = "native_channel_list"

                    AdsManager.getAdLive(tag).observe(this@ChannelListActivity) { state ->
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    this@ChannelListActivity,
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
        binding.back.setOnClickListener { onBackPressed() }

        binding.etSearchChannel.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            binding.ivClearSearchChannel.visibility =
                if (query.isEmpty()) View.GONE else View.VISIBLE
            adapter?.filter(query)
        }

        binding.ivClearSearchChannel.setOnClickListener {
            binding.etSearchChannel.setText("")
        }
    }

    override fun bindMethod() {
        loadChannels()
    }

    private fun loadChannels() {

        val channelData = AppDatabase.getInstance(this).historyDao()
            .getChannelsByGroup(pName ?: "", gName ?: "")

        if (channelData != null) {
            if (adapter == null) {
                adapter = ChannelAdapter(this, ArrayList(channelData))
                binding.rvChannels.adapter = adapter
            } else {
                adapter?.updateList(ArrayList(channelData))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadChannels()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}