# Godot YandexAds

Плагин YandexAds под Godot для интеграции рекламы в приложение 

## Инициализация

Как инициализировать SDK

```gdscript
var ads: Object

func _init():
	if Engine.has_singleton("YandexAds"):
		ads = Engine.get_singleton("YandexAds")
		ads.enableLogging(true)
		ads.setAdGroupPreloading(true)
		
        # инициализация
		ads.init()

        # загрузка рекламы
		ads.loadBanner("demo-banner-yandex", {
			"size_type": "sticky",
			"width": 300
		})

		ads.loadInterstitial("demo-interstitial-yandex")
```

## Показ рекламы

Рекламу нужно загрузить и после этого можно вызывать методы для показа рекламы

```gdscript
ads.loadInterstitial("demo-interstitial-yandex")

# после загрузки можно показывать стишел 
ads.showInterstitial()
```