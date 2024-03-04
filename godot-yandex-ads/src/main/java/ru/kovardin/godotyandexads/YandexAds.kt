package ru.kovardin.godotyandexads

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.util.ArraySet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.instream.MobileInstreamAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class YandexAds(godot: Godot?) : GodotPlugin(godot) {
    val tag = "YandexAds"

    private lateinit var layout: FrameLayout
    private var layoutParams: FrameLayout.LayoutParams? = null

    private var interstitial: InterstitialAd? = null
    private var banner: BannerAdView? = null
    private var rewarded: RewardedAd? = null

    override fun getPluginName(): String {
        return "YandexAds"
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = ArraySet()
        signals.add(SignalInfo("ads_initialized"))
        // banner
        signals.add(SignalInfo("banner_loaded"))
        signals.add(SignalInfo("banner_failed_to_load", Integer::class.java))
        signals.add(SignalInfo("banner_ad_clicked"))
        signals.add(SignalInfo("banner_left_application"))
        signals.add(SignalInfo("banner_returned_to_application"))
        signals.add(SignalInfo("banner_on_impression", String::class.java))
        // interstitial
        signals.add(SignalInfo("interstitial_loaded"))
        signals.add(SignalInfo("interstitial_failed_to_load", Integer::class.java))
        signals.add(SignalInfo("interstitial_failed_to_show", Integer::class.java))
        signals.add(SignalInfo("interstitial_ad_shown"))
        signals.add(SignalInfo("interstitial_ad_dismissed"))
        signals.add(SignalInfo("interstitial_ad_clicked"))
        signals.add(SignalInfo("interstitial_on_impression", String::class.java))
        //rewarded
        signals.add(SignalInfo("rewarded_loaded"))
        signals.add(SignalInfo("rewarded_failed_to_load", Integer::class.java))
        signals.add(SignalInfo("rewarded_ad_shown"))
        signals.add(SignalInfo("rewarded_ad_dismissed"))
        signals.add(SignalInfo("rewarded_rewarded", Dictionary::class.java))
        signals.add(SignalInfo("rewarded_ad_clicked"))
        signals.add(SignalInfo("rewarded_on_impression", String::class.java))
        return signals
    }

    override fun onMainCreate(activity: Activity): View? {
        layout = FrameLayout(activity)
        return layout
    }

    @UsedByGodot
    fun init() {
        MobileAds.initialize(godot.getActivity()!!) {
            emitSignal("ads_initialized")
        }
    }

    @UsedByGodot
    fun enableLogging(value: Boolean) {
        MobileAds.enableLogging(value)
    }

    @UsedByGodot
    fun setAdGroupPreloading(value: Boolean) {
        MobileInstreamAds.setAdGroupPreloading(value)
    }

    @UsedByGodot
    fun setUserConsent(value: Boolean) {
        MobileAds.setUserConsent(value)
    }

    @UsedByGodot
    fun setLocationConsent(value: Boolean) {
        MobileAds.setLocationConsent(value)
    }

    @UsedByGodot
    fun setAgeRestrictedUser(value: Boolean) {
        MobileAds.setAgeRestrictedUser(value)
    }

    private fun request(): AdRequest {
        return AdRequest.Builder().build()
    }

    @UsedByGodot
    fun loadBanner(id: String, params: Dictionary)  {
        godot.getActivity()?.runOnUiThread {
            if (banner == null) {
                banner = createBanner(id, params)
            } else {
                banner?.loadAd(request())
            }
        }
    }

    private fun createBanner(id: String, params: Dictionary): BannerAdView {
        layout = godot.getActivity()!!.window.decorView.rootView as FrameLayout

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        val banner = BannerAdView(activity!!)

        var position = POSITION_BOTTOM
        if (params.containsKey(BANNER_POSITION)) {
            position = params[BANNER_POSITION] as Int
        }

        var safearea = true
        if (params.containsKey(BANNER_SAFE_AREA)) {
            safearea = params[BANNER_SAFE_AREA] as Boolean
        }

        if (position == POSITION_BOTTOM) {
            layoutParams?.gravity = Gravity.BOTTOM
            if (safearea) banner.y = (-getSafeArea().bottom).toFloat()

        } else if (position == POSITION_TOP) {
            layoutParams?.gravity = Gravity.TOP
            if (safearea) banner.y = getSafeArea().top.toFloat()
        }

        banner.setAdUnitId(id);
        banner.setBackgroundColor(Color.TRANSPARENT);

        when (params[BANNER_SIZE_TYPE]) {
            BANNER_INLINE_SIZE ->
                banner.setAdSize(BannerAdSize.inlineSize(godot.getActivity()!!, params[BANNER_WIDTH] as Int, params[BANNER_HEIGHT] as Int))
            BANNER_STICKY_SIZE ->
                banner.setAdSize(BannerAdSize.stickySize(godot.getActivity()!!, params[BANNER_WIDTH] as Int))
        }

        banner.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
                Log.w("godot", "YandexAds: onBannerAdLoaded")
                emitSignal("banner_loaded")
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.w(tag, "YandexAds: onBannerAdFailedToLoad. Error: " + error.code)
                emitSignal("banner_failed_to_load", error.code)
            }

            override fun onAdClicked() {
                Log.w(tag, "YandexAds: onBannerAdClicked")
                emitSignal("banner_ad_clicked")
            }

            override fun onLeftApplication() {
                Log.w(tag, "YandexAds: onBannerLeftApplication")
                emitSignal("banner_left_application")
            }

            override fun onReturnedToApplication() {
                Log.w(tag, "YandexAds: onBannerReturnedToApplication")
                emitSignal("banner_returned_to_application")
            }

            override fun onImpression(impression: ImpressionData?) {
                Log.w("godot", "YandexAds: onBannerAdImpression");
                emitSignal("banner_on_impression", impression?.rawData.orEmpty());
            }
        })

        layout.addView(banner, layoutParams);
        banner.loadAd(request());

        return banner
    }

    @UsedByGodot
    fun removeBanner() {
        godot.getActivity()?.runOnUiThread {
            if (banner != null) {
                layout.removeView(banner) // Remove the banner
                Log.d(tag, "removeBanner: banner ok")
            } else {
                Log.w(tag, "removeBanner: banner not found")
            }
        }
    }

    @UsedByGodot
    fun showBanner() {
        godot.getActivity()?.runOnUiThread {
            if (banner != null) {
                banner?.visibility = View.VISIBLE
                Log.d(tag, "showBanner: banner ok")
            } else {
                Log.w(tag, "showBanner: banner not found")
            }
        }
    }

    @UsedByGodot
    fun hideBanner() {
        if (banner != null) {
            banner?.visibility = View.GONE
            Log.d(tag, "hideBanner: banner ok")
        } else {
            Log.w(tag, "hideBanner: banner not found")
        }
    }

    @UsedByGodot
    fun loadInterstitial(id: String) {
        godot.getActivity()?.runOnUiThread {
            createInterstitial(id)
        }
    }

    private fun createInterstitial(id: String) {
        val loader = InterstitialAdLoader(godot.getActivity()!!)
        loader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                Log.w(tag, "onAdLoaded")

                emitSignal("interstitial_loaded")

                interstitial?.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        Log.w(tag, "onAdShown")
                        emitSignal("interstitial_ad_shown")
                    }

                    override fun onAdFailedToShow(error: AdError) {
                        Log.w(tag, "onAdFailedToShow: ${error.description}")
                        emitSignal("interstitial_failed_to_show", error.description)
                    }

                    override fun onAdDismissed() {
                        Log.w(tag, "onAdDismissed")
                        emitSignal("interstitial_ad_dismissed")
                    }

                    override fun onAdClicked() {
                        Log.w(tag, "onAdClicked")
                        emitSignal("interstitial_ad_clicked")
                    }

                    override fun onAdImpression(data: ImpressionData?) {
                        Log.w(tag, "onAdImpression: ${data?.rawData.orEmpty()}")
                        emitSignal("interstitial_on_impression", data?.rawData.orEmpty())
                    }
                })
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.w(tag, "onAdFailedToLoad. error: " + error.code)
                emitSignal("interstitial_failed_to_load", error.description)
            }
        })
        loader.loadAd(AdRequestConfiguration.Builder(id).build())
    }

    @UsedByGodot
    fun showInterstitial() {
        godot.getActivity()?.runOnUiThread {
            if (interstitial != null) {
                interstitial?.show(godot.getActivity()!!)
            } else {
                Log.w(tag, "showInterstitial");
            }
        }
    }

    @UsedByGodot
    fun loadRewarded(id: String) {
        godot.getActivity()?.runOnUiThread {
            createRewarded(id)
        }
    }

    private fun createRewarded(id: String) {
        val loader = RewardedAdLoader(godot.getActivity()!!)

        loader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad

                Log.w(tag, "onAdLoaded")

                emitSignal("rewarded_loaded")

                rewarded?.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {
                        Log.w(tag, "onAdShown")
                        emitSignal("rewarded_ad_shown")
                    }

                    override fun onAdFailedToShow(error: AdError) {
                        Log.w(tag, "onAdFailedToShow. error: ${error.description}")
                        emitSignal("rewarded_ad_shown")
                    }

                    override fun onAdDismissed() {
                        Log.w(tag, "onAdDismissed")
                        emitSignal("rewarded_ad_dismissed")
                    }

                    override fun onRewarded(reward: Reward) {
                        Log.w(tag, "YandexAds: onRewarded")
                        val data = Dictionary()
                        data.set("amount", reward.amount)
                        data.set("type", reward.type)
                        emitSignal("rewarded_rewarded", data)
                    }

                    override fun onAdClicked() {
                        Log.w(tag, "onAdClicked")
                        emitSignal("rewarded_ad_clicked")
                    }

                    override fun onAdImpression(impression: ImpressionData?) {
                        Log.w(tag, "onAdImpression")
                        emitSignal("rewarded_on_impression", impression?.rawData.orEmpty())
                    }

                })
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.w(tag, "onAdFailedToLoad. error: " + error.code)
                emitSignal("rewarded_failed_to_load", error.description)
            }

        })
        loader.loadAd(AdRequestConfiguration.Builder(id).build())
    }

    @UsedByGodot
    fun showRewarded() {
        godot.getActivity()?.runOnUiThread {
            if (rewarded != null) {
                rewarded?.show(godot.getActivity()!!)
            } else {
                Log.w(tag, "showRewarded");
            }
        }
    }

    private fun getSafeArea(): Rect {
        val safeInsetRect = Rect()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return safeInsetRect
        }
        val windowInsets: WindowInsets = godot.getActivity()!!.getWindow().getDecorView().getRootWindowInsets()
            ?: return safeInsetRect
        val displayCutout = windowInsets.displayCutout
        if (displayCutout != null) {
            safeInsetRect[displayCutout.safeInsetLeft, displayCutout.safeInsetTop, displayCutout.safeInsetRight] =
                displayCutout.safeInsetBottom
        }
        return safeInsetRect
    }

    companion object {
        const val POSITION_TOP = 1
        const val POSITION_BOTTOM = 0

        const val BANNER_STICKY_SIZE = "sticky"
        const val BANNER_INLINE_SIZE = "inline"

        const val BANNER_POSITION = "position"
        const val BANNER_SAFE_AREA = "safe_area"
        const val BANNER_WIDTH = "width"
        const val BANNER_HEIGHT = "height"
        const val BANNER_SIZE_TYPE = "size_type"
    }
}