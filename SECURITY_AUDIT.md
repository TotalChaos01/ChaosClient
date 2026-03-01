# 🔒 Аудит безопасности Rise Client 5.99

## Дата аудита: 2026-02-28

## 🔴 КРИТИЧЕСКИЕ УГРОЗЫ

### 1. Email-шпионаж (MailUtil + PlayerUtil)
- **Файл:** `rise-main/dev/rise/util/mail/MailUtil.java`
- **Файл:** `rise-main/dev/rise/util/player/PlayerUtil.java` (строки 229-256)
- **Суть:** При обнаружении записей в hosts-файле Windows клиент **отправляет email** на `loginformationrise@gmail.com` с:
  - Имя пользователя Minecraft
  - IP сервера
  - Имя системы (hostname)
  - Intent аккаунт и UID
  - Discord тег
  - Последний залогиненный аккаунт
  - Статус Microsoft Login
- **Вердикт:** ⚠️ ШПИОНСКОЕ ПО. Собирает и отправляет личные данные.

### 2. HWID-проверка с бесконечными крашами (Validator.java)
- **Файл:** `rise-main/dev/rise/antipiracy/Validator.java`
- **Суть:** Обфусцированный URL (`intent.store/product/25/whitelist?hwid=`), при неудаче — бесконечный цикл, убивающий игру путём обнуления критических объектов (`entityRenderer = null`, `mouseHelper = null`).
- **Вердикт:** Вредоносное поведение — намеренное повреждение процесса.

### 3. Удалённый Kill Switch (KillSwitch.java)
- **Файл:** `rise-main/dev/rise/antipiracy/KillSwitch.java`
- **Суть:** Подключается к `pastebin.com/raw/PyAq9y1q` и может удалённо заблокировать клиент по решению разработчика, крашит игру обнулением `gameSettings`.
- **Вердикт:** Бэкдор. Разработчик может удалённо вырубить любую копию.

### 4. Скрытые HWID-проверки в утилитных классах
- **Файл:** `rise-main/dev/rise/util/misc/ClickUtil.java` — HWID-проверка в `static {}` блоке
- **Файл:** `rise-main/dev/rise/util/math/TimeUtil.java` — HWID-проверка замаскирована в утилите таймера
- **Файл:** `rise-main/dev/rise/util/alt/thealtening/SSLController.java` — HWID-проверка в SSL-коде
- **Файл:** `rise-main/dev/rise/util/alt/thealtening/service/FieldAdapter.java` — HWID-проверка в адаптере
- **Суть:** Скрытые проверки лицензии, разбросанные по утилитным классам. При неудаче — бесконечные циклы.
- **Вердикт:** Обфускация + бэкдоры.

## 🟡 ПОДОЗРИТЕЛЬНЫЕ КОМПОНЕНТЫ

### 5. Intent API интеграция (ProtectedLaunch.java)
- **Файл:** `rise-main/dev/rise/protection/ProtectedLaunch.java`
- **Суть:** Получает API-ключ, проверяет UID пользователя. Hardcoded developer UIDs.
- **Вердикт:** Система лицензирования — удалить полностью.

### 6. IntentGuard обфускация
- **Аннотации:** `@Native`, `@Exclude`, `@Bootstrap` из `store.intent.intentguard.annotation.*`
- **Суть:** Обфускатор, используемый для скрытия критического кода.
- **Вердикт:** Удалить все аннотации.

## ✅ ЧТО НЕ ОБНАРУЖЕНО

- ❌ Кража токенов Discord
- ❌ Чтение cookies/паролей браузера
- ❌ Кража файлов с диска
- ❌ Криптомайнер
- ❌ Загрузка/выполнение произвольного кода (кроме kill switch)
- ❌ Кейлоггер

## 📋 ДЕЙСТВИЯ ДЛЯ CHAOSCLIENT

Полностью **НЕ переносить** следующие файлы/пакеты:
- `antipiracy/*` — все файлы
- `protection/*` — все файлы
- `util/mail/*` — шпионское ПО
- `ui/auth/AuthGUI.java` — Intent авторизация
- Все `store.intent.*` импорты и зависимости

**Очистить** от HWID-проверок:
- `util/misc/ClickUtil.java`
- `util/math/TimeUtil.java`
- `util/alt/thealtening/SSLController.java`
- `util/alt/thealtening/service/FieldAdapter.java`

**Удалить** из `PlayerUtil.java`:
- Весь код чтения hosts-файла и отправки email (строки 229-256)

