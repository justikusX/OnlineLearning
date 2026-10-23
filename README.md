# Online Learning (Android)

Приложение по макету Figma «Online Learning App Design (Community)».
Kotlin + Jetpack Compose, **Retrofit** — каталог курсов, **Firebase** — Auth и Firestore.

## Архитектура: MVVM

Код разделён на три пакета по слоям **Model — View — ViewModel**:

```
com.example.onlinelearning
├── model/          M — данные и бизнес-логика, ничего не знает об интерфейсе
│   ├── entity/     Course, Lesson, UserProfile, AppNotification, LearningStats
│   ├── remote/     CourseApi (Retrofit)
│   ├── repository/ AuthRepository, UserRepository, CourseRepository, SessionRepository, ThemePreferences
│   ├── player/     LessonPlayback: ExoPlayer и TextToSpeech
│   └── util/       AvatarCodec, FirebaseErrors
├── viewmodel/      VM — состояние экранов (StateFlow) и действия пользователя
│   ├── AppViewModelFactory.kt
│   ├── SessionViewModel, AuthViewModel, PhoneAuthViewModel, ThemeViewModel
│   ├── HomeViewModel, CoursesViewModel, CourseDetailViewModel
│   └── MessagesViewModel, AccountViewModel
├── view/           V — Jetpack Compose: только отображение состояния и передача событий во ViewModel
│   ├── AppNavigation.kt, screens/, components/, theme/
├── LearningApp.kt  AppContainer — создаёт репозитории один раз на процесс
└── MainActivity.kt
```

Правила, которых придерживается код:

- **View** (экраны Compose) не обращается к Firebase, Retrofit и репозиториям. Экран получает свой ViewModel,
  подписывается на `state` через `collectAsStateWithLifecycle()` и вызывает методы ViewModel
  (`viewModel.play(lesson)`, `viewModel.buy()`, `viewModel.onEmailChange(...)`). В экранах остаётся только
  чисто визуальное состояние: открыт ли фильтр, положение ползунка во время перетаскивания.
- **ViewModel** хранит состояние экрана в `StateFlow` (`HomeUiState`, `CourseDetailUiState`, `PaymentState`, …),
  собирает его из репозиториев (`combine`) и выполняет действия в `viewModelScope`. Одноразовые события
  (сообщения для Snackbar, «Clocking in!») отдаются через `SharedFlow`. ViewModel не знает о Compose-экранах.
- **Model** — репозитории и сущности. `SessionRepository` держит одного слушателя профиля Firestore на всё
  приложение, `CourseRepository.catalog` — один каталог для всех экранов.
- ViewModel создаются фабрикой `AppViewModelFactory`, которая берёт репозитории из `AppContainer`.
  `CourseDetailViewModel` живёт, пока открыта страница курса: id курса он читает из маршрута через
  `SavedStateHandle`, а при закрытии страницы останавливает плеер в `onCleared()`.

| Слой | Что делает |
|---|---|
| `model/remote/CourseApi.kt` | Retrofit + OkHttp: `GET {COURSES_API_BASE_URL}courses.json` (по умолчанию — файл `hosting/public/api/courses.json` прямо из этого репозитория на GitHub) |
| `model/repository/CourseRepository.kt` | Каталог в `StateFlow`; если API недоступен — берёт `assets/courses.json` |
| `model/repository/AuthRepository.kt` | Firebase Auth: вход, регистрация, Google, телефон, сброс пароля |
| `model/repository/UserRepository.kt` | Firestore `users/{uid}`: избранное, покупки, прогресс уроков, уведомления, аватар |
| `model/repository/SessionRepository.kt` | Текущий пользователь и его профиль для всех ViewModel |
| `model/util/AvatarCodec.kt` | Фото из галереи (Photo Picker) → квадрат 256×256 JPEG в Base64 в профиле Firestore, без Firebase Storage |
| `model/player/LessonPlayback.kt` | Воспроизведение уроков: аудио и видео (Media3 ExoPlayer) и озвучка текста (TextToSpeech) |
| `viewmodel/*` | По ViewModel на экран или группу экранов (см. схему выше) |
| `view/screens/*` | Экраны из макета Figma: онбординг, Log In / Sign Up (+ Google и телефон), Continue with Phone / Verify Phone, Home (Learned today, Learning Plan, Meetup), Course (категории, All/Popular/New), Search + Search Filter, карточка курса (BESTSELLER, видео в шапке), Payment Method / Payment Password / Successful purchase, Clocking in!, My courses (сетка), Notifications (message / notification), Account (Favourite, Edit Account, Settings and Privacy, Help) |

## Уроки

Все уроки играют внутри приложения, урок засчитывается, когда пройдено 90%. У урока есть один из источников:

- `audioUrl` — аудиозапись (стрим);
- `videoUrl` — видео (стрим, показывается в плеере);
- `text` — текст урока, который телефон читает вслух встроенным синтезом речи (нужен английский голос; работает офлайн).

| Курс | Источник |
|---|---|
| Design & Craft | *Arts and Crafts Essays* (1893), LibriVox — общественное достояние |
| Color Theory | *A Color Notation* (1905), Albert H. Munsell, LibriVox — общественное достояние |
| The Story of Photography | *The Romance of Modern Photography* (1908), LibriVox — общественное достояние |
| History of Computing | *Passages from the Life of a Philosopher* (1864), Charles Babbage, LibriVox — общественное достояние |
| Java Development | Видеолекции Stanford CS106A «Programming Methodology» (Mehran Sahami), CC BY-NC-SA — поэтому курс бесплатный |
| Android with Kotlin | Собственные тексты уроков, озвучка синтезом речи на устройстве |

Каталог API по умолчанию берётся из этого репозитория (`raw.githubusercontent.com/.../hosting/public/api/`),
так что после `git push` в `main` приложение видит изменения без деплоя.

## Настройка Firebase

1. Проект Firebase — `mobileapp-252b2`, Android-приложение с пакетом `com.example.onlinelearning`.
2. `app/google-services.json` уже лежит в репозитории (для своего проекта замените его файлом из Firebase Console).
3. Authentication → Sign-in method → включите **Email/Password**, **Google** и **Phone**.
   - Для Google и телефона в Project settings → Your apps добавьте **SHA-1 и SHA-256** ключа, которым подписано
     приложение (`./gradlew signingReport`, вариант `debug`).
   - Для проверки входа по телефону без SMS добавьте тестовый номер: Authentication → Sign-in method → Phone →
     Phone numbers for testing (например, `+7 900 000 00 00` с кодом `123456`).
4. Firestore Database → **Rules** → вставьте содержимое `firestore.rules` → **Publish**
   (или `firebase deploy --only firestore:rules,hosting`). Без этого регистрация падает с
   `PERMISSION_DENIED`: база, созданная в production-режиме, по умолчанию запрещает всё.
5. (Необязательно) Чтобы брать каталог с Firebase Hosting, выполните `firebase deploy --only hosting`
   и укажите в `gradle.properties` `COURSES_API_BASE_URL=https://mobileapp-252b2.web.app/api/`.

Без `google-services.json` проект собирается и запускается: каталог работает, а вход и сохранение прогресса
выключены (на экранах входа показано предупреждение).

## Сборка

Нужен JDK 17+ и Android SDK 35.

```bash
./gradlew assembleDebug
```

Оплата — демо: «Pay Now» только записывает покупку в Firestore.

## Ресурсы

- Шрифт Poppins — SIL Open Font License, см. `licenses/Poppins-OFL.txt`.
- Иллюстрации (`app/src/main/assets/illustrations`) и обложки курсов нарисованы для этого проекта.
- Вход через Facebook из макета не добавлен: нужен App ID из Meta for Developers, а в России Facebook заблокирован.
