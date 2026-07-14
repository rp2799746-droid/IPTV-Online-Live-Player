package com.iptv.online.smart.liveplayer.tv.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.iptv.online.smart.liveplayer.tv.Model.LanguageModel;
import com.iptv.online.smart.liveplayer.tv.R;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.MyViewHolder> {
    Context context;
    public List<LanguageModel> languageModelNews;
    OnClickItemListener onClickItemListener;
    private String str_lang;
    private boolean isFromSettings;
    public interface OnClickItemListener {
        void onClickItem(LanguageModel languageModel_);
    }

    public LanguageAdapter(List<LanguageModel> list, Context context, String languageCode,boolean isFromSettings, OnClickItemListener onClickItemListener) {
        languageModelNews = list;
        this.context = context;
        this.str_lang = languageCode;
        this.isFromSettings = isFromSettings;
        this.onClickItemListener = onClickItemListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new MyViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_app_language, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(final MyViewHolder myViewHolder, int i) {
        LanguageModel model = languageModelNews.get(i);

        myViewHolder.txt_lan_name.setText(model.s_lan_name);
        if (isFromSettings && str_lang != null && str_lang.equals(model.s_lan_name)) {
            myViewHolder.iv_select.setImageResource(R.drawable.select);
            myViewHolder.iv_select.setSelected(true);
            myViewHolder.txt_lan_name.setTextColor(Color.BLACK);
            myViewHolder.main_view.setBackgroundResource(R.drawable.select_lan);

//            myViewHolder.txt_lan_name.setTextColor(context.getResources().getColor(R.color.HoverColor));
        } else {
            myViewHolder.iv_select.setImageResource(R.drawable.selectun);
            myViewHolder.iv_select.setSelected(false);
            myViewHolder.txt_lan_name.setTextColor(Color.BLACK);
            myViewHolder.main_view.setBackgroundResource(R.drawable.set_lan);
        }

        myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFromSettings = true;
                str_lang = model.s_lan_name;
                onClickItemListener.onClickItem(model);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }


    @Override
    public int getItemCount() {
        return languageModelNews.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView flag_iv;
        TextView txt_lan_name;
        ImageView iv_select;
        RelativeLayout main_view;

        public MyViewHolder(View view) {
            super(view);
            flag_iv = (ImageView) view.findViewById(R.id.flag_iv);
            iv_select = (ImageView) view.findViewById(R.id.lan_selection);
            txt_lan_name = (TextView) view.findViewById(R.id.lan_tv);
            main_view = (RelativeLayout) view.findViewById(R.id.main_view);
        }
    }
}