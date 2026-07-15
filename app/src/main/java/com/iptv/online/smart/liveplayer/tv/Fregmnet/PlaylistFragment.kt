package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.iptv.online.smart.liveplayer.tv.Activity.CategoryActivity
import com.iptv.online.smart.liveplayer.tv.Activity.MainActivity
import com.iptv.online.smart.liveplayer.tv.Activity.MirrorStepsActivity
import com.iptv.online.smart.liveplayer.tv.Activity.SettingsActivity
import com.iptv.online.smart.liveplayer.tv.Adapter.BannerAdapter
import com.iptv.online.smart.liveplayer.tv.Adapter.PlaylistAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.BannerItem
import com.iptv.online.smart.liveplayer.tv.Model.PlaylistGroup
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayNativeHome
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentPlaylistBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import java.util.Collections
import kotlin.text.trim

class PlaylistFragment : Fragment() {
    private var binding: FragmentPlaylistBinding? = null
    private var adapter: PlaylistAdapter? = null
    private val playlistList: MutableList<PlaylistGroup?> = ArrayList<PlaylistGroup?>()
    private var configScript: RemoteConfigdata? = null

    private val sliderHandler = Handler()
    private var bannerList: ArrayList<BannerItem?>? = null
    private val sliderRunnable: Runnable = object : Runnable {
        override fun run() {
            val currentItem = binding!!.bannerViewPager.getCurrentItem()
            val nextItem = (currentItem + 1) % bannerList!!.size
            binding!!.bannerViewPager.setCurrentItem(nextItem, true)
        }
    }

    private fun setupBanner() {
        bannerList = ArrayList<BannerItem?>()
        bannerList!!.add(BannerItem(R.drawable.imag))
        bannerList!!.add(BannerItem(R.drawable.imag2))
        bannerList!!.add(BannerItem(R.drawable.imag3))


        val adapter = BannerAdapter(bannerList) { position ->
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.viewPager.currentItem = 1
                mainActivity.updateNavUI(1)
            }
        }
        binding!!.bannerViewPager.setAdapter(adapter)
        binding!!.dotsIndicator.attachTo(binding!!.bannerViewPager)

        binding!!.bannerViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 1500)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        setupBanner()
        AdsManager.loadInterHome(requireActivity())
        AdsManager.loadInterMirroring(requireActivity())
        configScript = RemoteConfigdata(requireActivity())

        nativeAds()



        binding!!.rvPlaylists.setLayoutManager(LinearLayoutManager(getContext()))
        adapter = PlaylistAdapter(
            getContext(),
            playlistList,
            PlaylistAdapter.OnPlaylistClickListener { playlistName: String? ->


                AdsManager.showInterHome(requireActivity()) {
                    val intent = Intent(getActivity(), CategoryActivity::class.java)
                    intent.putExtra("PLAYLIST_NAME", playlistName)
                    startActivity(intent)
                }


            })
        binding!!.rvPlaylists.setAdapter(adapter)
        binding!!.cardCast.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {


                AdsManager.showInterMirroring(requireActivity()) {
                    val intent = Intent(getActivity(), MirrorStepsActivity::class.java)
                    startActivity(intent)
                }

            }
        })

        binding!!.btnSettings.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {


                val intent = Intent(getActivity(), SettingsActivity::class.java)
                startActivity(intent)


            }
        })
        binding!!.cardIptvSmart.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {


                AdsManager.showInterHome(requireActivity()) {
                    if (getActivity() is MainActivity) {
                        val mainActivity = getActivity() as MainActivity?
                        if (mainActivity!!.viewPager != null) {
                            mainActivity.viewPager.setCurrentItem(1, true)
                            mainActivity.updateNavUI(1)
                        }
                    }
                }


            }
        })

        return binding!!.getRoot()
    }

    private fun nativeAds() {

        Log.d("hh", "nativeAds: "+requireActivity().getShouldDisplayNativeHome())
        if (!requireActivity().getShouldDisplayNativeHome()) {
            binding?.adShimmer?.root?.gone
            binding?.frAds?.gone
            return
        }
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativehome2005) {

                    val handledAds = mutableSetOf<String>()
                    val retriedTags = mutableSetOf<String>()
                    val tag = "native_home_2005"

                    AdsManager.getAdLive(tag).observe(viewLifecycleOwner) { state ->
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    requireActivity(),
                                    state.ad,
                                    binding?.frAds,
                                    binding?.adShimmer?.root
                                )
                                //// Height cta
                                val remoteHeightDp = state.ctaHeight
                                val adBtn = binding!!.frAds.findViewById<View>(R.id.ad_call_to_action)

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

                            } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                                // No-fill / preload fail (real id ma sambhave) ->
                                // 90%+ show rate mate ahiya ek j var jate FRESH load karo (fallback).
                                if (retriedTags.add(tag)) {
                                    binding?.adShimmer?.root?.visible
                                    binding?.frAds?.visible
                                    AdsManager.loadAd(
                                        requireActivity(),
                                        AdsId.nativehome2005,
                                        R.layout.layout_native_ad_medium,
                                        tag
                                    )
                                    Log.d("AdManager123", "[$tag] fallback reload triggered")
                                } else {
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

    private fun updatePlaylists() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val context = context ?: return@launchWhenResumed
            val allChannels = AppDatabase.getInstance(context).historyDao().allHistory1

            if (!allChannels.isNullOrEmpty()) {
                val map = HashMap<String, Int>()

                for (c in allChannels) {
                    val pName = c.playlistName ?: "Local Playlist"


                    if (pName == "Cloud_Playlist") continue

                    map[pName] = map.getOrDefault(pName, 0) + 1
                }

                playlistList.clear()
                for ((key, value) in map) {
                    playlistList.add(PlaylistGroup(key, value))
                }

                playlistList.sortBy { it?.name?.lowercase() }

                adapter?.notifyDataSetChanged()
                showNoData(playlistList.isEmpty())
            } else {
                showNoData(true)
            }
        }
    }

    private fun showNoData(isEmpty: Boolean) {
        binding!!.rvPlaylists.setVisibility(if (isEmpty) View.GONE else View.VISIBLE)
        binding!!.layoutNoData.setVisibility(if (isEmpty) View.VISIBLE else View.GONE)
    }

    override fun onResume() {
        super.onResume()
        updatePlaylists()
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }
}