package com.iptv.online.smart.liveplayer.tv.Activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.iptv.online.smart.liveplayer.tv.Adapter.MyViewPagerAdapter
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager.loadAndShowCollapsingBanner
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId

import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityMainBinding

class MainActivity : Base__Activity<ActivityMainBinding>() {
    private var RATE = 0
    private var isLaterClicked = false
    private val NOTIFICATION_PERMISSION_CODE = 101

    companion object {
        var isRatingShownInSession = false
        var triggerFromMirroring = false
        var triggerFromPlaylist = false
    }

    private val ratingHandler = Handler(Looper.getMainLooper())

    private var configScript: RemoteConfigdata? = null
    public override fun setViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(getLayoutInflater())
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_required))
                    .setMessage(getString(R.string.notification_permission_is_required))
                    .setPositiveButton(getString(R.string.allow)) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_CODE
                        )
                    }
                    .setNegativeButton(getString(R.string.deny), null)
                    .show()

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                Toast.makeText(this, "Notification Permission Granted", Toast.LENGTH_SHORT).show()

            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntent(intent)
        handleRatingTrigger()
    }

    public override fun bindObjects() {
        configScript = RemoteConfigdata(this)

        if (configScript!!.isNeedToShowADs) {
            if (configScript!!.bannerCollapsehomeOn) {
                loadAndShowCollapsingBanner(binding.bannerAdLayout)
            } else {
                binding.bannerAdLayout.setVisibility(View.GONE)
            }
        } else {
            binding.bannerAdLayout.setVisibility(View.GONE)
        }

        // Demo jevu CENTRALIZED: interAddPlaylist ne AdsManager thi load.
        AdsManager.loadInterAddPlaylist(this)
        binding.viewPager.setAdapter(MyViewPagerAdapter(this))
        binding.viewPager.setOffscreenPageLimit(3)

        checkIntent(getIntent())

        if (!checkNotificationPermission()) {
            requestNotificationPermission()
        }
        val action = intent.getStringExtra("ACTION_TYPE")
        if (action == "OPEN_IPTV") {
            binding!!.viewPager.post {
                binding!!.viewPager.setCurrentItem(1, true)
                updateNavUI(1)
            }
        }


        //preload
        if (configScript!!.nativechannel2005) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.nativechannel2005,
                R.layout.layout_native_ad_medium_channel,
                "native_channel_2005"
            )
        }
        if (configScript!!.nativeMirroring) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.NATIVE_MIRRORING,
                R.layout.layout_native_ad_large,
                "native_mirroring"
            )
        }

        if (configScript!!.nativeChannelList) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.NATIVE_CHANNELLIST,
                R.layout.layout_native_ad_large,
                "native_channel_list"
            )
        }
        if (configScript!!.nativePlaylist) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.NATIVE_PLAYLIST,
                R.layout.layout_native_ad_large,
                "native_playlist"
            )
        }



        if (configScript!!.nativefavorite2005) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.nativefavorite2005,
                R.layout.layout_native_ad_small,
                "native_favorite_2005"
            )
        }
        if (configScript!!.nativehistory2005) {
            AdsManager.loadAd(
                this@MainActivity,
                AdsId.nativehistory2005,
                R.layout.layout_native_ad_small,
                "native_history_2005"
            )
        }
    }

    public override fun bindListener() {
        binding.navHome.setOnClickListener({ v -> binding.viewPager.setCurrentItem(0) })
        binding.navChannel.setOnClickListener({ v -> binding.viewPager.setCurrentItem(1) })
        binding.navFav.setOnClickListener({ v -> binding.viewPager.setCurrentItem(2) })
        binding.navHistory.setOnClickListener({ v -> binding.viewPager.setCurrentItem(3) })
        binding.fabAdd.setOnClickListener({ v ->


            AdsManager.showInterAddPlaylist(this) {
                val intent = Intent(this@MainActivity, FileSelectActivity::class.java)
                startActivity(intent)
            }


        })

        // ViewPager Page Change Callback
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavUI(position)
            }
        })
    }

    public override fun bindMethod() {
        updateNavUI(0)
    }


    private fun checkIntent(intent: Intent?) {
        if (intent != null && intent.hasExtra("target_fragment")) {
            val target = intent.getStringExtra("target_fragment")
            if (target != null) {
                when (target) {
                    "history" -> binding.viewPager.setCurrentItem(3, false)
                    "favorite" -> binding.viewPager.setCurrentItem(2, false)
                    "channel" -> binding.viewPager.setCurrentItem(1, false)
                }
            }
        }
    }

    fun updateNavUI(position: Int) {
        binding.imgHome.setColorFilter(Color.GRAY)
        binding.txtHome.setTextColor(Color.GRAY)
        binding.imgChannel.setColorFilter(Color.GRAY)
        binding.txtChannel.setTextColor(Color.GRAY)
        binding.imgFav.setColorFilter(Color.GRAY)
        binding.txtFav.setTextColor(Color.GRAY)
        binding.imgHistory.setColorFilter(Color.GRAY)
        binding.txtHistory.setTextColor(Color.GRAY)

        val activeColor = getResources().getColor(R.color.HoverColor)

        if (position == 0) {
            binding.imgHome.setColorFilter(activeColor)
            binding.txtHome.setTextColor(activeColor)
        } else if (position == 1) {
            binding.imgChannel.setColorFilter(activeColor)
            binding.txtChannel.setTextColor(activeColor)
        } else if (position == 2) {
            binding.imgFav.setColorFilter(activeColor)
            binding.txtFav.setTextColor(activeColor)
        } else if (position == 3) {
            binding.imgHistory.setColorFilter(activeColor)
            binding.txtHistory.setTextColor(activeColor)
        }
    }


    override fun onBackPressed() {
        if (binding.viewPager.getCurrentItem() !== 0) {
            binding.viewPager.setCurrentItem(0)
        } else {
//            openRateDialog()
            exitDialogStatus(this)

        }
    }

    val viewPager: ViewPager2
        get() = binding.viewPager

    private fun openRateDialog() {
        val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
        val isExit = shared.getBoolean("exit", false)
        val isRatingDone = shared.getBoolean("rating_done", false)

        if (isExit || isLaterClicked || isRatingDone) {
            exitDialogStatus(this)
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
            isLaterClicked = true
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

    fun exitDialogStatus(activity: Activity) {
        val dialog = Dialog(activity, R.style.CustomDialog)
        dialog.setContentView(R.layout.exit_dialog)

        if (dialog.getWindow() != null) {
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.findViewById<View?>(R.id.btn_exit_yes)
            .setOnClickListener(View.OnClickListener { v: View? ->
                dialog.dismiss()
                activity.finishAffinity()
            })

        dialog.findViewById<View?>(R.id.btn_exit_not)
            .setOnClickListener(View.OnClickListener { v: View? -> dialog.dismiss() })

        if (!activity.isFinishing()) dialog.show()
    }

    fun gotoPlayStore(activity: Activity) {
        val shared = activity.getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
        shared.edit().putBoolean("exit", true).apply()
        shared.edit().putBoolean("rating_done", true).apply()

        val packageName = activity.getPackageName()
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
                )
            )
        }
    }

    protected override fun onResume() {
        super.onResume()
        binding.viewPager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
        handleRatingTrigger()
    }

    private fun handleRatingTrigger() {
        if (isRatingShownInSession) {
            triggerFromMirroring = false
            triggerFromPlaylist = false
            return
        }

        if (triggerFromMirroring || triggerFromPlaylist) {

            Log.d("RatingCheck", "Trigger detected. Session status: $isRatingShownInSession")

            triggerFromMirroring = false
            triggerFromPlaylist = false

            ratingHandler.postDelayed({
                val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
                val isRatingDone = shared.getBoolean("rating_done", false)


                if (!isRatingDone && !isRatingShownInSession) {
                    openRateDialog()


                    isRatingShownInSession = true
                    Log.d("RatingCheck", "Rating Dialog Shown and Session Flag Set to True")
                }
            }, 2000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ratingHandler.removeCallbacksAndMessages(null)
    }
}