package ru.kovardin.godotyandexads

import android.util.Log
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.instream.MobileInstreamAds
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot

class YandexAds(godot: Godot?) : GodotPlugin(godot) {
    val tag = "YandexAds"

    override fun getPluginName(): String {
        return "YandexAds"
    }

    @UsedByGodot
    fun init() {
        MobileAds.initialize(godot.requireContext()) {
            Log.d(tag, "ads initialized")
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
}