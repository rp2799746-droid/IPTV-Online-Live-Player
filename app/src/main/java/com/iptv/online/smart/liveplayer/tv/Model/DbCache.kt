package com.iptv.online.smart.liveplayer.tv.Model

/**
 * DB mathi chhelli vakhat vanchelu data memory ma raakhe chhe.
 *
 * Screen pachhi kholiye tyare list TARAT (cache mathi) dekhay chhe, ane
 * saathe saathe background ma fresh data vanchi ne update thai jaay chhe.
 * Etle na to main thread block thay (ANR), na to khali screen dekhay.
 *
 * DB ma kaink lakhaay (insert/delete) tyare invalidate() call karvu -
 * jethi juno data na dekhay.
 */
object DbCache {

    private val channelLists = HashMap<String, List<Channel>>()
    private val groupLists = HashMap<String, List<ChannelGroup?>>()
    private var playlists: List<PlaylistGroup?>? = null

    // ---- Channel list (History / Favorite / Group ni channels) ----

    @Synchronized
    fun getChannels(key: String): List<Channel>? = channelLists[key]

    @Synchronized
    fun putChannels(key: String, value: List<Channel>) {
        channelLists[key] = value
    }

    // ---- Group list (CategoryActivity) ----

    @Synchronized
    fun getGroups(key: String): List<ChannelGroup?>? = groupLists[key]

    @Synchronized
    fun putGroups(key: String, value: List<ChannelGroup?>) {
        groupLists[key] = value
    }

    // ---- Playlist list (PlaylistFragment) ----

    @Synchronized
    fun getPlaylists(): List<PlaylistGroup?>? = playlists

    @Synchronized
    fun putPlaylists(value: List<PlaylistGroup?>) {
        playlists = value
    }

    /** DB ma kaink badlay (insert/delete) etle badhu cache faaki devu. */
    @JvmStatic
    @Synchronized
    fun invalidate() {
        channelLists.clear()
        groupLists.clear()
        playlists = null
    }
}
