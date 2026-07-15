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
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import java.util.Locale

class Historychannel(private val context: Context, private val channelList: MutableList<Channel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {

    private val channelListFull: MutableList<Channel> = ArrayList(channelList)
    private var listener: OnChannelClickListener? = null

    // Remote Config ઇનિશિયલાઈઝેશન
    private val configScript = RemoteConfigdata(context as Activity)

    // એડ બતાવવી કે નહીં તેની કન્ડિશન (Remote Config માંથી)
    private val isAdEnabled: Boolean = configScript.isNeedToShowADs && configScript.nativehistory2005

    interface OnChannelClickListener {
        fun onChannelClick(position: Int)
    }

    fun setOnChannelClickListener(listener: OnChannelClickListener?) {
        this.listener = listener
    }

    override fun getItemViewType(position: Int): Int {
        if (isAdEnabled && (position + 1) % (AD_INTERVAL + 1) == 0) {
            return TYPE_AD
        }
        return TYPE_ITEM
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

            val actualPosition: Int = if (isAdEnabled) {
                position - (position / (AD_INTERVAL + 1))
            } else {
                position
            }

            val channel = channelList.get(actualPosition)

            if (channel.isFavorite()) {
                itemHolder.ivFav.setImageResource(R.drawable.ic_fav_on)
                itemHolder.ivFav.setColorFilter(Color.parseColor("#FF4081"))
            } else {
                itemHolder.ivFav.setImageResource(R.drawable.ic_fav_off)
                itemHolder.ivFav.setColorFilter(Color.parseColor("#757575"))
            }

            itemHolder.ivFav.setOnClickListener {
                val newFavoriteStatus = !channel.isFavorite()
                channel.isFavorite = newFavoriteStatus
                val db = AppDatabase.getInstance(context)
                val primaryKey = "${channel.channelUrl}|${channel.playlistName}"
                channel.id = primaryKey
                channel.historyTimestamp = System.currentTimeMillis()
                db.historyDao().insertHistory(channel)
                notifyItemChanged(position)
            }

            if (context is PlayerActivity) {
                val playingPos = (context as PlayerActivity).getCurrentPosition()
                if (playingPos == actualPosition) {
                    itemHolder.itemView.setBackgroundColor(Color.parseColor("#E1F5FE"))
                    itemHolder.tvName.setTextColor(Color.parseColor("#41A4FF"))
                } else {
                    itemHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
                    itemHolder.tvName.setTextColor(Color.BLACK)
                }
            }

            itemHolder.tvName.text = channel.getChannelName()
            itemHolder.tvGroup.text = channel.getChannelGroup()

            Glide.with(context)
                .load(channel.getChannelImg())
                .placeholder(R.drawable.ic_tv)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(itemHolder.ivLogo)

            itemHolder.itemView.setOnClickListener {
                if (listener != null) {
                    listener!!.onChannelClick(actualPosition)
                } else {
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putParcelableArrayListExtra("channel_list", ArrayList(channelList))
                    intent.putExtra("position", actualPosition)
                    context.startActivity(intent)
                }
            }

        } else {
            loadNativeAdForAdapter(holder as AdViewHolder)
        }
    }

    private fun loadNativeAdForAdapter(adHolder: AdViewHolder) {
        val tag = "native_history_2005"
        val state = AdsManager.getAdLive(tag).value

        if (state is NativeAdUiState.Success) {
            adHolder.itemView.visibility = View.VISIBLE
            updateLayoutParams(adHolder.itemView, true)
            adHolder.adShimmer.visibility = View.GONE
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

        } else if (state is NativeAdUiState.Loading) {
            adHolder.itemView.visibility = View.VISIBLE
            adHolder.adShimmer.visibility = View.VISIBLE
            adHolder.frAds.visibility = View.GONE
        } else {
            adHolder.itemView.visibility = View.GONE
            updateLayoutParams(adHolder.itemView, false)
        }
    }

    private fun updateLayoutParams(view: View, isVisible: Boolean) {
        val params = view.layoutParams as RecyclerView.LayoutParams
        if (isVisible) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            params.height = 0
            params.width = 0
            params.setMargins(0, 0, 0, 0)
        }
        view.layoutParams = params
    }

    override fun getItemCount(): Int {
        if (channelList.isEmpty()) return 0
        return if (isAdEnabled) {
            channelList.size + (channelList.size / AD_INTERVAL)
        } else {
            channelList.size
        }
    }

    fun filter(query: String) {
        channelList.clear()
        if (query.isEmpty()) {
            channelList.addAll(channelListFull)
        } else {
            val pattern = query.lowercase(Locale.getDefault()).trim()
            for (item in channelListFull) {
                if (item.getChannelName().lowercase(Locale.getDefault()).contains(pattern)) {
                    channelList.add(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    fun updateList(newList: MutableList<Channel>) {
        this.channelList.clear()
        this.channelList.addAll(newList)
        this.channelListFull.clear()
        this.channelListFull.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.ivChannelLogo)
        val ivFav: ImageView = itemView.findViewById(R.id.ivFav)
        val tvName: TextView = itemView.findViewById(R.id.tvChannelName)
        val tvGroup: TextView = itemView.findViewById(R.id.tvChannelGroup)
    }

    class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frAds: FrameLayout = itemView.findViewById(R.id.fr_ads)
        val adShimmer: View = itemView.findViewById(R.id.adShimmer)
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_AD = 1
        private const val AD_INTERVAL = 3
    }
}