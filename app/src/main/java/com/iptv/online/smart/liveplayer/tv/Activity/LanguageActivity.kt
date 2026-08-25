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


/**
 * Demo jevu: language na BADHA case ek j activity ma.
 *
 * Splash flow (settingss = false):
 *  - Screen kholta -> "native_lang_tag" ad observe thay chhe.
 *  - User language par click kare -> e J screen par observer swap thai ne
 *    "native_lang_click_tag" ad batave chhe ane Done button dekhay chhe.
 *
 * Settings flow (settingss = true):
 *  - Back + Done visible, koi native ad nahi, back e inter_back batave chhe.
 *
 * (pehla aa be case LanguageOptionalActivity ane LanguageChangeActivity ma alag
 *  hata -> have alag activity nathi.)
 */
class LanguageActivity : Base__Activity<ActivityLanguageBinding>(),
    LanguageAdapter.OnClickItemListener {
    private var languageAdapterNew: LanguageAdapter? = null
    private val languageModelNews: MutableList<LanguageModel?> = ArrayList<LanguageModel?>()
    private var global_Language: LanguageModel? = null
    private var preferenceManager: PreferenceManager? = null
    private var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null
    private var configScript: RemoteConfigdata? = null

    // Click-ad no swap thai gayo -> fari na karvo (duplicate observer na thay).
    private var isClickAdPhase = false

    // Onboarding native no preload fakt pehla click par ek j var.
    private var isOnboardingAdRequested = false
    private val doneHandler = Handler(Looper.getMainLooper())

    // Settings mathi aavya hoy tya J back par inter_back joie -> Base e preload kare chhe.
    override val needBackInterAd: Boolean
        get() = intent.getBooleanExtra("settingss", false)

    companion object {
        private const val TAG_LANG = "native_lang_tag"
        private const val TAG_LANG_CLICK = "native_lang_click_tag"

        // Language screen na ad flow no logcat tag - logcat ma "LangAd" filter karo.
        private const val AD_LOG = "LangAd"
    }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(getLayoutInflater())
    }

    override fun bindObjects() {

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

        // iptv2 jevu dev-settings dialog: title 10-click e khule, toggle pachi splash restart.
        val restartSplash = {
            val i = Intent(this, SplashActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
        }
        // iptv2 jevu: fakt title (textheader) par.
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
            // Ad-review (native_language_1_click): Done button ma fakt ✓ icon rakhvu,
            // "Done" text kadhi nakhvu. Settings screen par text pehla jevu j rahe chhe.
            binding.gradinttext.visibility = View.GONE

            // screenCount Splash ma J nakki thay chhe (pehli vaar _1, pachi hammesha _2) ane
            // companion dwara ahiya aave chhe. Ahiya FARI compute NA karvu -> lang ad ane
            // click ad banne same variant vaapre. Consistent.
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

    // Ad-review: native ad ni bilkul uper vertical-swipe hint (fakt screen 1 par).
    // User language list na chhelle pahonche etle hint jato rahe chhe.
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

        // List etli nani hoy ke scroll j na thay -> hint ni jarur nathi.
        binding.recyclerView.post {
            if (!binding.recyclerView.canScrollVertically(1)) hideSwipeHint()
        }
    }

    private fun hideSwipeHint() {
        if (binding.swipeHint.visibility != View.VISIBLE) return
        binding.swipeHint.cancelAnimation()
        binding.swipeHint.visibility = View.GONE
    }


    private fun loadLanguageAd() {
        Log.d(AD_LOG, "STEP 1 | screen khulyu (screenCount=$screenCount) -> [$TAG_LANG] observe")
        adjustLangShimmerHeight()

        startAdFlow(TAG_LANG)

        // Reviewer: click-ad ne screen enter thatan EK J VAR, ~100ms delay sathe preload karo.
        // (Fail thay to language select vakhte FARI load nathi karvano -> retry kadhi nakhyo.)
        Log.d(AD_LOG, "STEP 2 | [$TAG_LANG_CLICK] preload chalu (100ms delay)")
        binding.root.postDelayed({
            AdsManager.loadNativeLanguageClick(this@LanguageActivity, screenCount)
        }, 100)
    }

    // Language click pachi E J screen par click-ad batavo:
    // juno (lang) observer band -> juno ad view kadho -> shimmer -> click-ad observe.
    //
    // Return: swap kharekhar thayo ke nahi. Click-ad load J na thavano hoy (config off ke
    // net nathi) tya swap NA karvo -> nahi to juno dekhato lang ad jato rahe ane screen par
    // kayamnu shimmer rahi jaay (loadAd tyare koi state post karto nathi).
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

    // Shimmer ne current ad variant JEVU J banave (nanu-motu nahi, EXACT match):
    //  screenCount==1 -> small ad  : media 80dp,  media topMargin 6dp,  CTA topMargin 8dp,  card side 0.
    //  screenCount==2 -> layout_native_ad_large : media 130dp, media topMargin 8dp, CTA topMargin 16dp, card 8dp.
    // Aa mate shimmer-ad ni height sarkhi -> loading pachi koi jump/khali space nahi.
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
            // Enable have JSON (AdRemoteConfig) mathi aave chhe.
            val ad = AdRemoteConfig.getInstance()
            val isAdEnabled = if (tag == TAG_LANG) {
                if (screenCount == 1) ad.native_language_1.isEnable else ad.native_language_2.isEnable
            } else if (tag == TAG_LANG_CLICK) {
                if (screenCount == 1) ad.native_language_1_click.isEnable else ad.native_language_2_click.isEnable
            } else {
                false
            }

            if (config.isNeedToShowADs && isAdEnabled) {
                // Safety: aa tag no ad kyarey request J na thayo hoy (dakhla tarike splash ma
                // inter_splash load na thayo etle preload chuki gayo) to LiveData khali rahe
                // -> observer kyarey na chale -> screen par kayamnu shimmer. Etle ahiya
                // jate load kari daie.
                if (AdsManager.getAdLive(tag).value == null) {
                    Log.d(AD_LOG, "[$tag] preload thayo J nathi -> ahiya thi load karie")
                    if (tag == TAG_LANG) AdsManager.loadNativeLanguage(this, screenCount)
                    else if (tag == TAG_LANG_CLICK) AdsManager.loadNativeLanguageClick(this, screenCount)
                }

                val retriedTags = mutableSetOf<String>()
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
                                // CTA gradient (#2663FF -> #7BD5F5) have layout XML ma J chhe:
                                // layout_native_ad_lang_1 ane layout_native_ad_lang_click
                                // (banne fakt screen 1 vaapre chhe) -> bg_btn_native_cta_review.
                            } else {
                                Log.e(
                                    "AdManager123",
                                    "CTA Button not found in layout for tag: $tag"
                                )
                            }
                            //


                        } else if (state is NativeAdUiState.Loading) {
                            if (binding.frAds.visibility != View.VISIBLE) {
                                binding.adShimmer.root.visibility = View.VISIBLE
                            }
                        } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            if (binding.frAds.visibility != View.VISIBLE) {
                                // Reviewer: click-ad (TAG_LANG_CLICK) fail thay to FARI load NAHI
                                // (duplicate request atkave). Fakt native ad (TAG_LANG) mate ek j
                                // var fallback reload rahe.
                                if (tag == TAG_LANG && retriedTags.add(tag)) {
                                    binding.adShimmer.root.visibility = View.VISIBLE
                                    AdsManager.loadNativeLanguage(this@LanguageActivity, screenCount)
                                    Log.d("AdManager123", "[$tag] fallback reload triggered")
                                } else {
                                    // click fail (ke native retry pachi pan fail) -> ad slot band
                                    binding.adShimmer.root.visibility = View.GONE
                                    binding.frAds.visibility = View.GONE
                                }
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

        // iptv2 jevu dev-settings dialog: title 10-click e khule, toggle pachi splash restart.
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

                // Settings mathi language badlyu - aakho task fresh banavie jethi
                // MainActivity pan navi language ma recreate thay. startActivities thi
                // Main (niche) + Settings (uper) stack banave -> user Settings par land
                // thay ane back kare tya Main navi language ma male.
                val mainIntent = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivities(arrayOf(mainIntent, settingsIntent))
                finish()

            } else {
                // iptv2 jevu: is_intro_page true -> onboarding, nahi to skip -> ActivityNotice.
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
            // ===== Settings flow: pehla jevu j single screen (Done batavo) =====
            binding.done.visibility = View.VISIBLE
        } else {
            // ===== Splash flow: same screen par click-ad par switch (demo jevu) =====
            Log.d(
                AD_LOG,
                "STEP 3 | language click: ${languageModel_?.s_lan_name} " +
                        "(alreadySwapped=$isClickAdPhase)"
            )
            if (!isOnboardingAdRequested) {
                isOnboardingAdRequested = true
                // pehla LanguageOptionalActivity ma hatu -> have pehla click par.
                AdsManager.loadNativeOnboarding1(this, isIntroFlowDone())
            }
            // swap na thai shakyo hoy (net nathi) to bija click par fari try thay.
            if (!isClickAdPhase && loadLanguageClickAd()) {
                isClickAdPhase = true
            }
            showDoneButton()
        }
    }

    // Config pramane Done ne turat ke thodi var pachi batave.
    private fun showDoneButton() {
        if (configScript?.delayButtonDoneLanguage == true) {
            binding.done.visibility = View.INVISIBLE
            doneHandler.removeCallbacksAndMessages(null)
            doneHandler.postDelayed({
                binding.done.visibility = View.VISIBLE
            }, configScript?.timeDelayLanguageDone ?: 1200L)
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
            // Settings flow: Base nu default -> inter_back batavi ne Settings par pacha.
            super.onBackPressed()
        } else {
            // Splash flow: app band.
            finishAffinity()
        }
    }
}
