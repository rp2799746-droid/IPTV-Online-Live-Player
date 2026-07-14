package com.iptv.online.smart.liveplayer.tv.Fregmnet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.online.smart.liveplayer.tv.Adapter.FavoriteAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata

class FavoriteFragment : Fragment() {
    private var rvFav: RecyclerView? = null
    private var adapter: FavoriteAdapter? = null
    private var tvNoData: LinearLayout? = null
    private var configScript: RemoteConfigdata? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorite, container, false)

        rvFav = view.findViewById<RecyclerView?>(R.id.rvFavorite)
        val back = view.findViewById<ImageView?>(R.id.back)
        back.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                requireActivity().onBackPressed()
            }
        })
        tvNoData = view.findViewById<LinearLayout?>(R.id.layoutNoData)



        configScript = RemoteConfigdata(requireActivity())


        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            InfinityAdsManager.adStateFlow.collect { states ->
                val state = states["native_favorite_2005"]

                // જો સ્ટેટ Success હોય કે Loading હોય, બંને વખતે એડેપ્ટરને જાણ કરો
                // જેથી તે શિમર અથવા એડ બતાવી શકે
                if (state is NativeAdUiState.Success || state is NativeAdUiState.Loading) {
                    adapter?.notifyDataSetChanged()
                }
            }
        }




        rvFav!!.setLayoutManager(LinearLayoutManager(getContext()))

        loadFavoriteChannels()

        return view
    }

private fun loadFavoriteChannels() {

    val favList = AppDatabase.getInstance(requireContext()).historyDao().favoriteChannels

    if (favList.isNullOrEmpty()) {
        tvNoData?.visibility = View.VISIBLE
        rvFav?.visibility = View.GONE
    } else {
        tvNoData?.visibility = View.GONE
        rvFav?.visibility = View.VISIBLE

        adapter = FavoriteAdapter(
            requireActivity(),
            ArrayList(favList)
        )
        rvFav?.adapter = adapter

        adapter?.setOnFavoriteChangeListener(object : FavoriteAdapter.OnFavoriteChangeListener {
            override fun onFavoriteEmpty() {
                tvNoData?.visibility = View.VISIBLE
                rvFav?.visibility = View.GONE
            }
        })
    }
}


    override fun onResume() {
        super.onResume()
        loadFavoriteChannels()
    }
}