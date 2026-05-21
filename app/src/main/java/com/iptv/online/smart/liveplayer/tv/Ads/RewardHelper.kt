/*
package com.iptv.online.smart.liveplayer.tv.adsutils

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.ads.module.ads.ERainAd
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.AdmobHelper.isPurchased
import com.ads.module.funtion.RewardCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd

@SuppressLint("StaticFieldLeak")
object RewardHelper {

    private var rewardedAd: RewardedAd? = null
    private var isEarned = false
    private var isLoading = false   // 🔥 NEW FLAG

    */
/** Load a reward every time this is called *//*

    fun preloadReward(activity: Activity) {

        // ❌ Ads disabled
        if (!RemoteConfigdata(activity).isNeedToShowADs) return

        if (!RemoteConfigdata(activity).rewardOn) return

        // ❌ Already loaded
        if (rewardedAd != null) return

        // ❌ Already loading → avoid duplicate
        if (isLoading) return

        isLoading = true  // 🔥 now loading has started

        ERainAd.getInstance().initRewardAds(
            activity,
            AdsId.rewardServer,
            object : AdCallback() {

                override fun onRewardAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false   // 🔥 loading completed
                    Log.d("RewardHelper", "Reward PRELOADED ✔")
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    rewardedAd = null
                    isLoading = false   // 🔥 loading completed (failed)
                    Log.e("RewardHelper", "Reward PRELOAD FAILED ❌")
                }
            }
        )
    }

    fun isRewardLoaded(): Boolean {
        return rewardedAd != null
    }

    */
/** Show reward only if:
     *  - Preloaded
     *  - VPN disconnected
     *//*

    fun showRewardIfAllowed(
        activity: Activity,
        vpnConnected: Boolean,
        onRewardDone: () -> Unit,
    ) {

        if (isPurchased(activity)){
            onRewardDone()
            return
        }

        if (RemoteConfigdata(activity).isNeedToShowADs) {
            // ❌ No reward loaded
            if (rewardedAd == null) {
//                Toast.makeText(activity, "Reward not ready!", Toast.LENGTH_SHORT).show()
                onRewardDone()
                return
            }

            // ❌ VPN connected
            if (vpnConnected) {
                Log.d("RewardHelper", "VPN connected → skip reward")
                onRewardDone()
                return
            }

            // 🎥 Show reward
            isEarned = false

            ERainAd.getInstance().showRewardAds(
                activity,
                rewardedAd,
                object : RewardCallback {

                    override fun onUserEarnedReward(item: RewardItem?) {
                        isEarned = true
                    }

                    override fun onRewardedAdClosed() {
                        if (isEarned) onRewardDone()

                        // 🔥 After showing → load next reward
                        rewardedAd = null
//                        if (AppHelper.IS_CONNECTED.not())
//                        preloadReward(activity)
                    }

                    override fun onRewardedAdFailedToShow(codeError: Int) {
                        rewardedAd = null
//                        preloadReward(activity)
                    }

                    override fun onAdClicked() {}
                }
            )
        } else {
            onRewardDone()
        }
    }
}
*/
