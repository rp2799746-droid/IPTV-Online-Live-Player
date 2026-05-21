package com.iptv.online.smart.liveplayer.tv.Activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.iptv.online.smart.liveplayer.tv.App
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences.set
import com.iptv.online.smart.liveplayer.tv.Ads.InfinityAdsManager
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.getShouldDisplayWidgetUninstall
import com.iptv.online.smart.liveplayer.tv.databinding.ActivitySplashBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.isIntroFlowDone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.itg.iaumodule.IAdConsentCallBack
import com.itg.iaumodule.ITGAdConsent.loadAndShowConsent
import com.itg.iaumodule.ITGAdConsent.resetConsentDialog
import java.security.MessageDigest


class SplashActivity : Base__Activity<ActivitySplashBinding>() {
    private var isFlowStarted = false
    private var prefs: SharedPreferences? = null
    private var canPersonalized = true
    private var appUpdate_Manager: AppUpdateManager? = null
    val UPDATE_REQUEST_CODE: Int = 123
    private var isUpdateChecked = false
    private var noInternetDialog: Dialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null


    override fun setViewBinding() = ActivitySplashBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        setheader()
        super.onCreate(savedInstanceState)

        binding.btnSettings.isSelected = true
        appUpdate_Manager = AppUpdateManagerFactory.create(this)
        manageAppShortcuts()

    }

    override fun bindObjects() {

        handleIncomingLink(intent)

        prefs = EasyPreferences.defaultPrefs(this)

        if (!prefs!!.getBoolean(
                "KEY_IS_USER_GLOBAL", false
            ) && !prefs!!.getBoolean("KEY_CONFIRM_CONSENT", false)
        ) {
            checkNeedToLoadConsent()
        } else {
            startAppFlow()
        }


    }

    private fun startAppFlow() {
        if (!isNetworkAvailable(this@SplashActivity)) {
            Internet_Connection_Status(this@SplashActivity)
            registerNetworkCallback()
        } else {
            loadingRemoteConfig()
        }
    }

    private fun manageAppShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java)

            val shouldShowShortcuts = getShouldDisplayWidgetUninstall()

            if (shouldShowShortcuts) {
                val shortcutList = mutableListOf<ShortcutInfo>()

                // ૧. Stream TV
                val Streamtv = ShortcutInfo.Builder(this, "stream_id")
                    .setShortLabel(getString(R.string.stream_your_tv))
                    .setIcon(Icon.createWithResource(this, R.drawable.stream_tv))
                    .setIntent(Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("widget_flow", "flow_iptv")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    })
                    .build()
                // 2. IPTV

                val mirrorShortcut = ShortcutInfo.Builder(this, "mirror_id")
                    .setShortLabel(getString(R.string.iptv_smart))
                    .setIcon(Icon.createWithResource(this, R.drawable.iptv_wid))
                    .setIntent(Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("ACTION_TYPE", "OPEN_IPTV")
                        putExtra("widget_flow", "flow_mirror")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                    })
                    .build()

                // ૨. Cast TV Shortcut
                val castShortcut = ShortcutInfo.Builder(this, "cast_id")
                    .setShortLabel(getString(R.string.cast_to_tv))
                    .setIcon(Icon.createWithResource(this, R.drawable.cast_wiget))
                    .setIntent(Intent(this, MirrorStepsActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("widget_flow", "flow_tv")
                    })
                    .build()

                // ૩. Uninstall Shortcut
                val uninstallShortcut = ShortcutInfo.Builder(this, "uninstall_id")
                    .setShortLabel(getString(R.string.uninstall))
                    .setIcon(Icon.createWithResource(this, R.drawable.uninstall_wid))
                    .setIntent(Intent(this, SplashActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("widget_flow", "flow_uninstall")
                    })
                    .build()

                shortcutList.add(Streamtv)
                shortcutList.add(mirrorShortcut)
                shortcutList.add(castShortcut)
                shortcutList.add(uninstallShortcut)

                shortcutManager.dynamicShortcuts = shortcutList
                Log.d("ShortcutLog", "Shortcuts SHOWN because variable is true")
            } else {
                shortcutManager.removeAllDynamicShortcuts()
                Log.d("ShortcutLog", "Shortcuts HIDDEN because variable is false")
            }
        }
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        runOnUiThread(Runnable {
                            if (noInternetDialog != null && noInternetDialog!!.isShowing()) {
                                noInternetDialog!!.dismiss()
                                loadingRemoteConfig()

                            }
                        })
                    }
                }
                connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (networkCallback != null) {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
        }
    }

    private fun handleIncomingLink(intent: Intent?) {
        if (intent != null && intent.hasExtra("link")) {
            val link = intent.getStringExtra("link")
            if (link != null && !link.isEmpty()) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(browserIntent)
            }
        }
    }


    private fun setheader() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false)
        } else {
            getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = getWindow().getDecorView()
            var flags = decorView.getSystemUiVisibility()
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            decorView.setSystemUiVisibility(flags)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002) {
            loadingRemoteConfig()
        }
    }

    private fun check_AppUpdate() {
        if (appUpdate_Manager == null) {
            appUpdate_Manager = AppUpdateManagerFactory.create(this)
        }

        appUpdate_Manager!!.getAppUpdateInfo()
            .addOnSuccessListener(OnSuccessListener { appUpdateInfo: AppUpdateInfo? ->
                if (appUpdateInfo!!.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    try {
                        appUpdate_Manager!!.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this@SplashActivity,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                        isUpdateChecked = true
                        proceedAfterChecks()
                    }
                } else {
                    isUpdateChecked = true
                    proceedAfterChecks()
                }
            }).addOnFailureListener(OnFailureListener { e: java.lang.Exception? ->
                isUpdateChecked = true
                proceedAfterChecks()
            })
    }

    private fun proceedAfterChecks() {
        if (isUpdateChecked) {
            check_AppUpdate()
        }
    }

    protected override fun onResume() {
        super.onResume()

        if (isNetworkAvailable(this)) {
            if (noInternetDialog != null && noInternetDialog!!.isShowing()) {
                noInternetDialog!!.dismiss()
                loadingRemoteConfig()

            }
        }

        if (appUpdate_Manager != null) {
            appUpdate_Manager!!.getAppUpdateInfo()
                .addOnSuccessListener(OnSuccessListener { appUpdateInfo: AppUpdateInfo? ->
                    if (appUpdateInfo!!.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        try {
                            appUpdate_Manager!!.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this@SplashActivity,
                                UPDATE_REQUEST_CODE
                            )
                        } catch (ignored: IntentSender.SendIntentException) {
                        }
                    }
                })
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connectivityManager != null) {
            val activeNetworkInfo = connectivityManager.getActiveNetworkInfo()
            return activeNetworkInfo != null && activeNetworkInfo.isConnected()
        }
        return false
    }

    fun Internet_Connection_Status(activity: Activity?) {
        showNoInternetBottomSheet()
    }

    private fun showNoInternetBottomSheet() {
        if (noInternetDialog != null && noInternetDialog!!.isShowing()) return

        noInternetDialog = Dialog(this, R.style.CustomDialog)
        val view = getLayoutInflater().inflate(R.layout.dialog_no_internet, null)
        noInternetDialog!!.setContentView(view)
        noInternetDialog!!.setCancelable(false)

        view.findViewById<View?>(R.id.btn_settings)
            .setOnClickListener(View.OnClickListener { v: View? ->
                if (isNetworkAvailable(this@SplashActivity)) {
                    noInternetDialog!!.dismiss()
                    loadingRemoteConfig()

                } else {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            })

        noInternetDialog!!.show()
    }


    private fun checkNeedToLoadConsent() {
        loadAndShowConsent(true, object : IAdConsentCallBack {
            override fun getCurrentActivity(): Activity {
                return this@SplashActivity
            }

            override fun isDebug(): Boolean {
                return BuildConfig.DEBUG
            }

            override fun isUnderAgeAd(): Boolean {
                return false
            }

            override fun onConsentError(formError: FormError) {
                canPersonalized = true
                loadingRemoteConfig()
                Log.i(TAG, "checkNeedToLoadConsent: onConsentError")
                startAppFlow()

            }

            override fun onConsentStatus(consentStatus: Int) {
                canPersonalized = consentStatus != ConsentInformation.ConsentStatus.REQUIRED
                Log.i(TAG, "checkNeedToLoadConsent: onConsentStatus : $consentStatus")
            }

            override fun onConsentSuccess(b: Boolean) {
                canPersonalized = b
                Log.i(TAG, "checkNeedToLoadConsent: onConsentSuccess : $b")
                handleClickConsent(canPersonalized)
                startAppFlow()
            }

            override fun onNotUsingAdConsent() {
                prefs!!.set("KEY_CONFIRM_CONSENT", true)
                Log.i(TAG, "checkNeedToLoadConsent: onNotUsingAdConsent :")
                canPersonalized = true
                startAppFlow()
            }

            override fun onRequestShowDialog() {
            }

            override fun testDeviceID(): String {
                return "ED3576D8FCF2F8C52AD8E98B4CFA4005"
            }
        })
    }

    private fun loadingRemoteConfig() {
        App.Companion.app?.initFirebaseConfigs(this)
        App.Companion.onRemoteFetched = { firebaseConfig ->
            if (!isFlowStarted) {
                firebaseConfig?.let { config ->
                    setConfigData(config)
                    check_AppUpdate()
                } ?: run {
                    if (isPurchased(this)) {
                        remoteData.isNeedToShowADs = false
                    }
                    goNextScreen()
                }
            }
        }
    }

    private fun loadInfinityFlow() {

        if (isFlowStarted) return
        isFlowStarted = true

        val widgetFlow = intent.getStringExtra("widget_flow")
        Log.e("kk", "loadInfinityFlow: Started, Ads Enabled: ${remoteData.isNeedToShowADs}")

        var currentFlow = widgetFlow
        if (getShouldDisplayWidgetUninstall()) {
            Log.d("AdManager123", "Widget flow active: Options will be shown.")
        } else {
            currentFlow = null
            Log.d("AdManager123", "Widget flow disabled: Moving to normal flow.")
        }
        if (remoteData.isNeedToShowADs) {
            val isUninstallFlow = currentFlow == "flow_uninstall"
            val shouldShowBanner = if (isUninstallFlow) {
                remoteData.bannerSplashUninstall
            } else {
                remoteData.bannerSplashOn
            }
            if (remoteData.isNeedToShowADs && shouldShowBanner) {
                binding.bannerAdLayout.visible

                val bannerIdToUse = if (isUninstallFlow) {
                    Log.d("AdManager123", "loadInfinityFlow: " + "AdsId.BANNER_SPLASH_UNINSTALL")

                    AdsId.BANNER_SPLASH_UNINSTALL
                } else {
                    Log.d("AdManager123", "loadInfinityFlow: " + " AdsId.bannerSplash")

                    AdsId.bannerSplash

                }

                ERainAd.getInstance().loadBanner(this@SplashActivity, bannerIdToUse)
            } else {
                binding.bannerAdLayout.gone
            }

            if (remoteData.nativeLang1On || remoteData.nativeLang2On) {
                val languageNativeId = if (isIntroFlowDone()) AdsId.nativeLanguage2
                else AdsId.nativeLanguage1

                val tagName = "native_lang_tag"

                InfinityAdsManager.loadAd(
                    this,
                    languageNativeId,
                    R.layout.layout_native_ad_large,
                    tagName
                )
                Log.d("PreloadAd", "Language Native Preloading started with tag: $tagName")
            }
            if (remoteData.interOnboardingOn) {
                ERainAd.getInstance().getInterstitialAds(
                    this,
                    AdsId.interOnboarding,
                    object : AdCallback() {
                        override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                            super.onApInterstitialLoad(apInterstitialAd)
                            Log.d("PreloadAd", "Inter Onboarding Preloaded Success")
                        }
                    }
                )
            }


            if (widgetFlow == "flow_uninstall") {
                if (remoteData.interSplashUninstall) {
                    ERainAd.getInstance().loadSplashInterstitialAds(
                        this, AdsId.INTER_SPLASH_UNINSTALL, 25000, 5000, object : AdCallback() {
                            override fun onNextAction() {
                                goNextScreen()
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                goNextScreen()
                            }
                        })
                } else {
                    goNextScreen()
                }
                return
            }

            if (widgetFlow != null && remoteData.interSplashOn) {
                ERainAd.getInstance().loadSplashInterstitialAds(
                    this, AdsId.interSplash, 25000, 5000, object : AdCallback() {
                        override fun onNextAction() {
                            goNextScreen()
                        }

                        override fun onAdFailedToLoad(i: LoadAdError?) {
                            super.onAdFailedToLoad(i)
                            goNextScreen()
                        }
                    })
                return
            }

            if (remoteData.interSplashOn) {
                Log.w(
                    TAG,
                    "loadInfinityFlow: remoteData.isInterOnSplash -> ${remoteData.isInterOnSplash}"
                )
                if (remoteData.isInterOnSplash) {
                    ERainAd.getInstance().loadSplashInterstitialAds(
                        this, AdsId.interSplash, 25000, 5000, object : AdCallback() {
                            override fun onNextAction() {
                                super.onNextAction()
                                Log.w(TAG, "loadInfinityFlow: onNextAction")
                                goNextScreen()
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                goNextScreen()
                            }
                        })
                } else {
                    AppOpenManager.getInstance().loadOpenAppAdSplash(
                        this,
                        AdsId.openSplash,
                        25000, 25000, true, object : AdCallback() {
                            override fun onNextAction() {
                                super.onNextAction()
                                Log.w(TAG, "loadInfinityFlow: onNextAction-else")
                                goNextScreen()
                            }

                            override fun onAdFailedToLoad(i: LoadAdError?) {
                                super.onAdFailedToLoad(i)
                                goNextScreen()
                            }
                        })
                }
            } else {
                goNextScreen()
            }


        } else {
            goNextScreen()
        }
    }

    private fun handleClickConsent(canPersonalized: Boolean) {
        if (canPersonalized) {
            prefs!!.set("KEY_CONFIRM_CONSENT", true)
            Log.i(TAG, "handleClickConsent: $canPersonalized")
        } else {
            resetConsentDialog()
        }
        loadingRemoteConfig()
    }

    private fun goNextScreen() {
        printHashKey()
        val widgetFlow = intent.getStringExtra("widget_flow")

        val intent = when (widgetFlow) {
            "flow_uninstall" -> {
                Intent(this, UninstallScreenActivity::class.java)
            }

            "flow_tv" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("ACTION_TYPE", "OPEN_IPTV")
                }
            }

            "flow_mirror" -> {
                Intent(this, MirrorStepsActivity::class.java)
            }

            "flow_iptv" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("ACTION_TYPE", "OPEN_IPTV")
                }
            }

            else -> {
                if (isPurchased(this)) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, Language_Activity::class.java).apply {
                        putExtra("isFromSplash", true)
                    }
                }
            }
        }

        startActivity(intent)
        finish()
    }


    private fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(
                packageName, PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (_: Exception) {
        }
    }

    override fun bindListener() = Unit
    override fun bindMethod() = Unit

    override fun onBackPressed() {

    }

    private val remoteData: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }

    private fun setConfigData(config: FirebaseRemoteConfig) {
        Log.i(TAG, "setConfigData: $config")
        remoteData.delayButtonDoneLanguage = config.getBoolean("delay_button_done_language")
        remoteData.privacyLink = config.getString("privacy_policy")
        remoteData.height_button_cta = config.getString("height_button_cta")
//        remoteData.terms_of_uses = config.getString("terms_of_uses")
//        remoteData.aboutUsLink = config.getString("about_us")
        remoteData.isNeedToShowADs = config.getBoolean("is_need_to_show_ads")


        // ---------- Splash ----------
        remoteData.interSplashOn = config.getBoolean("ad_inter_splash_on")
        remoteData.bannerSplashOn = config.getBoolean("ad_banner_splash_on")
        remoteData.appOpenOn = config.getBoolean("ad_app_open_on")

        // ---------- Language Screen ----------
        remoteData.nativeLang1On = config.getBoolean("ad_native_lang1_on")
        remoteData.nativeLang2On = config.getBoolean("ad_native_lang2_on")
        remoteData.nativeLang1ClickOn = config.getBoolean("ad_native_lang1_click_on")
        remoteData.nativeLang2ClickOn = config.getBoolean("ad_native_lang2_click_on")

        // ---------- Onboarding ----------
        remoteData.nativeOnb11On = config.getBoolean("ad_native_onb_1_1_on")
        remoteData.nativeOnbFull1On = config.getBoolean("ad_native_onb_full_1_on")
        remoteData.nativeOnb14On = config.getBoolean("ad_native_onb_1_4_on")
        remoteData.nativeOnbFull2On = config.getBoolean("ad_native_onb_full_2_on")
        remoteData.nativeOnb21On = config.getBoolean("native_onboarding_2_1_on")
        remoteData.nativeOnb24On = config.getBoolean("ad_native_onb_2_4_on")
        remoteData.interOnboardingOn = config.getBoolean("ad_inter_onb_on")

        // ---------- Home ----------
        remoteData.nativeHomeOn = config.getBoolean("ad_native_home_on")
        remoteData.interHomeOn = config.getBoolean("ad_inter_home_on")

        // ---------- Collapsible Banner ----------
        remoteData.bannerCollapsehomeOn = config.getBoolean("ad_banner_collapse_home_on")

        // ---------- Reward ----------
        remoteData.rewardOn = config.getBoolean("ad_reward_on")

        //Add
        remoteData.interback = config.getBoolean("inter_back")
        remoteData.nativeChannel = config.getBoolean("native_channel")
        remoteData.nativeFavorite = config.getBoolean("native_favorite")
        remoteData.nativeHistory = config.getBoolean("native_history")
        remoteData.nativePlaylist = config.getBoolean("native_playlist")
        remoteData.interMirroring = config.getBoolean("inter_mirroring")
        remoteData.nativeMirroring = config.getBoolean("native_mirroring")
        remoteData.nativeChannelList = config.getBoolean("native_channel_list")
        remoteData.interAddPlaylist = config.getBoolean("inter_add_playlist")
        remoteData.interSplashUninstall = config.getBoolean("inter_splash_uninstall")
        remoteData.bannerSplashUninstall = config.getBoolean("banner_splash_uninstall")
        remoteData.nativeUninstall = config.getBoolean("native_uninstall")
        remoteData.nativesurveyUninstall = config.getBoolean("native_survey_uninstall")

        remoteData.isInterOnSplash = config.getBoolean("is_inter_on_splash")

        if (isPurchased(this)) {
            remoteData.isNeedToShowADs = false
        }

        if (RemoteConfigdata(this).appOpenOn && RemoteConfigdata(this).isNeedToShowADs) AppOpenManager.getInstance()
            .enableAppResume()
        else AppOpenManager.getInstance().disableAppResume()

        if ((remoteData.nativeLang1ClickOn || remoteData.nativeLang2ClickOn) && remoteData.isNeedToShowADs) {
            val languageNativeId = if (isIntroFlowDone())
                AdsId.nativeLanguage2Click
            else AdsId.nativeLanguage1Click

            val tagName = "native_lang_click_tag"

            InfinityAdsManager.loadAd(
                this,
                languageNativeId,
                R.layout.layout_native_ad_large,
                tagName
            )
            Log.d("PreloadAd", "Language Click Native Preloading started")
        }

        loadInfinityFlow()
    }

}

