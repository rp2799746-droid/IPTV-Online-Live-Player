package com.iptv.online.smart.liveplayer.tv.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.iptv.online.smart.liveplayer.tv.Fregmnet.ChannelFragment;
import com.iptv.online.smart.liveplayer.tv.Fregmnet.FavoriteFragment;
import com.iptv.online.smart.liveplayer.tv.Fregmnet.HistoryFragment;
import com.iptv.online.smart.liveplayer.tv.Fregmnet.PlaylistFragment;


public class MyViewPagerAdapter extends FragmentStateAdapter {

    public MyViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PlaylistFragment();
            case 1:
                return new ChannelFragment();
            case 2:
                return new FavoriteFragment();
            case 3:
                return new HistoryFragment();
            default:
                return new PlaylistFragment();
        }
    }

    @Override
    public int getItemCount() {

        return 4;
    }
}