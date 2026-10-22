# Online Learning (Android)

Приложение по макету Figma «Online Learning App Design (Community)».
Kotlin + Jetpack Compose, **Retrofit** — каталог курсов, **Firebase** — Auth и Firestore.

## Архитектура

| Слой | Что делает |
|---|---|
| `data/remote/CourseApi.kt` | Retrofit + OkHttp: `GET {COURSES_API_BASE_URL}courses.json` (по умолчанию — файл `hosting/public/api/courses.json` прямо из этого репозитория на GitHub) |
| `data/CourseRepository.kt` | Кэш каталога; если API недоступен — берёт `assets/courses.json` |
| `data/AuthRepository.kt` | Firebase Auth: вход, регистрация, сброс пароля |
| `data/UserRepository.kt` | Firestore `users/{uid}`: избранное, покупки, прогресс уроков |
| `ui/ViewModels.kt` | `SessionViewModel`, `AuthViewModel`, `CoursesViewModel` |
| `ui/screens/*` | Онбординг, Log In / Sign Up, Home, Course (поиск + категории), карточка курса с оплатой, My courses, Account |
| `ui/player/LessonPlayback.kt`, `ui/screens/LessonPlayerSheet.kt` | Встроенный плеер уроков: аудио и видео (Media3 ExoPlayer) и озвучка текста (TextToSpeech); пауза, перемотка ±10 с, скорость 1–2x, следующий урок |

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
3. Authentication → Sign-in method → включите **Email/Password**.
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
