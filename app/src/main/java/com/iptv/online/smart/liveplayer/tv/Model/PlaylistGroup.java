package com.iptv.online.smart.liveplayer.tv.Model;

public class PlaylistGroup {
    private String name;
    private int count;
    public PlaylistGroup(String name, int count) { this.name = name; this.count = count; }
    public String getName() { return name; }
    public int getCount() { return count; }
}