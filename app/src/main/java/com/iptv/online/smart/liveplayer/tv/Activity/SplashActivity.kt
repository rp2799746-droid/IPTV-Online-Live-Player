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
import androidx.lifecycle.lifecycleScope
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.google.android.gms.ads.LoadAdError
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
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.itg.iaumodule.IAdConsentCallBack
import com.itg.iaumodule.ITGAdConsent.loadAndShowConsent
import com.itg.iaumodule.ITGAdConsent.resetConsentDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class SplashActivity : Base__Activity<ActivitySplashBinding>() {
    private var isFlowStarted = false
    private var prefs: SharedPreferences? = null
    private var canPersonalized = true
    private var appUpdate_Manager: AppUpdateManager? = null
    val UPDATE_REQUEST_CODE: Int = 123
    private var noInternetDialog: Dialog? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var splashResumed = false
    private var pendingProceed: (() -> Unit)? = null
    private var hasNavigated = false
    private var langPreloaded = false

    private fun runWhenResumed(action: () -> Unit) {
        if (splashResumed) action() else pendingProceed = action
    }

    companion object {
        var screenCount = 1
    }

    override fun setViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        setheader()
        super.onCreate(savedInstanceState)

        binding.btnSettings.isSelected = true
        appUpdate_Manager = AppUpdateManagerFactory.create(this)

        // Shortcut setup background ma post karo jethi splash UI launch na rokay
        lifecycleScope.launch(Dispatchers.Default) {
            manageAppShortcuts()
        }
    }

    override fun bindObjects() {
        handleIncomingLink(intent)
        prefs = EasyPreferences.defaultPrefs(this)

        if (!prefs!!.getBoolean("KEY_IS_USER_GLOBAL", false) && !prefs!!.getBoolean("KEY_CONFIRM_CONSENT", false)) {
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
            try {
                val shortcutManager = getSystemService(ShortcutManager::class.java)
                val shouldShowShortcuts = ERainAd.getInstance().getShouldDisplayWidgetUninstall(true) == true

                if (shouldShowShortcuts) {
                    val shortcutList = mutableListOf<ShortcutInfo>()

                    val streamtv = ShortcutInfo.Builder(this, "stream_id")
                        .setShortLabel(getString(R.string.stream_your_tv))
                        .setIcon(Icon.createWithResource(this, R.drawable.stream_tv))
                        .setIntent(Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("widget_flow", "flow_iptv")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        })
                        .build()

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

                    val castShortcut = ShortcutInfo.Builder(this, "cast_id")
                        .setShortLabel(getString(R.string.cast_to_tv))
                        .setIcon(Icon.createWithResource(this, R.drawable.cast_wiget))
                        .setIntent(Intent(this, MirrorStepsActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("widget_flow", "flow_tv")
                        })
                        .build()

                    val uninstallShortcut = ShortcutInfo.Builder(this, "uninstall_id")
                        .setShortLabel(getString(R.string.uninstall))
                        .setIcon(Icon.createWithResource(this, R.drawable.uninstall_wid))
                        .setIntent(Intent(this, SplashActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("widget_flow", "flow_uninstall")
                        })
                        .build()

                    shortcutList.add(streamtv)
                    shortcutList.add(mirrorShortcut)
                    shortcutList.add(castShortcut)
                    shortcutList.add(uninstallShortcut)

                    shortcutManager.dynamicShortcuts = shortcutList
                } else {
                    shortcutManager.removeAllDynamicShortcuts()
                }
            } catch (e: Exception) {
                Log.e(TAG, "manageAppShortcuts error: ${e.message}")
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
                        runOnUiThread {
                            if (noInternetDialog != null && noInternetDialog!!.isShowing) {
                                noInternetDialog!!.dismiss()
                                loadingRemoteConfig()
                            }
                        }
                    }
                }
                connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (networkCallback != null) {
                val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (_: Exception) {}
    }

    private fun handleIncomingLink(intent: Intent?) {
        if (intent != null && intent.hasExtra("link")) {
            val link = intent.getStringExtra("link")
            if (!link.isNullOrEmpty()) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(browserIntent)
            }
        }
    }

    private fun setheader() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.statusBarColor = Color.TRANSPARENT
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002) {
            loadingRemoteConfig()
        }
    }

    private fun check_AppUpdate() {
        try {
            if (appUpdate_Manager == null) {
                appUpdate_Manager = AppUpdateManagerFactory.create(this)
            }

            appUpdate_Manager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo: AppUpdateInfo? ->
                if (appUpdateInfo != null && appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    try {
                        appUpdate_Manager?.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this@SplashActivity,
                            UPDATE_REQUEST_CODE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "check_AppUpdate error: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        splashResumed = false
    }

    override fun onResume() {
        super.onResume()
        splashResumed = true
        pendingProceed?.let { proceed ->
            pendingProceed = null
            proceed()
        }

        if (isFlowStarted && !hasNavigated) {
            ERainAd.getInstance().onCheckShowSplashWhenFail(this@SplashActivity, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    goNextScreen()
                }
            }, 1000)
        }

        if (isNetworkAvailable(this)) {
            if (noInternetDialog != null && noInternetDialog!!.isShowing) {
                noInternetDialog!!.dismiss()
                loadingRemoteConfig()
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connectivityManager != null) {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
        return false
    }

    fun Internet_Connection_Status(activity: Activity?) {
        showNoInternetBottomSheet()
    }

    private fun showNoInternetBottomSheet() {
        if (noInternetDialog != null && noInternetDialog!!.isShowing) return

        noInternetDialog = Dialog(this, R.style.CustomDialog)
        val view = layoutInflater.inflate(R.layout.dialog_no_internet, null)
        noInternetDialog!!.setContentView(view)
        noInternetDialog!!.setCancelable(false)

        view.findViewById<View?>(R.id.btn_settings)?.setOnClickListener {
            if (isNetworkAvailable(this@SplashActivity)) {
                noInternetDialog!!.dismiss()
                loadingRemoteConfig()
            } else {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        if (!isFinishing && !isDestroyed) {
            noInternetDialog!!.show()
        }
    }

    private fun checkNeedToLoadConsent() {
        loadAndShowConsent(true, object : IAdConsentCallBack {
            override fun getCurrentActivity(): Activity = this@SplashActivity
            override fun isDebug(): Boolean = BuildConfig.DEBUG
            override fun isUnderAgeAd(): Boolean = false

            override fun onConsentError(formError: FormError) {
                canPersonalized = true
                startAppFlow()
            }

            override fun onConsentStatus(consentStatus: Int) {
                canPersonalized = consentStatus != ConsentInformation.ConsentStatus.REQUIRED
            }

            override fun onConsentSuccess(b: Boolean) {
                canPersonalized = b
                handleClickConsent(canPersonalized)
                startAppFlow()
            }

            override fun onNotUsingAdConsent() {
                prefs!!.set("KEY_CONFIRM_CONSENT", true)
                canPersonalized = true
                startAppFlow()
            }

            override fun onRequestShowDialog() {}
            override fun testDeviceID(): String = "ED3576D8FCF2F8C52AD8E98B4CFA4005"
        })
    }

    private fun loadingRemoteConfig() {
        App.app?.initFirebaseConfigs(this)
        App.onRemoteFetched = { firebaseConfig ->
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
        var currentFlow = widgetFlow
        if (ERainAd.getInstance().getShouldDisplayWidgetUninstall(true) == true) {
            Log.d("AdManager123", "Widget flow active: Options will be shown.")
        } else {
            currentFlow = null
        }

        if (remoteData.isNeedToShowADs) {
            val isUninstallFlow = currentFlow == "flow_uninstall"
            val shouldShowBanner = if (isUninstallFlow) {
                AdRemoteConfig.getInstance().banner_splash_uninstall.isEnable
            } else {
                AdRemoteConfig.getInstance().banner_splash.isEnable
            }

            if (shouldShowBanner) {
                binding.bannerAdLayout.visible
                val bannerIdToUse = if (isUninstallFlow) {
                    AdRemoteConfig.getInstance().banner_splash_uninstall.id
                } else {
                    AdRemoteConfig.getInstance().banner_splash.id
                }
                ERainAd.getInstance().loadBanner(this@SplashActivity, bannerIdToUse)
            } else {
                binding.bannerAdLayout.gone
            }

            val splashInterConfig = if (widgetFlow == "flow_uninstall") {
                AdRemoteConfig.getInstance().inter_splash_uninstall
            } else {
                AdRemoteConfig.getInstance().inter_splash
            }

            if (splashInterConfig.isEnable) {
                // Timeout 30000 ms thi ghatadi ne 15000 ms karyo jethi App ANR na aave
                ERainAd.getInstance().loadSplashInterstitialAds(
                    this, splashInterConfig.id, 15000, 4000, object : AdCallback() {
                        override fun onAdLoaded() {
                            super.onAdLoaded()
                            if (isGoingToLanguageScreen()) {
                                preloadLanguageAdInApp()
                            }
                        }

                        override fun onNextAction() {
                            super.onNextAction()
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
        } else {
            resetConsentDialog()
        }
        loadingRemoteConfig()
    }

    private fun goNextScreen() {
        if (!splashResumed) {
            pendingProceed = { goNextScreen() }
            return
        }
        if (hasNavigated) return
        hasNavigated = true

        // Debug mode ma j print karo, production ma IPC/CPU time bachavo
        if (BuildConfig.DEBUG) {
            printHashKey()
        }

        val widgetFlow = intent.getStringExtra("widget_flow")
        val intent = when (widgetFlow) {
            "flow_uninstall" -> Intent(this, UninstallScreenActivity::class.java)
            "flow_tv", "flow_iptv" -> Intent(this, MainActivity::class.java).apply {
                putExtra("ACTION_TYPE", "OPEN_IPTV")
            }
            "flow_mirror" -> Intent(this, MirrorStepsActivity::class.java)
            else -> {
                if (isPurchased(this)) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, LanguageActivity::class.java).apply {
                        putExtra("isFromSplash", true)
                    }
                }
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    private fun printHashKey() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                for (signature in info.signatures!!) {
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                }
            } catch (_: Exception) {}
        }
    }

    override fun bindListener() = Unit
    override fun bindMethod() = Unit
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}

    private val remoteData: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }

    private fun isGoingToLanguageScreen(): Boolean {
        return when (intent.getStringExtra("widget_flow")) {
            "flow_uninstall", "flow_tv", "flow_mirror", "flow_iptv" -> false
            else -> !isPurchased(this)
        }
    }

    private fun preloadLanguageAdInApp() {
        if (langPreloaded) return
        langPreloaded = true

        lifecycleScope.launch(Dispatchers.IO) {
            val sp = getSharedPreferences(packageName, 0)
            val prevLangCount = sp.getInt("lang_screen_count", 0)
            screenCount = if (prevLangCount == 0) 1 else 2
            sp.edit().putInt("lang_screen_count", screenCount).apply()

            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    AdsManager.loadNativeLanguage(this@SplashActivity, screenCount)
                }
            }
        }
    }

    private fun setConfigData(config: FirebaseRemoteConfig) {
        lifecycleScope.launch(Dispatchers.IO) {
            val adRemoteJson = if (BuildConfig.DEBUG) config.getString("ad_rem_dup")
            else config.getString("ad_remote_config")

            // Heavy JSON Parsing in Background Thread
            AdRemoteConfig.initialize(this@SplashActivity, adRemoteJson)

            withContext(Dispatchers.Main) {
                remoteData.delayButtonDoneLanguage = config.getBoolean("delay_button_done_language")
                remoteData.privacyLink = config.getString("privacy_policy")
                remoteData.height_button_cta = config.getString("height_button_cta")

                val introPageValue = config.getValue("is_intro_page")
                if (introPageValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                    remoteData.isIntroPage = introPageValue.asBoolean()
                }

                val closeBtnValue = config.getValue("show_full_native_close_btn")
                if (closeBtnValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                    remoteData.isFullNativeCloseBtn = closeBtnValue.asBoolean()
                }

                remoteData.isInAppUpdate = config.getBoolean("is_in_app_update")

                val delayTimeValue = config.getValue("time_delay_show_language_done_button")
                if (delayTimeValue.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) {
                    remoteData.timeDelayLanguageDone = delayTimeValue.asLong()
                }

                remoteData.isAllAdsShow = config.getBoolean("is_all_ads_show")

                if (isPurchased(this@SplashActivity)) {
                    remoteData.isNeedToShowADs = false
                }

                AppOpenManager.getInstance().disableAppResume()
                runWhenResumed { loadInfinityFlow() }
            }
        }
    }
}