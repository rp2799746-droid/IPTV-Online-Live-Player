package com.iptv.online.smart.liveplayer.tv.Model


object DbCache {

    private val channelLists = HashMap<String, List<Channel>>()
    private val groupLists = HashMap<String, List<ChannelGroup?>>()
    private var playlists: List<PlaylistGroup?>? = null


    @Synchronized
    fun getChannels(key: String): List<Channel>? = channelLists[key]

    @Synchronized
    fun putChannels(key: String, value: List<Channel>) {
        channelLists[key] = value
    }


    @Synchronized
    fun getGroups(key: String): List<ChannelGroup?>? = groupLists[key]

    @Synchronized
    fun putGroups(key: String, value: List<ChannelGroup?>) {
        groupLists[key] = value
    }


    @Synchronized
    fun getPlaylists(): List<PlaylistGroup?>? = playlists

    @Synchronized
    fun putPlaylists(value: List<PlaylistGroup?>) {
        playlists = value
    }

    @JvmStatic
    @Synchronized
    fun invalidate() {
        channelLists.clear()
        groupLists.clear()
        playlists = null
    }
}
