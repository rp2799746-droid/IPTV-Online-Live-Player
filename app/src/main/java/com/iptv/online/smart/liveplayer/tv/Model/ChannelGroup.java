package com.iptv.online.smart.liveplayer.tv.Model;

public class ChannelGroup {
    private String name;
    private int count;

    public ChannelGroup(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() { return name; }
    public int getCount() { return count; }
}