package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ads.module.ads.ERainAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptv.online.smart.liveplayer.tv.Adapter.LanguageAdapter
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.Model.LanguageModel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.ReadFile.BOOKER_Manager
import com.iptv.online.smart.liveplayer.tv.ReadFile.PreferenceManager
import com.iptv.online.smart.liveplayer.tv.adsutils.LanguageAdVariant
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigUtils
import com.iptv.online.smart.liveplayer.tv.app.AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityLanguageBinding
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.itg.devconfig.utils.setOnAdminAdToggleListener
import java.util.Locale


class LanguageActivity : Base__Activity<ActivityLanguageBinding>(),
    LanguageAdapter.OnClickItemListener {
    private var languageAdapterNew: LanguageAdapter? = null
    private val languageModelNews: MutableList<LanguageModel?> = ArrayList<LanguageModel?>()
    private var global_Language: LanguageModel? = null
    private var preferenceManager: PreferenceManager? = null
    private var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null
    private var isClickAdPhase = false
    private val doneHandler = Handler(Looper.getMainLooper())
    private var languageVariant = LanguageAdVariant.FIRST
    private var shouldDelayDoneButton = true
    private var timeDelayDoneButton = DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON

    private val isFirstLanguage: Boolean
        get() = languageVariant == LanguageAdVariant.FIRST

    override val needBackInterAd: Boolean
        get() = intent.getBooleanExtra("settingss", false)

    companion object {
        private const val STATE_LANGUAGE_VARIANT = "state_language_variant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        languageVariant = LanguageAdVariant.fromIntentValue(
            savedInstanceState?.getString(STATE_LANGUAGE_VARIANT)
                ?: intent.getStringExtra(LanguageAdVariant.EXTRA),
            SplashActivity.isLanguageDone(this)
        )
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_LANGUAGE_VARIANT, LanguageAdVariant.intentValue(languageVariant))
        super.onSaveInstanceState(outState)
    }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(getLayoutInflater())
    }

    override fun bindObjects() {
        shouldDelayDoneButton = RemoteConfigUtils.shouldDelayLanguageDoneButton()
        timeDelayDoneButton = RemoteConfigUtils.getTimeDelayButtonDoneLanguage()
        sp = getSharedPreferences(getPackageName(), 0)
        sp_editor = sp!!.edit()
        val colors = intArrayOf(
            Color.parseColor("#51CBFF"),
            Color.parseColor("#45AFFF"),
            Color.parseColor("#2663FF")
        )


        binding.gradinttext.post {
            val paint = binding.gradinttext.paint
            paint.isAntiAlias = true

            val textShader: Shader = LinearGradient(
                0f,
                0f,
                binding.gradinttext.paint.measureText(binding.gradinttext.text.toString()),
                binding.gradinttext.textSize,
                colors,
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = textShader
            binding.gradinttext.invalidate()
        }

        val restartSplash = {
            val i = Intent(this, SplashActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
        }
        binding.textheader.setOnAdminAdToggleListener { restartSplash() }

        val isFromSettings = intent.getBooleanExtra("settingss", false)

        if (isFromSettings) {
            binding.back.visibility = View.VISIBLE
            binding.textheader.text = getString(R.string.set_language)
            binding.done.visibility = View.VISIBLE

            binding.adShimmer.root.visibility = View.GONE
            binding.frAds.visibility = View.GONE

        } else {
            binding.back.visibility = View.GONE
            binding.textheader.text = getString(R.string.choose_your_language)
            binding.done.visibility = View.GONE
            binding.gradinttext.visibility = View.GONE
        }


        preferenceManager = PreferenceManager(this)
        BOOKER_Manager.initializingSharedPreference(this)
        addList()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadSavedLanguage()
        setAdapter()

        if (!isFromSettings) {
            setupSwipeHint()
            initAds()
        }

    }

    private fun setupSwipeHint() {
        if (!isFirstLanguage) return

        binding.swipeHint.visibility = View.VISIBLE
        binding.swipeHint.playAnimation()

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!rv.canScrollVertically(1)) {
                    hideSwipeHint()
                    rv.removeOnScrollListener(this)
                }
            }
        })

        binding.recyclerView.post {
            if (!binding.recyclerView.canScrollVertically(1)) hideSwipeHint()
        }
    }

    private fun hideSwipeHint() {
        if (binding.swipeHint.visibility != View.VISIBLE) return
        binding.swipeHint.cancelAnimation()
        binding.swipeHint.visibility = View.GONE
    }

    private fun initAds() {
        adjustLangShimmerHeight()
        binding.root.postDelayed({
            if (!isFinishing && !isDestroyed) {
                AdsManager.loadNativeLanguageClick(
                    this@LanguageActivity,
                    isFirstLanguage,
                    if (isFirstLanguage) R.layout.layout_native_ad_lang_click else R.layout.layout_native_ad_lang_click_2
                )
                AdsManager.loadNativeOnboarding1(
                    this@LanguageActivity,
                    !isIntroFlowDone(),
                    R.layout.layout_native_ad_large
                )
            }
        }, 100)
    }

    override fun bindObserver() {
        if (!intent.getBooleanExtra("settingss", false)) listenLanguageAd()
    }

    private fun listenLanguageAd() {
        AdsManager.nativeLanguageClickAdLive.removeObservers(this)
        AdsManager.nativeLanguageAdLive.observe(this) { ad ->
            renderLanguageAd(ad)
        }
    }

    private fun listenLanguageClickAd() {
        AdsManager.nativeLanguageAdLive.removeObservers(this)
        AdsManager.nativeLanguageClickAdLive.observe(this) { ad ->
            renderLanguageAd(ad)
        }
    }

    private fun adjustLangShimmerHeight() {
        val d = resources.displayMetrics.density
        val large = !isFirstLanguage
        fun dp(v: Int) = (v * d).toInt()
        val root = binding.adShimmer.root

        root.findViewById<View>(R.id.shimmer_ad_media)?.let { media ->
            val lp = media.layoutParams as ViewGroup.MarginLayoutParams
            lp.height = dp(if (large) 130 else 80)
            lp.topMargin = dp(if (large) 8 else 6)
            media.layoutParams = lp
        }
        root.findViewById<View>(R.id.shimmer_ad_call_to_action)?.let { cta ->
            val lp = cta.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = dp(if (large) 16 else 8)
            cta.layoutParams = lp
        }
        root.findViewById<View>(R.id.shimmer_card)?.let { card ->
            val lp = card.layoutParams as ViewGroup.MarginLayoutParams
            val side = dp(if (large) 8 else 0)
            lp.setMargins(side, dp(if (large) 8 else 2), side, if (large) dp(8) else 0)
            card.layoutParams = lp
        }
    }

    private fun renderLanguageAd(ad: com.ads.module.ads.wrapper.ApNativeAd?) {
        if (ad == null) {
            binding.adShimmer.root.visibility = View.GONE
            binding.frAds.visibility = View.GONE
            return
        }

        binding.frAds.removeAllViews()
        binding.frAds.visibility = View.VISIBLE
        ERainAd.getInstance().populateNativeAdView(
            this,
            ad,
            binding.frAds,
            binding.adShimmer.root
        )
        binding.adShimmer.root.visibility = View.GONE

        binding.frAds.findViewById<View>(R.id.ad_call_to_action)?.let { button ->
            button.layoutParams?.let { params ->
                params.height = (AdsManager.getCTAButtonHeight(this) * resources.displayMetrics.density).toInt()
                button.layoutParams = params
            }
        }
    }

    public override fun bindListener() {
        binding.back.setOnClickListener({ v -> onBackPressed() })

        val restartSplash = {
            val i = Intent(this, SplashActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
        }
        binding.textheader.setOnAdminAdToggleListener { restartSplash() }

        binding.done.setOnClickListener({ view ->
            if (global_Language != null) {
                saveAndApplyLanguage(global_Language!!)
            } else {
                saveAndApplyLanguage(LanguageModel("en", "English", true))
            }
        })


    }

    public override fun bindMethod() {
    }

    private fun loadSavedLanguage() {
        val json = sp!!.getString("lang", null)
        if (json != null) {
            val type = object : TypeToken<ArrayList<LanguageModel?>?>() {
            }.getType()
            val list = Gson().fromJson<MutableList<LanguageModel?>?>(json, type)
            if (list != null && !list.isEmpty()) {
                global_Language = list.get(0)
            }
        }
    }

    private fun saveAndApplyLanguage(model: LanguageModel) {
        sp_editor!!.putString("selected_language_code", model.s_lan_code)
        sp_editor!!.putString("selected_language_name", model.s_lan_name)
        val success = sp_editor!!.commit()
        if (success) {
            updateLocale(model.s_lan_code)
            BOOKER_Manager.setLanguageSelected(true)

            val isFromSettings = intent.getBooleanExtra("settingss", false)

            if (isFromSettings) {


                val mainIntent = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivities(arrayOf(mainIntent, settingsIntent))
                finish()

            } else {

                SplashActivity.markLanguageDone(this)
                val intent = if (RemoteConfigdata(this).isIntroPage)
                    Intent(this, IntroActivity::class.java)
                else
                    Intent(this, ActivityNotice::class.java).putExtra("isFromIntro", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
        }


    }


    private fun updateLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun addList() {
        languageModelNews.clear()
        languageModelNews.add(LanguageModel("es", "Spanish", false))
        languageModelNews.add(LanguageModel("fr", "French", false))
        languageModelNews.add(LanguageModel("pt", "Portuguese", false))
        languageModelNews.add(LanguageModel("ko", "South Korea", false))
        languageModelNews.add(LanguageModel("ja", "Japan", false))
        languageModelNews.add(LanguageModel("en", "English", false))
        languageModelNews.add(LanguageModel("hi", "India", false))
        languageModelNews.add(LanguageModel("id", "Indonesia", false))
        languageModelNews.add(LanguageModel("tr", "Turkiye", false))
        languageModelNews.add(LanguageModel("ar", "Saudi Arabia", false))
        languageModelNews.add(LanguageModel("uz", "Uzbekistan", false))
        languageModelNews.add(LanguageModel("fil", "Philippines", false))
        languageModelNews.add(LanguageModel("de", "Germany", false))
        languageModelNews.add(LanguageModel("vi", "Vietnam", false))
        languageModelNews.add(LanguageModel("sv", "Swedish", false))
    }


    private fun setAdapter() {
        val currentLang = sp!!.getString("selected_language_name", "")
        val isFromSettings = intent.getBooleanExtra("settingss", false)


        languageAdapterNew =
            LanguageAdapter(languageModelNews, this, currentLang, isFromSettings, this)
        binding.recyclerView.adapter = languageAdapterNew

        if (!currentLang.isNullOrEmpty()) {
            for (model in languageModelNews) {
                if (model?.s_lan_name == currentLang) {
                    global_Language = model
                    break
                }
            }
        } else {
            global_Language = null
        }
    }

    override fun onClickItem(languageModel_: LanguageModel?) {
        global_Language = languageModel_
        val isFromSettings = intent.getBooleanExtra("settingss", false)

        sp_editor!!.putBoolean("is_lang_btn_visible", true).apply()

        if (isFromSettings) {
                      binding.done.visibility = View.VISIBLE
        } else {
            if (!isClickAdPhase) {
                isClickAdPhase = true
                AdsManager.nativeLanguageAdLive.removeObservers(this)
                binding.frAds.removeAllViews()
                binding.frAds.visibility = View.GONE
                adjustLangShimmerHeight()
                // The shimmer remains while the preloaded click native resolves.
                binding.adShimmer.root.visibility = View.VISIBLE
                listenLanguageClickAd()
            }
            showDoneButton()
        }
    }

    private fun showDoneButton() {
        if (shouldDelayDoneButton) {
            binding.done.visibility = View.INVISIBLE
            doneHandler.removeCallbacksAndMessages(null)
            doneHandler.postDelayed({
                binding.done.visibility = View.VISIBLE
            }, timeDelayDoneButton)
        } else {
            binding.done.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        doneHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (getIntent().getBooleanExtra("settingss", false)) {
            super.onBackPressed()
        } else {
            finishAffinity()
        }
    }
}
