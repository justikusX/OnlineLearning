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
| `ui/screens/AudioPlayerSheet.kt` | Встроенный аудиоплеер (Media3 ExoPlayer): пауза, перемотка ±10 с, скорость 1–2x, следующий урок |

## Аудиоуроки

У урока есть поле `audioUrl` — такой урок играет прямо в приложении; урок засчитывается, когда прослушано 90%.
Уроки только с `videoUrl` по-прежнему открываются во внешнем плеере.

Записи взяты из [LibriVox](https://librivox.org) (общественное достояние) и стримятся с Internet Archive:

| Курс | Книга |
|---|---|
| Design & Craft | *Arts and Crafts Essays* (1893), Walter Crane, William Morris и др. |
| Color Theory | *A Color Notation* (1905), Albert H. Munsell |
| The Story of Photography | *The Romance of Modern Photography* (1908), Charles R. Gibson |
| History of Computing | *Passages from the Life of a Philosopher* (1864), Charles Babbage |

После изменения каталога снова выполните `firebase deploy --only hosting`.

## Настройка Firebase

1. Проект Firebase — `mobileapp-252b2`, Android-приложение с пакетом `com.example.onlinelearning`.
2. `app/google-services.json` уже лежит в репозитории (для своего проекта замените его файлом из Firebase Console).
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
