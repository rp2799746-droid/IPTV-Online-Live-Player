package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ads.module.ads.ERainAd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptv.online.smart.liveplayer.tv.Adapter.LanguageAdapter
import com.iptv.online.smart.liveplayer.tv.Model.LanguageModel
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.ReadFile.BOOKER_Manager
import com.iptv.online.smart.liveplayer.tv.ReadFile.PreferenceManager
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityLanguageBinding
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import kotlinx.coroutines.launch
import java.util.Locale


class Language_Activity : Base__Activity<ActivityLanguageBinding>(),
    LanguageAdapter.OnClickItemListener {
    private var languageAdapterNew: LanguageAdapter? = null
    private val languageModelNews: MutableList<LanguageModel?> = ArrayList<LanguageModel?>()
    private var global_Language: LanguageModel? = null
    private var preferenceManager: PreferenceManager? = null
    private var sp: SharedPreferences? = null
    private var sp_editor: SharedPreferences.Editor? = null
    private var screenCount = 1
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
        preloadIntroAds()
    }


    private fun loadLanguageAd() {
        val currentAdId = if (screenCount == 1) AdsId.nativeLanguage1 else AdsId.nativeLanguage2
        val tag = "native_lang_tag"
        configScript = RemoteConfigdata(this@Language_Activity)
        InfinityAdsManager.loadAd(
            this,
            currentAdId,
            R.layout.layout_native_ad_large,
            tag
        )

        startAdFlow(tag)
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

            // ૨. Native Onboarding 1_4 / 2_4
            val adId4 = if (isDone) AdsId.nativeOnboarding2_4 else AdsId.nativeOnboarding1_4
            val tag4 = if (isDone) "native_onboarding_2_4" else "native_onboarding_1_4"
            InfinityAdsManager.loadAd(this, adId4, R.layout.layout_native_ad_large, tag4)

            // ૩. જો ફૂલ એડ પણ પ્રીલોડ કરવી હોય (Native Onboarding Full)
            val isFullAdEnabled = if (isDone) config.nativeOnbFull2On else config.nativeOnbFull1On
            if (isFullAdEnabled) {
                val adIdFull =
                    if (isDone) AdsId.nativeOnboardingFull2 else AdsId.nativeOnboardingFull1
                val tagFull = if (isDone) "native_onboarding_full_2" else "native_onboarding_full_1"
                InfinityAdsManager.loadAd(this, adIdFull, R.layout.layout_native_ad_full, tagFull)
            }
        }
    }

    private fun startAdFlow(tag: String) {
        configScript?.let { config ->
            // Ads Id અને Screen Count મુજબ કઈ કી ચેક કરવી તે નક્કી કરો
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
                                    Log.d("AdManager123", "Height set to: $heightInPx px for tag: $tag")
                                }
                            } else {
                                Log.e("AdManager123", "CTA Button not found in layout for tag: $tag")
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
                val intent = Intent(this, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
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
        languageModelNews.add(LanguageModel("en", "English", false))
        languageModelNews.add(LanguageModel("hi", "Hindi", false))
        languageModelNews.add(LanguageModel("es", "Spanish", false))
        languageModelNews.add(LanguageModel("fr", "French", false))
        languageModelNews.add(LanguageModel("de", "German", false))
        languageModelNews.add(LanguageModel("pt", "Portuguese", false))
        languageModelNews.add(LanguageModel("in", "Indonesian", false))
        languageModelNews.add(LanguageModel("ja", "Japanese", false))
        languageModelNews.add(LanguageModel("sv", "Swedish", false))
        languageModelNews.add(LanguageModel("tr", "Turkish", false))
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

        if (!isFromSettings && !isAdRefreshedInSession) {
            val clickAdId =
                if (screenCount == 1) AdsId.nativeLanguage1Click else AdsId.nativeLanguage2Click
            InfinityAdsManager.loadAd(
                this,
                clickAdId,
                R.layout.layout_native_ad_large,
                "native_lang_click_tag"
            )
            startAdFlow("native_lang_click_tag")
            isAdRefreshedInSession = true
        }


        if (isFromSettings) {
            binding.done.visibility = View.VISIBLE
        } else {
            if (configScript?.delayButtonDoneLanguage == true) {
                binding.done.visibility = View.INVISIBLE
                Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null) // જૂના હેન્ડલર ક્લિયર કરવા માટે
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.done.visibility = View.VISIBLE
                }, 1000)
            } else {
                binding.done.visibility = View.VISIBLE
            }
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