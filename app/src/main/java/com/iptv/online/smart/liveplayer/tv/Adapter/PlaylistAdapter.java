package com.iptv.online.smart.liveplayer.tv.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iptv.online.smart.liveplayer.tv.Model.PlaylistGroup;
import com.iptv.online.smart.liveplayer.tv.R;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private Context context;
    private List<PlaylistGroup> playlistList;
    private OnPlaylistClickListener listener;


    public interface OnPlaylistClickListener {
        void onPlaylistClick(String playlistName);
    }

    public PlaylistAdapter(Context context, List<PlaylistGroup> playlistList, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlistList = playlistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        PlaylistGroup playlist = playlistList.get(position);


        holder.tvName.setText(playlist.getName());
        holder.tvCount.setText(String.valueOf(playlist.getCount()));
        
        holder.tvSubInfo.setText("Total " + playlist.getCount() + " Channels available");


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist.getName());
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvSubInfo, tvCount;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubInfo = itemView.findViewById(R.id.tvSubInfo);
            tvCount = itemView.findViewById(R.id.tvCount);
        }
    }
}