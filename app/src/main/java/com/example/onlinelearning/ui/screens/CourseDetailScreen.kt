package com.example.onlinelearning.ui.screens

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.example.onlinelearning.data.model.Course
import com.example.onlinelearning.data.model.Lesson
import com.example.onlinelearning.data.model.LessonKind
import com.example.onlinelearning.ui.CoursesViewModel
import com.example.onlinelearning.ui.SessionViewModel
import com.example.onlinelearning.ui.components.CourseImage
import com.example.onlinelearning.ui.components.EmptyState
import com.example.onlinelearning.ui.components.LoadingBox
import com.example.onlinelearning.ui.components.PrimaryButton
import com.example.onlinelearning.ui.player.LessonPlayback
import com.example.onlinelearning.ui.player.MediaLessonPlayback
import com.example.onlinelearning.ui.theme.Ink
import com.example.onlinelearning.ui.theme.Orange
import com.example.onlinelearning.ui.theme.PinkLight
import com.example.onlinelearning.ui.theme.Primary
import kotlinx.coroutines.delay

/** A lesson counts as done once this share of it has been played. */
private const val COMPLETE_AT = 0.9f

@Composable
fun CourseDetailScreen(
    courseId: String,
    session: SessionViewModel,
    courses: CoursesViewModel,
    onBack: () -> Unit,
    showMessage: (String) -> Unit,
) {
    val state by courses.state.collectAsStateWithLifecycle()
    val profile by session.profile.collectAsStateWithLifecycle()
    val user by session.user.collectAsStateWithLifecycle()
    val course = state.courses.firstOrNull { it.id == courseId }
    val context = LocalContext.current

    var playing by remember { mutableStateOf<Lesson?>(null) }
    val playback = remember(playing?.id) { playing?.let { LessonPlayback.create(context, it) } }
    DisposableEffect(playback) { onDispose { playback?.release() } }
    var showPayment by remember { mutableStateOf(false) }

    if (course == null) {
        if (state.loading) LoadingBox() else EmptyState(Icons.Filled.Lock, "Course not found", "")
        return
    }

    val purchased = course.price == 0.0 || profile?.purchased?.contains(course.id) == true
    val favorite = profile?.favorites?.contains(course.id) == true
    val completed = profile?.progress?.get(course.id).orEmpty()
    val currentCourse by rememberUpdatedState(course)

    // Refresh the position and record the lesson once 90% of it has been played.
    LaunchedEffect(playback) {
        val pb = playback ?: return@LaunchedEffect
        val lesson = playing ?: return@LaunchedEffect
        var reported = false
        while (true) {
            pb.tick()
            val d = pb.durationMs
            if (!reported && (pb.ended || (d > 0 && pb.positionMs >= d * COMPLETE_AT))) {
                reported = true
                session.completeLesson(currentCourse, lesson)
            }
            delay(500)
        }
    }

    fun isUnlocked(lesson: Lesson) = purchased || lesson.free

    fun play(lesson: Lesson) {
        if (!isUnlocked(lesson)) {
            showMessage("Купите курс, чтобы открыть этот урок")
            return
        }
        if (lesson.kind == LessonKind.NONE) {
            showMessage("У этого урока пока нет записи")
            return
        }
        playing = lesson
    }

    fun nextAfter(lesson: Lesson): Lesson? =
        course.lessons.dropWhile { it.id != lesson.id }.drop(1).firstOrNull()

    val video = (playback as? MediaLessonPlayback)?.takeIf { playing?.kind == LessonKind.VIDEO }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item {
                if (video != null) {
                    HeroVideo(video, onBack = onBack, onClose = { playing = null })
                } else {
                    Box(Modifier.fillMaxWidth().background(PinkLight)) {
                        CourseImage(course.imageUrl, Modifier.fillMaxWidth().height(260.dp))
                        Column(Modifier.statusBarsPadding().padding(8.dp)) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Ink)
                            }
                            if (course.isBestseller) {
                                Text(
                                    "BESTSELLER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink,
                                    modifier = Modifier
                                        .padding(start = 12.dp, top = 4.dp)
                                        .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp))
                                        .background(Color(0xFFFFD233))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(course.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (course.price == 0.0) "Free" else course.priceLabel,
                            color = Primary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${course.durationLabel} · ${course.lessons.size} Lessons",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("About this course", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(course.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    if (course.sourceName.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Источник: ${course.sourceName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            modifier = Modifier.clickable {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(course.sourceUrl))) }
                            },
                        )
                    }
                }
            }
            itemsIndexed(course.lessons, key = { _, l -> l.id }) { index, lesson ->
                val current = playing?.id == lesson.id
                LessonRow(
                    number = index + 1,
                    lesson = lesson,
                    unlocked = isUnlocked(lesson),
                    done = lesson.id in completed,
                    current = current,
                    playingNow = current && playback?.isPlaying == true,
                    onClick = {
                        if (current && playback != null) {
                            if (playback.isPlaying) playback.pause() else playback.play()
                        } else {
                            play(lesson)
                        }
                    },
                )
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(width = 70.dp, height = 50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PinkLight)
                        .clickable { session.toggleFavorite(course.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "В избранное",
                        tint = Orange,
                    )
                }
                if (purchased) {
                    PrimaryButton("Start learning", { course.lessons.firstOrNull { it.id !in completed }?.let(::play) }, Modifier.weight(1f))
                } else {
                    PrimaryButton(
                        "Buy Now",
                        {
                            if (user == null) showMessage("Войдите в аккаунт, чтобы купить курс") else showPayment = true
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }

    val lesson = playing
    if (lesson != null && playback != null && lesson.kind != LessonKind.VIDEO) {
        val next = nextAfter(lesson)
        LessonPlayerSheet(
            course = course,
            lesson = lesson,
            playback = playback,
            hasNext = next != null,
            onNext = { next?.let(::play) },
            onDismiss = { playing = null },
        )
    }

    if (showPayment) {
        PaymentFlow(
            course = course,
            onConfirm = { done -> session.purchase(course, done) },
            onDismiss = { showPayment = false },
            onStartLearning = {
                showPayment = false
                course.lessons.firstOrNull { it.id !in completed }?.let { playing = it }
            },
        )
    }
}

/** Video lesson playing in place of the course picture, with a fullscreen mode. */
@Composable
private fun HeroVideo(video: MediaLessonPlayback, onBack: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var fullscreen by remember { mutableStateOf(false) }
    var heroView by remember { mutableStateOf<PlayerView?>(null) }

    Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = video.player
                    keepScreenOn = true
                    setFullscreenButtonClickListener { fullscreen = true }
                }.also { heroView = it }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Закрыть видео", tint = Color.White) }
        }
    }

    if (fullscreen) {
        val activity = context.findActivity()
        DisposableEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            var fullView by remember { mutableStateOf<PlayerView?>(null) }
            DisposableEffect(Unit) {
                // Hand the video surface back to the small player when leaving fullscreen.
                onDispose { PlayerView.switchTargetView(video.player, fullView, heroView) }
            }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            keepScreenOn = true
                            setFullscreenButtonClickListener { fullscreen = false }
                        }.also {
                            PlayerView.switchTargetView(video.player, heroView, it)
                            fullView = it
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun LessonRow(
    number: Int,
    lesson: Lesson,
    unlocked: Boolean,
    done: Boolean,
    current: Boolean,
    playingNow: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (current) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(number),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(48.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            Text(
                when (lesson.kind) {
                    LessonKind.AUDIO -> "Audio · "
                    LessonKind.VIDEO -> "Video · "
                    LessonKind.NARRATION -> "Narrated · "
                    LessonKind.NONE -> ""
                } + "${lesson.durationMinutes} mins" + if (done) " ✓" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (done) Orange else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(if (current) Modifier.border(2.dp, Orange, CircleShape) else Modifier)
                .background(if (unlocked) Primary else Primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when {
                    !unlocked -> Icons.Filled.Lock
                    playingNow -> Icons.Filled.Pause
                    lesson.isAudio -> Icons.Filled.Headphones
                    else -> Icons.Filled.PlayArrow
                },
                null,
                tint = Color.White,
            )
        }
    }
}
