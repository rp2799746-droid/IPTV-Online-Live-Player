package com.iptv.online.smart.liveplayer.tv.Activity

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.textfield.TextInputEditText
import com.iptv.online.smart.liveplayer.tv.Adapter.ChannelAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.Model.DbCache
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : Base__Activity<ActivityPlayerBinding>() {
    private val ratingHandler = Handler(Looper.getMainLooper())
    private val countdownHandler = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null
    private var channelList: ArrayList<Channel>? = null
    private var currentPosition: Int = 0
    private var countdownTime = 5
    private var nextAutoDialog: AlertDialog? = null
    private var upNextAdapter: ChannelAdapter? = null
    private var RATE = 0

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun setViewBinding(): ActivityPlayerBinding {
        return ActivityPlayerBinding.inflate(layoutInflater)
    }

    override fun bindObjects() {
        channelList = intent.getParcelableArrayListExtra("channel_list")
        currentPosition = intent.getIntExtra("position", 0)

        initPlayer()
        if (!channelList.isNullOrEmpty()) {
            setupUpNextList()
        }
    }

    override fun bindListener() {
        binding.back.setOnClickListener { onBackPressed() }

        binding.History.setOnClickListener {
            if (!channelList.isNullOrEmpty() && currentPosition in channelList!!.indices) {
                addToHistory(channelList!![currentPosition])
            }
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("target_fragment", "history")
            }
            startActivity(intent)
            finish()
        }

        binding.Favorite.setOnClickListener {
            if (!channelList.isNullOrEmpty() && currentPosition in channelList!!.indices) {
                toggleFavorite(channelList!![currentPosition])
            }
        }

        binding.Share.setOnClickListener { shareCurrentChannel() }
    }

    override fun bindMethod() {
        if (!channelList.isNullOrEmpty()) {
            playChannel(currentPosition)
            checkAndShowRatingAfterDelay()
        }
    }

    private fun checkAndShowRatingAfterDelay() {
        ratingHandler.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed

            if (!MainActivity.isRatingShownInSession) {
                val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
                val isRatingDone = shared.getBoolean("rating_done", false)

                if (!isRatingDone) {
                    MainActivity.isRatingShownInSession = true
                    openRateDialog()
                }
            }
        }, 10000)
    }

    fun getCurrentPosition(): Int = currentPosition

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                binding.playerProgressBar.visibility =
                    if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                if (state == Player.STATE_ENDED) {
                    showCustomNextDialog()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                player?.let {
                    currentPosition = it.currentMediaItemIndex
                    updateUI(currentPosition)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.link_error_skipping_to_next),
                    Toast.LENGTH_SHORT
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    if (player != null) {
                        if (player!!.hasNextMediaItem()) playNext()
                        else finish()
                    }
                }, 2000)
            }
        })
    }

    // ✅ ANR FIX: લૂપ અને ડેટાબેઝ ક્વેરી બેકગ્રાઉન્ડ થ્રેડમાં
    private fun setupUpNextList() {
        upNextAdapter = ChannelAdapter(this, channelList)
        upNextAdapter?.setOnChannelClickListener { position ->
            if (player != null) {
                player?.seekTo(position, 0)
                player?.prepare()
                player?.play()
                updateUI(position)
            }
        }

        binding.rvUpNext.layoutManager = LinearLayoutManager(this)
        binding.rvUpNext.adapter = upNextAdapter
        binding.rvUpNext.scrollToPosition(currentPosition)

        // બેકગ્રાઉન્ડમાં ફેવરિટ સ્ટેટસ ચેક કરો
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext).historyDao()
            channelList?.forEach { channel ->
                val primaryKey = channel.channelUrl + "|" + channel.playlistName
                val dbData = db.getChannelById(primaryKey)
                if (dbData != null) {
                    channel.isFavorite = dbData.isFavorite
                }
            }
            withContext(Dispatchers.Main) {
                upNextAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun playChannel(position: Int) {
        val list = channelList ?: return
        if (position !in list.indices) return
        currentPosition = position

        val items = list.map { MediaItem.fromUri(it.channelUrl) }
        player?.setMediaItems(items, position, 0)
        player?.prepare()
        player?.play()
        updateUI(position)
    }

    private fun updateUI(position: Int) {
        val list = channelList ?: return
        if (position !in list.indices) return

        val channel = list[position]
        binding.tvPlayerChannelName.text = channel.channelName
        binding.tvPlaylistInfo.text = "${getString(R.string.playing)} ${position + 1} ${getString(R.string.of)} ${list.size}"

        // ✅ ANR FIX: DB Call in Background
        lifecycleScope.launch(Dispatchers.IO) {
            val primaryKey = channel.channelUrl + "|" + channel.playlistName
            val dbData = AppDatabase.getInstance(applicationContext).historyDao().getChannelById(primaryKey)
            val favStatus = (dbData != null && dbData.isFavorite)
            channel.isFavorite = favStatus

            // History માં સેવ કરો
            channel.historyTimestamp = System.currentTimeMillis()
            AppDatabase.getInstance(applicationContext).historyDao().insertHistory(channel)
            DbCache.invalidate()

            withContext(Dispatchers.Main) {
                updateFavoriteIcon(favStatus)
                upNextAdapter?.notifyDataSetChanged()
                binding.rvUpNext.smoothScrollToPosition(position)
            }
        }
    }

    private fun toggleFavorite(channel: Channel) {
        lifecycleScope.launch(Dispatchers.IO) {
            val primaryKey = channel.channelUrl + "|" + channel.playlistName
            val dao = AppDatabase.getInstance(applicationContext).historyDao()
            var dbData = dao.getChannelById(primaryKey)

            val newFavoriteStatus: Boolean
            if (dbData != null) {
                newFavoriteStatus = !dbData.isFavorite
                dbData.isFavorite = newFavoriteStatus
                dbData.historyTimestamp = System.currentTimeMillis()
            } else {
                newFavoriteStatus = true
                channel.id = primaryKey
                channel.isFavorite = true
                channel.historyTimestamp = System.currentTimeMillis()
                dbData = channel
            }

            dao.insertHistory(dbData)
            DbCache.invalidate()
            channel.isFavorite = newFavoriteStatus

            withContext(Dispatchers.Main) {
                updateFavoriteIcon(newFavoriteStatus)
                Toast.makeText(
                    this@PlayerActivity,
                    if (newFavoriteStatus) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites),
                    Toast.LENGTH_SHORT
                ).show()
                upNextAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.Favorite.setImageResource(R.drawable.ic_fav_on)
        } else {
            binding.Favorite.setImageResource(R.drawable.ic_fav_off)
        }
    }

    private fun addToHistory(channel: Channel?) {
        if (channel?.channelUrl == null) return
        lifecycleScope.launch(Dispatchers.IO) {
            channel.historyTimestamp = System.currentTimeMillis()
            AppDatabase.getInstance(applicationContext).historyDao().insertHistory(channel)
            DbCache.invalidate()
        }
    }

    private fun shareCurrentChannel() {
        val list = channelList
        if (!list.isNullOrEmpty() && currentPosition in list.indices) {
            val currentChannel = list[currentPosition]
            val shareMessage = "${getString(R.string.check_out_this_iptv_online_live_player)}\n${getString(R.string.channel1)} ${currentChannel.channelName}\n${getString(R.string.stream_link)} ${currentChannel.channelUrl}"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Channel"))
        }
    }

    private fun showCustomNextDialog() {
        val list = channelList ?: return
        if (currentPosition >= list.size - 1) return

        val dialogView = layoutInflater.inflate(R.layout.layout_next_channel, null)
        nextAutoDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        nextAutoDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        nextAutoDialog?.show()

        val tvNextName = dialogView.findViewById<TextView>(R.id.tvNextChannelName)
        val tvTimer = dialogView.findViewById<TextView>(R.id.tvTimerCount)
        val pb = dialogView.findViewById<ProgressBar>(R.id.pbCountdown)

        tvNextName?.text = list[currentPosition + 1].channelName
        countdownTime = 5
        pb?.max = 5

        val countdownRunnable: Runnable = object : Runnable {
            override fun run() {
                if (countdownTime > 0) {
                    tvTimer?.text = countdownTime.toString()
                    pb?.progress = countdownTime
                    countdownTime--
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    nextAutoDialog?.dismiss()
                    playNext()
                }
            }
        }
        countdownHandler.post(countdownRunnable)

        dialogView.findViewById<View>(R.id.btnPlayNow)?.setOnClickListener {
            countdownHandler.removeCallbacksAndMessages(null)
            nextAutoDialog?.dismiss()
            playNext()
        }

        dialogView.findViewById<View>(R.id.btnCancelNext)?.setOnClickListener {
            countdownHandler.removeCallbacksAndMessages(null)
            nextAutoDialog?.dismiss()
        }
    }

    private fun playNext() {
        if (player != null && player!!.hasNextMediaItem()) {
            player?.seekToNext()
            player?.prepare()
            player?.play()
        } else {
            Toast.makeText(this, getString(R.string.playlist_ended), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    // ✅ ANR FIX: Safe ExoPlayer release
    override fun onDestroy() {
        super.onDestroy()
        countdownHandler.removeCallbacksAndMessages(null)
        ratingHandler.removeCallbacksAndMessages(null)
        nextAutoDialog?.dismiss()
        player?.let {
            it.stop()
            it.clearMediaItems()
            it.release()
            player = null
        }
    }

    private fun openRateDialog() {
        val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
        val isExit = shared.getBoolean("exit", false)
        val isRatingDone = shared.getBoolean("rating_done", false)

        if (isExit || isRatingDone) {
            Toast.makeText(this, getString(R.string.thanks_for_rating), Toast.LENGTH_SHORT).show()
        } else {
            showRateDialogLogic(this)
        }
    }

    private fun showRateDialogLogic(activity: Activity) {
        RATE = 0
        val mainDialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.fragment_rate)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val imgStar1 = mainDialog.findViewById<ImageView>(R.id.img_start_1)
        val imgStar2 = mainDialog.findViewById<ImageView>(R.id.img_start_2)
        val imgStar3 = mainDialog.findViewById<ImageView>(R.id.img_start_3)
        val imgStar4 = mainDialog.findViewById<ImageView>(R.id.img_start_4)
        val imgStar5 = mainDialog.findViewById<ImageView>(R.id.img_start_5)
        val btnSubmit = mainDialog.findViewById<TextView>(R.id.btn_rate_yes)
        val btnLater = mainDialog.findViewById<TextView>(R.id.btn_rate_not)

        imgStar1?.setOnClickListener { updateStars(1, mainDialog) }
        imgStar2?.setOnClickListener { updateStars(2, mainDialog) }
        imgStar3?.setOnClickListener { updateStars(3, mainDialog) }
        imgStar4?.setOnClickListener { updateStars(4, mainDialog) }
        imgStar5?.setOnClickListener { updateStars(5, mainDialog) }
        btnLater?.setOnClickListener { mainDialog.dismiss() }

        btnSubmit?.setOnClickListener {
            if (RATE == 0) {
                Toast.makeText(activity, getString(R.string.please_select_stars), Toast.LENGTH_SHORT).show()
            } else {
                handleRating(RATE, activity, mainDialog)
            }
        }

        if (!isFinishing && !isDestroyed) {
            mainDialog.show()
        }
    }

    private fun handleRating(stars: Int, activity: Activity, mainDialog: Dialog) {
        RATE = stars
        mainDialog.dismiss()
        if (stars >= 4) {
            gotoPlayStore(activity)
        } else {
            showFeedbackDialog(activity)
        }
    }

    private fun showFeedbackDialog(activity: Activity) {
        val feedbackDialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.thank_you)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val btnSubmit = feedbackDialog.findViewById<TextView>(R.id.btn_rate_yes)
        val btnCancel = feedbackDialog.findViewById<TextView>(R.id.btn_rate_not)
        val etFeedback = feedbackDialog.findViewById<TextInputEditText>(R.id.editText)

        btnSubmit?.setOnClickListener {
            val feedback = etFeedback?.text.toString()
            if (feedback.isNotEmpty()) {
                activity.getSharedPreferences("MyPrefFileExit", Context.MODE_PRIVATE)
                    .edit().putBoolean("rating_done", true).apply()
                Toast.makeText(activity, getString(R.string.thanks_for_rating), Toast.LENGTH_SHORT).show()
                feedbackDialog.dismiss()
            } else {
                Toast.makeText(activity, getString(R.string.please_describe_your_thought), Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel?.setOnClickListener {
            activity.getSharedPreferences("MyPrefFileExit", Context.MODE_PRIVATE)
                .edit().putBoolean("rating_done", true).apply()
            feedbackDialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) {
            feedbackDialog.show()
        }
    }

    private fun updateStars(count: Int, dialog: Dialog) {
        RATE = count
        val starIds = intArrayOf(
            R.id.img_start_1,
            R.id.img_start_2,
            R.id.img_start_3,
            R.id.img_start_4,
            R.id.img_start_5
        )

        for (i in starIds.indices) {
            dialog.findViewById<ImageView>(starIds[i])?.let {
                if (i < count) {
                    it.setImageResource(R.drawable.select_star)
                    it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
                        it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    }.start()
                } else {
                    it.setImageResource(R.drawable.starpn)
                }
            }
        }
    }

    fun gotoPlayStore(activity: Activity) {
        activity.getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
            .edit().putBoolean("exit", true).apply()
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
        }
    }
}