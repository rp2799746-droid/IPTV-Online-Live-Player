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
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.adsutils.AdRemoteConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences
import com.iptv.online.smart.liveplayer.tv.adsutils.EasyPreferences.set
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.BuildConfig
import com.iptv.online.smart.liveplayer.tv.adsutils.LanguageAdVariant
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigUtils
import com.iptv.online.smart.liveplayer.tv.app.AppConstants
import com.iptv.online.smart.liveplayer.tv.app.ResumeAdsEntryRule
import com.iptv.online.smart.liveplayer.tv.databinding.ActivitySplashBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible
import com.itg.iaumodule.IAdConsentCallBack
import com.itg.iaumodule.ITGAdConsent.loadAndShowConsent
import com.itg.iaumodule.ITGAdConsent.resetConsentDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest

internal fun shouldEnableAdsForPurchase(isPurchased: Boolean): Boolean = !isPurchased

class SplashActivity : Base__Activity<ActivitySplashBinding>(), RemoteConfigUtils.Listener {
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
    private var getConfigSuccess = false

    private fun runWhenResumed(action: () -> Unit) {
        if (splashResumed) action() else pendingProceed = action
    }

    companion object {
        private const val KEY_LANG_DONE = "lang_flow_done"
        private const val ADS_TAG = "AdManager123"

        fun isLanguageDone(context: Context): Boolean =
            context.getSharedPreferences(context.packageName, 0)
                .getBoolean(KEY_LANG_DONE, false)

        fun markLanguageDone(context: Context) {
            context.getSharedPreferences(context.packageName, 0)
                .edit().putBoolean(KEY_LANG_DONE, true).apply()
        }
    }


    override fun setViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        setheader()
        super.onCreate(savedInstanceState)

        binding.btnSettings.isSelected = true
        appUpdate_Manager = AppUpdateManagerFactory.create(this)

    }

    override fun bindObjects() {
        RemoteConfigUtils.init(this, this)
        handleIncomingLink(intent)
        prefs = EasyPreferences.defaultPrefs(this)

        if (!prefs!!.getBoolean(
                "KEY_IS_USER_GLOBAL",
                false
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
            try {
                val shortcutManager = getSystemService(ShortcutManager::class.java)
                val shouldShowShortcuts = ERainAd.getInstance().getShouldDisplayWidgetUninstall(
                    RemoteConfigUtils.getOnEnableUninstallWidget()
                ) == true

                if (shouldShowShortcuts) {
                    val shortcutList = mutableListOf<ShortcutInfo>()

                    val streamtv = ShortcutInfo.Builder(this, "stream_id")
                        .setShortLabel(getString(R.string.stream_your_tv))
                        .setIcon(Icon.createWithResource(this, R.drawable.stream_tv))
                        .setIntent(Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("widget_flow", "flow_iptv")
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        })
                        .build()

                    val mirrorShortcut = ShortcutInfo.Builder(this, "mirror_id")
                        .setShortLabel(getString(R.string.iptv_smart))
                        .setIcon(Icon.createWithResource(this, R.drawable.iptv_wid))
                        .setIntent(Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("ACTION_TYPE", "OPEN_IPTV")
                            putExtra("widget_flow", "flow_mirror")
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
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
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
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
                val connectivityManager =
                    getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(networkCallback!!)
            }
        } catch (_: Exception) {
        }
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
            ERainAd.getInstance()
                .onCheckShowSplashWhenFail(this@SplashActivity, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        Log.d(ADS_TAG, "➡️ [inter_splash] onCheckShowSplashWhenFail -> moveActivity()")
                        moveActivity()
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
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager?
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
        object : CountDownTimer(AppConstants.DEFAULT_TIME_SPLASH, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                if (getConfigSuccess && millisUntilFinished < AppConstants.DEFAULT_LIMIT_TIME_SPLASH) {
                    checkRemoteConfigResult()
                    cancel()
                }
            }

            override fun onFinish() {
                if (!getConfigSuccess) {
                    checkRemoteConfigResult()
                }
            }
        }.start()
    }

    private fun checkRemoteConfigResult() {
        if (isFlowStarted || isFinishing || isDestroyed) return
        isFlowStarted = true

        AdRemoteConfig.initialize(this, RemoteConfigUtils.getAdRemoteConfig())
        RemoteConfigUtils.applyAppSpecificConfig(this)
        remoteData.isNeedToShowADs = shouldEnableAdsForPurchase(isPurchased(this))
        if (remoteData.isInAppUpdate) check_AppUpdate()
        lifecycleScope.launch(Dispatchers.Default) { manageAppShortcuts() }

        val widgetFlow = intent.getStringExtra("widget_flow")
        var currentFlow = widgetFlow
 

        val isUninstallFlow = currentFlow == "flow_uninstall"
        val bannerUnit = if (isUninstallFlow) "banner_splash_uninstall_3009" else "banner_splash_3009"
        val interUnit = if (isUninstallFlow) "inter_splash_uninstall_3009" else "inter_splash_3009"
        Log.d(
            ADS_TAG,
            "🚦 [splash] flow=${currentFlow ?: "normal"} | uninstallFlow=$isUninstallFlow | isNeedToShowADs=${remoteData.isNeedToShowADs} | " +
                    "inter_splash(enable=${AdRemoteConfig.getInstance().inter_splash.isEnable}, id=${AdRemoteConfig.getInstance().inter_splash.id}) | " +
                    "inter_splash_uninstall(enable=${AdRemoteConfig.getInstance().inter_splash_uninstall.isEnable}, id=${AdRemoteConfig.getInstance().inter_splash_uninstall.id})"
        )
        val shouldShowBanner = if (isUninstallFlow) {
            AdRemoteConfig.getInstance().banner_splash_uninstall.isEnable
        } else {
            AdRemoteConfig.getInstance().banner_splash.isEnable
        }

        if (remoteData.isNeedToShowADs && shouldShowBanner) {
            binding.bannerAdLayout.visible
            val bannerIdToUse = if (isUninstallFlow) {
                AdRemoteConfig.getInstance().banner_splash_uninstall.id
            } else {
                AdRemoteConfig.getInstance().banner_splash.id
            }
            Log.d(ADS_TAG, "🔄 [$bannerUnit] Loading banner: id=$bannerIdToUse | uninstallFlow=$isUninstallFlow")
            ERainAd.getInstance().loadBanner(this@SplashActivity, bannerIdToUse, object : AdCallback() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    Log.d(ADS_TAG, "✅ [$bannerUnit] Banner loaded: id=$bannerIdToUse")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    Log.e(
                        ADS_TAG,
                        "❌ [$bannerUnit] Banner failed: id=$bannerIdToUse | code=${i?.code} | error=${i?.message}"
                    )
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d(ADS_TAG, "👆 [$bannerUnit] Banner clicked: id=$bannerIdToUse")
                }
            })
        } else {
            Log.w(
                ADS_TAG,
                "⏭️ [$bannerUnit] SKIP banner | reason=${if (!remoteData.isNeedToShowADs) "isNeedToShowADs=false" else "unit disabled in remote config"}"
            )
            binding.bannerAdLayout.gone
        }
        if (isGoingToLanguageScreen()) {
            loadNativeLanguage()
        }
        val splashInterConfig = if (currentFlow == "flow_uninstall") {
            AdRemoteConfig.getInstance().inter_splash_uninstall
        } else {
            AdRemoteConfig.getInstance().inter_splash
        }
        if (remoteData.isNeedToShowADs && splashInterConfig.isEnable && isNetworkAvailable(this@SplashActivity)) {
            Log.d(ADS_TAG, "🔄 [$interUnit] Loading splash inter: id=${splashInterConfig.id} | timeout=30000 delay=5000")
            ERainAd.getInstance().loadSplashInterstitialAds(
                this,
                splashInterConfig.id,
                30000,
                5000,
                object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        Log.d(ADS_TAG, "➡️ [$interUnit] Next action -> moveActivity()")
                        moveActivity()
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.d(ADS_TAG, "✅ [$interUnit] Splash inter loaded: id=${splashInterConfig.id}")
                    }

                    override fun onAdSplashReady() {
                        super.onAdSplashReady()
                        Log.d(ADS_TAG, "🎬 [$interUnit] Splash ad ready to show")
                    }

                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        Log.e(
                            ADS_TAG,
                            "❌ [$interUnit] Failed to load: id=${splashInterConfig.id} | code=${i?.code} | error=${i?.message}"
                        )
                    }

                    override fun onAdFailedToShow(adError: AdError?) {
                        super.onAdFailedToShow(adError)
                        Log.e(ADS_TAG, "❌ [$interUnit] Failed to show: error=${adError?.message}")
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        Log.d(ADS_TAG, "🚪 [$interUnit] Splash inter closed")
                    }
                })
        } else {
            val reason = when {
                !remoteData.isNeedToShowADs -> "isNeedToShowADs=false"
                !splashInterConfig.isEnable -> "unit disabled in remote config"
                else -> "no internet"
            }
            Log.w(ADS_TAG, "⏭️ [$interUnit] SKIP: id=${splashInterConfig.id} | reason=$reason -> moveActivity()")
            moveActivity()
        }

        if (ResumeAdsEntryRule.shouldEnableOpenResume()) {
            Log.d(ADS_TAG, "✅ [open_resume] Enabled: id=${AdRemoteConfig.getInstance().open_resume.id}")
            AppOpenManager.getInstance()
                .setAppResumeAdId(AdRemoteConfig.getInstance().open_resume.id)
            AppOpenManager.getInstance().enableAppResume()
        } else {
            Log.w(ADS_TAG, "⏭️ [open_resume] Disabled by ResumeAdsEntryRule")
            AppOpenManager.getInstance().disableAppResume()
        }
    }

    private fun handleClickConsent(canPersonalized: Boolean) {
        if (canPersonalized) {
            prefs!!.set("KEY_CONFIRM_CONSENT", true)
        } else {
            resetConsentDialog()
        }
    }

    private fun moveActivity() {
        if (!splashResumed) {
            pendingProceed = { moveActivity() }
            return
        }
        if (hasNavigated) return
        hasNavigated = true

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
                        putExtra(
                            LanguageAdVariant.EXTRA,
                            LanguageAdVariant.intentValue(prepareLanguageAdState())
                        )
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
            } catch (_: Exception) {
            }
        }
    }

    override fun bindListener() = Unit
    override fun bindMethod() = Unit

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
    }

    private val remoteData: RemoteConfigdata by lazy {
        RemoteConfigdata(this)
    }

    private fun isGoingToLanguageScreen(): Boolean {
        return when (intent.getStringExtra("widget_flow")) {
            "flow_uninstall", "flow_tv", "flow_mirror", "flow_iptv" -> false
            else -> !isPurchased(this)
        }
    }


    private fun prepareLanguageAdState(): LanguageAdVariant {
        return LanguageAdVariant.fromLanguageCompleted(isLanguageDone(this))
    }

    private fun loadNativeLanguage() {
        AdsManager.clearNativeLanguage()
        val variant = prepareLanguageAdState()
        AdsManager.loadNativeLanguage(
            this,
            variant == LanguageAdVariant.FIRST,
            if (variant == LanguageAdVariant.FIRST) R.layout.layout_native_ad_lang_1
            else R.layout.layout_native_ad_large
        )
    }

    override fun loadSuccess() {
        getConfigSuccess = true
    }
}