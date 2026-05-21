package com.iptv.online.smart.liveplayer.tv.Model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "history_table")
public class Channel  implements Parcelable {

    @PrimaryKey
    @NonNull
    private String id;

    private String channelUrl;
    private String channelName;

    private String channelImg;
    private String channelGroup;
    private String channelDrmType;
    private String channelDrmKey;
    private String playlistName;
    private boolean isFavorite;
    // Channel.java માં
    private String playlistUrl;
    private long historyTimestamp;

    // Constructor
    public Channel() {
    }

    public String getPlaylistUrl() { return playlistUrl; }
    public void setPlaylistUrl(String playlistUrl) { this.playlistUrl = playlistUrl; }


    // --- Getters and Setters ---
    public String getChannelName() { return channelName; }
    public void setChannelName(String channelName) { this.channelName = channelName; }

    public String getChannelUrl() { return channelUrl; }
    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
        updateId();
        }

    public String getChannelImg() { return channelImg; }
    public void setChannelImg(String channelImg) { this.channelImg = channelImg; }

    public String getChannelGroup() { return channelGroup; }
    public void setChannelGroup(String channelGroup) { this.channelGroup = channelGroup; }

    public String getChannelDrmType() { return channelDrmType; }
    public void setChannelDrmType(String channelDrmType) { this.channelDrmType = channelDrmType; }

    public String getChannelDrmKey() { return channelDrmKey; }
    public void setChannelDrmKey(String channelDrmKey) { this.channelDrmKey = channelDrmKey; }


    public String getPlaylistName() { return playlistName; }
    public void setPlaylistName(String playlistName) { this.playlistName = playlistName;
        updateId();}

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public long getHistoryTimestamp() { return historyTimestamp; }
    public void setHistoryTimestamp(long historyTimestamp) { this.historyTimestamp = historyTimestamp; }
    private void updateId() {
        if (channelUrl != null && playlistName != null) {
            this.id = channelUrl + "|" + playlistName;
        }
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }    // --- Parcelable Implementation ---

    protected Channel(Parcel in) {
        id = in.readString();
        channelName = in.readString();
        channelUrl = in.readString();
        channelImg = in.readString();
        channelGroup = in.readString();
        channelDrmType = in.readString();
        channelDrmKey = in.readString();
        playlistName = in.readString();
        playlistUrl = in.readString(); // આ ઉમેરો
        isFavorite = in.readByte() != 0;
        historyTimestamp = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(channelName);
        dest.writeString(channelUrl);
        dest.writeString(channelImg);
        dest.writeString(channelGroup);
        dest.writeString(channelDrmType);
        dest.writeString(channelDrmKey);
        dest.writeString(playlistName);
        dest.writeString(playlistUrl); // આ ઉમેરો
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeLong(historyTimestamp);

    }

    public static final Creator<Channel> CREATOR = new Creator<Channel>() {
        @Override
        public Channel createFromParcel(Parcel in) {
            return new Channel(in);
        }

        @Override
        public Channel[] newArray(int size) {
            return new Channel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}