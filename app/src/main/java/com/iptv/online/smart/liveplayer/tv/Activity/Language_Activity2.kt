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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ads.module.ads.ERainAd
import com.iptv.online.smart.liveplayer.tv.Activity.SplashActivity.Companion.screenCount
import com.iptv.online.smart.liveplayer.tv.Adapter.LanguageAdapter2
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
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


class Language_Activity2 : Base__Activity<ActivityLanguageBinding>(),
    LanguageAdapter2.OnClickItemListener {

    private var languageAdapterNew: LanguageAdapter2? = null
    private val languageModelNews: MutableList<LanguageModel?> = ArrayList<LanguageModel?>()
    private var global_Language: LanguageModel? = null
    private var preferenceManager: PreferenceManager? = null
    private var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null
    private var configScript: RemoteConfigdata? = null

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(getLayoutInflater())
    }

    override fun bindObjects() {

        sp = getSharedPreferences(getPackageName(), 0)
        sp_editor = sp!!.edit()
        configScript = RemoteConfigdata(this@Language_Activity2)
        preferenceManager = PreferenceManager(this)
        BOOKER_Manager.initializingSharedPreference(this)

        // ---- Gradient "Done" text (screen 1 jevu j) ----
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

        // ---- Screen 1 ma select thayeli language intent ma aave chhe ----
        val selectedCode = intent.getStringExtra("selectedLanguage") ?: "en"
        val selectedName = intent.getStringExtra("selectedLanguageName") ?: "English"
        global_Language = LanguageModel(selectedCode, selectedName, true)

        // Screen 2 hammesha splash flow no j part chhe -> back chhupavo
        binding.back.visibility = View.GONE
        binding.textheader.text = getString(R.string.choose_your_language)

        // Done button: delay config hoy to thodi var pachi batavo, nahi to turat
        if (configScript?.delayButtonDoneLanguage == true) {
            binding.done.visibility = View.INVISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                binding.done.visibility = View.VISIBLE
            }, 3000)
        } else {
            binding.done.visibility = View.VISIBLE
        }

        addList()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        setAdapter()

        // Screen 2 par "click" native ad batave (screen 1 na click ad jevu).
        // Aa ad Language screen 1 khulti vakhte pehla thi preload thai gayu hoy chhe.
        loadLanguageClickAd()

        // Onboarding native ad ne AHIYA (screen 2 = Intro ni AGAL-ni screen) PRELOAD karo.
        // Pehla aa Language screen 1 ma thato hato (2 screen aagal -> vadhu drop-off -> low
        // show rate). Have 1 j screen aagal -> Intro (IntroFragment1) khule tyare ad READY
        // (instant, shimmer ochho) + show rate pan saras.
        if (configScript?.isNeedToShowADs == true) {
            val isDone = isIntroFlowDone()
            val onbAdId = if (isDone) AdsId.nativeOnboarding2_1 else AdsId.nativeOnboarding1_1
            val onbTag = if (isDone) "native_onboarding_2_1" else "native_onboarding_1_1"
            AdsManager.loadAd(this, onbAdId, R.layout.layout_native_ad_large, onbTag)
            Log.d("AdManager123", "Onboarding native PRELOADED on Language screen 2: $onbTag")
        }
    }

    override fun bindListener() {
        binding.back.setOnClickListener { onBackPressed() }

        binding.done.setOnClickListener {
            if (global_Language != null) {
                saveAndApplyLanguage(global_Language!!)
            } else {
                saveAndApplyLanguage(LanguageModel("en", "English", true))
            }
        }
    }

    override fun bindMethod() {
    }

    // ---- Screen 2 par "native_lang_click_tag" ad observe/populate karo ----
    private fun loadLanguageClickAd() {
        val tag = "native_lang_click_tag"
        configScript?.let { config ->
            val isAdEnabled =
                if (screenCount == 1) config.nativeLang1ClickOn else config.nativeLang2ClickOn

            if (config.isNeedToShowADs && isAdEnabled) {
                val retriedTags = mutableSetOf<String>()
                lifecycleScope.launchWhenStarted {
                    AdsManager.adStateFlow.collect { states ->
                        val state = states[tag]
                        if (state is NativeAdUiState.Success) {
                            Log.d("AdManager123", "[$tag] Success, 🚀 showing ID: [${state.adsID}]")
                            binding.adShimmer.root.visibility = View.GONE
                            binding.frAds.visibility = View.VISIBLE
                            ERainAd.getInstance().populateNativeAdView(
                                this@Language_Activity2,
                                state.ad,
                                binding.frAds,
                                binding.adShimmer.root
                            )
                            // CTA button height (screen 1 jevu j)
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
                                }
                                if (screenCount == 1) {
                                    val bgColor = Color.parseColor("#4ADA34")
                                    val bg = adBtn.background?.mutate()
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
                            }
                        } else if (state is NativeAdUiState.Loading) {
                            if (binding.frAds.visibility != View.VISIBLE) {
                                binding.adShimmer.root.visibility = View.VISIBLE
                            }
                        } else if (state == null || state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                            // Click ad ready nathi (preload j nathi thayu ke fail gayu) ->
                            // 90%+ show rate mate ahiya ek j var jate FRESH load karo (fallback).
                            if (binding.frAds.visibility != View.VISIBLE) {
                                if (retriedTags.add(tag)) {
                                    binding.adShimmer.root.visibility = View.VISIBLE
                                    val adId = if (screenCount == 1)
                                        AdsId.nativeLanguage1Click else AdsId.nativeLanguage2Click
                                    AdsManager.loadAd(
                                        this@Language_Activity2, adId,
                                        R.layout.layout_native_ad_lang_click, tag
                                    )
                                    Log.d("AdManager123", "[$tag] fallback reload triggered")
                                } else {
                                    binding.adShimmer.root.visibility = View.GONE
                                    binding.frAds.visibility = View.GONE
                                }
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

    private fun saveAndApplyLanguage(model: LanguageModel) {
        sp_editor!!.putString("selected_language_code", model.s_lan_code)
        sp_editor!!.putString("selected_language_name", model.s_lan_name)
        val success = sp_editor!!.commit()
        if (success) {
            updateLocale(model.s_lan_code)
            BOOKER_Manager.setLanguageSelected(true)

            // Screen 2 hammesha splash flow -> IntroActivity ma jao
            val i = Intent(this, IntroActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
            finish()
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
        // Screen 1 ma pasand karel language ne highlight karva mate name pass karo.
        // isFromSettings = true pass karvathi adapter selected item highlight kare chhe.
        val currentLang = global_Language?.s_lan_name
        languageAdapterNew =
            LanguageAdapter2(languageModelNews, this, currentLang, true, this)
        binding.recyclerView.adapter = languageAdapterNew
    }

    override fun onClickItem(languageModel_: LanguageModel?) {
        global_Language = languageModel_
        sp_editor!!.putBoolean("is_lang_btn_visible", true).apply()

        if (configScript?.delayButtonDoneLanguage == true) {
            binding.done.visibility = View.INVISIBLE
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.done.visibility = View.VISIBLE
            }, 3000)
        } else {
            binding.done.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        // Splash flow -> app band karo
        finishAffinity()
    }
}
