package com.iptv.online.smart.liveplayer.tv.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iptv.online.smart.liveplayer.tv.Model.BannerItem;
import com.iptv.online.smart.liveplayer.tv.R;

import java.util.ArrayList;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private ArrayList<BannerItem> bannerItems;
    private OnBannerClickListener listener;
    public interface OnBannerClickListener {
        void onBannerClick(int position);
    }
    public BannerAdapter(ArrayList<BannerItem> bannerItems, OnBannerClickListener listener) {
        this.bannerItems = bannerItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.imageView.setImageResource(bannerItems.get(position).getImageRes());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bannerItems.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgBanner);
        }
    }
}