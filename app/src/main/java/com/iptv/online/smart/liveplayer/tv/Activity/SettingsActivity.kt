package com.iptv.online.smart.liveplayer.tv.Activity

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivitySettingsBinding
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : Base__Activity<ActivitySettingsBinding>() {
    private var RATE = 0
    private var currentLang = ""
    private var configScript: RemoteConfigdata? = null

    public override fun setViewBinding(): ActivitySettingsBinding {
        return ActivitySettingsBinding.inflate(getLayoutInflater())
    }

    public override fun bindObjects() {
        val pref = getSharedPreferences(getPackageName(), MODE_PRIVATE)
        currentLang = pref.getString("selected_language_code", "en")!!
        updateLanguageUI()
        configScript = RemoteConfigdata(this@SettingsActivity)

    }

    public override fun bindListener() {
        binding.back.setOnClickListener({ v -> onBackPressed() })


        binding.cardLanguage.setOnClickListener({ v ->
            val intent = Intent(this, Language_Activity::class.java)
            intent.putExtra("settingss", true)
            startActivity(intent)
        })


        binding.llRate.setOnClickListener({ v -> openRateDialog() })


        binding.llShare.setOnClickListener({ v -> shareapp(this) })

        binding.llPrivacy.setOnClickListener { v ->
            val urlString = configScript?.privacyLink

            if (!urlString.isNullOrBlank() && urlString.startsWith("http")) {
                // Pass the Uri directly, NO casting to String
                openChromeCustomTabUrl(this, urlString)
            } else {
                Toast.makeText(this, R.string.notfound, Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun bindMethod() {
        // વધારાની મેથડ્સ
    }

    public override fun onBackPressed() {
        if (isTaskRoot()) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private fun openRateDialog() {
        val shared = getSharedPreferences("MyPrefFileExit", MODE_PRIVATE)
        val isRatingDone = shared.getBoolean("rating_done", false)

        if (isRatingDone) {
            // જો રેટિંગ કાયમી થઈ ગયું હોય તો ડાયલોગ ખોલવાની જરૂર નથી
            Toast.makeText(this, getString(R.string.thanks_for_rating), Toast.LENGTH_SHORT).show()
        } else {
            // *** મુખ્ય ફેરફાર અહીં છે ***
            // યુઝરે સેટિંગ્સમાં ક્લિક કર્યું એટલે આખા સેશન માટે ફ્લેગ TRUE કરી દો
            // જેથી હવે Player કે MainActivity માં ઓટોમેટિક ડાયલોગ નહીં આવે
            MainActivity.isRatingShownInSession = true

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

    fun shareapp(context: Context) {
        try {
            val bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher)
            val f = File(context.getExternalCacheDir().toString() + "/image.png")
            val outStream = FileOutputStream(f)
            bm.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.close()

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("image/*")
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Check out this app: https://play.google.com/store/apps/details?id=" + context.getPackageName()
            )

            val urishare =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    f
                ) else Uri.fromFile(f)

            shareIntent.putExtra(Intent.EXTRA_STREAM, urishare)
            context.startActivity(Intent.createChooser(shareIntent, "Share App via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openChromeCustomTabUrl(context: Context, webUrl: String?) {
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(Color.parseColor("#ffffff"))
            val customTabsIntent = builder.build()
            customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(webUrl))
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            } catch (ex: Exception) {
                Toast.makeText(context, R.string.error_url, Toast.LENGTH_SHORT).show()
            }
        }
    }

    protected override fun onResume() {
        super.onResume()
        val pref = getSharedPreferences(getPackageName(), MODE_PRIVATE)
        val newLang: String = pref.getString("selected_language_code", "en")!!

        // જો ભાષા બદલાઈ હોય, તો recreate કરો જેથી BaseActivity નું attachBaseContext ફરીથી રન થાય
        if (currentLang != newLang) {
            currentLang = newLang
            recreate()
        } else {
            updateLanguageUI()
        }
    }

    private fun updateLanguageUI() {
        val pref = getSharedPreferences(getPackageName(), MODE_PRIVATE)
        val selectedLanguage: String = pref.getString("selected_language_name", "English")!!
        if (binding.txtCurrentLanguage != null) {
            binding.txtCurrentLanguage.setText(selectedLanguage)
        }
    }
}