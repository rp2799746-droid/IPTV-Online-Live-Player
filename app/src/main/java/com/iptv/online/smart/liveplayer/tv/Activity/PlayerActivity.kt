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
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.textfield.TextInputEditText
import com.iptv.online.smart.liveplayer.tv.Activity.MainActivity.Companion.isRatingShownInSession
import com.iptv.online.smart.liveplayer.tv.Adapter.ChannelAdapter
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.Model.Channel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityPlayerBinding

class PlayerActivity : Base__Activity<ActivityPlayerBinding>() {
    private val ratingHandler = Handler()
    private var player: ExoPlayer? = null
    private var channelList: ArrayList<Channel>? = null
    private var currentPosition: Int = 0
    private val countdownHandler = Handler()
    private var countdownTime = 5
    private var nextAutoDialog: AlertDialog? = null
    private var upNextAdapter: ChannelAdapter? = null
    private var RATE = 0


    public override fun onBackPressed() {
        super.onBackPressed()
    }

    public override fun setViewBinding(): ActivityPlayerBinding {
        // Intent ડેટા મેળવો
        return ActivityPlayerBinding.inflate(getLayoutInflater())
    }

    public override fun bindObjects() {
        channelList = getIntent().getParcelableArrayListExtra<Channel?>("channel_list")
        currentPosition = getIntent().getIntExtra("position", 0)

        // પ્લેયર અને લિસ્ટ સેટઅપ
        initPlayer()
        if (channelList != null && !channelList!!.isEmpty()) {
            setupUpNextList()
        }
    }

    public override fun bindListener() {
        // બેક બટન
        binding.back.setOnClickListener({ v -> onBackPressed() })

        binding.History.setOnClickListener({ v ->
            if (channelList != null && !channelList!!.isEmpty()) {
                addToHistory(channelList!!.get(currentPosition))
            }
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra("target_fragment", "history")
            startActivity(intent)
            finish()
        })

        binding.Favorite.setOnClickListener({ v ->
            if (channelList != null && !channelList!!.isEmpty()) {
                toggleFavorite(channelList!!.get(currentPosition))
            }
        })

        // શેર બટન
        binding.Share.setOnClickListener({ v -> shareCurrentChannel() })
    }

    public override fun bindMethod() {
        if (channelList != null && !channelList!!.isEmpty()) {
            playChannel(currentPosition)
            checkAndShowRatingAfterDelay()
        }
    }

    private fun checkAndShowRatingAfterDelay() {
        ratingHandler.postDelayed({
            // પેહલા ચેક કરો કે એપ હજી ચાલુ છે કે નહીં (Finish તો નથી થઈ ગઈ ને?)
            if (isFinishing || isDestroyed) return@postDelayed

            // ૧. ચેક કરો કે આ સેશનમાં ડાયલોગ દેખાઈ ગયો છે?
            if (!MainActivity.isRatingShownInSession) {

                val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
                val isRatingDone = shared.getBoolean("rating_done", false)

                // ૨. ચેક કરો કે યુઝરે કાયમી રેટિંગ (Play Store/Feedback) આપી દીધું છે?
                if (!isRatingDone) {
                    // રેટિંગ બતાવતા પહેલા ફ્લેગ TRUE કરો જેથી બીજા ટ્રિગર્સ કામ ના કરે
                    MainActivity.isRatingShownInSession = true
                    openRateDialog()
                    Log.d("RatingCheck", "Rating shown in Player Activity")
                }
            }
        }, 10000) // ૧૦ સેકન્ડ પછી
    }

    fun getCurrentPosition(): Int {
        return currentPosition
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.setPlayer(player)

        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                binding.playerProgressBar.setVisibility(if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE)
                if (state == Player.STATE_ENDED) {
                    showCustomNextDialog()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentPosition = player!!.getCurrentMediaItemIndex()
                updateUI(currentPosition)
            }

            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.link_error_skipping_to_next),
                    Toast.LENGTH_SHORT
                ).show()
                Handler().postDelayed(Runnable {
                    if (player != null) {
                        if (player!!.hasNextMediaItem()) playNext()
                        else finish()
                    }
                }, 2000)
            }
        })
    }

    private fun setupUpNextList() {
        for (channel in channelList!!) {
            val primaryKey = channel.getChannelUrl() + "|" + channel.getPlaylistName()
            val dbData = AppDatabase.getInstance(this).historyDao().getChannelById(primaryKey)
            if (dbData != null) {
                channel.setFavorite(dbData.isFavorite())
            }
        }

        upNextAdapter = ChannelAdapter(this, channelList)
        upNextAdapter!!.setOnChannelClickListener(ChannelAdapter.OnChannelClickListener { position: Int ->
            if (player != null) {
                player!!.seekTo(position, 0)
                player!!.prepare()
                player!!.play()
                updateUI(position)
            }
        })

        binding.rvUpNext.setLayoutManager(LinearLayoutManager(this))
        binding.rvUpNext.setAdapter(upNextAdapter)
        binding.rvUpNext.scrollToPosition(currentPosition)
    }

    private fun playChannel(position: Int) {
        if (channelList == null || position < 0 || position >= channelList!!.size) return
        currentPosition = position

        val items: MutableList<MediaItem?> = ArrayList<MediaItem?>()
        for (c in channelList) {
            items.add(MediaItem.fromUri(c.getChannelUrl()))
        }

        player!!.setMediaItems(items as List<MediaItem>, position, 0)
        player!!.prepare()
        player!!.play()
        updateUI(position)
    }

    private fun updateUI(position: Int) {
        if (channelList == null || position < 0 || position >= channelList!!.size) return

        val channel = channelList!!.get(position)
        binding.tvPlayerChannelName.setText(channel.getChannelName())
        binding.tvPlaylistInfo.setText(
            getString(R.string.playing) + " " + (position + 1) + " " + getString(
                R.string.of
            ) + " " + channelList!!.size
        )

        val primaryKey = channel.getChannelUrl() + "|" + channel.getPlaylistName()
        val dbData = AppDatabase.getInstance(this).historyDao().getChannelById(primaryKey)

        val favStatus = (dbData != null && dbData.isFavorite())
        channel.setFavorite(favStatus)
        updateFavoriteIcon(favStatus)

        addToHistory(channel)

        if (upNextAdapter != null) {
            upNextAdapter!!.notifyDataSetChanged()
            binding.rvUpNext.smoothScrollToPosition(position)
        }
    }

    private fun toggleFavorite(channel: Channel) {
        val primaryKey = channel.getChannelUrl() + "|" + channel.getPlaylistName()

        var dbData = AppDatabase.getInstance(this).historyDao().getChannelById(primaryKey)

        val newFavoriteStatus: Boolean
        if (dbData != null) {
            newFavoriteStatus = !dbData.isFavorite()
            dbData.setFavorite(newFavoriteStatus)
            dbData.setHistoryTimestamp(System.currentTimeMillis())
        } else {
            newFavoriteStatus = true
            channel.setId(primaryKey)
            channel.setFavorite(true)
            channel.setHistoryTimestamp(System.currentTimeMillis())
            dbData = channel
        }


        AppDatabase.getInstance(this).historyDao().insertHistory(dbData)


        channel.setFavorite(newFavoriteStatus)
        updateFavoriteIcon(newFavoriteStatus)

        Toast.makeText(
            this,
            if (newFavoriteStatus) getString(R.string.added_to_favorites) else getString(R.string.removed_from_favorites),
            Toast.LENGTH_SHORT
        ).show()

        if (upNextAdapter != null) upNextAdapter!!.notifyDataSetChanged()
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.Favorite.setImageResource(R.drawable.ic_fav_on)
        } else {
            binding.Favorite.setImageResource(R.drawable.ic_fav_off)
        }
    }

    private fun addToHistory(channel: Channel?) {
        if (channel == null || channel.getChannelUrl() == null) return

        // Timestamp સેટ કરો
        channel.setHistoryTimestamp(System.currentTimeMillis())

        // Room માં સેવ કરો - આ ડેટા ક્યારેય ગાયબ નહીં થાય
        AppDatabase.getInstance(this).historyDao().insertHistory(channel)

        Log.d("ROOM_SAVE", "History saved for: " + channel.getChannelName())
    }

    private fun shareCurrentChannel() {
        if (channelList != null && !channelList!!.isEmpty()) {
            val currentChannel = channelList!!.get(currentPosition)
            val shareMessage = getString(R.string.check_out_this_iptv_online_live_player) +
                    "\n" + getString(R.string.channel1) + " " + currentChannel.getChannelName() +
                    "\n" + getString(R.string.stream_link) + " " + currentChannel.getChannelUrl()

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "Share Channel"))
        }
    }

    private fun showCustomNextDialog() {
        if (channelList == null || currentPosition >= channelList!!.size - 1) return

        val dialogView = getLayoutInflater().inflate(R.layout.layout_next_channel, null)
        nextAutoDialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        if (nextAutoDialog!!.getWindow() != null) nextAutoDialog!!.getWindow()!!
            .setBackgroundDrawableResource(android.R.color.transparent)
        nextAutoDialog!!.show()

        val tvNextName = dialogView.findViewById<TextView?>(R.id.tvNextChannelName)
        val tvTimer = dialogView.findViewById<TextView?>(R.id.tvTimerCount)
        val pb = dialogView.findViewById<ProgressBar?>(R.id.pbCountdown)

        tvNextName.setText(channelList!!.get(currentPosition + 1).getChannelName())
        countdownTime = 5
        pb.setMax(5)

        val countdownRunnable: Runnable = object : Runnable {
            override fun run() {
                if (countdownTime > 0) {
                    tvTimer.setText(countdownTime.toString())
                    pb.setProgress(countdownTime)
                    countdownTime--
                    countdownHandler.postDelayed(this, 1000)
                } else {
                    nextAutoDialog!!.dismiss()
                    playNext()
                }
            }
        }
        countdownHandler.post(countdownRunnable)

        dialogView.findViewById<View?>(R.id.btnPlayNow)
            .setOnClickListener(View.OnClickListener { v: View? ->
                countdownHandler.removeCallbacksAndMessages(null)
                nextAutoDialog!!.dismiss()
                playNext()
            })

        dialogView.findViewById<View?>(R.id.btnCancelNext)
            .setOnClickListener(View.OnClickListener { v: View? ->
                countdownHandler.removeCallbacksAndMessages(null)
                nextAutoDialog!!.dismiss()
            })
    }

    private fun playNext() {
        if (player != null && player!!.hasNextMediaItem()) {
            player!!.seekToNext()
            player!!.prepare()
            player!!.play()
        } else {
            Toast.makeText(this, getString(R.string.playlist_ended), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        if (player != null) player!!.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownHandler.removeCallbacksAndMessages(null)
        ratingHandler.removeCallbacksAndMessages(null)
        if (player != null) {
            player!!.release()
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
        val mainDialog = Dialog(activity)
        mainDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mainDialog.setContentView(R.layout.fragment_rate)

        if (mainDialog.window != null) {
            mainDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            mainDialog.window!!.setLayout(
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
        val btnLater = mainDialog.findViewById<TextView?>(R.id.btn_rate_not)

        imgStar1?.setOnClickListener { updateStars(1, mainDialog) }
        imgStar2?.setOnClickListener { updateStars(2, mainDialog) }
        imgStar3?.setOnClickListener { updateStars(3, mainDialog) }
        imgStar4?.setOnClickListener { updateStars(4, mainDialog) }
        imgStar5?.setOnClickListener { updateStars(5, mainDialog) }
        btnLater.setOnClickListener(View.OnClickListener { v: View? ->
            mainDialog.dismiss()
        })
        btnSubmit?.setOnClickListener {
            if (RATE == 0) {
                Toast.makeText(
                    activity,
                    getString(R.string.please_select_stars),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                handleRating(RATE, activity, mainDialog)
            }
        }

        mainDialog.show()
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
        val feedbackDialog = Dialog(activity)
        feedbackDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        feedbackDialog.setContentView(R.layout.thank_you)

        if (feedbackDialog.window != null) {
            feedbackDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            feedbackDialog.window!!.setLayout(
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
                val shared = activity.getSharedPreferences("MyPrefFileExit", Context.MODE_PRIVATE)
                shared.edit().putBoolean("rating_done", true).apply()

                Toast.makeText(activity, getString(R.string.thanks_for_rating), Toast.LENGTH_SHORT)
                    .show()
                feedbackDialog.dismiss()
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.please_describe_your_thought),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnCancel?.setOnClickListener {
            val shared = activity.getSharedPreferences("MyPrefFileExit", Context.MODE_PRIVATE)
            shared.edit().putBoolean("rating_done", true).apply()
            feedbackDialog.dismiss()


        }


        feedbackDialog.show()
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
            val star = dialog.findViewById<ImageView>(starIds[i])
            star?.let {
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
        val shared = activity.getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
        shared.edit().putBoolean("exit", true).apply()
        val packageName = activity.getPackageName()
        try {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)
                )
            )
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
                )
            )
        }
    }


}