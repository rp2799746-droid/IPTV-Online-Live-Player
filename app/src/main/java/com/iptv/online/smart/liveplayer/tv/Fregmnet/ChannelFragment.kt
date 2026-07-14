package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ads.module.ads.ERainAd
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.iptv.online.smart.liveplayer.tv.Activity.ChannelListActivity
import com.iptv.online.smart.liveplayer.tv.Adapter.GroupAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.Model.ChannelGroup
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.FragmentChannelBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale

class ChannelFragment : Fragment() {
    private var binding: FragmentChannelBinding? = null
    private var adapter: GroupAdapter? = null
    private val groupList: MutableList<ChannelGroup?> = ArrayList<ChannelGroup?>()
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null
    private lateinit var db: AppDatabase
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var noInternetDialog: Dialog? = null
    companion object {
        const val CLOUD_PLAYLIST_NAME = "Cloud_Playlist"
    }
    private var configScript: RemoteConfigdata? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChannelBinding.inflate(inflater, container, false)
        db = AppDatabase.getInstance(requireContext())
        configScript = RemoteConfigdata(requireActivity())

        nativeAds()
        binding!!.back.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                requireActivity().onBackPressed()
            }
        })
        setupRecyclerView()
        initRemoteConfig()
        setupSearch()


        return binding!!.getRoot()
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
                                Log.d("AdManager121113", "[$tag] Success,🚀 showing ad is [${state.adsID}]")
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

    private fun setupRecyclerView() {
        binding!!.rvChannels.layoutManager = GridLayoutManager(context, 2)
        adapter = GroupAdapter(context, groupList) { groupName ->
            val intent = Intent(activity, ChannelListActivity::class.java)
            intent.putExtra("PLAYLIST_NAME", CLOUD_PLAYLIST_NAME)
            intent.putExtra("GROUP_NAME", groupName)
            startActivity(intent)
        }
        binding!!.rvChannels.adapter = adapter
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        if (getActivity() != null) {
                            requireActivity().runOnUiThread(Runnable {
                                if (noInternetDialog != null && noInternetDialog!!.isShowing()) {
                                    noInternetDialog!!.dismiss()
                                    // નેટ આવતા જ ડેટા ફેચ કરવાનું શરૂ કરો
                                    initRemoteConfig()
                                }
                            })
                        }
                    }
                }
                connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSearch() {
        binding!!.etSearchChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val query = s.toString()
                binding!!.ivClearSearchChannel.setVisibility(if (query.isEmpty()) View.GONE else View.VISIBLE)


                if (adapter != null) {
                    adapter!!.filter(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding!!.ivClearSearchChannel.setOnClickListener({ v -> binding!!.etSearchChannel.setText("") })
    }

    private fun initRemoteConfig() {
        if (!this.isNetworkAvailable) {
            showNoInternetDialog()
            return
        }

        binding!!.pbLoading.setVisibility(View.VISIBLE)

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0).build()
        mFirebaseRemoteConfig!!.setConfigSettingsAsync(configSettings)

        mFirebaseRemoteConfig!!.fetch(0)
            .addOnCompleteListener(requireActivity(), OnCompleteListener { task: Task<Void?>? ->
                if (task!!.isSuccessful()) {
                    mFirebaseRemoteConfig!!.activate()
                        .addOnCompleteListener(OnCompleteListener { activateTask: Task<Boolean?>? ->
                            val m3uContent = mFirebaseRemoteConfig!!.getString("iptv_url")
                            if (m3uContent != null && m3uContent.contains("#EXTM3U")) {
                                parseAndSave(m3uContent)
                            } else {
                                binding!!.pbLoading.setVisibility(View.GONE)
                                updateGroups()
                            }
                        })
                } else {
                    binding!!.pbLoading.setVisibility(View.GONE)
                    updateGroups()
                }
            })
    }

    val isNetworkAvailable: Boolean
        get() {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager != null) {
                val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
                return activeNetworkInfo != null && activeNetworkInfo.isConnected()
            }
            return false
        }

    private fun showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog!!.isShowing()) return

        noInternetDialog = Dialog(requireContext(), R.style.CustomDialog)
        val view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_no_internet, null)
        noInternetDialog!!.setContentView(view)
        noInternetDialog!!.setCancelable(false)

        view.findViewById<View?>(R.id.btn_settings)
            .setOnClickListener(View.OnClickListener { v: View? ->
                if (this.isNetworkAvailable) {
                    noInternetDialog!!.dismiss()
                    initRemoteConfig()
                } else {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            })

        noInternetDialog!!.show()
        registerNetworkCallback()
    }
    private fun parseAndSave(data: String) {
        binding!!.pbLoading.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val channels: MutableList<Channel> = ArrayList()
            val uniqueUrls = HashSet<String>()
            val lines = data.split("\\r?\\n|\\r".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var current: Channel? = null

            for (line in lines) {
                val row = line.trim()
                if (row.isEmpty()) continue

                if (row.uppercase(Locale.getDefault()).contains("#EXTINF")) {
                    current = Channel()
                    current.playlistName = CLOUD_PLAYLIST_NAME

                    if (row.contains(",")) {
                        current.channelName = row.substring(row.lastIndexOf(",") + 1).trim()
                    }
                    if (row.contains("tvg-logo=\"")) {
                        val s = row.indexOf("tvg-logo=\"") + 10
                        val e = row.indexOf("\"", s)
                        if (e > s) current.channelImg = row.substring(s, e)
                    }

                    if (row.contains("group-title=\"")) {
                        val s = row.indexOf("group-title=\"") + 13
                        val e = row.indexOf("\"", s)
                        if (e > s) current.channelGroup = row.substring(s, e)
                    } else {
                        current.channelGroup = "Other"
                    }
                } else if (row.lowercase(Locale.getDefault()).startsWith("http") && current != null) {
                    current.channelUrl = row
                    val uniqueId = "$row|$CLOUD_PLAYLIST_NAME"
                    current.id = uniqueId

                    // --- અહીં સુધારો છે ---
                    val existing = db.historyDao().getChannelById(uniqueId)
                    if (existing != null) {
                        current.isFavorite = existing.isFavorite
                        // જો જૂની હિસ્ટ્રી હોય તો તે ટાઈમ પાછો સેટ કરો
                        current.historyTimestamp = existing.historyTimestamp
                    } else {
                        // જો નવી ચેનલ હોય તો 0 સેટ કરો
                        current.historyTimestamp = 0
                    }

                    if (!uniqueUrls.contains(row)) {
                        channels.add(current)
                        uniqueUrls.add(row)
                    }
                    current = null
                }
            }

            if (channels.isNotEmpty()) {
                for (item in channels) {
                    db.historyDao().insertHistory(item)
                }
            }

            withContext(Dispatchers.Main) {
                binding!!.pbLoading.visibility = View.GONE
                updateGroups()
            }
        }
    }

    private fun updateGroups() {
        lifecycleScope.launch(Dispatchers.IO) {
            val all = db.historyDao().getChannelsByPlaylist(CLOUD_PLAYLIST_NAME)

            if (!all.isNullOrEmpty()) {
                val map = HashMap<String, Int>()
                for (c in all) {
                    val gName = if (c.channelGroup.isNullOrEmpty()) "Other" else c.channelGroup!!
                    map[gName] = map.getOrDefault(gName, 0) + 1
                }

                val tempList: MutableList<ChannelGroup> = ArrayList()
                for (entry in map.entries) {
                    tempList.add(ChannelGroup(entry.key, entry.value))
                }

                Collections.sort(tempList) { g1, g2 ->
                    g1.name.compareTo(g2.name, ignoreCase = true)
                }

                withContext(Dispatchers.Main) {
                    groupList.clear()
                    groupList.addAll(tempList)
                    adapter!!.updateList(groupList)
                    binding!!.rvChannels.visibility = View.VISIBLE
                    binding!!.layoutNoData.visibility = View.GONE
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding!!.rvChannels.visibility = View.GONE
                    binding!!.layoutNoData.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateGroups()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkCallback != null) {
            val connectivityManager =
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (connectivityManager != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
            }
        }
    }
}