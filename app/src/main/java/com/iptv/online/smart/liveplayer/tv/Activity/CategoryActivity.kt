package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Adapter.GroupAdapter
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.ChannelGroup
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityCategoryBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import java.util.Collections

class CategoryActivity : Base__Activity<ActivityCategoryBinding>() {
    private var playlistName: String? = null
    private var adapter: GroupAdapter? = null
    private val groupList: MutableList<ChannelGroup?> = ArrayList<ChannelGroup?>()
    private var configScript: RemoteConfigdata? = null

    public override fun setViewBinding(): ActivityCategoryBinding {
        return ActivityCategoryBinding.inflate(getLayoutInflater())
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    public override fun bindObjects() {
        // Intent માંથી ડેટા મેળવો

        configScript = RemoteConfigdata(this@CategoryActivity)



        nativeAds()

        playlistName = getIntent().getStringExtra("PLAYLIST_NAME")

        binding.rvCategories.setLayoutManager(GridLayoutManager(this, 2))

        adapter =
            GroupAdapter(this, groupList, GroupAdapter.OnGroupClickListener { groupName: String? ->
                val intent = Intent(this, ChannelListActivity::class.java)
                intent.putExtra("PLAYLIST_NAME", playlistName)
                intent.putExtra("GROUP_NAME", groupName)
                startActivity(intent)
            })

        binding.rvCategories.setAdapter(adapter)
    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativechannel2005) {

                    val handledAds = mutableSetOf<String>()

                    lifecycleScope.launchWhenStarted {
                        InfinityAdsManager.adStateFlow.collect { states ->
                            val tag = "native_channel_2005"
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
                                    this@CategoryActivity,
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

    public override fun bindListener() {
        // Back બટન ક્લિક
        binding.back.setOnClickListener({ v -> onBackPressed() })
        binding.etSearchChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val query = s.toString().trim { it <= ' ' }
                binding.ivClearSearchChannel.setVisibility(if (query.isEmpty()) View.GONE else View.VISIBLE)

                if (adapter != null) {
                    adapter!!.filter(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearchChannel.setOnClickListener({ v -> binding.etSearchChannel.setText("") })
    }

    public override fun bindMethod() {
        loadGroups()
    }

    private fun loadGroups() {
        val channels =
            AppDatabase.getInstance(this).historyDao().getChannelsByPlaylist(playlistName)

        if (channels == null || channels.isEmpty()) {
            binding.rvCategories.setVisibility(View.GONE)
            binding.layoutNoData.setVisibility(View.VISIBLE)
            return
        }

        // જો ડેટા મળે તો જૂનું લોજિક
        binding.rvCategories.setVisibility(View.VISIBLE)
        binding.layoutNoData.setVisibility(View.GONE)

        val map = HashMap<String?, Int?>()
        for (c in channels) {
            val gName = if (c.getChannelGroup() == null || c.getChannelGroup()
                    .isEmpty()
            ) "Other" else c.getChannelGroup()
            map.put(gName, map.getOrDefault(gName, 0)!! + 1)
        }

        groupList.clear()
        for (entry in map.entries) {
            groupList.add(ChannelGroup(entry.key, entry.value!!))
        }

        Collections.sort<ChannelGroup?>(
            groupList,
            Comparator { g1: ChannelGroup?, g2: ChannelGroup? ->
                g1!!.getName().compareTo(
                    g2!!.getName(), ignoreCase = true
                )
            })

        adapter!!.updateList(groupList)
    }
}