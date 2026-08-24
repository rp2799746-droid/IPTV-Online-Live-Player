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
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.Model.DbCache
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoriteFragment : Fragment() {
    private companion object {
        const val CACHE_KEY = "favorite"
    }

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


        AdsManager.getAdLive("native_favorite_2005").observe(viewLifecycleOwner) { state ->
            if (state is NativeAdUiState.Success || state is NativeAdUiState.Loading) {
                adapter?.notifyDataSetChanged()
            }
        }




        rvFav!!.setLayoutManager(LinearLayoutManager(getContext()))

        // onResume ma pan aa j load thay chhe, etle ahi bijivar call nathi karvi.
        return view
    }

// Cache ma data hoy to TARAT batavi daie, ane saathe background ma DB mathi
// fresh data vanchi ne update kari daie (main thread block na thay - ANR).
private fun loadFavoriteChannels() {
    val dao = AppDatabase.getInstance(requireContext()).historyDao()

    DbCache.getChannels(CACHE_KEY)?.let { showFavorites(it) }

    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
        val favList = dao.favoriteChannels ?: emptyList()
        DbCache.putChannels(CACHE_KEY, favList)

        withContext(Dispatchers.Main) { showFavorites(favList) }
    }
}

private fun showFavorites(favList: List<Channel>) {
    if (favList.isEmpty()) {
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