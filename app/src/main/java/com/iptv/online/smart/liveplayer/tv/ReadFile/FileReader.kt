package com.iptv.online.smart.liveplayer.tv.ReadFile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.iptv.online.smart.liveplayer.tv.Activity.MainActivity
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.R
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FileReader(
    private val activity: Activity,
    private val fileUri: Uri,
    private val playlistName: String?
) {
    private val EXT_INF_SP = "#EXTINF:"
    private val COMMA = ","
    private val TVG_LOGO = "tvg-logo="
    private val GROUP_TITLE = "group-title="

    private val executorService: ExecutorService

    private var mInterstitialAd_addplaylist: ApInterstitialAd? = null

    interface OnFileReadListener {
        fun onFinish(playlistName: String?, playlistUrl: String?)
    }

    private var listener: OnFileReadListener? = null

    fun setOnFileReadListener(listener: OnFileReadListener?) {
        this.listener = listener
    }

    init {
        this.executorService = Executors.newSingleThreadExecutor()
    }


    fun readFile() {
        executorService.execute(Runnable execute@{
            val tempChannelList: MutableList<Channel?> = ArrayList<Channel?>()
            var inputStream: InputStream? = null
            try {
                if (fileUri.toString().startsWith("http")) {
                    val url = URL(fileUri.toString())
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setConnectTimeout(15000)
                    connection.setReadTimeout(15000)
                    connection.setRequestMethod("GET")
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connect()

                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        inputStream = connection.getInputStream()
                    } else {
                        showToast(activity.getString(R.string.server_error) + connection.getResponseCode())
                        return@execute
                    }
                } else {
                    inputStream = activity.getContentResolver().openInputStream(fileUri)
                }

                if (inputStream != null) {
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    var currentLine: String?
                    var channel = Channel()

                    while ((bufferedReader.readLine().also { currentLine = it }) != null) {
                        currentLine = currentLine!!.trim { it <= ' ' }
                        if (currentLine.isEmpty() || currentLine.startsWith("#EXTM3U")) continue

                        if (currentLine.startsWith(EXT_INF_SP)) {
                            parseExtInf(currentLine, channel)
                        } else if (!currentLine.startsWith("#")) {
                            channel.setChannelUrl(currentLine)
                            channel.setPlaylistName(this.playlistName)
                            channel.setPlaylistUrl(fileUri.toString())

                            if (channel.getChannelName() == null || channel.getChannelName()
                                    .isEmpty()
                            ) {
                                channel.setChannelName("Channel " + (tempChannelList.size + 1))
                            }
                            tempChannelList.add(channel)
                            channel = Channel()
                        }
                    }
                    bufferedReader.close()
                    inputStream.close()
                    saveToDb(tempChannelList)
                }
            } catch (e: Exception) {
                showToast(activity.getString(R.string.error) + " : " + e.getLocalizedMessage())
            }
        })
    }


    private fun parseExtInf(line: String, channel: Channel) {
        if (line.contains(COMMA)) {
            val name =
                line.substring(line.lastIndexOf(COMMA) + 1).replace("\"", "").trim { it <= ' ' }
            channel.setChannelName(name)
        }


        if (line.contains(TVG_LOGO)) {
            try {
                val parts: Array<String?> =
                    line.split(TVG_LOGO.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val logo = parts[1]!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].split(COMMA.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].replace("\"", "").trim { it <= ' ' }
                channel.setChannelImg(logo)
            } catch (ignored: Exception) {
            }
        }


        if (line.contains(GROUP_TITLE)) {
            try {
                val parts: Array<String?> =
                    line.split(GROUP_TITLE.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val group = parts[1]!!.split(COMMA.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].replace("\"", "").trim { it <= ' ' }
                channel.setChannelGroup(group)
            } catch (ignored: Exception) {
                channel.setChannelGroup("Other")
            }
        } else {
            channel.setChannelGroup("Other")
        }
    }


   private fun saveToDb(list: MutableList<Channel?>) {
       if (!list.isEmpty()) {
           try {
               for (channel in list) {
                   AppDatabase.getInstance(activity).historyDao().insertHistory(channel)
               }
               activity.runOnUiThread {
                   listener?.onFinish(playlistName, fileUri.toString())
               }
           } catch (e: Exception) {
               showToast(activity.getString(R.string.failed_to_save_data))
           }
       } else {
           showToast(activity.getString(R.string.no_valid_channels_found_in_file))
       }
   }
    private fun showToast(message: String?) {
        if (!activity.isFinishing()) {
            activity.runOnUiThread(Runnable {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            })
        }
    }
}