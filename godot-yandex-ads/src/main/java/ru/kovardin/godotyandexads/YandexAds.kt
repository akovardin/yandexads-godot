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
import com.yandex.mobile.ads.banner.AdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.instream.MobileInstreamAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.rewarded.RewardedAd
import org.godotengine.godot.Dictionary
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class YandexAds(godot: Godot?) : GodotPlugin(godot) {
    val tag = "YandexAds"

    private var layout: FrameLayout? = null
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

        signals.add(SignalInfo("banner_loaded"))
        signals.add(SignalInfo("banner_failed_to_load", Integer::class.java))
        signals.add(SignalInfo("banner_ad_clicked"))
        signals.add(SignalInfo("banner_left_application"))
        signals.add(SignalInfo("banner_returned_to_application"))
        signals.add(SignalInfo("banner_on_impression", String::class.java))
        return signals
    }

    override fun onMainCreate(activity: Activity): View? {
        layout = FrameLayout(activity)
        return layout
    }

    @UsedByGodot
    fun init() {
        MobileAds.initialize(godot.requireContext()) {
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

    private fun createBanner(id: String, params: Dictionary): BannerAdView {
        layout = activity!!.window.decorView.rootView as FrameLayout

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
                banner.setAdSize(AdSize.inlineSize(params[BANNER_WIDTH] as Int, params[BANNER_HEIGHT] as Int))
            BANNER_STICKY_SIZE ->
                banner.setAdSize(AdSize.stickySize(godot!!.requireContext(), params[BANNER_WIDTH] as Int))
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
                Log.w(tag, "YandexAds: onLeftApplication")
                emitSignal("banner_left_application")
            }

            override fun onReturnedToApplication() {
                Log.w(tag, "YandexAds: onReturnedToApplication")
                emitSignal("banner_returned_to_application")
            }

            override fun onImpression(impressionData: ImpressionData?) {
                Log.w("godot", "YandexAds: onBannerAdImpression");
                emitSignal("banner_on_impression", impressionData?.getRawData());
            }
        })

        layout?.addView(banner, layoutParams);
        banner.loadAd(request());

        return banner
    }

    @UsedByGodot
    fun loadBanner(id: String, params: Dictionary)  {
        activity?.runOnUiThread {
            if (banner == null) {
                banner = createBanner(id, params)
            } else {
                banner?.loadAd(request())
            }
        }
    }

    fun showBanner() {

    }

    private fun getSafeArea(): Rect {
        val safeInsetRect = Rect()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return safeInsetRect
        }
        val windowInsets: WindowInsets = activity!!.getWindow().getDecorView().getRootWindowInsets()
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