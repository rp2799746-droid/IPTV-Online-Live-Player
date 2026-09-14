package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ads.module.ads.ERainAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity.Companion.screenCount
import com.iptv.online.smart.liveplayer.tv.Adapter.LanguageAdapter
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.Model.LanguageModel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.ReadFile.BOOKER_Manager
import com.iptv.online.smart.liveplayer.tv.ReadFile.PreferenceManager
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
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
    private var configScript: RemoteConfigdata? = null

    private var isClickAdPhase = false

    private val doneHandler = Handler(Looper.getMainLooper())

    private val adHandler = Handler(Looper.getMainLooper())

    override val needBackInterAd: Boolean
        get() = intent.getBooleanExtra("settingss", false)

    companion object {
        private const val TAG_LANG = "native_lang_tag"
        private const val TAG_LANG_CLICK = "native_lang_click_tag"


        private const val AD_LOG = "LangAd"


        private const val SHIMMER_TIMEOUT_MS = 10_000L
    }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(getLayoutInflater())
    }

    override fun bindObjects() {
        if (intent.hasExtra("screen_count")) {
            screenCount = intent.getIntExtra("screen_count", 1)
        }

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

        configScript = RemoteConfigdata(this@LanguageActivity)

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

            loadLanguageAd()


        }


        preferenceManager = PreferenceManager(this)
        BOOKER_Manager.initializingSharedPreference(this)
        addList()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadSavedLanguage()
        setAdapter()

        if (!isFromSettings) setupSwipeHint()

    }

    private fun setupSwipeHint() {
        if (screenCount != 1) return

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

    private fun startShimmerTimeout(tag: String) {
        adHandler.removeCallbacksAndMessages(null)
        adHandler.postDelayed({
            if (binding.frAds.visibility != View.VISIBLE) {
                Log.d(AD_LOG, "[$tag] ${SHIMMER_TIMEOUT_MS}ms ma ad na aavyo -> ad slot band")
                binding.adShimmer.root.visibility = View.GONE
                binding.frAds.visibility = View.GONE
            }
        }, SHIMMER_TIMEOUT_MS)
    }


    private fun loadLanguageAd() {
        Log.d(AD_LOG, "STEP 1 | screen khulyu (screenCount=$screenCount) -> [$TAG_LANG] observe")
        adjustLangShimmerHeight()

        startAdFlow(TAG_LANG)


        Log.d(AD_LOG, "STEP 2 | [$TAG_LANG_CLICK] preload chalu (100ms delay)")
        binding.root.postDelayed({
            if (!isFinishing && !isDestroyed) {
                AdsManager.loadNativeLanguageClick(this@LanguageActivity, screenCount)
                AdsManager.loadNativeOnboarding1(this@LanguageActivity, isIntroFlowDone())
            }
        }, 100)
    }


    private fun loadLanguageClickAd(): Boolean {
        val cfg = AdRemoteConfig.getInstance()
        val isClickAdEnabled = if (screenCount == 1) cfg.native_language_1_click.isEnable
        else cfg.native_language_2_click.isEnable
        if (configScript?.isNeedToShowADs != true || !isClickAdEnabled || !isInternetAvailable()) {
            Log.d(
                AD_LOG,
                "STEP 4 | SWAP SKIP -> juno [$TAG_LANG] ad j rakhyo " +
                        "(needToShowAds=${configScript?.isNeedToShowADs}, " +
                        "clickAdEnable=$isClickAdEnabled, net=${isInternetAvailable()})"
            )
            return false
        }

        Log.d(AD_LOG, "STEP 4 | SWAP: [$TAG_LANG] observer band + juno ad view kadhyo")
        AdsManager.getAdLive(TAG_LANG).removeObservers(this)

        binding.frAds.removeAllViews()
        binding.frAds.visibility = View.GONE
        adjustLangShimmerHeight()
        binding.adShimmer.root.visibility = View.VISIBLE

        Log.d(AD_LOG, "STEP 5 | have [$TAG_LANG_CLICK] observe -> shimmer batavyu")
        startAdFlow(TAG_LANG_CLICK)
        return true
    }

    private fun adjustLangShimmerHeight() {
        val d = resources.displayMetrics.density
        val large = screenCount != 1
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


    private fun startAdFlow(tag: String) {
        configScript?.let { config ->

            val ad = AdRemoteConfig.getInstance()
            val isAdEnabled = if (tag == TAG_LANG) {
                if (screenCount == 1) ad.native_language_1.isEnable else ad.native_language_2.isEnable
            } else if (tag == TAG_LANG_CLICK) {
                if (screenCount == 1) ad.native_language_1_click.isEnable else ad.native_language_2_click.isEnable
            } else {
                false
            }

            if (config.isNeedToShowADs && isAdEnabled) {


                startShimmerTimeout(tag)

                AdsManager.getAdLive(tag).observe(this@LanguageActivity) { state ->

                    if (state is NativeAdUiState.Success) {
                        Log.d(
                            AD_LOG,
                            "SHOW  | [$tag] ad screen par mukyo (id=${state.adsID})"
                        )
                        binding.adShimmer.root.visibility = View.GONE
                        binding.frAds.visibility = View.VISIBLE

                        ERainAd.getInstance().populateNativeAdView(
                            this@LanguageActivity,
                            state.ad,
                            binding.frAds,
                            binding.adShimmer.root
                        )
//// Height cta
                        val remoteHeightDp = state.ctaHeight
                        val adBtn = binding.frAds.findViewById<View>(R.id.ad_call_to_action)
                        if (adBtn != null) {
                            val density = resources.displayMetrics.density
                            val heightInPx = (remoteHeightDp * density).toInt()

                            val params = adBtn.layoutParams
                            params?.let {
                                it.height = heightInPx
                                adBtn.layoutParams = it
                                adBtn.requestLayout()
                                Log.d(
                                    "AdManager123",
                                    "Height set to: $heightInPx px for tag: $tag"
                                )
                            }

                        } else {
                            Log.e(
                                "AdManager123",
                                "CTA Button not found in layout for tag: $tag"
                            )
                        }


                    } else if (state is NativeAdUiState.Loading) {
                        if (binding.frAds.visibility != View.VISIBLE) {
                            binding.adShimmer.root.visibility = View.VISIBLE
                        }
                    } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                        if (binding.frAds.visibility != View.VISIBLE) {
                            Log.d(AD_LOG, "[$tag] fail -> ad slot band (retry nahi)")
                            binding.adShimmer.root.visibility = View.GONE
                            binding.frAds.visibility = View.GONE
                        }
                    }
                }
            } else {
                binding.adShimmer.root.visibility = View.GONE
                binding.frAds.visibility = View.GONE
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
            Log.d(
                AD_LOG,
                "STEP 3 | language click: ${languageModel_?.s_lan_name} " +
                        "(alreadySwapped=$isClickAdPhase)"
            )

            if (!isClickAdPhase && loadLanguageClickAd()) {
                isClickAdPhase = true
            }
            showDoneButton()
        }
    }

    private fun showDoneButton() {
        if (configScript?.delayButtonDoneLanguage == true) {
            binding.done.visibility = View.INVISIBLE
            doneHandler.removeCallbacksAndMessages(null)
            doneHandler.postDelayed({
                binding.done.visibility = View.VISIBLE
            }, configScript?.timeDelayLanguageDone ?: 2000L)
        } else {
            binding.done.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        doneHandler.removeCallbacksAndMessages(null)
        adHandler.removeCallbacksAndMessages(null)
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
