package com.iptv.online.smart.liveplayer.tv.Activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.AdsId
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.utils.CustomLoader
import java.util.Locale


abstract class Base__Activity<actBinding : ViewBinding> : AppCompatActivity() {

    open lateinit var binding: actBinding
    lateinit var mActivity: FragmentActivity
    lateinit var TAG: String
    private var _progressDialog: CustomLoader? = null

    open val needBackInterAd: Boolean = false
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("ffff", "Main onNewIntent called")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController =
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())


        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
        windowInsetsController.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        )
        binding = setViewBinding()
        setContentView(binding.root)

        mActivity = this
        TAG = "_${this::class.simpleName}"

        _progressDialog =
            CustomLoader(mActivity)



        bindObjects()
        bindListener()
        bindMethod()
        bindObserver()
        if (needBackInterAd) AdsManager.loadInterBack(this)
    }


    abstract fun setViewBinding(): actBinding
    abstract fun bindObjects()
    abstract fun bindListener()
    abstract fun bindMethod()
    open fun bindObserver() {}
    override fun onBackPressed() {
        AdsManager.showInterBack(this) {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }


    override fun attachBaseContext(newBase: Context) {
        val sp = newBase.getSharedPreferences(newBase.packageName, MODE_PRIVATE)
        val langCode = sp.getString("selected_language_code", "en") ?: "en"

        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val configuration = Configuration(newBase.resources.configuration)

        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            newBase.createConfigurationContext(configuration)
        } else {
            configuration.locale = locale
            newBase.resources.updateConfiguration(configuration, newBase.resources.displayMetrics)
            newBase
        }
        super.attachBaseContext(context)
    }

}