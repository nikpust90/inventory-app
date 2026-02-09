# inventory-app

## Краткое описание

Система инвентаризации со складами и документами: мобильный Android-клиент, веб-интерфейс и бэкенд на 1С.

## Функциональность

### Общие возможности

- Авторизация по PIN-коду
- Работа с документами инвентаризации: список, просмотр, обновление позиций
- Отчеты по остаткам складов
- Интеграции с 1С (бэкенд), Bitrix24, автомобильным API и Яндекс.Диском

### Android-клиент

- Просмотр и обработка документов инвентаризации
- Сканирование штрихкодов и серийных номеров
- Массовое сканирование для нескольких документов
- Работа со схемами автомобилей
- Отчет по остаткам в разрезе складов
- Загрузка фото в Яндекс.Диск с фоновыми задачами
- Отправка комментариев по заказам в Bitrix24

### Web-интерфейс

- PIN-авторизация
- Просмотр отчета по остаткам
- Работа со схемами автомобилей (категории, бренды, модели, поколения)
- Отправка комментариев в Bitrix24

## Стек

- Android: Java/Kotlin, Retrofit, Room, WorkManager
- Web: React, Axios, Nginx
- Backend: 1С (HTTP-сервисы)
- Инфраструктура: Docker/Docker Compose

## Структура

- `app/` — Android-клиент (Java/Kotlin, Room, Retrofit, WorkManager)
- `web/` — React-приложение
- `1C/` — бэкенд: HTTP-сервисы и логика 1С
- `gradle/`, `build.gradle`, `settings.gradle` — Android/Gradle конфигурация
- `web/docker-compose.yml`, `web/Dockerfile` — контейнеризация веб-части

## Быстрый старт (Android)

1. Откройте проект в Android Studio.
2. Убедитесь, что установлен Android SDK (minSdk 24, targetSdk 35).
3. Соберите и запустите приложение на устройстве или эмуляторе.

Альтернатива через Gradle:
```
./gradlew :app:assembleDebug
```

Готовый APK можно найти в `app/debug/`.

## Быстрый старт (Web)

```
cd web
npm install
npm start
```

Сборка:
```
npm run build
```

## Конфигурация API

Android:
- `app/src/main/java/com/example/inventory_app/ApiClient.java` — базовый URL и учетные данные для 1С
- `app/src/main/java/com/example/inventory_app/CarApiClient.java` — базовый URL Python API

Web:
- По умолчанию используется относительный `/api`.
- Можно задать `REACT_APP_API_URL` для явного адреса backend.

1С:
- Исходники HTTP-сервисов находятся в `1C/`.

## Деплой Web

Подробные инструкции:
- `web/QUICK_START.md`
- `web/DEPLOYMENT.md`

Быстро через Docker:
```
cd web
docker compose up -d --build
```


