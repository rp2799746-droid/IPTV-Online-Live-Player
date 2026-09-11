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

    @Query("SELECT CASE WHEN channelGroup IS NULL OR channelGroup = '' THEN 'Other' ELSE channelGroup END AS name, "
            + "COUNT(*) AS count FROM history_table WHERE playlistName = :pName "
            + "GROUP BY name ORDER BY name COLLATE NOCASE ASC")
    List<ChannelGroup> getGroupCounts(String pName);

    @Query("SELECT IFNULL(playlistName, 'Local Playlist') AS name, "
            + "COUNT(*) AS count FROM history_table "
            + "WHERE playlistName IS NULL OR playlistName != 'Cloud_Playlist' "
            + "GROUP BY name ORDER BY name COLLATE NOCASE ASC")
    List<PlaylistGroup> getPlaylistCounts();


    @Query("SELECT EXISTS(SELECT 1 FROM history_table WHERE "
            + "playlistName = :name COLLATE NOCASE "
            + "OR playlistUrl = :url COLLATE NOCASE "
            + "OR channelUrl = :url COLLATE NOCASE LIMIT 1)")
    boolean playlistExists(String name, String url);

}