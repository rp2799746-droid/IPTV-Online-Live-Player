package com.iptv.online.smart.liveplayer.tv.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.iptv.online.smart.liveplayer.tv.Model.ChannelGroup;
import com.iptv.online.smart.liveplayer.tv.R;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {
    private Context context;
    private List<ChannelGroup> list;
    private List<ChannelGroup> listFull;
    private OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(String groupName);
    }

    public GroupAdapter(Context context, List<ChannelGroup> list, OnGroupClickListener listener) {
        this.context = context;
        this.list = list;
        this.listFull = new ArrayList<>(list);
        this.listener = listener;
    }


    public void filter(String query) {
        list.clear();
        if (query.isEmpty()) {
            list.addAll(listFull);
        } else {
            String filterPattern = query.toLowerCase().trim();
            for (ChannelGroup item : listFull) {

                if (item.getName().toLowerCase().contains(filterPattern)) {
                    list.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<ChannelGroup> newList) {
        this.list = newList;
        this.listFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ChannelGroup group = list.get(position);
        holder.tvName.setText(group.getName());
        holder.tvCount.setText(group.getCount() + " Channels");

        holder.itemView.setOnClickListener(v -> listener.onGroupClick(group.getName()));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        ImageView ivGroupIcon;
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGroupName);
            tvCount = itemView.findViewById(R.id.tvChannelCount);
            ivGroupIcon = itemView.findViewById(R.id.ivFolder);
        }
    }
}