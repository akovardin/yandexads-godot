extends Node

var ads: Object

func _init():
	if Engine.has_singleton("YandexAds"):
		ads = Engine.get_singleton("YandexAds")
		ads.enableLogging(true)
		ads.setAdGroupPreloading(true)
		
		ads.init()

		ads.loadBanner("demo-banner-yandex", {
			"size_type": "sticky",
			"width": 300
		})

		ads.loadInterstitial("demo-interstitial-yandex")
