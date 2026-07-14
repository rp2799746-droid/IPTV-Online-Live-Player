package com.iptv.online.smart.liveplayer.tv.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.iptv.online.smart.liveplayer.tv.Activity.PlayerActivity;
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase;
import com.iptv.online.smart.liveplayer.tv.Model.Channel;
import com.iptv.online.smart.liveplayer.tv.R;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    private final Context context;
    private List<Channel> channelList;
    private List<Channel> channelListFull;
    private OnChannelClickListener listener;

    public interface OnChannelClickListener {
        void onChannelClick(int position);
    }

    public void setOnChannelClickListener(OnChannelClickListener listener) {
        this.listener = listener;
    }

    public ChannelAdapter(Context context, List<Channel> channelList) {
        this.context = context;
        this.channelList = channelList;
        this.channelListFull = new ArrayList<>(channelList);
    }
    public void filter(String query) {
        channelList.clear();
        if (query.isEmpty()) {
            channelList.addAll(channelListFull);
        } else {
            String pattern = query.toLowerCase().trim();
            for (Channel item : channelListFull) {
                if (item.getChannelName().toLowerCase().contains(pattern)) {
                    channelList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }


    public void updateList(List<Channel> newList) {
        this.channelList.clear();
        this.channelList.addAll(newList);
        this.channelListFull.clear();
        this.channelListFull.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = channelList.get(position);


        if (channel.isFavorite()) {
            holder.ivFav.setImageResource(R.drawable.ic_fav_on);
            holder.ivFav.setColorFilter(Color.parseColor("#FF4081"));
        } else {
            holder.ivFav.setImageResource(R.drawable.ic_fav_off);
            holder.ivFav.setColorFilter(Color.parseColor("#757575"));
        }


        holder.ivFav.setOnClickListener(v -> {
            boolean newFavoriteStatus = !channel.isFavorite();
            channel.setFavorite(newFavoriteStatus);


            String uniqueId = channel.getChannelUrl() + "|" + channel.getPlaylistName();
            channel.setId(uniqueId);


            AppDatabase.getInstance(context).historyDao().insertHistory(channel);

            notifyItemChanged(position);

            String msg = newFavoriteStatus
                    ? context.getString(R.string.added_to_favorites)
                    : context.getString(R.string.removed_from_favorites);

            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        });
        if (context instanceof PlayerActivity) {
            int playingPos = ((PlayerActivity) context).getCurrentPosition();
            if (playingPos == position) {
                holder.itemView.setBackgroundColor(Color.parseColor("#E1F5FE"));
                holder.tvName.setTextColor(Color.parseColor("#41A4FF"));
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                holder.tvName.setTextColor(Color.BLACK);
            }
        }


        holder.tvName.setText(channel.getChannelName());
        holder.tvGroup.setText(channel.getChannelGroup());

        Glide.with(context)
                .load(channel.getChannelImg())
                .placeholder(R.drawable.ic_tv)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.ivLogo);


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChannelClick(position);
            } else {
                Intent intent = new Intent(context, PlayerActivity.class);
                intent.putParcelableArrayListExtra("channel_list", new ArrayList<>(channelList));
                intent.putExtra("position", position);
                context.startActivity(intent);
            }
        });
    }
    @Override
    public int getItemCount() {

        Log.d("dd", "getItemCount: "+channelList.size());
        return channelList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivLogo, ivFav;
        TextView tvName, tvGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivLogo = itemView.findViewById(R.id.ivChannelLogo);
            ivFav = itemView.findViewById(R.id.ivFav); // નવી લાઈન
            tvName = itemView.findViewById(R.id.tvChannelName);
            tvGroup = itemView.findViewById(R.id.tvChannelGroup);
        }
    }
}
