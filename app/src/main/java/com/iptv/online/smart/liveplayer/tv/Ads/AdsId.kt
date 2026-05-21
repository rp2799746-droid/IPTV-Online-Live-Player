package com.iptv.online.smart.liveplayer.tv.adsutils


object AdsId {

    // ---------------- TEST IDs (AdMob official) ----------------
    private const val TEST_BANNER = "/21775744923/example/fixed-size-banner"
    private const val TEST_INTER = "/21775744923/example/interstitial"
    private const val TEST_APPOPEN = "/21775744923/example/app-open"
    private const val TEST_NATIVE = "/21775744923/example/native"
    private const val TEST_Cbanner = "ca-app-pub-3940256099942544/9214589741"
    private const val TEST_REWARD = "/21775744923/example/rewarded"

    val isDebug: Boolean
        get() = false

    // ---------------- REAL IDs (fill from sheet) ----------------
    // Splash
    private const val INTER_SPLASH_REAL_NEW =
        "ca-app-pub-2864154863223892/7338519873"   // inter_splash
    private const val BANNER_SPLASH_REAL =
        "ca-app-pub-2864154863223892/2366025684"   // banner_splash
    private const val OPEN_RESUME_REAL =
        "ca-app-pub-2864154863223892/9493127079"   // open_resume (app open ad)

    private const val SPLASH_OPEN_AD = "ca-app-pub-2864154863223892/9493127079"

    // Language screen
    private const val NATIVE_LANGUAGE_1_REAL = "ca-app-pub-2864154863223892/8769548860"
    private const val NATIVE_LANGUAGE_2_REAL = "ca-app-pub-2864154863223892/2062142460"
    private const val NATIVE_LANGUAGE_1_CLICK_REAL = "ca-app-pub-2864154863223892/6143385528"
    private const val NATIVE_LANGUAGE_2_CLICK_REAL = "ca-app-pub-2864154863223892/1493694077"

    // Onboarding
    private const val NATIVE_ONBOARDING_1_1_REAL = "ca-app-pub-2864154863223892/9290526689"
    private const val NATIVE_ONBOARDING_2_1_REAL = "ca-app-pub-2864154863223892/4800617334"
    private const val NATIVE_ONBOARDING_FULL_1_REAL = "ca-app-pub-2864154863223892/3295963974"
    private const val NATIVE_ONBOARDING_1_4_REAL = "ca-app-pub-2864154863223892/4038200002"
    private const val NATIVE_ONBOARDING_FULL_2_REAL = "ca-app-pub-2864154863223892/8603500089"
    private const val NATIVE_ONBOARDING_2_4_REAL = "ca-app-pub-2864154863223892/4496734116"
    private const val INTER_ONBOARDING_REAL = "ca-app-pub-2864154863223892/5730555625"

    // Home
    private const val NATIVE_HOME_REAL = "ca-app-pub-2864154863223892/9478228940"
    private const val INTER_HOME_REAL = "ca-app-pub-2864154863223892/3207703179"
    private const val INTER_BACK = "ca-app-pub-2864154863223892/9581539835"

    // Collapsible banner
    private const val BANNER_COLLAPSE_home_REAL = "ca-app-pub-2864154863223892/7785873321"

    // Reward
//    private const val REWARD_SERVER_REAL = "/21775744923/example/rewarded"

    //native_channel
    private const val NATIVE_CHANNEL_REAL = "ca-app-pub-2864154863223892/1820997138"
    private const val NATIVE_FAVORITE_REAL = "ca-app-pub-2864154863223892/5159709986"
    private const val NATIVE_HOSTORY_REAL = "ca-app-pub-2864154863223892/3918149616"
    private const val NATIVE_PLAYLIST_REAL = "ca-app-pub-2864154863223892/3561318166"
    private const val INTER_MIRRORING_REAL = "ca-app-pub-2864154863223892/1264672324"
    private const val NATIVE_MIRRORING_REAL = "ca-app-pub-2864154863223892/7638508982"
    private const val NATIVE_CHANNELLIST_REAL = "ca-app-pub-2864154863223892/6325427310"
    private const val INTER_ADD_PLAYLIST_REAL = "ca-app-pub-2864154863223892/1204128423"
    private const val INTER_SPLASH_UNINSTALL_REAL = "ca-app-pub-2864154863223892/6002205712"
    private const val BANNER_SPLASH_UNINSTALL_REAL = "ca-app-pub-2864154863223892/5012345649"
    private const val NATIVE_UNINSTALL_REAL = "ca-app-pub-2864154863223892/2248236490"
    private const val NATIVE_SURVEY_UNINSTALL_REAL = "ca-app-pub-2864154863223892/9935154824"


    // ---------------- PUBLIC IDs (auto-debug switch) ----------------
    // Splash
    @JvmStatic
    val interSplash: String
        get() = if (isDebug) TEST_INTER else INTER_SPLASH_REAL_NEW
    val openSplash: String
        get() = if (isDebug) TEST_APPOPEN else SPLASH_OPEN_AD

    @JvmStatic
    val bannerSplash: String
        get() = if (isDebug) TEST_BANNER else BANNER_SPLASH_REAL

    @JvmStatic
    val openResume: String
        get() = if (isDebug) TEST_APPOPEN else OPEN_RESUME_REAL

    // Language
    @JvmStatic
    val nativeLanguage1: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_LANGUAGE_1_REAL

    @JvmStatic
    val nativeLanguage2: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_LANGUAGE_2_REAL

    @JvmStatic
    val nativeLanguage1Click: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_LANGUAGE_1_CLICK_REAL

    @JvmStatic
    val nativeLanguage2Click: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_LANGUAGE_2_CLICK_REAL

    // Onboarding
    @JvmStatic
    val nativeOnboarding1_1: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_1_1_REAL

    // Onboarding
    @JvmStatic
    val nativeOnboarding2_1: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_2_1_REAL

    @JvmStatic
    val nativeOnboardingFull1: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_FULL_1_REAL

    @JvmStatic
    val nativeOnboarding1_4: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_1_4_REAL

    @JvmStatic
    val nativeOnboardingFull2: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_FULL_2_REAL

    @JvmStatic
    val nativeOnboarding2_4: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_ONBOARDING_2_4_REAL

    @JvmStatic
    val interOnboarding: String
        get() = if (isDebug) TEST_INTER else INTER_ONBOARDING_REAL

    // Home
    @JvmStatic
    val nativeHome: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_HOME_REAL

    @JvmStatic
    val interHome: String
        get() = if (isDebug) TEST_INTER else INTER_HOME_REAL

    // Collapsible banner
    @JvmStatic
    val banner_collap_home: String
        get() = if (isDebug) TEST_Cbanner else BANNER_COLLAPSE_home_REAL

    // Reward
    /*
        @JvmStatic
        val rewardServer: String
            get() = if (isDebug) TEST_REWARD else REWARD_SERVER_REAL
    */


    @JvmStatic
    val interback: String
        get() = if (isDebug) TEST_INTER else INTER_BACK

    @JvmStatic
    val NATIVE_CHANNEL: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_CHANNEL_REAL

    @JvmStatic
    val NATIVE_FAVORITE: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_FAVORITE_REAL

    @JvmStatic
    val NATIVE_HOSTORY: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_HOSTORY_REAL

    @JvmStatic
    val NATIVE_PLAYLIST: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_PLAYLIST_REAL

    @JvmStatic
    val INTER_MIRRORING: String
        get() = if (isDebug) TEST_INTER else INTER_MIRRORING_REAL

    @JvmStatic
    val NATIVE_MIRRORING: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_MIRRORING_REAL

    @JvmStatic
    val NATIVE_CHANNELLIST: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_CHANNELLIST_REAL

    @JvmStatic
    val INTER_ADD_PLAYLIST: String
        get() = if (isDebug) TEST_INTER else INTER_ADD_PLAYLIST_REAL

    @JvmStatic
    val INTER_SPLASH_UNINSTALL: String
        get() = if (isDebug) TEST_INTER else INTER_SPLASH_UNINSTALL_REAL

    @JvmStatic
    val BANNER_SPLASH_UNINSTALL: String
        get() = if (isDebug) TEST_BANNER else BANNER_SPLASH_UNINSTALL_REAL

    @JvmStatic
    val NATIVE_UNINSTALL: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_UNINSTALL_REAL

    @JvmStatic
    val NATIVE_SURVEY_UNINSTALL: String
        get() = if (isDebug) TEST_NATIVE else NATIVE_SURVEY_UNINSTALL_REAL


}
