package com.iptv.online.smart.liveplayer.tv.Adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ads.module.ads.ERainAd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.shimmer.ShimmerFrameLayout
import com.iptv.online.smart.liveplayer.tv.Activity.PlayerActivity
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata

class FavoriteAdapter(private val context: Context, incomingList: MutableList<Channel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val favList: MutableList<Channel> = ArrayList(incomingList)
    private var listener: OnFavoriteChangeListener? = null

    // ૧. Remote Config ડેટા મેળવો
    private val configScript = RemoteConfigdata(context as Activity)
    private val isAdEnabled: Boolean = configScript.isNeedToShowADs && configScript.nativeFavorite

    interface OnFavoriteChangeListener {
        fun onFavoriteEmpty()
    }

    fun setOnFavoriteChangeListener(listener: OnFavoriteChangeListener?) {
        this.listener = listener
    }

    override fun getItemViewType(position: Int): Int {
        // જો સ્વીચ ઓન હોય તો દર ૩ આઈટમ પછી એડ સ્લોટ આપો
        return if (isAdEnabled && (position + 1) % (AD_INTERVAL + 1) == 0) {
            TYPE_AD
        } else {
            TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(context).inflate(R.layout.layout_native_ad_item_large, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false)
            ViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_ITEM) {
            val itemHolder = holder as ViewHolder

            val actualPosition = if (isAdEnabled) {
                position - (position / (AD_INTERVAL + 1))
            } else {
                position
            }

            if (actualPosition < favList.size) {
                val channel = favList[actualPosition]
                bindChannelData(itemHolder, channel, actualPosition)
            }
        } else {
            handleNativeAd(holder as AdViewHolder)
        }
    }

    private fun handleNativeAd(adHolder: AdViewHolder) {
        val tag = "native_favorite"
        val state = InfinityAdsManager.adStateFlow.value[tag]

        if (isAdEnabled) {
            adHolder.itemView.visibility = View.VISIBLE
            updateHeight(adHolder.itemView, true)

            when (state) {
                is NativeAdUiState.Success -> {
                    adHolder.adShimmer.visibility = View.GONE
                    if (adHolder.adShimmer is ShimmerFrameLayout) {
                        (adHolder.adShimmer as ShimmerFrameLayout).stopShimmer()
                    }
                    adHolder.frAds.visibility = View.VISIBLE
                    ERainAd.getInstance().populateNativeAdView(
                        context as Activity, state.ad, adHolder.frAds, adHolder.adShimmer as ShimmerFrameLayout
                    )
                    //height cta
                    val remoteHeightDp = state.ctaHeight

                    val adBtn = adHolder.frAds.findViewById<View>(R.id.ad_call_to_action)

                    if (adBtn != null) {
                        val density = context.resources.displayMetrics.density
                        val heightInPx = (remoteHeightDp * density).toInt()

                        val params = adBtn.layoutParams
                        if (params != null) {
                            params.height = heightInPx
                            adBtn.layoutParams = params
                            adBtn.requestLayout()
                            Log.d("AdManager123", "✅ Height set to: $heightInPx px for holder tag: $tag")
                        }
                    } else {
                        Log.e("AdManager123", "❌ CTA Button not found in adHolder layout")
                    }

//

                }
                // Loading અને null બંને કેસમાં શિમર ચલાવો
                is NativeAdUiState.Loading, null -> {
                    adHolder.frAds.visibility = View.GONE
                    adHolder.adShimmer.visibility = View.VISIBLE
                    if (adHolder.adShimmer is ShimmerFrameLayout) {
                        (adHolder.adShimmer as ShimmerFrameLayout).startShimmer()
                    }
                }
                else -> {
                    // Failed કે Empty હોય તો જ છુપાવો
                    adHolder.itemView.visibility = View.GONE
                    updateHeight(adHolder.itemView, false)
                }
            }
        }
    }
    private fun bindChannelData(holder: ViewHolder, channel: Channel, pos: Int) {
        holder.tvName.text = channel.getChannelName()
        holder.tvGroup.text = channel.getChannelGroup()
        holder.ivFav.setImageResource(R.drawable.ic_fav_on)
        holder.ivFav.setColorFilter(Color.parseColor("#FF4081"))

        Glide.with(context)
            .load(channel.getChannelImg())
            .placeholder(R.drawable.ic_tv)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.ivLogo)

        holder.ivFav.setOnClickListener {
            // ફેવરિટ રિમૂવ લોજિક
            val cPos = holder.bindingAdapterPosition
            val idx = if (isAdEnabled) cPos - (cPos / (AD_INTERVAL + 1)) else cPos

            if (idx in favList.indices) {
                val ch = favList[idx]
                ch.isFavorite = false
                AppDatabase.getInstance(context).historyDao().insertHistory(ch)
                favList.removeAt(idx)
                notifyDataSetChanged()
                if (favList.isEmpty()) listener?.onFavoriteEmpty()
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putParcelableArrayListExtra("channel_list", ArrayList(favList))
                putExtra("position", pos)
            }
            context.startActivity(intent)
        }
    }

    private fun updateHeight(view: View, isVisible: Boolean) {
        val params = view.layoutParams as RecyclerView.LayoutParams
        if (isVisible) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.setMargins(0, 15, 0, 15)
        } else {
            params.height = 0
            params.width = 0
            params.setMargins(0, 0, 0, 0)
        }
        view.layoutParams = params
    }

    override fun getItemCount(): Int {
        if (favList.isEmpty()) return 0
        return if (isAdEnabled) {
            favList.size + (favList.size / AD_INTERVAL)
        } else {
            favList.size
        }
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivLogo: ImageView = v.findViewById(R.id.ivChannelLogo)
        val ivFav: ImageView = v.findViewById(R.id.ivFav)
        val tvName: TextView = v.findViewById(R.id.tvChannelName)
        val tvGroup: TextView = v.findViewById(R.id.tvChannelGroup)
    }

    class AdViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val frAds: FrameLayout = v.findViewById(R.id.fr_ads)
        val adShimmer: View = v.findViewById(R.id.adShimmer)
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 3
    }
}