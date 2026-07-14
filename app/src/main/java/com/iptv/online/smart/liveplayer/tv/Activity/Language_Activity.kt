package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ads.module.ads.ERainAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity.Companion.screenCount
import com.iptv.online.smart.liveplayer.tv.Adapter.LanguageAdapter
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.Model.LanguageModel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.ReadFile.BOOKER_Manager
import com.iptv.online.smart.liveplayer.tv.ReadFile.PreferenceManager
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityLanguageBinding
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import java.util.Locale


class Language_Activity : Base__Activity<ActivityLanguageBinding>(),
    LanguageAdapter.OnClickItemListener {
    private var languageAdapterNew: LanguageAdapter? = null
    private val languageModelNews: MutableList<LanguageModel?> = ArrayList<LanguageModel?>()
    private var global_Language: LanguageModel? = null
    private var preferenceManager: PreferenceManager? = null
    private var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null
//    private var screenCount = 1
    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(getLayoutInflater())
    }


    companion object {
        var isAdRefreshedInSession = false
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

//        binding.tvTitle.setOnAdminAdToggleListener(){
//            Routes.startSplashActivity(this@LanguageActivity)
//            finish()
//        }

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

            screenCount = sp!!.getInt("lang_screen_count", 0) + 1
            if (screenCount > 2) screenCount = 1
            sp_editor!!.putInt("lang_screen_count", screenCount).apply()
            loadLanguageAd()


        }
        configScript = RemoteConfigdata(this@Language_Activity)


        preferenceManager = PreferenceManager(this)
        BOOKER_Manager.initializingSharedPreference(this)
        addList()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        loadSavedLanguage()
        setAdapter()
        // Show-rate optimization: native_onboarding_1_1 ne ahiya (Language screen) preload
        // NA karo -> te bahu vehlu hatu (Intro sudhi user pohonche tya sudhi ghana exit thai
        // jata -> show rate 23%). Have IntroFragment1 pote j (jyare Intro khule tyare) load
        // kare che (eno fallback che), etle load show point ni najik thay -> show rate uncho.
        // preloadIntroAds()
    }


    private fun loadLanguageAd() {


        val tag = "native_lang_tag"
        configScript = RemoteConfigdata(this@Language_Activity)

        startAdFlow(tag)

        // CLICK ad ne Language screen 1 khule tyare J PRELOAD karo (demo jevu; splash ma nahi).
        // Screen 1 nu single action = language tap -> screen 2. Etle je users screen 1 e aave
        // e lagbhag badha screen 2 e jaay -> preload thi screen 2 par ad READY (instant, shimmer
        // ochho) + fakt 1 screen aagal hovathi show rate pan saras.
        val clickAdId =
            if (screenCount == 1) AdsId.nativeLanguage1Click else AdsId.nativeLanguage2Click
        val isClickAdEnabled =
            if (screenCount == 1) configScript!!.nativeLang1ClickOn else configScript!!.nativeLang2ClickOn
        if (configScript!!.isNeedToShowADs && isClickAdEnabled) {
            InfinityAdsManager.loadAd(
                this, clickAdId, R.layout.layout_native_ad_lang_click, "native_lang_click_tag"
            )
            Log.d("AdManager123", "Click ad PRELOADED on Language screen 1 open")
        }
    }

    private fun preloadIntroAds() {
        val isFromSettings = intent.getBooleanExtra("settingss", false)
        if (isFromSettings) return

        val isDone = isIntroFlowDone()
        val config = RemoteConfigdata(this)

        if (config.isNeedToShowADs) {
            Log.d("AdManager123", "Preloading Intro Native Ads in Language Activity...")

            // ૧. Native Onboarding 1_1 / 2_1
            val adId1 = if (isDone) AdsId.nativeOnboarding2_1 else AdsId.nativeOnboarding1_1
            val tag1 = if (isDone) "native_onboarding_2_1" else "native_onboarding_1_1"
            InfinityAdsManager.loadAd(this, adId1, R.layout.layout_native_ad_large, tag1)

        }
    }

    private fun startAdFlow(tag: String) {
        configScript?.let { config ->
            val isAdEnabled = if (tag == "native_lang_tag") {
                if (screenCount == 1) config.nativeLang1On else config.nativeLang2On
            } else if (tag == "native_lang_click_tag") {
                if (screenCount == 1) config.nativeLang1ClickOn else config.nativeLang2ClickOn
            } else {
                false
            }

            if (config.isNeedToShowADs && isAdEnabled) {
                lifecycleScope.launchWhenStarted {
                    InfinityAdsManager.adStateFlow.collect { states ->
                        val state = states[tag]

                        if (state is NativeAdUiState.Success) {
                            binding.adShimmer.root.visibility = View.GONE
                            binding.frAds.visibility = View.VISIBLE

                            ERainAd.getInstance().populateNativeAdView(
                                this@Language_Activity,
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
                                if (tag == "native_lang_click_tag" && screenCount == 1) {
                                    val bgColor = Color.parseColor("#4ADA34")

                                    val bg = adBtn.background?.mutate()   // <-- mutate() સૌથી પહેલા
                                    if (bg is GradientDrawable) {
                                        bg.setColor(bgColor)
                                        adBtn.background = bg
                                    } else if (bg != null) {
                                        bg.setColorFilter(bgColor, PorterDuff.Mode.SRC_ATOP)
                                        adBtn.background = bg
                                    } else {
                                        adBtn.setBackgroundColor(bgColor)
                                    }
                                }

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
                        } else if (state is NativeAdUiState.Failed) {
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
                // ===== FIX 2: reliable + FAST (no flash) =====
                // Problem: fakt finish() karta jivti juni SettingsActivity (juni/english
                // language) pehla dekhati, pachi onResume ma recreate() thato -> flash +
                // "time lage che" evu lagtu.
                // Solution: CLEAR_TOP thi SettingsActivity ne finish+FRESH recreate karo.
                // Standard launchMode hovathi CLEAR_TOP = e instance destroy thai ne navu
                // bane -> navu attachBaseContext SIDHU navi language ma render kare (juni
                // english no flash j na aave). MainActivity back-stack ma saval rahe che.
                // SettingsActivity.onResume nu recreate() logic have safety-net tarike rahe.
                val i = Intent(this, SettingsActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
                finish()

                // ===== OLD approaches (comment karya chhe, delete nathi karya) =====
                // finish()   // FIX 1: reliable pan flash aavto
                /*
                val intent = Intent(this, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
                */
            } else {
                val intent = Intent(this, IntroActivity::class.java)
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
            // ===== NEW: GpsTracker jevu 2-screen flow =====
            // Click ad screen 1 khulti vakhte j preload thai gayu chhe (loadLanguageAd ma),
            // etle screen 2 par te ready hoy -> instant show.
            // Splash flow ma language tap thatra j biju language screen (screen 2) kholo.
            val i = Intent(this, Language_Activity2::class.java)
            i.putExtra("selectedLanguage", languageModel_?.s_lan_code)
            i.putExtra("selectedLanguageName", languageModel_?.s_lan_name)
            i.putExtra("isFromSplash", true)
            // GpsTracker jevu seamless transition (animation vagar) -> jethi activity
            // badlati hoy evu na dekhay, same screen par ad refresh thayu hoy evu lage.
            val options = android.app.ActivityOptions.makeCustomAnimation(this, 0, 0)
            startActivity(i, options.toBundle())
            overridePendingTransition(0, 0)

            // ===== OLD single-screen behaviour (comment karyu chhe, delete nathi karyu) =====
            /*
            if (!isAdRefreshedInSession) {
                startAdFlow("native_lang_click_tag")
                isAdRefreshedInSession = true
            }

            if (configScript?.delayButtonDoneLanguage == true) {
                binding.done.visibility = View.INVISIBLE
                Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.done.visibility = View.VISIBLE
                }, 3000)
            } else {
                binding.done.visibility = View.VISIBLE
            }
            */
        }
    }

    override fun onBackPressed() {
        if (getIntent().getBooleanExtra("settingss", false)) {
            super.onBackPressed()
        } else {
            finishAffinity()
        }
    }
}