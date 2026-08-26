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
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences.set
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
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
    private var noInternetDialog: Dialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isRemoteConfigLoading = false

    // Home dabavi background karyu hoy tyare splash inter background ma show
    // na thay ne flow atki jato. Aa flag thi ad-show + next navigation ne
    // foreground (onResume) sudhi wait karavie -> pachi j ad dekhay ne aagal jay.
    private var splashResumed = false
    private var pendingProceed: (() -> Unit)? = null

    // Ek j var navigate thay (safety-net + ad callback banne goNextScreen kare to pan
    // 2 activity na khule). goNextScreen idempotent banave.
    private var hasNavigated = false

    private fun runWhenResumed(action: () -> Unit) {
        if (splashResumed) action() else pendingProceed = action
    }

    companion object {
        var screenCount = 1     // 👈 static thai gayu
    }

    override fun setViewBinding() = ActivitySplashBinding.inflate(layoutInflater)
    override fun onCreate(savedInstanceState: Bundle?) {
        setheader()
        super.onCreate(savedInstanceState)

        binding.btnSettings.isSelected = true
        appUpdate_Manager = AppUpdateManagerFactory.create(this)
        // Shortcut setup ne first frame pachi defer kariye — aa system ne IPC
        // call kare che, etle onCreate no critical path block na thay (ANR ghate).
        binding.root.post {
            manageAppShortcuts()
        }

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

            val shouldShowShortcuts = ERainAd.getInstance().getShouldDisplayWidgetUninstall(true) == true

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
                    }
                }
            })
    }

    override fun onPause() {
        super.onPause()
        splashResumed = false
    }

    protected override fun onResume() {
        super.onResume()
        splashResumed = true
        // Background ma jyare flow ready thai gayo hato (ad show / navigate) te
        // have foreground ma chalavie.
        pendingProceed?.let { proceed ->
            pendingProceed = null
            proceed()
        }

        // IPTV2 jevu safety-net: flow start thai gayo hoy pan splash ad fail/interrupt thai ne
        // callback na aavyo hoy to app splash par stuck rahe. onCheckShowSplashWhenFail ad FAIL
        // hoy TO J (showing ad ne interrupt karya vagar) 1 sec pachi jate aagal moklे.
        if (isFlowStarted && !hasNavigated) {
            ERainAd.getInstance().onCheckShowSplashWhenFail(this@SplashActivity, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    goNextScreen()
                }
            }, 1000)
        }

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

        // BadTokenException fix: Activity finish/destroy thai gayu hoy (async no-internet
        // callback pachi user niki gayo) to dialog show na karo -> "Unable to add window" crash na aave.
        if (!isFinishing && !isDestroyed) {
            noInternetDialog!!.show()
        }
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
//                loadingRemoteConfig()
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
        if (ERainAd.getInstance().getShouldDisplayWidgetUninstall(true) == true) {
            Log.d("AdManager123", "Widget flow active: Options will be shown.")
        } else {
            currentFlow = null
            Log.d("AdManager123", "Widget flow disabled: Moving to normal flow.")
        }
        if (remoteData.isNeedToShowADs) {
            val isUninstallFlow = currentFlow == "flow_uninstall"
            val shouldShowBanner = if (isUninstallFlow) {
                AdRemoteConfig.getInstance().banner_splash_uninstall.isEnable
            } else {
                AdRemoteConfig.getInstance().banner_splash.isEnable
            }
            if (remoteData.isNeedToShowADs && shouldShowBanner) {
                binding.bannerAdLayout.visible

                val bannerIdToUse = if (isUninstallFlow) {
                    Log.d("AdManager123", "loadInfinityFlow: " + "AdsId.BANNER_SPLASH_UNINSTALL")

                    AdRemoteConfig.getInstance().banner_splash_uninstall.id
                } else {
                    Log.d("AdManager123", "loadInfinityFlow: " + " AdsId.bannerSplash")

                    AdRemoteConfig.getInstance().banner_splash.id

                }

                ERainAd.getInstance().loadBanner(this@SplashActivity, bannerIdToUse)
            } else {
                binding.bannerAdLayout.gone
            }


            // Reviewer: splash nu interstitial FAKT EK J VAR load karvu.
            //   uninstall flow -> inter_splash_uninstall
            //   baki badhu (widget hoy ke na hoy) -> hammesha inter_splash
            // Pehla widget_flow == null / != null pramane alag-alag ternu call hatu,
            // ane isInterOnSplash false hoy tyare open_resume (App Open) vaparatu hatu -
            // e badhu kadhi nakhyu.
            val splashInterConfig = if (widgetFlow == "flow_uninstall") {
                AdRemoteConfig.getInstance().inter_splash_uninstall
            } else {
                AdRemoteConfig.getInstance().inter_splash
            }

            if (splashInterConfig.isEnable) {
                Log.w(TAG, "loadInfinityFlow: splash inter load (widgetFlow=$widgetFlow)")
                ERainAd.getInstance().loadSplashInterstitialAds(
                    this, splashInterConfig.id, 30000, 5000, object : AdCallback() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            // Reviewer: language ad no preload FAKT ahiya J -> inter
                            // dekhay e darmiyan language ad ready thai jay.
                            // Pan FAKT tyare J jyare next screen kharekhar LanguageActivity
                            // hoy (normal flow + non-purchased). flow_uninstall / flow_tv /
                            // flow_mirror / flow_iptv ke purchased user ma language screen
                            // aavti J nathi -> tya native_language request = waste request.
                            if (isGoingToLanguageScreen()) {
                                preloadLanguageAdInApp()
                            } else {
                                Log.w(
                                    TAG,
                                    "onAdLoaded: language preload skipped (next screen is not LanguageActivity)"
                                )
                            }
                        }

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
        // Background (home dabayelu) hoy to startActivity block thay -> foreground
        // (onResume) sudhi wait karavie, pachi j next screen kholie.
        if (!splashResumed) {
            pendingProceed = { goNextScreen() }
            return
        }
        // Ek j var navigate: real ad-callback ke resume safety-net, je pehla aave e j chale.
        if (hasNavigated) return
        hasNavigated = true
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
                    // Reviewer: ahiya preload NA karvo - fakt inter_splash.onAdLoaded() ma J.
                    Intent(this, LanguageActivity::class.java).apply {
                        putExtra("isFromSplash", true)
                    }
                }
            }
        }

        // Amuk device par startActivity vakhte system_server (WindowManager Transition)
        // ma NPE aave che — OS no bug che pan app crash kare. try-catch thi crash atkave.
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    // Reviewer: language ad ne inter_splash onAdLoaded() thi vhela preload karo (goNextScreen
    // ni badle). Aa flag thi ek j var chale.
    private var langPreloaded = false

    // Reviewer: goNextScreen() ma je condition LanguageActivity kholi aape che e J ahiya
    // mirror kari che. widget_flow (flow_uninstall / flow_tv / flow_mirror / flow_iptv) hoy
    // to language screen aavti nathi, ane purchased user MainActivity ma jay che -> aa
    // badha case ma native_language ad preload karvano koi matlab nathi.
    private fun isGoingToLanguageScreen(): Boolean {
        return when (intent.getStringExtra("widget_flow")) {
            "flow_uninstall", "flow_tv", "flow_mirror", "flow_iptv" -> false
            else -> !isPurchased(this)
        }
    }

    private fun preloadLanguageAdInApp() {
        if (langPreloaded) return
        langPreloaded = true
        // Pehli j vaar native_language_1, tya pachi hammesha native_language_2.
        // Aa J screenCount (companion) ne LanguageActivity vaapre -> lang ad ane
        // click ad banne same variant batave (consistent).
        // prev == 0 => sauthi pehli vaar -> _1. Pachi (prev 1 ke 2) -> hammesha _2.
        val sp = getSharedPreferences(packageName, 0)
        val prevLangCount = sp.getInt("lang_screen_count", 0)
        screenCount = if (prevLangCount == 0) 1 else 2
        sp.edit().putInt("lang_screen_count", screenCount).apply()

        AdsManager.loadNativeLanguage(this, screenCount)
    }
    private fun setConfigData(config: FirebaseRemoteConfig) {
        Log.i(TAG, "setConfigData: $config")

        // iptv2 jevu: aakho ad config JSON Firebase mathi lai AdRemoteConfig re-init karo.
        // debug -> "ad_rem_dup" (test ids), release -> "ad_remote_config" (live). Blank thay to asset.
        val adRemoteJson = if (BuildConfig.DEBUG) config.getString("ad_rem_dup")
        else config.getString("ad_remote_config")
        AdRemoteConfig.initialize(this, adRemoteJson)
        remoteData.delayButtonDoneLanguage = config.getBoolean("delay_button_done_language")
        remoteData.privacyLink = config.getString("privacy_policy")
        remoteData.height_button_cta = config.getString("height_button_cta")

        // iptv2 jevu: is_intro_page -> false hoy to onboarding skip. Firebase console ma key
        // HOY (VALUE_SOURCE_REMOTE) to j override -> nahi to key missing e getBoolean() false
        // aape ne onboarding kayamI skip thai jay (aa bug taalva mate).
        val introPageValue = config.getValue("is_intro_page")
        if (introPageValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            remoteData.isIntroPage = introPageValue.asBoolean()
        }

        // iptv2 jevu: show_full_native_close_btn -> fullscreen ad par close button on/off.
        // REMOTE guard: Firebase ma key hoy to j override, nahi to default true (close batavo,
        // nahi to aa project ma btnNext na hoy etle user trap thay).
        val closeBtnValue = config.getValue("show_full_native_close_btn")
        if (closeBtnValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            remoteData.isFullNativeCloseBtn = closeBtnValue.asBoolean()
        }

        // iptv2 jevu: is_in_app_update -> fakt fetch (use nathi, IPTV2 ma pan em j).
        remoteData.isInAppUpdate = config.getBoolean("is_in_app_update")

        // iptv2 jevu: language Done button delay (ms). REMOTE guard -> key hoy to j override,
        // nahi to default 2000 (2 sec).
        val delayTimeValue = config.getValue("time_delay_show_language_done_button")
        if (delayTimeValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
            remoteData.timeDelayLanguageDone = delayTimeValue.asLong()
        }

        // iptv2 jevu: is_all_ads_show -> fakt fetch (IPTV2 ma pan use nathi thatu, master
        // switch tarike wire NATHI karyu). Default true.
        remoteData.isAllAdsShow = config.getBoolean("is_all_ads_show")

        // ===== iptv2 jevu: badha ad on/off flags have JSON (AdRemoteConfig / ad_config.json)
        // mathi aave chhe -> ahiya Firebase mathi fetch KADHI NAKHYA chhe.
        // RemoteConfigdata na properties default 'true' aape chhe, etle ad gating have
        // JSON (AdRemoteConfig) par j depend kare. Firebase ma fakt meta keys rahe:
        //   delay_button_done_language, privacy_policy, height_button_cta,
        //   ad_remote_config / ad_rem_dup (JSON ad config), is_inter_on_splash sudhi na
        //   badha per-ad flags KADHI didha. =====

        if (isPurchased(this)) {
            remoteData.isNeedToShowADs = false
        }


        AppOpenManager.getInstance().disableAppResume()
        runWhenResumed { loadInfinityFlow() }
    }

}

