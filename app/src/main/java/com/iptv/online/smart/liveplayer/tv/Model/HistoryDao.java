package com.iptv.online.smart.liveplayer.tv.Model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHistory(Channel channel);

    @Query("SELECT * FROM history_table WHERE historyTimestamp > 0 ORDER BY historyTimestamp DESC")
    List<Channel> getAllHistory();

    @Query("DELETE FROM history_table")
    void clearAllHistory();
    @Query("SELECT * FROM history_table WHERE isFavorite = 1")
    List<Channel> getFavoriteChannels();
    @Query("SELECT * FROM history_table WHERE id = :channelId LIMIT 1")
    Channel getChannelById(String channelId);

    @Query("SELECT * FROM history_table WHERE playlistName = :pName")
    List<Channel> getChannelsByPlaylist(String pName);

    @Query("SELECT * FROM history_table WHERE playlistName = :pName AND channelGroup = :gName")
    List<Channel> getChannelsByGroup(String pName, String gName);
    @Query("SELECT * FROM history_table ORDER BY id DESC")
    List<Channel> getAllHistory1();

}