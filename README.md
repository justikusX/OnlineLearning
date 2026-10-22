# Online Learning (Android)

Приложение по макету Figma «Online Learning App Design (Community)».
Kotlin + Jetpack Compose, **Retrofit** — каталог курсов, **Firebase** — Auth и Firestore.

## Архитектура

| Слой | Что делает |
|---|---|
| `data/remote/CourseApi.kt` | Retrofit + OkHttp: `GET {COURSES_API_BASE_URL}courses.json` |
| `data/CourseRepository.kt` | Кэш каталога; если API недоступен — берёт `assets/courses.json` |
| `data/AuthRepository.kt` | Firebase Auth: вход, регистрация, сброс пароля |
| `data/UserRepository.kt` | Firestore `users/{uid}`: избранное, покупки, прогресс уроков |
| `ui/ViewModels.kt` | `SessionViewModel`, `AuthViewModel`, `CoursesViewModel` |
| `ui/screens/*` | Онбординг, Log In / Sign Up, Home, Course (поиск + категории), карточка курса с оплатой, My courses, Account |

## Настройка Firebase

1. Создайте проект в [Firebase Console](https://console.firebase.google.com), добавьте Android-приложение
   с пакетом `com.example.onlinelearning`.
2. Скачайте `google-services.json` в папку `app/`.
3. Authentication → Sign-in method → включите **Email/Password**.
4. Создайте базу Firestore и задеплойте правила и каталог:
   ```bash
   firebase deploy --only firestore:rules,hosting
   ```
5. В `gradle.properties` укажите `COURSES_API_BASE_URL=https://<project-id>.web.app/api/`.

Без `google-services.json` проект собирается и запускается: каталог работает, а вход и сохранение прогресса
выключены (на экранах входа показано предупреждение).

## Сборка

Нужен JDK 17+ и Android SDK 35.

```bash
./gradlew assembleDebug
```

Оплата — демо: «Pay Now» только записывает покупку в Firestore.
